package com.aistudio.dexter.engine

import com.aistudio.dexter.data.model.Priority
import java.util.regex.Pattern

data class ParsedIntent(
    val intent: String,
    val parameters: Map<String, Any?> = emptyMap(),
    val confidence: Float = 0.99f,
    val requiresConfirmation: Boolean = false,
    val confirmationPrompt: String? = null,
    val isNegationOrClarification: Boolean = false
)

object DexterIntentRouter {

    fun stripWakeWord(input: String): String {
        val pattern = Pattern.compile("^\\s*(?:hey\\s+|ok\\s+|okay\\s+|hi\\s+|hello\\s+)?dexter[,\\s:]*\\s*", Pattern.CASE_INSENSITIVE)
        return pattern.matcher(input).replaceFirst("").trim()
    }

    fun parse(rawInput: String): ParsedIntent {
        val text = stripWakeWord(rawInput)
        if (text.isBlank()) {
            return ParsedIntent("EMPTY", isNegationOrClarification = true)
        }

        val lower = text.lowercase().trim()

        // 1. Negation Safety Check
        if (isNegation(lower)) {
            return ParsedIntent(
                intent = "NEGATION_SAFETY",
                isNegationOrClarification = true
            )
        }

        // 2. Daily Briefing
        if (lower.contains("briefing") || lower.contains("morning summary") || lower.contains("what do i need to know today") || lower == "daily summary") {
            return ParsedIntent("DAILY_BRIEFING")
        }

        // 3. Complete All Tasks
        if (lower.contains("complete all") || lower.contains("finish all") || lower.contains("mark all tasks complete") || lower.contains("complete everything")) {
            return ParsedIntent(
                intent = "COMPLETE_ALL_TASKS",
                requiresConfirmation = true,
                confirmationPrompt = "Mark all pending tasks complete. Confirm?"
            )
        }

        // 4. Pending / Completed Tasks queries
        if (lower.contains("pending tasks") || lower.contains("my tasks") || lower.contains("show tasks") || lower.contains("list tasks") || lower.contains("what do i need to do") || lower.contains("unfinished tasks")) {
            return ParsedIntent("LIST_PENDING_TASKS")
        }

        if (lower.contains("completed tasks") || lower.contains("what have i finished") || lower.contains("finished tasks") || lower.contains("done tasks")) {
            return ParsedIntent("LIST_COMPLETED_TASKS")
        }

        // 5. Complete Task (By ID or Title)
        val completeIdMatch = Pattern.compile("(?:complete|finish|mark)\\s+(?:task\\s+)?#?(\\d+)(?:\\s+done|\\s+as\\s+done)?", Pattern.CASE_INSENSITIVE).matcher(text)
        if (completeIdMatch.find()) {
            val id = completeIdMatch.group(1)?.toLongOrNull() ?: 0L
            return ParsedIntent(
                intent = "COMPLETE_TASK",
                parameters = mapOf("taskId" to id),
                requiresConfirmation = true,
                confirmationPrompt = "Complete task #$id. Confirm?"
            )
        }

        val completeTitleMatch = Pattern.compile("(?:mark|finish|complete)\\s+(?:task\\s+)?['\"]?([^'\"]+?)['\"]?\\s+(?:done|complete|as\\s+done)", Pattern.CASE_INSENSITIVE).matcher(text)
        if (completeTitleMatch.find()) {
            val title = completeTitleMatch.group(1)?.trim() ?: ""
            if (title.isNotEmpty() && !title.equals("all", ignoreCase = true)) {
                return ParsedIntent(
                    intent = "COMPLETE_TASK_TITLE",
                    parameters = mapOf("title" to title),
                    requiresConfirmation = true,
                    confirmationPrompt = "Complete task '$title'. Confirm?"
                )
            }
        }

        // 6. Cancel Task
        val cancelIdMatch = Pattern.compile("(?:cancel|delete|remove|abandon)\\s+(?:task\\s+)?#?(\\d+)", Pattern.CASE_INSENSITIVE).matcher(text)
        if (cancelIdMatch.find()) {
            val id = cancelIdMatch.group(1)?.toLongOrNull() ?: 0L
            return ParsedIntent(
                intent = "CANCEL_TASK",
                parameters = mapOf("taskId" to id),
                requiresConfirmation = true,
                confirmationPrompt = "Cancel task #$id. Confirm?"
            )
        }

        val cancelTitleMatch = Pattern.compile("(?:cancel|delete|remove)\\s+(?:task\\s+)?['\"]?([^'\"]+?)['\"]?$", Pattern.CASE_INSENSITIVE).matcher(text)
        if (cancelTitleMatch.find() && (lower.startsWith("cancel ") || lower.startsWith("delete ") || lower.startsWith("remove "))) {
            val candidate = cancelTitleMatch.group(1)?.trim() ?: ""
            if (candidate.isNotEmpty() && !candidate.contains("alarm") && !candidate.contains("reminder")) {
                return ParsedIntent(
                    intent = "CANCEL_TASK_TITLE",
                    parameters = mapOf("title" to candidate),
                    requiresConfirmation = true,
                    confirmationPrompt = "Cancel task '$candidate'. Confirm?"
                )
            }
        }

        // 7. Finance / Expense Logging
        val expenseMatch = extractExpense(text)
        if (expenseMatch != null) {
            val (amountRupees, desc) = expenseMatch
            val amountMinor = (amountRupees * 100).toLong()
            return ParsedIntent(
                intent = "LOG_EXPENSE",
                parameters = mapOf("amountMinor" to amountMinor, "description" to desc, "amountRupees" to amountRupees)
            )
        }

        // 8. Finance Summary
        if (lower.contains("how much did i spend") || lower.contains("total expenses") || lower.contains("my spending") || lower.contains("summarize expenses") || lower.contains("expense summary") || lower.contains("what did i spend")) {
            return ParsedIntent("SUMMARIZE_EXPENSES")
        }

        // 9. Journal / Note Logging
        val noteContent = extractJournalNote(text)
        if (noteContent != null) {
            return ParsedIntent(
                intent = "LOG_JOURNAL",
                parameters = mapOf("content" to noteContent)
            )
        }

        if (lower.contains("show notes") || lower.contains("read notes") || lower.contains("my notes") || lower.contains("show journal") || lower.contains("what did i journal")) {
            return ParsedIntent("LIST_JOURNAL")
        }

        // 10. Alarms & Reminders
        if (lower.contains("show alarms") || lower.contains("show reminders") || lower.contains("my reminders") || lower.contains("list alarms") || lower.contains("what reminders do i have")) {
            return ParsedIntent("LIST_ALARMS")
        }

        if (lower.contains("cancel alarms") || lower.contains("cancel all reminders") || lower.contains("turn off my alarms") || lower.contains("turn off alarms")) {
            return ParsedIntent(
                intent = "CANCEL_ALARMS",
                requiresConfirmation = true,
                confirmationPrompt = "Cancel all active alarms. Confirm?"
            )
        }

        val reminderMatch = extractReminder(text)
        if (reminderMatch != null) {
            val (label, epochSecs) = reminderMatch
            return ParsedIntent(
                intent = "CREATE_ALARM",
                parameters = mapOf("label" to label, "alarmTime" to epochSecs),
                requiresConfirmation = true,
                confirmationPrompt = "Set reminder '$label'. Confirm?"
            )
        }

        // 11. Goals
        val goalMatch = extractGoal(text)
        if (goalMatch != null) {
            return ParsedIntent(
                intent = "CREATE_GOAL",
                parameters = mapOf("name" to goalMatch)
            )
        }

        if (lower.contains("show goals") || lower.contains("my goals") || lower.contains("list goals") || lower.contains("what are my goals")) {
            return ParsedIntent("LIST_GOALS")
        }

        // 12. Projects
        val projectMatch = extractProject(text)
        if (projectMatch != null) {
            return ParsedIntent(
                intent = "CREATE_PROJECT",
                parameters = mapOf("name" to projectMatch)
            )
        }

        if (lower.contains("show projects") || lower.contains("my projects") || lower.contains("list projects") || lower.contains("active projects")) {
            return ParsedIntent("LIST_PROJECTS")
        }

        // 13. Create Task (General)
        val taskTitleMatch = extractTaskCreation(text)
        if (taskTitleMatch != null) {
            val (title, priority) = taskTitleMatch
            return ParsedIntent(
                intent = "CREATE_TASK",
                parameters = mapOf("title" to title, "priority" to priority)
            )
        }

        // Fallback: If user spoke something constructive like "Buy groceries", treat as quick task creation
        if (text.length > 2 && !text.endsWith("?")) {
            return ParsedIntent(
                intent = "CREATE_TASK",
                parameters = mapOf("title" to text, "priority" to Priority.NORMAL)
            )
        }

        return ParsedIntent("UNKNOWN", isNegationOrClarification = true)
    }

    private fun isNegation(lower: String): Boolean {
        return lower.contains("don't") || lower.contains("do not") ||
                lower.contains("dont") || lower.contains("never mind") ||
                lower.contains("cancel that") || lower.contains("stop") ||
                lower.contains("please don't")
    }

    private fun extractExpense(text: String): Pair<Double, String>? {
        // e.g. "Spent 350 on lunch", "Paid 1200 for electricity", "500 bucks on groceries", "spent 5k for dinner"
        val pattern = Pattern.compile("(?:spent|paid|spend|log expense|bought)\\s+(?:rs\\.?|inr|₹)?\\s*(\\d+(?:\\.\\d+)?)\\s*(k|thousand|bucks|rupees)?\\s*(?:on|for|towards)?\\s*(.+)?", Pattern.CASE_INSENSITIVE)
        val m = pattern.matcher(text)
        if (m.find()) {
            var rawAmount = m.group(1)?.toDoubleOrNull() ?: return null
            val unit = m.group(2)?.lowercase() ?: ""
            if (unit == "k" || unit == "thousand") {
                rawAmount *= 1000.0
            }
            var desc = m.group(3)?.trim() ?: "General expense"
            desc = desc.removePrefix("on ").removePrefix("for ").removePrefix("towards ").trim()
            if (desc.isBlank()) desc = "Expense"
            return Pair(rawAmount, desc.replaceFirstChar { it.uppercase() })
        }
        return null
    }

    private fun extractJournalNote(text: String): String? {
        val pattern = Pattern.compile("^(?:take a note|save note|note|journal entry|save this|write down)[:\\s]+(.+)", Pattern.CASE_INSENSITIVE)
        val m = pattern.matcher(text)
        if (m.find()) {
            return m.group(1)?.trim()
        }
        return null
    }

    private fun extractReminder(text: String): Pair<String, Long>? {
        val now = System.currentTimeMillis() / 1000
        val lower = text.lowercase()

        // "remind me in 30 minutes to take medicine"
        val relPattern = Pattern.compile("remind me in\\s+(\\d+)\\s+(minute|minutes|hour|hours)\\s+(?:to\\s+)?(.+)", Pattern.CASE_INSENSITIVE)
        val mRel = relPattern.matcher(text)
        if (mRel.find()) {
            val amount = mRel.group(1)?.toLongOrNull() ?: 10L
            val unit = mRel.group(2)?.lowercase() ?: "minute"
            val label = mRel.group(3)?.trim() ?: "Reminder"
            val secs = if (unit.startsWith("hour")) amount * 3600 else amount * 60
            return Pair(label, now + secs)
        }

        // "remind me to call mom at 5 PM" or "set an alarm for 7 AM"
        val absPattern = Pattern.compile("(?:remind me to\\s+(.+?)\\s+at|set an? alarm (?:for|at)\\s+)(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE)
        val mAbs = absPattern.matcher(text)
        if (mAbs.find()) {
            val label = mAbs.group(1)?.trim() ?: "Alarm"
            val hour = mAbs.group(2)?.toIntOrNull() ?: 8
            val amPm = mAbs.group(4)?.lowercase() ?: ""
            var hour24 = hour
            if (amPm == "pm" && hour24 < 12) hour24 += 12
            if (amPm == "am" && hour24 == 12) hour24 = 0

            // Estimate epoch
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, hour24)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            if (cal.timeInMillis / 1000 <= now) {
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            return Pair(label, cal.timeInMillis / 1000)
        }

        return null
    }

    private fun extractGoal(text: String): String? {
        val pattern = Pattern.compile("^(?:set a goal to|my goal is to|create goal:?|new goal:?)\\s+(.+)", Pattern.CASE_INSENSITIVE)
        val m = pattern.matcher(text)
        if (m.find()) {
            return m.group(1)?.trim()
        }
        return null
    }

    private fun extractProject(text: String): String? {
        val pattern = Pattern.compile("^(?:create a project called|start project:?|create project:?|new project:?)\\s+(.+)", Pattern.CASE_INSENSITIVE)
        val m = pattern.matcher(text)
        if (m.find()) {
            return m.group(1)?.trim()
        }
        return null
    }

    private fun extractTaskCreation(text: String): Pair<String, Priority>? {
        var priority = Priority.NORMAL
        val lower = text.lowercase()
        if (lower.contains("urgent") || lower.contains("critical")) priority = Priority.CRITICAL
        else if (lower.contains("important") || lower.contains("high priority")) priority = Priority.HIGH
        else if (lower.contains("low priority")) priority = Priority.LOW

        val patterns = listOf(
            "^(?:create a task to|create task to|add a task to|task:?|add)\\s+(.+?)(?:\\s+to my tasks)?$",
            "^(?:remind me to|i need to|todo:?)\\s+(.+)$"
        )

        for (pat in patterns) {
            val m = Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(text)
            if (m.find()) {
                var title = m.group(1)?.trim() ?: continue
                title = title.replace(Regex("(?i)\\b(urgent|critical|important|low priority)\\b"), "").trim()
                if (title.isNotEmpty()) {
                    return Pair(title.replaceFirstChar { it.uppercase() }, priority)
                }
            }
        }
        return null
    }
}
