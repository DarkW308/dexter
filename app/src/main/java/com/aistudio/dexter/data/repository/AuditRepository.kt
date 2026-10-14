package com.aistudio.dexter.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.aistudio.dexter.data.db.DexterDatabaseHelper
import com.aistudio.dexter.data.model.AuditLog

class AuditRepository(dbHelper: DexterDatabaseHelper) : BaseRepository(dbHelper) {

    constructor(context: Context) : this(DexterDatabaseHelper.getInstance(context))

    fun logAction(action: String, details: String, entityType: String = "SYSTEM"): Long {
        val db = writableDb
        val now = System.currentTimeMillis() / 1000

        val cv = ContentValues().apply {
            put("action", action.trim())
            put("details", details.trim())
            put("timestamp", now)
        }
        return db.insert("audit_logs", null, cv)
    }

    fun listRecentLogs(limit: Int = 100): List<AuditLog> {
        val list = mutableListOf<AuditLog>()
        val db = readableDb
        val cursor = db.rawQuery(
            "SELECT id, action, details, timestamp FROM audit_logs ORDER BY timestamp DESC, id DESC LIMIT ?",
            arrayOf(limit.toString())
        )
        while (cursor.moveToNext()) {
            list.add(mapRowToAudit(cursor))
        }
        cursor.close()
        return list
    }

    private fun mapRowToAudit(cursor: Cursor): AuditLog {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
        val action = cursor.getString(cursor.getColumnIndexOrThrow("action"))
        val details = cursor.getString(cursor.getColumnIndexOrThrow("details"))
        val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
        return AuditLog(
            id = id,
            action = action,
            details = details,
            actionType = action,
            entityType = "SYSTEM",
            message = details,
            timestamp = timestamp,
            createdAt = timestamp
        )
    }
}
