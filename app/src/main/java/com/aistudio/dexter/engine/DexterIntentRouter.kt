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

data class FinancialMatch(
    val amountRupees: Double,
    val amountMinor: Long,
    val description: String,
    val category: String,
    val type: String = "DEBIT" // DEBIT or CREDIT
)

data class JournalMatch(
    val content: String,
    val title: String,
    val mood: String,
    val category: String
)

data class ProjectVoiceMatch(
    val name: String,
    val deadline: String = "",
    val milestones: List<String> = emptyList()
)

data class ParsedGoal(
    val title: String,
    val category: String = "Personal",
    val targetAmount: Double = 0.0,
    val metricUnit: String = "₹",
    val targetDate: String = ""
)

object DexterIntentRouter {

    fun stripWakeWord(input: String): String {
        val pattern = Pattern.compile("^\\s*(?:hey\\s+|ok\\s+|okay\\s+|hi\\s+|hello\\s+)?(?:dexter|alexa|buddy)[,\\s:]*\\s*", Pattern.CASE_INSENSITIVE)
        return pattern.matcher(input).replaceFirst("").trim()
    }

    fun splitCompoundCommand(text: String): List<String> {
        val connectors = listOf(
            " and then ", " and also ", " and ", " then ",
            "而且", "然后", "并且", "还要",
            " 和 ", " 而且 ", " и ", " затем ", " aur "
        )
        for (connector in connectors) {
            if (text.contains(connector, ignoreCase = true)) {
                val parts = text.split(Regex("(?i)" + Pattern.quote(connector)))
                val trimmedParts = parts.map { it.trim() }.filter { it.isNotBlank() }
                if (trimmedParts.size >= 2) {
                    return trimmedParts
                }
            }
        }
        return emptyList()
    }

    fun parse(rawInput: String): ParsedIntent {
        val text = stripWakeWord(rawInput)
        if (text.isBlank()) {
            return ParsedIntent("EMPTY", isNegationOrClarification = true)
        }

        val lower = text.lowercase().trim()

        // 0. Chained Multi-Command Detection (e.g. "add task review code and spent 35 on coffee")
        val chainedParts = splitCompoundCommand(text)
        if (chainedParts.size >= 2) {
            val subIntents = chainedParts.map { parse(it) }
            val areAllActionable = subIntents.all {
                it.intent != "UNKNOWN" && it.intent != "EMPTY" && it.intent != "NEGATION_SAFETY" && !it.isNegationOrClarification
            }
            if (areAllActionable) {
                return ParsedIntent(
                    intent = "CHAINED_MULTI_COMMAND",
                    parameters = mapOf("subIntents" to subIntents),
                    confidence = 0.93f
                )
            }
        }

        // 1. Proactive Alarm & Morning Wake-Up Conversational Interaction (Handles "Alexa stop alarm", "I'm awake", "Snooze")
        if (lower.matches(Regex("^(?:yes\\s+)?(?:i'm|i am|i got|i have)?\\s*(?:awake|up|risen|ready)(?:\\s+now)?$")) ||
            lower == "i'm awake" || lower == "i am awake" || lower == "i'm up" || lower == "i am up" ||
            lower == "good morning" || lower == "yes i am" || lower == "yes i'm awake" || lower == "i am awake now" ||
            lower == "i got up" || lower == "i'm awake now") {
            return ParsedIntent("ALARM_I_AM_AWAKE")
        }

        if (lower.contains("snooze") || lower.contains("5 more minutes") || lower.contains("five more minutes") ||
            lower.contains("10 more minutes") || lower.contains("ten more minutes") ||
            lower.contains("give me 5 minutes") || lower.contains("let me sleep") || lower == "sleep more") {
            val minutes = if (lower.contains("10") || lower.contains("ten")) 10 else 5
            return ParsedIntent("ALARM_SNOOZE", parameters = mapOf("minutes" to minutes))
        }

        if (lower.matches(Regex("^(?:stop|dismiss|turn off|cancel|shut off|shut up|kill)\\s+(?:the\\s+)?(?:alarm|reminder|timer|ringing)?$")) ||
            lower == "stop alarm" || lower == "dismiss alarm" || lower == "turn off alarm" ||
            lower == "stop the alarm" || lower == "cancel alarm" || lower == "dismiss") {
            return ParsedIntent("ALARM_DISMISS")
        }

        // 2. Negation Safety Check
        if (isNegation(lower)) {
            return ParsedIntent(
                intent = "NEGATION_SAFETY",
                isNegationOrClarification = true
            )
        }

        // 2. Interrogative Voice Query Guard (Android STT never includes '?')
        val queryIntent = interrogativeQueryGuard(lower, text)
        if (queryIntent != null) {
            return queryIntent
        }

        // 3. Daily Briefing
        if (lower.contains("briefing") || lower.contains("morning summary") || lower == "daily summary" ||
            lower.contains("start briefing") || lower.contains("read briefing") || lower.contains("read my briefing") ||
            lower.contains("what does my day look like") || lower.contains("what is my day like")) {
            return ParsedIntent("DAILY_BRIEFING")
        }

        // 4. Complete All Tasks
        if (lower.contains("complete all") || lower.contains("finish all") || lower.contains("mark all tasks complete") || lower.contains("complete everything")) {
            return ParsedIntent(
                intent = "COMPLETE_ALL_TASKS",
                requiresConfirmation = true,
                confirmationPrompt = "Mark all pending tasks complete. Confirm?"
            )
        }

        // 5. Complete Task (By ID or Title)
        val completeIdMatch = Pattern.compile("^(?:complete|finish|mark)\\s+(?:task\\s+)?#?(\\d+)(?:\\s+done|\\s+as\\s+done)?$", Pattern.CASE_INSENSITIVE).matcher(text.trim())
        if (completeIdMatch.find()) {
            val id = completeIdMatch.group(1)?.toLongOrNull() ?: 0L
            return ParsedIntent(
                intent = "COMPLETE_TASK",
                parameters = mapOf("taskId" to id),
                requiresConfirmation = true,
                confirmationPrompt = "Complete task #$id. Confirm?"
            )
        }

        val completeTitleMatch = Pattern.compile("^(?:mark|finish|complete)\\s+(?:task\\s+)?['\"]?([^'\"]+?)['\"]?\\s+(?:done|complete|as\\s+done)$", Pattern.CASE_INSENSITIVE).matcher(text.trim())
        if (completeTitleMatch.find()) {
            val title = completeTitleMatch.group(1)?.trim() ?: ""
            if (title.isNotEmpty() && !title.equals("all", ignoreCase = true) && !title.contains("milestone", ignoreCase = true) && !title.contains("project", ignoreCase = true)) {
                return ParsedIntent(
                    intent = "COMPLETE_TASK_TITLE",
                    parameters = mapOf("title" to title),
                    requiresConfirmation = true,
                    confirmationPrompt = "Complete task '$title'. Confirm?"
                )
            }
        }

        // 6. Cancel Task
        val cancelIdMatch = Pattern.compile("^(?:cancel|delete|remove|abandon)\\s+(?:task\\s+)?#?(\\d+)$", Pattern.CASE_INSENSITIVE).matcher(text.trim())
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

        // 7. Alarms & Reminders (Supports time-first: "remind me at 5 PM regarding a team meeting")
        val reminderMatch = extractReminder(text)
        if (reminderMatch != null) {
            val (label, epochMs) = reminderMatch
            return ParsedIntent(
                intent = "CREATE_ALARM",
                parameters = mapOf(
                    "label" to label,
                    "alarmTime" to epochMs,
                    "alarm_time" to epochMs
                ),
                requiresConfirmation = false
            )
        }

        if (lower.contains("cancel alarms") || lower.contains("cancel all reminders") || lower.contains("turn off my alarms") || lower.contains("turn off alarms")) {
            return ParsedIntent(
                intent = "CANCEL_ALARMS",
                requiresConfirmation = true,
                confirmationPrompt = "Cancel all active alarms. Confirm?"
            )
        }

        // 8. Finance / Expense & Income Logging
        val financeMatch = extractFinance(text)
        if (financeMatch != null) {
            return ParsedIntent(
                intent = if (financeMatch.type == "CREDIT") "LOG_TRANSACTION" else "LOG_EXPENSE",
                parameters = mapOf(
                    "amountMinor" to financeMatch.amountMinor,
                    "amountRupees" to financeMatch.amountRupees,
                    "amount" to financeMatch.amountRupees,
                    "description" to financeMatch.description,
                    "category" to financeMatch.category,
                    "type" to financeMatch.type,
                    "payment_mode" to "UPI"
                )
            )
        }

        // 9. Journal / Note Logging
        val journalMatch = extractJournalNote(text)
        if (journalMatch != null) {
            return ParsedIntent(
                intent = "LOG_JOURNAL",
                parameters = mapOf(
                    "content" to journalMatch.content,
                    "title" to journalMatch.title,
                    "mood" to journalMatch.mood,
                    "category" to journalMatch.category
                )
            )
        }

        // 10. Goals
        val goalMatch = extractGoal(text)
        if (goalMatch != null) {
            return ParsedIntent(
                intent = "CREATE_GOAL",
                parameters = mapOf(
                    "name" to goalMatch.title,
                    "title" to goalMatch.title,
                    "category" to goalMatch.category,
                    "target_amount" to goalMatch.targetAmount,
                    "metric_unit" to goalMatch.metricUnit,
                    "target_date" to goalMatch.targetDate
                )
            )
        }

        // 11a. Complete Milestone: e.g. "complete milestone UI in project Website"
        val completeMilestoneMatch = extractCompleteMilestone(text)
        if (completeMilestoneMatch != null) {
            return ParsedIntent(
                intent = "COMPLETE_MILESTONE",
                parameters = mapOf(
                    "milestone" to completeMilestoneMatch.first,
                    "milestone_name" to completeMilestoneMatch.first,
                    "project_name" to completeMilestoneMatch.second
                )
            )
        }

        // 11b. Add Milestone: e.g. "add milestone Wireframes to project Website"
        val addMilestoneMatch = extractAddMilestone(text)
        if (addMilestoneMatch != null) {
            return ParsedIntent(
                intent = "CREATE_MILESTONE",
                parameters = mapOf(
                    "milestone" to addMilestoneMatch.first,
                    "title" to addMilestoneMatch.first,
                    "project_name" to addMilestoneMatch.second
                )
            )
        }

        // 11c. Update Project Deadline: e.g. "set deadline for project Website to Dec 15"
        val updateDeadlineMatch = extractProjectDeadlineUpdate(text)
        if (updateDeadlineMatch != null) {
            return ParsedIntent(
                intent = "UPDATE_PROJECT_DEADLINE",
                parameters = mapOf("project_name" to updateDeadlineMatch.first, "deadline" to updateDeadlineMatch.second)
            )
        }

        // 11d. Projects & Milestones Creation
        val projectMatch = extractProject(text)
        if (projectMatch != null) {
            val params = mutableMapOf<String, Any?>(
                "name" to projectMatch.name,
                "title" to projectMatch.name
            )
            if (projectMatch.deadline.isNotBlank()) params["deadline"] = projectMatch.deadline
            if (projectMatch.milestones.isNotEmpty()) params["milestones"] = projectMatch.milestones
            return ParsedIntent(
                intent = "CREATE_PROJECT",
                parameters = params
            )
        }

        // 12. Explicit Task Creation
        val taskTitleMatch = extractTaskCreation(text)
        if (taskTitleMatch != null) {
            val (title, priority) = taskTitleMatch
            return ParsedIntent(
                intent = "CREATE_TASK",
                parameters = mapOf("title" to title, "priority" to priority)
            )
        }

        // 13. Actionable Imperatives (e.g. "buy groceries", "call electrician", "submit report")
        if (isActionableImperative(lower)) {
            val cleanTitle = text.replaceFirstChar { it.uppercase() }
            return ParsedIntent(
                intent = "CREATE_TASK",
                parameters = mapOf("title" to cleanTitle, "priority" to Priority.NORMAL)
            )
        }

        // 13.5. On-Device Conversational Needle Reasoning (Needle-3)
        if (isConversationalNeedleQuery(lower)) {
            return ParsedIntent(
                intent = "CONVERSATIONAL_REASONING",
                parameters = mapOf("query" to text)
            )
        }

        // 14. Safe Fallback: NEVER turn random unrecognized phrases into tasks!
        return ParsedIntent(
            intent = "UNKNOWN",
            isNegationOrClarification = true,
            confirmationPrompt = "I heard '$text'. Did you want to create a task, set a reminder, or log an expense?"
        )
    }

    private fun isNegation(lower: String): Boolean {
        if (lower.contains("alarm") || lower.contains("timer") || lower.contains("ringing")) return false
        return lower.contains("don't") || lower.contains("do not") ||
                lower.contains("dont") || lower.contains("never mind") ||
                lower.contains("cancel that") || lower == "stop" ||
                lower.contains("please don't")
    }

    /**
     * Extracts financial transaction (Expense or Income) and automatically classifies
     * category, payment mode, and minor amounts.
     */
    fun extractFinance(text: String): FinancialMatch? {
        val trimmed = text.trim()
        val lower = trimmed.lowercase()

        // 1. Check for Income / Credit expressions:
        // "received 50000 from salary", "got 200 cashback from gpay", "credited 1500 for refund", "earned 3000 consulting"
        val incomeRegex = Pattern.compile(
            "^(?:received|got|credited|credit|earned)\\s+(?:rs\\.?|inr|₹)?\\s*(\\d+(?:\\.\\d+)?)\\s*(k|thousand|lakh|bucks|rupees)?\\s*(?:from|for|as|in|towards)?\\s*(.+)?",
            Pattern.CASE_INSENSITIVE
        )
        val mIncome = incomeRegex.matcher(trimmed)
        if (mIncome.find()) {
            var rawAmount = mIncome.group(1)?.toDoubleOrNull() ?: return null
            val unit = mIncome.group(2)?.lowercase() ?: ""
            if (unit == "k" || unit == "thousand") rawAmount *= 1000.0
            if (unit == "lakh") rawAmount *= 100000.0

            var desc = mIncome.group(3)?.trim() ?: "Income"
            desc = desc.replace(Regex("^(?:from|for|as|in|towards)\\s+", RegexOption.IGNORE_CASE), "").trim()
            if (desc.isBlank()) desc = "Credit"

            val category = inferFinanceCategory(desc, type = "CREDIT")
            val amountMinor = (rawAmount * 100).toLong()
            return FinancialMatch(
                amountRupees = rawAmount,
                amountMinor = amountMinor,
                description = desc.replaceFirstChar { it.uppercase() },
                category = category,
                type = "CREDIT"
            )
        }

        // Explicit "salary of 50000 credited" or "cashback 200 received"
        val incomeNounRegex = Pattern.compile(
            "^(salary|cashback|refund|dividend|income)\\s+(?:of\\s+)?(?:rs\\.?|inr|₹)?\\s*(\\d+(?:\\.\\d+)?)\\s*(k|thousand|lakh|bucks|rupees)?\\s*(?:received|credited|got)?",
            Pattern.CASE_INSENSITIVE
        )
        val mNounIncome = incomeNounRegex.matcher(trimmed)
        if (mNounIncome.find()) {
            val noun = mNounIncome.group(1)?.trim() ?: "Income"
            var rawAmount = mNounIncome.group(2)?.toDoubleOrNull() ?: return null
            val unit = mNounIncome.group(3)?.lowercase() ?: ""
            if (unit == "k" || unit == "thousand") rawAmount *= 1000.0
            if (unit == "lakh") rawAmount *= 100000.0

            val category = inferFinanceCategory(noun, type = "CREDIT")
            val amountMinor = (rawAmount * 100).toLong()
            return FinancialMatch(
                amountRupees = rawAmount,
                amountMinor = amountMinor,
                description = noun.replaceFirstChar { it.uppercase() },
                category = category,
                type = "CREDIT"
            )
        }

        // 2. Check for Expense / Debit expressions:
        // "spent 45 in traveling", "spent 350 on lunch", "paid 1200 for electricity", "bought groceries for 500", "500 bucks on fuel"
        val expenseRegex = Pattern.compile(
            "(?:spent|paid|spend|log expense|bought|purchased|deducted)\\s+(?:rs\\.?|inr|₹)?\\s*(\\d+(?:\\.\\d+)?)\\s*(k|thousand|lakh|bucks|rupees)?\\s*(?:on|for|in|towards|at|during)?\\s*(.+)?",
            Pattern.CASE_INSENSITIVE
        )
        val mExpense = expenseRegex.matcher(trimmed)
        if (mExpense.find()) {
            var rawAmount = mExpense.group(1)?.toDoubleOrNull() ?: return null
            val unit = mExpense.group(2)?.lowercase() ?: ""
            if (unit == "k" || unit == "thousand") rawAmount *= 1000.0
            if (unit == "lakh") rawAmount *= 100000.0

            var desc = mExpense.group(3)?.trim() ?: "General expense"
            desc = desc.replace(Regex("^(?:on|for|in|towards|at|during)\\s+", RegexOption.IGNORE_CASE), "").trim()
            if (desc.isBlank()) desc = "Expense"

            val category = inferFinanceCategory(desc, type = "DEBIT")
            val amountMinor = (rawAmount * 100).toLong()
            return FinancialMatch(
                amountRupees = rawAmount,
                amountMinor = amountMinor,
                description = desc.replaceFirstChar { it.uppercase() },
                category = category,
                type = "DEBIT"
            )
        }

        // "paid electricity 1200" or "paid rent 15000"
        val paidNounRegex = Pattern.compile(
            "^(?:paid|spent)\\s+(.+?)\\s+(?:of\\s+|for\\s+|rs\\.?|inr|₹)\\s*(\\d+(?:\\.\\d+)?)\\s*(k|thousand|lakh|bucks|rupees)?$",
            Pattern.CASE_INSENSITIVE
        )
        val mPaidNoun = paidNounRegex.matcher(trimmed)
        if (mPaidNoun.find()) {
            var desc = mPaidNoun.group(1)?.trim() ?: "Expense"
            var rawAmount = mPaidNoun.group(2)?.toDoubleOrNull() ?: return null
            val unit = mPaidNoun.group(3)?.lowercase() ?: ""
            if (unit == "k" || unit == "thousand") rawAmount *= 1000.0
            if (unit == "lakh") rawAmount *= 100000.0

            val category = inferFinanceCategory(desc, type = "DEBIT")
            val amountMinor = (rawAmount * 100).toLong()
            return FinancialMatch(
                amountRupees = rawAmount,
                amountMinor = amountMinor,
                description = desc.replaceFirstChar { it.uppercase() },
                category = category,
                type = "DEBIT"
            )
        }

        // "coffee cost 120"
        val costRegex = Pattern.compile(
            "^(.+?)\\s+cost(?:s)?\\s+(?:rs\\.?|inr|₹)?\\s*(\\d+(?:\\.\\d+)?)\\s*(k|thousand|bucks|rupees)?$",
            Pattern.CASE_INSENSITIVE
        )
        val mCost = costRegex.matcher(trimmed)
        if (mCost.find()) {
            var desc = mCost.group(1)?.trim() ?: "Expense"
            var rawAmount = mCost.group(2)?.toDoubleOrNull() ?: return null
            val unit = mCost.group(3)?.lowercase() ?: ""
            if (unit == "k" || unit == "thousand") rawAmount *= 1000.0

            val category = inferFinanceCategory(desc, type = "DEBIT")
            val amountMinor = (rawAmount * 100).toLong()
            return FinancialMatch(
                amountRupees = rawAmount,
                amountMinor = amountMinor,
                description = desc.replaceFirstChar { it.uppercase() },
                category = category,
                type = "DEBIT"
            )
        }

        // 3. Chinese Finance Patterns:
        // "花了50元买咖啡", "花费100块打车", "支出200元", "买咖啡花了50元"
        val zhExpRegex = Pattern.compile("^(?:花了|花费|支出|消费)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:元|块|块钱|rmb|cny)?(?:\\s*(?:在|买|用于|到)?\\s*(.+))?$", Pattern.CASE_INSENSITIVE)
        val mZhExp = zhExpRegex.matcher(trimmed)
        if (mZhExp.find()) {
            val amt = mZhExp.group(1)?.toDoubleOrNull() ?: 0.0
            val desc = mZhExp.group(2)?.trim()?.ifBlank { "支出" } ?: "支出"
            return FinancialMatch(
                amountRupees = amt,
                amountMinor = (amt * 100).toLong(),
                description = desc,
                category = inferFinanceCategory(desc, "DEBIT"),
                type = "DEBIT"
            )
        }

        val zhBuyRegex = Pattern.compile("^(?:买|购买|购入)\\s*(.+?)\\s*(?:花了|用去|用了)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:元|块)?$", Pattern.CASE_INSENSITIVE)
        val mZhBuy = zhBuyRegex.matcher(trimmed)
        if (mZhBuy.find()) {
            val desc = mZhBuy.group(1)?.trim() ?: "购物"
            val amt = mZhBuy.group(2)?.toDoubleOrNull() ?: 0.0
            return FinancialMatch(
                amountRupees = amt,
                amountMinor = (amt * 100).toLong(),
                description = desc,
                category = inferFinanceCategory(desc, "DEBIT"),
                type = "DEBIT"
            )
        }

        // 4. Russian Finance Patterns:
        // "потратил 500 рублей на такси", "купил кофе за 250 рублей", "расход 1000 на продукты"
        val ruExpRegex = Pattern.compile("^(?:потратил|заплатил|купил|оплатил|расход|списано)\\s*(\\d+(?:\\.\\d+)?)\\s*(рублей|руб\\.?|₽|к|k|тысяч|тыс)?(?:\\s*(?:на|за|для)?\\s*(.+))?$", Pattern.CASE_INSENSITIVE)
        val mRuExp = ruExpRegex.matcher(trimmed)
        if (mRuExp.find()) {
            var amt = mRuExp.group(1)?.toDoubleOrNull() ?: 0.0
            val unit = mRuExp.group(2)?.trim()?.lowercase() ?: ""
            if (unit == "к" || unit == "k" || unit.startsWith("тыс")) amt *= 1000.0
            val desc = mRuExp.group(3)?.trim()?.ifBlank { "Расход" } ?: "Расход"
            return FinancialMatch(
                amountRupees = amt,
                amountMinor = (amt * 100).toLong(),
                description = desc,
                category = inferFinanceCategory(desc, "DEBIT"),
                type = "DEBIT"
            )
        }

        val ruBuyRegex = Pattern.compile("^(?:купил|оплатил)\\s*(.+?)\\s*(?:за|на сумму)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:рублей|руб\\.?|₽)?$", Pattern.CASE_INSENSITIVE)
        val mRuBuy = ruBuyRegex.matcher(trimmed)
        if (mRuBuy.find()) {
            val desc = mRuBuy.group(1)?.trim() ?: "Покупка"
            val amt = mRuBuy.group(2)?.toDoubleOrNull() ?: 0.0
            return FinancialMatch(
                amountRupees = amt,
                amountMinor = (amt * 100).toLong(),
                description = desc,
                category = inferFinanceCategory(desc, "DEBIT"),
                type = "DEBIT"
            )
        }

        val ruIncRegex = Pattern.compile("^(?:получил|заработал|пришло)\\s*(\\d+(?:\\.\\d+)?)\\s*(рублей|руб\\.?|₽|к|k|тысяч|тыс)?(?:\\s*(?:от|за|с)?\\s*(.+))?$", Pattern.CASE_INSENSITIVE)
        val mRuInc = ruIncRegex.matcher(trimmed)
        if (mRuInc.find()) {
            var amt = mRuInc.group(1)?.toDoubleOrNull() ?: 0.0
            val unit = mRuInc.group(2)?.trim()?.lowercase() ?: ""
            if (unit == "к" || unit == "k" || unit.startsWith("тыс")) amt *= 1000.0
            val desc = mRuInc.group(3)?.trim()?.ifBlank { "Доход" } ?: "Доход"
            return FinancialMatch(
                amountRupees = amt,
                amountMinor = (amt * 100).toLong(),
                description = desc,
                category = inferFinanceCategory(desc, "CREDIT"),
                type = "CREDIT"
            )
        }

        // 5. Hindi / Hinglish Finance Patterns:
        // "500 rupaye lunch pe kharch kiye", "kharcha hua 200 chai pe", "spent 300 on petrol"
        val hiExpRegex = Pattern.compile("^(?:kharch kiye|kharcha hua|diye|diya|bhara)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:rupaye|rs|₹)?(?:\\s*(?:pe|par|for)?\\s*(.+))?$", Pattern.CASE_INSENSITIVE)
        val mHiExp = hiExpRegex.matcher(trimmed)
        if (mHiExp.find()) {
            val amt = mHiExp.group(1)?.toDoubleOrNull() ?: 0.0
            val desc = mHiExp.group(2)?.trim()?.ifBlank { "Kharcha" } ?: "Kharcha"
            return FinancialMatch(
                amountRupees = amt,
                amountMinor = (amt * 100).toLong(),
                description = desc,
                category = inferFinanceCategory(desc, "DEBIT"),
                type = "DEBIT"
            )
        }

        val hiSuffRegex = Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*(?:rupaye|rs|₹)?\\s*(.+?)\\s*(?:pe|par)\\s*(?:kharch kiye|kharcha|diye)$", Pattern.CASE_INSENSITIVE)
        val mHiSuff = hiSuffRegex.matcher(trimmed)
        if (mHiSuff.find()) {
            val amt = mHiSuff.group(1)?.toDoubleOrNull() ?: 0.0
            val desc = mHiSuff.group(2)?.trim() ?: "Kharcha"
            return FinancialMatch(
                amountRupees = amt,
                amountMinor = (amt * 100).toLong(),
                description = desc,
                category = inferFinanceCategory(desc, "DEBIT"),
                type = "DEBIT"
            )
        }

        // 6. Accent & Slang Financial Patterns:
        // "spen 50 bucks on fuel", "drop 30 on coffee", "blew 40 on boba", "chuck 250 into petrol"
        val slangExpRegex = Pattern.compile("^(?:spen|spend|drop|chuck|blow|blew|put)\\s+(?:rs\\.?|inr|₹|\\$)?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:bucks|rupees|dollars)?\\s*(?:on|for|in|into|towards)\\s+(.+)$", Pattern.CASE_INSENSITIVE)
        val mSlang = slangExpRegex.matcher(trimmed)
        if (mSlang.find()) {
            val amt = mSlang.group(1)?.toDoubleOrNull() ?: 0.0
            val desc = mSlang.group(2)?.trim() ?: "Expense"
            return FinancialMatch(
                amountRupees = amt,
                amountMinor = (amt * 100).toLong(),
                description = desc.replaceFirstChar { it.uppercase() },
                category = inferFinanceCategory(desc, "DEBIT"),
                type = "DEBIT"
            )
        }

        return null
    }

    /**
     * Intelligent Category Classifier for Finance.
     * Evaluates semantic keywords across English, Chinese, Russian, and Hindi.
     */
    fun inferFinanceCategory(description: String, type: String = "DEBIT"): String {
        val lower = description.lowercase()

        // Transportation
        if (lower.contains(Regex("\\b(traveling|travel|travelling|auto|cab|taxi|uber|ola|bus|train|flight|airline|metro|petrol|diesel|fuel|toll|fastag|parking|commute|transit|rickshaw)\\b")) ||
            lower.contains("такси") || lower.contains("метро") || lower.contains("бензин") || lower.contains("билет") ||
            lower.contains("打车") || lower.contains("地铁") || lower.contains("加油") || lower.contains("公交") ||
            lower.contains("petrol") || lower.contains("gaadi")) {
            return "Transportation"
        }

        // Food & Dining
        if (lower.contains(Regex("\\b(food|lunch|dinner|breakfast|brunch|snacks?|coffee|cafe|tea|chai|swiggy|zomato|groceries|grocery|vegetables?|veggies?|fruits?|milk|bread|restaurant|bistro|burger|pizza|bakery|dining|meal|drinks?|barista|boba)\\b")) ||
            lower.contains("кофе") || lower.contains("обед") || lower.contains("ужин") || lower.contains("продукты") || lower.contains("еда") ||
            lower.contains("咖啡") || lower.contains("午餐") || lower.contains("晚餐") || lower.contains("奶茶") || lower.contains("水果") ||
            lower.contains("khana") || lower.contains("chai") || lower.contains("nashta")) {
            return "Food & Dining"
        }

        // Utilities & Bills
        if (lower.contains(Regex("\\b(electricity|electric|power|water|internet|wifi|broadband|mobile|recharge|phone|sim|gas|cylinder|lpg|rent|maintenance|bills?|dth|tv|society)\\b")) ||
            lower.contains("аренда") || lower.contains("жкх") || lower.contains("интернет") || lower.contains("свет") ||
            lower.contains("电费") || lower.contains("话费") || lower.contains("房租") || lower.contains("网费") ||
            lower.contains("bijli") || lower.contains("kiraya")) {
            return "Utilities & Bills"
        }

        // Shopping
        if (lower.contains(Regex("\\b(clothes|clothing|shoes?|shirt|pants?|dress|amazon|flipkart|myntra|mall|electronics|gadgets?|laptop|phone case|shopping|purchases?|market|supermarket|store|apparel)\\b")) ||
            lower.contains("одежда") || lower.contains("магазин") || lower.contains("покупки") ||
            lower.contains("衣服") || lower.contains("购物") || lower.contains("商场") ||
            lower.contains("kapde") || lower.contains("kharidari")) {
            return "Shopping"
        }

        // Health & Medical
        if (lower.contains(Regex("\\b(medicines?|meds|pills?|doctor|physician|pharmacy|chemist|clinic|tests?|hospital|gym|fitness|workout|supplements?|vitamins?|dentist|medical|health|pharma)\\b")) ||
            lower.contains("аптека") || lower.contains("врач") || lower.contains("лекарства") ||
            lower.contains("药品") || lower.contains("医院") || lower.contains("看病") ||
            lower.contains("dawai") || lower.contains("doctor")) {
            return "Health & Medical"
        }

        // Entertainment
        if (lower.contains(Regex("\\b(movies?|cinema|theatre|netflix|prime|hotstar|games?|gaming|party|outing|concert|shows?|club|pub|weekend|subscriptions?|entertainment)\\b")) ||
            lower.contains("кино") || lower.contains("фильм") || lower.contains("игра") ||
            lower.contains("电影") || lower.contains("游戏") || lower.contains("娱乐")) {
            return "Entertainment"
        }

        // Salary & Income
        if (type == "CREDIT" || lower.contains(Regex("\\b(salary|stipend|consulting|bonus|freelance|dividends?|interest|cashback|refund|client|wage|income)\\b")) ||
            lower.contains("зарплата") || lower.contains("доход") || lower.contains("премия") ||
            lower.contains("工资") || lower.contains("收入") || lower.contains("奖金") ||
            lower.contains("tankhwah") || lower.contains("kamai")) {
            return "Salary & Income"
        }

        return "General"
    }

    /**
     * Extracts journal/diary content and auto-infers mood and category tags.
     */
    fun extractJournalNote(text: String): JournalMatch? {
        val pattern = Pattern.compile("^(?:take a note|save note|note|journal entry|journal|diary entry|diary|write down)[:\\s]+(.+)", Pattern.CASE_INSENSITIVE)
        val m = pattern.matcher(text)
        if (m.find()) {
            val content = m.group(1)?.trim() ?: return null
            val mood = inferJournalMood(content)
            val category = inferJournalCategory(content)
            val title = generateJournalTitle(content)
            return JournalMatch(
                content = content,
                title = title,
                mood = mood,
                category = category
            )
        }
        return null
    }

    fun inferJournalMood(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains(Regex("\\b(focused|focus|productive|deep work|concentrated|locked in|clarity|sprint)\\b")) -> "Focused"
            lower.contains(Regex("\\b(energized|energetic|excited|inspired|motivated|pumped|enthusiastic|breakthrough)\\b")) -> "Energized"
            lower.contains(Regex("\\b(calm|peaceful|relaxed|serene|quiet|chill|mindful|zen)\\b")) -> "Calm"
            lower.contains(Regex("\\b(grateful|thankful|blessed|appreciation|gratitude|thank)\\b")) -> "Grateful"
            lower.contains(Regex("\\b(tired|exhausted|sleepy|fatigued|drained|low energy|burnout|rough day)\\b")) -> "Tired"
            else -> "Reflective"
        }
    }

    fun inferJournalCategory(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains(Regex("\\b(work|project|code|architecture|client|meeting|office|bug|feature|release|deadline|sprint|task|engineering|report|presentation)\\b")) -> "Work"
            lower.contains(Regex("\\b(health|workout|gym|run|running|diet|sleep|fitness|walk|doctor|body|exercise|meds|meditation)\\b")) -> "Health"
            lower.contains(Regex("\\b(idea|ideas|concept|plan|innovation|brainstorm|future|vision|startup|thought|notes?)\\b")) -> "Ideas"
            lower.contains(Regex("\\b(personal|family|mom|dad|friend|relationship|home|life|travel|trip|day off|weekend|feeling|happy|sad|tired|chill)\\b")) -> "Personal"
            lower.contains(Regex("\\b(grateful|thankful|gratitude|blessed|appreciation)\\b")) -> "Gratitude"
            lower.contains(Regex("\\b(milestone|achieved|finished|launched|goal met|celebration|completed)\\b")) -> "Milestones"
            else -> "Personal"
        }
    }

    fun generateJournalTitle(content: String): String {
        val clean = content.trim().replace(Regex("^(?:today|i am|feeling)\\s+", RegexOption.IGNORE_CASE), "")
        val words = clean.split(Regex("\\s+"))
        val titleWords = words.take(5).joinToString(" ")
        return if (titleWords.length > 35) {
            titleWords.take(32) + "..."
        } else {
            titleWords
        }.replaceFirstChar { it.uppercase() }
    }

    private fun extractReminder(text: String): Pair<String, Long>? {
        // 1. Relative: "remind me in 30 minutes to take medicine", "in 1 hour about meeting"
        val relPattern = Pattern.compile("(?:remind me in|alert me in|in)\\s+(\\d+)\\s*(minute|minutes|min|mins|hour|hours)\\s+(?:to\\s+|about\\s+|for\\s+|regarding\\s+)?(.+)", Pattern.CASE_INSENSITIVE)
        val mRel = relPattern.matcher(text)
        if (mRel.find()) {
            val amount = mRel.group(1)?.toLongOrNull() ?: 10L
            val unit = mRel.group(2)?.lowercase() ?: "minute"
            var label = mRel.group(3)?.trim() ?: "Reminder"
            label = cleanReminderLabel(label)
            val ms = if (unit.startsWith("hour")) amount * 3600000L else amount * 60000L
            return Pair(label, System.currentTimeMillis() + ms)
        }

        // 2. Time-First: "remind me at 5 PM regarding a team meeting", "set a reminder at 5:30 PM for team meeting", "remind me for 5 PM to call mom"
        val timeFirstPattern = Pattern.compile("(?:remind me|set a reminder|set an? alarm|alert me)\\s+(?:at|for)\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\s+(?:regarding|about|for|to)\\s+(.+)", Pattern.CASE_INSENSITIVE)
        val mTimeFirst = timeFirstPattern.matcher(text)
        if (mTimeFirst.find()) {
            val hour = mTimeFirst.group(1)?.toIntOrNull() ?: 8
            val min = mTimeFirst.group(2)?.toIntOrNull() ?: 0
            val amPm = mTimeFirst.group(3) ?: ""
            var label = mTimeFirst.group(4)?.trim() ?: "Reminder"
            label = cleanReminderLabel(label)
            return Pair(label, computeAlarmEpochMs(hour, min, amPm))
        }

        // 3. Action-First: "remind me to call mom at 5 PM", "set a reminder to submit code at 4:30 PM"
        val actionFirstPattern = Pattern.compile("(?:remind me to|set a reminder to)\\s+(.+?)\\s+(?:at|for)\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?$", Pattern.CASE_INSENSITIVE)
        val mActionFirst = actionFirstPattern.matcher(text)
        if (mActionFirst.find()) {
            var label = mActionFirst.group(1)?.trim() ?: "Reminder"
            val hour = mActionFirst.group(2)?.toIntOrNull() ?: 8
            val min = mActionFirst.group(3)?.toIntOrNull() ?: 0
            val amPm = mActionFirst.group(4) ?: ""
            label = cleanReminderLabel(label)
            return Pair(label, computeAlarmEpochMs(hour, min, amPm))
        }

        // 4. Alarm-style: "set an alarm for 6:30 AM", "wake me up at 6 AM", "set alarm at 7 AM called Workout"
        val alarmPattern = Pattern.compile("(?:set an? alarm (?:for|at)|wake me up at)\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?(?:\\s+(?:for|to|called|titled)\\s+(.+))?", Pattern.CASE_INSENSITIVE)
        val mAlarm = alarmPattern.matcher(text)
        if (mAlarm.find()) {
            val hour = mAlarm.group(1)?.toIntOrNull() ?: 7
            val min = mAlarm.group(2)?.toIntOrNull() ?: 0
            val amPm = mAlarm.group(3) ?: ""
            var label = mAlarm.group(4)?.trim() ?: "Alarm"
            label = cleanReminderLabel(label)
            return Pair(label, computeAlarmEpochMs(hour, min, amPm))
        }

        // 4. Multilingual & Accents (Chinese, Russian, Hindi, Phonetic Slips)
        // Chinese: "定个早上7点的闹钟", "设个闹钟下午三点", "提醒我三点开会"
        val zhAlarmPattern = Pattern.compile("^(?:定个|设个|设置)?\\s*(.+?)\\s*(?:的闹钟|闹钟)$|^(?:定个闹钟|设个闹钟|设置闹钟|闹钟|提醒我)\\s*[:：]?\\s*(.+)$", Pattern.CASE_INSENSITIVE)
        val mZhAlarm = zhAlarmPattern.matcher(text.trim())
        if (mZhAlarm.find()) {
            val label = (mZhAlarm.group(1) ?: mZhAlarm.group(2))?.trim() ?: "闹钟"
            return Pair(label, System.currentTimeMillis() + 3600000L)
        }

        // Russian: "поставь будильник на 7 утра", "напомни мне в 5 вечера"
        val ruAlarmPattern = Pattern.compile("^(?:поставь будильник|разбуди меня|будильник|напомни мне)\\s*(?:на|в)?\\s*(.+)$", Pattern.CASE_INSENSITIVE)
        val mRuAlarm = ruAlarmPattern.matcher(text.trim())
        if (mRuAlarm.find()) {
            val label = mRuAlarm.group(1)?.trim() ?: "Будильник"
            return Pair(label, System.currentTimeMillis() + 3600000L)
        }

        // Hindi: "subah 7 baje ka alarm lagao", "7 baje ka alarm"
        val hiAlarmPattern = Pattern.compile("^(.+?)\\s*(?:ka alarm lagao|alarm lagao|baje jagao|ka alarm)$", Pattern.CASE_INSENSITIVE)
        val mHiAlarm = hiAlarmPattern.matcher(text.trim())
        if (mHiAlarm.find()) {
            val label = mHiAlarm.group(1)?.trim() ?: "Alarm"
            return Pair(label, System.currentTimeMillis() + 3600000L)
        }

        return null
    }

    fun isInterrogativeQuery(rawInput: String): Boolean {
        val lower = stripWakeWord(rawInput).lowercase().trim()
        if (rawInput.trim().endsWith("?")) return true
        return lower.startsWith("what") || lower.startsWith("which") ||
                lower.startsWith("how") || lower.startsWith("when") ||
                lower.startsWith("where") || lower.startsWith("who") ||
                lower.startsWith("show") || lower.startsWith("list") ||
                lower.startsWith("tell me") || lower.startsWith("do i have") ||
                lower.startsWith("are there") || lower.startsWith("any ") ||
                Regex("\\b(what|show|list|do i have|how much|tell me)\\b").containsMatchIn(lower) ||
                lower.contains("pending")
    }

    fun interrogativeQueryGuard(lower: String, text: String): ParsedIntent? {
        val cleanLower = lower.lowercase().trim()
        if (!isInterrogativeQuery(cleanLower) && !cleanLower.endsWith("?") && !text.endsWith("?")) {
            return null
        }

        // 1. Finance queries (check before tasks so "spending" is not caught by "pending")
        if (cleanLower.contains(Regex("\\b(spend|spent|expense|expenses|spending|cost|costs|money|budget|balance)\\b"))) {
            return ParsedIntent("SUMMARIZE_EXPENSES")
        }

        // 2. Task queries: "what are all the pending tasks today", "what tasks do i have", "show completed tasks"
        if (cleanLower.contains(Regex("\\b(tasks?|todos?|pending|to do|to-do|work|agenda|unfinished)\\b")) ||
            cleanLower.contains("what do i need to do") || cleanLower.contains("what do i have to do") ||
            cleanLower.contains("what have i finished") || cleanLower.contains("do i have any tasks")) {
            if (cleanLower.contains("completed") || cleanLower.contains("finished") || cleanLower.contains("done")) {
                return ParsedIntent("LIST_COMPLETED_TASKS")
            }
            return ParsedIntent("LIST_PENDING_TASKS")
        }

        // 3. Alarm / Reminder queries: "what reminders do i have", "show alarms", "list reminders"
        if (cleanLower.contains(Regex("\\b(alarms?|reminders?|timers?)\\b"))) {
            return ParsedIntent("LIST_ALARMS")
        }

        // 4. Journal / Diary queries: "show notes", "what did i journal", "read notes"
        if (cleanLower.contains(Regex("\\b(notes?|journal|diary|log)\\b"))) {
            return ParsedIntent("LIST_JOURNAL")
        }

        // 5. Goal queries
        if (cleanLower.contains(Regex("\\b(goals?)\\b"))) {
            return ParsedIntent("LIST_GOALS")
        }

        // 6. Specific Project Status query
        val projectStatusMatch = Pattern.compile(
            "^(?:what is the status of|what's the status of|how is project|status of project|progress of project|check project|milestones for project|milestones for)\\s+(.+)$",
            Pattern.CASE_INSENSITIVE
        ).matcher(text)
        if (projectStatusMatch.find()) {
            val pName = projectStatusMatch.group(1)?.trim()?.removePrefix("project ") ?: ""
            return ParsedIntent("GET_PROJECT_STATUS", parameters = mapOf("project_name" to pName, "name" to pName))
        }

        // 7. Project queries
        if (cleanLower.contains(Regex("\\b(projects?)\\b"))) {
            return ParsedIntent("LIST_PROJECTS")
        }

        // 7. Daily briefing queries: "what do i need to know today", "what's my summary"
        if (cleanLower.contains(Regex("\\b(briefing|morning summary|daily summary|summary|schedule)\\b"))) {
            return ParsedIntent("DAILY_BRIEFING")
        }

        // 8. On-Device Conversational Needle Reasoning (Needle-3)
        if (isConversationalNeedleQuery(cleanLower)) {
            return ParsedIntent(
                intent = "CONVERSATIONAL_REASONING",
                parameters = mapOf("query" to text)
            )
        }

        // Unhandled queries should NEVER be made into tasks!
        return ParsedIntent(
            intent = "UNKNOWN",
            isNegationOrClarification = true,
            confirmationPrompt = "I heard '$text'. I manage tasks, reminders, expenses, journal notes, and daily briefings. Which would you like?"
        )
    }

    fun isConversationalNeedleQuery(lower: String): Boolean {
        return lower.contains("focus on next") || lower.contains("reality check") ||
                lower.contains("how am i doing") || lower.contains("blindspot") ||
                lower.contains("what am i missing") || lower.contains("can i afford") ||
                lower.contains("afford dinner") || lower.contains("advise me") ||
                lower.contains("what should i do next") || lower.contains("who are you") ||
                lower.contains("what are you") || lower.contains("talk to me") ||
                lower.contains("what can you do") || lower.contains("give me advice") ||
                lower == "hello" || lower == "hey dexter" || lower == "how are you" ||
                lower.contains("performance audit") || lower.contains("check in on me")
    }

    private fun cleanReminderLabel(raw: String): String {
        var clean = raw.trim().replace(Regex("^(?:regarding|about|for|to)\\s+", RegexOption.IGNORE_CASE), "")
        if (clean.isBlank()) clean = "Reminder"
        return clean.replaceFirstChar { it.uppercase() }
    }

    private fun computeAlarmEpochMs(hour: Int, minute: Int, amPm: String): Long {
        val nowMs = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = nowMs
        var hour24 = hour
        val ap = amPm.lowercase().trim()
        if (ap == "pm" && hour24 < 12) hour24 += 12
        if (ap == "am" && hour24 == 12) hour24 = 0
        if (ap.isEmpty() && hour24 in 1..7 && cal.get(java.util.Calendar.HOUR_OF_DAY) >= 12) {
            hour24 += 12
        }

        cal.set(java.util.Calendar.HOUR_OF_DAY, hour24)
        cal.set(java.util.Calendar.MINUTE, minute)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)

        if (cal.timeInMillis <= nowMs) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun isActionableImperative(lower: String): Boolean {
        val actionVerbs = listOf(
            "buy", "purchase", "call", "email", "message", "send", "submit", "pay",
            "clean", "prepare", "order", "book", "schedule", "check", "fix", "repair",
            "read", "write", "study", "review", "pick up", "bring", "finish", "complete",
            "organize", "update", "cancel"
        )
        return actionVerbs.any { lower.startsWith("$it ") || lower == it }
    }

    fun extractGoal(text: String): ParsedGoal? {
        val trimmed = text.trim()
        val patterns = listOf(
            "^(?:set an?|create an?|new|add)\\s+(health|fitness|education|learning|work|career|financial|finance|personal|wellbeing|lifestyle)\\s+goal(?::|\\s+to|\\s+is\\s+to|\\s+)?\\s+(.+)$",
            "^(?:set a goal to|set goal to|create a goal to|create goal:?|new goal:?|add goal:?|my goal is to)\\s+(.+)$",
            "^(?:设定目标|新建目标|目标是?|设立目标)\\s*[:：]?\\s*(.+)$",
            "^(?:поставь цель|новая цель|создай цель|цель)\\s*[:]?\\s*(.+)$"
        )

        var explicitCategory: String? = null
        var rawContent: String? = null

        val p1 = Pattern.compile(patterns[0], Pattern.CASE_INSENSITIVE).matcher(trimmed)
        if (p1.find()) {
            explicitCategory = normalizeGoalCategory(p1.group(1))
            rawContent = p1.group(2)?.trim()
        } else {
            val p2 = Pattern.compile(patterns[1], Pattern.CASE_INSENSITIVE).matcher(trimmed)
            if (p2.find()) {
                rawContent = p2.group(1)?.trim()
            } else {
                val p3 = Pattern.compile(patterns[2], Pattern.CASE_INSENSITIVE).matcher(trimmed)
                if (p3.find()) {
                    rawContent = p3.group(1)?.trim()
                } else {
                    val p4 = Pattern.compile(patterns[3], Pattern.CASE_INSENSITIVE).matcher(trimmed)
                    if (p4.find()) {
                        rawContent = p4.group(1)?.trim()
                    }
                }
            }
        }

        if (rawContent == null || rawContent.isBlank()) return null

        var working = rawContent

        // 1. Check for trailing category: e.g. "in education", "in health", "category work"
        val catSuffixPattern = Pattern.compile("\\s+(?:in|for|under|category)\\s+(health|fitness|education|learning|work|career|financial|finance|personal|wellbeing|lifestyle)$", Pattern.CASE_INSENSITIVE)
        val catSuffixMatcher = catSuffixPattern.matcher(working)
        if (catSuffixMatcher.find()) {
            if (explicitCategory == null) {
                explicitCategory = normalizeGoalCategory(catSuffixMatcher.group(1))
            }
            working = working.substring(0, catSuffixMatcher.start()).trim()
        }

        // 2. Check for deadline / target date: e.g. "by Dec 31", "by Dec 31 2026", "before next month", "until Q4"
        var targetDate = ""
        val deadlinePattern = Pattern.compile("\\s+(?:by|before|until|deadline)\\s+([A-Za-z]+ \\d{1,2}(?:,?\\s+\\d{4})?|\\d{4}-\\d{2}-\\d{2}|[A-Za-z]+ \\d{4}|next month|end of year|end of the year|end of month|next year|summer|winter|spring|Q[1-4]|december|november|october|september|august|july|june|may|april|march|february|january)$", Pattern.CASE_INSENSITIVE)
        val deadlineMatcher = deadlinePattern.matcher(working)
        if (deadlineMatcher.find()) {
            targetDate = deadlineMatcher.group(1)?.trim()?.replaceFirstChar { it.uppercase() } ?: ""
            working = working.substring(0, deadlineMatcher.start()).trim()
        }

        // 3. Extract quantity & metric unit
        var targetAmount = 0.0
        var metricUnit = ""
        var inferredCategory: String? = null

        val unitPattern = Pattern.compile("(\\d+(?:,\\d+)*(?:\\.\\d+)?)\\s*(₹|rs\\.?|inr|rupees?|dollars?|\\$|kms?|kilometers?|kgs?|kilograms?|books?|courses?|hours?|hrs?|projects?|milestones?|days?|workouts?|sessions?|steps|pages?)", Pattern.CASE_INSENSITIVE)
        val unitMatcher = unitPattern.matcher(working)
        if (unitMatcher.find()) {
            val numStr = unitMatcher.group(1)?.replace(",", "") ?: "0"
            targetAmount = numStr.toDoubleOrNull() ?: 0.0
            val rawUnit = unitMatcher.group(2)?.lowercase() ?: ""
            when {
                rawUnit in listOf("₹", "rs", "rs.", "inr", "rupee", "rupees", "$", "dollar", "dollars") -> {
                    metricUnit = if (rawUnit.startsWith("$")) "$" else "₹"
                    inferredCategory = "Financial"
                }
                rawUnit in listOf("km", "kms", "kilometer", "kilometers") -> {
                    metricUnit = "km"
                    inferredCategory = "Health & Fitness"
                }
                rawUnit in listOf("kg", "kgs", "kilogram", "kilograms") -> {
                    metricUnit = "kg"
                    inferredCategory = "Health & Fitness"
                }
                rawUnit in listOf("workout", "workouts") -> {
                    metricUnit = "workouts"
                    inferredCategory = "Health & Fitness"
                }
                rawUnit in listOf("step", "steps") -> {
                    metricUnit = "steps"
                    inferredCategory = "Health & Fitness"
                }
                rawUnit in listOf("book", "books") -> {
                    metricUnit = "books"
                    inferredCategory = "Education & Learning"
                }
                rawUnit in listOf("course", "courses") -> {
                    metricUnit = "courses"
                    inferredCategory = "Education & Learning"
                }
                rawUnit in listOf("hour", "hours", "hr", "hrs") -> {
                    metricUnit = "hours"
                    inferredCategory = "Education & Learning"
                }
                rawUnit in listOf("project", "projects", "milestone", "milestones") -> {
                    metricUnit = "projects"
                    inferredCategory = "Work & Career"
                }
                rawUnit in listOf("day", "days") -> {
                    metricUnit = "days"
                    inferredCategory = "Personal & Habits"
                }
                rawUnit in listOf("session", "sessions") -> {
                    metricUnit = "sessions"
                    inferredCategory = "Personal & Habits"
                }
                rawUnit in listOf("page", "pages") -> {
                    metricUnit = "pages"
                    inferredCategory = "Education & Learning"
                }
            }
        } else {
            val savePattern = Pattern.compile("save\\s+(\\d+(?:,\\d+)*(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE).matcher(working)
            if (savePattern.find()) {
                val numStr = savePattern.group(1)?.replace(",", "") ?: "0"
                targetAmount = numStr.toDoubleOrNull() ?: 0.0
                metricUnit = "₹"
                inferredCategory = "Financial"
            }
        }

        // 4. Resolve Category
        val resolvedCategory = explicitCategory ?: inferredCategory ?: inferCategoryFromKeywords(working)

        if (metricUnit.isBlank()) {
            metricUnit = when {
                resolvedCategory.contains("Financial", ignoreCase = true) -> "₹"
                resolvedCategory.contains("Health", ignoreCase = true) -> "km"
                resolvedCategory.contains("Work", ignoreCase = true) -> "projects"
                resolvedCategory.contains("Education", ignoreCase = true) -> "books"
                resolvedCategory.contains("Wellbeing", ignoreCase = true) -> "sessions"
                else -> "days"
            }
        }

        // 5. Clean Title
        var cleanTitle = working.trim()
        if (cleanTitle.startsWith("to ", ignoreCase = true)) {
            cleanTitle = cleanTitle.substring(3).trim()
        }
        cleanTitle = cleanTitle.replaceFirstChar { it.uppercase() }

        return ParsedGoal(
            title = cleanTitle,
            category = resolvedCategory,
            targetAmount = targetAmount,
            metricUnit = metricUnit,
            targetDate = targetDate
        )
    }

    private fun normalizeGoalCategory(raw: String?): String {
        return when (raw?.lowercase()?.trim()) {
            "health", "fitness", "health & fitness" -> "Health & Fitness"
            "education", "learning", "study", "education & learning" -> "Education & Learning"
            "work", "career", "business", "work & career" -> "Work & Career"
            "financial", "finance", "money" -> "Financial"
            "wellbeing", "lifestyle", "mindfulness", "wellbeing & lifestyle" -> "Wellbeing & Lifestyle"
            else -> "Personal & Habits"
        }
    }

    private fun inferCategoryFromKeywords(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("read") || lower.contains("study") || lower.contains("book") || lower.contains("course") || lower.contains("certif") || lower.contains("exam") || lower.contains("learn") -> "Education & Learning"
            lower.contains("run") || lower.contains("gym") || lower.contains("workout") || lower.contains("exercise") || lower.contains("weight") || lower.contains("walk") || lower.contains("cardio") -> "Health & Fitness"
            lower.contains("save") || lower.contains("invest") || lower.contains("debt") || lower.contains("budget") || lower.contains("rupee") || lower.contains("dollar") || lower.contains("money") -> "Financial"
            lower.contains("project") || lower.contains("client") || lower.contains("launch") || lower.contains("ship") || lower.contains("sales") || lower.contains("promote") || lower.contains("job") -> "Work & Career"
            lower.contains("meditat") || lower.contains("sleep") || lower.contains("water") || lower.contains("mindful") || lower.contains("yoga") -> "Wellbeing & Lifestyle"
            else -> "Personal & Habits"
        }
    }

    private fun extractCompleteMilestone(text: String): Pair<String, String>? {
        val patterns = listOf(
            "^(?:complete milestone|mark milestone|finish milestone)\\s+(.+?)(?:\\s+(?:done|completed|as completed|as done))?\\s+(?:in|for|on)\\s+(?:project\\s+)?(.+)$",
            "^(?:mark|check off)\\s+(?:milestone\\s+)?(.+?)\\s+(?:as\\s+)?(?:completed|done)\\s+(?:in|for|on)\\s+(?:project\\s+)?(.+)$"
        )
        for (pat in patterns) {
            val m = Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(text.trim())
            if (m.find()) {
                val milestone = m.group(1)?.trim() ?: continue
                val project = m.group(2)?.trim() ?: continue
                return Pair(milestone, project)
            }
        }
        return null
    }

    private fun extractAddMilestone(text: String): Pair<String, String>? {
        val patterns = listOf(
            "^(?:add milestone|create milestone|new milestone)\\s+(.+?)\\s+(?:to|for|in)\\s+(?:project\\s+)?(.+)$"
        )
        for (pat in patterns) {
            val m = Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(text.trim())
            if (m.find()) {
                val milestone = m.group(1)?.trim() ?: continue
                val project = m.group(2)?.trim() ?: continue
                return Pair(milestone, project)
            }
        }
        return null
    }

    private fun extractProjectDeadlineUpdate(text: String): Pair<String, String>? {
        val patterns = listOf(
            "^(?:update|change|set)\\s+(?:the\\s+)?deadline\\s+(?:of|for)\\s+(?:project\\s+)?(.+?)\\s+to\\s+(.+)$",
            "^(?:set deadline (?:for|of) project|change deadline (?:for|of) project|update deadline (?:for|of) project|set project)\\s+(.+?)\\s+(?:deadline to|to deadline|to)\\s+(.+)$",
            "^(?:project\\s+)?(.+?)\\s+deadline\\s+is\\s+(.+)$"
        )
        for (pat in patterns) {
            val m = Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(text.trim())
            if (m.find()) {
                val project = m.group(1)?.trim() ?: continue
                val deadline = m.group(2)?.trim() ?: continue
                return Pair(project, deadline)
            }
        }
        return null
    }

    private fun extractProject(text: String): ProjectVoiceMatch? {
        val clean = text.trim()
        val prefixes = listOf("create a project called", "start project", "create project", "new project", "plan project")
        val prefix = prefixes.firstOrNull { clean.startsWith(it, ignoreCase = true) } ?: return null
        var remainder = clean.substring(prefix.length).trim().removePrefix(":").trim()
        if (remainder.isBlank()) return null

        var deadline = ""
        var milestones = emptyList<String>()

        // Check for milestones
        val milestoneSplit = Pattern.compile("\\s+(?:with|and)?\\s*milestones?:?\\s*", Pattern.CASE_INSENSITIVE).split(remainder)
        if (milestoneSplit.size > 1) {
            remainder = milestoneSplit[0].trim()
            milestones = milestoneSplit[1].split(Regex("[,;]|\\band\\b")).map { it.trim() }.filter { it.isNotBlank() }
        }

        // Check for deadline
        val deadlineMatch = Pattern.compile("\\s+(?:with deadline|due|deadline)\\s+(.+)$", Pattern.CASE_INSENSITIVE).matcher(remainder)
        if (deadlineMatch.find()) {
            deadline = deadlineMatch.group(1)?.trim() ?: ""
            remainder = remainder.substring(0, deadlineMatch.start()).trim()
        }

        val name = remainder.trim()
        if (name.isBlank()) return null
        return ProjectVoiceMatch(name = name, deadline = deadline, milestones = milestones)
    }

    private fun extractTaskCreation(text: String): Pair<String, Priority>? {
        var priority = Priority.NORMAL
        val lower = text.lowercase()
        if (lower.contains("urgent") || lower.contains("critical") || lower.contains("紧急") || lower.contains("срочно")) priority = Priority.CRITICAL
        else if (lower.contains("important") || lower.contains("high priority") || lower.contains("重要") || lower.contains("важно")) priority = Priority.HIGH
        else if (lower.contains("low priority")) priority = Priority.LOW

        val patterns = listOf(
            "^(?:create a task to|create task to|create task|add a task to|add task|ad task|crate task|new task|make task|task:?|add)\\s+(.+?)(?:\\s+to my tasks)?$",
            "^(?:remind me to|i need to|todo:?)\\s+(.+)$",
            // Chinese patterns
            "^(?:新建任务|添加任务|记下任务|创建任务|新任务|任务)\\s*[:：]?\\s*(.+)$",
            // Russian patterns
            "^(?:добавь задачу|добавить задачу|новая задача|создай задачу|задача)\\s*[:]?\\s*(.+)$",
            // Hindi / Hinglish patterns
            "^(?:naya task|task likho|task banao)\\s*[:]?\\s*(.+)$",
            "^(.+?)\\s*(?:ka task banao|ka task likho|likho|yaad dilana)$"
        )

        for (pat in patterns) {
            val m = Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(text.trim())
            if (m.find()) {
                var title = m.group(1)?.trim() ?: continue
                title = title.replace(Regex("(?i)\\b(urgent|critical|important|high priority|low priority)\\b"), "").trim()
                if (title.isNotEmpty()) {
                    return Pair(title.replaceFirstChar { it.uppercase() }, priority)
                }
            }
        }
        return null
    }
}
