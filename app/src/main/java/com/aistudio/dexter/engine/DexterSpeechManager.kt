package com.aistudio.dexter.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class OrbVisualState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

class DexterSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsInitialized = false

    private val _orbState = MutableStateFlow(OrbVisualState.IDLE)
    val orbState: StateFlow<OrbVisualState> = _orbState.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _lastSpokenResponse = MutableStateFlow("")
    val lastSpokenResponse: StateFlow<String> = _lastSpokenResponse.asStateFlow()

    var onSpeechResult: ((String) -> Unit)? = null
    var onSpeechError: ((String) -> Unit)? = null

    init {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e("DexterSpeech", "Failed to initialize TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.US)
                }
                engine.setPitch(1.0f)
                engine.setSpeechRate(1.05f)
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _orbState.value = OrbVisualState.SPEAKING
                    }

                    override fun onDone(utteranceId: String?) {
                        _orbState.value = OrbVisualState.IDLE
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _orbState.value = OrbVisualState.IDLE
                    }
                })
                isTtsInitialized = true
            }
        }
    }

    fun setVisualState(state: OrbVisualState) {
        _orbState.value = state
    }

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun speak(text: String) {
        _lastSpokenResponse.value = text
        if (isTtsInitialized && tts != null) {
            _orbState.value = OrbVisualState.SPEAKING
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "DEXTER_UTTERANCE")
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "DEXTER_UTTERANCE")
        } else {
            // Emulate visual pulse if TTS is not ready or silent
            _orbState.value = OrbVisualState.SPEAKING
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        if (_orbState.value == OrbVisualState.SPEAKING) {
            _orbState.value = OrbVisualState.IDLE
        }
    }

    fun startListening() {
        // Barge-in: if Dexter is speaking, silence him immediately
        stopSpeaking()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _orbState.value = OrbVisualState.IDLE
            val msg = "Speech recognizer is not installed on this emulator. You can tap the command chips below or type instructions directly."
            Log.w("DexterSpeech", msg)
            onSpeechError?.invoke(msg)
            return
        }

        _orbState.value = OrbVisualState.LISTENING
        _partialTranscript.value = ""

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _orbState.value = OrbVisualState.LISTENING
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _orbState.value = OrbVisualState.PROCESSING
                }

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Speak closer to the microphone or tap a sample command below."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out. Tap orb to try again or tap a sample command below."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording issue. Tap a sample command or type your command below."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required. Please grant permission."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER ->
                            "Voice network service unavailable on emulator. You can tap sample commands or type below."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer was busy. Ready to listen again."
                        else -> "Speech recognition error ($error). Tap a sample command below to test immediately."
                    }
                    Log.w("DexterSpeech", "Speech recognition error $error: $message")
                    _orbState.value = OrbVisualState.IDLE
                    onSpeechError?.invoke(message)
                }

                override fun onResults(results: Bundle?) {
                    _orbState.value = OrbVisualState.PROCESSING
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    _partialTranscript.value = text
                    val cleaned = filterHallucinations(text)
                    if (cleaned.isNotBlank()) {
                        onSpeechResult?.invoke(cleaned)
                    } else {
                        _orbState.value = OrbVisualState.IDLE
                        onSpeechError?.invoke("No clear speech detected. Try again or tap a command below.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let {
                        _partialTranscript.value = it
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("DexterSpeech", "Error starting SpeechRecognizer", e)
            _orbState.value = OrbVisualState.IDLE
            onSpeechError?.invoke("Could not start speech recognizer: ${e.message}. Use sample commands below.")
        }
    }

    fun shutdown() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e("DexterSpeech", "Error shutting down speech engine", e)
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        if (_orbState.value == OrbVisualState.LISTENING) {
            _orbState.value = OrbVisualState.IDLE
        }
    }

    private fun filterHallucinations(text: String): String {
        val lower = text.trim().lowercase()
        val hallucinatoryPhrases = setOf(
            "thank you",
            "thanks for watching",
            "please subscribe",
            "subtitles by",
            "you"
        )
        if (hallucinatoryPhrases.contains(lower)) return ""
        return text.trim()
    }
}
