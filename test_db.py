import sqlite3
import traceback

try:
    conn = sqlite3.connect('dexter/data/dexter.db')
    cur = conn.cursor()
    cur.execute("SELECT name FROM sqlite_master WHERE type='table';")
    tables = cur.fetchall()
    print('Tables:', tables)
    
    if ('audit_logs',) in tables:
        cur.execute("SELECT * FROM audit_logs ORDER BY log_id DESC LIMIT 5")
        for row in cur.fetchall():
            print(row)
    else:
        print('No audit_logs table found.')
except Exception as e:
    print('Error:', e)
    traceback.print_exc()
