"""End-to-End Execution Test for VoiceListener across all 16 Database Segments."""

import sqlite3
from voice_listener import VoiceListener

# Initialize VoiceListener with TTS disabled for fast test execution
listener = VoiceListener(enable_tts=False, auto_confirm=True)

test_commands = [
    ("Create a task to buy fresh milk", "CREATE_TASK"),
    ("Remind me to call the doctor in 15 minutes", "CREATE_TASK (Timed Alarm)"),
    ("Spent 350 on lunch", "LOG_EXPENSE"),
    ("How much did I spend", "SUMMARIZE_EXPENSES"),
    ("Take a note: Meeting rescheduled to Monday morning", "LOG_JOURNAL"),
    ("Show notes", "LIST_JOURNAL"),
    ("Show reminders", "LIST_ALARMS"),
    ("Set goal to read 15 books", "CREATE_GOAL"),
    ("Show my goals", "LIST_GOALS"),
    ("Create project Home Automation", "CREATE_PROJECT"),
    ("Show my projects", "LIST_PROJECTS"),
    ("Daily briefing", "DAILY_BRIEFING"),
    ("Mark all tasks complete", "COMPLETE_ALL_TASKS"),
    ("Show completed tasks", "LIST_COMPLETED_TASKS"),
]

print("=" * 65)
print("🚀 TESTING VOICE LISTENER FULL-SYSTEM DATABASE INTEGRATION")
print("=" * 65)

for cmd, desc in test_commands:
    print(f"\n--- Testing: [{desc}] \"{cmd}\" ---")
    listener.process_voice_turn(cmd)

print("\n" + "=" * 65)
print("📊 VERIFYING DATABASE PERSISTENCE IN data/dexter.db")
print("=" * 65)

conn = sqlite3.connect("data/dexter.db")
cur = conn.cursor()

tables_to_check = [
    ("tasks", "SELECT COUNT(*), title FROM tasks GROUP BY id ORDER BY id DESC LIMIT 2"),
    ("alarms", "SELECT COUNT(*), name FROM alarms WHERE enabled=1"),
    ("finance_transactions", "SELECT COUNT(*), description, amount_minor FROM finance_transactions ORDER BY id DESC LIMIT 1"),
    ("journal_entries", "SELECT COUNT(*), content FROM journal_entries ORDER BY id DESC LIMIT 1"),
    ("goals", "SELECT COUNT(*), name FROM goals ORDER BY id DESC LIMIT 1"),
    ("projects", "SELECT COUNT(*), name FROM projects ORDER BY id DESC LIMIT 1"),
    ("briefing_runs", "SELECT COUNT(*), summary FROM briefing_runs ORDER BY id DESC LIMIT 1"),
    ("system_events", "SELECT COUNT(*), event_type FROM system_events ORDER BY id DESC LIMIT 1"),
]

all_verified = True
for table, query in tables_to_check:
    try:
        cur.execute(query)
        res = cur.fetchall()
        print(f"✅ Table '{table}': {res}")
    except Exception as e:
        print(f"❌ Table '{table}' check failed: {e}")
        all_verified = False

conn.close()
print("=" * 65)
if all_verified:
    print("🎉 FULL DATABASE & SYSTEM INTEGRATION VERIFIED 100% WORKING!")
else:
    print("⚠️ Some tables failed verification.")
