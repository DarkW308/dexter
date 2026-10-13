"""Deterministic validation for Qwen -> Dexter action envelopes."""

from __future__ import annotations
from typing import Any

ALLOWED = {
    "CREATE_TASK": {"title"},
    "LIST_PENDING_TASKS": set(),
    "COMPLETE_TASK": {"task_id"},
    "USER_REPORT_TASK_COMPLETION": {"task_id"},
    "CANCEL_TASK": {"task_id"},
}
OPTIONAL = {
    "CREATE_TASK": {"priority"},
    "LIST_PENDING_TASKS": set(),
    "COMPLETE_TASK": set(),
    "USER_REPORT_TASK_COMPLETION": set(),
    "CANCEL_TASK": set(),
}

class ContractError(ValueError):
    pass

def validate_envelope(payload: Any) -> dict:
    if not isinstance(payload, dict):
        raise ContractError("Envelope must be an object.")
    if payload.get("contract_version") != "1":
        raise ContractError("Unsupported contract_version.")

    clarification = payload.get("needs_clarification", False)
    if not isinstance(clarification, bool):
        raise ContractError("needs_clarification must be boolean.")

    confidence = payload.get("confidence")
    if not isinstance(confidence, (int, float)) or isinstance(confidence, bool):
        raise ContractError("confidence must be numeric.")
    if not 0.0 <= float(confidence) <= 1.0:
        raise ContractError("confidence must be between 0 and 1.")

    action = payload.get("action")
    if clarification:
        if action is not None:
            raise ContractError("Clarification requests must set action to null.")
        return {
            "clarification": True,
            "intent": None,
            "parameters": {},
            "confidence": float(confidence),
        }

    if not isinstance(action, dict):
        raise ContractError("action must be an object.")

    intent = action.get("intent")
    params = action.get("parameters")
    if intent not in ALLOWED:
        raise ContractError(f"Unknown intent: {intent!r}")
    if not isinstance(params, dict):
        raise ContractError("action.parameters must be an object.")

    allowed = ALLOWED[intent] | OPTIONAL[intent]
    unknown = set(params) - allowed
    if unknown:
        raise ContractError(f"Unknown parameters: {sorted(unknown)}")

    missing = ALLOWED[intent] - set(params)
    if missing:
        raise ContractError(f"Missing parameters: {sorted(missing)}")

    if "task_id" in params:
        if isinstance(params["task_id"], bool) or not isinstance(params["task_id"], int):
            raise ContractError("task_id must be an integer.")
        if params["task_id"] <= 0:
            raise ContractError("task_id must be positive.")

    if "title" in params:
        if not isinstance(params["title"], str) or not params["title"].strip():
            raise ContractError("title must be a non-empty string.")

    return {
        "clarification": False,
        "intent": intent,
        "parameters": params,
        "confidence": float(confidence),
    }
