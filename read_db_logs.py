import sqlite3

conn = sqlite3.connect("data/dexter.db")
for tbl in ["system_logs", "system_events", "audit_logs", "action_traces", "alarms"]:
    try:
        rows = conn.execute(f"SELECT * FROM {tbl} ORDER BY 1 DESC LIMIT 10").fetchall()
        print(f"=== {tbl} (showing up to 10 of {len(rows)}) ===")
        for r in rows:
            print(r)
    except Exception as e:
        print(f"{tbl}: {e}")
