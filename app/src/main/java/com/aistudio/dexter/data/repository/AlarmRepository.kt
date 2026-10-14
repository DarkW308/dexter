package com.aistudio.dexter.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.aistudio.dexter.data.db.DexterDatabaseHelper
import com.aistudio.dexter.data.model.AlarmItem

class AlarmRepository(dbHelper: DexterDatabaseHelper) : BaseRepository(dbHelper) {

    constructor(context: Context) : this(DexterDatabaseHelper.getInstance(context))

    fun createAlarm(alarmTime: Long, label: String, repeatDays: String = "DAILY"): AlarmItem {
        val db = writableDb
        val now = System.currentTimeMillis() / 1000

        val cv = ContentValues().apply {
            put("alarm_time", alarmTime)
            put("label", label.trim())
            put("is_enabled", 1)
            put("repeat_days", repeatDays)
            put("created_at", now)
        }
        val id = db.insertOrThrow("alarms", null, cv)
        return AlarmItem(
            id = id,
            alarmTime = alarmTime,
            label = label.trim(),
            isEnabled = true,
            repeatDays = repeatDays,
            createdAt = now
        )
    }

    fun queryPendingAlarms(limit: Int = 50): List<AlarmItem> {
        val list = mutableListOf<AlarmItem>()
        val db = readableDb
        val now = System.currentTimeMillis() / 1000

        val cursor = db.rawQuery(
            """
            SELECT id, alarm_time, label, is_enabled, repeat_days, created_at
            FROM alarms
            WHERE is_enabled = 1 AND alarm_time >= ?
            ORDER BY alarm_time ASC
            LIMIT ?
            """,
            arrayOf(now.toString(), limit.toString())
        )
        while (cursor.moveToNext()) {
            list.add(mapRowToAlarm(cursor))
        }
        cursor.close()
        return list
    }

    fun listAlarms(limit: Int = 50): List<AlarmItem> {
        val list = mutableListOf<AlarmItem>()
        val db = readableDb
        val cursor = db.rawQuery(
            """
            SELECT id, alarm_time, label, is_enabled, repeat_days, created_at
            FROM alarms
            ORDER BY alarm_time ASC
            LIMIT ?
            """,
            arrayOf(limit.toString())
        )
        while (cursor.moveToNext()) {
            list.add(mapRowToAlarm(cursor))
        }
        cursor.close()
        return list
    }

    fun cancelAlarms(id: Long? = null, actorType: String = "USER"): Int {
        val db = writableDb
        return if (id != null) {
            db.delete("alarms", "id = ?", arrayOf(id.toString()))
        } else {
            db.delete("alarms", null, null)
        }
    }

    fun deleteAlarm(id: Long): Boolean {
        return cancelAlarms(id) > 0
    }

    private fun mapRowToAlarm(cursor: Cursor): AlarmItem {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
        val alarmTime = cursor.getLong(cursor.getColumnIndexOrThrow("alarm_time"))
        val label = cursor.getString(cursor.getColumnIndexOrThrow("label"))
        val isEnabled = cursor.getInt(cursor.getColumnIndexOrThrow("is_enabled")) == 1
        val repeatDays = cursor.getString(cursor.getColumnIndexOrThrow("repeat_days"))
        val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))

        return AlarmItem(
            id = id,
            alarmTime = alarmTime,
            label = label,
            isEnabled = isEnabled,
            repeatDays = repeatDays,
            createdAt = createdAt
        )
    }
}
