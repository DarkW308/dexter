package com.aistudio.dexter.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.aistudio.dexter.data.db.DexterDatabaseHelper
import com.aistudio.dexter.data.model.AlarmItem

class AlarmRepository(dbHelper: DexterDatabaseHelper) : BaseRepository(dbHelper) {

    constructor(context: Context) : this(DexterDatabaseHelper.getInstance(context))

    fun createAlarm(alarmTime: Long, label: String, repeatDays: String = ""): AlarmItem {
        val db = writableDb
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("alarm_time", alarmTime)
            put("label", label.trim())
            put("repeat_days", repeatDays)
            put("is_enabled", 1)
            put("created_at", now)
        }
        val id = db.insertOrThrow("alarms", null, values)
        return AlarmItem(
            id = id,
            alarmTime = alarmTime,
            label = label.trim(),
            repeatDays = repeatDays,
            isEnabled = true,
            createdAt = now
        )
    }

    fun queryPendingAlarms(limit: Int = 20): List<AlarmItem> {
        val list = mutableListOf<AlarmItem>()
        val db = readableDb
        val now = System.currentTimeMillis()
        val cursor = db.rawQuery(
            "SELECT id, alarm_time, label, repeat_days, is_enabled, created_at FROM alarms WHERE is_enabled = 1 AND alarm_time >= ? ORDER BY alarm_time ASC LIMIT ?",
            arrayOf(now.toString(), limit.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    AlarmItem(
                        id = it.getLong(0),
                        alarmTime = it.getLong(1),
                        label = it.getString(2) ?: "Alarm",
                        repeatDays = it.getString(3) ?: "",
                        isEnabled = it.getInt(4) == 1,
                        createdAt = it.getLong(5)
                    )
                )
            }
        }
        return list
    }

    fun cancelAlarms(actorType: String = "USER"): Int {
        val db = writableDb
        val values = ContentValues().apply {
            put("is_enabled", 0)
        }
        return db.update("alarms", values, "is_enabled = 1", null)
    }

    fun listAlarms(limit: Int = 50): List<AlarmItem> {
        val list = mutableListOf<AlarmItem>()
        val db = readableDb
        val cursor = db.rawQuery(
            "SELECT id, alarm_time, label, repeat_days, is_enabled, created_at FROM alarms ORDER BY is_enabled DESC, alarm_time ASC LIMIT ?",
            arrayOf(limit.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    AlarmItem(
                        id = it.getLong(0),
                        alarmTime = it.getLong(1),
                        label = it.getString(2) ?: "Alarm",
                        repeatDays = it.getString(3) ?: "",
                        isEnabled = it.getInt(4) == 1,
                        createdAt = it.getLong(5)
                    )
                )
            }
        }
        return list
    }
}
