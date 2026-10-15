package com.aistudio.dexter.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.aistudio.dexter.data.db.DexterDatabaseHelper
import com.aistudio.dexter.data.model.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Calendar

class DexterRepository(context: Context) {

    private val dbHelper = DexterDatabaseHelper.getInstance(context)

    val tasks: TaskRepository = TaskRepository(dbHelper)
    val finance: FinanceRepository = FinanceRepository(dbHelper)
    val alarms: AlarmRepository = AlarmRepository(dbHelper)
    val journal: JournalRepository = JournalRepository(dbHelper)
    val audit: AuditRepository = AuditRepository(dbHelper)

    val dataRevision: SharedFlow<Long>
        get() = Companion.dataRevision

    // Task convenience methods
    fun createTask(
        title: String,
        priority: Priority = Priority.NORMAL,
        dueTimestamp: Long = 0L,
        notes: String = "",
        category: String = "General"
    ): TaskItem {
        val task = tasks.createTask(title, priority, dueTimestamp, notes, category)
        notifyDataChanged()
        return task
    }

    fun completeTask(taskId: Long): Boolean {
        val result = tasks.completeTask(taskId)
        if (result) notifyDataChanged()
        return result
    }

    fun cancelTask(taskId: Long): Boolean {
        val result = tasks.cancelTask(taskId)
        if (result) notifyDataChanged()
        return result
    }

    fun listPendingTasks(limit: Int = 50): List<TaskItem> = tasks.listPendingTasks(limit)

    fun listCompletedTasks(limit: Int = 50): List<TaskItem> = tasks.listCompletedTasks(limit)

    fun getTaskCounts(): TaskCounts = tasks.getTaskCounts()

    // Finance convenience methods
    fun logTransaction(
        amountMinor: Long,
        desc: String,
        type: String,
        category: String,
        paymentMode: String = "UPI",
        actorType: String = "USER"
    ): FinanceTransaction {
        val tx = finance.logTransaction(
            amountMinor = amountMinor,
            desc = desc,
            type = type,
            category = category,
            paymentMode = paymentMode,
            actorType = actorType
        )
        notifyDataChanged()
        return tx
    }

    fun logExpense(
        amountMinor: Long,
        desc: String,
        category: String,
        paymentMode: String = "UPI"
    ): FinanceTransaction {
        return logTransaction(
            amountMinor = amountMinor,
            desc = desc,
            type = "DEBIT",
            category = category,
            paymentMode = paymentMode,
            actorType = "USER"
        )
    }

    fun deleteTransaction(id: Long): Boolean {
        val db = dbHelper.writableDatabase
        val rows = db.delete("finance_transactions", "id = ?", arrayOf(id.toString()))
        if (rows > 0) notifyDataChanged()
        return rows > 0
    }

    fun listTransactions(limit: Int = 100): List<FinanceTransaction> = finance.listTransactions(limit)

    fun getFinanceStatement(): Triple<Long, Long, Long> {
        return Triple(finance.getNetBalance(), finance.getTotalInflow(), finance.getTotalOutflow())
    }

    fun summarizeExpenses(): Pair<Long, Long> = finance.summarizeExpenses()

    fun getFinanceCategoryBreakdown(): Map<String, Long> = finance.getCategoryBreakdown()

    fun getFinanceBalance(): Long = finance.getNetBalance()

    fun getFinanceInflow(): Long = finance.getTotalInflow()

    fun getFinanceOutflow(): Long = finance.getTotalOutflow()

    // Alarm convenience methods
    fun createAlarm(alarmTime: Long, label: String): Long {
        val alarm = alarms.createAlarm(alarmTime, label)
        notifyDataChanged()
        return alarm.id
    }

    fun listAlarms(limit: Int = 20): List<AlarmItem> = alarms.listAlarms(limit)

    fun cancelAlarms(actorType: String = "USER"): Int {
        val count = alarms.cancelAlarms(actorType)
        if (count > 0) notifyDataChanged()
        return count
    }

    // Journal convenience methods
    fun logJournal(
        content: String,
        title: String = "",
        mood: String = "Calm",
        category: String = "Reflections",
        source: String = "APP",
        actorType: String = "USER"
    ): JournalEntry {
        val entry = journal.logJournal(
            content = content,
            title = title,
            mood = mood,
            category = category,
            source = source,
            actorType = actorType
        )
        notifyDataChanged()
        return entry
    }

    fun deleteJournal(id: Long): Boolean {
        val db = dbHelper.writableDatabase
        val rows = db.delete("journal_entries", "id = ?", arrayOf(id.toString()))
        if (rows > 0) notifyDataChanged()
        return rows > 0
    }

    fun insertNote(
        title: String,
        content: String,
        mood: String = "Calm",
        category: String = "General"
    ): Long {
        val id = journal.insertNote(title, content, mood, category)
        notifyDataChanged()
        return id
    }

    fun listNotes(limit: Int = 50): List<JournalEntry> = journal.listNotes(limit)

    fun listJournal(limit: Int = 100): List<JournalEntry> = journal.listNotes(limit)

    fun getJournalStats(): Triple<Int, Int, Int> {
        val all = journal.listNotes(100)
        return Triple(all.size, journal.getWritingStreakDays(), journal.getWordCountToday())
    }

    // Goals & Projects
    fun createGoal(name: String, targetDate: Long = 0L): GoalItem {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("title", name.trim())
            put("target_date", targetDate)
            put("progress_percent", 0)
            put("status", "IN_PROGRESS")
        }
        val id = db.insert("goals", null, values)
        notifyDataChanged()
        return GoalItem(
            id = id,
            name = name.trim(),
            title = name.trim(),
            targetDate = targetDate,
            progressPercent = 0,
            status = "IN_PROGRESS"
        )
    }

    fun listGoals(): List<GoalItem> {
        val list = mutableListOf<GoalItem>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT id, title, target_date, progress_percent, status FROM goals ORDER BY id DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                val title = it.getString(1) ?: ""
                list.add(
                    GoalItem(
                        id = it.getLong(0),
                        name = title,
                        title = title,
                        targetDate = it.getLong(2),
                        progressPercent = it.getInt(3),
                        status = it.getString(4) ?: "IN_PROGRESS"
                    )
                )
            }
        }
        return list
    }

    fun createProject(name: String, colorHex: String = "#4FC3F7"): ProjectItem {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", name.trim())
            put("status", "ACTIVE")
            put("color_hex", colorHex)
            put("task_count", 0)
        }
        val id = db.insert("projects", null, values)
        notifyDataChanged()
        return ProjectItem(
            id = id,
            name = name.trim(),
            status = "ACTIVE",
            colorHex = colorHex,
            taskCount = 0
        )
    }

    fun listProjects(): List<ProjectItem> {
        val list = mutableListOf<ProjectItem>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT id, name, status, color_hex, task_count FROM projects ORDER BY id DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ProjectItem(
                        id = it.getLong(0),
                        name = it.getString(1) ?: "",
                        status = it.getString(2) ?: "ACTIVE",
                        colorHex = it.getString(3) ?: "#4FC3F7",
                        taskCount = it.getInt(4)
                    )
                )
            }
        }
        return list
    }

    // Audit convenience
    fun getAuditLogs(limit: Int = 50): List<AuditLog> = audit.listRecentLogs(limit)

    // 16 Segments Architecture Definition
    fun getAllSegments(): List<SegmentInfo> {
        val db = dbHelper.readableDatabase
        fun count(table: String): Int {
            return try {
                val c = db.rawQuery("SELECT COUNT(*) FROM $table", null)
                c.use { if (it.moveToFirst()) it.getInt(0) else 0 }
            } catch (e: Exception) { 0 }
        }

        return listOf(
            SegmentInfo(1, "Executive & Identity", "Core", "👑", "User persona, executive directives, and system permissions", listOf("identity", "user_profile"), count("tasks")),
            SegmentInfo(2, "Personal Finance", "Finance", "💳", "Ledger, bank transactions, expenses, UPI payments, and budget control", listOf("finance_transactions", "finance_accounts", "budgets"), count("finance_transactions")),
            SegmentInfo(3, "Task & Execution", "Execution", "✅", "GTD sprint engine, recurring occurrences, and dual-table task tracking", listOf("tasks", "task_occurrences"), count("tasks")),
            SegmentInfo(4, "Projects", "Execution", "📁", "Initiative hubs, milestones, and high-impact deliverables", listOf("projects", "project_milestones"), count("projects")),
            SegmentInfo(5, "Goals & Outcomes", "Execution", "🎯", "Quarterly targets, vision boards, and progress percentages", listOf("goals", "goal_targets"), count("goals")),
            SegmentInfo(6, "Personal Diary & Journal", "Life Tracking", "📖", "Reflections, voice memos, mood telemetry, and daily streaks", listOf("journal_entries", "journal_tags"), count("journal_entries")),
            SegmentInfo(7, "Alarms & Reminders", "Core", "⏰", "Alarm manager triggers, audio cues, and Doze-resistant timers", listOf("alarms", "reminder_alerts"), count("alarms")),
            SegmentInfo(8, "Health & Metrics", "Life Tracking", "❤️", "Vitals, workouts, body telemetry, sleep, and nutrition", listOf("health_metrics", "body_stats"), 0),
            SegmentInfo(9, "Habits & Streaks", "Life Tracking", "⚡", "Daily habit tracking, consistency streaks, and micro-habits", listOf("habits", "habit_logs"), 0),
            SegmentInfo(10, "Contacts & Network", "Core", "👥", "Key contacts, relationships, and interaction notes", listOf("contacts", "interactions"), 0),
            SegmentInfo(11, "Reading & Knowledge", "Reflection", "📚", "Books, articles, bookmarks, and synthesis notes", listOf("reading_items", "bookmarks"), 0),
            SegmentInfo(12, "Inventory & Assets", "Finance", "📦", "Physical assets, warranty tracking, and serial numbers", listOf("inventory_items", "warranty_records"), 0),
            SegmentInfo(13, "Travel & Itinerary", "Life Tracking", "✈️", "Trips, boarding passes, flight bookings, and itineraries", listOf("trips", "travel_legs"), 0),
            SegmentInfo(14, "Media & Entertainment", "Reflection", "🎬", "Watchlists, media consumption logs, and podcasts", listOf("media_items", "playlists"), 0),
            SegmentInfo(15, "Automations & Device", "Core", "⚙️", "Trigger rules, device sensor states, and hardware events", listOf("automation_rules", "device_states"), 0),
            SegmentInfo(16, "System Audit & Logs", "Core", "🛡️", "Deterministic action ledger, verification audits, and runtime state", listOf("audit", "telemetry_logs"), count("audit"))
        )
    }

    fun getSegmentDetails(segmentNumber: Int): List<String> {
        val list = mutableListOf<String>()
        val db = dbHelper.readableDatabase
        try {
            when (segmentNumber) {
                2 -> {
                    val cursor = db.rawQuery("SELECT description, type, amount_minor, payment_mode, strftime('%Y-%m-%d %H:%M', created_at / 1000, 'unixepoch') FROM finance_transactions ORDER BY id DESC LIMIT 20", null)
                    cursor.use {
                        while (it.moveToNext()) {
                            val sign = if (it.getString(1).equals("CREDIT", true)) "+" else "-"
                            list.add("${it.getString(0)} · $sign₹${it.getLong(2) / 100.0} (${it.getString(3)}) · ${it.getString(4)}")
                        }
                    }
                }
                3 -> {
                    val cursor = db.rawQuery("SELECT title, priority, status, category FROM tasks ORDER BY id DESC LIMIT 20", null)
                    cursor.use {
                        while (it.moveToNext()) {
                            list.add("[${it.getString(2)}] ${it.getString(0)} (Priority: ${it.getString(1)}, ${it.getString(3)})")
                        }
                    }
                }
                4 -> {
                    val cursor = db.rawQuery("SELECT name, status, task_count FROM projects ORDER BY id DESC LIMIT 20", null)
                    cursor.use {
                        while (it.moveToNext()) {
                            list.add("${it.getString(0)} · Status: ${it.getString(1)} (${it.getInt(2)} tasks)")
                        }
                    }
                }
                5 -> {
                    val cursor = db.rawQuery("SELECT title, progress_percent, status FROM goals ORDER BY id DESC LIMIT 20", null)
                    cursor.use {
                        while (it.moveToNext()) {
                            list.add("${it.getString(0)} · ${it.getInt(1)}% (${it.getString(2)})")
                        }
                    }
                }
                6 -> {
                    val cursor = db.rawQuery("SELECT title, content, mood, strftime('%Y-%m-%d %H:%M', created_at / 1000, 'unixepoch') FROM journal_entries ORDER BY id DESC LIMIT 20", null)
                    cursor.use {
                        while (it.moveToNext()) {
                            list.add("${it.getString(0)} [${it.getString(2)}]: ${it.getString(1).take(80)} (${it.getString(3)})")
                        }
                    }
                }
                7 -> {
                    val cursor = db.rawQuery("SELECT label, alarm_time, is_enabled FROM alarms ORDER BY id DESC LIMIT 20", null)
                    cursor.use {
                        while (it.moveToNext()) {
                            list.add("${it.getString(0)} · Enabled: ${it.getInt(2) == 1}")
                        }
                    }
                }
                16 -> {
                    val cursor = db.rawQuery("SELECT action, details, timestamp FROM audit ORDER BY id DESC LIMIT 20", null)
                    cursor.use {
                        while (it.moveToNext()) {
                            list.add("${it.getString(0)}: ${it.getString(1)}")
                        }
                    }
                }
                else -> {
                    list.add("Segment #$segmentNumber is provisioned in SQLite. Listening for voice entries.")
                }
            }
        } catch (e: Exception) {
            list.add("Error loading segment rows: ${e.message}")
        }
        return list
    }

    fun getSegmentDiagnostics(): List<Pair<String, Int>> {
        return getAllSegments().map { it.title to it.itemCount }
    }

    // Generic segment fetcher for SegmentDetailActivity
    fun getSegmentItems(segmentId: String): List<SegmentItem> {
        val list = mutableListOf<SegmentItem>()

        when (segmentId.lowercase()) {
            "tasks" -> {
                tasks.listPendingTasks(50).forEach {
                    list.add(SegmentItem(it.id, it.title, "Due: ${it.humanDueTime} · ${it.priority}", it.category))
                }
            }
            "finance" -> {
                finance.listTransactions(50).forEach {
                    val sign = if (it.type.equals("CREDIT", true)) "+" else "-"
                    list.add(SegmentItem(it.id, "${it.description} (${sign}${it.formattedAmount})", "${it.paymentMode} · ${it.humanTime}", it.category))
                }
            }
            "journal", "notes" -> {
                journal.listNotes(50).forEach {
                    list.add(SegmentItem(it.id, it.title.ifBlank { "Journal Entry" }, it.content, "${it.mood} · ${it.humanTime}"))
                }
            }
            "alarms" -> {
                alarms.listAlarms(30).forEach {
                    list.add(SegmentItem(it.id, it.label, it.humanTime, if (it.isEnabled) "ACTIVE" else "OFF"))
                }
            }
            "goals" -> {
                listGoals().forEach {
                    list.add(SegmentItem(it.id, it.name, "${it.progressPercent}% Completed", it.status))
                }
            }
            "projects" -> {
                listProjects().forEach {
                    list.add(SegmentItem(it.id, it.name, "${it.taskCount} tasks active", it.status))
                }
            }
            else -> {
                list.add(SegmentItem(1L, "Data for $segmentId", "Database segment active and synchronized with Dexter engine.", "ACTIVE"))
            }
        }
        return list
    }

    fun getDailyBriefing(): DailyBriefing {
        val pendingCount = tasks.getTaskCounts().pending
        val balance = finance.getNetBalance()
        val alarmsCount = alarms.listAlarms().count { it.isEnabled }
        val dateString = java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault()).format(java.util.Date())

        val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }

        val topPriority = tasks.listPendingTasks(3).firstOrNull()?.title
        val speech = if (pendingCount > 0) {
            "$greeting. You have $pendingCount tasks pending today. Top priority: ${topPriority ?: "none"}."
        } else {
            "$greeting. You have no pending tasks today. All caught up!"
        }

        return DailyBriefing(
            greeting = greeting,
            dateString = dateString,
            speechText = speech,
            pendingTasksCount = pendingCount,
            activeAlarmsCount = alarmsCount,
            netBalancePaise = balance,
            topPriorityTask = topPriority,
            topTaskTitle = topPriority
        )
    }

    private fun notifyDataChanged() {
        _dataRevision.tryEmit(System.currentTimeMillis())
    }

    companion object {
        private val _dataRevision = MutableSharedFlow<Long>(extraBufferCapacity = 1)
        val dataRevision: SharedFlow<Long> = _dataRevision.asSharedFlow()

        @Volatile
        private var INSTANCE: DexterRepository? = null

        fun getInstance(context: Context): DexterRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DexterRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
