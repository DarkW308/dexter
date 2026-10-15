package com.aistudio.dexter.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.aistudio.dexter.data.db.DexterDatabaseHelper
import com.aistudio.dexter.data.model.AuditLog

class AuditRepository(dbHelper: DexterDatabaseHelper) : BaseRepository(dbHelper) {

    constructor(context: Context) : this(DexterDatabaseHelper.getInstance(context))

    fun logAction(
        action: String,
        details: String = "",
        actorType: String = "USER"
    ): Long {
        val db = writableDb
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("action", action)
            put("details", details)
            put("timestamp", now)
            put("actor_type", actorType)
        }
        return db.insert("audit", null, values)
    }

    fun listRecentLogs(limit: Int = 50): List<AuditLog> {
        val list = mutableListOf<AuditLog>()
        val db = readableDb
        val cursor = db.rawQuery(
            "SELECT id, action, details, timestamp, actor_type FROM audit ORDER BY timestamp DESC LIMIT ?",
            arrayOf(limit.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    AuditLog(
                        id = it.getLong(0),
                        action = it.getString(1) ?: "",
                        details = it.getString(2) ?: "",
                        timestamp = it.getLong(3),
                        actorType = it.getString(4) ?: "USER"
                    )
                )
            }
        }
        return list
    }
}
