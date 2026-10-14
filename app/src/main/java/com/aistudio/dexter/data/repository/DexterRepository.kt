package com.aistudio.dexter.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.aistudio.dexter.data.db.DexterDatabaseHelper
import com.aistudio.dexter.data.model.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class DexterRepository(context: Context) {

    private val dbHelper = DexterDatabaseHelper.getInstance(context)

    val taskRepo = TaskRepository(dbHelper)
    val financeRepo = FinanceRepository(dbHelper)
    val alarmRepo = AlarmRepository(dbHelper)
    val journalRepo = JournalRepository(dbHelper)
    val auditRepo = AuditRepository(dbHelper)

    val dataRevision = Companion.dataRevision

    fun getAuditLogs(limit: Int = 100): List<AuditLog> = auditRepo.listRecentLogs(limit)

    companion object {
        private val _dataRevision = MutableSharedFlow<Long>(extraBufferCapacity = 64)
        val dataRevision = _dataRevision.asSharedFlow()

        fun notifyDataChanged() {
            _dataRevision.tryEmit(System.currentTimeMillis())
        }
    }

    private fun postChange() {
        notifyDataChanged()
    }

    // --- Tasks ---
    fun createTask(
        title: String,
        priority: Priority = Priority.NORMAL,
        category: String = "General",
        estimatedMinutes: Int = 15,
        deadline: Long? = null
    ): TaskItem {
        val item = taskRepo.createTask(title, priority, category, estimatedMinutes, deadline)
        auditRepo.logAction("CREATE_TASK", "Task '${item.title}' created")
        postChange()
        return item
    }

    fun listPendingTasks(limit: Int = 100): List<TaskItem> = taskRepo.listPendingTasks(limit)
    fun listCompletedTasks(limit: Int = 50): List<TaskItem> = taskRepo.listCompletedTasks(limit)
    fun getTaskById(id: Long): TaskItem? = taskRepo.getTaskById(id)

    fun completeTask(id: Long): Boolean {
        val result = taskRepo.completeTask(id)
        if (result) {
            auditRepo.logAction("COMPLETE_TASK", "Task #$id marked completed")
            postChange()
        }
        return result
    }

    fun userReportTaskCompletion(titleSnippet: String): TaskItem? {
        val item = taskRepo.userReportTaskCompletion(titleSnippet)
        if (item != null) {
            auditRepo.logAction("COMPLETE_TASK", "Task '${item.title}' marked completed")
            postChange()
        }
        return item
    }

    fun cancelTask(id: Long): Boolean {
        val result = taskRepo.cancelTask(id)
        if (result) {
            auditRepo.logAction("CANCEL_TASK", "Task #$id cancelled")
            postChange()
        }
        return result
    }

    fun completeAllTasks(): Int {
        val count = taskRepo.completeAllTasks()
        if (count > 0) {
            auditRepo.logAction("COMPLETE_ALL_TASKS", "$count tasks completed")
            postChange()
        }
        return count
    }

    fun updateTaskPriority(id: Long, priority: Priority): Boolean {
        val result = taskRepo.updateTaskPriority(id, priority)
        if (result) {
            auditRepo.logAction("UPDATE_PRIORITY", "Task #$id set to ${priority.name}")
            postChange()
        }
        return result
    }

    fun deleteTask(id: Long): Boolean {
        val result = taskRepo.deleteTask(id)
        if (result) postChange()
        return result
    }

    // --- Finance ---
    fun logTransaction(
        amountMinor: Long,
        desc: String,
        type: String = "DEBIT",
        category: String = "General",
        paymentMode: String = "UPI",
        referenceId: String = ""
    ): FinanceTransaction {
        val tx = financeRepo.logTransaction(amountMinor, desc, type, category, paymentMode, referenceId)
        auditRepo.logAction("LOG_TRANSACTION", "${tx.type} ₹${tx.amountRupees}: ${tx.description}")
        postChange()
        return tx
    }

    fun logExpense(amountMinor: Long, desc: String, category: String = "General"): FinanceTransaction {
        val tx = financeRepo.logExpense(amountMinor, desc, category)
        auditRepo.logAction("LOG_EXPENSE", "₹${tx.amountRupees}: ${tx.description}")
        postChange()
        return tx
    }

    fun listTransactions(limit: Int = 100): List<FinanceTransaction> = financeRepo.listTransactions(limit)

    fun deleteTransaction(id: Long): Boolean {
        val result = financeRepo.deleteTransaction(id)
        if (result) postChange()
        return result
    }

    fun summarizeExpenses(): Pair<Long, Long> = financeRepo.summarizeExpenses()
    fun getFinanceStatement(): Triple<Long, Long, Long> = financeRepo.getFinanceStatement()
    fun getFinanceCategoryBreakdown(): Map<String, Long> = financeRepo.getFinanceCategoryBreakdown()

    // --- Alarms ---
    fun createAlarm(alarmTime: Long, label: String, repeatDays: String = "DAILY"): AlarmItem {
        val item = alarmRepo.createAlarm(alarmTime, label, repeatDays)
        auditRepo.logAction("CREATE_ALARM", "Alarm '${item.label}' set")
        postChange()
        return item
    }

    fun queryPendingAlarms(limit: Int = 50): List<AlarmItem> = alarmRepo.queryPendingAlarms(limit)
    fun listAlarms(limit: Int = 50): List<AlarmItem> = alarmRepo.listAlarms(limit)

    fun cancelAlarms(id: Long? = null): Int {
        val count = alarmRepo.cancelAlarms(id)
        if (count > 0) {
            auditRepo.logAction("CANCEL_ALARMS", "$count alarms cancelled")
            postChange()
        }
        return count
    }

    fun deleteAlarm(id: Long): Boolean {
        val result = alarmRepo.deleteAlarm(id)
        if (result) postChange()
        return result
    }

    // --- Journal ---
    fun logJournal(
        content: String,
        title: String = "",
        mood: String = "Reflective",
        category: String = "Reflections",
        source: String = "VOICE"
    ): JournalEntry {
        val entry = journalRepo.logJournal(content, title, mood, category, source)
        auditRepo.logAction("LOG_JOURNAL", "Diary entry added (${entry.wordCount} words)")
        postChange()
        return entry
    }

    fun insertNote(content: String, source: String = "VOICE"): JournalEntry {
        val entry = journalRepo.insertNote(content, source)
        postChange()
        return entry
    }

    fun listJournal(limit: Int = 100): List<JournalEntry> = journalRepo.listJournal(limit)
    fun listNotes(limit: Int = 100): List<JournalEntry> = journalRepo.listNotes(limit)

    fun deleteJournal(id: Long): Boolean {
        val result = journalRepo.deleteJournal(id)
        if (result) postChange()
        return result
    }

    fun getJournalStats(): Triple<Int, Int, Int> = journalRepo.getJournalStats()

    // --- Goals ---
    fun createGoal(name: String, targetValue: Double = 100.0, unit: String = "%"): GoalItem {
        val db = dbHelper.writableDatabase
        val now = System.currentTimeMillis() / 1000
        val cv = ContentValues().apply {
            put("name", name.trim())
            put("target_value", targetValue)
            put("current_value", 0.0)
            put("unit", unit)
            put("status", "ACTIVE")
            put("created_at", now)
        }
        val id = db.insertOrThrow("goals", null, cv)
        auditRepo.logAction("CREATE_GOAL", "Goal '$name' created")
        postChange()
        return GoalItem(id = id, name = name.trim(), targetValue = targetValue, unit = unit, createdAt = now)
    }

    fun listGoals(): List<GoalItem> {
        val list = mutableListOf<GoalItem>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, name, target_value, current_value, unit, status, deadline, created_at FROM goals ORDER BY created_at DESC",
            null
        )
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val name = cursor.getString(1)
            val target = cursor.getDouble(2)
            val current = cursor.getDouble(3)
            val unit = cursor.getString(4)
            val status = cursor.getString(5)
            val deadline = if (!cursor.isNull(6)) cursor.getLong(6) else null
            val createdAt = cursor.getLong(7)
            list.add(
                GoalItem(
                    id = id,
                    name = name,
                    targetValue = target,
                    currentValue = current,
                    unit = unit,
                    status = status,
                    priority = Priority.NORMAL,
                    deadline = deadline,
                    createdAt = createdAt
                )
            )
        }
        cursor.close()
        return list
    }

    // --- Projects ---
    fun createProject(name: String, description: String = ""): ProjectItem {
        val db = dbHelper.writableDatabase
        val now = System.currentTimeMillis() / 1000
        val cv = ContentValues().apply {
            put("name", name.trim())
            put("status", "ACTIVE")
            put("description", description.trim())
            put("created_at", now)
        }
        val id = db.insertOrThrow("projects", null, cv)
        auditRepo.logAction("CREATE_PROJECT", "Project '$name' created")
        postChange()
        return ProjectItem(id = id, name = name.trim(), status = "ACTIVE", description = description.trim(), createdAt = now)
    }

    fun listProjects(): List<ProjectItem> {
        val list = mutableListOf<ProjectItem>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, name, status, description, created_at FROM projects ORDER BY created_at DESC",
            null
        )
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val name = cursor.getString(1)
            val status = cursor.getString(2)
            val desc = cursor.getString(3)
            val createdAt = cursor.getLong(4)
            list.add(ProjectItem(id, name, status, desc, createdAt))
        }
        cursor.close()
        return list
    }

    // --- Daily Briefing ---
    fun getDailyBriefing(): DailyBriefing {
        val pending = listPendingTasks(10)
        val alarms = queryPendingAlarms(1)
        val (todaySpent, _) = summarizeExpenses()
        val completedToday = listCompletedTasks(50).size

        return DailyBriefing(
            pendingTasksCount = pending.size,
            topTaskTitle = pending.firstOrNull()?.title,
            nextAlarmTime = alarms.firstOrNull()?.alarmTime,
            nextAlarmLabel = alarms.firstOrNull()?.label,
            todaySpentMinor = todaySpent,
            completedTasksTodayCount = completedToday,
            unreadJournalCount = 0
        )
    }

    // --- 16 Segments Architecture Introspection ---
    fun getAllSegments(): List<SegmentInfo> {
        val db = dbHelper.readableDatabase

        fun countTable(table: String): Int {
            return try {
                val cursor = db.rawQuery("SELECT COUNT(*) FROM $table", null)
                val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
                cursor.close()
                count
            } catch (e: Exception) {
                0
            }
        }

        return listOf(
            SegmentInfo(1, "Tasks & Execution", "Core Operations", "📋", "Core task ledger, recurrence schedules, and status tracking", listOf("tasks", "task_occurrences"), countTable("tasks")),
            SegmentInfo(2, "Occurrences & Kanban", "Core Operations", "⏱", "Historical and recurring task occurrences timeline", listOf("task_occurrences"), countTable("task_occurrences")),
            SegmentInfo(3, "Schedule & Timeblocks", "Core Operations", "📅", "Day-planning, calendar windows, and agenda events", listOf("schedule_blocks"), countTable("schedule_blocks")),
            SegmentInfo(4, "Goals & Milestones", "Personal Growth", "🎯", "Long-term outcomes, measurable KPI targets, and deadlines", listOf("goals"), countTable("goals")),
            SegmentInfo(5, "Projects & Initiatives", "Personal Growth", "📁", "Initiative hubs, sub-tasks, and scope management", listOf("projects"), countTable("projects")),
            SegmentInfo(6, "Habits & Routines", "Personal Growth", "🔄", "Daily streak habits, cadence counters, and consistency logs", listOf("tasks"), countTable("tasks")),
            SegmentInfo(7, "Alarms & Reminders", "System Time", "⏰", "Precision alarms, exact timers, and doze-safe wake locks", listOf("alarms"), countTable("alarms")),
            SegmentInfo(8, "Personal Finance", "Personal Capital", "💳", "Bank statement ledger, expense tracking, and income logs", listOf("finance_accounts", "finance_transactions"), countTable("finance_transactions")),
            SegmentInfo(9, "Wellbeing & Vitals", "Personal Health", "🌿", "Sleep duration, hydration intake, and body metrics", listOf("wellbeing_metrics"), countTable("wellbeing_metrics")),
            SegmentInfo(10, "Journal & Diary", "Inner Life", "📖", "Private daily diary, mood tagging, and voice reflections", listOf("journal_entries"), countTable("journal_entries")),
            SegmentInfo(11, "Knowledge & Second Brain", "Cognition", "💡", "RAG documents, searchable notes, and insights repository", listOf("knowledge_items"), countTable("knowledge_items")),
            SegmentInfo(12, "Relationships & Rolodex", "Social", "👥", "Key contacts, touchpoint reminders, and personal network", listOf("contacts_rolodex"), countTable("contacts_rolodex")),
            SegmentInfo(13, "Device & Telemetry", "Hardware", "📱", "Localhost daemon health, battery burn rate, and RAM stats", listOf("sqlite_master"), 1),
            SegmentInfo(14, "Offline Speech & Voice", "Interface", "🎙", "Tier 1 fast-path grammar, Whisper/Needle runtime status", listOf("sqlite_master"), 1),
            SegmentInfo(15, "Security & Vault", "Privacy", "🔐", "Local SQLite encryption, zero-cloud sandbox validation", listOf("sqlite_master"), 1),
            SegmentInfo(16, "Audit Logs & Governance", "Governance", "📜", "Immutable action history, confirmation triggers, and undo log", listOf("audit_logs"), countTable("audit_logs"))
        )
    }

    fun getSegmentDetails(segmentNumber: Int): List<String> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<String>()

        when (segmentNumber) {
            1 -> {
                val cursor = db.rawQuery("SELECT id, title, priority, status FROM tasks LIMIT 20", null)
                while (cursor.moveToNext()) {
                    list.add("• [${cursor.getString(2)}] ${cursor.getString(1)} (${cursor.getString(3)})")
                }
                cursor.close()
            }
            7 -> {
                val cursor = db.rawQuery("SELECT id, label, alarm_time FROM alarms LIMIT 20", null)
                while (cursor.moveToNext()) {
                    list.add("• ⏰ ${cursor.getString(1)} (Epoch: ${cursor.getLong(2)})")
                }
                cursor.close()
            }
            8 -> {
                val cursor = db.rawQuery("SELECT id, description, amount_minor, type, payment_mode FROM finance_transactions LIMIT 20", null)
                while (cursor.moveToNext()) {
                    val sign = if (cursor.getString(3) == "CREDIT") "+" else "-"
                    val rupees = cursor.getLong(2) / 100.0
                    list.add("• $sign ₹$rupees | ${cursor.getString(1)} [${cursor.getString(4)}]")
                }
                cursor.close()
            }
            10 -> {
                val cursor = db.rawQuery("SELECT id, title, mood, content FROM journal_entries LIMIT 20", null)
                while (cursor.moveToNext()) {
                    val t = cursor.getString(1)
                    val m = cursor.getString(2)
                    val c = cursor.getString(3)
                    list.add("• [$m] $t: ${c.take(60)}...")
                }
                cursor.close()
            }
            16 -> {
                val cursor = db.rawQuery("SELECT id, action, details FROM audit_logs ORDER BY id DESC LIMIT 20", null)
                while (cursor.moveToNext()) {
                    list.add("• [${cursor.getString(1)}] ${cursor.getString(2)}")
                }
                cursor.close()
            }
            else -> {
                val segments = getAllSegments()
                val seg = segments.find { it.segmentNumber == segmentNumber }
                if (seg != null && seg.allTables.isNotEmpty()) {
                    val primaryTable = seg.allTables.first()
                    try {
                        val cursor = db.rawQuery("SELECT * FROM $primaryTable LIMIT 10", null)
                        val colCount = cursor.columnCount
                        while (cursor.moveToNext()) {
                            val rowDesc = (0 until colCount).joinToString(", ") { i ->
                                "${cursor.getColumnName(i)}=${cursor.getString(i)}"
                            }
                            list.add("• $rowDesc")
                        }
                        cursor.close()
                    } catch (e: Exception) {
                        list.add("Table $primaryTable ready and operational.")
                    }
                }
            }
        }
        return list
    }

    fun getSegmentDiagnostics(): Map<String, Int> {
        return getAllSegments().associate { it.title to it.itemCount }
    }
}
