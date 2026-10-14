package com.aistudio.dexter.engine

import com.aistudio.dexter.data.model.Priority

open class ContractException(message: String) : Exception(message)
class ContractValidationError(message: String) : ContractException(message)
class ConfidenceTooLowException(message: String) : ContractException(message)
class ConfirmationRequiredException(val prompt: String) : ContractException(prompt)

data class ActionContract(
    val intent: String,
    val parameters: Map<String, Any?> = emptyMap(),
    val actorType: String = "USER",
    val confirmed: Boolean = false,
    val requestId: String? = null
)

data class ValidatedEnvelope(
    val requestId: String,
    val confidence: Double,
    val needsClarification: Boolean,
    val action: ActionContract?,
    val clarificationPrompt: String? = null
)

object DexterContractValidator {

    const val DEFAULT_CONFIDENCE_THRESHOLD = 0.30

    // All 18 tools mapped from dexter_tools.json (both snake_case and UPPER_SNAKE_CASE)
    val TOOL_NAME_TO_INTENT: Map<String, String> = mapOf(
        "create_task" to "CREATE_TASK",
        "list_pending_tasks" to "LIST_PENDING_TASKS",
        "complete_task" to "COMPLETE_TASK",
        "user_report_task_completion" to "USER_REPORT_TASK_COMPLETION",
        "cancel_task" to "CANCEL_TASK",
        "complete_all_tasks" to "COMPLETE_ALL_TASKS",
        "list_completed_tasks" to "LIST_COMPLETED_TASKS",
        "log_expense" to "LOG_EXPENSE",
        "summarize_expenses" to "SUMMARIZE_EXPENSES",
        "log_journal" to "LOG_JOURNAL",
        "list_journal" to "LIST_JOURNAL",
        "list_alarms" to "LIST_ALARMS",
        "cancel_alarms" to "CANCEL_ALARMS",
        "daily_briefing" to "DAILY_BRIEFING",
        "create_goal" to "CREATE_GOAL",
        "list_goals" to "LIST_GOALS",
        "create_project" to "CREATE_PROJECT",
        "list_projects" to "LIST_PROJECTS"
    )

    val ALLOWED_INTENTS: Set<String> = TOOL_NAME_TO_INTENT.values.toSet()

    val DESTRUCTIVE_INTENTS: Set<String> = setOf(
        "COMPLETE_TASK",
        "CANCEL_TASK",
        "COMPLETE_ALL_TASKS",
        "CANCEL_ALARMS"
    )

    val ALLOWED_ACTORS: Set<String> = setOf("USER", "DEXTER", "SYSTEM", "AUTOMATION")

    fun normalizeIntent(name: String): String {
        val trimmed = name.trim()
        val lower = trimmed.lowercase()
        return TOOL_NAME_TO_INTENT[lower] ?: trimmed.uppercase()
    }

    fun isDestructive(intent: String): Boolean {
        return DESTRUCTIVE_INTENTS.contains(normalizeIntent(intent))
    }

    /**
     * Port of validate_envelope from qwen_contract.py.
     */
    fun validateEnvelope(
        envelope: Map<String, Any?>,
        confidenceThreshold: Double = DEFAULT_CONFIDENCE_THRESHOLD
    ): ValidatedEnvelope {
        val requestId = (envelope["request_id"] as? String)?.trim() ?: "req_local"
        val confidence = (envelope["confidence"] as? Number)?.toDouble() ?: 1.0
        val needsClarification = (envelope["needs_clarification"] as? Boolean) ?: false

        // Rule: confidence < 0.30 -> reject/clarify
        if (confidence < confidenceThreshold) {
            return ValidatedEnvelope(
                requestId = requestId,
                confidence = confidence,
                needsClarification = true,
                action = null,
                clarificationPrompt = "I'm not quite confident in that request. Could you please clarify?"
            )
        }

        if (needsClarification) {
            val prompt = (envelope["clarification_prompt"] as? String)?.trim()
                ?: "I need some clarification to execute that."
            return ValidatedEnvelope(
                requestId = requestId,
                confidence = confidence,
                needsClarification = true,
                action = null,
                clarificationPrompt = prompt
            )
        }

        @Suppress("UNCHECKED_CAST")
        val rawAction = envelope["action"] as? Map<String, Any?>
            ?: throw ContractValidationError("Missing 'action' object in proposal envelope.")

        val action = parseAndValidateAction(rawAction, requestId)

        return ValidatedEnvelope(
            requestId = requestId,
            confidence = confidence,
            needsClarification = false,
            action = action
        )
    }

    /**
     * Parses, sanitizes, and checks anti-hallucination constraints on action payload.
     */
    fun parseAndValidateAction(
        rawAction: Map<String, Any?>,
        requestId: String? = null
    ): ActionContract {
        val rawName = (rawAction["intent"] as? String)
            ?: (rawAction["name"] as? String)
            ?: throw ContractValidationError("Action 'intent' or tool 'name' is required.")

        val intent = normalizeIntent(rawName)
        if (!ALLOWED_INTENTS.contains(intent)) {
            throw ContractValidationError("Unsupported action or tool intent: $rawName")
        }

        val actorType = (rawAction["actor_type"] as? String)?.trim()?.uppercase() ?: "USER"
        if (!ALLOWED_ACTORS.contains(actorType)) {
            throw ContractValidationError("Unsupported actor type: $actorType")
        }

        val confirmed = (rawAction["confirmed"] as? Boolean) ?: false

        @Suppress("UNCHECKED_CAST")
        val rawParams = (rawAction["parameters"] as? Map<String, Any?>) ?: emptyMap()
        val sanitizedParams = sanitizeParameters(intent, rawParams)

        return ActionContract(
            intent = intent,
            parameters = sanitizedParams,
            actorType = actorType,
            confirmed = confirmed,
            requestId = requestId ?: (rawAction["request_id"] as? String)
        )
    }

    private fun sanitizeParameters(
        intent: String,
        params: Map<String, Any?>
    ): Map<String, Any?> {
        val sanitized = mutableMapOf<String, Any?>()

        when (intent) {
            "CREATE_TASK" -> {
                // ID Safety: The AI model must NEVER invent or supply a task ID on creation
                if (params.containsKey("id") || params.containsKey("task_id")) {
                    throw ContractValidationError("Model hallucination detected: AI cannot invent database IDs during task creation.")
                }

                val title = requireText(params, "title", maxLen = 500)
                sanitized["title"] = title

                val priorityStr = (params["priority"] as? String)?.trim()?.uppercase() ?: "NORMAL"
                val priority = try {
                    Priority.valueOf(priorityStr)
                } catch (e: Exception) {
                    throw ContractValidationError("Invalid task priority: $priorityStr")
                }
                sanitized["priority"] = priority

                params["project_id"]?.let {
                    sanitized["project_id"] = requirePositiveLong(it, "project_id")
                }
                for (key in listOf("due_at", "scheduled_start", "scheduled_end", "estimated_duration_seconds")) {
                    params[key]?.let {
                        sanitized[key] = requirePositiveLong(it, key)
                    }
                }
            }

            "COMPLETE_TASK", "USER_REPORT_TASK_COMPLETION", "CANCEL_TASK" -> {
                val rawId = params["task_id"] ?: params["id"]
                    ?: throw ContractValidationError("Parameter 'task_id' is required for $intent.")
                val taskId = requirePositiveLong(rawId, "task_id")
                sanitized["task_id"] = taskId
            }

            "LOG_EXPENSE" -> {
                val amountRaw = params["amount"]
                    ?: throw ContractValidationError("Parameter 'amount' is required for LOG_EXPENSE.")
                val amountDouble = when (amountRaw) {
                    is Number -> amountRaw.toDouble()
                    is String -> amountRaw.toDoubleOrNull()
                        ?: throw ContractValidationError("Invalid expense amount: $amountRaw")
                    else -> throw ContractValidationError("Parameter 'amount' must be numeric.")
                }
                if (amountDouble <= 0) {
                    throw ContractValidationError("Expense amount must be positive and greater than zero.")
                }
                sanitized["amount"] = amountDouble
                sanitized["description"] = requireText(params, "description", maxLen = 500)
            }

            "LOG_JOURNAL" -> {
                sanitized["content"] = requireText(params, "content", maxLen = 5000)
            }

            "CREATE_GOAL" -> {
                if (params.containsKey("id") || params.containsKey("goal_id")) {
                    throw ContractValidationError("Model hallucination detected: AI cannot invent goal IDs.")
                }
                sanitized["title"] = requireText(params, "title", maxLen = 500)
            }

            "CREATE_PROJECT" -> {
                if (params.containsKey("id") || params.containsKey("project_id")) {
                    throw ContractValidationError("Model hallucination detected: AI cannot invent project IDs.")
                }
                sanitized["name"] = requireText(params, "name", maxLen = 500)
            }

            // Zero-parameter safe queries & bulk operations
            "LIST_PENDING_TASKS",
            "LIST_COMPLETED_TASKS",
            "COMPLETE_ALL_TASKS",
            "SUMMARIZE_EXPENSES",
            "LIST_JOURNAL",
            "LIST_ALARMS",
            "CANCEL_ALARMS",
            "DAILY_BRIEFING",
            "LIST_GOALS",
            "LIST_PROJECTS" -> {
                // No required parameters
            }
        }

        return sanitized
    }

    private fun requireText(params: Map<String, Any?>, name: String, maxLen: Int): String {
        val value = params[name]
        if (value !is String || value.trim().isEmpty()) {
            throw ContractValidationError("Parameter '$name' is required.")
        }
        val trimmed = value.trim()
        if (trimmed.length > maxLen) {
            throw ContractValidationError("Parameter '$name' exceeds maximum length of $maxLen characters.")
        }
        return trimmed
    }

    private fun requirePositiveLong(value: Any?, name: String): Long {
        if (value == null) {
            throw ContractValidationError("Parameter '$name' is required.")
        }
        val num = when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
                ?: throw ContractValidationError("Parameter '$name' must be an integer.")
            else -> throw ContractValidationError("Parameter '$name' must be an integer.")
        }
        if (num <= 0) {
            throw ContractValidationError("Parameter '$name' must be a valid positive integer > 0.")
        }
        return num
    }
}
