package com.aistudio.dexter.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DexterDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "dexter.db"
        const val DATABASE_VERSION = 4
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
            // Segment 1: Tasks & Occurrences
            ensureColumn(db, "tasks", "project_id", "INTEGER")
            ensureColumn(db, "tasks", "due_at", "INTEGER")
            ensureColumn(db, "tasks", "scheduled_start", "INTEGER")
            ensureColumn(db, "tasks", "scheduled_end", "INTEGER")
            ensureColumn(db, "tasks", "estimated_duration_seconds", "INTEGER")
            ensureColumn(db, "tasks", "completion_source", "TEXT")
            ensureColumn(db, "tasks", "notes", "TEXT NOT NULL DEFAULT ''")
            ensureColumn(db, "tasks", "category", "TEXT NOT NULL DEFAULT 'General'")

            ensureColumn(db, "task_occurrences", "scheduled_start", "INTEGER NOT NULL DEFAULT 0")
            ensureColumn(db, "task_occurrences", "scheduled_end", "INTEGER NOT NULL DEFAULT 0")
            ensureColumn(db, "task_occurrences", "completed_at", "INTEGER")

            // Segment 2: Projects & Milestones
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS projects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    title TEXT NOT NULL DEFAULT '',
                    description TEXT NOT NULL DEFAULT '',
                    deadline TEXT NOT NULL DEFAULT '',
                    color_hex TEXT NOT NULL DEFAULT '#4FC3F7',
                    progress_percent INTEGER NOT NULL DEFAULT 0,
                    task_count INTEGER NOT NULL DEFAULT 0,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                )
            """)
            ensureColumn(db, "projects", "deadline", "TEXT NOT NULL DEFAULT ''")
            ensureColumn(db, "projects", "progress_percent", "INTEGER NOT NULL DEFAULT 0")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS project_milestones (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    is_completed INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
                    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
                )
            """)

            // Segment 3: Goals
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS goals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    name TEXT NOT NULL DEFAULT '',
                    category TEXT NOT NULL DEFAULT 'Personal',
                    target_date TEXT NOT NULL DEFAULT '',
                    target_amount REAL NOT NULL DEFAULT 0.0,
                    current_amount REAL NOT NULL DEFAULT 0.0,
                    metric_unit TEXT NOT NULL DEFAULT '₹',
                    progress_percent INTEGER NOT NULL DEFAULT 0,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                )
            """)
            ensureColumn(db, "goals", "category", "TEXT NOT NULL DEFAULT 'Personal'")
            ensureColumn(db, "goals", "target_date", "TEXT NOT NULL DEFAULT ''")
            ensureColumn(db, "goals", "target_amount", "REAL NOT NULL DEFAULT 0.0")
            ensureColumn(db, "goals", "current_amount", "REAL NOT NULL DEFAULT 0.0")
            ensureColumn(db, "goals", "metric_unit", "TEXT NOT NULL DEFAULT '₹'")

            // Segment 4: Personal Finance
            ensureColumn(db, "finance_transactions", "payment_mode", "TEXT NOT NULL DEFAULT 'UPI'")
            ensureColumn(db, "finance_transactions", "reference_id", "TEXT NOT NULL DEFAULT ''")
            ensureColumn(db, "finance_transactions", "notes", "TEXT NOT NULL DEFAULT ''")
            ensureColumn(db, "finance_transactions", "actor_type", "TEXT NOT NULL DEFAULT 'USER'")

            // Segment 5: Personal Diary & Journal
            ensureColumn(db, "journal_entries", "title", "TEXT NOT NULL DEFAULT ''")
            ensureColumn(db, "journal_entries", "mood", "TEXT NOT NULL DEFAULT 'Reflective'")
            ensureColumn(db, "journal_entries", "category", "TEXT NOT NULL DEFAULT 'Reflections'")
            ensureColumn(db, "journal_entries", "actor_type", "TEXT NOT NULL DEFAULT 'USER'")

            // Segment 6: Alarms & Timed Reminders
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS alarms (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    alarm_time INTEGER NOT NULL,
                    label TEXT NOT NULL,
                    is_enabled INTEGER NOT NULL DEFAULT 1,
                    repeat_days TEXT NOT NULL DEFAULT 'DAILY',
                    vibrate INTEGER NOT NULL DEFAULT 1,
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                )
            """)
            ensureColumn(db, "alarms", "vibrate", "INTEGER NOT NULL DEFAULT 1")

            // Segment 7: Time & Schedule (Daily Timetable)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS schedule_blocks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    start_time TEXT NOT NULL,
                    end_time TEXT NOT NULL,
                    category TEXT NOT NULL DEFAULT 'Deep Work',
                    color_hex TEXT NOT NULL DEFAULT '#4FC3F7',
                    location TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                )
            """)

            // Segment 8: People & Relationships
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS relationship_people (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    relation_type TEXT NOT NULL DEFAULT 'Friend',
                    phone TEXT NOT NULL DEFAULT '',
                    email TEXT NOT NULL DEFAULT '',
                    birthday TEXT NOT NULL DEFAULT '',
                    notes TEXT NOT NULL DEFAULT '',
                    last_contacted_text TEXT NOT NULL DEFAULT 'Recently',
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                )
            """)
            ensureColumn(db, "relationship_people", "notes", "TEXT NOT NULL DEFAULT ''")
            ensureColumn(db, "relationship_people", "last_contacted_text", "TEXT NOT NULL DEFAULT 'Recently'")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS relationship_interactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    person_id INTEGER NOT NULL,
                    interaction_type TEXT NOT NULL DEFAULT 'Meeting',
                    note TEXT NOT NULL,
                    interaction_date TEXT NOT NULL DEFAULT 'Today',
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
                    FOREIGN KEY (person_id) REFERENCES relationship_people(id) ON DELETE CASCADE
                )
            """)

            // Segment 9: Health & Wellbeing (Hydration, Sleep & Mood)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS wellbeing_hydration (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    amount_ml INTEGER NOT NULL DEFAULT 250,
                    logged_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                )
            """)

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS wellbeing_sleep (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    bedtime TEXT NOT NULL,
                    waketime TEXT NOT NULL,
                    duration_text TEXT NOT NULL DEFAULT '7h 30m',
                    quality_rating INTEGER NOT NULL DEFAULT 4,
                    quality_label TEXT NOT NULL DEFAULT 'Restful',
                    notes TEXT NOT NULL DEFAULT '',
                    logged_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                )
            """)
            ensureColumn(db, "wellbeing_sleep", "notes", "TEXT NOT NULL DEFAULT ''")
            ensureColumn(db, "wellbeing_sleep", "duration_text", "TEXT NOT NULL DEFAULT '7h 30m'")
            ensureColumn(db, "wellbeing_sleep", "quality_rating", "INTEGER NOT NULL DEFAULT 4")
            ensureColumn(db, "wellbeing_sleep", "quality_label", "TEXT NOT NULL DEFAULT 'Restful'")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS wellbeing_mood (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    mood TEXT NOT NULL,
                    energy_level TEXT NOT NULL DEFAULT 'Medium',
                    note TEXT NOT NULL DEFAULT '',
                    logged_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                )
            """)
            ensureColumn(db, "wellbeing_mood", "note", "TEXT NOT NULL DEFAULT ''")
            ensureColumn(db, "wellbeing_mood", "energy_level", "TEXT NOT NULL DEFAULT 'Medium'")

            // Segment 10: Habits & Streaks
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS wellbeing_habits (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    frequency TEXT NOT NULL DEFAULT 'Daily',
                    reminder_time TEXT NOT NULL DEFAULT '08:00 AM',
                    streak_count INTEGER NOT NULL DEFAULT 0,
                    is_done_today INTEGER NOT NULL DEFAULT 0,
                    last_completed_at INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                )
            """)
            ensureColumn(db, "wellbeing_habits", "last_completed_at", "INTEGER NOT NULL DEFAULT 0")
            ensureColumn(db, "wellbeing_habits", "reminder_time", "TEXT NOT NULL DEFAULT '08:00 AM'")

            // Segment 11: Reading & Knowledge Vault (Local RAG / Notes)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS knowledge_documents (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    source TEXT NOT NULL DEFAULT 'Manual',
                    file_type TEXT NOT NULL DEFAULT 'DOC',
                    excerpt TEXT NOT NULL DEFAULT '',
                    chunk_count INTEGER NOT NULL DEFAULT 1,
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                )
            """)

            // Segment 12: Quick Capture & Inbox
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS capture_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    note_text TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'UNPROCESSED',
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                )
            """)

            // Segment 13: Attention & Priority Feed
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS attention_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    urgency TEXT NOT NULL DEFAULT 'Normal',
                    is_acknowledged INTEGER NOT NULL DEFAULT 0,
                    snoozed_until INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                )
            """)

            // Segment 14: Daily Briefings & Summaries
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS briefing_runs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    summary_text TEXT NOT NULL,
                    date_string TEXT NOT NULL,
                    tasks_count INTEGER NOT NULL DEFAULT 0,
                    spent_minor INTEGER NOT NULL DEFAULT 0,
                    weather_text TEXT NOT NULL DEFAULT '28°C Sunny',
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                )
            """)

            // Segment 15: Safety & System Rules
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS system_rules (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    rule_text TEXT NOT NULL,
                    enforcement_level TEXT NOT NULL DEFAULT 'Strict',
                    is_enabled INTEGER NOT NULL DEFAULT 1,
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                )
            """)

            // Segment 16: Audit
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS audit (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    action TEXT NOT NULL,
                    details TEXT NOT NULL DEFAULT '',
                    timestamp INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
                    actor_type TEXT NOT NULL DEFAULT 'USER'
                )
            """)

            // Ensure primary finance account exists
            db.execSQL("INSERT OR IGNORE INTO finance_accounts (id, name, balance_minor, currency) VALUES (1, 'Primary Account', 4500000, 'INR')")

            // Seed segments if empty
            seedSegmentDataIfEmpty(db)
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
            // Segment 1: Tasks & Occurrences
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

            // Segment 4: Finance
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

            // Segment 5: Journal & Diary
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

            ensureSchemaIntegrity(db)
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

    private fun seedSegmentDataIfEmpty(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()

        fun count(table: String): Int {
            return try {
                val c = db.rawQuery("SELECT COUNT(*) FROM $table", null)
                c.use { if (it.moveToFirst()) it.getInt(0) else 0 }
            } catch (e: Exception) { 0 }
        }

        // Segment 2: Projects & Milestones
        if (count("projects") == 0) {
            db.execSQL("""
                INSERT INTO projects (id, name, title, description, deadline, color_hex, progress_percent, task_count, status, created_at)
                VALUES (1, 'College Final Year Seminar', 'College Final Year Seminar', 'Research and slide deck on Edge AI on mobile chips', 'Nov 30, 2026', '#4FC3F7', 42, 7, 'ACTIVE', $now)
            """)
            db.execSQL("INSERT INTO project_milestones (project_id, title, is_completed, created_at) VALUES (1, 'Literature review on Mobile Edge AI', 1, $now)")
            db.execSQL("INSERT INTO project_milestones (project_id, title, is_completed, created_at) VALUES (1, 'Benchmark MediaTek Helio P60 NPU/DSP', 1, $now)")
            db.execSQL("INSERT INTO project_milestones (project_id, title, is_completed, created_at) VALUES (1, 'Implement quantized 4-bit model runner', 1, $now)")
            db.execSQL("INSERT INTO project_milestones (project_id, title, is_completed, created_at) VALUES (1, 'Create demo slides on offline inference', 0, $now)")
            db.execSQL("INSERT INTO project_milestones (project_id, title, is_completed, created_at) VALUES (1, 'Measure memory footprint ≤ 120MB', 0, $now)")
            db.execSQL("INSERT INTO project_milestones (project_id, title, is_completed, created_at) VALUES (1, 'Draft presentation speech and Q&A', 0, $now)")
            db.execSQL("INSERT INTO project_milestones (project_id, title, is_completed, created_at) VALUES (1, 'Faculty dry run and final review', 0, $now)")
        }

        // Segment 3: Goals
        if (count("goals") == 0) {
            db.execSQL("""
                INSERT INTO goals (id, title, name, category, target_date, target_amount, current_amount, metric_unit, progress_percent, status, created_at)
                VALUES (1, 'Save ₹50,000 for emergency fund', 'Save ₹50,000 for emergency fund', 'Financial', 'Dec 31, 2026', 50000.0, 24500.0, '₹', 49, 'ACTIVE', $now)
            """)
            db.execSQL("""
                INSERT INTO goals (id, title, name, category, target_date, target_amount, current_amount, metric_unit, progress_percent, status, created_at)
                VALUES (2, 'Run 100 Kilometers this month', 'Run 100 Kilometers this month', 'Health', 'End of Month', 100.0, 35.0, 'km', 35, 'ACTIVE', $now)
            """)
            db.execSQL("""
                INSERT INTO goals (id, title, name, category, target_date, target_amount, current_amount, metric_unit, progress_percent, status, created_at)
                VALUES (3, 'Read 12 Technical Books this year', 'Read 12 Technical Books this year', 'Career', 'Dec 31, 2026', 12.0, 4.0, 'books', 33, 'ACTIVE', $now)
            """)
        }

        // Segment 6: Alarms & Timed Reminders
        if (count("alarms") <= 1) {
            val morningCal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 6)
                set(java.util.Calendar.MINUTE, 30)
                set(java.util.Calendar.SECOND, 0)
            }
            val afternoonCal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 14)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
            }
            db.execSQL("INSERT OR IGNORE INTO alarms (id, alarm_time, label, repeat_days, is_enabled, vibrate, created_at) VALUES (2, ${morningCal.timeInMillis}, 'Morning Workout', 'Mon, Wed, Fri', 1, 1, $now)")
            db.execSQL("INSERT OR IGNORE INTO alarms (id, alarm_time, label, repeat_days, is_enabled, vibrate, created_at) VALUES (3, ${afternoonCal.timeInMillis}, 'Take Vitamin D tablet', 'One-time', 1, 1, $now)")
        }

        // Segment 7: Schedule Blocks
        if (count("schedule_blocks") == 0) {
            db.execSQL("INSERT INTO schedule_blocks (title, start_time, end_time, category, color_hex, location, created_at) VALUES ('Deep Work: Coding', '10:00 AM', '01:00 PM', 'Deep Work', '#4FC3F7', 'Home Lab', $now)")
            db.execSQL("INSERT INTO schedule_blocks (title, start_time, end_time, category, color_hex, location, created_at) VALUES ('Lunch & Walk', '01:00 PM', '02:00 PM', 'Wellness', '#69F0AE', 'Park Street', $now)")
            db.execSQL("INSERT INTO schedule_blocks (title, start_time, end_time, category, color_hex, location, created_at) VALUES ('Client Sync Call', '04:30 PM', '05:00 PM', 'Meeting', '#FFAB40', 'Google Meet', $now)")
        }

        // Segment 8: Relationships
        if (count("relationship_people") == 0) {
            db.execSQL("INSERT INTO relationship_people (id, name, relation_type, phone, email, birthday, notes, last_contacted_text, created_at) VALUES (1, 'Rahul Verma', 'Close Friend', '+91 98765 43210', 'rahul@example.com', 'Nov 14', 'Discussed buying bike helmet together.', '3 days ago', $now)")
            db.execSQL("INSERT INTO relationship_people (id, name, relation_type, phone, email, birthday, notes, last_contacted_text, created_at) VALUES (2, 'Dr. Sharma', 'Doctor', '+91 98111 22334', 'dr.sharma@clinic.com', 'Aug 22', 'Family physician and annual wellness', '2 weeks ago', $now)")
            db.execSQL("INSERT INTO relationship_interactions (person_id, interaction_type, note, interaction_date, created_at) VALUES (1, 'Call', 'Discussed buying bike helmet together and upcoming weekend ride.', '3 days ago', $now)")
        }

        // Segment 9: Wellbeing (Hydration, Sleep, Mood)
        if (count("wellbeing_hydration") == 0) {
            repeat(7) {
                db.execSQL("INSERT INTO wellbeing_hydration (amount_ml, logged_at) VALUES (250, ${now - (it * 3600000)})")
            }
        }
        if (count("wellbeing_sleep") == 0) {
            db.execSQL("INSERT INTO wellbeing_sleep (bedtime, waketime, duration_text, quality_rating, quality_label, notes, logged_at) VALUES ('11:30 PM', '06:50 AM', '7h 20m', 5, 'Restful', 'Deep restful sleep, feeling energetic.', $now)")
        }
        if (count("wellbeing_mood") == 0) {
            db.execSQL("INSERT INTO wellbeing_mood (mood, energy_level, note, logged_at) VALUES ('Calm & Energetic', 'High', 'Productive morning building Dexter local features', $now)")
        }

        // Segment 10: Habits
        if (count("wellbeing_habits") == 0) {
            db.execSQL("INSERT INTO wellbeing_habits (id, name, frequency, reminder_time, streak_count, is_done_today, created_at) VALUES (1, 'Read 15 pages of book', 'Daily', '09:30 PM', 12, 1, $now)")
            db.execSQL("INSERT INTO wellbeing_habits (id, name, frequency, reminder_time, streak_count, is_done_today, created_at) VALUES (2, '10-minute Meditation', 'Daily', '07:00 AM', 4, 0, $now)")
            db.execSQL("INSERT INTO wellbeing_habits (id, name, frequency, reminder_time, streak_count, is_done_today, created_at) VALUES (3, 'Hydrate 2.5L Water', 'Daily', 'Hourly', 8, 1, $now)")
        }

        // Segment 11: Knowledge
        if (count("knowledge_documents") == 0) {
            db.execSQL("INSERT INTO knowledge_documents (id, title, source, file_type, excerpt, chunk_count, created_at) VALUES (1, 'Vivo V11 Camera Manual & Specs', 'Manual.pdf', 'PDF', 'Helio P60 camera ISP supports dual 16MP + 5MP sensors with AI scene recognition...', 14, $now)")
            db.execSQL("INSERT INTO knowledge_documents (id, title, source, file_type, excerpt, chunk_count, created_at) VALUES (2, 'Edge AI Offline Engine Architecture', 'Specs.md', 'MD', 'Offline inference architecture running within 120MB RAM constraint on Android 8.1 Oreo...', 8, $now)")
        }

        // Segment 12: Capture Events
        if (count("capture_events") == 0) {
            db.execSQL("INSERT INTO capture_events (id, note_text, status, created_at) VALUES (1, 'Idea: Build a local RSS reader into Dexter''s knowledge base next month.', 'UNPROCESSED', ${now - 86400000})")
            db.execSQL("INSERT INTO capture_events (id, note_text, status, created_at) VALUES (2, 'Look into high-durability bike helmets with MIPS protection.', 'UNPROCESSED', $now)")
        }

        // Segment 13: Attention Feed
        if (count("attention_events") == 0) {
            db.execSQL("INSERT INTO attention_events (id, title, urgency, is_acknowledged, snoozed_until, created_at) VALUES (1, 'Electricity Bill Due Tomorrow', 'Critical', 0, 0, $now)")
            db.execSQL("INSERT INTO attention_events (id, title, urgency, is_acknowledged, snoozed_until, created_at) VALUES (2, 'Rahul''s Birthday in 2 days', 'Normal', 0, 0, $now)")
            db.execSQL("INSERT INTO attention_events (id, title, urgency, is_acknowledged, snoozed_until, created_at) VALUES (3, 'Dexter local database backup verified', 'Info', 1, 0, $now)")
        }

        // Segment 14: Daily Briefings
        if (count("briefing_runs") == 0) {
            db.execSQL("INSERT INTO briefing_runs (id, title, summary_text, date_string, tasks_count, spent_minor, weather_text, created_at) VALUES (1, 'Morning Briefing — Oct 17, 2026', '3 tasks scheduled today. ₹1,200 spent this week. Weather: 28°C sunny. High priority: Finish Seminar slides.', 'Oct 17, 2026', 3, 120000, '28°C Sunny', $now)")
            db.execSQL("INSERT INTO briefing_runs (id, title, summary_text, date_string, tasks_count, spent_minor, weather_text, created_at) VALUES (2, 'Morning Briefing — Oct 16, 2026', 'All priority tasks completed. Net savings positive. Sleep target reached.', 'Oct 16, 2026', 4, 35000, '27°C Clear', ${now - 86400000})")
        }

        // Segment 15: Safety Rules
        if (count("system_rules") == 0) {
            db.execSQL("INSERT INTO system_rules (id, rule_text, enforcement_level, is_enabled, created_at) VALUES (1, 'Require manual PIN or confirmation before deleting finance records.', 'Strict', 1, $now)")
            db.execSQL("INSERT INTO system_rules (id, rule_text, enforcement_level, is_enabled, created_at) VALUES (2, 'Maximum daily notification limit: 5.', 'Strict', 1, $now)")
            db.execSQL("INSERT INTO system_rules (id, rule_text, enforcement_level, is_enabled, created_at) VALUES (3, 'Do not execute network calls when on cellular data.', 'Warn', 1, $now)")
        }

        // Segment 16: Audit
        if (count("audit") == 0) {
            db.execSQL("INSERT INTO audit (action, details, timestamp, actor_type) VALUES ('SYSTEM_INIT', 'Dexter offline core engine initialized with WAL mode', $now, 'SYSTEM')")
            db.execSQL("INSERT INTO audit (action, details, timestamp, actor_type) VALUES ('INTEGRITY_CHECK', 'RAM footprint 84 MB / 120 MB budget · DB 324 KB · Status OK', $now, 'SYSTEM')")
        }
    }
}
