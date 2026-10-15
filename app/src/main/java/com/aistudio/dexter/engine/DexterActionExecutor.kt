package com.aistudio.dexter.engine

import android.content.Context
import com.aistudio.dexter.data.model.ActionResult
import com.aistudio.dexter.data.model.Priority
import com.aistudio.dexter.data.repository.*

class DexterActionExecutor(
    private val taskRepo: TaskRepository,
    private val financeRepo: FinanceRepository,
    private val alarmRepo: AlarmRepository,
    private val journalRepo: JournalRepository,
    private val auditRepo: AuditRepository,
    private val repository: DexterRepository
) {

    constructor(context: Context) : this(
        taskRepo = TaskRepository(context),
        financeRepo = FinanceRepository(context),
        alarmRepo = AlarmRepository(context),
        journalRepo = JournalRepository(context),
        auditRepo = AuditRepository(context),
        repository = DexterRepository(context)
    )

    constructor(repository: DexterRepository, context: Context) : this(
        taskRepo = TaskRepository(context),
        financeRepo = FinanceRepository(context),
        alarmRepo = AlarmRepository(context),
        journalRepo = JournalRepository(context),
        auditRepo = AuditRepository(context),
        repository = repository
    )

    /**
     * Executes validated ActionContract. Enforces confirmation for destructive mutations.
     */
    suspend fun executeContract(action: ActionContract): ActionResult {
        val intent = action.intent.uppercase().trim()

        // Enforce Confirmation for destructive operations unless explicitly confirmed
        if (DexterContractValidator.isDestructive(intent) && !action.confirmed) {
            val prompt = when (intent) {
                "COMPLETE_TASK" -> {
                    val id = action.parameters["task_id"] as? Long ?: 0L
                    val task = taskRepo.getTaskById(id)
                    if (task != null) "Complete task '${task.title}'? Confirm?" else "Complete task #$id? Confirm?"
                }
                "CANCEL_TASK" -> {
                    val id = action.parameters["task_id"] as? Long ?: 0L
                    val task = taskRepo.getTaskById(id)
                    if (task != null) "Cancel task '${task.title}'? Confirm?" else "Cancel task #$id? Confirm?"
                }
                "COMPLETE_ALL_TASKS" -> {
                    val pending = taskRepo.listPendingTasks()
                    "Mark all ${pending.size} pending tasks complete? Confirm?"
                }
                "CANCEL_ALARMS" -> {
                    val alarms = alarmRepo.queryPendingAlarms()
                    "Cancel all ${alarms.size} active alarms? Confirm?"
                }
                else -> "Execute destructive action $intent? Confirm?"
            }
            return ActionResult(
                success = false,
                intent = intent,
                message = prompt,
                errorCode = "CONFIRMATION_REQUIRED",
                data = mapOf("requiresConfirmation" to true, "prompt" to prompt)
            )
        }

        return when (intent) {
            // 1. CREATE_TASK
            "CREATE_TASK" -> {
                val title = action.parameters["title"] as? String ?: "New Task"
                val priority = action.parameters["priority"] as? Priority ?: Priority.NORMAL
                val projectId = action.parameters["project_id"] as? Long
                val dueAt = action.parameters["due_at"] as? Long
                val start = action.parameters["scheduled_start"] as? Long
                val end = action.parameters["scheduled_end"] as? Long
                val dur = action.parameters["estimated_duration_seconds"] as? Long

                val task = taskRepo.createTask(
                    title = title,
                    priority = priority,
                    projectId = projectId,
                    dueAt = dueAt,
                    scheduledStart = start,
                    scheduledEnd = end,
                    durationSeconds = dur,
                    actorType = action.actorType
                )
                val prioritySuffix = if (task.priority != Priority.NORMAL && task.priority != Priority.SOMEDAY) {
                    " with ${task.priority.name.lowercase().replaceFirstChar { it.uppercase() }} priority"
                } else ""
                val dueSuffix = if (task.dueAt != null && task.dueAt > 0) " due ${task.humanDueTime.lowercase()}" else ""
                val confirmationMsg = "Added task '${task.title}'$prioritySuffix$dueSuffix."

                ActionResult(
                    success = true,
                    intent = "CREATE_TASK",
                    message = confirmationMsg,
                    entityType = "TASK",
                    entityId = task.id,
                    data = mapOf("task" to task)
                )
            }

            // 2. LIST_PENDING_TASKS
            "LIST_PENDING_TASKS" -> {
                val tasks = taskRepo.listPendingTasks()
                val msg = if (tasks.isEmpty()) {
                    "You have no pending tasks."
                } else {
                    "You have ${tasks.size} pending tasks. Top task is '${tasks.first().title}'."
                }
                ActionResult(
                    success = true,
                    intent = "LIST_PENDING_TASKS",
                    message = msg,
                    entityType = "TASK",
                    data = mapOf("count" to tasks.size, "tasks" to tasks)
                )
            }

            // 3. COMPLETE_TASK
            "COMPLETE_TASK" -> {
                val taskId = action.parameters["task_id"] as? Long ?: 0L
                val success = taskRepo.completeTask(taskId, completionSource = "DEXTER_RECORDED", actorType = action.actorType)
                if (success) {
                    ActionResult(
                        success = true,
                        intent = "COMPLETE_TASK",
                        message = "Marked task #$taskId complete.",
                        entityType = "TASK",
                        entityId = taskId
                    )
                } else {
                    ActionResult(
                        success = false,
                        intent = "COMPLETE_TASK",
                        message = "Task #$taskId was not found.",
                        errorCode = "TASK_NOT_FOUND"
                    )
                }
            }

            // 4. USER_REPORT_TASK_COMPLETION
            "USER_REPORT_TASK_COMPLETION" -> {
                val taskId = action.parameters["task_id"] as? Long ?: 0L
                val success = taskRepo.userReportTaskCompletion(taskId)
                if (success) {
                    val task = taskRepo.getTaskById(taskId)
                    val title = task?.title ?: "#$taskId"
                    ActionResult(
                        success = true,
                        intent = "USER_REPORT_TASK_COMPLETION",
                        message = "Recorded that you completed task '$title'.",
                        entityType = "TASK",
                        entityId = taskId
                    )
                } else {
                    ActionResult(
                        success = false,
                        intent = "USER_REPORT_TASK_COMPLETION",
                        message = "Task #$taskId was not found.",
                        errorCode = "TASK_NOT_FOUND"
                    )
                }
            }

            // 5. CANCEL_TASK
            "CANCEL_TASK" -> {
                val taskId = action.parameters["task_id"] as? Long ?: 0L
                val success = taskRepo.cancelTask(taskId, actorType = action.actorType)
                if (success) {
                    ActionResult(
                        success = true,
                        intent = "CANCEL_TASK",
                        message = "Cancelled task #$taskId.",
                        entityType = "TASK",
                        entityId = taskId
                    )
                } else {
                    ActionResult(
                        success = false,
                        intent = "CANCEL_TASK",
                        message = "Task #$taskId was not found.",
                        errorCode = "TASK_NOT_FOUND"
                    )
                }
            }

            // 6. COMPLETE_ALL_TASKS
            "COMPLETE_ALL_TASKS" -> {
                val count = taskRepo.completeAllTasks(actorType = action.actorType)
                ActionResult(
                    success = true,
                    intent = "COMPLETE_ALL_TASKS",
                    message = if (count > 0) "Marked all $count pending tasks complete." else "No pending tasks to complete.",
                    data = mapOf("count" to count)
                )
            }

            // 7. LIST_COMPLETED_TASKS
            "LIST_COMPLETED_TASKS" -> {
                val tasks = taskRepo.listCompletedTasks()
                ActionResult(
                    success = true,
                    intent = "LIST_COMPLETED_TASKS",
                    message = "You have ${tasks.size} completed tasks.",
                    entityType = "TASK",
                    data = mapOf("count" to tasks.size, "tasks" to tasks)
                )
            }

            // 8. LOG_EXPENSE & LOG_TRANSACTION
            "LOG_EXPENSE", "LOG_TRANSACTION" -> {
                val amountMinor = (action.parameters["amountMinor"] as? Number)?.toLong()
                    ?: (((action.parameters["amount"] as? Number)?.toDouble() ?: 0.0) * 100).toLong()
                val desc = action.parameters["description"] as? String ?: "Expense"
                val category = action.parameters["category"] as? String ?: "General"
                val type = (action.parameters["type"] as? String)?.uppercase() ?: if (intent == "LOG_EXPENSE") "DEBIT" else "CREDIT"
                val paymentMode = action.parameters["payment_mode"] as? String ?: "UPI"

                val tx = financeRepo.logTransaction(
                    amountMinor = amountMinor,
                    desc = desc,
                    type = type,
                    category = category,
                    paymentMode = paymentMode,
                    actorType = action.actorType
                )

                val formattedAmount = if (tx.amountRupees % 1.0 == 0.0) {
                    tx.amountRupees.toInt().toString()
                } else {
                    String.format(java.util.Locale.US, "%.2f", tx.amountRupees)
                }

                val confirmationMsg = if (tx.type == "CREDIT") {
                    "Recorded income of ₹$formattedAmount for ${tx.description} under ${tx.category}."
                } else {
                    "Logged ₹$formattedAmount for ${tx.description} under ${tx.category}."
                }

                ActionResult(
                    success = true,
                    intent = intent,
                    message = confirmationMsg,
                    entityType = "FINANCE",
                    entityId = tx.id,
                    data = mapOf("transaction" to tx)
                )
            }

            // 9. SUMMARIZE_EXPENSES
            "SUMMARIZE_EXPENSES" -> {
                val (todaySpent, monthSpent) = financeRepo.summarizeExpenses()
                ActionResult(
                    success = true,
                    intent = "SUMMARIZE_EXPENSES",
                    message = "You've spent ₹${todaySpent / 100} today, and ₹${monthSpent / 100} this month.",
                    entityType = "FINANCE",
                    data = mapOf("todaySpent" to todaySpent, "monthSpent" to monthSpent)
                )
            }

            // 10. LOG_JOURNAL
            "LOG_JOURNAL" -> {
                val content = action.parameters["content"] as? String ?: ""
                val title = action.parameters["title"] as? String ?: ""
                val mood = action.parameters["mood"] as? String ?: "Reflective"
                val category = action.parameters["category"] as? String ?: "Reflections"

                val entry = journalRepo.logJournal(
                    content = content,
                    title = title,
                    mood = mood,
                    category = category,
                    source = "VOICE",
                    actorType = action.actorType
                )

                val confirmationMsg = if (category.isNotBlank() && category != "Reflections" && category != "General") {
                    "Saved diary note under $category."
                } else {
                    "Saved diary note with $mood mood."
                }

                ActionResult(
                    success = true,
                    intent = "LOG_JOURNAL",
                    message = confirmationMsg,
                    entityType = "JOURNAL",
                    entityId = entry.id,
                    data = mapOf("entry" to entry)
                )
            }

            // 11. LIST_JOURNAL
            "LIST_JOURNAL" -> {
                val notes = journalRepo.listNotes(limit = 5)
                val msg = if (notes.isEmpty()) {
                    "You have no journal notes yet."
                } else {
                    "You have ${notes.size} recent notes. Latest: '${notes.first().content.take(60)}'."
                }
                ActionResult(
                    success = true,
                    intent = "LIST_JOURNAL",
                    message = msg,
                    entityType = "JOURNAL",
                    data = mapOf("notes" to notes)
                )
            }

            // 12. LIST_ALARMS
            "LIST_ALARMS" -> {
                val alarms = alarmRepo.queryPendingAlarms()
                val msg = if (alarms.isEmpty()) "You have no active alarms." else "You have ${alarms.size} active alarms."
                ActionResult(
                    success = true,
                    intent = "LIST_ALARMS",
                    message = msg,
                    entityType = "ALARM",
                    data = mapOf("alarms" to alarms)
                )
            }

            // 13. CANCEL_ALARMS
            "CANCEL_ALARMS" -> {
                val count = alarmRepo.cancelAlarms(actorType = action.actorType)
                ActionResult(
                    success = true,
                    intent = "CANCEL_ALARMS",
                    message = if (count > 0) "Turned off all $count alarms." else "No alarms to turn off.",
                    data = mapOf("count" to count)
                )
            }

            // 14. DAILY_BRIEFING
            "DAILY_BRIEFING" -> {
                val briefing = repository.getDailyBriefing()
                ActionResult(
                    success = true,
                    intent = "DAILY_BRIEFING",
                    message = briefing.summaryText,
                    entityType = "BRIEFING",
                    data = mapOf("briefing" to briefing)
                )
            }

            // 15. CREATE_GOAL
            "CREATE_GOAL" -> {
                val title = action.parameters["title"] as? String ?: "New Goal"
                val goal = repository.createGoal(title)
                ActionResult(
                    success = true,
                    intent = "CREATE_GOAL",
                    message = "Goal '${goal.name}' created.",
                    entityType = "GOAL",
                    entityId = goal.id,
                    data = mapOf("goal" to goal)
                )
            }

            // 16. LIST_GOALS
            "LIST_GOALS" -> {
                val goals = repository.listGoals()
                ActionResult(
                    success = true,
                    intent = "LIST_GOALS",
                    message = "You have ${goals.size} active goals.",
                    entityType = "GOAL",
                    data = mapOf("goals" to goals)
                )
            }

            // 17. CREATE_PROJECT
            "CREATE_PROJECT" -> {
                val name = action.parameters["name"] as? String ?: "New Project"
                val proj = repository.createProject(name)
                ActionResult(
                    success = true,
                    intent = "CREATE_PROJECT",
                    message = "Project '${proj.name}' started.",
                    entityType = "PROJECT",
                    entityId = proj.id,
                    data = mapOf("project" to proj)
                )
            }

            // 18. LIST_PROJECTS
            "LIST_PROJECTS" -> {
                val projs = repository.listProjects()
                ActionResult(
                    success = true,
                    intent = "LIST_PROJECTS",
                    message = "You have ${projs.size} active projects.",
                    entityType = "PROJECT",
                    data = mapOf("projects" to projs)
                )
            }

            else -> {
                ActionResult(
                    success = false,
                    intent = intent,
                    message = "Unsupported action intent: $intent",
                    errorCode = "UNSUPPORTED_INTENT"
                )
            }
        }
    }

    /**
     * Compatibility bridge for ParsedIntent from fast-path voice pipeline.
     */
    suspend fun execute(parsedIntent: ParsedIntent): ActionResult {
        if (parsedIntent.isNegationOrClarification) {
            return if (parsedIntent.intent == "NEGATION_SAFETY") {
                ActionResult(
                    success = true,
                    intent = "NEGATION_SAFETY",
                    message = "Understood. Action was safely cancelled."
                )
            } else {
                ActionResult(
                    success = false,
                    intent = "UNKNOWN",
                    message = "I couldn't quite understand. You can ask for tasks, log expenses, take notes, or ask for your daily briefing.",
                    errorCode = "CLARIFICATION_NEEDED"
                )
            }
        }

        // Bridge to contract format
        val params = mutableMapOf<String, Any?>()
        parsedIntent.parameters.forEach { (k, v) ->
            if (k == "taskId") params["task_id"] = v
            else if (k == "amountRupees") params["amount"] = v
            else if (k == "amountMinor") {
                params["amountMinor"] = v
                params["amount"] = (v as Long) / 100.0
            }
            else params[k] = v
        }

        val contract = ActionContract(
            intent = parsedIntent.intent,
            parameters = params,
            confirmed = !parsedIntent.requiresConfirmation
        )

        return executeContract(contract)
    }
}
