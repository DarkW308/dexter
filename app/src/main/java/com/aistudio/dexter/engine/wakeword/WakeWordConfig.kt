package com.aistudio.dexter.engine.wakeword

/**
 * Configuration parameters and data models for offline wake-word detection.
 */
object WakeWordConfig {
    const val KEYWORD_HEY_DEXTER = "Hey Dexter"
    const val KEYWORD_DEXTER = "Dexter"
    const val KEYWORD_BUDDY = "buddy"

    val SUPPORTED_KEYWORDS = listOf(
        KEYWORD_HEY_DEXTER,
        KEYWORD_BUDDY
    )

    const val SAMPLE_RATE_HZ = 16000
    const val FRAME_SIZE_SAMPLES = 400     // 25ms @ 16kHz
    const val FRAME_STEP_SAMPLES = 160     // 10ms hop @ 16kHz
    const val FFT_SIZE = 512               // Radix-2 FFT padding
    const val MEL_BINS = 40                // Log-Mel filterbank channels
    const val WINDOW_DURATION_MS = 1200L   // 1.2s rolling audio buffer
    const val WINDOW_SAMPLES = (SAMPLE_RATE_HZ * WINDOW_DURATION_MS / 1000).toInt() // 19200 samples

    const val DEFAULT_CONFIDENCE_THRESHOLD = 0.65f
    const val DEBOUNCE_COOLDOWN_MS = 1200L // Minimum duration between successive wake triggers
}

data class WakeWordDetection(
    val keyword: String,
    val confidence: Float,
    val timestampMs: Long = System.currentTimeMillis()
)
