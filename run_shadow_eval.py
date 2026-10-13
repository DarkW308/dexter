"""Run a live shadow evaluation session on Dexter with representative user queries.

Evaluates:
- Creation commands
- List pending queries
- Completion commands vs User completion reports
- Negation commands
- Ambiguous commands
- Out of domain queries

Logs side-by-side telemetry to logs/shadow_eval.jsonl without altering live database state.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from shadow_interpreter import ShadowInterpreter
from needle_adapter import NeedleAdapter


class PrimaryDeterministicFallback:
    """Represents Dexter's primary baseline interpreter."""
    def interpret(self, text: str, request_id: str = "local") -> dict:
        import re
        normalized = text.lower().strip()
        
        if re.search(r"\b(?:don't|do not)\s+(?:complete|finish)\b", normalized):
            return {"contract_version": "1", "request_id": request_id, "clarification": True, "intent": None, "parameters": {}, "confidence": 0.99}
            
        if re.search(r"\b(pending|unfinished)\s+tasks\b", normalized):
            return {"contract_version": "1", "request_id": request_id, "clarification": False, "intent": "LIST_PENDING_TASKS", "parameters": {}, "confidence": 0.99}
            
        m = re.search(r"create\s+(?:a\s+)?task(?:\s+to)?\s+(.+)$", normalized)
        if m:
            title = m.group(1).strip().capitalize()
            return {"contract_version": "1", "request_id": request_id, "clarification": False, "intent": "CREATE_TASK", "parameters": {"title": title}, "confidence": 0.99}
            
        m = re.search(r"i\s+(?:already\s+)?(?:finished|completed)\s+task\s+(\d+)", normalized)
        if m:
            return {"contract_version": "1", "request_id": request_id, "clarification": False, "intent": "USER_REPORT_TASK_COMPLETION", "parameters": {"task_id": int(m.group(1))}, "confidence": 0.99}
            
        m = re.search(r"(?:complete|finish|mark)\s+task\s+(\d+)", normalized)
        if m:
            return {"contract_version": "1", "request_id": request_id, "clarification": False, "intent": "COMPLETE_TASK", "parameters": {"task_id": int(m.group(1))}, "confidence": 0.99}

        return {"contract_version": "1", "request_id": request_id, "clarification": True, "intent": None, "parameters": {}, "confidence": 0.80}


def run_session():
    primary = PrimaryDeterministicFallback()
    needle = NeedleAdapter(base_url="http://127.0.0.1:8081", confidence_threshold=0.50)
    shadow_log = Path("logs/shadow_eval.jsonl")
    
    # Clean previous evaluation log
    if shadow_log.exists():
        shadow_log.unlink()
        
    shadow_engine = ShadowInterpreter(primary_interpreter=primary, needle_adapter=needle, log_file=shadow_log)
    
    test_queries = [
        "Create a task to buy groceries",
        "Show my unfinished tasks",
        "Mark task 4 complete",
        "I already finished task 4",
        "Cancel task 7",
        "Do the thing",
        "What's the weather today?",
        "Don't complete task 4",
        "Could you add buying milk to my list?",
        "I took care of task 4",
    ]
    
    print("=" * 65)
    print("RUNNING LIVE SHADOW EVALUATION SESSION")
    print("=" * 65)
    
    for i, q in enumerate(test_queries, 1):
        req_id = f"eval-{i:02d}"
        res = shadow_engine.interpret(q, request_id=req_id)
        print(f"[{req_id}] Query: '{q}' -> Primary Intent: {res.get('intent', 'CLARIFICATION')}")

    print("\n--- SUMMARY OF SHADOW EVALUATION LOG ---")
    lines = shadow_log.read_text(encoding="utf-8").strip().splitlines()
    total = len(lines)
    agreed = 0
    total_needle_latency = 0.0
    
    for line in lines:
        data = json.loads(line)
        if data["agreement"]:
            agreed += 1
        total_needle_latency += data["shadow_needle"]["latency_s"]
        
    avg_latency = total_needle_latency / total if total > 0 else 0.0
    print(f"Total Queries Evaluated: {total}")
    print(f"Intent & Parameter Agreement Rate: {agreed}/{total} ({agreed/total*100:.1f}%)")
    print(f"Average Needle 3 Shadow Latency: {avg_latency:.3f}s")
    print(f"Executed Strictly by Primary: 100% ({total}/{total})")
    print("=" * 65)


if __name__ == "__main__":
    run_session()
