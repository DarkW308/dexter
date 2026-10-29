package com.aistudio.dexter.engine.wakeword

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Continuous background audio recorder that captures 16kHz 16-bit PCM mono stream
 * and pipes it to the OnnxWakeWordDetector for offline keyword spotting.
 */
class ContinuousWakeWordRecorder(
    private val context: Context,
    private val detector: OnnxWakeWordDetector
) {

    companion object {
        private const val TAG = "WakeWordRecorder"
        private const val SAMPLE_RATE = WakeWordConfig.SAMPLE_RATE_HZ
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val CHUNK_SIZE_SAMPLES = 1600 // 100ms chunk @ 16kHz
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val isRunning = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)

    val isActive: Boolean
        get() = isRunning.get() && !isPaused.get()

    /**
     * Starts continuous low-latency audio capture on a dedicated thread.
     */
    @SuppressLint("MissingPermission")
    @Synchronized
    fun startRecording(): Boolean {
        if (isRunning.get()) {
            isPaused.set(false)
            return true
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "Cannot start wake-word recorder: RECORD_AUDIO permission missing.")
            return false
        }

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = maxOf(minBufferSize * 2, CHUNK_SIZE_SAMPLES * 4)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                // Try fallback to MIC audio source
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed.")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            isRunning.set(true)
            isPaused.set(false)

            recordingThread = Thread({
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
                val buffer = ShortArray(CHUNK_SIZE_SAMPLES)

                while (isRunning.get()) {
                    if (isPaused.get()) {
                        try {
                            Thread.sleep(100L)
                        } catch (e: InterruptedException) {
                            break
                        }
                        continue
                    }

                    val record = audioRecord ?: break
                    val readCount = record.read(buffer, 0, buffer.size)

                    if (readCount > 0) {
                        detector.feedAudio(buffer, readCount)
                    } else if (readCount < 0) {
                        Log.w(TAG, "AudioRecord read error: $readCount")
                        try {
                            Thread.sleep(50L)
                        } catch (e: InterruptedException) {
                            break
                        }
                    }
                }
            }, "DexterWakeWordAudioThread").apply {
                isDaemon = true
                start()
            }

            Log.i(TAG, "Continuous offline wake-word recorder started.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start wake-word recording loop", e)
            stopRecording()
            return false
        }
    }

    /**
     * Temporarily pauses wake-word recording (e.g., when Dexter is actively speaking).
     */
    fun pause() {
        isPaused.set(true)
        detector.reset()
    }

    /**
     * Resumes wake-word recording after speaking finishes.
     */
    fun resume() {
        isPaused.set(false)
    }

    /**
     * Stops and tears down AudioRecord thread and resources.
     */
    @Synchronized
    fun stopRecording() {
        isRunning.set(false)
        isPaused.set(false)
        try {
            recordingThread?.interrupt()
            recordingThread = null
        } catch (e: Exception) {
            // Ignore
        }

        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioRecord", e)
        }
        detector.reset()
        Log.i(TAG, "Wake-word audio recording stopped.")
    }
}
