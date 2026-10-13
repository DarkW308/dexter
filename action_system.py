"""Dexter Core Action System v1.

Models may propose structured actions. This module validates and executes only
allow-listed actions through deterministic domain services. It never executes
raw SQL supplied by a model.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
import json
import re
from typing import Any, Mapping

import database


ALLOWED_INTENTS = {
    "LIST_PENDING_TASKS",
    "CREATE_TASK",
    "COMPLETE_TASK",
    "USER_REPORT_TASK_COMPLETION",
    "CANCEL_TASK",
}

PRIORITIES = {"CRITICAL", "HIGH", "NORMAL", "LOW", "SOMEDAY"}
ACTORS = {"USER", "DEXTER", "SYSTEM", "AUTOMATION"}


class ActionError(Exception):
    """Base class for expected action failures."""


class ActionValidationError(ActionError):
    pass


class PermissionDenied(ActionError):
    pass


class ConfirmationRequired(ActionError):
    pass


class NotFound(ActionError):
    pass


@dataclass(frozen=True)
class Action:
    intent: str
    parameters: Mapping[str, Any] = field(default_factory=dict)
    actor_type: str = "USER"
    confirmed: bool = False
    request_id: str | None = None

    @classmethod
    def from_mapping(cls, value: Mapping[str, Any]) -> "Action":
        if not isinstance(value, Mapping):
            raise ActionValidationError("Action must be an object.")
        return cls(
            intent=str(value.get("intent", "")).strip().upper(),
            parameters=value.get("parameters") or {},
            actor_type=str(value.get("actor_type", "USER")).strip().upper(),
            confirmed=bool(value.get("confirmed", False)),
            request_id=value.get("request_id"),
        )


@dataclass(frozen=True)
class ActionResult:
    success: bool
    intent: str
    message: str
    entity_type: str | None = None
    entity_id: int | None = None
    data: Mapping[str, Any] = field(default_factory=dict)
    error_code: str | None = None

    def to_dict(self) -> dict[str, Any]:
        return {
            "success": self.success,
            "intent": self.intent,
            "message": self.message,
            "entity_type": self.entity_type,
            "entity_id": self.entity_id,
            "data": dict(self.data),
            "error_code": self.error_code,
        }


def now_ts() -> int:
    return int(datetime.now(timezone.utc).timestamp())


def _require_text(params: Mapping[str, Any], name: str, max_len: int = 500) -> str:
    value = params.get(name)
    if not isinstance(value, str) or not value.strip():
        raise ActionValidationError(f"{name} is required.")
    value = value.strip()
    if len(value) > max_len:
        raise ActionValidationError(f"{name} is too long.")
    return value


def _require_int(params: Mapping[str, Any], name: str) -> int:
    value = params.get(name)
    if isinstance(value, bool):
        raise ActionValidationError(f"{name} must be an integer.")
    try:
        return int(value)
    except (TypeError, ValueError):
        raise ActionValidationError(f"{name} must be an integer.") from None


def validate_action(action: Action) -> None:
    if action.intent not in ALLOWED_INTENTS:
        raise ActionValidationError(f"Unsupported intent: {action.intent}")
    if action.actor_type not in ACTORS:
        raise ActionValidationError(f"Unsupported actor type: {action.actor_type}")
    if not isinstance(action.parameters, Mapping):
        raise ActionValidationError("parameters must be an object.")

    if action.intent == "CREATE_TASK":
        _require_text(action.parameters, "title")
        priority = str(action.parameters.get("priority", "NORMAL")).upper()
        if priority not in PRIORITIES:
            raise ActionValidationError(f"Invalid priority: {priority}")
        if "project_id" in action.parameters and action.parameters["project_id"] is not None:
            _require_int(action.parameters, "project_id")
        for key in ("due_at", "scheduled_start", "scheduled_end", "estimated_duration_seconds"):
            if key in action.parameters and action.parameters[key] is not None:
                _require_int(action.parameters, key)

    elif action.intent in {"COMPLETE_TASK", "USER_REPORT_TASK_COMPLETION", "CANCEL_TASK"}:
        _require_int(action.parameters, "task_id")

    # Deletion is intentionally not an action in v1. Prefer archive/cancel
    # semantics until a separate destructive-action confirmation contract exists.


def _audit(conn, *, action: Action, result: ActionResult, before: Any = None, after: Any = None) -> None:
    ts = now_ts()
    conn.execute(
        """
        INSERT INTO audit_logs(
            actor_type, action_type, entity_type, entity_id,
            before_json, after_json, reason, source_type, source_id, created_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        (
            action.actor_type,
            action.intent,
            result.entity_type or "SYSTEM",
            result.entity_id,
            json.dumps(before, default=str) if before is not None else None,
            json.dumps(after, default=str) if after is not None else None,
            "Dexter Core action execution",
            "REQUEST",
            None,
            ts,
        ),
    )
    conn.execute(
        """
        INSERT INTO action_traces(
            action_type, actor_type, entity_type, entity_id,
            trigger_type, trigger_id, reason, source_type, source_id, result, created_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        (
            action.intent,
            action.actor_type,
            result.entity_type,
            result.entity_id,
            "VOICE_OR_TEXT",
            None,
            "Validated Dexter Core action",
            "REQUEST",
            None,
            "SUCCESS" if result.success else (result.error_code or "FAILED"),
            ts,
        ),
    )


class TaskService:
    """Deterministic task mutations and reads."""

    @staticmethod
    def list_pending() -> ActionResult:
        conn = database.connect()
        try:
            rows = conn.execute(
                """
                SELECT t.id, t.title, t.priority, o.due_at, o.scheduled_start, o.status
                FROM tasks t
                LEFT JOIN task_occurrences o ON o.task_id = t.id
                WHERE t.archived_at IS NULL
                  AND (o.id IS NULL OR o.status IN ('PENDING','IN_PROGRESS','BLOCKED'))
                ORDER BY CASE t.priority
                    WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1
                    WHEN 'NORMAL' THEN 2 WHEN 'LOW' THEN 3 ELSE 4 END,
                    COALESCE(o.due_at, o.scheduled_start, 9223372036854775807)
                LIMIT 50
                """
            ).fetchall()
            tasks = [dict(r) for r in rows]
            return ActionResult(
                True, "LIST_PENDING_TASKS",
                f"You have {len(tasks)} pending tasks." if tasks else "You have no pending tasks.",
                entity_type="TASK",
                data={"tasks": tasks, "count": len(tasks)},
            )
        finally:
            conn.close()

    @staticmethod
    def create(action: Action) -> ActionResult:
        p = action.parameters
        title = _require_text(p, "title")
        description = p.get("description")
        if description is not None and not isinstance(description, str):
            raise ActionValidationError("description must be text.")
        priority = str(p.get("priority", "NORMAL")).upper()
        project_id = p.get("project_id")
        due_at = p.get("due_at")
        scheduled_start = p.get("scheduled_start")
        scheduled_end = p.get("scheduled_end")
        duration = p.get("estimated_duration_seconds")
        ts = now_ts()

        conn = database.connect()
        try:
            with conn:
                if project_id is not None:
                    if conn.execute("SELECT 1 FROM projects WHERE id = ?", (int(project_id),)).fetchone() is None:
                        raise NotFound(f"Project {project_id} was not found.")
                cur = conn.execute(
                    """
                    INSERT INTO tasks(title, description, priority, project_id,
                                      estimated_duration_seconds, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    (title, description, priority, project_id, duration, ts, ts),
                )
                task_id = int(cur.lastrowid)

                # A task without timing is still valid and remains pending.
                if any(v is not None for v in (due_at, scheduled_start, scheduled_end)):
                    conn.execute(
                        """
                        INSERT INTO task_occurrences(
                            task_id, status, scheduled_start, scheduled_end, due_at,
                            created_at, updated_at
                        ) VALUES (?, 'PENDING', ?, ?, ?, ?, ?)
                        """,
                        (task_id, scheduled_start, scheduled_end, due_at, ts, ts),
                    )

                result = ActionResult(
                    True, action.intent,
                    f"Created task #{task_id}: '{title}'.",
                    entity_type="TASK",
                    entity_id=task_id,
                    data={"title": title, "priority": priority},
                )
                _audit(conn, action=action, result=result, after=result.data)
                return result
        finally:
            conn.close()

    @staticmethod
    def set_status(action: Action, status: str, completion_source: str | None = None) -> ActionResult:
        task_id = _require_int(action.parameters, "task_id")
        ts = now_ts()
        conn = database.connect()
        try:
            with conn:
                task = conn.execute(
                    "SELECT id, title, priority FROM tasks WHERE id = ? AND archived_at IS NULL",
                    (task_id,),
                ).fetchone()
                if not task:
                    raise NotFound(f"Task {task_id} was not found.")

                occ = conn.execute(
                    """
                    SELECT id, status, due_at, scheduled_start
                    FROM task_occurrences
                    WHERE task_id = ?
                      AND status IN ('PENDING','IN_PROGRESS','BLOCKED')
                    ORDER BY COALESCE(due_at, scheduled_start, 9223372036854775807) ASC, id DESC
                    LIMIT 1
                    """,
                    (task_id,),
                ).fetchone()

                if occ:
                    completed_at = ts if status == "COMPLETED" else None
                    conn.execute(
                        """
                        UPDATE task_occurrences
                        SET status = ?, completed_at = ?, completion_source = ?, updated_at = ?
                        WHERE id = ?
                        """,
                        (status, completed_at, completion_source, ts, occ["id"]),
                    )
                else:
                    conn.execute(
                        """
                        INSERT INTO task_occurrences(
                            task_id, status, completed_at, completion_source, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?)
                        """,
                        (task_id, status, ts if status == "COMPLETED" else None, completion_source, ts, ts),
                    )

                result = ActionResult(
                    True, action.intent,
                    (
                    f"Recorded that you completed task '{task['title']}'."
                    if completion_source == "USER_REPORTED"
                    else f"Marked task '{task['title']}' as {status.lower()}."
                ),
                    entity_type="TASK",
                    entity_id=task_id,
                    data={"status": status},
                )
                _audit(conn, action=action, result=result,
                       before={"status": occ["status"] if occ else None},
                       after={"status": status})
                return result
        finally:
            conn.close()


class ActionExecutor:
    """Validated action dispatcher with deterministic execution."""

    def execute(self, action: Action | Mapping[str, Any]) -> ActionResult:
        if not isinstance(action, Action):
            action = Action.from_mapping(action)
        validate_action(action)

        # Destructive deletion is deliberately excluded from v1.
        if action.intent == "LIST_PENDING_TASKS":
            return TaskService.list_pending()
        if action.intent == "CREATE_TASK":
            return TaskService.create(action)
        if action.intent == "COMPLETE_TASK":
            return TaskService.set_status(action, "COMPLETED", "DEXTER_RECORDED")
        if action.intent == "USER_REPORT_TASK_COMPLETION":
            return TaskService.set_status(action, "COMPLETED", "USER_REPORTED")
        if action.intent == "CANCEL_TASK":
            return TaskService.set_status(action, "CANCELLED")

        raise ActionValidationError(f"No executor for {action.intent}")


def result_to_json(result: ActionResult) -> str:
    return json.dumps(result.to_dict(), ensure_ascii=False)
