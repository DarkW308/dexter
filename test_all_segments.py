from needle_adapter import NeedleAdapter

adapter = NeedleAdapter()

test_queries = [
    ("Hey Dexter, what are my pending tasks?", "LIST_PENDING_TASKS"),
    ("Dexter, mark task 4 complete", "COMPLETE_TASK"),
    ("Mark all tasks complete", "COMPLETE_ALL_TASKS"),
    ("Mark oil task is completed.", "COMPLETE_ALL_TASKS"),
    ("Show completed tasks", "LIST_COMPLETED_TASKS"),
    ("Spent 500 on groceries", "LOG_EXPENSE"),
    ("Paid 1200 for electricity", "LOG_EXPENSE"),
    ("How much did I spend", "SUMMARIZE_EXPENSES"),
    ("Take a note: Meeting tomorrow at 10 AM", "LOG_JOURNAL"),
    ("Show notes", "LIST_JOURNAL"),
    ("Show reminders", "LIST_ALARMS"),
    ("Cancel all alarms", "CANCEL_ALARMS"),
    ("Set goal to read 10 books", "CREATE_GOAL"),
    ("Show my goals", "LIST_GOALS"),
    ("Create project Dexter Assistant", "CREATE_PROJECT"),
    ("Show my projects", "LIST_PROJECTS"),
    ("Daily briefing", "DAILY_BRIEFING"),
]

print("Running All-Segments Intent Extraction Verification:")
print("=" * 60)
all_passed = True
for query, expected_intent in test_queries:
    res = adapter.interpret(query)
    intent = res.get("intent")
    params = res.get("parameters")
    passed = (intent == expected_intent)
    status = "✅ PASS" if passed else "❌ FAIL"
    if not passed:
        all_passed = False
    print(f"{status} | \"{query}\" -> {intent} | params: {params}")

print("=" * 60)
if all_passed:
    print("🎯 ALL 17 SYSTEM & DATABASE SEGMENT INTENTS EXTRACTED PERFECTLY!")
else:
    print("⚠️ Some intents failed.")
