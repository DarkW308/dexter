import sqlite3

conn = sqlite3.connect("data/dexter.db")
cur = conn.cursor()
cand = "Doing SQL course"
cur.execute(
    "SELECT id, title FROM tasks WHERE archived_at IS NULL AND (LOWER(title) = LOWER(?) OR LOWER(title) LIKE LOWER(?)) ORDER BY id DESC LIMIT 1",
    (cand, f"%{cand}%")
)
row = cur.fetchone()
print("Matched row for Doing SQL course:", row)
