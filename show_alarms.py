import sqlite3
import time
from datetime import datetime

conn = sqlite3.connect("data/dexter.db")

print("=" * 65)
print("⏰ LATEST ALARMS / REMINDERS IN DEXTER DB")
print("=" * 65)
rows = conn.execute("SELECT id, name, label, enabled, alarm_time FROM alarms ORDER BY id DESC LIMIT 5").fetchall()
for r in rows:
    target_dt = datetime.fromtimestamp(r[4]).strftime("%Y-%m-%d %H:%M:%S") if r[4] else "N/A"
    status = "🟢 ACTIVE" if r[3] == 1 else "⚪ TRIGGERED/DONE"
    print(f"  • [#{r[0]}] {r[1]}")
    print(f"     Status: {status} | Scheduled Time: {target_dt}")

print("\n" + "=" * 65)
print("📋 RECENT TASKS IN DEXTER DB")
print("=" * 65)
task_rows = conn.execute("SELECT id, title, priority, created_at FROM tasks ORDER BY id DESC LIMIT 5").fetchall()
for t in task_rows:
    created_dt = datetime.fromtimestamp(t[3]).strftime("%Y-%m-%d %H:%M:%S")
    print(f"  • [#{t[0]}] {t[1]} (Priority: {t[2]}) - {created_dt}")

conn.close()
print("=" * 65)
