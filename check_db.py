import sqlite3

conn = sqlite3.connect("data/dexter.db")
cur = conn.cursor()
cur.execute("SELECT name FROM sqlite_master WHERE type='table'")
tables = [r[0] for r in cur.fetchall()]
print(f"Total tables: {len(tables)}")
print("Tables:", tables)

for t in ["tasks", "alarms", "goals", "projects", "journal_entries", "finance_accounts", "finance_transactions", "briefing_runs"]:
    try:
        cur.execute(f"SELECT COUNT(*) FROM {t}")
        count = cur.fetchone()[0]
        print(f"  {t}: {count} rows")
    except Exception as e:
        print(f"  {t}: Error: {e}")
