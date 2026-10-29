package com.aistudio.dexter.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.aistudio.dexter.DexterApp
import com.aistudio.dexter.data.repository.DexterRepository
import com.aistudio.dexter.engine.*
import com.aistudio.dexter.ui.screens.VoiceLabScreen
import com.aistudio.dexter.ui.theme.DexterTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceLabActivity : ComponentActivity() {

    private lateinit var repository: DexterRepository
    private lateinit var layaEngine: LayaEngine
    private lateinit var actionExecutor: DexterActionExecutor

    private var currentTraceState = mutableStateOf<VoicePipelineTrace?>(null)
    private val historyState = mutableStateListOf<VoicePipelineTrace>()

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                evaluateQuery(spoken)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = (application as DexterApp).repository
        layaEngine = LayaEngine.getInstance(this)
        actionExecutor = DexterActionExecutor(repository, this)

        // Seed with a default demonstration query
        evaluateQuery("spen 50 bucks on fuel")

        setContent {
            DexterTheme {
                VoiceLabScreen(
                    currentTrace = currentTraceState.value,
                    history = historyState,
                    onBack = { finish() },
                    onExecuteQuery = { query -> evaluateQuery(query) },
                    onVoiceClick = { launchSpeechRecognizer() }
                )
            }
        }
    }

    private fun evaluateQuery(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (query.startsWith("[PROACTIVE]")) {
                val motiveEngine = ProactiveMotiveEngine.getInstance(this@VoiceLabActivity)
                val motive = when {
                    query.contains("Morning") -> ProactiveMotive.EXECUTIVE_ADVISORY
                    query.contains("Velocity") -> ProactiveMotive.MOMENTUM_MOTIVATOR
                    query.contains("Budget") -> ProactiveMotive.FINANCIAL_PACING
                    query.contains("Wellbeing") -> ProactiveMotive.WELLBEING_CHECKIN
                    else -> ProactiveMotive.CURATED_WIT
                }
                val vocalization = motiveEngine.simulateMotive(motive)
                val trace = VoicePipelineTrace(
                    query = query,
                    tokenTrace = PipelineToken(
                        raw = query,
                        normalized = vocalization.headline,
                        languageTag = "Autonomous Proactive Speech (Unsolicited)",
                        accentNote = "Triggered by ${vocalization.triggeredBy}"
                    ),
                    intent = "PROACTIVE_VOCALIZATION",
                    confidence = 0.99f,
                    targetSegment = vocalization.targetSegments.firstOrNull() ?: 1,
                    segmentName = "Autonomous Speech: ${vocalization.motive.label}",
                    slots = listOf(
                        ExtractedSlot("motive", vocalization.motive.name, "MOTIVE"),
                        ExtractedSlot("headline", vocalization.headline, "HEADLINE"),
                        ExtractedSlot("urgency", vocalization.urgency.name, "STATE"),
                        ExtractedSlot("context", vocalization.contextSummary, "CONTEXT")
                    ),
                    dbTraces = listOf(
                        DatabaseRowTrace(
                            tableName = "proactive_log",
                            segmentNumber = vocalization.targetSegments.firstOrNull() ?: 1,
                            segmentTitle = vocalization.motive.label,
                            operation = "AUTONOMOUS_SPEAK",
                            rowData = mapOf(
                                "headline" to vocalization.headline,
                                "spoken_text" to vocalization.spokenText,
                                "urgency" to vocalization.urgency.name
                            )
                        )
                    ),
                    responseText = vocalization.spokenText,
                    executionTimeMs = 5L,
                    isSuccess = true
                )

                withContext(Dispatchers.Main) {
                    currentTraceState.value = trace
                    if (historyState.none { it.query == trace.query }) {
                        historyState.add(0, trace)
                    }
                }
                return@launch
            }

            // Run full acoustic and Laya slot extraction pipeline
            val trace = layaEngine.analyzePipeline(query)

            // Also execute in real repository to verify SQLite metrics and WAL consistency
            val parsed = DexterIntentRouter.parse(query)
            try {
                actionExecutor.execute(parsed)
            } catch (e: Exception) {
                android.util.Log.w("VoiceLabActivity", "Notice during lab execution: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                currentTraceState.value = trace
                if (historyState.none { it.query == trace.query }) {
                    historyState.add(0, trace)
                }
            }
        }
    }

    private fun launchSpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak command (Any accent or language supported)...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition unavailable on this device", Toast.LENGTH_SHORT).show()
        }
    }
}
