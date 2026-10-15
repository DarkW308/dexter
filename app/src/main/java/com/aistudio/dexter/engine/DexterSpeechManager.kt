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
        _orbState.value = OrbVisualState.SPEAKING
        if (isTtsInitialized && tts != null) {
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "DEXTER_UTTERANCE")
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "DEXTER_UTTERANCE")
        }

        // Safety fallback: Ensure orb returns to IDLE even if TTS is silent or lacks hardware on emulator
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (_orbState.value == OrbVisualState.SPEAKING) {
                _orbState.value = OrbVisualState.IDLE
            }
        }, (text.length * 60L).coerceIn(1800L, 6000L))
    }

    fun stopSpeaking() {
        tts?.stop()
        if (_orbState.value == OrbVisualState.SPEAKING) {
            _orbState.value = OrbVisualState.IDLE
        }
    }

    private val _rmsVolume = MutableStateFlow(0f)
    val rmsVolume: StateFlow<Float> = _rmsVolume.asStateFlow()

    private val _latestDiagnostics = MutableStateFlow(SpeechSessionDiagnostics())
    val latestDiagnostics: StateFlow<SpeechSessionDiagnostics> = _latestDiagnostics.asStateFlow()

    fun startListening() {
        // Run on Main Looper to satisfy SpeechRecognizer thread requirements
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post { startListening() }
            return
        }

        // Barge-in: if Dexter is speaking, silence him immediately
        stopSpeaking()

        val diag = SpeechSessionDiagnostics()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
        if (audioManager != null) {
            diag.isMicrophoneMuted = audioManager.isMicrophoneMute
            diag.audioMode = when (audioManager.mode) {
                android.media.AudioManager.MODE_NORMAL -> "MODE_NORMAL"
                android.media.AudioManager.MODE_RINGTONE -> "MODE_RINGTONE"
                android.media.AudioManager.MODE_IN_CALL -> "MODE_IN_CALL"
                android.media.AudioManager.MODE_IN_COMMUNICATION -> "MODE_IN_COMMUNICATION"
                else -> "MODE_${audioManager.mode}"
            }
        }

        val availableServices = try {
            val serviceIntent = Intent("android.speech.RecognitionService")
            val matches = context.packageManager.queryIntentServices(serviceIntent, 0)
            matches.map { it.serviceInfo.packageName }
        } catch (e: Exception) {
            emptyList()
        }

        diag.recognitionServicePackage = if (availableServices.isNotEmpty()) {
            availableServices.joinToString(", ")
        } else {
            "System Default"
        }

        _latestDiagnostics.value = diag

        val currentLocale = Locale.getDefault()
        val languageTag = currentLocale.toLanguageTag().ifBlank { "en-IN" }

        Log.i("DexterSTTDiagnostics", "==================================================")
        Log.i("DexterSTTDiagnostics", "SPEECH RECOGNITION SESSION INITIALIZED")
        Log.i("DexterSTTDiagnostics", "Package: ${context.packageName}")
        Log.i("DexterSTTDiagnostics", "Locale Tag: $languageTag")
        Log.i("DexterSTTDiagnostics", "Microphone Muted (AudioManager): ${diag.isMicrophoneMuted}")
        Log.i("DexterSTTDiagnostics", "Audio Mode: ${diag.audioMode}")
        Log.i("DexterSTTDiagnostics", "Available Recognition Services: $availableServices")
        Log.i("DexterSTTDiagnostics", "==================================================")

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _orbState.value = OrbVisualState.IDLE
            diag.lastErrorMessage = "Speech recognizer service unavailable on this device."
            _latestDiagnostics.value = diag.copy()
            val msg = "Speech recognizer service unavailable on this device. You can tap the sample commands or type below."
            Log.w("DexterSTTDiagnostics", msg)
            onSpeechError?.invoke(msg)
            return
        }

        _orbState.value = OrbVisualState.LISTENING
        _partialTranscript.value = ""
        _rmsVolume.value = 0f

        try {
            // Re-instantiate SpeechRecognizer to clear any blocked state or previous crash
            try {
                speechRecognizer?.destroy()
            } catch (ignored: Exception) {}

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra("android.speech.extra.DICTATION_MODE", true)

                // Extended silence timeouts so users have plenty of time to speak naturally
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _orbState.value = OrbVisualState.LISTENING
                    diag.onReadyForSpeechFired = true
                    _latestDiagnostics.value = diag.copy()
                    Log.i("DexterSTTDiagnostics", "[Callback] onReadyForSpeech: audio stream opened, listening for microphone input")
                }

                override fun onBeginningOfSpeech() {
                    _orbState.value = OrbVisualState.LISTENING
                    diag.onBeginningOfSpeechFired = true
                    _latestDiagnostics.value = diag.copy()
                    Log.i("DexterSTTDiagnostics", "[Callback] onBeginningOfSpeech: speech threshold surpassed (Current RMS = ${diag.currentRmsDb} dB)")
                }

                override fun onRmsChanged(rmsdB: Float) {
                    diag.recordRms(rmsdB)
                    if (rmsdB > 0f) {
                        _rmsVolume.value = rmsdB
                    }
                    _latestDiagnostics.value = diag.copy()
                    if (diag.rmsSamplesCount % 12 == 0 || rmsdB >= diag.maxRmsDb) {
                        Log.d("DexterSTTDiagnostics", "[Signal] RMS Level: ${String.format(Locale.US, "%.1f", rmsdB)} dB | Peak: ${String.format(Locale.US, "%.1f", diag.maxRmsDb)} dB | Samples: ${diag.rmsSamplesCount}")
                    }
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    diag.rawBuffersCount++
                    _latestDiagnostics.value = diag.copy()
                    Log.v("DexterSTTDiagnostics", "[Callback] onBufferReceived: ${buffer?.size ?: 0} raw PCM bytes received")
                }

                override fun onEndOfSpeech() {
                    _orbState.value = OrbVisualState.PROCESSING
                    diag.onEndOfSpeechFired = true
                    _latestDiagnostics.value = diag.copy()
                    Log.i("DexterSTTDiagnostics", "[Callback] onEndOfSpeech: input ended, beginning recognizer processing")
                }

                override fun onError(error: Int) {
                    val (errorName, message) = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO (3)" to "Microphone audio recording issue. Please check permissions."
                        SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT (5)" to "Speech client error. Ready to listen again."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS (9)" to "Microphone permission required. Please grant permission in Settings."
                        SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK (2)" to "Voice network service unavailable."
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT (1)" to "Voice network request timed out."
                        SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH (7)" to "No speech detected. Tap the orb to speak, or choose a command below."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY (8)" to "Voice recognizer was busy. Ready to listen again."
                        SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER (4)" to "Voice recognition server error."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT (6)" to "Listening timed out. Tap the orb to speak, or choose a command below."
                        else -> "ERROR_UNKNOWN ($error)" to "Speech recognition paused ($error). Tap the orb to speak again."
                    }

                    diag.lastErrorCode = error
                    diag.lastErrorName = errorName
                    diag.lastErrorMessage = message
                    diag.durationMs = System.currentTimeMillis() - diag.sessionStartTimeMs
                    _latestDiagnostics.value = diag.copy()

                    Log.w("DexterSTTDiagnostics", "==================================================")
                    Log.w("DexterSTTDiagnostics", "SPEECH RECOGNITION ERROR: $errorName")
                    Log.w("DexterSTTDiagnostics", "Session Duration: ${diag.durationMs}ms")
                    Log.w("DexterSTTDiagnostics", "Total RMS Samples: ${diag.rmsSamplesCount}")
                    Log.w("DexterSTTDiagnostics", "Peak RMS: ${String.format(Locale.US, "%.1f", diag.maxRmsDb)} dB")
                    Log.w("DexterSTTDiagnostics", "Avg RMS: ${String.format(Locale.US, "%.1f", diag.avgRmsDb)} dB")
                    Log.w("DexterSTTDiagnostics", "Speech Began Callback: ${diag.onBeginningOfSpeechFired}")
                    Log.w("DexterSTTDiagnostics", "DIAGNOSIS: ${diag.getSignalHealthSummary()}")
                    Log.w("DexterSTTDiagnostics", "==================================================")

                    _orbState.value = OrbVisualState.IDLE
                    _rmsVolume.value = 0f
                    onSpeechError?.invoke(message)
                }

                override fun onResults(results: Bundle?) {
                    _orbState.value = OrbVisualState.PROCESSING
                    _rmsVolume.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    _partialTranscript.value = text

                    diag.recognizedText = text
                    diag.durationMs = System.currentTimeMillis() - diag.sessionStartTimeMs
                    _latestDiagnostics.value = diag.copy()

                    Log.i("DexterSTTDiagnostics", "==================================================")
                    Log.i("DexterSTTDiagnostics", "SPEECH RECOGNITION SUCCESSFUL: \"$text\"")
                    Log.i("DexterSTTDiagnostics", "Peak RMS: ${String.format(Locale.US, "%.1f", diag.maxRmsDb)} dB | Samples: ${diag.rmsSamplesCount}")
                    Log.i("DexterSTTDiagnostics", "All Alternatives: $matches")
                    Log.i("DexterSTTDiagnostics", "==================================================")

                    val cleaned = filterHallucinations(text)
                    if (cleaned.isNotBlank()) {
                        onSpeechResult?.invoke(cleaned)
                    } else {
                        _orbState.value = OrbVisualState.IDLE
                        onSpeechError?.invoke("No clear speech detected. Tap the orb to speak again.")
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
            Log.e("DexterSTTDiagnostics", "Error starting SpeechRecognizer", e)
            _orbState.value = OrbVisualState.IDLE
            onSpeechError?.invoke("Could not start speech recognizer: ${e.message}. Tap a command below.")
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
