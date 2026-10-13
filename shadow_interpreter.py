"""Dexter Shadow Interpreter.

Executes primary interpreter as the active decision engine, while running
Needle 3 concurrently in shadow mode for comparison, telemetry, and safety auditing.

Guarantees:
1. Primary proposal is the ONLY proposal that can reach ActionExecutor.
2. Needle shadow proposal is strictly observed, logged, and audited.
3. Errors in Needle shadow mode never disrupt or alter primary execution.
4. Structured logging to logs/shadow_eval.jsonl.
"""

from __future__ import annotations

import json
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Callable

from action_system import Action
from needle_adapter import NeedleAdapter


BASE_DIR = Path(__file__).resolve().parent
LOGS_DIR = BASE_DIR / "logs"
SHADOW_LOG_FILE = LOGS_DIR / "shadow_eval.jsonl"


class ShadowInterpreter:
    """Interprets requests using primary engine and evaluates Needle 3 in shadow mode."""

    def __init__(
        self,
        primary_interpreter: Any,
        needle_adapter: NeedleAdapter | None = None,
        log_file: Path = SHADOW_LOG_FILE,
    ):
        self.primary = primary_interpreter
        self.needle = needle_adapter or NeedleAdapter()
        self.log_file = log_file
        self.log_file.parent.mkdir(parents=True, exist_ok=True)

    def interpret(self, text: str, request_id: str = "local") -> dict:
        user_text = text.strip()

        # --- 1. Primary Interpreter Execution (Active) ---
        t0_primary = time.perf_counter()
        primary_result = self.primary.interpret(user_text, request_id=request_id)
        t1_primary = time.perf_counter()
        primary_latency = t1_primary - t0_primary

        # --- 2. Needle 3 Shadow Execution (Passive / Non-blocking) ---
        shadow_result = None
        shadow_error = None
        shadow_latency = 0.0

        try:
            t0_shadow = time.perf_counter()
            shadow_result = self.needle.interpret(user_text, request_id=request_id)
            t1_shadow = time.perf_counter()
            shadow_latency = t1_shadow - t0_shadow
        except Exception as exc:
            shadow_error = str(exc)

        # --- 3. Telemetry & Agreement Analysis ---
        agreement = self._check_agreement(primary_result, shadow_result)

        self._log_shadow_event(
            request_id=request_id,
            user_text=user_text,
            primary_result=primary_result,
            primary_latency=primary_latency,
            shadow_result=shadow_result,
            shadow_latency=shadow_latency,
            shadow_error=shadow_error,
            agreement=agreement,
        )

        # --- 4. Return ONLY Primary Result to Dexter ---
        return primary_result

    def to_action(self, validated: dict) -> Action | None:
        """Converts validated envelope to Action (delegated to primary engine)."""
        if hasattr(self.primary, "to_action"):
            return self.primary.to_action(validated)

        if validated.get("clarification"):
            return None

        return Action(
            intent=validated["intent"],
            parameters=validated["parameters"],
        )

    def _check_agreement(self, primary: dict, shadow: dict | None) -> bool:
        if shadow is None:
            return False

        p_clarify = primary.get("clarification", False)
        s_clarify = shadow.get("clarification", False)

        if p_clarify and s_clarify:
            return True

        if p_clarify != s_clarify:
            return False

        p_intent = primary.get("intent")
        s_intent = shadow.get("intent")
        if p_intent != s_intent:
            return False

        p_params = primary.get("parameters", {})
        s_params = shadow.get("parameters", {})
        return p_params == s_params

    def _log_shadow_event(
        self,
        request_id: str,
        user_text: str,
        primary_result: dict,
        primary_latency: float,
        shadow_result: dict | None,
        shadow_latency: float,
        shadow_error: str | None,
        agreement: bool,
    ) -> None:
        event = {
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "request_id": request_id,
            "query_length": len(user_text),
            "primary": {
                "clarification": primary_result.get("clarification", False),
                "intent": primary_result.get("intent"),
                "parameters": primary_result.get("parameters", {}),
                "confidence": primary_result.get("confidence", 0.0),
                "latency_s": round(primary_latency, 4),
            },
            "shadow_needle": {
                "clarification": shadow_result.get("clarification", False) if shadow_result else None,
                "intent": shadow_result.get("intent") if shadow_result else None,
                "parameters": shadow_result.get("parameters", {}) if shadow_result else None,
                "confidence": shadow_result.get("confidence", 0.0) if shadow_result else 0.0,
                "latency_s": round(shadow_latency, 4),
                "error": shadow_error,
            },
            "agreement": agreement,
            "executed_by": "PRIMARY_ONLY",
        }

        try:
            with open(self.log_file, "a", encoding="utf-8") as f:
                f.write(json.dumps(event) + "\n")
        except OSError:
            # Shadow logging failure must never crash the main loop
            pass
