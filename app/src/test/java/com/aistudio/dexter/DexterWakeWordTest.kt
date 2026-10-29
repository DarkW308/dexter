package com.aistudio.dexter

import com.aistudio.dexter.engine.DexterIntentRouter
import com.aistudio.dexter.engine.wakeword.AudioFeatureExtractor
import com.aistudio.dexter.engine.wakeword.OnnxWakeWordDetector
import com.aistudio.dexter.engine.wakeword.WakeWordConfig
import com.aistudio.dexter.engine.wakeword.WakeWordDetection
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class DexterWakeWordTest {

    private lateinit var featureExtractor: AudioFeatureExtractor
    private lateinit var detector: OnnxWakeWordDetector

    @Before
    fun setUp() {
        featureExtractor = AudioFeatureExtractor()
        detector = OnnxWakeWordDetector(
            context = null,
            featureExtractor = featureExtractor,
            confidenceThreshold = 0.50f,
            cooldownMs = 500L
        )
    }

    @Test
    fun testWakeWordConfigConstants() {
        assertTrue(WakeWordConfig.SUPPORTED_KEYWORDS.contains("Hey Dexter"))
        assertTrue(WakeWordConfig.SUPPORTED_KEYWORDS.contains("Dexter"))
        assertTrue(WakeWordConfig.SUPPORTED_KEYWORDS.contains("buddy"))
        assertEquals(16000, WakeWordConfig.SAMPLE_RATE_HZ)
        assertEquals(40, WakeWordConfig.MEL_BINS)
    }

    @Test
    fun testPcmToFloatConversion() {
        val pcm = shortArrayOf(0, 32767, -32768, 16384, -16384)
        val floats = featureExtractor.pcmToFloat(pcm)

        assertEquals(5, floats.size)
        assertEquals(0.0f, floats[0], 0.001f)
        assertEquals(1.0f, floats[1], 0.001f)
        assertEquals(-1.0f, floats[2], 0.001f)
        assertEquals(0.5f, floats[3], 0.001f)
        assertEquals(-0.5f, floats[4], 0.001f)
    }

    @Test
    fun testLogMelSpectrogramExtractionDimensions() {
        // 1 second of 16kHz audio = 16000 samples
        val samples = FloatArray(16000) { i ->
            (sin(2.0 * PI * 440.0 * i / 16000.0) * 0.5).toFloat()
        }

        val features = featureExtractor.extractLogMelFeatures(samples)

        // With 400 frameLength and 160 frameStep:
        // numFrames = 1 + (16000 - 400) / 160 = 1 + 97 = 98 frames
        assertEquals(98, features.size)
        assertEquals(40, features[0].size)

        // Features should contain non-trivial negative/log values
        assertTrue(features[0][10] < 5.0f)
    }

    @Test
    fun testDetectorRejectsSilence() {
        val silence = ShortArray(16000) { 0 }
        val result = detector.detectDirect(silence)
        assertNull("Silence should never trigger wake word", result)
    }

    @Test
    fun testDetectorSpotsBuddy() {
        // Generate synthetic acoustic waveform mimicking "buddy":
        // First half: strong low vowel ~700Hz, followed by dental transition and high front vowel ~2500Hz
        val sampleRate = 16000
        val numSamples = 16000 // 1.0s
        val audio = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val sampleVal = when {
                i < 3000 -> 0.0 // silence onset
                i < 9000 -> {
                    // /bʌ/ low central vowel energy (formant around 700Hz + low fundamental 140Hz)
                    (sin(2 * PI * 140 * t) * 0.4 + sin(2 * PI * 700 * t) * 0.5) * 20000.0
                }
                i < 10000 -> {
                    // /d/ closure
                    sin(2 * PI * 200 * t) * 5000.0
                }
                i < 15000 -> {
                    // /i/ high front vowel (high formant around 2500Hz)
                    (sin(2 * PI * 280 * t) * 0.2 + sin(2 * PI * 2400 * t) * 0.6) * 18000.0
                }
                else -> 0.0
            }
            audio[i] = sampleVal.toInt().coerceIn(-32768, 32767).toShort()
        }

        val result = detector.detectDirect(audio)
        assertNotNull("Buddy should be detected by acoustic classifier", result)
        assertEquals(WakeWordConfig.KEYWORD_BUDDY, result?.keyword)
        assertTrue((result?.confidence ?: 0f) >= 0.50f)
    }

    @Test
    fun testDetectorSpotsDexter() {
        // Generate synthetic acoustic waveform mimicking "Dexter":
        // Plosive onset, mid-vowel ~1200Hz, sharp high-frequency frication burst (/ks/ ~5000Hz), then alveolar ending
        val sampleRate = 16000
        val numSamples = 16000
        val audio = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val sampleVal = when {
                i < 2000 -> 0.0
                i < 6000 -> {
                    // /dɛ/ plosive + mid vowel ~1000Hz
                    (sin(2 * PI * 220 * t) * 0.3 + sin(2 * PI * 1100 * t) * 0.5) * 20000.0
                }
                i < 11000 -> {
                    // /ks/ sharp sibilant frication burst (energy around 5000Hz)
                    (sin(2 * PI * 4800 * t) * 0.6 + sin(2 * PI * 5500 * t) * 0.4) * 22000.0
                }
                i < 15000 -> {
                    // /tər/ release
                    sin(2 * PI * 800 * t) * 8000.0
                }
                else -> 0.0
            }
            audio[i] = sampleVal.toInt().coerceIn(-32768, 32767).toShort()
        }

        val result = detector.detectDirect(audio)
        assertNotNull("Dexter should be detected", result)
        assertTrue(result?.keyword == WakeWordConfig.KEYWORD_DEXTER || result?.keyword == WakeWordConfig.KEYWORD_HEY_DEXTER)
    }

    @Test
    fun testContinuousStreamingFeedAndDebounce() {
        var detectedCount = 0
        var lastDetectedWord = ""

        detector.onWakeWordDetected = { detection ->
            detectedCount++
            lastDetectedWord = detection.keyword
        }

        // Create 1.2s synthetic audio for "buddy"
        val sampleRate = 16000
        val totalSamples = 19200
        val audio = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val v = if (i in 4000..10000) {
                (sin(2 * PI * 140 * t) * 0.4 + sin(2 * PI * 700 * t) * 0.5) * 20000.0
            } else if (i in 11000..16000) {
                (sin(2 * PI * 280 * t) * 0.2 + sin(2 * PI * 2400 * t) * 0.6) * 18000.0
            } else {
                0.0
            }
            audio[i] = v.toInt().coerceIn(-32768, 32767).toShort()
        }

        // Feed in 100ms chunks (1600 samples)
        val chunkSize = 1600
        var offset = 0
        while (offset < totalSamples) {
            val count = minOf(chunkSize, totalSamples - offset)
            val chunk = ShortArray(count)
            System.arraycopy(audio, offset, chunk, 0, count)
            detector.feedAudio(chunk, count)
            offset += count
        }

        assertEquals(1, detectedCount)
        assertEquals(WakeWordConfig.KEYWORD_BUDDY, lastDetectedWord)

        // Immediate subsequent feeds within cooldown (500ms) should be suppressed by debounce
        detector.feedAudio(ShortArray(1600), 1600)
        assertEquals("Subsequent feeds within cooldown must be suppressed", 1, detectedCount)
    }

    @Test
    fun testIntentRouterStripsBuddyAndDexter() {
        val inputBuddy = "buddy add task finish project report"
        val strippedBuddy = DexterIntentRouter.stripWakeWord(inputBuddy)
        assertEquals("add task finish project report", strippedBuddy)

        val inputHeyDexter = "hey dexter what is my balance"
        val strippedHeyDexter = DexterIntentRouter.stripWakeWord(inputHeyDexter)
        assertEquals("what is my balance", strippedHeyDexter)

        val inputDexter = "Dexter, create milestone MVP release"
        val strippedDexter = DexterIntentRouter.stripWakeWord(inputDexter)
        assertEquals("create milestone MVP release", strippedDexter)
    }

    @Test
    fun testIntentRouterParsesSpokenCommandWithBuddy() {
        val parsed = DexterIntentRouter.parse("buddy spent 250 on lunch")
        assertEquals("LOG_EXPENSE", parsed.intent)
        assertEquals(250.0, parsed.parameters["amount"])
    }
}
