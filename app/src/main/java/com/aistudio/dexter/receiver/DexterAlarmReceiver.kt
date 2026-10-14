package com.aistudio.dexter.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aistudio.dexter.MainActivity
import com.aistudio.dexter.R

class DexterAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DexterAlarmReceiver"
        const val ACTION_ALARM_TRIGGER = "com.aistudio.dexter.ALARM_TRIGGER"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val CHANNEL_ALARM_ID = "dexter_alarms_channel"

        /**
         * Schedules an exact battery-safe alarm using setExactAndAllowWhileIdle for Doze compatibility.
         */
        fun scheduleExactAlarm(
            context: Context,
            alarmId: Long,
            label: String,
            triggerEpochSeconds: Long
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val triggerMs = triggerEpochSeconds * 1000L

            val intent = Intent(context, DexterAlarmReceiver::class.java).apply {
                action = ACTION_ALARM_TRIGGER
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_ALARM_LABEL, label)
            }

            val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId.toInt(),
                intent,
                piFlags
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerMs,
                        pendingIntent
                    )
                }
                Log.i(TAG, "Scheduled exact alarm #$alarmId '$label' for $triggerEpochSeconds")
            } catch (e: SecurityException) {
                Log.e(TAG, "Exact alarm permission missing, fallback to setWindow", e)
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerMs,
                    pendingIntent
                )
            }
        }

        fun cancelExactAlarm(context: Context, alarmId: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, DexterAlarmReceiver::class.java).apply {
                action = ACTION_ALARM_TRIGGER
            }
            val cancelFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            } else {
                PendingIntent.FLAG_NO_CREATE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId.toInt(),
                intent,
                cancelFlags
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Device booted: Rescheduling active pending alarms...")
            try {
                val repository = com.aistudio.dexter.data.repository.DexterRepository(context)
                val pending = repository.listAlarms()
                for (alarm in pending) {
                    scheduleExactAlarm(context, alarm.id, alarm.label, alarm.alarmTime)
                }
                Log.i(TAG, "Restored ${pending.size} active exact alarms on boot.")
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring alarms on boot", e)
            }
            return
        }

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)
        val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Reminder"

        Log.i(TAG, "Alarm triggered: #$alarmId '$label'")

        // 1. Vibration alert
        triggerVibration(context)

        // 2. Heads-up notification
        showAlarmNotification(context, alarmId, label)
    }

    private fun triggerVibration(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 400, 200, 400),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 400, 200, 400), -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed", e)
        }
    }

    private fun showAlarmNotification(context: Context, alarmId: Long, label: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ALARM_ID,
                "Dexter Alarms & Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up delivery for scheduled voice reminders."
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val openIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            Intent(context, MainActivity::class.java),
            piFlags
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALARM_ID)
            .setContentTitle("⏰ Dexter Reminder")
            .setContentText(label)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()

        notificationManager.notify(alarmId.toInt(), notification)
    }
}
