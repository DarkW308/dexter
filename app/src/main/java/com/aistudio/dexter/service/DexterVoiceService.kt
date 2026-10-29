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
import com.aistudio.dexter.receiver.DexterAlarmReceiver
import kotlinx.coroutines.*

class DexterVoiceService : Service() {

    companion object {
        private const val TAG = "DexterVoiceService"
        const val CHANNEL_ID = "dexter_voice_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_TRIGGER_PROACTIVE_ALARM = "com.aistudio.dexter.ACTION_TRIGGER_PROACTIVE_ALARM"
        const val ACTION_SIMULATE_WAKE_UP = "com.aistudio.dexter.ACTION_SIMULATE_WAKE_UP"
        const val ACTION_SET_ALEXA_MODE = "com.aistudio.dexter.ACTION_SET_ALEXA_MODE"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_IS_MORNING_BRIEFING = "extra_is_morning_briefing"
        const val EXTRA_ALEXA_MODE_ENABLED = "extra_alexa_mode_enabled"

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

        fun triggerProactiveAlarm(context: Context, alarmId: Long, label: String, isMorningBriefing: Boolean) {
            val intent = Intent(context, DexterVoiceService::class.java).apply {
                action = ACTION_TRIGGER_PROACTIVE_ALARM
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_ALARM_LABEL, label)
                putExtra(EXTRA_IS_MORNING_BRIEFING, isMorningBriefing)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error launching proactive alarm", e)
            }
        }

        fun triggerWakeUpSimulation(context: Context) {
            val intent = Intent(context, DexterVoiceService::class.java).apply {
                action = ACTION_SIMULATE_WAKE_UP
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error simulating wakeup", e)
            }
        }

        fun setAlexaMode(context: Context, enabled: Boolean) {
            val intent = Intent(context, DexterVoiceService::class.java).apply {
                action = ACTION_SET_ALEXA_MODE
                putExtra(EXTRA_ALEXA_MODE_ENABLED, enabled)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting alexa mode", e)
            }
        }

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

    // Proactive autonomous wake-up follow-up state
    private var isProactiveSessionActive = false
    private var proactiveFollowUpCount = 0
    private var activeAlarmId: Long = 0L
    private var activeAlarmLabel: String = ""
    private var isMorningBriefingMode: Boolean = false

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

            initializeAudioEngine()
        } catch (e: Exception) {
            Log.e(TAG, "DexterVoiceService failed to start safely", e)
            stopSelf()
        }
    }

    private fun initializeAudioEngine() {
        val hasMicPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasMicPermission) {
            audioEngine = DexterAudioEngine(
                context = this,
                onWakeWordDetectedCallback = { keyword ->
                    acquireWakeLockTemporarily(15000L)
                    Log.i(TAG, "Offline ONNX wake-word triggered: '$keyword'")
                    updateNotification("Heard '$keyword' • Listening for command...")
                },
                onVoiceTranscribed = { transcribed ->
                    handleVoiceTurn(transcribed)
                    updateNotification("Hands-Free Active • Say \"Hey Dexter\", \"Dexter\", or \"buddy\"")
                },
                onProactiveTimeout = {
                    handleProactiveSilenceFollowUp()
                }
            )
            audioEngine?.startContinuousListening()
        } else {
            Log.w(TAG, "Microphone permission not granted. Voice listening deferred.")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TRIGGER_PROACTIVE_ALARM -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)
                val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Alarm"
                val isBriefing = intent.getBooleanExtra(EXTRA_IS_MORNING_BRIEFING, false)
                startProactiveWakeUpInteraction(alarmId, label, isBriefing)
            }
            ACTION_SIMULATE_WAKE_UP -> {
                startProactiveWakeUpInteraction(0L, "Morning Briefing & Wake-Up Check", true)
            }
            ACTION_SET_ALEXA_MODE -> {
                val enabled = intent.getBooleanExtra(EXTRA_ALEXA_MODE_ENABLED, true)
                audioEngine?.isAlexaHandsFreeEnabled = enabled
                if (enabled) {
                    audioEngine?.startContinuousListening()
                } else {
                    audioEngine?.stopActiveListening()
                }
            }
        }
        return START_STICKY
    }

    /**
     * Proactive Autonomous Wake-Up Conversation:
     * Dexter speaks FIRST without requiring the user to tap or initiate.
     */
    private fun startProactiveWakeUpInteraction(alarmId: Long, label: String, isBriefing: Boolean) {
        acquireWakeLockTemporarily(30000L)
        activeAlarmId = alarmId
        activeAlarmLabel = label
        isMorningBriefingMode = isBriefing
        isProactiveSessionActive = true
        proactiveFollowUpCount = 0

        audioEngine?.isProactiveMode = true

        val greeting = if (isBriefing) {
            "Good morning! It's time for your daily morning briefing. Are you awake?"
        } else {
            "Good morning! Your wake up alarm for '$label' is going off. Are you awake?"
        }

        Log.i(TAG, "Starting autonomous wake-up greeting: '$greeting'")
        audioEngine?.speak(greeting) {
            // When Dexter finishes greeting aloud, immediately open the microphone to listen for user's voice reply!
            if (isProactiveSessionActive) {
                audioEngine?.startContinuousListening()
            }
        }
    }

    /**
     * Escalating Proactive Follow-Up if the user didn't get up or stayed silent:
     * Fulfills user requirement: "but i didn't get up so that it able to interact with me"
     */
    private fun handleProactiveSilenceFollowUp() {
        if (!isProactiveSessionActive) return
        proactiveFollowUpCount++
        acquireWakeLockTemporarily(20000L)

        serviceScope.launch {
            try {
                val repository = (application as com.aistudio.dexter.DexterApp).repository
                val pendingTasks = withContext(Dispatchers.IO) { repository.listPendingTasks() }

                when (proactiveFollowUpCount) {
                    1 -> {
                        // Level 1: Gentle follow-up check
                        val prompt = "Hey, I didn't hear you get up. Are you still sleeping? Say 'I am awake', or say 'Snooze'."
                        Log.i(TAG, "Proactive Follow-up Level 1: $prompt")
                        audioEngine?.speak(prompt) {
                            if (isProactiveSessionActive) {
                                audioEngine?.startContinuousListening()
                            }
                        }
                    }
                    2 -> {
                        // Level 2: Task-informed motivational escalation
                        val topTask = pendingTasks.firstOrNull()?.title
                        val prompt = if (topTask != null) {
                            "Still in bed? You have ${pendingTasks.size} tasks scheduled for today, including '$topTask'. Time to get up! Say 'I am awake' or 'Snooze'."
                        } else {
                            "Still in bed? It's time to rise and conquer the day! Say 'I am awake' or 'Snooze 5 minutes'."
                        }
                        Log.i(TAG, "Proactive Follow-up Level 2: $prompt")
                        audioEngine?.speak(prompt) {
                            if (isProactiveSessionActive) {
                                audioEngine?.startContinuousListening()
                            }
                        }
                    }
                    else -> {
                        // Level 3: Final safety snooze if user is still unresponsive
                        val prompt = "I'll let you rest for 5 more minutes and check in on you again. Snoozing now."
                        Log.i(TAG, "Proactive Follow-up Level 3 (Auto-snooze): $prompt")
                        withContext(Dispatchers.IO) {
                            val newTime = System.currentTimeMillis() + (5 * 60 * 1000L)
                            val snoozeId = repository.snoozeAlarm(activeAlarmId, 5)
                            DexterAlarmReceiver.scheduleExactAlarm(
                                this@DexterVoiceService,
                                snoozeId,
                                "Follow-up Wake Check",
                                newTime / 1000L
                            )
                        }
                        audioEngine?.speak(prompt) {
                            isProactiveSessionActive = false
                            audioEngine?.isProactiveMode = false
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in proactive silence follow-up", e)
                isProactiveSessionActive = false
                audioEngine?.isProactiveMode = false
            }
        }
    }

    /**
     * Handles voice turns for both Alexa commands and Proactive Alarm responses
     */
    private fun handleVoiceTurn(transcribedText: String) {
        acquireWakeLockTemporarily(15000L)

        // If user responded during proactive session, complete the wake-up check
        if (isProactiveSessionActive) {
            isProactiveSessionActive = false
            audioEngine?.isProactiveMode = false
        }

        serviceScope.launch {
            try {
                val repository = (application as com.aistudio.dexter.DexterApp).repository
                val executor = actionExecutor ?: DexterActionExecutor(this@DexterVoiceService)
                val harnessEngine = com.aistudio.dexter.engine.Harness2Engine.getInstance(this@DexterVoiceService, repository, executor)

                val harnessResult = withContext(Dispatchers.IO) {
                    harnessEngine.executeHarness(transcribedText)
                }

                // If user confirmed they're awake and it was morning briefing, offer/read the briefing
                audioEngine?.speak(harnessResult.summaryText)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing voice turn", e)
                audioEngine?.speak("Sorry, I encountered an issue processing that.")
            } finally {
                releaseWakeLock()
            }
        }
    }

    private fun acquireWakeLockTemporarily(timeoutMs: Long = 10000L) {
        try {
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(timeoutMs)
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
                description = "Runs continuous background Alexa voice listening and proactive alarms."
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
            .setContentTitle("Dexter Assistant")
            .setContentText("Offline Voice Active • Listening for \"Hey Dexter\", \"Dexter\", or \"buddy\"")
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
            Log.e(TAG, "startForeground with microphone type failed, retrying generic startForeground: ${e.message}")
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (fatal: Exception) {
                Log.e(TAG, "Fatal startForeground exception: ${fatal.message}")
                stopSelf()
            }
        }
    }

    private fun updateNotification(statusText: String) {
        try {
            val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), piFlags)
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Dexter Assistant")
                .setContentText(statusText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Error updating foreground notification", e)
        }
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
