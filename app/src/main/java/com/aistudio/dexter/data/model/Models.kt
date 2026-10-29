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
            val isCurrency = metricUnit == "₹" || metricUnit == "$" || metricUnit == "€" || metricUnit == "£"
            val curr = if (isCurrency) "$metricUnit" + String.format(Locale.US, "%,.0f", currentAmount) else String.format(Locale.US, "%,.0f", currentAmount) + " $metricUnit"
            val tgt = if (isCurrency) "$metricUnit" + String.format(Locale.US, "%,.0f", targetAmount) else String.format(Locale.US, "%,.0f", targetAmount) + " $metricUnit"
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
    val backstory: String = "",
    val lastContactedText: String = "Recently"
) : Serializable {
    val initial: String
        get() {
            val parts = name.trim().split(" ")
            return if (parts.size >= 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
            } else {
                name.trim().take(1).uppercase().ifEmpty { "?" }
            }
        }
}

data class DexterBackstoryInfo(
    val codename: String = "DEXTER",
    val title: String = "Dexter Local Operating System",
    val version: String = "v1.0.0 (ARM64 Native)",
    val tagline: String = "Zero Cloud. Sovereign Storage. Local Intelligence.",
    val manifesto: String = "Dexter was forged with an unwavering belief: your thoughts, finances, calendar, and human connections belong to you alone. In a digital world dominated by surveillance silos, Dexter operates completely on your hardware with offline determinism, mathematical privacy, and zero telemetry.",
    val pillars: List<Pair<String, String>> = listOf(
        "🛡️ Sovereign SQLite WAL" to "Encrypted, fully local single source of truth. Zero remote calls or unverified syncs.",
        "🧠 Localhost Neural Inference" to "Needle 3 engine running on 127.0.0.1:8081 with 71MB RAM footprint.",
        "⚡ Deterministic Fast-Path" to "Sub-5ms intent resolution with strict parameter sanity checks.",
        "🔋 Battery Preservation" to "Optimized energy-threshold VAD achieving ≤3% standby battery drain/hour.",
        "👥 Meaningful Human Bonds" to "Personal CRM with backstory, memories, and touchpoint continuity."
    )
) : Serializable

data class InteractionItem(
    val id: Long = 0L,
    val personId: Long = 0L,
    val note: String = "",
    val interactionType: String = "Call",
    val interactionDate: String = "Today"
) : Serializable

data class HydrationItem(
    val id: Long = 0L,
    val amountMl: Int = 250,
    val loggedAt: Long = System.currentTimeMillis()
) : Serializable {
    val humanTime: String
        get() = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(loggedAt))
}

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
    val isDoneToday: Boolean = false,
    val habitType: String = "BUILD",
    val category: String = "Health",
    val quitReason: String = ""
) : Serializable {
    val isQuitHabit: Boolean get() = habitType.equals("QUIT", ignoreCase = true)
}

data class DocumentItem(
    val id: Long = 0L,
    val title: String = "",
    val source: String = "User Profile",
    val fileType: String = "DOC",
    val excerpt: String = "",
    val chunkCount: Int = 1,
    val content: String = "",
    val category: String = "Personal Profile",
    val isCoreMemory: Boolean = false,
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable {
    val humanDate: String
        get() = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(createdAt))

    val wordCount: Int
        get() {
            val text = if (content.isNotBlank()) content else excerpt
            return text.split(Regex("\\s+")).count { it.isNotBlank() }
        }

    val estimatedReadMinutes: Int
        get() = (wordCount / 200).coerceAtLeast(1)

    val cleanPreview: String
        get() = if (content.isNotBlank()) content.take(160) else excerpt.take(160)
}

data class CaptureItem(
    val id: Long = 0L,
    val noteText: String = "",
    val status: String = "UNPROCESSED",
    val sourceType: String = "TEXT",
    val categoryTag: String = "General",
    val scheduledWindow: String = "Ad-hoc",
    val routedTo: String = "",
    val capturedAt: Long = System.currentTimeMillis()
) : Serializable {
    val isProcessed: Boolean get() = status != "UNPROCESSED"

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

    val sourceIcon: String
        get() = when (sourceType.uppercase()) {
            "VOICE" -> "🎙️"
            "SCHEDULED_MORNING" -> "🌅"
            "SCHEDULED_EVENING" -> "🌙"
            "SCHEDULED_MIDDAY" -> "☀️"
            "CLIPBOARD" -> "📋"
            else -> "✍️"
        }

    val sourceLabel: String
        get() = when (sourceType.uppercase()) {
            "VOICE" -> "Voice Command"
            "SCHEDULED_MORNING" -> "Morning Intake"
            "SCHEDULED_EVENING" -> "Evening Reflection"
            "SCHEDULED_MIDDAY" -> "Midday Sync"
            "CLIPBOARD" -> "Clipboard"
            else -> "Quick Input"
        }
}

data class IntakeScheduleItem(
    val id: Long = 0L,
    val name: String = "",
    val scheduledTime: String = "07:30 AM",
    val targetType: String = "MORNING_BRIEFING",
    val isEnabled: Boolean = true,
    val promptQuestion: String = ""
) : Serializable {
    val icon: String
        get() = when (targetType) {
            "MORNING_BRIEFING" -> "🌅"
            "MIDDAY_CHECKPOINT" -> "☀️"
            "END_OF_DAY_SUMMARY" -> "🌙"
            else -> "⏰"
        }
}

data class AttentionItem(
    val id: Long = 0L,
    val title: String = "",
    val urgency: String = "Normal", // Critical, High, Normal, Info
    val category: String = "SYSTEM", // DEADLINE, FINANCE, HEALTH, SYSTEM, SECURITY
    val detail: String = "",
    val isAcknowledged: Boolean = false,
    val snoozedUntil: Long = 0L,
    val actionTarget: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable {
    val isSnoozed: Boolean get() = snoozedUntil > System.currentTimeMillis()

    val remainingSnoozeMinutes: Long
        get() = if (isSnoozed) ((snoozedUntil - System.currentTimeMillis()) / 60_000L).coerceAtLeast(1L) else 0L

    val urgencyColorHex: String
        get() = when (urgency.uppercase()) {
            "CRITICAL" -> "#FF1744"
            "HIGH", "URGENT" -> "#FF9100"
            "NORMAL" -> "#4FC3F7"
            else -> "#A0A5BD"
        }

    val categoryIcon: String
        get() = when (category.uppercase()) {
            "DEADLINE" -> "⏳"
            "FINANCE" -> "💳"
            "HEALTH" -> "💧"
            "SECURITY" -> "🛡️"
            else -> "⚡"
        }

    val humanTime: String
        get() {
            val now = System.currentTimeMillis()
            val diff = now - createdAt
            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000}m ago"
                diff < 86400_000 -> "Today at " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(createdAt))
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(createdAt))
            }
        }
}

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

data class MorningBriefingReport(
    val title: String = "🌅 Morning Briefing",
    val dateString: String = "",
    val executiveSummary: String = "",
    val weatherText: String = "28°C Sunny & Clear",
    // Previous Day's Summarized Inputs:
    val yesterdayCompletedTasks: List<TaskItem> = emptyList(),
    val yesterdayCompletedCount: Int = 0,
    val yesterdayPendingCount: Int = 0,
    val yesterdayEveningReflection: String = "",
    val yesterdayIntakeCount: Int = 0,
    val yesterdaySpentMinor: Long = 0L,
    val lastNightSleep: SleepItem? = null,
    val yesterdayWaterMl: Int = 2500,
    val habitsMaintainedCount: Int = 0,
    // Today's Schedules & Focus:
    val todayScheduleBlocks: List<ScheduleBlockItem> = emptyList(),
    val todayPriorityTasks: List<TaskItem> = emptyList(),
    val activeAlarms: List<AlarmItem> = emptyList(),
    val criticalAlerts: List<AttentionItem> = emptyList(),
    // Projects & Goals Report:
    val activeProjects: List<ProjectItem> = emptyList(),
    val activeGoals: List<GoalItem> = emptyList(),
    // Motivational Catalyst:
    val motivationalQuote: String = "Focus on the process, and the outcomes will take care of themselves.",
    val motivationalAuthor: String = "Marcus Aurelius"
) : Serializable {
    val formattedSpent: String
        get() = "₹" + String.format(Locale.US, "%,.2f", yesterdaySpentMinor / 100.0)

    val sleepSummary: String
        get() = lastNightSleep?.let {
            "${it.durationText} (${it.qualityLabel}, ${it.qualityRating}★)"
        } ?: "7h 20m (Restful, 5★)"
}

data class EndOfDaySummaryReport(
    val title: String = "🌙 End-of-Day Summary",
    val dateString: String = "",
    val executiveReflection: String = "",
    val tasksCompletedToday: List<TaskItem> = emptyList(),
    val completedTasksCount: Int = 0,
    val pendingTasksCount: Int = 0,
    val completionPercentage: Int = 0,
    val activeProjects: List<ProjectItem> = emptyList(),
    val activeGoals: List<GoalItem> = emptyList(),
    val waterMl: Int = 2400,
    val sleep: SleepItem? = null,
    val mood: MoodItem? = null,
    val habitsMaintainedCount: Int = 0,
    val eveningReflectionNote: String = "",
    val spentMinorToday: Long = 0L,
    val motivationalClosing: String = "Every step completed today compounds tomorrow. Rest deeply with an unburdened mind."
) : Serializable {
    val formattedSpent: String
        get() = "₹" + String.format(Locale.US, "%,.2f", spentMinorToday / 100.0)

    val sleepSummary: String
        get() = sleep?.let {
            "${it.durationText} (${it.qualityLabel}, ${it.qualityRating}★)"
        } ?: "7h 20m (Restful)"
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
