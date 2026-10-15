package com.aistudio.dexter.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DexterDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "dexter.db"
        const val DATABASE_VERSION = 3
        private const val TAG = "DexterDatabaseHelper"

        @Volatile
        private var instance: DexterDatabaseHelper? = null

        fun getInstance(context: Context): DexterDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DexterDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
        db.enableWriteAheadLogging()
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        ensureSchemaIntegrity(db)
    }

    private fun ensureSchemaIntegrity(db: SQLiteDatabase) {
        try {
            // 1. Ensure tasks table has all required columns
            ensureColumn(db, "tasks", "project_id", "INTEGER")
            ensureColumn(db, "tasks", "due_at", "INTEGER")
            ensureColumn(db, "tasks", "scheduled_start", "INTEGER")
            ensureColumn(db, "tasks", "scheduled_end", "INTEGER")
            ensureColumn(db, "tasks", "estimated_duration_seconds", "INTEGER")
            ensureColumn(db, "tasks", "completion_source", "TEXT")
            ensureColumn(db, "tasks", "notes", "TEXT NOT NULL DEFAULT ''")
            ensureColumn(db, "tasks", "category", "TEXT NOT NULL DEFAULT 'General'")

            // 2. Ensure task_occurrences has all columns
            ensureColumn(db, "task_occurrences", "scheduled_start", "INTEGER NOT NULL DEFAULT 0")
            ensureColumn(db, "task_occurrences", "scheduled_end", "INTEGER NOT NULL DEFAULT 0")
            ensureColumn(db, "task_occurrences", "completed_at", "INTEGER")

            // 3. Ensure finance_transactions has all columns
            ensureColumn(db, "finance_transactions", "payment_mode", "TEXT NOT NULL DEFAULT 'UPI'")
            ensureColumn(db, "finance_transactions", "reference_id", "TEXT NOT NULL DEFAULT ''")
            ensureColumn(db, "finance_transactions", "notes", "TEXT NOT NULL DEFAULT ''")
            ensureColumn(db, "finance_transactions", "actor_type", "TEXT NOT NULL DEFAULT 'USER'")

            // 4. Ensure journal_entries has all columns
            ensureColumn(db, "journal_entries", "title", "TEXT NOT NULL DEFAULT ''")
            ensureColumn(db, "journal_entries", "mood", "TEXT NOT NULL DEFAULT 'Reflective'")
            ensureColumn(db, "journal_entries", "category", "TEXT NOT NULL DEFAULT 'Reflections'")
            ensureColumn(db, "journal_entries", "actor_type", "TEXT NOT NULL DEFAULT 'USER'")

            // 5. Ensure goals has all columns
            ensureColumn(db, "goals", "title", "TEXT NOT NULL DEFAULT ''")
            ensureColumn(db, "goals", "target_date", "INTEGER NOT NULL DEFAULT 0")
            ensureColumn(db, "goals", "progress_percent", "INTEGER NOT NULL DEFAULT 0")

            // 6. Ensure projects has all columns
            ensureColumn(db, "projects", "color_hex", "TEXT NOT NULL DEFAULT '#4FC3F7'")
            ensureColumn(db, "projects", "task_count", "INTEGER NOT NULL DEFAULT 0")

            // 7. Ensure audit table exists
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS audit (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    action TEXT NOT NULL,
                    details TEXT NOT NULL DEFAULT '',
                    timestamp INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
                    actor_type TEXT NOT NULL DEFAULT 'USER'
                )
            """)

            // 8. Ensure primary finance account exists
            db.execSQL("INSERT OR IGNORE INTO finance_accounts (id, name, balance_minor, currency) VALUES (1, 'Primary Account', 4500000, 'INR')")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking/migrating schema in onOpen: ${e.message}")
        }
    }

    private fun ensureColumn(db: SQLiteDatabase, table: String, column: String, definition: String) {
        try {
            val cursor = db.rawQuery("PRAGMA table_info($table)", null)
            val columns = mutableListOf<String>()
            cursor.use {
                while (it.moveToNext()) {
                    val nameIdx = it.getColumnIndex("name")
                    if (nameIdx != -1) columns.add(it.getString(nameIdx))
                }
            }
            if (!columns.contains(column)) {
                db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice adding column $column to $table: ${e.message}")
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            // Segment 1 & 2: Tasks & Occurrences
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    priority TEXT NOT NULL DEFAULT 'NORMAL',
                    project_id INTEGER,
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    due_at INTEGER,
                    scheduled_start INTEGER,
                    scheduled_end INTEGER,
                    estimated_duration_seconds INTEGER,
                    completion_source TEXT,
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
                    notes TEXT NOT NULL DEFAULT '',
                    category TEXT NOT NULL DEFAULT 'General'
                )
            """)

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS task_occurrences (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_id INTEGER NOT NULL,
                    status TEXT NOT NULL DEFAULT 'SCHEDULED',
                    scheduled_start INTEGER NOT NULL DEFAULT 0,
                    scheduled_end INTEGER NOT NULL DEFAULT 0,
                    completed_at INTEGER,
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
                    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
                )
            """)

            // Segment 8: Finance
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS finance_accounts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    balance_minor INTEGER NOT NULL DEFAULT 0,
                    currency TEXT NOT NULL DEFAULT 'INR'
                )
            """)

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS finance_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    account_id INTEGER NOT NULL DEFAULT 1,
                    amount_minor INTEGER NOT NULL,
                    description TEXT NOT NULL,
                    type TEXT NOT NULL DEFAULT 'DEBIT',
                    category TEXT NOT NULL DEFAULT 'General',
                    payment_mode TEXT NOT NULL DEFAULT 'UPI',
                    reference_id TEXT NOT NULL DEFAULT '',
                    notes TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
                    actor_type TEXT NOT NULL DEFAULT 'USER'
                )
            """)

            // Segment 7: Alarms & Reminders
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS alarms (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    alarm_time INTEGER NOT NULL,
                    label TEXT NOT NULL,
                    is_enabled INTEGER NOT NULL DEFAULT 1,
                    repeat_days TEXT NOT NULL DEFAULT 'DAILY',
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                )
            """)

            // Segment 10: Journal & Diary
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS journal_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    content TEXT NOT NULL,
                    source TEXT NOT NULL DEFAULT 'VOICE',
                    title TEXT NOT NULL DEFAULT '',
                    mood TEXT NOT NULL DEFAULT 'Reflective',
                    category TEXT NOT NULL DEFAULT 'Reflections',
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
                    actor_type TEXT NOT NULL DEFAULT 'USER'
                )
            """)

            // Segment 4: Goals
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS goals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    name TEXT NOT NULL DEFAULT '',
                    target_date INTEGER NOT NULL DEFAULT 0,
                    progress_percent INTEGER NOT NULL DEFAULT 0,
                    status TEXT NOT NULL DEFAULT 'IN_PROGRESS',
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                )
            """)

            // Segment 5: Projects
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS projects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    color_hex TEXT NOT NULL DEFAULT '#4FC3F7',
                    task_count INTEGER NOT NULL DEFAULT 0,
                    description TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                )
            """)

            // Segment 16: Audit Logs
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS audit (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    action TEXT NOT NULL,
                    details TEXT NOT NULL DEFAULT '',
                    timestamp INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
                    actor_type TEXT NOT NULL DEFAULT 'USER'
                )
            """)

            seedInitialData(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        ensureSchemaIntegrity(db)
    }

    private fun seedInitialData(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()

        // Starter tasks
        db.execSQL("INSERT OR IGNORE INTO tasks (id, title, priority, status, category, created_at) VALUES (1, 'Review Dexter architecture and test offline voice recognition', 'HIGH', 'PENDING', 'Work', $now)")
        db.execSQL("INSERT OR IGNORE INTO tasks (id, title, priority, status, category, created_at) VALUES (2, 'Verify bank statement ledger transactions and monthly budget', 'NORMAL', 'PENDING', 'Finance', $now)")
        db.execSQL("INSERT OR IGNORE INTO tasks (id, title, priority, status, category, created_at) VALUES (3, 'Write daily evening reflection in personal diary', 'NORMAL', 'PENDING', 'Personal', $now)")

        db.execSQL("INSERT OR IGNORE INTO task_occurrences (id, task_id, status, scheduled_start, scheduled_end, created_at) VALUES (1, 1, 'SCHEDULED', $now, ${now + 1800000}, $now)")
        db.execSQL("INSERT OR IGNORE INTO task_occurrences (id, task_id, status, scheduled_start, scheduled_end, created_at) VALUES (2, 2, 'SCHEDULED', $now, ${now + 1800000}, $now)")
        db.execSQL("INSERT OR IGNORE INTO task_occurrences (id, task_id, status, scheduled_start, scheduled_end, created_at) VALUES (3, 3, 'SCHEDULED', $now, ${now + 1800000}, $now)")

        // Primary account
        db.execSQL("INSERT OR IGNORE INTO finance_accounts (id, name, balance_minor, currency) VALUES (1, 'Primary Account', 4500000, 'INR')")

        // Seed bank statement transactions
        val oneDayAgo = now - 86400000
        val twoDaysAgo = now - (86400000 * 2)

        db.execSQL("INSERT OR IGNORE INTO finance_transactions (id, account_id, amount_minor, description, type, category, payment_mode, reference_id, created_at) VALUES (1, 1, 5500000, 'Monthly Consulting Retainer', 'CREDIT', 'Salary', 'Net Banking', 'TXN-984321', $twoDaysAgo)")
        db.execSQL("INSERT OR IGNORE INTO finance_transactions (id, account_id, amount_minor, description, type, category, payment_mode, reference_id, created_at) VALUES (2, 1, 145000, 'Supermarket Weekly Groceries', 'DEBIT', 'Groceries', 'Card', 'TXN-837194', $oneDayAgo)")
        db.execSQL("INSERT OR IGNORE INTO finance_transactions (id, account_id, amount_minor, description, type, category, payment_mode, reference_id, created_at) VALUES (3, 1, 35000, 'Coffee and Bistro Lunch with Team', 'DEBIT', 'Food & Dining', 'UPI', 'UPI-492810', $now)")
        db.execSQL("INSERT OR IGNORE INTO finance_transactions (id, account_id, amount_minor, description, type, category, payment_mode, reference_id, created_at) VALUES (4, 1, 85000, 'Broadband Internet Fiber Bill', 'DEBIT', 'Utilities', 'UPI', 'UPI-110294', $now)")

        // Seed personal diary entries
        db.execSQL("""
            INSERT OR IGNORE INTO journal_entries (id, content, source, title, mood, category, created_at) VALUES (
                1,
                'Had an invigorating engineering session today focusing on offline voice interaction and local database integrity. Dexter is evolving into a truly reliable companion.',
                'VOICE',
                'Breakthrough on Dexter Architecture',
                'Energized',
                'Reflections',
                $twoDaysAgo
            )
        """)

        db.execSQL("""
            INSERT OR IGNORE INTO journal_entries (id, content, source, title, mood, category, created_at) VALUES (
                2,
                'Spent the evening in quiet reading. Grateful for the calm moments between deep work sprints.',
                'TEXT',
                'Evening Calm & Gratitude',
                'Calm',
                'Personal',
                $oneDayAgo
            )
        """)

        db.execSQL("""
            INSERT OR IGNORE INTO journal_entries (id, content, source, title, mood, category, created_at) VALUES (
                3,
                'Planning tomorrow''s roadmap. High clarity on building structured personal finance ledgers and frictionless diary capture.',
                'VOICE',
                'Clarity on Personal Goals',
                'Focused',
                'Ideas',
                $now
            )
        """)

        // Seed goals & projects
        db.execSQL("INSERT OR IGNORE INTO goals (id, title, name, progress_percent, status, created_at) VALUES (1, 'Run 100 Kilometers this month', 'Run 100 Kilometers this month', 35, 'IN_PROGRESS', $now)")
        db.execSQL("INSERT OR IGNORE INTO goals (id, title, name, progress_percent, status, created_at) VALUES (2, 'Read 12 Technical Books this year', 'Read 12 Technical Books this year', 33, 'IN_PROGRESS', $now)")

        db.execSQL("INSERT OR IGNORE INTO projects (id, name, status, color_hex, task_count, created_at) VALUES (1, 'Dexter AI OS Architecture', 'ACTIVE', '#4FC3F7', 3, $now)")

        // Seed reminder
        val reminderTime = now + (14400 * 1000)
        db.execSQL("INSERT OR IGNORE INTO alarms (id, alarm_time, label, is_enabled, repeat_days, created_at) VALUES (1, $reminderTime, 'Review day wrap-up and priorities', 1, 'DAILY', $now)")
    }
}
