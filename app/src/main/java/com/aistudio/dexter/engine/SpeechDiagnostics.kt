package com.aistudio.dexter.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

data class HardwareProbeResult(
    val isWorking: Boolean,
    val peakAmplitude: Int,
    val rmsDb: Float,
    val message: String
)

data class SpeechSessionDiagnostics(
    val sessionStartTimeMs: Long = System.currentTimeMillis(),
    var durationMs: Long = 0L,
    var recognitionServicePackage: String = "Default / Auto-detected",
    var isMicrophoneMuted: Boolean = false,
    var audioMode: String = "NORMAL",
    var onReadyForSpeechFired: Boolean = false,
    var onBeginningOfSpeechFired: Boolean = false,
    var onEndOfSpeechFired: Boolean = false,
    var currentRmsDb: Float = 0f,
    var minRmsDb: Float = 0f,
    var maxRmsDb: Float = 0f,
    var rmsSamplesCount: Int = 0,
    var sumRmsDb: Float = 0f,
    var rawBuffersCount: Int = 0,
    var lastErrorCode: Int? = null,
    var lastErrorName: String? = null,
    var lastErrorMessage: String? = null,
    var recognizedText: String? = null,
    var hardwareProbeResult: String? = null
) {
    val avgRmsDb: Float
        get() = if (rmsSamplesCount > 0) sumRmsDb / rmsSamplesCount else 0f

    fun recordRms(rmsdB: Float) {
        currentRmsDb = rmsdB
        if (rmsSamplesCount == 0) {
            minRmsDb = rmsdB
            maxRmsDb = rmsdB
        } else {
            if (rmsdB < minRmsDb) minRmsDb = rmsdB
            if (rmsdB > maxRmsDb) maxRmsDb = rmsdB
        }
        sumRmsDb += rmsdB
        rmsSamplesCount++
    }

    fun getSignalHealthSummary(): String {
        return when {
            lastErrorCode == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "PERMISSION_ERROR: Microphone permission is missing or blocked by Android privacy settings."
            isMicrophoneMuted ->
                "MIC_MUTED: Device microphone is muted in system AudioManager."
            rmsSamplesCount > 0 && maxRmsDb < 0.5f ->
                "DIGITAL_SILENCE: Signal stayed below 0.5 dB throughout session. The recognizer stream received zero audio data. Check system mic access."
            rmsSamplesCount > 0 && maxRmsDb < 3.0f ->
                "VERY_QUIET_SIGNAL: Peak signal was ${String.format(Locale.US, "%.1f", maxRmsDb)} dB (Below speech detection threshold). Tap 'Test Direct Mic' to probe physical hardware."
            maxRmsDb >= 3.0f ->
                "HEALTHY_SIGNAL: Peak signal reached ${String.format(Locale.US, "%.1f", maxRmsDb)} dB. Good audio level."
            else ->
                "NO_AUDIO_DATA: No RMS audio callbacks were received from SpeechRecognizer."
        }
    }

    fun formatDiagnosticReport(): String {
        val peakStr = String.format(Locale.US, "%.1f", maxRmsDb)
        val avgStr = String.format(Locale.US, "%.1f", avgRmsDb)
        val currentStr = String.format(Locale.US, "%.1f", currentRmsDb)

        val errInfo = if (lastErrorCode != null) {
            "Error: $lastErrorName ($lastErrorCode)"
        } else if (recognizedText != null) {
            "Result: \"$recognizedText\""
        } else {
            "State: Listening"
        }

        return buildString {
            append("• Signal Health: ${getSignalHealthSummary()}\n")
            append("• Audio Signal: Peak = $peakStr dB, Avg = $avgStr dB, Current = $currentStr dB ($rmsSamplesCount samples)\n")
            append("• Device Mic: Muted = $isMicrophoneMuted, Mode = $audioMode\n")
            append("• Recognizer Callbacks: Ready = $onReadyForSpeechFired, SpeechBegan = $onBeginningOfSpeechFired, SpeechEnded = $onEndOfSpeechFired\n")
            append("• Status: $errInfo\n")
            append("• Engine: $recognitionServicePackage")
            if (hardwareProbeResult != null) {
                append("\n• Hardware Probe: $hardwareProbeResult")
            }
        }
    }
}

object SpeechHardwareProbe {

    private const val TAG = "DexterSTTDiagnostics"

    /**
     * Directly probes the physical microphone hardware using AudioRecord (MediaRecorder.AudioSource.MIC).
     * Bypasses Google SpeechRecognizer to prove if the hardware mic is physically functioning and delivering PCM audio.
     */
    suspend fun runProbe(context: Context): HardwareProbeResult = withContext(Dispatchers.IO) {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = minBufferSize.coerceAtLeast(4096)

        var audioRecord: AudioRecord? = null
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "[HardwareProbe] RECORD_AUDIO permission missing!")
                return@withContext HardwareProbeResult(
                    isWorking = false,
                    peakAmplitude = 0,
                    rmsDb = -99f,
                    message = "RECORD_AUDIO permission not granted"
                )
            }

            Log.i(TAG, "[HardwareProbe] Initializing direct AudioRecord probe (16kHz PCM 16-bit)...")
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                val errMsg = "AudioRecord failed to initialize (State != INITIALIZED). Another process may hold exclusive microphone lock."
                Log.e(TAG, "[HardwareProbe] $errMsg")
                return@withContext HardwareProbeResult(
                    isWorking = false,
                    peakAmplitude = 0,
                    rmsDb = -99f,
                    message = errMsg
                )
            }

            audioRecord.startRecording()
            val audioBuffer = ShortArray(bufferSize / 2)
            var maxAmplitude = 0
            var sumSquare = 0.0
            var totalSamples = 0L

            val startTime = System.currentTimeMillis()
            Log.i(TAG, "[HardwareProbe] Sampling microphone hardware for 1.2 seconds...")

            while (System.currentTimeMillis() - startTime < 1200) {
                val read = audioRecord.read(audioBuffer, 0, audioBuffer.size)
                if (read > 0) {
                    for (i in 0 until read) {
                        val sample = audioBuffer[i].toInt()
                        val abs = Math.abs(sample)
                        if (abs > maxAmplitude) maxAmplitude = abs
                        sumSquare += (sample.toDouble() * sample.toDouble())
                        totalSamples++
                    }
                }
                delay(20)
            }

            audioRecord.stop()

            val rms = if (totalSamples > 0) Math.sqrt(sumSquare / totalSamples) else 0.0
            // Normalize to approx 0-90 dB scale
            val rmsDb = if (rms > 1.0) (20 * Math.log10(rms / 32767.0) + 90.0).toFloat().coerceIn(0f, 90f) else 0f

            val isWorking = maxAmplitude > 250
            val summaryMsg = if (isWorking) {
                "Direct Hardware Mic is ACTIVE! Peak amplitude: $maxAmplitude (PCM), RMS: ${String.format(Locale.US, "%.1f", rmsDb)} dB. Hardware capture confirmed working."
            } else {
                "Direct Hardware Mic captured SILENCE (Peak: $maxAmplitude). Hardware or system privacy mute is active."
            }

            Log.i(TAG, "[HardwareProbe] Result: $summaryMsg")

            HardwareProbeResult(
                isWorking = isWorking,
                peakAmplitude = maxAmplitude,
                rmsDb = rmsDb,
                message = summaryMsg
            )
        } catch (e: Exception) {
            val err = "Mic hardware probe error: ${e.message}"
            Log.e(TAG, "[HardwareProbe] $err", e)
            HardwareProbeResult(
                isWorking = false,
                peakAmplitude = 0,
                rmsDb = -99f,
                message = err
            )
        } finally {
            try {
                audioRecord?.release()
            } catch (ignored: Exception) {}
        }
    }
}
