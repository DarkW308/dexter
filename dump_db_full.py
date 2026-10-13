import sqlite3

conn = sqlite3.connect("data/dexter.db")
conn.row_factory = sqlite3.Row

tables = ["tasks", "task_occurrences", "alarms", "finance_transactions", "journal_entries", "goals", "projects", "briefing_runs", "audit_logs", "system_events"]

for tbl in tables:
    try:
        rows = conn.execute(f"SELECT * FROM {tbl} ORDER BY id DESC LIMIT 5").fetchall()
        print(f"=== {tbl} ({len(rows)}) ===")
        for r in rows:
            print(dict(r))
    except Exception as e:
        print(f"=== {tbl}: Error: {e} ===")
