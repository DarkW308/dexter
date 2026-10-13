import json
import time

TEST_CASES = [
    # CREATE_TASK
    ("Create a task to buy milk", "CREATE_TASK"),
    ("Put buy milk on my list", "CREATE_TASK"),
    ("I need to remember to buy milk", "CREATE_TASK"),
    ("Add a task called buy milk", "CREATE_TASK"),
    
    # LIST_PENDING_TASKS
    ("Show my pending tasks", "LIST_PENDING_TASKS"),
    ("What do I still have left?", "LIST_PENDING_TASKS"),
    ("What's on my todo list?", "LIST_PENDING_TASKS"),
    ("What haven't I finished?", "LIST_PENDING_TASKS"),
    
    # COMPLETE_TASK
    ("Finish task 4", "COMPLETE_TASK"),
    ("Mark task 4 complete", "COMPLETE_TASK"),
    ("Take care of task number 4", "COMPLETE_TASK"),
    
    # USER_REPORT_TASK_COMPLETION
    ("I've done task 4", "USER_REPORT_TASK_COMPLETION"),
    ("I already finished task 4", "USER_REPORT_TASK_COMPLETION"),
    
    # CANCEL_TASK
    ("Cancel task 7", "CANCEL_TASK"),
    ("Remove task 7", "CANCEL_TASK"),
    ("Delete task number 7", "CANCEL_TASK"),
    
    # CLARIFICATION
    ("Do the thing", "CLARIFICATION"),
    ("Handle that", "CLARIFICATION"),
    ("Never mind", "CLARIFICATION"),
    ("What's the weather?", "CLARIFICATION"),
    ("Tell me a joke", "CLARIFICATION")
]

script_code = '''
import time
from qwen_adapter import QwenAdapter

def run_tests():
    print("Loading Qwen 0.8B for Acceptance Testing...")
    adapter = QwenAdapter()
    
    test_cases = {cases}
    
    passed = 0
    failed = []
    
    print("\\nRunning", len(test_cases), "natural language tests...\\n")
    
    for i, (phrase, expected) in enumerate(test_cases):
        try:
            # We call the model to get the intent
            start = time.time()
            intent = adapter.get_intent(phrase, request_id=f"test_{i}")
            duration = time.time() - start
            
            if intent == expected:
                print(f"[PASS] {phrase!r} -> {intent} ({duration:.2f}s)")
                passed += 1
            else:
                print(f"[FAIL] {phrase!r} -> Expected: {expected}, Got: {intent} ({duration:.2f}s)")
                failed.append((phrase, expected, intent))
        except Exception as e:
            print(f"[ERROR] {phrase!r} -> Failed to process: {e}")
            failed.append((phrase, expected, str(e)))
            
    print(f"\\n=== ACCEPTANCE TEST RESULTS ===")
    print(f"Passed: {passed}/{len(test_cases)}")
    if failed:
        print("\\nFailures:")
        for phrase, exp, got in failed:
            print(f"  - {phrase!r}\\n    Expected: {exp}\\n    Got: {got}\\n")

if __name__ == "__main__":
    run_tests()
'''

with open('acceptance_test.py', 'w') as f:
    f.write(script_code.replace('{cases}', str(TEST_CASES)))
