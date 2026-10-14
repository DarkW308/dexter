package com.aistudio.dexter.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class NeedleHttpClient(
    private val context: Context,
    private val baseUrl: String = "http://127.0.0.1:8081"
) {

    companion object {
        private const val TAG = "NeedleHttpClient"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Interprets natural language request through Needle 3 HTTP proposal with Tier 1 fallback.
     */
    suspend fun interpret(userText: String, requestId: String = "req_voice"): ValidatedEnvelope = withContext(Dispatchers.IO) {
        val trimmed = DexterIntentRouter.stripWakeWord(userText)

        // Tier 1 Fast-Path Check
        val fastPath = DexterIntentRouter.parse(trimmed)
        if (!fastPath.isNegationOrClarification && fastPath.intent != "UNKNOWN") {
            // High confidence fast path match (~1-5 ms)
            val actionContract = ActionContract(
                intent = fastPath.intent,
                parameters = fastPath.parameters,
                actorType = "USER",
                confirmed = !fastPath.requiresConfirmation,
                requestId = requestId
            )
            return@withContext ValidatedEnvelope(
                requestId = requestId,
                confidence = 0.99,
                needsClarification = false,
                action = actionContract
            )
        }

        // Tier 2: Query Localhost Needle 3 Daemon
        try {
            val proposal = queryNeedle(trimmed)
            if (proposal != null) {
                return@withContext DexterContractValidator.validateEnvelope(proposal)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tier 2 Needle 3 query failed, falling back to fast-path: ${e.message}")
        }

        // Fallback: If Tier 2 is cold or unreachable, return fast path envelope or clarification
        if (fastPath.isNegationOrClarification && fastPath.intent == "NEGATION_SAFETY") {
            ValidatedEnvelope(
                requestId = requestId,
                confidence = 0.99,
                needsClarification = false,
                action = ActionContract("NEGATION_SAFETY", emptyMap(), "USER", true, requestId)
            )
        } else {
            ValidatedEnvelope(
                requestId = requestId,
                confidence = 0.0,
                needsClarification = true,
                action = null,
                clarificationPrompt = "I couldn't quite understand that request. Could you please rephrase?"
            )
        }
    }

    private fun queryNeedle(text: String): Map<String, Any?>? {
        return try {
            // 1. Reset conversation state
            val resetRequest = Request.Builder()
                .url("$baseUrl/reset")
                .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(resetRequest).execute().close()

            // 2. Complete inference
            val payload = buildJsonObject {
                put("input", text)
            }.toString()

            val completeRequest = Request.Builder()
                .url("$baseUrl/complete")
                .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(completeRequest).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyStr = response.body?.string() ?: return null
                parseEnvelopeJson(bodyStr)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseEnvelopeJson(jsonStr: String): Map<String, Any?>? {
        return try {
            val element = jsonParser.parseToJsonElement(jsonStr).jsonObject
            val map = mutableMapOf<String, Any?>()

            element["request_id"]?.jsonPrimitive?.contentOrNull?.let { map["request_id"] = it }
            element["confidence"]?.jsonPrimitive?.doubleOrNull?.let { map["confidence"] = it }
            element["needs_clarification"]?.jsonPrimitive?.booleanOrNull?.let { map["needs_clarification"] = it }
            element["clarification_prompt"]?.jsonPrimitive?.contentOrNull?.let { map["clarification_prompt"] = it }

            element["action"]?.jsonObject?.let { actionObj ->
                val actionMap = mutableMapOf<String, Any?>()
                actionObj["intent"]?.jsonPrimitive?.contentOrNull?.let { actionMap["intent"] = it }
                actionObj["actor_type"]?.jsonPrimitive?.contentOrNull?.let { actionMap["actor_type"] = it }
                actionObj["confirmed"]?.jsonPrimitive?.booleanOrNull?.let { actionMap["confirmed"] = it }

                actionObj["parameters"]?.jsonObject?.let { paramsObj ->
                    val paramsMap = mutableMapOf<String, Any?>()
                    for ((k, v) in paramsObj) {
                        paramsMap[k] = when {
                            v is JsonPrimitive && v.isString -> v.content
                            v is JsonPrimitive && v.longOrNull != null -> v.long
                            v is JsonPrimitive && v.doubleOrNull != null -> v.double
                            v is JsonPrimitive && v.booleanOrNull != null -> v.boolean
                            else -> v.toString()
                        }
                    }
                    actionMap["parameters"] = paramsMap
                }
                map["action"] = actionMap
            }

            map
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Needle proposal JSON", e)
            null
        }
    }
}
