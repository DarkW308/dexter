package com.aistudio.dexter.data.repository

import com.aistudio.dexter.data.model.Priority
import com.aistudio.dexter.data.model.TaskCounts
import com.aistudio.dexter.data.model.TaskItem

interface TaskDataSource {
    fun getTaskById(taskId: Long): TaskItem?
    fun createTask(
        title: String,
        priority: Priority = Priority.NORMAL,
        projectId: Long? = null,
        dueAt: Long? = null,
        scheduledStart: Long? = null,
        scheduledEnd: Long? = null,
        durationSeconds: Long? = null,
        actorType: String = "USER",
        notes: String = "",
        category: String = "General"
    ): TaskItem
    fun completeTask(taskId: Long, completionSource: String = "DEXTER_RECORDED", actorType: String = "USER"): Boolean
    fun markTaskPending(taskId: Long): Boolean
    fun toggleTaskCompletion(taskId: Long): Boolean
    fun deleteTask(taskId: Long): Boolean
    fun getAllTasks(limit: Int = 200): List<TaskItem>
    fun listPendingTasks(limit: Int = 100): List<TaskItem>
    fun listCompletedTasks(limit: Int = 100): List<TaskItem>
    fun getTaskCounts(): TaskCounts
}
