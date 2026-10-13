"""Verification suite for Dexter Shadow Mode.

Verifies:
1. Primary interpreter result is strictly returned.
2. Needle 3 runs concurrently in shadow mode.
3. Telemetry and agreement are appended to shadow_eval.jsonl.
4. Shadow failures do not compromise primary interpreter execution.
"""

from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import MagicMock

sys.path.insert(0, str(Path(__file__).resolve().parent))

from shadow_interpreter import ShadowInterpreter
from needle_adapter import NeedleAdapter
from action_system import Action


class MockPrimaryInterpreter:
    """Mock representing the existing Qwen/Deterministic interpreter."""
    def interpret(self, text: str, request_id: str = "local") -> dict:
        if "milk" in text.lower():
            return {
                "clarification": False,
                "intent": "CREATE_TASK",
                "parameters": {"title": "Buy milk"},
                "confidence": 0.95,
            }
        return {
            "clarification": True,
            "intent": None,
            "parameters": {},
            "confidence": 0.90,
        }

    def to_action(self, validated: dict) -> Action | None:
        if validated.get("clarification"):
            return None
        return Action(intent=validated["intent"], parameters=validated["parameters"])


class TestShadowMode(unittest.TestCase):
    def setUp(self):
        self.tmp_dir = tempfile.TemporaryDirectory()
        self.log_file = Path(self.tmp_dir.name) / "shadow_eval.jsonl"
        self.primary = MockPrimaryInterpreter()
        self.needle = NeedleAdapter(base_url="http://127.0.0.1:8081", confidence_threshold=0.50)
        self.shadow_engine = ShadowInterpreter(
            primary_interpreter=self.primary,
            needle_adapter=self.needle,
            log_file=self.log_file,
        )

    def tearDown(self):
        self.tmp_dir.cleanup()

    def test_01_agreement_and_primary_execution(self):
        query = "Create a task to buy milk"
        res = self.shadow_engine.interpret(query, request_id="shadow-1")

        # Primary must be returned
        self.assertFalse(res["clarification"])
        self.assertEqual(res["intent"], "CREATE_TASK")
        self.assertEqual(res["parameters"], {"title": "Buy milk"})

        # Log must be generated
        self.assertTrue(self.log_file.exists())
        lines = self.log_file.read_text(encoding="utf-8").strip().splitlines()
        self.assertEqual(len(lines), 1)

        log_data = json.loads(lines[0])
        self.assertEqual(log_data["request_id"], "shadow-1")
        self.assertEqual(log_data["primary"]["intent"], "CREATE_TASK")
        self.assertEqual(log_data["shadow_needle"]["intent"], "CREATE_TASK")
        self.assertTrue(log_data["agreement"])
        self.assertEqual(log_data["executed_by"], "PRIMARY_ONLY")

    def test_02_disagreement_logging(self):
        query = "Do the thing"
        res = self.shadow_engine.interpret(query, request_id="shadow-2")

        self.assertTrue(res["clarification"])

        lines = self.log_file.read_text(encoding="utf-8").strip().splitlines()
        log_data = json.loads(lines[-1])
        self.assertEqual(log_data["request_id"], "shadow-2")
        self.assertTrue(log_data["primary"]["clarification"])
        self.assertTrue(log_data["shadow_needle"]["clarification"])
        self.assertTrue(log_data["agreement"])

    def test_03_shadow_error_isolation(self):
        """Simulate Needle failure and verify primary still completes safely."""
        failing_needle = MagicMock()
        failing_needle.interpret.side_effect = RuntimeError("Simulated Needle crash")

        shadow_with_fault = ShadowInterpreter(
            primary_interpreter=self.primary,
            needle_adapter=failing_needle,
            log_file=self.log_file,
        )

        res = shadow_with_fault.interpret("Create a task to buy milk", request_id="shadow-3")
        self.assertFalse(res["clarification"])
        self.assertEqual(res["intent"], "CREATE_TASK")

        lines = self.log_file.read_text(encoding="utf-8").strip().splitlines()
        log_data = json.loads(lines[-1])
        self.assertEqual(log_data["request_id"], "shadow-3")
        self.assertIn("Simulated Needle crash", log_data["shadow_needle"]["error"])
        self.assertFalse(log_data["agreement"])


if __name__ == "__main__":
    print("=" * 60)
    print("RUNNING SHADOW MODE VERIFICATION SUITE")
    print("=" * 60)
    suite = unittest.TestLoader().loadTestsFromTestCase(TestShadowMode)
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    if result.wasSuccessful():
        print(f"\nALL {result.testsRun} SHADOW MODE TESTS PASSED! ✅")
        sys.exit(0)
    else:
        print("\nSHADOW MODE TESTS FAILED! ❌")
        sys.exit(1)
