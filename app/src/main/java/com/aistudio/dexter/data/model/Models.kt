package com.aistudio.dexter.data.model

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Priority : Serializable {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL,
    SOMEDAY;

    companion object {
        fun fromString(value: String): Priority {
            return try {
                valueOf(value.uppercase().trim())
            } catch (e: Exception) {
                NORMAL
            }
        }
    }
}

enum class TaskStatus : Serializable {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    companion object {
        fun fromString(value: String): TaskStatus {
            return try {
                valueOf(value.uppercase().trim())
            } catch (e: Exception) {
                PENDING
            }
        }
    }
}

data class TaskCounts(
    val pending: Int = 0,
    val completed: Int = 0,
    val total: Int = 0
) : Serializable

data class SegmentInfo(
    val segmentNumber: Int,
    val title: String,
    val category: String,
    val icon: String,
    val description: String,
    val allTables: List<String>,
    val itemCount: Int = 0
) : Serializable

data class SegmentItem(
    val id: Long = 0L,
    val title: String = "",
    val subtitle: String = "",
    val badge: String = ""
) : Serializable

data class TaskItem(
    val id: Long = 0L,
    val title: String,
    val priority: Priority = Priority.NORMAL,
    val projectId: Long? = null,
    val status: TaskStatus = TaskStatus.PENDING,
    val dueTimestamp: Long? = null,
    val dueAt: Long? = dueTimestamp,
    val scheduledStart: Long? = null,
    val scheduledEnd: Long? = null,
    val estimatedDurationSeconds: Long? = null,
    val durationSeconds: Long? = estimatedDurationSeconds,
    val completionSource: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = "",
    val category: String = "General"
) : Serializable {
    val isCompleted: Boolean
        get() = status == TaskStatus.COMPLETED

    val humanDueTime: String
        get() {
            val due = dueAt ?: dueTimestamp ?: return "No due date"
            val now = System.currentTimeMillis()
            val diff = due - now
            return when {
                diff < 0 -> "Overdue"
                diff < 3600_000 -> "Due in ${(diff / 60_000).coerceAtLeast(1)}m"
                diff < 86400_000 -> "Due in ${diff / 3600_000}h"
                else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(due))
            }
        }

    val humanTime: String
        get() = humanDueTime
}

data class FinanceTransaction(
    val id: Long = 0L,
    val amountMinor: Long,
    val type: String = "DEBIT",
    val category: String = "General",
    val description: String = "",
    val paymentMode: String = "UPI",
    val referenceId: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val actorType: String = "USER"
) : Serializable {
    val amountRupees: Double get() = amountMinor / 100.0

    val isDebit: Boolean get() = type.equals("DEBIT", true)

    val formattedRef: String
        get() = if (referenceId.isNotBlank()) "Ref: $referenceId" else "UPI / TXN-${id}"

    val formattedAmount: String
        get() = "₹" + String.format(Locale.US, "%,.2f", amountRupees)

    val humanTime: String
        get() {
            val now = System.currentTimeMillis()
            val diff = now - createdAt
            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000}m ago"
                diff < 86400_000 -> "Today at " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(createdAt))
                diff < 172800_000 -> "Yesterday"
                else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(createdAt))
            }
        }
}

data class JournalEntry(
    val id: Long = 0L,
    val title: String = "",
    val content: String = "",
    val mood: String = "Calm",
    val category: String = "Reflections",
    val source: String = "VOICE",
    val createdAt: Long = System.currentTimeMillis(),
    val actorType: String = "USER"
) : Serializable {
    val humanTime: String
        get() {
            val now = System.currentTimeMillis()
            val diff = now - createdAt
            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000}m ago"
                diff < 86400_000 -> "Today at " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(createdAt))
                diff < 172800_000 -> "Yesterday"
                else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(createdAt))
            }
        }

    val wordCount: Int
        get() = content.trim().split(Regex("\\s+")).count { it.isNotBlank() }
}

data class GoalItem(
    val id: Long = 0L,
    val name: String = "",
    val title: String = name,
    val priority: Priority = Priority.NORMAL,
    val targetDate: Long = 0L,
    val progressPercent: Int = 0,
    val status: String = "IN_PROGRESS"
) : Serializable

data class ProjectItem(
    val id: Long = 0L,
    val name: String = "",
    val status: String = "ACTIVE",
    val colorHex: String = "#4FC3F7",
    val taskCount: Int = 0
) : Serializable

data class AlarmItem(
    val id: Long = 0L,
    val alarmTime: Long,
    val label: String,
    val repeatDays: String = "",
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable {
    val humanTime: String
        get() = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(alarmTime))
}

data class AuditLog(
    val id: Long = 0L,
    val action: String,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val actorType: String = "USER",
    val entityType: String = "GENERAL"
) : Serializable {
    val actionType: String get() = action
    val createdAt: Long get() = timestamp
    val message: String get() = details
}

data class DailyBriefing(
    val greeting: String = "Hello",
    val dateString: String = "",
    val speechText: String = "",
    val pendingTasksCount: Int = 0,
    val activeAlarmsCount: Int = 0,
    val netBalancePaise: Long = 0L,
    val topPriorityTask: String? = null,
    val topTaskTitle: String? = topPriorityTask,
    val nextAlarmTime: Long? = null,
    val nextAlarmLabel: String? = null,
    val todaySpentMinor: Long = 0L,
    val completedTasksTodayCount: Int = 0
) : Serializable {
    val summaryText: String get() = speechText
}

data class ActionResult(
    val success: Boolean,
    val intent: String,
    val message: String,
    val entityType: String = "GENERAL",
    val entityId: Long = 0L,
    val errorCode: String? = null,
    val data: Map<String, Any?> = emptyMap()
) : Serializable
