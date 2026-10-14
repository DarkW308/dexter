package com.aistudio.dexter.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aistudio.dexter.MainActivity
import com.aistudio.dexter.R
import com.aistudio.dexter.engine.DexterActionExecutor
import com.aistudio.dexter.engine.DexterAudioEngine
import com.aistudio.dexter.engine.NeedleHttpClient
import com.aistudio.dexter.engine.NeedleProcessManager
import kotlinx.coroutines.*

class DexterVoiceService : Service() {

    companion object {
        private const val TAG = "DexterVoiceService"
        const val CHANNEL_ID = "dexter_voice_channel"
        const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val hasMicPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasMicPermission) {
                Log.w(TAG, "DexterVoiceService deferred: RECORD_AUDIO permission not yet granted.")
                return
            }

            try {
                val intent = Intent(context, DexterVoiceService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unable to start foreground service: ${e.message}")
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DexterVoiceService::class.java)
            context.stopService(intent)
        }

        /**
         * Helper intent to request battery optimization whitelist (Rules 4, 13).
         */
        @SuppressLint("BatteryLife")
        fun requestIgnoreBatteryOptimizations(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val packageName = context.packageName
                if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var wakeLock: PowerManager.WakeLock? = null
    private var processManager: NeedleProcessManager? = null
    private var httpClient: NeedleHttpClient? = null
    private var audioEngine: DexterAudioEngine? = null
    private var actionExecutor: DexterActionExecutor? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "DexterVoiceService initializing...")

        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Dexter:VoiceInferenceLock")

            createNotificationChannel()
            startForegroundWithNotification()

            processManager = NeedleProcessManager(this)
            processManager?.startDaemon()

            httpClient = NeedleHttpClient(this)
            actionExecutor = DexterActionExecutor(this)

            val hasMicPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasMicPermission) {
                audioEngine = DexterAudioEngine(
                    context = this,
                    onWakeWordDetected = {
                        acquireWakeLockTemporarily()
                        audioEngine?.startActiveListening()
                    },
                    onVoiceTranscribed = { transcribed ->
                        handleVoiceTurn(transcribed)
                    }
                )
                audioEngine?.startWakeWordListening()
            } else {
                Log.w(TAG, "Microphone permission not granted. Wake-word deferred.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "DexterVoiceService failed to start safely", e)
            stopSelf()
        }
    }

    private fun handleVoiceTurn(transcribedText: String) {
        acquireWakeLockTemporarily()
        serviceScope.launch {
            try {
                val envelope = httpClient?.interpret(transcribedText)
                if (envelope != null && !envelope.needsClarification && envelope.action != null) {
                    val result = actionExecutor?.executeContract(envelope.action)
                    if (result != null) {
                        audioEngine?.speak(result.message)
                    }
                } else {
                    val prompt = envelope?.clarificationPrompt
                        ?: "I didn't quite catch that. Could you please repeat?"
                    audioEngine?.speak(prompt)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing voice turn", e)
                audioEngine?.speak("Sorry, I encountered an issue processing that.")
            } finally {
                releaseWakeLock()
            }
        }
    }

    private fun acquireWakeLockTemporarily() {
        try {
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(10000L) // Safety cap 10 seconds max
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error acquiring wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            // Ignore release exception
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Dexter Voice Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs background wake-word listening and offline assistant operations."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification() {
        val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            piFlags
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dexter")
            .setContentText("Listening for \"Hey Dexter\" (Offline)")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val hasMicPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasMicPermission) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground caught exception: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        audioEngine?.shutdown()
        processManager?.stopDaemon()
        serviceJob.cancel()
        Log.i(TAG, "DexterVoiceService destroyed cleanly.")
    }
}
