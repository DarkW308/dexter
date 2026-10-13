import sqlite3

conn = sqlite3.connect('dexter/data/dexter.db')
conn.row_factory = sqlite3.Row
cur = conn.cursor()

print("=== SYSTEM LOGS ===")
try:
    cur.execute("SELECT * FROM system_logs ORDER BY 1 DESC LIMIT 3")
    for row in cur.fetchall():
        print(dict(row))
except Exception as e: print(e)

print("\n=== ACTION TRACES ===")
try:
    cur.execute("SELECT * FROM action_traces ORDER BY 1 DESC LIMIT 3")
    for row in cur.fetchall():
        print(dict(row))
except Exception as e: print(e)

print("\n=== AUDIT LOGS ===")
try:
    cur.execute("SELECT * FROM audit_logs ORDER BY 1 DESC LIMIT 3")
    for row in cur.fetchall():
        print(dict(row))
except Exception as e: print(e)
