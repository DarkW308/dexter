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
                val topic = action.parameters["topic"] as? String ?: "Conversation"
                val type = action.parameters["interactionType"] as? String ?: "Call"
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

            // 22. CONVERSATIONAL_REASONING (Needle-3 on-device intelligence across 16 segments)
            "CONVERSATIONAL_REASONING" -> {
                val query = action.parameters["query"] as? String ?: "Status check"
                val result = NeedleConversationalEngine.synthesizeResponse(query, repository)
                ActionResult(
                    success = true,
                    intent = "CONVERSATIONAL_REASONING",
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
