package com.aistudio.dexter.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class NeedleProcessManager(private val context: Context) {

    companion object {
        private const val TAG = "NeedleProcessManager"
        private const val BINARY_ASSET_PATH = "needle/needle3"
        private const val BINARY_FILENAME = "needle3"
        const val NEEDLE_PORT = 8081
        const val HEALTH_ENDPOINT = "http://127.0.0.1:$NEEDLE_PORT/health"
    }

    private var process: Process? = null
    private val isRunning = AtomicBoolean(false)
    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    /**
     * Unpacks and launches the Needle 3 ARM64 background process.
     */
    fun startDaemon() {
        if (isRunning.get()) return

        scope.launch {
            try {
                val binaryFile = prepareBinary()
                if (binaryFile != null && binaryFile.exists()) {
                    launchProcess(binaryFile)
                    startHealthMonitor()
                } else {
                    Log.i(TAG, "Tier 1 deterministic fast-path active (low-memory/storage optimized).")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting Needle daemon", e)
            }
        }
    }

    /**
     * Extracts binary from assets to filesDir and sets 755 permissions.
     */
    private fun prepareBinary(): File? {
        val destination = File(context.filesDir, BINARY_FILENAME)
        try {
            val assetManager = context.assets
            val list = assetManager.list("needle") ?: emptyArray()
            if (!list.contains(BINARY_FILENAME) && !destination.exists()) {
                return null
            }

            if (!destination.exists()) {
                assetManager.open(BINARY_ASSET_PATH).use { input ->
                    FileOutputStream(destination).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val cactDestination = File(context.filesDir, "needle3.cact")
            if (!cactDestination.exists() && list.contains("needle3.cact")) {
                assetManager.open("needle/needle3.cact").use { input ->
                    FileOutputStream(cactDestination).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(TAG, "Unpacked bundled default Needle 3 weights (${cactDestination.length() / (1024 * 1024)} MB)")
            }

            // Set executable chmod 755
            destination.setReadable(true, false)
            destination.setExecutable(true, false)
            try {
                Runtime.getRuntime().exec(arrayOf("chmod", "755", destination.absolutePath)).waitFor()
            } catch (e: Exception) {
                Log.w(TAG, "chmod failed, fallback to File.setExecutable", e)
            }

            return destination
        } catch (e: Exception) {
            Log.w(TAG, "Assets needle binary unpack notice: ${e.message}")
            return if (destination.exists()) destination else null
        }
    }

    private fun launchProcess(binary: File) {
        try {
            val pb = ProcessBuilder(binary.absolutePath, "--serve", ":$NEEDLE_PORT")
            pb.directory(context.filesDir)
            pb.redirectErrorStream(true)
            process = pb.start()
            isRunning.set(true)
            Log.i(TAG, "Needle 3 daemon process launched on 127.0.0.1:$NEEDLE_PORT (PID: ${process.toString()})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch process", e)
            isRunning.set(false)
        }
    }

    /**
     * Periodically pings the daemon. Restarts with exponential backoff if terminated.
     */
    private fun startHealthMonitor() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            var backoffMs = 2000L
            while (isActive) {
                delay(backoffMs)
                val healthy = checkHealth()
                if (!healthy && isRunning.get()) {
                    Log.w(TAG, "Needle 3 health check failed. Attempting restart...")
                    restart()
                    backoffMs = (backoffMs * 2).coerceAtMost(30000L)
                } else if (healthy) {
                    backoffMs = 5000L
                }
            }
        }
    }

    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(HEALTH_ENDPOINT)
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    fun isDaemonActive(): Boolean {
        val proc = process ?: return false
        if (!isRunning.get()) return false
        return try {
            proc.exitValue()
            false // Process exited if exitValue did not throw
        } catch (e: IllegalThreadStateException) {
            true // Process still alive
        } catch (t: Throwable) {
            false
        }
    }

    private fun restart() {
        stopDaemon()
        startDaemon()
    }

    /**
     * Clean shutdown logic compatible with Android 5.0 (API 21+).
     */
    fun stopDaemon() {
        monitorJob?.cancel()
        isRunning.set(false)
        try {
            process?.destroy()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                process?.destroyForcibly()
            }
            process = null
            Log.i(TAG, "Needle 3 daemon stopped successfully.")
        } catch (e: Throwable) {
            Log.e(TAG, "Error stopping process", e)
        }
    }
}
