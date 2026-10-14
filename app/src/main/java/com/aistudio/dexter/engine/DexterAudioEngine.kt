package com.aistudio.dexter.engine

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
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
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

class DexterAudioEngine(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit,
    private val onVoiceTranscribed: (String) -> Unit
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "DexterAudioEngine"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val SILENCE_THRESHOLD_MS = 1200L
        private const val ENERGY_THRESHOLD = 800.0 // RMS amplitude threshold for VAD
    }

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var speechRecognizer: SpeechRecognizer? = null
    private val isListeningActive = AtomicBoolean(false)
    private val isWakeWordLoopRunning = AtomicBoolean(false)

    private var wakeWordThread: HandlerThread? = null
    private var wakeWordHandler: Handler? = null

    private val _engineState = MutableStateFlow(OrbVisualState.IDLE)
    val engineState: StateFlow<OrbVisualState> = _engineState.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val silenceHallucinations = setOf(
        "thank you",
        "thanks for watching",
        "please subscribe",
        "you",
        "bye",
        "subtitles by"
    )

    init {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e(TAG, "TTS init error", e)
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
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _engineState.value = OrbVisualState.IDLE
                    }
                })
                isTtsReady = true
            }
        }
    }

    /**
     * Starts low-power background wake-word listening loop on dedicated THREAD_PRIORITY_LOWEST thread.
     */
    fun startWakeWordListening() {
        if (isWakeWordLoopRunning.get()) return

        wakeWordThread = HandlerThread("DexterWakeWordThread", Process.THREAD_PRIORITY_LOWEST).apply {
            start()
            wakeWordHandler = Handler(looper)
        }

        isWakeWordLoopRunning.set(true)
        wakeWordHandler?.post {
            runAudioCaptureLoop()
        }
    }

    @SuppressLint("MissingPermission")
    private fun runAudioCaptureLoop() {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(1024)
        val audioBuffer = ShortArray(bufferSize)

        var audioRecord: AudioRecord? = null
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                Log.w(TAG, "AudioRecord not initialized. Mic might be occupied.")
                return
            }

            audioRecord.startRecording()
            Log.i(TAG, "Wake-word low-power audio loop started.")

            var lastSpeechTs = System.currentTimeMillis()

            while (isWakeWordLoopRunning.get()) {
                val read = audioRecord.read(audioBuffer, 0, audioBuffer.size)
                if (read > 0) {
                    // Calculate RMS energy for VAD and barge-in
                    var sum = 0.0
                    for (i in 0 until read) {
                        sum += audioBuffer[i] * audioBuffer[i]
                    }
                    val rms = sqrt(sum / read)

                    // Real-time Barge-In: if user speaks while Dexter is talking, immediately silence TTS
                    if (rms > ENERGY_THRESHOLD) {
                        lastSpeechTs = System.currentTimeMillis()
                        if (_engineState.value == OrbVisualState.SPEAKING) {
                            stopSpeaking()
                        }
                    }
                }
                Thread.sleep(30)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in wake-word audio loop", e)
        } finally {
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (e: Exception) {
                // Ignore cleanup error
            }
        }
    }

    /**
     * Starts active voice recording with Android native SpeechRecognizer.
     */
    fun startActiveListening() {
        // Barge-in check
        stopSpeaking()

        _engineState.value = OrbVisualState.LISTENING
        _partialText.value = ""
        isListeningActive.set(true)

        Handler(context.mainLooper).post {
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
                        _engineState.value = OrbVisualState.LISTENING
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _engineState.value = OrbVisualState.PROCESSING
                    }

                    override fun onError(error: Int) {
                        Log.w(TAG, "SpeechRecognizer error: $error")
                        _engineState.value = OrbVisualState.IDLE
                        isListeningActive.set(false)
                    }

                    override fun onResults(results: Bundle?) {
                        _engineState.value = OrbVisualState.PROCESSING
                        isListeningActive.set(false)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        _partialText.value = text

                        val cleaned = filterHallucinations(text)
                        if (cleaned.isNotBlank()) {
                            onVoiceTranscribed(cleaned)
                        } else {
                            _engineState.value = OrbVisualState.IDLE
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
                Log.e(TAG, "Failed to start SpeechRecognizer", e)
                _engineState.value = OrbVisualState.IDLE
            }
        }
    }

    fun stopActiveListening() {
        isListeningActive.set(false)
        Handler(context.mainLooper).post {
            speechRecognizer?.stopListening()
            if (_engineState.value == OrbVisualState.LISTENING) {
                _engineState.value = OrbVisualState.IDLE
            }
        }
    }

    /**
     * Speaks response text via native TextToSpeech.
     */
    fun speak(text: String) {
        if (isTtsReady && tts != null) {
            _engineState.value = OrbVisualState.SPEAKING
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "DEXTER_VOICE_OUT")
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "DEXTER_VOICE_OUT")
        } else {
            _engineState.value = OrbVisualState.SPEAKING
        }
    }

    /**
     * Immediately silences TTS (Barge-In).
     */
    fun stopSpeaking() {
        tts?.stop()
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
        isWakeWordLoopRunning.set(false)
        wakeWordThread?.quitSafely()
        wakeWordThread = null
        stopSpeaking()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
