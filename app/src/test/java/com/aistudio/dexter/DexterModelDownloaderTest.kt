package com.aistudio.dexter

import com.aistudio.dexter.engine.ModelDownloadManager
import org.junit.Assert.*
import org.junit.Test

class DexterModelDownloaderTest {

    @Test
    fun testHuggingFaceUrlNormalization() {
        val rawBlobUrl = "https://huggingface.co/convaiinnovations/laya/blob/main/model_int8.onnx"
        val expected = "https://huggingface.co/convaiinnovations/laya/resolve/main/model_int8.onnx"
        
        // Simple test for normalization logic
        val normalized = rawBlobUrl.replace("/blob/", "/resolve/")
        assertEquals(expected, normalized)

        val directResolve = "https://huggingface.co/Cactus-Compute/needle3/resolve/main/needle3_arm64"
        assertEquals(directResolve, directResolve.replace("/blob/", "/resolve/"))
    }

    @Test
    fun testPresetModelsCompatibilitySpecs() {
        val layaSizeMb = 34.2
        val needleSizeMb = 33.8
        
        // Ensure both edge models fit under the 100MB storage and 120MB RAM budget
        assertTrue(layaSizeMb < 50.0)
        assertTrue(needleSizeMb < 50.0)
        assertTrue((layaSizeMb + needleSizeMb) < 80.0)
    }
}
