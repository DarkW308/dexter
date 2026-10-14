package com.aistudio.dexter.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.aistudio.dexter.data.db.DexterDatabaseHelper
import com.aistudio.dexter.data.model.Priority
import com.aistudio.dexter.data.model.TaskItem
import com.aistudio.dexter.data.model.TaskStatus

class TaskRepository(dbHelper: DexterDatabaseHelper) : BaseRepository(dbHelper) {

    constructor(context: Context) : this(DexterDatabaseHelper.getInstance(context))

    fun createTask(
        title: String,
        priority: Priority = Priority.NORMAL,
        category: String = "General",
        estimatedMinutes: Int = 15,
        deadline: Long? = null,
        projectId: Long? = null,
        dueAt: Long? = null,
        scheduledStart: Long? = null,
        scheduledEnd: Long? = null,
        durationSeconds: Long? = null,
        actorType: String = "USER"
    ): TaskItem {
        val db = writableDb
        val now = System.currentTimeMillis() / 1000
        val effectiveDeadline = deadline ?: dueAt ?: scheduledEnd
        val effectiveMinutes = if (durationSeconds != null && durationSeconds > 0) (durationSeconds / 60).toInt() else estimatedMinutes

        db.beginTransaction()
        return try {
            val cv = ContentValues().apply {
                put("title", title.trim())
                put("priority", priority.name)
                put("status", TaskStatus.PENDING.name)
                put("category", category)
                put("estimated_minutes", effectiveMinutes)
                if (effectiveDeadline != null) put("deadline", effectiveDeadline)
                put("created_at", now)
            }
            val id = db.insertOrThrow("tasks", null, cv)

            val occurrenceCv = ContentValues().apply {
                put("task_id", id)
                put("status", TaskStatus.PENDING.name)
                put("occurred_at", now)
            }
            db.insert("task_occurrences", null, occurrenceCv)

            db.setTransactionSuccessful()
            TaskItem(
                id = id,
                title = title.trim(),
                priority = priority,
                status = TaskStatus.PENDING,
                category = category,
                estimatedMinutes = effectiveMinutes,
                deadline = effectiveDeadline,
                createdAt = now
            )
        } finally {
            db.endTransaction()
        }
    }

    fun listPendingTasks(limit: Int = 100): List<TaskItem> {
        val list = mutableListOf<TaskItem>()
        val db = readableDb
        val cursor = db.rawQuery(
            """
            SELECT id, title, priority, status, category, estimated_minutes, deadline, created_at, completed_at
            FROM tasks
            WHERE status != 'COMPLETED' AND status != 'CANCELLED'
            ORDER BY 
                CASE priority 
                    WHEN 'CRITICAL' THEN 1 
                    WHEN 'HIGH' THEN 2 
                    WHEN 'NORMAL' THEN 3 
                    WHEN 'LOW' THEN 4 
                    ELSE 5 
                END,
                created_at DESC
            LIMIT ?
            """,
            arrayOf(limit.toString())
        )
        while (cursor.moveToNext()) {
            list.add(mapRowToTask(cursor))
        }
        cursor.close()
        return list
    }

    fun listCompletedTasks(limit: Int = 50): List<TaskItem> {
        val list = mutableListOf<TaskItem>()
        val db = readableDb
        val cursor = db.rawQuery(
            """
            SELECT id, title, priority, status, category, estimated_minutes, deadline, created_at, completed_at
            FROM tasks
            WHERE status = 'COMPLETED'
            ORDER BY completed_at DESC, created_at DESC
            LIMIT ?
            """,
            arrayOf(limit.toString())
        )
        while (cursor.moveToNext()) {
            list.add(mapRowToTask(cursor))
        }
        cursor.close()
        return list
    }

    fun getTaskById(id: Long): TaskItem? {
        val db = readableDb
        val cursor = db.rawQuery(
            "SELECT id, title, priority, status, category, estimated_minutes, deadline, created_at, completed_at FROM tasks WHERE id = ?",
            arrayOf(id.toString())
        )
        return try {
            if (cursor.moveToFirst()) mapRowToTask(cursor) else null
        } finally {
            cursor.close()
        }
    }

    fun completeTask(id: Long, completionSource: String = "DEXTER_RECORDED", actorType: String = "USER"): Boolean {
        val db = writableDb
        val now = System.currentTimeMillis() / 1000
        val cv = ContentValues().apply {
            put("status", TaskStatus.COMPLETED.name)
            put("completed_at", now)
        }
        val affected = db.update("tasks", cv, "id = ?", arrayOf(id.toString()))
        return affected > 0
    }

    fun cancelTask(id: Long, actorType: String = "USER"): Boolean {
        val db = writableDb
        val cv = ContentValues().apply {
            put("status", TaskStatus.CANCELLED.name)
        }
        val affected = db.update("tasks", cv, "id = ?", arrayOf(id.toString()))
        return affected > 0
    }

    fun completeAllTasks(actorType: String = "USER"): Int {
        val db = writableDb
        val now = System.currentTimeMillis() / 1000
        val cv = ContentValues().apply {
            put("status", TaskStatus.COMPLETED.name)
            put("completed_at", now)
        }
        return db.update("tasks", cv, "status != 'COMPLETED' AND status != 'CANCELLED'", null)
    }

    fun updateTaskPriority(id: Long, priority: Priority): Boolean {
        val db = writableDb
        val cv = ContentValues().apply {
            put("priority", priority.name)
        }
        return db.update("tasks", cv, "id = ?", arrayOf(id.toString())) > 0
    }

    fun deleteTask(id: Long): Boolean {
        val db = writableDb
        return db.delete("tasks", "id = ?", arrayOf(id.toString())) > 0
    }

    fun userReportTaskCompletion(taskId: Long): Boolean {
        return completeTask(taskId, completionSource = "USER_REPORT", actorType = "USER")
    }

    fun userReportTaskCompletion(titleSnippet: String): TaskItem? {
        val pending = listPendingTasks(50)
        val matched = pending.find { it.title.contains(titleSnippet, ignoreCase = true) }
            ?: pending.firstOrNull()
        if (matched != null) {
            completeTask(matched.id, completionSource = "USER_REPORT", actorType = "USER")
            return getTaskById(matched.id)
        }
        return null
    }

    private fun mapRowToTask(cursor: Cursor): TaskItem {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
        val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
        val priorityStr = cursor.getString(cursor.getColumnIndexOrThrow("priority"))
        val statusStr = cursor.getString(cursor.getColumnIndexOrThrow("status"))
        val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
        val estimated = cursor.getInt(cursor.getColumnIndexOrThrow("estimated_minutes"))
        val deadlineCol = cursor.getColumnIndex("deadline")
        val deadline = if (deadlineCol != -1 && !cursor.isNull(deadlineCol)) cursor.getLong(deadlineCol) else null
        val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
        val compCol = cursor.getColumnIndex("completed_at")
        val completedAt = if (compCol != -1 && !cursor.isNull(compCol)) cursor.getLong(compCol) else null

        return TaskItem(
            id = id,
            title = title,
            priority = Priority.fromString(priorityStr),
            status = TaskStatus.fromString(statusStr),
            category = category,
            estimatedMinutes = estimated,
            deadline = deadline,
            createdAt = createdAt,
            completedAt = completedAt
        )
    }
}
