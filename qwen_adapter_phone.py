"""Dexter natural-language interpreter.

Architecture:

1. Deterministic fast-path for obvious commands.
2. Qwen3.5 fallback for semantic intent classification.
3. Deterministic parameter extraction.
4. Contract validation.
5. No model-generated database IDs.
"""

from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any, Protocol

from action_system import Action
from qwen_contract import ContractError, validate_envelope


BASE_DIR = Path(__file__).resolve().parent
CONTRACT_PATH = BASE_DIR / "qwen_dexter_contract_v1.json"


INTENTS = (
    "CREATE_TASK",
    "LIST_PENDING_TASKS",
    "COMPLETE_TASK",
    "USER_REPORT_TASK_COMPLETION",
    "CANCEL_TASK",
)


CLASSIFIER_PROMPT = """You are Dexter's intent classifier.

Classify the user's request into exactly ONE of these intents:

CREATE_TASK
LIST_PENDING_TASKS
COMPLETE_TASK
USER_REPORT_TASK_COMPLETION
CANCEL_TASK
CLARIFICATION

Return ONLY the intent name.
Do not return JSON.
Do not explain.
Do not invent task IDs.

Definitions:

CREATE_TASK:
The user wants Dexter to create/add a new task.

LIST_PENDING_TASKS:
The user wants to see pending/current tasks.

COMPLETE_TASK:
The user directly instructs Dexter to complete/finish a task.

USER_REPORT_TASK_COMPLETION:
The user tells Dexter that they already completed a task.

CANCEL_TASK:
The user wants Dexter to cancel a task.

CLARIFICATION:
The request does not contain enough information to safely determine an action.

Examples:

"Create a task to buy milk"
CREATE_TASK

"Add buy milk to my tasks"
CREATE_TASK

"What tasks are pending?"
LIST_PENDING_TASKS

"Show my unfinished tasks"
LIST_PENDING_TASKS

"Complete task #4"
COMPLETE_TASK

"Finish task number 4"
COMPLETE_TASK

"I completed task #4"
USER_REPORT_TASK_COMPLETION

"I already finished task 4"
USER_REPORT_TASK_COMPLETION

"Cancel task #7"
CANCEL_TASK

"Delete task number 7"
CANCEL_TASK

"Do it"
CLARIFICATION

"Handle the thing"
CLARIFICATION
"""


class ChatModel(Protocol):
    def create_chat_completion(self, **kwargs: Any) -> Any:
        ...


class QwenAdapter:
    def __init__(self, llm: ChatModel):
        self.llm = llm
        self.schema = json.loads(
            CONTRACT_PATH.read_text(encoding="utf-8")
        )

    def interpret(self, text: str, request_id: str = "local") -> dict:
        user_text = text.strip()

        if not user_text:
            raise ContractError("Empty user request.")

        # ---------------------------------------------------------
        # Phase 1: deterministic fast-path.
        # ---------------------------------------------------------
        deterministic = self._deterministic_interpret(
            user_text,
            request_id,
        )

        if deterministic is not None:
            # Normalize deterministic and Qwen paths to the same
            # validated internal representation.
            return validate_envelope(deterministic)

        # ---------------------------------------------------------
        # Phase 2: Qwen semantic classification.
        # ---------------------------------------------------------
        intent = self._classify_with_qwen(user_text)

        # ---------------------------------------------------------
        # Phase 3: deterministic parameter extraction.
        # ---------------------------------------------------------
        payload = self._build_payload(
            intent=intent,
            user_text=user_text,
            request_id=request_id,
        )

        # ---------------------------------------------------------
        # Phase 4: deterministic contract validation.
        # ---------------------------------------------------------
        return validate_envelope(payload)

    def _deterministic_interpret(
        self,
        text: str,
        request_id: str,
    ) -> dict | None:
        normalized = self._normalize(text)

        # Pending-task queries.
        if self._looks_like_pending_query(normalized):
            return self._envelope(
                request_id=request_id,
                intent="LIST_PENDING_TASKS",
                parameters={},
                confidence=0.99,
            )

        # Explicit task-ID commands.
        task_id = self._extract_task_id(text)

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

        # Explicit creation commands.
        title = self._extract_create_title(text)

        if title is not None:
            return self._envelope(
                request_id=request_id,
                intent="CREATE_TASK",
                parameters={"title": title},
                confidence=0.99,
            )

        # Clearly incomplete creation request.
        if self._looks_like_create_request(normalized):
            return self._clarification(
                request_id=request_id,
                confidence=0.99,
            )

        return None

    def _classify_with_qwen(self, text: str) -> str:
        response = self.llm.create_chat_completion(
            messages=[
                {
                    "role": "system",
                    "content": CLASSIFIER_PROMPT,
                },
                {
                    "role": "user",
                    "content": text,
                },
            ],
            temperature=0.0,
            max_tokens=8,
        )

        content = self._extract_content(response)

        # IMPORTANT:
        # Do not sanitize arbitrary model output into a valid intent.
        #
        # For example:
        #   "COMPLETE_TASK 4"
        #
        # must NOT become:
        #   "COMPLETE_TASK"
        #
        # The model is allowed to provide only one exact token.
        intent = content.strip().upper()

        if intent not in INTENTS and intent != "CLARIFICATION":
            raise ContractError(
                f"Qwen returned invalid intent: {content!r}"
            )

        return intent

    def _build_payload(
        self,
        intent: str,
        user_text: str,
        request_id: str,
    ) -> dict:
        if intent == "CLARIFICATION":
            return self._clarification(
                request_id=request_id,
                confidence=0.75,
            )

        if intent == "LIST_PENDING_TASKS":
            return self._envelope(
                request_id=request_id,
                intent=intent,
                parameters={},
                confidence=0.90,
            )

        if intent == "CREATE_TASK":
            title = self._extract_create_title(user_text)

            if not title:
                return self._clarification(
                    request_id=request_id,
                    confidence=0.80,
                )

            return self._envelope(
                request_id=request_id,
                intent=intent,
                parameters={"title": title},
                confidence=0.90,
            )

        if intent in {
            "COMPLETE_TASK",
            "USER_REPORT_TASK_COMPLETION",
            "CANCEL_TASK",
        }:
            task_id = self._extract_task_id(user_text)

            if task_id is None:
                return self._clarification(
                    request_id=request_id,
                    confidence=0.90,
                )

            return self._envelope(
                request_id=request_id,
                intent=intent,
                parameters={"task_id": task_id},
                confidence=0.90,
            )

        raise ContractError(
            f"Unsupported intent: {intent!r}"
        )

    @staticmethod
    def _extract_task_id(text: str) -> int | None:
        patterns = (
            r"\btask\s*#\s*(\d+)\b",
            r"\btask\s+number\s+(\d+)\b",
            r"\btask\s+(\d+)\b",
        )

        for pattern in patterns:
            match = re.search(
                pattern,
                text,
                flags=re.IGNORECASE,
            )

            if match:
                task_id = int(match.group(1))

                if task_id > 0:
                    return task_id

        return None

    @staticmethod
    def _extract_create_title(text: str) -> str | None:
        patterns = (
            r"^\s*(?:please\s+)?create\s+(?:a\s+)?task\s+to\s+(.+?)\s*$",
            r"^\s*(?:please\s+)?create\s+(?:a\s+)?task\s*:\s*(.+?)\s*$",
            r"^\s*(?:please\s+)?add\s+(.+?)\s+to\s+(?:my\s+)?tasks?\s*$",
            r"^\s*(?:please\s+)?add\s+(?:a\s+)?task\s*:\s*(.+?)\s*$",
            r"^\s*(?:please\s+)?make\s+(?:a\s+)?task\s+to\s+(.+?)\s*$",
            r"^\s*(?:please\s+)?new\s+task\s*:\s*(.+?)\s*$",
        )

        for pattern in patterns:
            match = re.match(
                pattern,
                text,
                flags=re.IGNORECASE,
            )

            if match:
                title = match.group(1).strip()

                if title:
                    return title[0].upper() + title[1:]

        return None

    @staticmethod
    def _looks_like_create_request(text: str) -> bool:
        return bool(
            re.match(
                r"^\s*(?:please\s+)?(?:create|add|make|new)(?:\s+a)?\s+task\b",
                text,
                flags=re.IGNORECASE,
            )
        )

    @staticmethod
    def _looks_like_pending_query(text: str) -> bool:
        patterns = (
            r"\bpending\s+tasks?\b",
            r"\bunfinished\s+tasks?\b",
            r"\bincomplete\s+tasks?\b",
            r"\bwhat\s+tasks?\s+(?:are\s+)?pending\b",
            r"\bshow\s+(?:me\s+)?(?:my\s+)?(?:pending|unfinished|incomplete)\s+tasks?\b",
            r"\blist\s+(?:my\s+)?(?:pending|unfinished|incomplete)\s+tasks?\b",
        )

        return any(
            re.search(
                pattern,
                text,
                flags=re.IGNORECASE,
            )
            for pattern in patterns
        )

    @staticmethod
    def _looks_like_completion_command(text: str) -> bool:
        patterns = (
            r"\bcomplete\b",
            r"\bfinish\b",
            r"\bmark\b.*\bcomplete\b",
        )

        return any(
            re.search(
                pattern,
                text,
                flags=re.IGNORECASE,
            )
            for pattern in patterns
        )

    @staticmethod
    def _looks_like_user_report(text: str) -> bool:
        patterns = (
            r"^\s*i\s+(?:have\s+)?completed\b",
            r"^\s*i\s+(?:have\s+)?finished\b",
            r"^\s*i\s+did\b.*\btask\b",
            r"^\s*task\b.*\bis\s+(?:done|complete)\b",
        )

        return any(
            re.search(
                pattern,
                text,
                flags=re.IGNORECASE,
            )
            for pattern in patterns
        )

    @staticmethod
    def _looks_like_cancel_command(text: str) -> bool:
        patterns = (
            r"\bcancel\b",
            r"\bdelete\b",
            r"\bremove\b",
            r"\babandon\b",
        )

        return any(
            re.search(
                pattern,
                text,
                flags=re.IGNORECASE,
            )
            for pattern in patterns
        )

    @staticmethod
    def _normalize(text: str) -> str:
        return re.sub(
            r"\s+",
            " ",
            text.strip().lower(),
        )

    @staticmethod
    def _envelope(
        request_id: str,
        intent: str,
        parameters: dict,
        confidence: float,
    ) -> dict:
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
    def _clarification(
        request_id: str,
        confidence: float,
    ) -> dict:
        return {
            "contract_version": "1",
            "request_id": request_id,
            "needs_clarification": True,
            "confidence": confidence,
            "action": None,
        }

    @staticmethod
    def _strip_markdown_fence(content: str) -> str:
        text = content.strip()

        if text.startswith("```") and text.endswith("```"):
            lines = text.splitlines()

            if lines and lines[0].strip().lower() in (
                "```",
                "```json",
            ):
                lines = lines[1:]

            if lines and lines[-1].strip() == "```":
                lines = lines[:-1]

            text = "\n".join(lines).strip()

        return text

    @staticmethod
    def _extract_content(response: Any) -> str:
        try:
            choice = response["choices"][0]
            message = choice.get("message", {})
            content = message.get("content")

            if content is None:
                content = choice.get("text")

        except (KeyError, IndexError, TypeError) as exc:
            raise ContractError(
                "Malformed Qwen response."
            ) from exc

        if not isinstance(content, str) or not content.strip():
            raise ContractError(
                "Qwen returned empty content."
            )

        return content.strip()

    def to_action(self, validated: dict) -> Action | None:
        if validated.get("clarification"):
            return None

        return Action(
            intent=validated["intent"],
            parameters=validated["parameters"],
        )
