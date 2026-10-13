import time
import sys
import json
from ollama_chat import OllamaChatModel
from qwen_adapter import QwenAdapter

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
    ("I've done task 4", "COMPLETE_TASK"),
    ("Take care of task number 4", "COMPLETE_TASK"),
    ("I already finished task 4", "COMPLETE_TASK"),
    
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

def run_tests():
    print("Loading Qwen 0.8B for Acceptance Testing...")
    llm = OllamaChatModel(model='qwen3.5:0.8b')
    adapter = QwenAdapter(llm=llm)
    
    passed = 0
    failed = []
    
    print(f"\nRunning {len(TEST_CASES)} natural language tests against the real model...\n")
    
    for i, (phrase, expected) in enumerate(TEST_CASES):
        try:
            start = time.time()
            res = adapter.interpret(phrase, request_id=f"test_{i}")
            
            # If clarification is True, the intent is essentially CLARIFICATION
            intent = "CLARIFICATION" if res.get("clarification") else res.get("intent", "UNKNOWN")
            
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
            
    print(f"\n=== ACCEPTANCE TEST RESULTS ===")
    print(f"Passed: {passed}/{len(TEST_CASES)}")
    if failed:
        print("\nFailures:")
        for phrase, exp, got in failed:
            print(f"  - {phrase!r}\n    Expected: {exp}\n    Got: {got}\n")
    
    # Write report to file
    with open('acceptance_test_report.txt', 'w') as f:
        f.write(f"Passed: {passed}/{len(TEST_CASES)}\n")
        if failed:
            f.write("Failures:\n")
            for phrase, exp, got in failed:
                f.write(f"  - {phrase!r} | Expected: {exp} | Got: {got}\n")

if __name__ == '__main__':
    run_tests()
