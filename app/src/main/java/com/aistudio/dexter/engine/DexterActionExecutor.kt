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
    private val repository: DexterRepository,
    private val context: Context? = null
) {

    constructor(context: Context) : this(
        taskRepo = TaskRepository(context),
        financeRepo = FinanceRepository(context),
        alarmRepo = AlarmRepository(context),
        journalRepo = JournalRepository(context),
        auditRepo = AuditRepository(context),
        repository = DexterRepository(context),
        context = context
    )

    constructor(repository: DexterRepository, context: Context) : this(
        taskRepo = TaskRepository(context),
        financeRepo = FinanceRepository(context),
        alarmRepo = AlarmRepository(context),
        journalRepo = JournalRepository(context),
        auditRepo = AuditRepository(context),
        repository = repository,
        context = context
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

            // 1b. ALARM_I_AM_AWAKE
            "ALARM_I_AM_AWAKE" -> {
                val pending = taskRepo.listPendingTasks()
                val projects = repository.listProjects()
                val msg = buildString {
                    append("Good morning! Fantastic to hear you are up. ")
                    if (pending.isEmpty()) {
                        append("You have no urgent tasks scheduled right now. ")
                    } else {
                        append("You have ${pending.size} pending tasks today. First on your list is '${pending.first().title}'. ")
                    }
                    if (projects.isNotEmpty()) {
                        append("Your active project '${projects.first().name}' is at ${projects.first().progressPercent} percent. ")
                    }
                    append("Let's make today great!")
                }
                ActionResult(
                    success = true,
                    intent = "ALARM_I_AM_AWAKE",
                    message = msg
                )
            }

            // 1c. ALARM_SNOOZE
            "ALARM_SNOOZE" -> {
                val minutes = (action.parameters["minutes"] as? Int ?: 5).coerceIn(1, 60)
                val newAlarmTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
                val alarmId = repository.createAlarm(newAlarmTime, "Snoozed Alarm ($minutes mins)")
                context?.let { ctx ->
                    com.aistudio.dexter.receiver.DexterAlarmReceiver.scheduleExactAlarm(ctx, alarmId, "Snoozed Reminder", newAlarmTime / 1000L)
                }
                ActionResult(
                    success = true,
                    intent = "ALARM_SNOOZE",
                    message = "Alarm snoozed for $minutes minutes. Rest up, I will check in on you again soon."
                )
            }

            // 1d. ALARM_DISMISS
            "ALARM_DISMISS" -> {
                repository.dismissRecentAlarm()
                ActionResult(
                    success = true,
                    intent = "ALARM_DISMISS",
                    message = "Alarm dismissed. Have a wonderful and productive day!"
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

            // 3b. COMPLETE_TASK_TITLE
            "COMPLETE_TASK_TITLE" -> {
                val rawTitle = (action.parameters["title"] as? String)?.trim() ?: ""
                val pending = taskRepo.listPendingTasks()
                val isPronoun = rawTitle.isEmpty() || rawTitle.equals("this", ignoreCase = true) ||
                        rawTitle.equals("this task", ignoreCase = true) || rawTitle.equals("the task", ignoreCase = true) ||
                        rawTitle.equals("it", ignoreCase = true) || rawTitle.equals("current task", ignoreCase = true)

                val targetTask = if (isPronoun) {
                    pending.firstOrNull()
                } else {
                    pending.firstOrNull { it.title.equals(rawTitle, ignoreCase = true) }
                        ?: pending.firstOrNull { it.title.contains(rawTitle, ignoreCase = true) }
                }

                if (targetTask != null) {
                    val success = taskRepo.completeTask(targetTask.id, completionSource = "DEXTER_RECORDED", actorType = action.actorType)
                    if (success) {
                        ActionResult(
                            success = true,
                            intent = "COMPLETE_TASK_TITLE",
                            message = "Marked task '${targetTask.title}' complete.",
                            entityType = "TASK",
                            entityId = targetTask.id,
                            data = mapOf("taskId" to targetTask.id, "title" to targetTask.title)
                        )
                    } else {
                        ActionResult(
                            success = false,
                            intent = "COMPLETE_TASK_TITLE",
                            message = "Could not complete task '${targetTask.title}'.",
                            errorCode = "UPDATE_FAILED"
                        )
                    }
                } else {
                    val msg = if (isPronoun) "You have no active pending tasks to complete." else "Could not find a pending task matching '$rawTitle'."
                    ActionResult(
                        success = false,
                        intent = "COMPLETE_TASK_TITLE",
                        message = msg,
                        errorCode = "TASK_NOT_FOUND"
                    )
                }
            }

            // 3c. CANCEL_TASK_TITLE
            "CANCEL_TASK_TITLE" -> {
                val rawTitle = (action.parameters["title"] as? String)?.trim() ?: ""
                val pending = taskRepo.listPendingTasks()
                val isPronoun = rawTitle.isEmpty() || rawTitle.equals("this", ignoreCase = true) ||
                        rawTitle.equals("this task", ignoreCase = true) || rawTitle.equals("it", ignoreCase = true)

                val targetTask = if (isPronoun) {
                    pending.firstOrNull()
                } else {
                    pending.firstOrNull { it.title.equals(rawTitle, ignoreCase = true) }
                        ?: pending.firstOrNull { it.title.contains(rawTitle, ignoreCase = true) }
                }

                if (targetTask != null) {
                    val success = taskRepo.deleteTask(targetTask.id)
                    if (success) {
                        ActionResult(
                            success = true,
                            intent = "CANCEL_TASK_TITLE",
                            message = "Cancelled task '${targetTask.title}'.",
                            entityType = "TASK",
                            entityId = targetTask.id
                        )
                    } else {
                        ActionResult(
                            success = false,
                            intent = "CANCEL_TASK_TITLE",
                            message = "Could not cancel task '${targetTask.title}'.",
                            errorCode = "UPDATE_FAILED"
                        )
                    }
                } else {
                    val msg = if (isPronoun) "You have no active pending tasks to cancel." else "Could not find a pending task matching '$rawTitle'."
                    ActionResult(
                        success = false,
                        intent = "CANCEL_TASK_TITLE",
                        message = msg,
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
                val title = action.parameters["title"] as? String ?: (action.parameters["name"] as? String ?: "New Goal")
                val category = action.parameters["category"] as? String ?: "Personal"
                val amount = (action.parameters["target_amount"] as? Number)?.toDouble()
                    ?: (action.parameters["target_amount"] as? String)?.toDoubleOrNull() ?: 0.0
                val targetDate = action.parameters["target_date"] as? String ?: ""
                val unit = action.parameters["metric_unit"] as? String ?: (if (category.equals("Financial", ignoreCase = true)) "₹" else "units")
                val goal = repository.createGoal(title, category, amount, targetDate, unit)
                val targetPart = if (amount > 0) {
                    if (unit == "₹") " with target ₹${formatAmount(amount)}" else " with target ${formatAmount(amount)} $unit"
                } else ""
                val datePart = if (targetDate.isNotBlank()) " by $targetDate" else ""
                val msg = "Created $category goal '${goal.name}'$targetPart$datePart."
                ActionResult(
                    success = true,
                    intent = "CREATE_GOAL",
                    message = msg,
                    entityType = "GOAL",
                    entityId = goal.id,
                    data = mapOf("goal" to goal)
                )
            }

            // 16. LIST_GOALS
            "LIST_GOALS" -> {
                val goals = repository.listGoals()
                val summary = if (goals.isEmpty()) {
                    "You don't have any active goals yet."
                } else {
                    "You have ${goals.size} active goals. Top goals are: " + goals.take(3).joinToString("; ") { "${it.name} at ${it.progressPercent}%" }
                }
                ActionResult(
                    success = true,
                    intent = "LIST_GOALS",
                    message = summary,
                    entityType = "GOAL",
                    data = mapOf("goals" to goals)
                )
            }

            // 17. CREATE_PROJECT
            "CREATE_PROJECT" -> {
                val name = action.parameters["name"] as? String ?: "New Project"
                val desc = action.parameters["description"] as? String ?: ""
                val deadline = action.parameters["deadline"] as? String ?: ""
                val proj = repository.createProject(name, desc, deadline)

                val milestones = action.parameters["milestones"] as? List<*>
                val addedMilestones = mutableListOf<String>()
                if (milestones != null) {
                    for (m in milestones) {
                        val mStr = m?.toString()?.trim()
                        if (!mStr.isNullOrBlank()) {
                            repository.createMilestone(proj.id, mStr)
                            addedMilestones.add(mStr)
                        }
                    }
                }

                val msg = buildString {
                    append("Project '${proj.name}' created")
                    if (deadline.isNotBlank()) append(" with deadline $deadline")
                    if (addedMilestones.isNotEmpty()) append(" and ${addedMilestones.size} milestones")
                    append(".")
                }
                ActionResult(
                    success = true,
                    intent = "CREATE_PROJECT",
                    message = msg,
                    entityType = "PROJECT",
                    entityId = proj.id,
                    data = mapOf("project" to proj)
                )
            }

            // 17b. CREATE_MILESTONE
            "CREATE_MILESTONE" -> {
                val milestoneTitle = (action.parameters["milestone"] as? String ?: action.parameters["title"] as? String ?: "").trim()
                val projectName = (action.parameters["project_name"] as? String ?: action.parameters["project"] as? String ?: "").trim()

                val allProjects = repository.listProjects()
                val targetProject = if (projectName.isNotBlank()) {
                    allProjects.firstOrNull { it.name.equals(projectName, ignoreCase = true) }
                        ?: allProjects.firstOrNull { it.name.contains(projectName, ignoreCase = true) }
                } else {
                    allProjects.firstOrNull()
                }

                if (targetProject != null && milestoneTitle.isNotBlank()) {
                    repository.createMilestone(targetProject.id, milestoneTitle)
                    ActionResult(
                        success = true,
                        intent = "CREATE_MILESTONE",
                        message = "Added milestone '$milestoneTitle' to project '${targetProject.name}'.",
                        entityType = "PROJECT",
                        entityId = targetProject.id
                    )
                } else if (targetProject == null && projectName.isNotBlank()) {
                    val newProj = repository.createProject(projectName)
                    repository.createMilestone(newProj.id, milestoneTitle)
                    ActionResult(
                        success = true,
                        intent = "CREATE_MILESTONE",
                        message = "Started project '$projectName' and added milestone '$milestoneTitle'.",
                        entityType = "PROJECT",
                        entityId = newProj.id
                    )
                } else {
                    ActionResult(
                        success = false,
                        intent = "CREATE_MILESTONE",
                        message = "Which project would you like to add this milestone to?"
                    )
                }
            }

            // 17c. COMPLETE_MILESTONE
            "COMPLETE_MILESTONE" -> {
                val milestoneTitle = (action.parameters["milestone"] as? String ?: action.parameters["title"] as? String ?: "").trim()
                val projectName = (action.parameters["project_name"] as? String ?: action.parameters["project"] as? String ?: "").trim()

                val allProjects = repository.listProjects()
                val targetProject = if (projectName.isNotBlank()) {
                    allProjects.firstOrNull { it.name.equals(projectName, ignoreCase = true) }
                        ?: allProjects.firstOrNull { it.name.contains(projectName, ignoreCase = true) }
                } else {
                    allProjects.firstOrNull()
                }

                if (targetProject != null) {
                    val milestones = repository.listMilestones(targetProject.id)
                    val targetMilestone = if (milestoneTitle.isNotBlank()) {
                        milestones.firstOrNull { it.title.equals(milestoneTitle, ignoreCase = true) }
                            ?: milestones.firstOrNull { it.title.contains(milestoneTitle, ignoreCase = true) }
                    } else {
                        milestones.firstOrNull { !it.isCompleted }
                    }

                    if (targetMilestone != null) {
                        repository.toggleMilestone(targetMilestone.id, true, targetProject.id)
                        val updated = repository.listProjects().firstOrNull { it.id == targetProject.id }
                        val prog = updated?.progressPercent ?: 100
                        ActionResult(
                            success = true,
                            intent = "COMPLETE_MILESTONE",
                            message = "Marked milestone '${targetMilestone.title}' completed in '${targetProject.name}'. Progress is now $prog%.",
                            entityType = "PROJECT",
                            entityId = targetProject.id
                        )
                    } else {
                        ActionResult(
                            success = false,
                            intent = "COMPLETE_MILESTONE",
                            message = "I couldn't find an incomplete milestone matching '$milestoneTitle' in project '${targetProject.name}'."
                        )
                    }
                } else {
                    ActionResult(
                        success = false,
                        intent = "COMPLETE_MILESTONE",
                        message = "I couldn't find project '$projectName'."
                    )
                }
            }

            // 17d. UPDATE_PROJECT_DEADLINE
            "UPDATE_PROJECT_DEADLINE" -> {
                val projectName = (action.parameters["project_name"] as? String ?: action.parameters["name"] as? String ?: "").trim()
                val deadline = (action.parameters["deadline"] as? String ?: "").trim()

                val allProjects = repository.listProjects()
                val targetProject = if (projectName.isNotBlank()) {
                    allProjects.firstOrNull { it.name.equals(projectName, ignoreCase = true) }
                        ?: allProjects.firstOrNull { it.name.contains(projectName, ignoreCase = true) }
                } else {
                    allProjects.firstOrNull()
                }

                if (targetProject != null && deadline.isNotBlank()) {
                    repository.updateProjectDeadline(targetProject.id, deadline)
                    ActionResult(
                        success = true,
                        intent = "UPDATE_PROJECT_DEADLINE",
                        message = "Updated deadline for project '${targetProject.name}' to $deadline.",
                        entityType = "PROJECT",
                        entityId = targetProject.id
                    )
                } else {
                    ActionResult(
                        success = false,
                        intent = "UPDATE_PROJECT_DEADLINE",
                        message = "Please specify which project and the target deadline."
                    )
                }
            }

            // 17e. GET_PROJECT_STATUS
            "GET_PROJECT_STATUS" -> {
                val projectName = (action.parameters["project_name"] as? String ?: action.parameters["name"] as? String ?: "").trim()
                val allProjects = repository.listProjects()
                val targetProject = if (projectName.isNotBlank()) {
                    allProjects.firstOrNull { it.name.equals(projectName, ignoreCase = true) }
                        ?: allProjects.firstOrNull { it.name.contains(projectName, ignoreCase = true) }
                } else {
                    allProjects.firstOrNull()
                }

                if (targetProject != null) {
                    val milestones = repository.listMilestones(targetProject.id)
                    val completed = milestones.count { it.isCompleted }
                    val total = milestones.size
                    val deadlineMsg = if (targetProject.deadline.isNotBlank()) " Target deadline is ${targetProject.deadline}." else ""
                    val pendingList = milestones.filter { !it.isCompleted }.take(2).joinToString { it.title }
                    val nextUpMsg = if (pendingList.isNotBlank()) " Next up: $pendingList." else ""
                    ActionResult(
                        success = true,
                        intent = "GET_PROJECT_STATUS",
                        message = "Project '${targetProject.name}' is ${targetProject.status} at ${targetProject.progressPercent}% completion with $completed of $total milestones done.$deadlineMsg$nextUpMsg",
                        entityType = "PROJECT",
                        entityId = targetProject.id,
                        data = mapOf("project" to targetProject, "milestones" to milestones)
                    )
                } else {
                    val count = allProjects.size
                    ActionResult(
                        success = true,
                        intent = "GET_PROJECT_STATUS",
                        message = "You have $count projects. Which one would you like to check?",
                        entityType = "PROJECT"
                    )
                }
            }

            // 18. LIST_PROJECTS
            "LIST_PROJECTS" -> {
                val projs = repository.listProjects()
                val message = if (projs.isEmpty()) {
                    "You don't have any projects yet. Say 'create project' followed by the name to start one."
                } else {
                    val summaryList = projs.take(3).joinToString("; ") { "${it.name} at ${it.progressPercent}% with ${it.taskCount} milestones" }
                    "You have ${projs.size} projects. $summaryList."
                }
                ActionResult(
                    success = true,
                    intent = "LIST_PROJECTS",
                    message = message,
                    entityType = "PROJECT",
                    data = mapOf("projects" to projs)
                )
            }

            // 19. CREATE_ALARM
            "CREATE_ALARM" -> {
                val label = action.parameters["label"] as? String ?: "Reminder"
                val rawTime = (action.parameters["alarm_time"] as? Number)?.toLong()
                    ?: (action.parameters["alarmTime"] as? Number)?.toLong()
                    ?: (System.currentTimeMillis() + 60000L)
                val repeatDays = action.parameters["repeat_days"] as? String ?: ""

                // Ensure alarmTime is in milliseconds (if passed in seconds, convert)
                val alarmTimeMs = if (rawTime < 10000000000L) rawTime * 1000L else rawTime

                val alarm = alarmRepo.createAlarm(
                    alarmTime = alarmTimeMs,
                    label = label,
                    repeatDays = repeatDays
                )

                val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                val timeStr = sdf.format(java.util.Date(alarmTimeMs))

                ActionResult(
                    success = true,
                    intent = "CREATE_ALARM",
                    message = "Set reminder for '$label' at $timeStr.",
                    entityType = "ALARM",
                    entityId = alarm.id,
                    data = mapOf("alarm" to alarm)
                )
            }

            // 20. LOG_INTERACTION (People & Relationships - Segment 8)
            "LOG_INTERACTION" -> {
                val person = action.parameters["person"] as? String ?: "Contact"
                val topic = action.parameters["topic"] as? String ?: (action.parameters["note"] as? String ?: "Conversation")
                val type = action.parameters["interactionType"] as? String ?: (action.parameters["interaction_type"] as? String ?: "Call")
                val contactId = repository.createPerson(name = person, relationType = "Acquaintance")
                repository.createInteraction(personId = contactId, note = topic, interactionType = type)
                ActionResult(
                    success = true,
                    intent = "LOG_INTERACTION",
                    message = "Logged $type with $person regarding $topic.",
                    entityType = "RELATIONSHIP",
                    entityId = contactId
                )
            }

            // Segment 7: Time & Schedule (Daily Timetable)
            "CREATE_SCHEDULE_BLOCK" -> {
                val title = action.parameters["title"] as? String ?: "Deep Work"
                val start = action.parameters["start_time"] as? String ?: (action.parameters["startTime"] as? String ?: "09:00 AM")
                val end = action.parameters["end_time"] as? String ?: (action.parameters["endTime"] as? String ?: "10:00 AM")
                val category = action.parameters["category"] as? String ?: "Deep Work"
                val colorHex = action.parameters["color_hex"] as? String ?: (action.parameters["colorHex"] as? String ?: "#4FC3F7")
                val location = action.parameters["location"] as? String ?: ""
                val id = repository.createScheduleBlock(title, start, end, category, colorHex, location)
                ActionResult(
                    success = true,
                    intent = "CREATE_SCHEDULE_BLOCK",
                    message = "Scheduled '$title' from $start to $end.",
                    entityType = "SCHEDULE",
                    entityId = id
                )
            }

            "LIST_SCHEDULE" -> {
                val blocks = repository.listScheduleBlocks()
                val msg = if (blocks.isEmpty()) {
                    "Your schedule is open today with no timetable blocks set."
                } else {
                    val summary = blocks.take(3).joinToString("; ") { "${it.title} (${it.startTime} - ${it.endTime})" }
                    "You have ${blocks.size} block(s) scheduled today: $summary."
                }
                ActionResult(
                    success = true,
                    intent = "LIST_SCHEDULE",
                    message = msg,
                    entityType = "SCHEDULE",
                    data = mapOf("count" to blocks.size, "blocks" to blocks)
                )
            }

            // Segment 8: People & Relationships
            "CREATE_PERSON" -> {
                val name = action.parameters["name"] as? String ?: "Contact"
                val rel = action.parameters["relation_type"] as? String ?: (action.parameters["relationType"] as? String ?: "Friend")
                val phone = action.parameters["phone"] as? String ?: ""
                val id = repository.createPerson(name = name, relationType = rel, phone = phone)
                ActionResult(
                    success = true,
                    intent = "CREATE_PERSON",
                    message = "Added contact '$name' as $rel.",
                    entityType = "PERSON",
                    entityId = id
                )
            }

            "LIST_PEOPLE" -> {
                val people = repository.listPeople()
                val msg = if (people.isEmpty()) {
                    "You have no saved contacts in your relationship vault."
                } else {
                    val names = people.take(4).joinToString(", ") { it.name }
                    "You have ${people.size} saved contact(s), including $names."
                }
                ActionResult(
                    success = true,
                    intent = "LIST_PEOPLE",
                    message = msg,
                    entityType = "PERSON",
                    data = mapOf("count" to people.size, "people" to people)
                )
            }

            // Segment 9: Health & Wellbeing (Hydration, Sleep, Mood)
            "LOG_HYDRATION" -> {
                val amount = (action.parameters["amount_ml"] as? Number)?.toInt()
                    ?: (action.parameters["amountMl"] as? Number)?.toInt() ?: 250
                repository.logHydration(amount)
                val total = repository.getHydrationTodayMl()
                ActionResult(
                    success = true,
                    intent = "LOG_HYDRATION",
                    message = "Logged $amount ml of water. Total intake today is now $total ml.",
                    entityType = "WELLBEING",
                    data = mapOf("loggedMl" to amount, "totalTodayMl" to total)
                )
            }

            "GET_HYDRATION" -> {
                val total = repository.getHydrationTodayMl()
                ActionResult(
                    success = true,
                    intent = "GET_HYDRATION",
                    message = "Your total water intake recorded today is $total ml.",
                    entityType = "WELLBEING",
                    data = mapOf("totalTodayMl" to total)
                )
            }

            "LOG_SLEEP" -> {
                val bed = action.parameters["bedtime"] as? String ?: "11:00 PM"
                val wake = action.parameters["waketime"] as? String ?: "07:00 AM"
                val dur = action.parameters["duration_text"] as? String ?: (action.parameters["durationText"] as? String ?: "8h")
                val rating = (action.parameters["quality_rating"] as? Number)?.toInt()
                    ?: (action.parameters["qualityRating"] as? Number)?.toInt() ?: 4
                val label = action.parameters["quality_label"] as? String ?: (action.parameters["qualityLabel"] as? String ?: "Restful")
                val notes = action.parameters["notes"] as? String ?: ""
                val id = repository.logSleep(bed, wake, dur, rating, label, notes)
                ActionResult(
                    success = true,
                    intent = "LOG_SLEEP",
                    message = "Recorded $dur of $label sleep ($bed to $wake).",
                    entityType = "WELLBEING",
                    entityId = id
                )
            }

            "LIST_SLEEP" -> {
                val logs = repository.listSleep()
                val msg = if (logs.isEmpty()) {
                    "No sleep sessions recorded yet."
                } else {
                    val latest = logs.first()
                    "Latest sleep recorded: ${latest.durationText} (${latest.qualityLabel}, ${latest.bedtime} to ${latest.waketime})."
                }
                ActionResult(
                    success = true,
                    intent = "LIST_SLEEP",
                    message = msg,
                    entityType = "WELLBEING",
                    data = mapOf("logs" to logs)
                )
            }

            "LOG_MOOD" -> {
                val mood = action.parameters["mood"] as? String ?: "Reflective"
                val energy = action.parameters["energy_level"] as? String ?: (action.parameters["energyLevel"] as? String ?: "Medium")
                val note = action.parameters["note"] as? String ?: ""
                val id = repository.logMood(mood, energy, note)
                ActionResult(
                    success = true,
                    intent = "LOG_MOOD",
                    message = "Logged mood: $mood with $energy energy.",
                    entityType = "WELLBEING",
                    entityId = id
                )
            }

            "LIST_MOODS" -> {
                val moods = repository.listMoods()
                val msg = if (moods.isEmpty()) {
                    "No recent mood logs recorded."
                } else {
                    val latest = moods.first()
                    "Latest mood logged: ${latest.mood} (${latest.energyLevel} energy)."
                }
                ActionResult(
                    success = true,
                    intent = "LIST_MOODS",
                    message = msg,
                    entityType = "WELLBEING",
                    data = mapOf("moods" to moods)
                )
            }

            // Segment 10: Habits & Streaks
            "CREATE_HABIT" -> {
                val name = action.parameters["name"] as? String ?: "New Habit"
                val freq = action.parameters["frequency"] as? String ?: "Daily"
                val time = action.parameters["reminder_time"] as? String ?: (action.parameters["reminderTime"] as? String ?: "08:00 AM")
                val cat = action.parameters["category"] as? String ?: "Health"
                val id = repository.createHabit(name = name, frequency = freq, reminderTime = time, category = cat)
                ActionResult(
                    success = true,
                    intent = "CREATE_HABIT",
                    message = "Started habit '$name' ($freq at $time).",
                    entityType = "HABIT",
                    entityId = id
                )
            }

            "COMPLETE_HABIT" -> {
                val habitName = action.parameters["name"] as? String ?: ""
                val habits = repository.listHabits()
                val target = habits.firstOrNull { it.name.contains(habitName, ignoreCase = true) }
                    ?: habits.firstOrNull { !it.isDoneToday }
                if (target != null) {
                    repository.toggleHabitDoneToday(target.id)
                    ActionResult(
                        success = true,
                        intent = "COMPLETE_HABIT",
                        message = "Completed habit '${target.name}' for today! Current streak: ${target.streakCount + 1} days.",
                        entityType = "HABIT",
                        entityId = target.id
                    )
                } else {
                    ActionResult(
                        success = false,
                        intent = "COMPLETE_HABIT",
                        message = "Could not find an active habit matching '$habitName'.",
                        errorCode = "HABIT_NOT_FOUND"
                    )
                }
            }

            "LIST_HABITS" -> {
                val habits = repository.listHabits()
                val msg = if (habits.isEmpty()) {
                    "You have no habits tracked."
                } else {
                    val summary = habits.joinToString(", ") { "${it.name} (${it.streakCount}d streak)" }
                    "You have ${habits.size} active habit(s): $summary."
                }
                ActionResult(
                    success = true,
                    intent = "LIST_HABITS",
                    message = msg,
                    entityType = "HABIT",
                    data = mapOf("habits" to habits)
                )
            }

            // Segment 11: Reading & Knowledge Vault (Memory)
            "SAVE_KNOWLEDGE" -> {
                val content = action.parameters["content"] as? String ?: ""
                val title = action.parameters["title"] as? String ?: "Memory"
                val cat = action.parameters["category"] as? String ?: "Personal Profile"
                val id = repository.createDocument(title = title, content = content, category = cat)
                ActionResult(
                    success = true,
                    intent = "SAVE_KNOWLEDGE",
                    message = "Saved note to Knowledge Vault: '$title'.",
                    entityType = "KNOWLEDGE",
                    entityId = id
                )
            }

            "QUERY_KNOWLEDGE" -> {
                val q = action.parameters["query"] as? String ?: ""
                val docs = repository.listDocuments(q)
                val msg = if (docs.isEmpty()) {
                    "I searched your Knowledge Vault for '$q' but found no matching records."
                } else {
                    val top = docs.first()
                    val excerpt = if (top.content.length > 100) top.content.take(100) + "..." else top.content
                    "Found in vault: '${top.title}' - $excerpt"
                }
                ActionResult(
                    success = true,
                    intent = "QUERY_KNOWLEDGE",
                    message = msg,
                    entityType = "KNOWLEDGE",
                    data = mapOf("query" to q, "results" to docs)
                )
            }

            // Segment 12: Data Gathering & Raw Intake Hub
            "CAPTURE_INTAKE" -> {
                val note = action.parameters["note_text"] as? String ?: (action.parameters["noteText"] as? String ?: "")
                val tag = action.parameters["category_tag"] as? String ?: (action.parameters["categoryTag"] as? String ?: "General")
                val id = repository.createCaptureNote(noteText = note, categoryTag = tag)
                ActionResult(
                    success = true,
                    intent = "CAPTURE_INTAKE",
                    message = "Captured note into intake hub: '$note'.",
                    entityType = "INTAKE",
                    entityId = id
                )
            }

            "LIST_INTAKE" -> {
                val notes = repository.listCaptureNotes(includeProcessed = false)
                val msg = if (notes.isEmpty()) {
                    "Your raw intake hub is clear. All notes have been processed."
                } else {
                    "You have ${notes.size} unprocessed intake item(s). Top item: '${notes.first().noteText}'."
                }
                ActionResult(
                    success = true,
                    intent = "LIST_INTAKE",
                    message = msg,
                    entityType = "INTAKE",
                    data = mapOf("count" to notes.size, "notes" to notes)
                )
            }

            // Segment 13: Attention & Priority Feed
            "LIST_ATTENTION" -> {
                val events = repository.listAttentionEvents(includeAcknowledged = false)
                val msg = if (events.isEmpty()) {
                    "No pending alerts in your priority feed. All systems optimal."
                } else {
                    "You have ${events.size} priority alert(s): '${events.first().title}' (${events.first().urgency})."
                }
                ActionResult(
                    success = true,
                    intent = "LIST_ATTENTION",
                    message = msg,
                    entityType = "ATTENTION",
                    data = mapOf("events" to events)
                )
            }

            "ACKNOWLEDGE_ATTENTION" -> {
                val id = (action.parameters["alert_id"] as? Number)?.toLong()
                    ?: (action.parameters["id"] as? Number)?.toLong() ?: 1L
                val success = repository.acknowledgeAttention(id)
                ActionResult(
                    success = success,
                    intent = "ACKNOWLEDGE_ATTENTION",
                    message = if (success) "Acknowledged alert #$id." else "Alert #$id not found.",
                    entityType = "ATTENTION"
                )
            }

            // Segment 14: Daily Briefings & Evening Summaries
            "EVENING_SUMMARY" -> {
                val report = repository.getComprehensiveEndOfDaySummary()
                val totalTasks = report.completedTasksCount + report.pendingTasksCount
                val msg = buildString {
                    append("Evening summary for ${report.dateString}: ")
                    append("Completed ${report.completedTasksCount} of $totalTasks tasks today. ")
                    append("Expenditure: ${report.formattedSpent}. ")
                    append("Hydration: ${report.waterMl} ml. ")
                    append(report.motivationalClosing)
                }
                ActionResult(
                    success = true,
                    intent = "EVENING_SUMMARY",
                    message = msg,
                    entityType = "BRIEFING",
                    data = mapOf("report" to report)
                )
            }

            // Segment 15: Safety & System Rules
            "CREATE_RULE" -> {
                val ruleText = action.parameters["rule_text"] as? String ?: (action.parameters["ruleText"] as? String ?: "")
                val level = action.parameters["enforcement_level"] as? String ?: (action.parameters["enforcementLevel"] as? String ?: "Strict")
                val id = repository.createRule(ruleText, level)
                ActionResult(
                    success = true,
                    intent = "CREATE_RULE",
                    message = "Created safety rule: '$ruleText' ($level).",
                    entityType = "RULE",
                    entityId = id
                )
            }

            "LIST_RULES" -> {
                val rules = repository.listRules()
                val msg = if (rules.isEmpty()) {
                    "No custom system rules defined."
                } else {
                    val summary = rules.take(3).joinToString("; ") { it.ruleText }
                    "You have ${rules.size} active rule(s): $summary."
                }
                ActionResult(
                    success = true,
                    intent = "LIST_RULES",
                    message = msg,
                    entityType = "RULE",
                    data = mapOf("rules" to rules)
                )
            }

            // Segment 16: Audit Trail & Action Logs
            "LIST_AUDIT" -> {
                val logs = repository.listAuditLogs(5)
                val msg = if (logs.isEmpty()) {
                    "Audit log is clean."
                } else {
                    val summary = logs.take(3).joinToString("; ") { "${it.action}: ${it.details}" }
                    "Recent system actions: $summary."
                }
                ActionResult(
                    success = true,
                    intent = "LIST_AUDIT",
                    message = msg,
                    entityType = "AUDIT",
                    data = mapOf("logs" to logs)
                )
            }

            // 21. DECOMPOSE_PLAN_ASYNC (Needle 3 Async Generative Synthesis - Dual-Engine)
            "DECOMPOSE_PLAN_ASYNC" -> {
                val topic = action.parameters["topic"] as? String ?: "Personal Plan"
                val originalPrompt = action.parameters["original_prompt"] as? String ?: topic
                val ctx = context
                if (ctx != null) {
                    val planner = NeedlePlannerEngine(ctx, repository)
                    planner.scheduleBackgroundPlan(topic, originalPrompt)
                }
                ActionResult(
                    success = true,
                    intent = "DECOMPOSE_PLAN_ASYNC",
                    message = "I've queued that. I'm building your roadmap for '$topic' in the background.",
                    entityType = "PLAN",
                    data = mapOf("topic" to topic)
                )
            }

            // 22. GENERAL / CONVERSATIONAL_REASONING (On-device conversational intelligence across 16 segments)
            "GENERAL", "CONVERSATIONAL_REASONING" -> {
                val query = action.parameters["query"] as? String ?: "Status check"
                val result = NeedleConversationalEngine.synthesizeResponse(query, repository)
                ActionResult(
                    success = true,
                    intent = intent,
                    message = result.answer,
                    entityType = "CONVERSATION",
                    data = mapOf(
                        "spokenAnswer" to result.spokenAnswer,
                        "accessedSegments" to result.accessedSegments,
                        "latencyMs" to result.latencyMs,
                        "personaTone" to result.personaTone
                    )
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

        if (parsedIntent.intent == "CHAINED_MULTI_COMMAND") {
            @Suppress("UNCHECKED_CAST")
            val subIntents = parsedIntent.parameters["subIntents"] as? List<ParsedIntent> ?: emptyList()
            if (subIntents.isNotEmpty()) {
                val results = mutableListOf<ActionResult>()
                for (sub in subIntents) {
                    results.add(execute(sub))
                }
                val messages = results.map { it.message }
                val allSuccess = results.all { it.success }
                return ActionResult(
                    success = allSuccess,
                    intent = "CHAINED_MULTI_COMMAND",
                    message = messages.joinToString(" and "),
                    data = mapOf("results" to results)
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

    private fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            String.format(java.util.Locale.US, "%,d", amount.toLong())
        } else {
            String.format(java.util.Locale.US, "%,.1f", amount)
        }
    }
}
