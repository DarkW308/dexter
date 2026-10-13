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

    val ALLOWED_INTENTS = setOf(
        "CREATE_TASK",
        "LIST_PENDING_TASKS",
        "COMPLETE_TASK",
        "USER_REPORT_TASK_COMPLETION",
        "CANCEL_TASK",
        "COMPLETE_ALL_TASKS",
        "LIST_COMPLETED_TASKS",
        "LOG_EXPENSE",
        "SUMMARIZE_EXPENSES",
        "LOG_JOURNAL",
        "LIST_JOURNAL",
        "LIST_ALARMS",
        "CANCEL_ALARMS",
        "DAILY_BRIEFING",
        "CREATE_GOAL",
        "LIST_GOALS",
        "CREATE_PROJECT",
        "LIST_PROJECTS"
    )

    val DESTRUCTIVE_INTENTS = setOf(
        "COMPLETE_TASK",
        "CANCEL_TASK",
        "COMPLETE_ALL_TASKS",
        "CANCEL_ALARMS"
    )

    val ALLOWED_ACTORS = setOf("USER", "DEXTER", "SYSTEM", "AUTOMATION")

    fun isDestructive(intent: String): Boolean = DESTRUCTIVE_INTENTS.contains(intent.uppercase().trim())

    /**
     * Validates raw contract envelopes conforming to qwen_contract.py.
     */
    fun validateEnvelope(
        envelope: Map<String, Any?>,
        confidenceThreshold: Double = DEFAULT_CONFIDENCE_THRESHOLD
    ): ValidatedEnvelope {
        val requestId = (envelope["request_id"] as? String) ?: "req_local"
        val confidence = (envelope["confidence"] as? Number)?.toDouble() ?: 1.0
        val needsClarification = (envelope["needs_clarification"] as? Boolean) ?: false

        if (confidence < confidenceThreshold) {
            return ValidatedEnvelope(
                requestId = requestId,
                confidence = confidence,
                needsClarification = true,
                action = null,
                clarificationPrompt = "Confidence score ($confidence) below threshold ($confidenceThreshold). Clarification requested."
            )
        }

        if (needsClarification) {
            val prompt = (envelope["clarification_prompt"] as? String)
                ?: "I need some clarification to process your request."
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
            ?: throw ContractValidationError("Missing action payload in proposal envelope.")

        val action = parseAndValidateAction(rawAction, requestId)

        return ValidatedEnvelope(
            requestId = requestId,
            confidence = confidence,
            needsClarification = false,
            action = action
        )
    }

    /**
     * Parses and strictly sanitizes action parameters against model hallucinations.
     */
    fun parseAndValidateAction(
        rawAction: Map<String, Any?>,
        requestId: String? = null
    ): ActionContract {
        val rawIntent = (rawAction["intent"] as? String)?.trim()?.uppercase()
            ?: throw ContractValidationError("Action 'intent' is required.")

        if (!ALLOWED_INTENTS.contains(rawIntent)) {
            throw ContractValidationError("Unsupported intent: $rawIntent")
        }

        val actorType = (rawAction["actor_type"] as? String)?.trim()?.uppercase() ?: "USER"
        if (!ALLOWED_ACTORS.contains(actorType)) {
            throw ContractValidationError("Unsupported actor type: $actorType")
        }

        val confirmed = (rawAction["confirmed"] as? Boolean) ?: false

        @Suppress("UNCHECKED_CAST")
        val rawParams = (rawAction["parameters"] as? Map<String, Any?>) ?: emptyMap()
        val sanitizedParams = sanitizeParameters(rawIntent, rawParams)

        return ActionContract(
            intent = rawIntent,
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
        val result = mutableMapOf<String, Any?>()

        when (intent) {
            "CREATE_TASK" -> {
                val title = requireText(params, "title", maxLen = 500)
                result["title"] = title

                val priorityStr = (params["priority"] as? String)?.trim()?.uppercase() ?: "NORMAL"
                val priority = try {
                    Priority.valueOf(priorityStr)
                } catch (e: Exception) {
                    throw ContractValidationError("Invalid task priority: $priorityStr")
                }
                result["priority"] = priority

                params["project_id"]?.let {
                    result["project_id"] = requirePositiveInt(it, "project_id")
                }
                for (key in listOf("due_at", "scheduled_start", "scheduled_end", "estimated_duration_seconds")) {
                    params[key]?.let {
                        result[key] = requirePositiveInt(it, key)
                    }
                }
            }

            "COMPLETE_TASK", "USER_REPORT_TASK_COMPLETION", "CANCEL_TASK" -> {
                val taskId = requirePositiveInt(params["task_id"], "task_id")
                result["task_id"] = taskId
            }

            "LOG_EXPENSE" -> {
                val amountRaw = params["amount"]
                    ?: throw ContractValidationError("Parameter 'amount' is required.")
                val amountDouble = when (amountRaw) {
                    is Number -> amountRaw.toDouble()
                    is String -> amountRaw.toDoubleOrNull()
                        ?: throw ContractValidationError("Invalid amount number: $amountRaw")
                    else -> throw ContractValidationError("Parameter 'amount' must be a numeric value.")
                }
                if (amountDouble <= 0) {
                    throw ContractValidationError("Expense amount must be greater than zero.")
                }
                result["amount"] = amountDouble
                result["description"] = requireText(params, "description", maxLen = 500)
            }

            "LOG_JOURNAL" -> {
                result["content"] = requireText(params, "content", maxLen = 5000)
            }

            "CREATE_GOAL" -> {
                result["title"] = requireText(params, "title", maxLen = 500)
            }

            "CREATE_PROJECT" -> {
                result["name"] = requireText(params, "name", maxLen = 500)
            }

            // Zero-parameter intents
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

        return result
    }

    private fun requireText(params: Map<String, Any?>, name: String, maxLen: Int = 500): String {
        val value = params[name]
        if (value !is String || value.trim().isEmpty()) {
            throw ContractValidationError("Parameter '$name' is required.")
        }
        val trimmed = value.trim()
        if (trimmed.length > maxLen) {
            throw ContractValidationError("Parameter '$name' exceeds max length of $maxLen.")
        }
        return trimmed
    }

    private fun requirePositiveInt(value: Any?, name: String): Long {
        if (value == null) {
            throw ContractValidationError("Parameter '$name' is required.")
        }
        val longVal = when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
                ?: throw ContractValidationError("Parameter '$name' must be an integer.")
            else -> throw ContractValidationError("Parameter '$name' must be an integer.")
        }
        if (longVal <= 0) {
            throw ContractValidationError("Parameter '$name' must be a positive integer > 0.")
        }
        return longVal
    }
}
