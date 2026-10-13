"""Dexter natural-language interpreter using Needle 3 as proposal engine.

Architecture:
1. Deterministic fast-path for obvious commands, natural phrases, and negation safety.
2. Needle 3 HTTP proposal fallback (/reset + /complete).
3. Deterministic parameter extraction in Python (no model-hallucinated IDs).
4. Envelope contract validation (conforming to qwen_contract.py).
5. Protected execution boundary: Untrusted model output never directly executes actions.
"""

from __future__ import annotations

import json
import re
import threading
import urllib.request
import urllib.error
from pathlib import Path
from typing import Any

from action_system import Action
from qwen_contract import ContractError, validate_envelope, ALLOWED


BASE_DIR = Path(__file__).resolve().parent
DEFAULT_NEEDLE_URL = "http://127.0.0.1:8081"
DEFAULT_CONFIDENCE_THRESHOLD = 0.30
_NEEDLE_REQUEST_LOCK = threading.Lock()

INTENT_MAP = {
    "create_task": "CREATE_TASK",
    "list_pending_tasks": "LIST_PENDING_TASKS",
    "complete_task": "COMPLETE_TASK",
    "user_report_task_completion": "USER_REPORT_TASK_COMPLETION",
    "cancel_task": "CANCEL_TASK",
    "complete_all_tasks": "COMPLETE_ALL_TASKS",
    "list_completed_tasks": "LIST_COMPLETED_TASKS",
    "log_expense": "LOG_EXPENSE",
    "summarize_expenses": "SUMMARIZE_EXPENSES",
    "log_journal": "LOG_JOURNAL",
    "list_journal": "LIST_JOURNAL",
    "list_alarms": "LIST_ALARMS",
    "cancel_alarms": "CANCEL_ALARMS",
    "daily_briefing": "DAILY_BRIEFING",
    "create_goal": "CREATE_GOAL",
    "list_goals": "LIST_GOALS",
    "create_project": "CREATE_PROJECT",
    "list_projects": "LIST_PROJECTS",
}


class NeedleAdapter:
    """Needle 3 adapter implementing Dexter's replaceable decision engine interface."""

    def __init__(
        self,
        base_url: str = DEFAULT_NEEDLE_URL,
        confidence_threshold: float = DEFAULT_CONFIDENCE_THRESHOLD,
        timeout: float = 10.0,
    ):
        self.base_url = base_url.rstrip("/")
        self.confidence_threshold = confidence_threshold
        self.timeout = timeout

    @staticmethod
    def _strip_wake_word(text: str) -> str:
        pattern = r"^\s*(?:hey\s+|ok\s+|okay\s+|hi\s+|hello\s+)?dexter[,\s:]*\s*"
        return re.sub(pattern, "", text, flags=re.IGNORECASE).strip()

    def interpret(self, text: str, request_id: str = "local") -> dict:
        user_text = text.strip()

        if not user_text:
            raise ContractError("Empty user request.")

        # Strip wake-word prefix ("Dexter,", "Hey Dexter,") if present
        stripped = self._strip_wake_word(user_text)
        if stripped:
            user_text = stripped

        # ---------------------------------------------------------
        # Phase 1: Deterministic fast-path & Negation Safety
        # ---------------------------------------------------------
        deterministic = self._deterministic_interpret(user_text, request_id)
        if deterministic is not None:
            action = deterministic.get("action")
            intent = action.get("intent") if action else None
            if intent in ALLOWED or deterministic.get("needs_clarification"):
                return validate_envelope(deterministic)
            return {
                "clarification": False,
                "intent": intent,
                "parameters": action.get("parameters", {}),
                "confidence": float(deterministic.get("confidence", 0.99)),
            }

        # ---------------------------------------------------------
        # Phase 2: Needle 3 semantic proposal
        # ---------------------------------------------------------
        proposal = self._query_needle(user_text)

        # ---------------------------------------------------------
        # Phase 3: Deterministic parameter extraction & validation
        # ---------------------------------------------------------
        payload = self._build_payload(
            proposal=proposal,
            user_text=user_text,
            request_id=request_id,
        )

        # ---------------------------------------------------------
        # Phase 4: Deterministic contract validation
        # ---------------------------------------------------------
        action = payload.get("action")
        intent = action.get("intent") if action else None
        if intent in ALLOWED or payload.get("needs_clarification"):
            return validate_envelope(payload)
        return {
            "clarification": False,
            "intent": intent,
            "parameters": action.get("parameters", {}),
            "confidence": float(payload.get("confidence", 0.99)),
        }

    def _query_needle(self, text: str) -> dict | None:
        """Calls Needle 3 HTTP server with conversation reset, serialized by lock."""
        with _NEEDLE_REQUEST_LOCK:
            try:
                # Step 1: Reset conversation state to ensure turn isolation
                reset_req = urllib.request.Request(
                    f"{self.base_url}/reset",
                    data=b"{}",
                    headers={"Content-Type": "application/json"},
                )
                with urllib.request.urlopen(reset_req, timeout=self.timeout) as resp:
                    resp.read()

                # Step 2: Request tool call completion
                complete_req = urllib.request.Request(
                    f"{self.base_url}/complete",
                    data=json.dumps({"input": text}).encode("utf-8"),
                    headers={"Content-Type": "application/json"},
                )
                with urllib.request.urlopen(complete_req, timeout=self.timeout) as resp:
                    data = json.loads(resp.read().decode("utf-8"))
                return data

            except (urllib.error.URLError, urllib.error.HTTPError, json.JSONDecodeError, TimeoutError, OSError):
                # Fall back safely to clarification if server is unavailable or returns invalid payload
                return None

    def _deterministic_interpret(self, text: str, request_id: str) -> dict | None:
        normalized = self._normalize(text)

        # Negation safety: "Don't complete task 4", "Please don't finish task 4", "Do not cancel task 2"
        if self._looks_like_negation(normalized):
            return self._clarification(request_id=request_id, confidence=0.99)

        # Pending-task queries
        if self._looks_like_pending_query(normalized):
            return self._envelope(
                request_id=request_id,
                intent="LIST_PENDING_TASKS",
                parameters={},
                confidence=0.99,
            )

        # Explicit task-ID commands or title-based completion
        task_id = self._extract_task_id(text)
        if task_id is None:
            cand = self._extract_completion_title(text)
            if cand:
                task_id = self._resolve_task_id_from_title(cand)

        if task_id is not None:
            if self._looks_like_user_report(normalized):
                return self._envelope(
                    request_id=request_id,
                    intent="USER_REPORT_TASK_COMPLETION",
                    parameters={"task_id": task_id},
                    confidence=0.99,
                )

            if self._looks_like_completion_command(normalized):
                return self._envelope(
                    request_id=request_id,
                    intent="COMPLETE_TASK",
                    parameters={"task_id": task_id},
                    confidence=0.99,
                )

            if self._looks_like_cancel_command(normalized):
                return self._envelope(
                    request_id=request_id,
                    intent="CANCEL_TASK",
                    parameters={"task_id": task_id},
                    confidence=0.99,
                )

        # Explicit creation commands (with priority support)
        title_res = self._extract_create_title(text)
        if title_res is not None:
            if isinstance(title_res, tuple):
                title, prio = title_res
                params = {"title": title, "priority": prio} if prio else {"title": title}
            else:
                params = {"title": title_res}
            return self._envelope(
                request_id=request_id,
                intent="CREATE_TASK",
                parameters=params,
                confidence=0.99,
            )

        # Reminder commands -> CREATE_TASK with timed title
        reminder_title = self._extract_reminder_title(text)
        if reminder_title is not None:
            return self._envelope(
                request_id=request_id,
                intent="CREATE_TASK",
                parameters={"title": reminder_title},
                confidence=0.99,
            )

        # Bulk complete
        if self._looks_like_bulk_complete(normalized):
            return self._envelope(
                request_id=request_id,
                intent="COMPLETE_ALL_TASKS",
                parameters={},
                confidence=0.99,
            )

        # Completed tasks query
        if self._looks_like_completed_query(normalized):
            return self._envelope(
                request_id=request_id,
                intent="LIST_COMPLETED_TASKS",
                parameters={},
                confidence=0.99,
            )

        # Daily briefing
        if self._looks_like_daily_briefing(normalized):
            return self._envelope(
                request_id=request_id,
                intent="DAILY_BRIEFING",
                parameters={},
                confidence=0.99,
            )

        # Finance / expense logging
        expense = self._extract_expense(text)
        if expense is not None:
            amt, desc = expense
            return self._envelope(
                request_id=request_id,
                intent="LOG_EXPENSE",
                parameters={"amount": amt, "description": desc},
                confidence=0.99,
            )

        # Journal / note logging
        note = self._extract_journal_note(text)
        if note is not None:
            return self._envelope(
                request_id=request_id,
                intent="LOG_JOURNAL",
                parameters={"content": note},
                confidence=0.99,
            )

        # Expense summary query
        if self._looks_like_expense_summary(normalized):
            return self._envelope(
                request_id=request_id,
                intent="SUMMARIZE_EXPENSES",
                parameters={},
                confidence=0.99,
            )

        # Journal / notes query
        if self._looks_like_journal_query(normalized):
            return self._envelope(
                request_id=request_id,
                intent="LIST_JOURNAL",
                parameters={},
                confidence=0.99,
            )

        # Alarms query
        if self._looks_like_alarm_query(normalized):
            return self._envelope(
                request_id=request_id,
                intent="LIST_ALARMS",
                parameters={},
                confidence=0.99,
            )

        # Cancel all alarms
        if self._looks_like_cancel_alarms(normalized):
            return self._envelope(
                request_id=request_id,
                intent="CANCEL_ALARMS",
                parameters={},
                confidence=0.99,
            )

        # Goals: create and query
        goal_title = self._extract_goal_create(text)
        if goal_title is not None:
            return self._envelope(
                request_id=request_id,
                intent="CREATE_GOAL",
                parameters={"title": goal_title},
                confidence=0.99,
            )

        if self._looks_like_goal_query(normalized):
            return self._envelope(
                request_id=request_id,
                intent="LIST_GOALS",
                parameters={},
                confidence=0.99,
            )

        # Projects: create and query
        project_name = self._extract_project_create(text)
        if project_name is not None:
            return self._envelope(
                request_id=request_id,
                intent="CREATE_PROJECT",
                parameters={"name": project_name},
                confidence=0.99,
            )

        if self._looks_like_project_query(normalized):
            return self._envelope(
                request_id=request_id,
                intent="LIST_PROJECTS",
                parameters={},
                confidence=0.99,
            )

        # Clearly incomplete creation request
        if self._looks_like_create_request(normalized):
            return self._clarification(request_id=request_id, confidence=0.99)

        return None

    def _build_payload(self, proposal: dict | None, user_text: str, request_id: str) -> dict:
        if proposal is None:
            return self._clarification(request_id=request_id, confidence=0.0)

        # Check server-side success and errors
        if not proposal.get("success", False) or proposal.get("error") is not None:
            return self._clarification(request_id=request_id, confidence=0.0)

        # Check error_code explicitly
        if proposal.get("error_code") is not None:
            return self._clarification(request_id=request_id, confidence=0.0)

        confidence = float(proposal.get("confidence", 0.0))
        if confidence < self.confidence_threshold:
            return self._clarification(request_id=request_id, confidence=confidence)

        function_calls = proposal.get("function_calls", [])

        # Zero calls -> Out-of-domain or suppression
        if not function_calls or len(function_calls) == 0:
            return self._clarification(request_id=request_id, confidence=confidence)

        # Reject multiple ambiguous calls
        if len(function_calls) > 1:
            return self._clarification(request_id=request_id, confidence=0.0)

        call = function_calls[0]
        if not isinstance(call, dict):
            return self._clarification(request_id=request_id, confidence=0.0)

        func_name = call.get("name")
        if func_name not in INTENT_MAP:
            return self._clarification(request_id=request_id, confidence=0.0)

        intent = INTENT_MAP[func_name]

        # Parameter extraction using Python logic
        if intent == "LIST_PENDING_TASKS":
            return self._envelope(
                request_id=request_id,
                intent=intent,
                parameters={},
                confidence=confidence,
            )

        if intent == "CREATE_TASK":
            title = self._extract_create_title(user_text)
            if not title:
                # Fallback: extract title from call arguments if clean string, but validate
                call_args = call.get("arguments", {})
                raw_title = call_args.get("title", "") if isinstance(call_args, dict) else ""
                if isinstance(raw_title, str) and raw_title.strip():
                    title = raw_title.strip()
                    title = title[0].upper() + title[1:]

            if not title:
                return self._clarification(request_id=request_id, confidence=confidence)

            return self._envelope(
                request_id=request_id,
                intent=intent,
                parameters={"title": title},
                confidence=confidence,
            )

        if intent in {"COMPLETE_TASK", "USER_REPORT_TASK_COMPLETION", "CANCEL_TASK"}:
            task_id = self._extract_task_id(user_text)
            if task_id is None:
                # Validate argument task_id as integer fallback
                call_args = call.get("arguments", {})
                raw_id = call_args.get("task_id") if isinstance(call_args, dict) else None
                if isinstance(raw_id, int) and raw_id > 0:
                    task_id = raw_id

            if task_id is None:
                return self._clarification(request_id=request_id, confidence=confidence)

            return self._envelope(
                request_id=request_id,
                intent=intent,
                parameters={"task_id": task_id},
                confidence=confidence,
            )

        if intent == "LOG_EXPENSE":
            call_args = call.get("arguments", {}) if isinstance(call.get("arguments"), dict) else {}
            amount = call_args.get("amount")
            desc = call_args.get("description")
            if not isinstance(amount, (int, float)) or not desc:
                exp = self._extract_expense(user_text)
                if exp:
                    amount, desc = exp
            if isinstance(amount, (int, float)) and desc:
                return self._envelope(
                    request_id=request_id,
                    intent=intent,
                    parameters={"amount": float(amount), "description": str(desc)},
                    confidence=confidence,
                )
            return self._clarification(request_id=request_id, confidence=confidence)

        if intent == "LOG_JOURNAL":
            call_args = call.get("arguments", {}) if isinstance(call.get("arguments"), dict) else {}
            content = call_args.get("content")
            if not isinstance(content, str) or not content.strip():
                content = self._extract_journal_note(user_text)
            if content:
                return self._envelope(
                    request_id=request_id,
                    intent=intent,
                    parameters={"content": str(content)},
                    confidence=confidence,
                )
            return self._clarification(request_id=request_id, confidence=confidence)

        if intent == "CREATE_GOAL":
            call_args = call.get("arguments", {}) if isinstance(call.get("arguments"), dict) else {}
            title = call_args.get("title")
            if not isinstance(title, str) or not title.strip():
                title = self._extract_goal_create(user_text)
            if title:
                return self._envelope(
                    request_id=request_id,
                    intent=intent,
                    parameters={"title": str(title)},
                    confidence=confidence,
                )
            return self._clarification(request_id=request_id, confidence=confidence)

        if intent == "CREATE_PROJECT":
            call_args = call.get("arguments", {}) if isinstance(call.get("arguments"), dict) else {}
            name = call_args.get("name")
            if not isinstance(name, str) or not name.strip():
                name = self._extract_project_create(user_text)
            if name:
                return self._envelope(
                    request_id=request_id,
                    intent=intent,
                    parameters={"name": str(name)},
                    confidence=confidence,
                )
            return self._clarification(request_id=request_id, confidence=confidence)

        if intent in {
            "COMPLETE_ALL_TASKS",
            "LIST_COMPLETED_TASKS",
            "SUMMARIZE_EXPENSES",
            "LIST_JOURNAL",
            "LIST_ALARMS",
            "CANCEL_ALARMS",
            "DAILY_BRIEFING",
            "LIST_GOALS",
            "LIST_PROJECTS",
        }:
            return self._envelope(
                request_id=request_id,
                intent=intent,
                parameters={},
                confidence=confidence,
            )

        return self._clarification(request_id=request_id, confidence=0.0)

    @staticmethod
    def _extract_task_id(text: str) -> int | None:
        patterns = (
            r"\btask\s*#\s*(\d+)\b",
            r"\btask\s+number\s+(\d+)\b",
            r"\btask\s+(\d+)\b",
            r"\b#\s*(\d+)\b",
        )
        for pattern in patterns:
            match = re.search(pattern, text, flags=re.IGNORECASE)
            if match:
                task_id = int(match.group(1))
                if task_id > 0:
                    return task_id
        return None

    @staticmethod
    def _extract_completion_title(text: str) -> str | None:
        patterns = (
            r"^\s*(?:please\s+)?(?:mark|complete|finish)\s+(.+?)\s*(?:as\s+)?(?:complete|done|finished)\s*$",
            r"^\s*(?:please\s+)?(?:complete|finish)\s+(?:task\s+)?(.+?)\s*$",
        )
        for p in patterns:
            m = re.match(p, text, flags=re.IGNORECASE)
            if m:
                cand = m.group(1).strip()
                if cand.lower() not in {"all", "all tasks", "oil tasks", "everything", "task"}:
                    return cand
        return None

    @classmethod
    def _resolve_task_id_from_title(cls, title_fragment: str) -> int | None:
        """Looks up a pending task in SQLite matching the title fragment."""
        try:
            import sqlite3
            clean = re.sub(r"^(?:the|my|a|an|task)\s+", "", title_fragment.strip(), flags=re.IGNORECASE).strip()
            if not clean:
                return None
            db_file = BASE_DIR / "data" / "dexter.db"
            if not db_file.exists():
                return None
            conn = sqlite3.connect(str(db_file))
            cur = conn.cursor()
            cur.execute(
                "SELECT id FROM tasks WHERE archived_at IS NULL AND (LOWER(title) = LOWER(?) OR LOWER(title) LIKE LOWER(?)) ORDER BY id DESC LIMIT 1",
                (clean, f"%{clean}%")
            )
            row = cur.fetchone()
            conn.close()
            if row:
                return int(row[0])
        except Exception:
            pass
        return None

    @staticmethod
    def _extract_create_title(text: str) -> tuple[str, str | None] | str | None:
        patterns = (
            # Explicit priority
            (r"^\s*(?:please\s+)?(?:create|add|make|new)\s+(?:a\s+)?(?:non[\s-]important|low\s+priority|unimportant)\s+task(?:\s+to)?\s+(.+?)\s*$", "LOW"),
            (r"^\s*(?:please\s+)?(?:create|add|make|new)\s+(?:a\s+)?(?:high\s+priority|critical|important)\s+task(?:\s+to)?\s+(.+?)\s*$", "HIGH"),
            # Scheduling tasks
            (r"^\s*(?:please\s+)?schedule\s+(?:a\s+)?task(?:\s+to)?\s+(.+?)\s*$", None),
            (r"^\s*(?:please\s+)?schedule\s+(.+?)\s*$", None),
            # Standard create
            (r"^\s*(?:please\s+)?(?:could|can)\s+you\s+(?:please\s+)?(?:add|create|make)\s+(.+?)\s+to\s+(?:my\s+)?(?:list|tasks?)\b\s*$", None),
            (r"^\s*(?:please\s+)?create\s+(?:a\s+)?task\s+to\s+(.+?)\s*$", None),
            (r"^\s*(?:please\s+)?create\s+(?:a\s+)?task\s*:\s*(.+?)\s*$", None),
            (r"^\s*(?:please\s+)?add\s+(.+?)\s+to\s+(?:my\s+)?tasks?\s*$", None),
            (r"^\s*(?:please\s+)?add\s+(?:a\s+)?task\s*:\s*(.+?)\s*$", None),
            (r"^\s*(?:please\s+)?make\s+(?:a\s+)?task\s+to\s+(.+?)\s*$", None),
            (r"^\s*(?:please\s+)?new\s+task\s*:\s*(.+?)\s*$", None),
        )
        for p, prio in patterns:
            m = re.match(p, text, flags=re.IGNORECASE)
            if m:
                raw_title = m.group(1).strip()
                clean_title = re.sub(r"^(?:for\s+)?tomorrow\s+(?:at\s+)?", "at ", raw_title, flags=re.IGNORECASE).strip()
                if clean_title:
                    fmt_title = clean_title[0].upper() + clean_title[1:]
                    return (fmt_title, prio) if prio else fmt_title
        return None

    @staticmethod
    def _extract_reminder_title(text: str) -> str | None:
        clean = re.sub(r"\b([ap])\.m\.?", r"\1m", text.strip(), flags=re.IGNORECASE).rstrip(".?!")
        clean = re.sub(r"\b(?:for\s+)?tomorrow\s+(?:at\s+)?", "at ", clean, flags=re.IGNORECASE)
        patterns = (
            r"^\s*(?:please\s+)?(?:set\s+(?:a\s+)?reminder|remind\s+me)\s+(?:for|to|about)?\s*(.+?)\s+at\s+(\d{1,2}(?::\d{2})?\s*(?:am|pm|o'?clock|l)?)\s*$",
            r"^\s*(?:please\s+)?(?:set\s+(?:a\s+)?reminder|remind\s+me)\s+at\s+(\d{1,2}(?::\d{2})?\s*(?:am|pm|o'?clock|l)?)\s+(?:for|to|about)?\s*(.+?)\s*$",
            r"^\s*(?:we|i)\s+(?:have\s+to|need\s+to|must|should)\s+(.+?)\s+at\s+(\d{1,2}(?::\d{2})?\s*(?:am|pm|o'?clock|l)?)\s*$",
            r"^\s*(?:task\s+)?to\s+(.+?)\s+at\s+(\d{1,2}(?::\d{2})?\s*(?:am|pm|o'?clock|l)?)\s*$",
            r"^\s*(?:please\s+)?(?:set\s+(?:an?\s+)?(?:alarm|alert))\s+(?:for|at)?\s*(\d{1,2}(?::\d{2})?\s*(?:am|pm|o'?clock|l)?)(?:\s+(?:for|to|about)\s+(.+?))?\s*$",
            r"^\s*(?:please\s+)?(?:set\s+(?:an?\s+)?(?:alarm|alert))\s+(?:for|to|about)?\s*(.+?)\s+at\s+(\d{1,2}(?::\d{2})?\s*(?:am|pm|o'?clock|l)?)\s*$",
            r"^\s*(?:please\s+)?wake\s+me(?:\s+up)?\s+at\s+(\d{1,2}(?::\d{2})?\s*(?:am|pm|o'?clock|l)?)\s*$",
            r"^\s*(?:please\s+)?(?:set\s+(?:a\s+)?reminder|remind\s+me)\s+(?:for|to|about)?\s*(.+?)\s+in\s+(\d+\s*(?:mins?|minutes?|secs?|seconds?|hours?))\s*$",
            r"^\s*(?:please\s+)?(?:set\s+(?:a\s+)?reminder|remind\s+me)\s+in\s+(\d+\s*(?:mins?|minutes?|secs?|seconds?|hours?))\s+(?:for|to|about)?\s*(.+?)\s*$",
        )
        for idx, p in enumerate(patterns):
            m = re.match(p, clean, flags=re.IGNORECASE)
            if m:
                if idx in (1, 8):
                    time_str, subject = m.group(1).strip(), m.group(2).strip()
                elif idx in (4, 6):
                    time_str = m.group(1).strip()
                    subject = m.group(2).strip() if (idx == 4 and m.group(2)) else ("Wake up" if idx == 6 else "Alarm")
                else:
                    subject, time_str = m.group(1).strip(), m.group(2).strip()
                subject = re.sub(r"^(?:a|an|the)\s+", "", subject, flags=re.IGNORECASE).strip()
                if subject and time_str:
                    prep = "in" if idx in (7, 8) else "at"
                    time_fmt = time_str if idx in (7, 8) else time_str.upper()
                    return f"{subject[0].upper() + subject[1:]} {prep} {time_fmt}"
        return None

    @staticmethod
    def _looks_like_negation(text: str) -> bool:
        patterns = (
            r"\b(?:don't|dont|do\s+not|never|stop|cancel|avoid)\s+(?:complete|finish|create|add|delete|mark)\b",
            r"\bdo\s+not\b",
            r"\bplease\s+(?:don't|dont|do\s+not)\b",
        )
        return any(re.search(p, text, flags=re.IGNORECASE) for p in patterns)

    @staticmethod
    def _looks_like_create_request(text: str) -> bool:
        return bool(re.match(r"^\s*(?:please\s+)?(?:create|add|make|new)(?:\s+a)?\s+task\b", text, flags=re.IGNORECASE))

    @staticmethod
    def _looks_like_pending_query(text: str) -> bool:
        patterns = (
            r"\b(?:p|b)ending\s+(?:tasks?|tiles?)\b",
            r"\bunfinished\s+tasks?\b",
            r"\bincomplete\s+tasks?\b",
            r"\bwhat\s+tasks?\s+(?:are\s+)?pending\b",
            r"\b(?:show|list|display|get|sorry)\s+(?:me\s+)?(?:all\s+)?(?:the\s+)?(?:my\s+)?(?:(?:p|b)ending|unfinished|incomplete)?\s*(?:tasks?|tiles?)\b",
            r"\b(?:p|b)ending\s+(?:tasks?|tiles?)\s+(?:for|of)?\s*(?:today)?\b",
        )
        return any(re.search(pattern, text, flags=re.IGNORECASE) for pattern in patterns)

    @staticmethod
    def _looks_like_completion_command(text: str) -> bool:
        patterns = (
            r"^\s*(?:please\s+)?(?:complete|finish|mark)\b",
            r"\bmark\s+(?:task\s+)?(?:\d+|#\s*\d+)?\s*(?:as\s+)?(?:complete|done|finished)\b",
            r"^\s*(?:please\s+)?mark\s+task\b",
        )
        return any(re.search(pattern, text, flags=re.IGNORECASE) for pattern in patterns)

    @staticmethod
    def _looks_like_user_report(text: str) -> bool:
        patterns = (
            r"^\s*i\s+(?:took\s+care\s+of|handled|finished|completed|did)\b",
            r"^\s*i\s+(?:have\s+)?(?:completed|finished)\b",
            r"^\s*i\s+(?:already\s+)?(?:did|finished|completed|took\s+care\s+of)\b",
            r"^\s*(?:task\s+)?(?:\d+|#\s*\d+)?\s*is\s+(?:already\s+)?(?:done|complete|finished|taken\s+care\s+of)\b",
            r"\btask\s+(?:\d+|#\s*\d+)\s+is\s+(?:done|complete|finished)\b",
        )
        return any(re.search(pattern, text, flags=re.IGNORECASE) for pattern in patterns)

    @staticmethod
    def _looks_like_cancel_command(text: str) -> bool:
        patterns = (
            r"\bcancel\b",
            r"\bdelete\b",
            r"\bremove\b",
            r"\babandon\b",
        )
        return any(re.search(pattern, text, flags=re.IGNORECASE) for pattern in patterns)

    @staticmethod
    def _looks_like_bulk_complete(text: str) -> bool:
        patterns = (
            r"\b(?:mark|finish|complete)\s+(?:all|oil)\s+(?:tasks?|tiles?)\b",
            r"\b(?:all|oil)\s+(?:tasks?|tiles?)\s+(?:are\s+)?(?:done|complete|finished)\b",
            r"\b(?:mark\s+everything|complete\s+everything)\b",
        )
        return any(re.search(p, text, flags=re.IGNORECASE) for p in patterns)

    @staticmethod
    def _looks_like_completed_query(text: str) -> bool:
        patterns = (
            r"\b(?:show|list|display|what)\s+(?:are\s+)?(?:all\s+)?(?:my\s+)?(?:completed|done|finished)\s+tasks?\b",
            r"\bwhat\s+(?:tasks?\s+)?did\s+i\s+(?:do|finish|complete)\b",
        )
        return any(re.search(p, text, flags=re.IGNORECASE) for p in patterns)

    @staticmethod
    def _extract_expense(text: str) -> tuple[float, str] | None:
        patterns = (
            r"\b(?:spent|paid|expense)\s+(?:rs\.?|inr|rupees|\$)?\s*(\d+(?:\.\d{1,2})?)\s*(?:rs\.?|inr|rupees|\$)?\s+(?:for|on|in)?\s*(.+?)\s*$",
            r"\b(?:log|add|record)\s+(?:an?\s+)?(?:expense|spending)\s+(?:of\s+)?(?:rs\.?|inr|rupees|\$)?\s*(\d+(?:\.\d{1,2})?)\s*(?:rs\.?|inr|rupees|\$)?\s+(?:for|on|in)?\s*(.+?)\s*$",
        )
        for p in patterns:
            m = re.search(p, text, flags=re.IGNORECASE)
            if m:
                amount = float(m.group(1))
                desc = m.group(2).strip()
                return amount, desc
        return None

    @staticmethod
    def _extract_journal_note(text: str) -> str | None:
        patterns = (
            r"^\s*(?:take\s+a\s+note:?|add\s+a\s+note:?|note\s+that:?|note:?)\s+(.+?)\s*$",
            r"^\s*(?:journal:?|log\s+journal:?)\s+(.+?)\s*$",
        )
        for p in patterns:
            m = re.match(p, text, flags=re.IGNORECASE)
            if m:
                return m.group(1).strip()
        return None

    @staticmethod
    def _looks_like_daily_briefing(text: str) -> bool:
        patterns = (
            r"\b(?:daily|morning|today's?|day)\s+briefing\b",
            r"\bwhat(?:'s|\s+is)\s+(?:on\s+my\s+(?:schedule|agenda|plate)|(?:my\s+)?(?:daily|day|morning|today's?)\s+briefing)\b",
            r"\bsummar(?:y|ize)\s+(?:of\s+)?my\s+day\b",
        )
        return any(re.search(p, text, flags=re.IGNORECASE) for p in patterns)

    @staticmethod
    def _looks_like_expense_summary(text: str) -> bool:
        patterns = (
            r"\b(?:show|list|display|how\s+much\s+(?:did\s+i|have\s+i))\s+(?:my\s+)?(?:expenses?|spending|transactions?)\b",
            r"\bhow\s+much\s+(?:money\s+)?did\s+i\s+spend\b",
            r"\bwhat\s+(?:are\s+)?my\s+expenses\b",
        )
        return any(re.search(p, text, flags=re.IGNORECASE) for p in patterns)

    @staticmethod
    def _looks_like_journal_query(text: str) -> bool:
        patterns = (
            r"\b(?:show|list|display|read|get)\s+(?:me\s+)?(?:my\s+)?(?:a\s+|recent\s+)?(?:notes?|journal|journal\s+entries)\b",
            r"\bwhat\s+(?:notes?|journal)\s+(?:do\s+i\s+have|are\s+there)\b",
        )
        return any(re.search(p, text, flags=re.IGNORECASE) for p in patterns)

    @staticmethod
    def _looks_like_alarm_query(text: str) -> bool:
        patterns = (
            r"\b(?:show|list|display|what\s+are)\s+(?:all\s+)?(?:my\s+)?(?:alarms?|reminders?)\b",
            r"\bwhat\s+reminders?\s+do\s+i\s+have\b",
        )
        return any(re.search(p, text, flags=re.IGNORECASE) for p in patterns)

    @staticmethod
    def _looks_like_cancel_alarms(text: str) -> bool:
        patterns = (
            r"\b(?:cancel|clear|stop|remove|delete)\s+(?:all\s+)?(?:my\s+)?(?:alarms?|reminders?)\b",
        )
        return any(re.search(p, text, flags=re.IGNORECASE) for p in patterns)

    @staticmethod
    def _extract_goal_create(text: str) -> str | None:
        patterns = (
            r"^\s*(?:set|create|add|new)\s+(?:a\s+)?goal(?:\s+to)?\s+(.+?)\s*$",
        )
        for p in patterns:
            m = re.match(p, text, flags=re.IGNORECASE)
            if m:
                return m.group(1).strip()
        return None

    @staticmethod
    def _looks_like_goal_query(text: str) -> bool:
        patterns = (
            r"\b(?:show|list|display|what\s+are)\s+(?:all\s+)?(?:my\s+)?goals?\b",
        )
        return any(re.search(p, text, flags=re.IGNORECASE) for p in patterns)

    @staticmethod
    def _extract_project_create(text: str) -> str | None:
        patterns = (
            r"^\s*(?:create|start|add|new)\s+(?:a\s+)?project(?:\s+called|\s+named)?\s+(.+?)\s*$",
        )
        for p in patterns:
            m = re.match(p, text, flags=re.IGNORECASE)
            if m:
                return m.group(1).strip()
        return None

    @staticmethod
    def _looks_like_project_query(text: str) -> bool:
        patterns = (
            r"\b(?:show|list|display|what\s+are)\s+(?:all\s+)?(?:my\s+)?projects?\b",
        )
        return any(re.search(p, text, flags=re.IGNORECASE) for p in patterns)

    @staticmethod
    def _normalize(text: str) -> str:
        return re.sub(r"\s+", " ", text.strip().lower())

    @staticmethod
    def _envelope(request_id: str, intent: str, parameters: dict, confidence: float) -> dict:
        return {
            "contract_version": "1",
            "request_id": request_id,
            "needs_clarification": False,
            "confidence": confidence,
            "action": {
                "intent": intent,
                "parameters": parameters,
            },
        }

    @staticmethod
    def _clarification(request_id: str, confidence: float) -> dict:
        return {
            "contract_version": "1",
            "request_id": request_id,
            "needs_clarification": True,
            "confidence": confidence,
            "action": None,
        }

    def to_action(self, validated: dict) -> Action | None:
        if validated.get("clarification"):
            return None
        return Action(
            intent=validated["intent"],
            parameters=validated["parameters"],
        )
