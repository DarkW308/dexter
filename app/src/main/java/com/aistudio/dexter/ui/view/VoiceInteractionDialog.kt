package com.aistudio.dexter.ui.view

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.aistudio.dexter.R
import com.aistudio.dexter.databinding.DialogVoiceInteractionBinding
import com.aistudio.dexter.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * Fullscreen modal voice interaction overlay for Dexter.
 * Shows enlarged animated VoiceOrbView, live partial STT transcription,
 * inline confirmation prompt for destructive actions, and text fallback input.
 */
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.core.content.ContextCompat
import java.util.Locale

class VoiceInteractionDialog(
    context: Context,
    private val speechManager: DexterSpeechManager,
    private val actionExecutor: DexterActionExecutor,
    private val onActionCompleted: ((String) -> Unit)? = null,
    private val onLaunchSystemVoice: (() -> Unit)? = null
) : Dialog(context, R.style.Theme_Dexter_Dialog_FullScreen) {

    private lateinit var binding: DialogVoiceInteractionBinding
    private val dialogScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var pendingDestructiveAction: ActionContract? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogVoiceInteractionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        setupListeners()
        observeSpeechState()

        // Auto-start active listening when dialog is presented
        speechManager.startListening()
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnCancelModal.setOnClickListener {
            dismiss()
        }

        binding.dialogVoiceOrb.onTap = {
            if (speechManager.orbState.value == OrbVisualState.LISTENING) {
                speechManager.stopListening()
            } else {
                speechManager.startListening()
            }
        }

        binding.dialogVoiceOrb.onLongPress = {
            speechManager.startListening()
        }

        // Confirmation Actions
        binding.btnAcceptConfirm.setOnClickListener {
            pendingDestructiveAction?.let { action ->
                val confirmedAction = action.copy(confirmed = true)
                executeAction(confirmedAction)
            }
            binding.confirmationCard.visibility = View.GONE
            pendingDestructiveAction = null
        }

        binding.btnCancelConfirm.setOnClickListener {
            binding.confirmationCard.visibility = View.GONE
            pendingDestructiveAction = null
            speechManager.speak("Action cancelled.")
        }

        // Silent manual text fallback
        binding.btnSendText.setOnClickListener {
            val text = binding.inputFallbackText.text.toString().trim()
            if (text.isNotEmpty()) {
                processUserText(text)
                binding.inputFallbackText.setText("")
            }
        }

        binding.inputFallbackText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val text = binding.inputFallbackText.text.toString().trim()
                if (text.isNotEmpty()) {
                    processUserText(text)
                    binding.inputFallbackText.setText("")
                }
                true
            } else {
                false
            }
        }

        // Quick Command Action Chips
        binding.chipTraveling.setOnClickListener {
            processUserText("spent 45 in traveling")
        }
        binding.chipDailyBriefing.setOnClickListener {
            processUserText("Daily briefing")
        }
        binding.chipExpense.setOnClickListener {
            processUserText("Spent 350 on lunch")
        }
        binding.chipTask.setOnClickListener {
            processUserText("Create task Review architecture")
        }
        binding.chipReminder.setOnClickListener {
            processUserText("Remind me at 5 PM to call team")
        }
        binding.chipDiary.setOnClickListener {
            processUserText("Note: Deep work session on Dexter")
        }
        binding.chipSummary.setOnClickListener {
            processUserText("Summarize expenses")
        }
        binding.chipShowTasks.setOnClickListener {
            processUserText("Pending tasks")
        }

        // Speech Diagnostics Controls
        binding.btnDiagnostics.setOnClickListener {
            if (binding.diagnosticsCard.visibility == View.VISIBLE) {
                binding.diagnosticsCard.visibility = View.GONE
            } else {
                binding.diagnosticsCard.visibility = View.VISIBLE
                binding.diagnosticsReportText.text = speechManager.latestDiagnostics.value.formatDiagnosticReport()
            }
        }

        binding.btnCloseDiagnostics.setOnClickListener {
            binding.diagnosticsCard.visibility = View.GONE
        }

        binding.btnProbeHardware.setOnClickListener {
            binding.btnProbeHardware.isEnabled = false
            binding.diagnosticsReportText.text = "Sampling direct microphone hardware for 1.2s...\nPlease speak now to test physical mic."
            dialogScope.launch {
                val probeResult = SpeechHardwareProbe.runProbe(context)
                binding.btnProbeHardware.isEnabled = true
                val currentDiag = speechManager.latestDiagnostics.value
                currentDiag.hardwareProbeResult = probeResult.message
                binding.diagnosticsReportText.text = currentDiag.formatDiagnosticReport()
            }
        }

        binding.btnSystemVoice.setOnClickListener {
            speechManager.stopListening()
            if (onLaunchSystemVoice != null) {
                onLaunchSystemVoice.invoke()
            } else {
                try {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a command to Dexter")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    binding.diagnosticsReportText.text = "System speech intent failed: ${e.message}"
                }
            }
        }
    }

    private fun observeSpeechState() {
        // Observe Orb State
        dialogScope.launch {
            speechManager.orbState.collectLatest { state ->
                binding.dialogVoiceOrb.setState(state)
                when (state) {
                    OrbVisualState.IDLE -> {
                        if (!speechManager.isRecognitionAvailable()) {
                            binding.stateLabel.text = "Voice Ready"
                        } else {
                            binding.stateLabel.text = "Tap orb to speak"
                        }
                        binding.audioIndicator.visibility = View.GONE
                    }
                    OrbVisualState.LISTENING -> {
                        binding.stateLabel.text = "Listening... Speak now"
                        binding.transcriptText.text = "Listening for voice command... (e.g. 'spent 45 in traveling')"
                        binding.audioIndicator.visibility = View.VISIBLE
                        binding.audioIndicator.text = "● LISTENING · 0.0 dB"
                    }
                    OrbVisualState.PROCESSING -> {
                        binding.stateLabel.text = "Processing..."
                        binding.audioIndicator.visibility = View.VISIBLE
                        binding.audioIndicator.text = "⚙ BUSY"
                    }
                    OrbVisualState.SPEAKING -> {
                        binding.stateLabel.text = "Speaking..."
                        binding.audioIndicator.visibility = View.GONE
                    }
                    OrbVisualState.ERROR -> {
                        binding.stateLabel.text = "Something went wrong"
                        binding.audioIndicator.visibility = View.GONE
                    }
                }
            }
        }

        // Observe Live Audio Detection & Volume
        dialogScope.launch {
            speechManager.rmsVolume.collectLatest { rms ->
                if (speechManager.orbState.value == OrbVisualState.LISTENING) {
                    val formattedRms = String.format(Locale.US, "%.1f", rms)
                    if (rms > 2.0f) {
                        binding.audioIndicator.text = "● AUDIO DETECTED · $formattedRms dB"
                        binding.audioIndicator.setTextColor(ContextCompat.getColor(context, R.color.status_live))
                    } else {
                        binding.audioIndicator.text = "● LISTENING · $formattedRms dB"
                        binding.audioIndicator.setTextColor(ContextCompat.getColor(context, R.color.dexter_primary))
                    }
                }
            }
        }

        // Observe Live Diagnostics Report
        dialogScope.launch {
            speechManager.latestDiagnostics.collectLatest { diag ->
                if (binding.diagnosticsCard.visibility == View.VISIBLE) {
                    binding.diagnosticsReportText.text = diag.formatDiagnosticReport()
                }
            }
        }

        // Observe Live Transcript
        dialogScope.launch {
            speechManager.partialTranscript.collectLatest { transcript ->
                if (transcript.isNotBlank()) {
                    binding.transcriptText.text = "\"$transcript\""
                }
            }
        }

        // Observe TTS Spoken Response
        dialogScope.launch {
            speechManager.lastSpokenResponse.collectLatest { response ->
                if (response.isNotBlank()) {
                    binding.responseText.visibility = View.VISIBLE
                    binding.responseText.text = response
                }
            }
        }

        speechManager.onSpeechResult = { recognizedText ->
            processUserText(recognizedText)
        }

        speechManager.onSpeechError = { errorMsg ->
            dialogScope.launch(Dispatchers.Main) {
                binding.stateLabel.text = "Voice Assistant Ready"
                binding.transcriptText.text = errorMsg
                binding.audioIndicator.visibility = View.GONE
            }
        }
    }

    fun processRecognizedText(text: String) {
        processUserText(text)
    }

    private fun processUserText(text: String) {
        binding.transcriptText.text = "\"$text\""
        speechManager.setVisualState(OrbVisualState.PROCESSING)

        dialogScope.launch {
            try {
                // Tier 1 Fast-Path Regex router
                val parsed = DexterIntentRouter.parse(text)
                if (parsed.intent != "EMPTY" && parsed.intent != "UNKNOWN" && !parsed.isNegationOrClarification) {
                    val contract = ActionContract(
                        intent = parsed.intent,
                        parameters = parsed.parameters,
                        confirmed = false
                    )
                    handleContract(contract)
                    return@launch
                }

                // If not matched by regex fast-path, try Needle or deterministic fallback
                val httpClient = NeedleHttpClient(context)
                val envelope = httpClient.interpret(text)

                if (envelope.needsClarification) {
                    val prompt = envelope.clarificationPrompt ?: "Could you please clarify that?"
                    speechManager.speak(prompt)
                } else if (envelope.action != null) {
                    handleContract(envelope.action)
                } else {
                    speechManager.speak("I didn't quite catch that. Try saying 'Remind me to call mom at 5 PM' or 'Spent 200 on fuel'.")
                }
            } catch (e: Exception) {
                speechManager.speak("Sorry, I encountered an issue processing that.")
            }
        }
    }

    private fun handleContract(action: ActionContract) {
        val intent = action.intent.uppercase()
        if (DexterContractValidator.isDestructive(intent) && !action.confirmed) {
            // Require user confirmation before destructive execution
            pendingDestructiveAction = action
            binding.confirmationCard.visibility = View.VISIBLE

            val promptText = when (intent) {
                "COMPLETE_TASK" -> "Complete this task? Confirm?"
                "CANCEL_TASK" -> "Cancel this task? Confirm?"
                "COMPLETE_ALL_TASKS" -> "Mark all pending tasks as complete? Confirm?"
                "CANCEL_ALARMS" -> "Cancel all alarms? Confirm?"
                else -> "Proceed with this change? Confirm?"
            }
            binding.confirmationPromptText.text = promptText
            speechManager.speak(promptText)
        } else {
            executeAction(action)
        }
    }

    private fun executeAction(action: ActionContract) {
        dialogScope.launch {
            val result = actionExecutor.executeContract(action)
            speechManager.speak(result.message)
            onActionCompleted?.invoke(result.message)
        }
    }

    override fun dismiss() {
        speechManager.stopListening()
        speechManager.stopSpeaking()
        speechManager.setVisualState(OrbVisualState.IDLE)
        dialogScope.cancel()
        super.dismiss()
    }
}
