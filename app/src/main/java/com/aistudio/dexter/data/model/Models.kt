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
    val category: String = "Financial",
    val priority: Priority = Priority.NORMAL,
    val targetDate: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val metricUnit: String = "₹",
    val progressPercent: Int = 0,
    val status: String = "ACTIVE"
) : Serializable {
    val progressLabel: String
        get() = if (targetAmount > 0) {
            val curr = if (metricUnit == "₹") "₹" + String.format(Locale.US, "%,.0f", currentAmount) else "$currentAmount $metricUnit"
            val tgt = if (metricUnit == "₹") "₹" + String.format(Locale.US, "%,.0f", targetAmount) else "$targetAmount $metricUnit"
            "$curr / $tgt ($progressPercent%)"
        } else {
            "$progressPercent% Completed"
        }
}

data class ProjectItem(
    val id: Long = 0L,
    val name: String = "",
    val title: String = name,
    val description: String = "",
    val deadline: String = "",
    val status: String = "ACTIVE",
    val colorHex: String = "#4FC3F7",
    val progressPercent: Int = 0,
    val taskCount: Int = 0
) : Serializable

data class MilestoneItem(
    val id: Long = 0L,
    val projectId: Long = 0L,
    val title: String = "",
    val isCompleted: Boolean = false
) : Serializable

data class AlarmItem(
    val id: Long = 0L,
    val alarmTime: Long = 0L,
    val label: String = "",
    val repeatDays: String = "Mon, Wed, Fri",
    val isEnabled: Boolean = true,
    val vibrate: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable {
    val humanTime: String
        get() = if (alarmTime > 0) SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(alarmTime)) else "06:30 AM"
}

data class ScheduleBlockItem(
    val id: Long = 0L,
    val title: String = "",
    val startTime: String = "10:00 AM",
    val endTime: String = "11:00 AM",
    val category: String = "Deep Work",
    val colorHex: String = "#4FC3F7",
    val location: String = ""
) : Serializable

data class PersonItem(
    val id: Long = 0L,
    val name: String = "",
    val relationType: String = "Friend",
    val phone: String = "",
    val email: String = "",
    val birthday: String = "",
    val notes: String = "",
    val lastContactedText: String = "Recently"
) : Serializable {
    val initial: String
        get() = name.trim().take(1).uppercase().ifEmpty { "?" }
}

data class InteractionItem(
    val id: Long = 0L,
    val personId: Long = 0L,
    val note: String = "",
    val interactionType: String = "Call",
    val interactionDate: String = "Today"
) : Serializable

data class SleepItem(
    val id: Long = 0L,
    val bedtime: String = "11:30 PM",
    val waketime: String = "06:50 AM",
    val durationText: String = "7h 20m",
    val qualityRating: Int = 5,
    val qualityLabel: String = "Restful",
    val notes: String = ""
) : Serializable

data class MoodItem(
    val id: Long = 0L,
    val mood: String = "Calm",
    val energyLevel: String = "High",
    val note: String = "",
    val loggedAt: Long = System.currentTimeMillis()
) : Serializable {
    val humanTime: String
        get() = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(loggedAt))
}

data class HabitItem(
    val id: Long = 0L,
    val name: String = "",
    val frequency: String = "Daily",
    val reminderTime: String = "08:00 AM",
    val streakCount: Int = 0,
    val isDoneToday: Boolean = false
) : Serializable

data class DocumentItem(
    val id: Long = 0L,
    val title: String = "",
    val source: String = "Manual.pdf",
    val fileType: String = "PDF",
    val excerpt: String = "",
    val chunkCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable {
    val humanDate: String
        get() = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(createdAt))
}

data class CaptureItem(
    val id: Long = 0L,
    val noteText: String = "",
    val status: String = "UNPROCESSED",
    val capturedAt: Long = System.currentTimeMillis()
) : Serializable {
    val humanTime: String
        get() {
            val now = System.currentTimeMillis()
            val diff = now - capturedAt
            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000}m ago"
                diff < 86400_000 -> "Today at " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(capturedAt))
                diff < 172800_000 -> "Yesterday"
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(capturedAt))
            }
        }
}

data class AttentionItem(
    val id: Long = 0L,
    val title: String = "",
    val urgency: String = "Normal", // Critical, Normal, Info
    val isAcknowledged: Boolean = false,
    val snoozedUntil: Long = 0L
) : Serializable

data class BriefingItem(
    val id: Long = 0L,
    val title: String = "",
    val summaryText: String = "",
    val dateString: String = "",
    val tasksCount: Int = 0,
    val spentMinor: Long = 0L,
    val weatherText: String = "28°C Sunny"
) : Serializable {
    val formattedSpent: String
        get() = "₹" + String.format(Locale.US, "%,.2f", spentMinor / 100.0)
}

data class RuleItem(
    val id: Long = 0L,
    val ruleText: String = "",
    val enforcementLevel: String = "Strict", // Strict, Warn
    val isEnabled: Boolean = true
) : Serializable

data class DiagnosticsInfo(
    val ramUsedMb: Int = 84,
    val ramBudgetMb: Int = 120,
    val dbSizeKb: Long = 324L,
    val walMode: Boolean = true,
    val modelStatus: String = "Needle 3 Offline (Ready)",
    val integrityStatus: String = "OK"
) : Serializable

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
    val humanTime: String
        get() = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
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
