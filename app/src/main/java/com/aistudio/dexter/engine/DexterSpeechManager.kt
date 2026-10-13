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

        _orbState.value = OrbVisualState.LISTENING
        _partialTranscript.value = ""

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
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
                        Log.w("DexterSpeech", "Speech recognition error: $error")
                        _orbState.value = OrbVisualState.IDLE
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
            }
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

    fun shutdown() {
        stopSpeaking()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
