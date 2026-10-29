package com.aistudio.dexter.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.aistudio.dexter.data.db.DexterDatabaseHelper
import com.aistudio.dexter.data.model.Priority
import com.aistudio.dexter.data.model.TaskCounts
import com.aistudio.dexter.data.model.TaskItem
import com.aistudio.dexter.data.model.TaskStatus

class TaskRepository(dbHelper: DexterDatabaseHelper) : BaseRepository(dbHelper), TaskDataSource {

    constructor(context: Context) : this(DexterDatabaseHelper.getInstance(context))

    override fun getTaskById(taskId: Long): TaskItem? {
        val db = readableDb
        val cursor = db.rawQuery(
            "SELECT id, title, priority, project_id, status, due_at, scheduled_start, scheduled_end, estimated_duration_seconds, completion_source, created_at, notes, category FROM tasks WHERE id = ?",
            arrayOf(taskId.toString())
        )
        cursor.use {
            if (it.moveToFirst()) {
                return parseTaskCursor(it)
            }
        }
        return null
    }

    override fun createTask(
        title: String,
        priority: Priority,
        projectId: Long?,
        dueAt: Long?,
        scheduledStart: Long?,
        scheduledEnd: Long?,
        durationSeconds: Long?,
        actorType: String,
        notes: String,
        category: String
    ): TaskItem {
        val db = writableDb
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("title", title.trim())
            put("priority", priority.name)
            if (projectId != null) put("project_id", projectId)
            put("status", "PENDING")
            if (dueAt != null && dueAt > 0) put("due_at", dueAt)
            if (scheduledStart != null && scheduledStart > 0) put("scheduled_start", scheduledStart)
            if (scheduledEnd != null && scheduledEnd > 0) put("scheduled_end", scheduledEnd)
            if (durationSeconds != null && durationSeconds > 0) put("estimated_duration_seconds", durationSeconds)
            put("created_at", now)
            put("notes", notes)
            put("category", category)
        }

        db.beginTransaction()
        try {
            val id = db.insertOrThrow("tasks", null, values)
            val occValues = ContentValues().apply {
                put("task_id", id)
                put("status", "SCHEDULED")
                put("scheduled_start", scheduledStart ?: now)
                put("scheduled_end", scheduledEnd ?: (now + (durationSeconds ?: 1800) * 1000))
                put("created_at", now)
            }
            db.insert("task_occurrences", null, occValues)
            db.setTransactionSuccessful()

            return TaskItem(
                id = id,
                title = title.trim(),
                priority = priority,
                projectId = projectId,
                status = TaskStatus.PENDING,
                dueAt = dueAt,
                dueTimestamp = dueAt,
                scheduledStart = scheduledStart,
                scheduledEnd = scheduledEnd,
                estimatedDurationSeconds = durationSeconds,
                durationSeconds = durationSeconds,
                notes = notes,
                category = category,
                createdAt = now
            )
        } finally {
            db.endTransaction()
        }
    }

    fun createTask(
        title: String,
        priority: Priority,
        dueTimestamp: Long,
        notes: String,
        category: String
    ): TaskItem {
        return createTask(
            title = title,
            priority = priority,
            projectId = null,
            dueAt = dueTimestamp,
            scheduledStart = null,
            scheduledEnd = null,
            durationSeconds = null,
            actorType = "USER",
            notes = notes,
            category = category
        )
    }

    override fun completeTask(
        taskId: Long,
        completionSource: String,
        actorType: String
    ): Boolean {
        val db = writableDb
        val values = ContentValues().apply {
            put("status", "COMPLETED")
            put("completion_source", completionSource)
        }
        val rows = db.update("tasks", values, "id = ?", arrayOf(taskId.toString()))
        if (rows > 0) {
            val occValues = ContentValues().apply {
                put("status", "COMPLETED")
                put("completed_at", System.currentTimeMillis())
            }
            db.update("task_occurrences", occValues, "task_id = ?", arrayOf(taskId.toString()))
        }
        return rows > 0
    }

    fun userReportTaskCompletion(taskId: Long): Boolean {
        return completeTask(taskId, completionSource = "USER_REPORTED", actorType = "USER")
    }

    fun cancelTask(taskId: Long, actorType: String = "USER"): Boolean {
        val db = writableDb
        val values = ContentValues().apply {
            put("status", "CANCELLED")
        }
        val rows = db.update("tasks", values, "id = ?", arrayOf(taskId.toString()))
        if (rows > 0) {
            val occValues = ContentValues().apply {
                put("status", "CANCELLED")
            }
            db.update("task_occurrences", occValues, "task_id = ?", arrayOf(taskId.toString()))
        }
        return rows > 0
    }

    fun completeAllTasks(actorType: String = "USER"): Int {
        val db = writableDb
        val values = ContentValues().apply {
            put("status", "COMPLETED")
            put("completion_source", "DEXTER_RECORDED")
        }
        return db.update("tasks", values, "status = 'PENDING'", null)
    }

    override fun markTaskPending(taskId: Long): Boolean {
        val db = writableDb
        val values = ContentValues().apply {
            put("status", "PENDING")
            putNull("completion_source")
        }
        val rows = db.update("tasks", values, "id = ?", arrayOf(taskId.toString()))
        if (rows > 0) {
            val occValues = ContentValues().apply {
                put("status", "SCHEDULED")
                putNull("completed_at")
            }
            db.update("task_occurrences", occValues, "task_id = ?", arrayOf(taskId.toString()))
        }
        return rows > 0
    }

    override fun toggleTaskCompletion(taskId: Long): Boolean {
        val task = getTaskById(taskId) ?: return false
        return if (task.status == TaskStatus.COMPLETED) {
            markTaskPending(taskId)
        } else {
            completeTask(taskId)
        }
    }

    override fun deleteTask(taskId: Long): Boolean {
        val db = writableDb
        db.delete("task_occurrences", "task_id = ?", arrayOf(taskId.toString()))
        val rows = db.delete("tasks", "id = ?", arrayOf(taskId.toString()))
        return rows > 0
    }

    override fun getAllTasks(limit: Int): List<TaskItem> {
        val list = mutableListOf<TaskItem>()
        val db = readableDb
        val cursor = db.rawQuery(
            "SELECT id, title, priority, project_id, status, due_at, scheduled_start, scheduled_end, estimated_duration_seconds, completion_source, created_at, notes, category FROM tasks WHERE status != 'CANCELLED' ORDER BY CASE status WHEN 'PENDING' THEN 1 WHEN 'IN_PROGRESS' THEN 2 ELSE 3 END, CASE priority WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'NORMAL' THEN 3 ELSE 4 END, id DESC LIMIT ?",
            arrayOf(limit.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(parseTaskCursor(it))
            }
        }
        return list
    }

    override fun listPendingTasks(limit: Int): List<TaskItem> {
        val list = mutableListOf<TaskItem>()
        val db = readableDb
        val cursor = db.rawQuery(
            "SELECT id, title, priority, project_id, status, due_at, scheduled_start, scheduled_end, estimated_duration_seconds, completion_source, created_at, notes, category FROM tasks WHERE status = 'PENDING' ORDER BY CASE priority WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'NORMAL' THEN 3 ELSE 4 END, id DESC LIMIT ?",
            arrayOf(limit.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(parseTaskCursor(it))
            }
        }
        return list
    }

    override fun listCompletedTasks(limit: Int): List<TaskItem> {
        val list = mutableListOf<TaskItem>()
        val db = readableDb
        val cursor = db.rawQuery(
            "SELECT id, title, priority, project_id, status, due_at, scheduled_start, scheduled_end, estimated_duration_seconds, completion_source, created_at, notes, category FROM tasks WHERE status = 'COMPLETED' ORDER BY id DESC LIMIT ?",
            arrayOf(limit.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(parseTaskCursor(it))
            }
        }
        return list
    }

    override fun getTaskCounts(): TaskCounts {
        val db = readableDb
        var pending = 0
        var completed = 0

        val cursor = db.rawQuery("SELECT status, COUNT(*) FROM tasks GROUP BY status", null)
        cursor.use {
            while (it.moveToNext()) {
                val status = it.getString(0) ?: ""
                val count = it.getInt(1)
                if (status == "PENDING") pending = count
                else if (status == "COMPLETED") completed = count
            }
        }
        return TaskCounts(pending = pending, completed = completed, total = pending + completed)
    }

    private fun parseTaskCursor(cursor: Cursor): TaskItem {
        return TaskItem(
            id = cursor.getLong(0),
            title = cursor.getString(1) ?: "",
            priority = Priority.fromString(cursor.getString(2) ?: "NORMAL"),
            projectId = if (cursor.isNull(3)) null else cursor.getLong(3),
            status = TaskStatus.fromString(cursor.getString(4) ?: "PENDING"),
            dueAt = if (cursor.isNull(5)) null else cursor.getLong(5),
            dueTimestamp = if (cursor.isNull(5)) null else cursor.getLong(5),
            scheduledStart = if (cursor.isNull(6)) null else cursor.getLong(6),
            scheduledEnd = if (cursor.isNull(7)) null else cursor.getLong(7),
            estimatedDurationSeconds = if (cursor.isNull(8)) null else cursor.getLong(8),
            durationSeconds = if (cursor.isNull(8)) null else cursor.getLong(8),
            completionSource = if (cursor.isNull(9)) null else cursor.getString(9),
            createdAt = cursor.getLong(10),
            notes = cursor.getString(11) ?: "",
            category = cursor.getString(12) ?: "General"
        )
    }
}
