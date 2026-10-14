package com.aistudio.dexter.data.model

import java.io.Serializable

enum class Priority : Serializable {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL,
    SOMEDAY;

    companion object {
        fun fromString(value: String): Priority {
            return try {
                valueOf(value.uppercase())
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
                valueOf(value.uppercase())
            } catch (e: Exception) {
                PENDING
            }
        }
    }
}

data class TaskItem(
    val id: Long,
    val title: String,
    val priority: Priority = Priority.NORMAL,
    val status: TaskStatus = TaskStatus.PENDING,
    val category: String = "General",
    val estimatedMinutes: Int = 15,
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis() / 1000,
    val completedAt: Long? = null,
    val occurrencesCount: Int = 1
) : Serializable

data class ActionResult(
    val success: Boolean,
    val message: String,
    val intent: String = "",
    val errorCode: String? = null,
    val entityType: String? = null,
    val entityId: Long? = null,
    val affectedId: Long? = null,
    val data: Any? = null,
    val payload: Any? = null
) : Serializable

data class FinanceTransaction(
    val id: Long,
    val accountId: Long = 1,
    val amountMinor: Long,
    val description: String,
    val type: String = "DEBIT", // DEBIT or CREDIT
    val category: String = "General",
    val paymentMode: String = "UPI", // UPI, Cash, Card, Net Banking
    val referenceId: String = "",
    val createdAt: Long = System.currentTimeMillis() / 1000
) : Serializable {
    val amountRupees: Double get() = amountMinor / 100.0
    val isDebit: Boolean get() = type.equals("DEBIT", ignoreCase = true)
    val formattedRef: String get() = if (referenceId.isNotBlank()) referenceId else "REF#${id.toString().padStart(5, '0')}"
}

data class JournalEntry(
    val id: Long,
    val content: String,
    val source: String = "VOICE", // VOICE, TEXT, SYSTEM
    val title: String = "",
    val mood: String = "Reflective", // Grateful, Focused, Calm, Reflective, Energized, Tired
    val category: String = "Reflections", // Reflections, Gratitude, Daily Log, Ideas, Milestones, Work
    val createdAt: Long = System.currentTimeMillis() / 1000
) : Serializable {
    val wordCount: Int
        get() = if (content.isBlank()) 0 else content.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
}

data class GoalItem(
    val id: Long,
    val name: String,
    val targetValue: Double = 100.0,
    val currentValue: Double = 0.0,
    val unit: String = "%",
    val status: String = "ACTIVE",
    val priority: Priority = Priority.NORMAL,
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis() / 1000
) : Serializable

data class ProjectItem(
    val id: Long,
    val name: String,
    val status: String = "ACTIVE",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis() / 1000
) : Serializable

data class AlarmItem(
    val id: Long,
    val alarmTime: Long,
    val label: String,
    val isEnabled: Boolean = true,
    val repeatDays: String = "DAILY",
    val createdAt: Long = System.currentTimeMillis() / 1000
) : Serializable

data class AuditLog(
    val id: Long = 0,
    val action: String = "",
    val details: String = "",
    val actionType: String = action,
    val entityType: String = "SYSTEM",
    val message: String = details,
    val timestamp: Long = System.currentTimeMillis() / 1000,
    val createdAt: Long = timestamp
) : Serializable

data class DailyBriefing(
    val pendingTasksCount: Int,
    val topTaskTitle: String?,
    val nextAlarmTime: Long?,
    val nextAlarmLabel: String?,
    val todaySpentMinor: Long,
    val completedTasksTodayCount: Int,
    val unreadJournalCount: Int = 0
) : Serializable {
    val summaryText: String
        get() {
            val tasks = "$pendingTasksCount tasks pending"
            val spent = "₹${todaySpentMinor / 100} spent today"
            val alarm = if (nextAlarmLabel != null) "next reminder: $nextAlarmLabel" else "no reminders"
            return "$tasks · $spent · $alarm"
        }
}

data class SegmentInfo(
    val segmentNumber: Int,
    val title: String,
    val category: String,
    val icon: String,
    val description: String,
    val allTables: List<String>,
    val itemCount: Int = 0
) : Serializable
