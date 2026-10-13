from needle_adapter import NeedleAdapter

adapter = NeedleAdapter()

phrases = [
    # Alarms, reminders, alerts
    "set an alarm for 7:30 AM",
    "set alarm at 6 am",
    "set alarm for tomorrow 8 am",
    "set an alert for meeting at 5 pm",
    "set alert 5 pm",
    "wake me up at 7 am",
    "remind me to take medicine at 9 pm",
    "remind me to drink water in 10 minutes",
    # Tasks & scheduling
    "create a task to study math",
    "create non important task to clean desk",
    "add a low priority task to wash car",
    "create low priority task read book",
    "schedule a task to study at 4 pm",
    "schedule meeting at 3 pm",
    "schedule workout for tomorrow at 6 am",
    "mark task 4 complete",
    "mark Doing SQL course complete",
    "complete task 23",
    "task 23 is completed",
    "mark all tasks complete",
    # Finance
    "spent 500 on groceries",
    "spent 500 rupees on groceries",
    "paid 200 rupees for auto",
    "paid 1200 for electricity bill",
    "log expense 350 for lunch",
    "log expense 350 lunch",
    "how much did I spend",
    "show my expenses",
    # Journal & Notes
    "take a note: call plumber",
    "take a note meeting schedule on Monday",
    "note that meeting is tomorrow",
    "show notes",
    "show a note",
    "read my notes",
    # Briefing
    "daily briefing",
    "day briefing",
    "today's briefing",
    "what is my day briefing",
]

print(f"{'Phrase':<45} | {'Intent':<25} | {'Params'}")
print("=" * 95)
for p in phrases:
    try:
        res = adapter.interpret(p)
        intent = res.get("intent")
        params = res.get("parameters")
        status = intent if intent else "❓ CLARIFICATION"
        print(f"{p:<45} | {status:<25} | {params}")
    except Exception as e:
        print(f"{p:<45} | ❌ ERROR: {e}")
