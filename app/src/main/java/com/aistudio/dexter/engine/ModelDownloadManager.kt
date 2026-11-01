package com.aistudio.dexter.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class ModelInfo(
    val id: String,
    val name: String,
    val repoId: String,
    val downloadUrl: String,
    val format: String,
    val targetFile: File,
    val estimatedSizeMb: Double,
    val role: String,
    val compatibilityNote: String,
    val isInstalled: Boolean,
    val installedSizeBytes: Long = 0L
)

sealed class ModelDownloadState {
    object Idle : ModelDownloadState()
    data class Progress(val modelId: String, val percent: Int, val bytesDownloaded: Long, val totalBytes: Long) : ModelDownloadState()
    data class Success(val modelId: String, val message: String) : ModelDownloadState()
    data class Failure(val modelId: String, val error: String) : ModelDownloadState()
}

/**
 * ModelDownloadManager
 *
 * Manages downloading, updating, and removing on-device Edge AI models from Hugging Face
 * for offline execution on resource-constrained devices (MediaTek Helio P60 / Android 8.1+).
 * Enables the base APK to remain slim (<40 MB) while downloading real neural weights
 * on first launch into local private storage (100% offline with zero cloud telemetry).
 */
class ModelDownloadManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "ModelDownloadManager"

        @Volatile
        private var INSTANCE: ModelDownloadManager? = null

        fun getInstance(context: Context): ModelDownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ModelDownloadManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    private var currentDownloadJob: Job? = null

    val gemmaTargetFile: File
        get() = File(context.filesDir, "models/gemma3_270m_q4.gguf")

    val layaTargetFile: File
        get() = File(context.filesDir, "models/laya_int8.onnx")

    val needleTargetFile: File
        get() = File(context.filesDir, "needle/needle3.cact")

    fun getPresetModels(): List<ModelInfo> {
        val gemmaFile = gemmaTargetFile
        val layaFile = layaTargetFile
        val needleFile = needleTargetFile

        // Verification checks: File must exceed 1MB to avoid 75-byte text stubs
        val gemmaInstalled = gemmaFile.exists() && gemmaFile.length() > 1024 * 1024
        val layaInstalled = layaFile.exists() && layaFile.length() > 1024 * 1024
        val needleInstalled = needleFile.exists() && needleFile.length() > 1024 * 1024

        val gemmaSize = if (gemmaInstalled) gemmaFile.length() else 0L
        val layaSize = if (layaInstalled) layaFile.length() else 0L
        val needleSize = if (needleInstalled) needleFile.length() else 0L

        return listOf(
            ModelInfo(
                id = "gemma3_270m",
                name = "Gemma 3 270M-IT (Q4_K_M GGUF)",
                repoId = "unsloth/gemma-3-270m-it-GGUF",
                downloadUrl = "https://huggingface.co/unsloth/gemma-3-270m-it-GGUF/resolve/main/gemma-3-270m-it-Q4_K_M.gguf",
                format = "GGUF INT4",
                targetFile = gemmaFile,
                estimatedSizeMb = 241.0,
                role = "Conversational Natural Language & Proactive Dialogue",
                compatibilityNote = "Generative brain · 4-thread ARM NEON on Helio P60 · ~250 MB RAM",
                isInstalled = gemmaInstalled,
                installedSizeBytes = gemmaSize
            ),
            ModelInfo(
                id = "laya",
                name = "Laya (ModernBERT INT8)",
                repoId = "convaiinnovations/laya",
                downloadUrl = "https://huggingface.co/convaiinnovations/laya/resolve/main/model_int8.onnx",
                format = "ONNX INT8",
                targetFile = layaFile,
                estimatedSizeMb = 34.2,
                role = "Fast Decision & Slot-Filling Classifier (ONNX Runtime)",
                compatibilityNote = "Sub-30ms intent parsing · 2-thread ARM NEON · ~34 MB RAM",
                isInstalled = layaInstalled,
                installedSizeBytes = layaSize
            ),
            ModelInfo(
                id = "needle3",
                name = "Needle 3 (121M LSAN)",
                repoId = "Cactus-Compute/needle3",
                downloadUrl = "https://huggingface.co/Cactus-Compute/needle3/resolve/main/needle3.cact",
                format = "Cactus LSAN",
                targetFile = needleFile,
                estimatedSizeMb = 33.8,
                role = "Multi-Step Deep Project Planning & Decomposition",
                compatibilityNote = "Asynchronous planner · Shuts down when idle · ~35 MB RAM",
                isInstalled = needleInstalled,
                installedSizeBytes = needleSize
            )
        )
    }

    fun hasAnyModelInstalled(): Boolean {
        return getPresetModels().any { it.isInstalled }
    }

    /**
     * Resolves a friendly Hugging Face URL into a direct binary resolve endpoint.
     * e.g. "https://huggingface.co/user/repo/blob/main/file.onnx" -> "https://huggingface.co/user/repo/resolve/main/file.onnx"
     */
    fun normalizeHuggingFaceUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        if (url.startsWith("huggingface.co/")) {
            url = "https://$url"
        }
        if (url.contains("huggingface.co/") && url.contains("/blob/")) {
            url = url.replace("/blob/", "/resolve/")
        }
        return url
    }

    /**
     * Downloads model weights from Hugging Face with live streaming progress updates.
     */
    fun startDownload(
        modelId: String,
        customUrl: String? = null,
        onProgress: ((percent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit)? = null,
        onResult: ((success: Boolean, message: String) -> Unit)? = null
    ) {
        val model = getPresetModels().find { it.id == modelId }
        val targetFile = model?.targetFile ?: File(context.filesDir, "models/${modelId}.bin")
        val downloadUrl = normalizeHuggingFaceUrl(customUrl ?: model?.downloadUrl ?: "")

        if (downloadUrl.isBlank()) {
            val err = "Download URL cannot be empty."
            _downloadState.value = ModelDownloadState.Failure(modelId, err)
            onResult?.invoke(false, err)
            return
        }

        currentDownloadJob?.cancel()
        currentDownloadJob = scope.launch {
            _downloadState.value = ModelDownloadState.Progress(modelId, 0, 0, 0)
            targetFile.parentFile?.mkdirs()
            val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")

            try {
                Log.i(TAG, "Starting download for $modelId from: $downloadUrl")
                val request = Request.Builder()
                    .url(downloadUrl)
                    .addHeader("User-Agent", "Dexter-Edge-AI/1.0 (Android; ARM64)")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code}: ${response.message}")
                }

                val body = response.body ?: throw IllegalStateException("Empty response body from server.")
                val totalLength = body.contentLength()

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var downloadedSoFar = 0L
                        var lastReportedPercent = -1

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedSoFar += bytesRead

                            val percent = if (totalLength > 0) {
                                ((downloadedSoFar * 100) / totalLength).toInt().coerceIn(0, 100)
                            } else {
                                -1
                            }

                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                _downloadState.value = ModelDownloadState.Progress(
                                    modelId, percent, downloadedSoFar, totalLength
                                )
                                withContext(Dispatchers.Main) {
                                    onProgress?.invoke(percent, downloadedSoFar, totalLength)
                                }
                            }
                        }
                    }
                }

                // Atomic rename
                if (targetFile.exists()) targetFile.delete()
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }

                // If needle binary, ensure executable permissions
                if (modelId == "needle3") {
                    targetFile.setReadable(true, false)
                    targetFile.setExecutable(true, false)
                }

                // If Laya ONNX, reload session
                if (modelId == "laya") {
                    LayaEngine.getInstance(context).reload()
                }

                val successMsg = "Successfully installed ${model?.name ?: modelId} (${targetFile.length() / (1024 * 1024)} MB). Active offline."
                Log.i(TAG, successMsg)
                _downloadState.value = ModelDownloadState.Success(modelId, successMsg)
                withContext(Dispatchers.Main) {
                    onResult?.invoke(true, successMsg)
                }

            } catch (e: Exception) {
                if (tempFile.exists()) tempFile.delete()
                val errorMsg = if (e is CancellationException) "Download cancelled." else "Download failed: ${e.message}"
                Log.e(TAG, errorMsg, e)
                _downloadState.value = ModelDownloadState.Failure(modelId, errorMsg)
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false, errorMsg)
                }
            }
        }
    }

    fun cancelDownload() {
        currentDownloadJob?.cancel()
        currentDownloadJob = null
        _downloadState.value = ModelDownloadState.Idle
    }

    fun deleteModel(modelId: String): Boolean {
        val model = getPresetModels().find { it.id == modelId } ?: return false
        if (model.targetFile.exists()) {
            val deleted = model.targetFile.delete()
            if (modelId == "laya") {
                LayaEngine.getInstance(context).reload()
            }
            return deleted
        }
        return false
    }
}
