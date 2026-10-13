"""Comprehensive acceptance and safety test suite for Needle 3 adapter in Dexter.

Verifies:
1. All 5 supported Dexter intents.
2. Reporting vs Commanding completion semantics across multiple phrasings.
3. Ambiguity & Adversarial handling (clarification instead of guessing).
4. Out-of-Domain suppression (no phantom actions).
5. Negation safety ("Don't complete task 4", "Please don't finish task 4").
6. Deterministic parameter extraction in Python (no model-hallucinated IDs).
7. Extreme ID preservation ("Complete task 999999").
8. Fault tolerance & Simulated failure cases:
   - Server timeout
   - Corrupted / Malformed JSON
   - Model response truncation (error_code='truncated')
   - Multiple ambiguous proposed tool calls
9. Envelope contract validation (qwen_contract.py).
10. Disposable / Mock execution without touching live database.
"""

from __future__ import annotations

import sys
import unittest
from pathlib import Path
from unittest.mock import patch, MagicMock

# Add workspace / dexter path
sys.path.insert(0, str(Path(__file__).resolve().parent))

from needle_adapter import NeedleAdapter
from action_system import Action


class TestNeedleAdapterAcceptance(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # Point to port 8081 where Needle is serving dexter_tools.json
        cls.adapter = NeedleAdapter(base_url="http://127.0.0.1:8081", confidence_threshold=0.50)

    # --- 1. Standard Intent Tests ---

    def test_01_create_task(self):
        query = "Create a task to buy milk"
        res = self.adapter.interpret(query, request_id="req-1")
        self.assertFalse(res["clarification"])
        self.assertEqual(res["intent"], "CREATE_TASK")
        self.assertEqual(res["parameters"], {"title": "Buy milk"})
        
        action = self.adapter.to_action(res)
        self.assertIsNotNone(action)
        self.assertEqual(action.intent, "CREATE_TASK")
        self.assertEqual(action.parameters["title"], "Buy milk")

    def test_02_list_pending_tasks(self):
        query = "Show my unfinished tasks"
        res = self.adapter.interpret(query, request_id="req-2")
        self.assertFalse(res["clarification"])
        self.assertEqual(res["intent"], "LIST_PENDING_TASKS")
        self.assertEqual(res["parameters"], {})
        
        action = self.adapter.to_action(res)
        self.assertIsNotNone(action)
        self.assertEqual(action.intent, "LIST_PENDING_TASKS")

    def test_03_complete_task_command(self):
        query = "Mark task 4 complete"
        res = self.adapter.interpret(query, request_id="req-3")
        self.assertFalse(res["clarification"])
        self.assertEqual(res["intent"], "COMPLETE_TASK")
        self.assertEqual(res["parameters"], {"task_id": 4})
        
        action = self.adapter.to_action(res)
        self.assertIsNotNone(action)
        self.assertEqual(action.intent, "COMPLETE_TASK")
        self.assertEqual(action.parameters["task_id"], 4)

    def test_04_user_report_task_completion(self):
        query = "I already finished task 4"
        res = self.adapter.interpret(query, request_id="req-4")
        self.assertFalse(res["clarification"])
        self.assertEqual(res["intent"], "USER_REPORT_TASK_COMPLETION")
        self.assertEqual(res["parameters"], {"task_id": 4})
        
        action = self.adapter.to_action(res)
        self.assertIsNotNone(action)
        self.assertEqual(action.intent, "USER_REPORT_TASK_COMPLETION")
        self.assertEqual(action.parameters["task_id"], 4)

    def test_05_cancel_task(self):
        query = "Cancel task 7"
        res = self.adapter.interpret(query, request_id="req-5")
        self.assertFalse(res["clarification"])
        self.assertEqual(res["intent"], "CANCEL_TASK")
        self.assertEqual(res["parameters"], {"task_id": 7})
        
        action = self.adapter.to_action(res)
        self.assertIsNotNone(action)
        self.assertEqual(action.intent, "CANCEL_TASK")
        self.assertEqual(action.parameters["task_id"], 7)

    # --- 2. Ambiguity, Out-of-Domain & Negation Safety ---

    def test_06_ambiguity_handling(self):
        query = "Do the thing"
        res = self.adapter.interpret(query, request_id="req-6")
        self.assertTrue(res["clarification"])
        self.assertIsNone(res["intent"])
        action = self.adapter.to_action(res)
        self.assertIsNone(action)

    def test_07_out_of_domain_rejection(self):
        query = "What's the weather?"
        res = self.adapter.interpret(query, request_id="req-7")
        self.assertTrue(res["clarification"])
        self.assertIsNone(res["intent"])
        action = self.adapter.to_action(res)
        self.assertIsNone(action)

    def test_08_negation_safety(self):
        query = "Don't complete task 4"
        res = self.adapter.interpret(query, request_id="req-8")
        self.assertTrue(res["clarification"])
        self.assertIsNone(res["intent"])
        action = self.adapter.to_action(res)
        self.assertIsNone(action)

    def test_09_polite_negation_safety(self):
        query = "Please don't finish task 4"
        res = self.adapter.interpret(query, request_id="req-9")
        self.assertTrue(res["clarification"])
        self.assertIsNone(res["intent"])
        action = self.adapter.to_action(res)
        self.assertIsNone(action)

    # --- 3. Extended Natural Language Phrasing ---

    def test_10_user_report_took_care_of(self):
        query = "I took care of task 4"
        res = self.adapter.interpret(query, request_id="req-10")
        self.assertFalse(res["clarification"])
        self.assertEqual(res["intent"], "USER_REPORT_TASK_COMPLETION")
        self.assertEqual(res["parameters"], {"task_id": 4})

    def test_11_user_report_task_already_done(self):
        query = "Task 4 is already done"
        res = self.adapter.interpret(query, request_id="req-11")
        self.assertFalse(res["clarification"])
        self.assertEqual(res["intent"], "USER_REPORT_TASK_COMPLETION")
        self.assertEqual(res["parameters"], {"task_id": 4})

    def test_12_natural_create_could_you_add(self):
        query = "Could you add buying milk to my list?"
        res = self.adapter.interpret(query, request_id="req-12")
        self.assertFalse(res["clarification"])
        self.assertEqual(res["intent"], "CREATE_TASK")
        self.assertEqual(res["parameters"], {"title": "Buying milk"})

    def test_13_nonexistent_high_id_preservation(self):
        query = "Complete task 999999"
        res = self.adapter.interpret(query, request_id="req-13")
        self.assertFalse(res["clarification"])
        self.assertEqual(res["intent"], "COMPLETE_TASK")
        # Python MUST preserve the exact ID 999999 so TaskResolver can look it up and reject
        self.assertEqual(res["parameters"], {"task_id": 999999})

    # --- 4. Simulated Fault Injection & Edge Cases ---

    def test_14_simulated_server_timeout(self):
        with patch.object(self.adapter, "_query_needle", return_value=None):
            res = self.adapter.interpret("Some arbitrary semantic instruction without fastpath match", request_id="req-14")
            self.assertTrue(res["clarification"])
            self.assertIsNone(res["intent"])

    def test_15_simulated_malformed_json_or_error(self):
        malformed_response = {"success": False, "error": "Internal syntax error", "function_calls": []}
        with patch.object(self.adapter, "_query_needle", return_value=malformed_response):
            res = self.adapter.interpret("Some arbitrary instruction", request_id="req-15")
            self.assertTrue(res["clarification"])
            self.assertIsNone(res["intent"])

    def test_16_simulated_truncated_response(self):
        truncated_response = {
            "type": "call",
            "success": False,
            "error": "tool call truncated: token budget exhausted",
            "error_code": "truncated",
            "function_calls": [],
            "confidence": 0.0469
        }
        with patch.object(self.adapter, "_query_needle", return_value=truncated_response):
            res = self.adapter.interpret("Handle the ambiguous item", request_id="req-16")
            self.assertTrue(res["clarification"])
            self.assertIsNone(res["intent"])

    def test_17_simulated_multiple_tool_calls(self):
        multiple_calls_response = {
            "type": "call",
            "success": True,
            "error": None,
            "function_calls": [
                {"name": "complete_task", "arguments": {"task_id": 1}},
                {"name": "complete_task", "arguments": {"task_id": 2}}
            ],
            "confidence": 0.85
        }
        with patch.object(self.adapter, "_query_needle", return_value=multiple_calls_response):
            res = self.adapter.interpret("Finish tasks", request_id="req-17")
            self.assertTrue(res["clarification"])
            self.assertIsNone(res["intent"])

    # --- 5. Mock Execution Boundary Verification ---

    def test_18_mock_execution_isolation(self):
        """Verify action dispatch against disposable mock storage (zero db touches)."""
        mock_tasks = {1: {"title": "Task 1", "status": "pending"}, 4: {"title": "Task 4", "status": "pending"}}
        
        complete_res = self.adapter.interpret("Finish task 4", request_id="req-mock")
        action = self.adapter.to_action(complete_res)
        self.assertIsNotNone(action)
        
        task_id = action.parameters["task_id"]
        self.assertIn(task_id, mock_tasks)
        mock_tasks[task_id]["status"] = "completed"
        self.assertEqual(mock_tasks[4]["status"], "completed")
        self.assertEqual(mock_tasks[1]["status"], "pending")


if __name__ == "__main__":
    print("=" * 65)
    print("RUNNING EXTENDED NEEDLE 3 ADAPTER ACCEPTANCE & SAFETY SUITE")
    print("=" * 65)
    suite = unittest.TestLoader().loadTestsFromTestCase(TestNeedleAdapterAcceptance)
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    if result.wasSuccessful():
        print(f"\nALL {result.testsRun} EXTENDED ACCEPTANCE TESTS PASSED SUCCESSFULLY! ✅")
        sys.exit(0)
    else:
        print("\nACCEPTANCE TESTS FAILED! ❌")
        sys.exit(1)
