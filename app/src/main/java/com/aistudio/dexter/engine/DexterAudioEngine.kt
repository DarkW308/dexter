package com.aistudio.dexter.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.aistudio.dexter.engine.wakeword.ContinuousWakeWordRecorder
import com.aistudio.dexter.engine.wakeword.OnnxWakeWordDetector
import com.aistudio.dexter.engine.wakeword.WakeWordConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class DexterAudioEngine(
    private val context: Context,
    val onWakeWordDetectedCallback: (String) -> Unit,
    private val onVoiceTranscribed: (String) -> Unit,
    var onProactiveTimeout: (() -> Unit)? = null
) : TextToSpeech.OnInitListener {

    constructor(
        context: Context,
        onWakeWordDetected: () -> Unit,
        onVoiceTranscribed: (String) -> Unit,
        onProactiveTimeout: (() -> Unit)? = null
    ) : this(
        context = context,
        onWakeWordDetectedCallback = { _ -> onWakeWordDetected() },
        onVoiceTranscribed = onVoiceTranscribed,
        onProactiveTimeout = onProactiveTimeout
    )

    companion object {
        private const val TAG = "DexterAudioEngine"
        private val WAKE_WORDS_REGEX = Regex("(?i)\\b(alexa|hey alexa|dexter|hey dexter|ok dexter|okay dexter|buddy|hey buddy)\\b")
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val isListeningActive = AtomicBoolean(false)
    var isAlexaHandsFreeEnabled = true
    var isProactiveMode = false

    private val _engineState = MutableStateFlow(OrbVisualState.IDLE)
    val engineState: StateFlow<OrbVisualState> = _engineState.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private var onSpeechDoneCallback: (() -> Unit)? = null
    private var consecutiveErrors = 0

    // Offline ONNX Wake-Word Detector for "Hey Dexter", "Dexter", and "buddy"
    val wakeWordDetector = OnnxWakeWordDetector(context).apply {
        onWakeWordDetected = { detection ->
            Log.i(TAG, "Offline ONNX Wake-Word Triggered: '${detection.keyword}' (conf=${"%.2f".format(detection.confidence)})")
            mainHandler.post {
                handleWakeWordTriggered(detection.keyword)
            }
        }
    }

    // AudioRecord continuous capture for low-power offline wake-word spotting
    val wakeWordRecorder = ContinuousWakeWordRecorder(context, wakeWordDetector)

    private val silenceHallucinations = setOf(
        "thank you",
        "thanks for watching",
        "please subscribe",
        "you",
        "bye",
        "subtitles by",
        "transcription"
    )

    init {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e(TAG, "TTS initialization error", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val res = engine.setLanguage(Locale.getDefault())
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.US)
                }
                engine.setPitch(1.0f)
                engine.setSpeechRate(1.05f)
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _engineState.value = OrbVisualState.SPEAKING
                    }

                    override fun onDone(utteranceId: String?) {
                        _engineState.value = OrbVisualState.IDLE
                        mainHandler.post {
                            val cb = onSpeechDoneCallback
                            onSpeechDoneCallback = null
                            if (cb != null) {
                                cb.invoke()
                            } else if (isAlexaHandsFreeEnabled) {
                                // Resume continuous background wake-word listening after speaking
                                mainHandler.postDelayed({
                                    startContinuousListening()
                                }, 300L)
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _engineState.value = OrbVisualState.IDLE
                        mainHandler.post {
                            val cb = onSpeechDoneCallback
                            onSpeechDoneCallback = null
                            cb?.invoke()
                        }
                    }
                })
                isTtsReady = true
                Log.i(TAG, "TTS engine initialized successfully.")
            }
        }
    }

    /**
     * Starts continuous listening for wake words ("Hey Dexter", "Dexter", "buddy", "Alexa").
     * Launches the offline ONNX wake-word detector on AudioRecord, and in proactive mode
     * launches conversational speech recognizer.
     */
    fun startContinuousListening() {
        if (_engineState.value == OrbVisualState.SPEAKING) {
            Log.d(TAG, "Skipping listen start: currently speaking.")
            return
        }

        if (isProactiveMode) {
            // In proactive alarm/wake-up mode, Dexter needs conversational response directly
            wakeWordRecorder.pause()
            startActiveSpeechCapture(triggerKeyword = null)
            return
        }

        if (isAlexaHandsFreeEnabled) {
            // Start low-latency offline ONNX audio stream
            val started = wakeWordRecorder.startRecording()
            if (!started) {
                // If AudioRecord fails (e.g. mic held or emulator without AudioRecord), fallback to SpeechRecognizer loop
                startActiveSpeechCapture(triggerKeyword = null)
            } else {
                _engineState.value = OrbVisualState.IDLE
            }
        }
    }

    /**
     * Handles wake-word spotted by the ONNX detector ("Hey Dexter", "Dexter", "buddy").
     */
    private fun handleWakeWordTriggered(keyword: String) {
        if (_engineState.value == OrbVisualState.SPEAKING) return

        onWakeWordDetectedCallback(keyword)
        _engineState.value = OrbVisualState.LISTENING

        // Pause wake-word recorder and open microphone for speech transcription of user's command
        wakeWordRecorder.pause()
        startActiveSpeechCapture(keyword)
    }

    /**
     * Captures speech utterance following wake-word activation or proactive alarm inquiry.
     */
    private fun startActiveSpeechCapture(triggerKeyword: String?) {
        if (isListeningActive.get()) return

        mainHandler.post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Log.w(TAG, "Speech recognition unavailable on this device.")
                    return@post
                }

                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                }

                isListeningActive.set(true)
                _engineState.value = OrbVisualState.LISTENING

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _engineState.value = OrbVisualState.LISTENING
                    }

                    override fun onBeginningOfSpeech() {
                        consecutiveErrors = 0
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _engineState.value = OrbVisualState.PROCESSING
                    }

                    override fun onError(error: Int) {
                        isListeningActive.set(false)
                        _engineState.value = OrbVisualState.IDLE
                        Log.d(TAG, "SpeechRecognizer status code: $error (isProactive=$isProactiveMode)")

                        if (isProactiveMode) {
                            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_CLIENT) {
                                mainHandler.postDelayed({
                                    onProactiveTimeout?.invoke()
                                }, 500L)
                            } else {
                                mainHandler.postDelayed({
                                    startContinuousListening()
                                }, 800L)
                            }
                            return
                        }

                        // Return to offline ONNX wake-word scanning
                        if (isAlexaHandsFreeEnabled) {
                            wakeWordRecorder.resume()
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isListeningActive.set(false)
                        _engineState.value = OrbVisualState.PROCESSING
                        consecutiveErrors = 0

                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim() ?: ""
                        _partialText.value = text
                        val cleaned = filterHallucinations(text)

                        Log.i(TAG, "Heard transcript: '$cleaned' (keyword=$triggerKeyword, isProactive=$isProactiveMode)")

                        if (cleaned.isNotBlank()) {
                            if (isProactiveMode) {
                                onVoiceTranscribed(cleaned)
                            } else {
                                // If triggered by ONNX wake word, dispatch directly
                                if (triggerKeyword != null) {
                                    onVoiceTranscribed(cleaned)
                                } else if (WAKE_WORDS_REGEX.containsMatchIn(cleaned)) {
                                    onWakeWordDetectedCallback("dexter")
                                    onVoiceTranscribed(cleaned)
                                } else {
                                    // Spoken speech didn't address the assistant; silently resume listening
                                    _engineState.value = OrbVisualState.IDLE
                                    if (isAlexaHandsFreeEnabled) {
                                        wakeWordRecorder.resume()
                                    }
                                }
                            }
                        } else {
                            if (isProactiveMode) {
                                onProactiveTimeout?.invoke()
                            } else {
                                _engineState.value = OrbVisualState.IDLE
                                if (isAlexaHandsFreeEnabled) {
                                    wakeWordRecorder.resume()
                                }
                            }
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let {
                            _partialText.value = it
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error initiating speechRecognizer", e)
                isListeningActive.set(false)
                _engineState.value = OrbVisualState.IDLE
                if (isAlexaHandsFreeEnabled) {
                    wakeWordRecorder.resume()
                }
            }
        }
    }

    fun startWakeWordListening() {
        startContinuousListening()
    }

    fun startActiveListening() {
        wakeWordRecorder.pause()
        startActiveSpeechCapture(triggerKeyword = "manual")
    }

    fun stopActiveListening() {
        isListeningActive.set(false)
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                // Ignore stop error
            }
            if (_engineState.value == OrbVisualState.LISTENING) {
                _engineState.value = OrbVisualState.IDLE
            }
        }
    }

    /**
     * Speaks response text via native TextToSpeech.
     * Pauses wake-word recording during speech to avoid acoustic feedback.
     */
    fun speak(text: String, onDone: (() -> Unit)? = null) {
        wakeWordRecorder.pause()
        stopActiveListening()
        onSpeechDoneCallback = onDone
        _engineState.value = OrbVisualState.SPEAKING

        if (isTtsReady && tts != null) {
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "DEXTER_VOICE_OUT")
            }
            val res = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "DEXTER_VOICE_OUT")
            if (res != TextToSpeech.SUCCESS) {
                Log.w(TAG, "TTS speak returned non-success code $res, triggering fallback onDone")
                mainHandler.postDelayed({
                    val cb = onSpeechDoneCallback
                    onSpeechDoneCallback = null
                    cb?.invoke()
                }, 2000L)
            }
        } else {
            // Emulate completion delay if TTS hardware not initialized
            val estimatedDurationMs = (text.length * 65L).coerceIn(1500L, 5000L)
            mainHandler.postDelayed({
                _engineState.value = OrbVisualState.IDLE
                val cb = onSpeechDoneCallback
                onSpeechDoneCallback = null
                cb?.invoke()
            }, estimatedDurationMs)
        }
    }

    /**
     * Immediately silences TTS (Barge-In).
     */
    fun stopSpeaking() {
        tts?.stop()
        onSpeechDoneCallback = null
        if (_engineState.value == OrbVisualState.SPEAKING) {
            _engineState.value = OrbVisualState.IDLE
        }
    }

    private fun filterHallucinations(text: String): String {
        val trimmed = text.trim()
        val lower = trimmed.lowercase()
        if (silenceHallucinations.contains(lower)) return ""
        return trimmed
    }

    fun shutdown() {
        isAlexaHandsFreeEnabled = false
        isProactiveMode = false
        stopSpeaking()
        stopActiveListening()
        wakeWordRecorder.stopRecording()
        wakeWordDetector.release()
        tts?.shutdown()
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
