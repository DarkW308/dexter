import sqlite3
import os
from datetime import datetime

DB_PATH = "data/dexter.db"
SCHEMA_PATH = "schema.sql"

def init_db():
    """Initializes the SQLite database with the final architecture schema."""
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    
    conn = sqlite3.connect(DB_PATH)
    # Enforce foreign keys as per architecture
    conn.execute("PRAGMA foreign_keys = ON;")
    conn.execute("PRAGMA journal_mode = WAL;")
    
    print("Executing final architecture schema...")
    try:
        with open(SCHEMA_PATH, 'r', encoding='utf-8') as f:
            schema_script = f.read()
            conn.executescript(schema_script)
        print("Database initialized successfully with Dexter v1 Architecture.")
    except Exception as e:
        print(f"Error initializing database schema: {e}")
    finally:
        conn.commit()
        conn.close()

if __name__ == "__main__":
    init_db()
