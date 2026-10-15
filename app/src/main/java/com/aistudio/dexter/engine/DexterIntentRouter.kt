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

        // 7. Finance / Expense & Income Logging
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

        // 8. Finance Summary
        if (lower.contains("how much did i spend") || lower.contains("total expenses") || lower.contains("my spending") || lower.contains("summarize expenses") || lower.contains("expense summary") || lower.contains("what did i spend") || lower.contains("how much money")) {
            return ParsedIntent("SUMMARIZE_EXPENSES")
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

        if (lower.contains("show notes") || lower.contains("read notes") || lower.contains("my notes") || lower.contains("show journal") || lower.contains("what did i journal") || lower.contains("list notes")) {
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

        return null
    }

    /**
     * Intelligent Category Classifier for Finance.
     * Evaluates semantic keywords to automatically assign categories complying with Rule 17.
     */
    fun inferFinanceCategory(description: String, type: String = "DEBIT"): String {
        val lower = description.lowercase()

        // Transportation: traveling, travel, auto, cab, uber, ola, bus, train, flight, metro, petrol, diesel, fuel, toll, parking
        if (lower.contains(Regex("\\b(traveling|travel|travelling|auto|cab|taxi|uber|ola|bus|train|flight|airline|metro|petrol|diesel|fuel|toll|fastag|parking|commute|transit|rickshaw)\\b"))) {
            return "Transportation"
        }

        // Food & Dining: lunch, dinner, breakfast, snacks, coffee, tea, chai, swiggy, zomato, groceries, vegetables, milk, restaurant
        if (lower.contains(Regex("\\b(food|lunch|dinner|breakfast|brunch|snacks?|coffee|cafe|tea|chai|swiggy|zomato|groceries|grocery|vegetables?|veggies?|fruits?|milk|bread|restaurant|bistro|burger|pizza|bakery|dining|meal|drinks?|barista)\\b"))) {
            return "Food & Dining"
        }

        // Utilities & Bills: electricity, water, internet, wifi, mobile, recharge, gas, rent, maintenance
        if (lower.contains(Regex("\\b(electricity|electric|power|water|internet|wifi|broadband|mobile|recharge|phone|sim|gas|cylinder|lpg|rent|maintenance|bills?|dth|tv|society)\\b"))) {
            return "Utilities & Bills"
        }

        // Shopping: clothes, shoes, amazon, flipkart, mall, electronics, gadgets
        if (lower.contains(Regex("\\b(clothes|clothing|shoes?|shirt|pants?|dress|amazon|flipkart|myntra|mall|electronics|gadgets?|laptop|phone case|shopping|purchases?|market|supermarket|store|apparel)\\b"))) {
            return "Shopping"
        }

        // Health & Medical: medicine, doctor, pharmacy, clinic, test, hospital, gym, supplements
        if (lower.contains(Regex("\\b(medicines?|meds|pills?|doctor|physician|pharmacy|chemist|clinic|tests?|hospital|gym|fitness|workout|supplements?|vitamins?|dentist|medical|health|pharma)\\b"))) {
            return "Health & Medical"
        }

        // Entertainment: movie, netflix, game, party, outing, concert
        if (lower.contains(Regex("\\b(movies?|cinema|theatre|netflix|prime|hotstar|games?|gaming|party|outing|concert|shows?|club|pub|weekend|subscriptions?|entertainment)\\b"))) {
            return "Entertainment"
        }

        // Salary & Income
        if (type == "CREDIT" || lower.contains(Regex("\\b(salary|stipend|consulting|bonus|freelance|dividends?|interest|cashback|refund|client|wage|income)\\b"))) {
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
        val now = System.currentTimeMillis() / 1000

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
                title = title.replace(Regex("(?i)\\b(urgent|critical|important|high priority|low priority)\\b"), "").trim()
                if (title.isNotEmpty()) {
                    return Pair(title.replaceFirstChar { it.uppercase() }, priority)
                }
            }
        }
        return null
    }
}
