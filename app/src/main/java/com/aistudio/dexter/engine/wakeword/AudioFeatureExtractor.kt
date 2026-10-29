package com.aistudio.dexter.engine.wakeword

import kotlin.math.*

/**
 * Fast, low-latency audio feature extractor for 16kHz PCM speech.
 * Computes Log-Mel Filterbank energies (40 bins) and spectral acoustic characteristics
 * suitable for feeding ONNX neural models and acoustic pattern matchers.
 */
class AudioFeatureExtractor(
    val sampleRate: Int = WakeWordConfig.SAMPLE_RATE_HZ,
    val frameLength: Int = WakeWordConfig.FRAME_SIZE_SAMPLES,
    val frameStep: Int = WakeWordConfig.FRAME_STEP_SAMPLES,
    val fftSize: Int = WakeWordConfig.FFT_SIZE,
    val numMelBins: Int = WakeWordConfig.MEL_BINS
) {
    private val hammingWindow = FloatArray(frameLength) { n ->
        (0.54 - 0.46 * cos(2.0 * PI * n / (frameLength - 1))).toFloat()
    }

    private val melFilterBank: Array<FloatArray> = buildMelFilterBank()

    private fun hzToMel(hz: Double): Double = 2595.0 * log10(1.0 + hz / 700.0)
    private fun melToHz(mel: Double): Double = 700.0 * (10.0.pow(mel / 2595.0) - 1.0)

    private fun buildMelFilterBank(): Array<FloatArray> {
        val lowFreq = 20.0
        val highFreq = sampleRate / 2.0
        val lowMel = hzToMel(lowFreq)
        val highMel = hzToMel(highFreq)

        val melPoints = DoubleArray(numMelBins + 2)
        val deltaMel = (highMel - lowMel) / (numMelBins + 1)
        for (i in melPoints.indices) {
            melPoints[i] = lowMel + i * deltaMel
        }

        val binIndices = IntArray(numMelBins + 2)
        for (i in binIndices.indices) {
            val hz = melToHz(melPoints[i])
            binIndices[i] = floor((fftSize + 1) * hz / sampleRate).toInt().coerceIn(0, fftSize / 2)
        }

        val bank = Array(numMelBins) { FloatArray(fftSize / 2 + 1) }
        for (m in 0 until numMelBins) {
            val left = binIndices[m]
            val center = binIndices[m + 1]
            val right = binIndices[m + 2]

            for (k in left..center) {
                if (center > left) {
                    bank[m][k] = (k - left).toFloat() / (center - left)
                }
            }
            for (k in center..right) {
                if (right > center) {
                    bank[m][k] = (right - k).toFloat() / (right - center)
                }
            }
        }
        return bank
    }

    /**
     * Converts 16-bit PCM shorts to normalized [-1.0f, 1.0f] float audio buffer.
     */
    fun pcmToFloat(pcmSamples: ShortArray): FloatArray {
        val floats = FloatArray(pcmSamples.size)
        for (i in pcmSamples.indices) {
            floats[i] = pcmSamples[i] / 32768.0f
        }
        return floats
    }

    /**
     * Extracts 2D Log-Mel Filterbank energies [numFrames, numMelBins] from float audio samples.
     */
    fun extractLogMelFeatures(samples: FloatArray): Array<FloatArray> {
        if (samples.size < frameLength) {
            return Array(0) { FloatArray(numMelBins) }
        }

        // Apply pre-emphasis filter: y[t] = x[t] - 0.97 * x[t-1]
        val emphasized = FloatArray(samples.size)
        emphasized[0] = samples[0]
        for (i in 1 until samples.size) {
            emphasized[i] = samples[i] - 0.97f * samples[i - 1]
        }

        val numFrames = 1 + (samples.size - frameLength) / frameStep
        val features = Array(numFrames) { FloatArray(numMelBins) }

        val real = DoubleArray(fftSize)
        val imag = DoubleArray(fftSize)
        val powerSpectrum = FloatArray(fftSize / 2 + 1)

        for (frameIdx in 0 until numFrames) {
            val start = frameIdx * frameStep
            real.fill(0.0)
            imag.fill(0.0)

            for (i in 0 until frameLength) {
                real[i] = (emphasized[start + i] * hammingWindow[i]).toDouble()
            }

            fft(real, imag)

            val scale = 1.0f / fftSize
            for (k in 0..fftSize / 2) {
                val magSq = (real[k] * real[k] + imag[k] * imag[k]).toFloat()
                powerSpectrum[k] = magSq * scale
            }

            for (m in 0 until numMelBins) {
                var melEnergy = 0.0f
                val filter = melFilterBank[m]
                for (k in 0..fftSize / 2) {
                    melEnergy += filter[k] * powerSpectrum[k]
                }
                // Log compression (log(max(energy, 1e-6)))
                features[frameIdx][m] = ln(max(melEnergy, 1e-6f))
            }
        }

        return features
    }

    /**
     * Radix-2 Cooley-Tukey In-place Decimation-in-Time FFT.
     */
    private fun fft(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR
                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        var len = 2
        while (len <= n) {
            val angle = -2.0 * PI / len
            val wStepR = cos(angle)
            val wStepI = sin(angle)
            var i = 0
            while (i < n) {
                var wR = 1.0
                var wI = 0.0
                for (k in 0 until len / 2) {
                    val uR = real[i + k]
                    val uI = imag[i + k]
                    val vR = real[i + k + len / 2] * wR - imag[i + k + len / 2] * wI
                    val vI = real[i + k + len / 2] * wI + imag[i + k + len / 2] * wR
                    real[i + k] = uR + vR
                    imag[i + k] = uI + vI
                    real[i + k + len / 2] = uR - vR
                    imag[i + k + len / 2] = uI - vI
                    val nextWR = wR * wStepR - wI * wStepI
                    val nextWI = wR * wStepI + wI * wStepR
                    wR = nextWR
                    wI = nextWI
                }
                i += len
            }
            len = len shl 1
        }
    }
}
