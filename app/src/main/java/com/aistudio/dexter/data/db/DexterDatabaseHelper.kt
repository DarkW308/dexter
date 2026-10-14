package com.aistudio.dexter.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DexterDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "dexter.db"
        const val DATABASE_VERSION = 2
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
        try {
            // Verify and add optional columns to finance_transactions if upgrading from v1
            val financeCursor = db.rawQuery("PRAGMA table_info(finance_transactions)", null)
            val financeCols = mutableListOf<String>()
            while (financeCursor.moveToNext()) {
                val colIndex = financeCursor.getColumnIndex("name")
                if (colIndex != -1) financeCols.add(financeCursor.getString(colIndex))
            }
            financeCursor.close()

            if (!financeCols.contains("payment_mode")) {
                db.execSQL("ALTER TABLE finance_transactions ADD COLUMN payment_mode TEXT NOT NULL DEFAULT 'UPI'")
            }
            if (!financeCols.contains("reference_id")) {
                db.execSQL("ALTER TABLE finance_transactions ADD COLUMN reference_id TEXT NOT NULL DEFAULT ''")
            }

            // Verify and add optional columns to journal_entries if upgrading from v1
            val journalCursor = db.rawQuery("PRAGMA table_info(journal_entries)", null)
            val journalCols = mutableListOf<String>()
            while (journalCursor.moveToNext()) {
                val colIndex = journalCursor.getColumnIndex("name")
                if (colIndex != -1) journalCols.add(journalCursor.getString(colIndex))
            }
            journalCursor.close()

            if (!journalCols.contains("title")) {
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN title TEXT NOT NULL DEFAULT ''")
            }
            if (!journalCols.contains("mood")) {
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN mood TEXT NOT NULL DEFAULT 'Reflective'")
            }
            if (!journalCols.contains("category")) {
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN category TEXT NOT NULL DEFAULT 'Reflections'")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking/migrating columns in onOpen: ${e.message}")
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
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    category TEXT NOT NULL DEFAULT 'General',
                    estimated_minutes INTEGER NOT NULL DEFAULT 15,
                    deadline INTEGER,
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                    completed_at INTEGER
                )
            """)

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS task_occurrences (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_id INTEGER NOT NULL,
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    occurred_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
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
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
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
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
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
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
                )
            """)

            // Segment 4: Goals
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS goals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    target_value REAL NOT NULL DEFAULT 100.0,
                    current_value REAL NOT NULL DEFAULT 0.0,
                    unit TEXT NOT NULL DEFAULT '%',
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    deadline INTEGER,
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
                )
            """)

            // Segment 5: Projects
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS projects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    description TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
                )
            """)

            // Segment 16: Audit Logs
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    action TEXT NOT NULL,
                    details TEXT NOT NULL,
                    timestamp INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
                )
            """)

            // Additional 16-Segment Tables
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS schedule_blocks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    start_time INTEGER NOT NULL,
                    end_time INTEGER NOT NULL,
                    tag TEXT NOT NULL DEFAULT 'Focus'
                )
            """)

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS contacts_rolodex (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    relationship TEXT NOT NULL DEFAULT 'Friend',
                    last_contacted INTEGER,
                    reminder_days INTEGER NOT NULL DEFAULT 14
                )
            """)

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS wellbeing_metrics (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    metric_type TEXT NOT NULL,
                    value REAL NOT NULL,
                    unit TEXT NOT NULL,
                    recorded_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
                )
            """)

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS knowledge_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    summary TEXT NOT NULL,
                    topic TEXT NOT NULL DEFAULT 'General',
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
                )
            """)

            seedInitialData(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Safe incremental migrations
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE finance_transactions ADD COLUMN payment_mode TEXT NOT NULL DEFAULT 'UPI'")
                db.execSQL("ALTER TABLE finance_transactions ADD COLUMN reference_id TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN title TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN mood TEXT NOT NULL DEFAULT 'Reflective'")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN category TEXT NOT NULL DEFAULT 'Reflections'")
            } catch (e: Exception) {
                Log.w(TAG, "Upgrade migration warning: ${e.message}")
            }
        }
    }

    private fun seedInitialData(db: SQLiteDatabase) {
        val now = System.currentTimeMillis() / 1000

        // Starter tasks
        db.execSQL("INSERT OR IGNORE INTO tasks (id, title, priority, status, category, estimated_minutes, created_at) VALUES (1, 'Review Dexter architecture and test offline voice recognition', 'HIGH', 'PENDING', 'Work', 30, $now)")
        db.execSQL("INSERT OR IGNORE INTO tasks (id, title, priority, status, category, estimated_minutes, created_at) VALUES (2, 'Verify bank statement ledger transactions and monthly budget', 'NORMAL', 'PENDING', 'Finance', 15, $now)")
        db.execSQL("INSERT OR IGNORE INTO tasks (id, title, priority, status, category, estimated_minutes, created_at) VALUES (3, 'Write daily evening reflection in personal diary', 'NORMAL', 'PENDING', 'Personal', 10, $now)")

        db.execSQL("INSERT OR IGNORE INTO task_occurrences (id, task_id, status, occurred_at) VALUES (1, 1, 'PENDING', $now)")
        db.execSQL("INSERT OR IGNORE INTO task_occurrences (id, task_id, status, occurred_at) VALUES (2, 2, 'PENDING', $now)")
        db.execSQL("INSERT OR IGNORE INTO task_occurrences (id, task_id, status, occurred_at) VALUES (3, 3, 'PENDING', $now)")

        // Primary account
        db.execSQL("INSERT OR IGNORE INTO finance_accounts (id, name, balance_minor, currency) VALUES (1, 'Primary Checking', 4500000, 'INR')")

        // Seed bank statement transactions
        val oneDayAgo = now - 86400
        val twoDaysAgo = now - (86400 * 2)

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
                'Gratitude',
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
        db.execSQL("INSERT OR IGNORE INTO goals (id, name, target_value, current_value, unit, status, created_at) VALUES (1, 'Run 100 Kilometers this month', 100.0, 34.5, 'km', 'ACTIVE', $now)")
        db.execSQL("INSERT OR IGNORE INTO goals (id, name, target_value, current_value, unit, status, created_at) VALUES (2, 'Read 12 Technical Books this year', 12.0, 4.0, 'books', 'ACTIVE', $now)")

        db.execSQL("INSERT OR IGNORE INTO projects (id, name, status, description, created_at) VALUES (1, 'Dexter AI OS Architecture', 'ACTIVE', 'Localhost autonomous companion on Helio P60', $now)")

        // Seed reminder
        val reminderTime = now + 14400 // 4 hours later
        db.execSQL("INSERT OR IGNORE INTO alarms (id, alarm_time, label, is_enabled, repeat_days, created_at) VALUES (1, $reminderTime, 'Review day wrap-up and priorities', 1, 'DAILY', $now)")
    }
}
