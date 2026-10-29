package com.aistudio.dexter.data.backup

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.aistudio.dexter.data.db.DexterDatabaseHelper
import com.aistudio.dexter.data.preferences.UserPreferencesRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupMetadata(
    val version: Int,
    val appName: String,
    val timestamp: Long,
    val totalRecords: Int,
    val tableCounts: Map<String, Int>
)

data class ValidationResult(
    val isValid: Boolean,
    val metadata: BackupMetadata? = null,
    val errorMessage: String? = null
)

data class RestoreResult(
    val isSuccess: Boolean,
    val restoredCount: Int,
    val message: String
)

object DexterBackupManager {

    private const val TAG = "DexterBackupManager"
    const val CURRENT_BACKUP_VERSION = 1
    const val BACKUP_FILE_PREFIX = "dexter_backup_"

    val ALL_EXPORTABLE_TABLES = listOf(
        "tasks",
        "task_occurrences",
        "projects",
        "project_milestones",
        "goals",
        "finance_transactions",
        "finance_accounts",
        "journal_entries",
        "alarms",
        "schedule_blocks",
        "relationship_people",
        "relationship_interactions",
        "wellbeing_hydration",
        "wellbeing_sleep",
        "wellbeing_mood",
        "wellbeing_habits",
        "knowledge_documents",
        "capture_events",
        "intake_schedules",
        "attention_events",
        "briefing_runs",
        "system_rules",
        "audit"
    )

    val DOMAIN_MAP = mapOf(
        "Tasks & Execution" to listOf("tasks", "task_occurrences"),
        "Projects & Milestones" to listOf("projects", "project_milestones"),
        "Goals & Targets" to listOf("goals"),
        "Personal Finance" to listOf("finance_transactions"),
        "Personal Diary & Notes" to listOf("journal_entries"),
        "Alarms & Reminders" to listOf("alarms"),
        "Time & Schedule" to listOf("schedule_blocks"),
        "People & Relationships" to listOf("relationship_people", "relationship_interactions"),
        "Health & Wellbeing" to listOf("wellbeing_hydration", "wellbeing_sleep", "wellbeing_mood"),
        "Habits & Streaks" to listOf("wellbeing_habits"),
        "Knowledge Vault" to listOf("knowledge_documents"),
        "Raw Intake & Hub" to listOf("capture_events"),
        "Attention Feed" to listOf("attention_events"),
        "Daily Briefings" to listOf("briefing_runs"),
        "Safety Rules" to listOf("system_rules"),
        "Audit Ledger" to listOf("audit")
    )

    fun exportDatabaseToJson(dbHelper: DexterDatabaseHelper): String {
        val root = JSONObject()
        val db = dbHelper.readableDatabase

        val tableCounts = mutableMapOf<String, Int>()
        var totalRecords = 0

        val dataObj = JSONObject()

        for (table in ALL_EXPORTABLE_TABLES) {
            val rowsArray = JSONArray()
            try {
                val cursor = db.rawQuery("SELECT * FROM $table", null)
                cursor.use {
                    val columnCount = it.columnCount
                    val colNames = it.columnNames
                    while (it.moveToNext()) {
                        val rowObj = JSONObject()
                        for (i in 0 until columnCount) {
                            val col = colNames[i]
                            when (it.getType(i)) {
                                Cursor.FIELD_TYPE_INTEGER -> rowObj.put(col, it.getLong(i))
                                Cursor.FIELD_TYPE_FLOAT -> rowObj.put(col, it.getDouble(i))
                                Cursor.FIELD_TYPE_STRING -> rowObj.put(col, it.getString(i))
                                Cursor.FIELD_TYPE_NULL -> rowObj.put(col, JSONObject.NULL)
                                Cursor.FIELD_TYPE_BLOB -> {
                                    // Base64 encode blob if needed
                                    val blob = it.getBlob(i)
                                    rowObj.put(col, android.util.Base64.encodeToString(blob, android.util.Base64.NO_WRAP))
                                }
                            }
                        }
                        rowsArray.put(rowObj)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Notice exporting table $table: ${e.message}")
            }
            dataObj.put(table, rowsArray)
            tableCounts[table] = rowsArray.length()
            totalRecords += rowsArray.length()
        }

        val metaObj = JSONObject()
        metaObj.put("version", CURRENT_BACKUP_VERSION)
        metaObj.put("appName", "Dexter")
        metaObj.put("timestamp", System.currentTimeMillis())
        metaObj.put("totalRecords", totalRecords)

        val countsObj = JSONObject()
        for ((k, v) in tableCounts) {
            countsObj.put(k, v)
        }
        metaObj.put("tableCounts", countsObj)

        root.put("metadata", metaObj)
        root.put("data", dataObj)

        return root.toString(2)
    }

    fun exportBackupToFile(context: Context, dbHelper: DexterDatabaseHelper): File {
        val json = exportDatabaseToJson(dbHelper)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "${BACKUP_FILE_PREFIX}${timeStamp}.dexter.json"

        val exportDir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
        val backupFile = File(exportDir, fileName)

        FileOutputStream(backupFile).use { fos ->
            fos.write(json.toByteArray(Charsets.UTF_8))
            fos.flush()
        }
        return backupFile
    }

    fun validateBackupJson(jsonString: String): ValidationResult {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("metadata") || !root.has("data")) {
                return ValidationResult(false, null, "Invalid backup format: Missing metadata or data sections")
            }
            val meta = root.getJSONObject("metadata")
            val version = meta.optInt("version", 1)
            val appName = meta.optString("appName", "Dexter")
            val timestamp = meta.optLong("timestamp", System.currentTimeMillis())
            val totalRecords = meta.optInt("totalRecords", 0)

            val tableCounts = mutableMapOf<String, Int>()
            if (meta.has("tableCounts")) {
                val countsObj = meta.getJSONObject("tableCounts")
                val keys = countsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    tableCounts[key] = countsObj.optInt(key, 0)
                }
            }

            ValidationResult(
                isValid = true,
                metadata = BackupMetadata(version, appName, timestamp, totalRecords, tableCounts)
            )
        } catch (e: Exception) {
            ValidationResult(false, null, "Malformed JSON: ${e.localizedMessage}")
        }
    }

    fun restoreBackupFromJson(
        dbHelper: DexterDatabaseHelper,
        jsonString: String,
        clearExisting: Boolean
    ): RestoreResult {
        val validation = validateBackupJson(jsonString)
        if (!validation.isValid) {
            return RestoreResult(false, 0, validation.errorMessage ?: "Invalid backup file")
        }

        val db = dbHelper.writableDatabase
        var restoredRecords = 0

        db.beginTransaction()
        try {
            val root = JSONObject(jsonString)
            val dataObj = root.getJSONObject("data")

            if (clearExisting) {
                // Clear tables in reverse dependency order
                for (table in ALL_EXPORTABLE_TABLES) {
                    try {
                        db.delete(table, null, null)
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice clearing table $table: ${e.message}")
                    }
                }
            }

            for (table in ALL_EXPORTABLE_TABLES) {
                if (!dataObj.has(table)) continue
                val rowsArray = dataObj.getJSONArray(table)
                for (i in 0 until rowsArray.length()) {
                    val rowObj = rowsArray.getJSONObject(i)
                    val values = ContentValues()

                    val keys = rowObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (rowObj.isNull(key)) {
                            values.putNull(key)
                        } else {
                            val v = rowObj.get(key)
                            when (v) {
                                is Long -> values.put(key, v)
                                is Int -> values.put(key, v.toLong())
                                is Double -> values.put(key, v)
                                is Float -> values.put(key, v.toDouble())
                                is Boolean -> values.put(key, if (v) 1 else 0)
                                is String -> values.put(key, v)
                                else -> values.put(key, v.toString())
                            }
                        }
                    }

                    val insertId = db.insertWithOnConflict(
                        table,
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE
                    )
                    if (insertId != -1L) {
                        restoredRecords++
                    }
                }
            }

            // Ensure baseline primary finance account exists if cleared
            db.execSQL("INSERT OR IGNORE INTO finance_accounts (id, name, balance_minor, currency) VALUES (1, 'Primary Account', 4500000, 'INR')")

            db.setTransactionSuccessful()
            return RestoreResult(true, restoredRecords, "Successfully restored $restoredRecords records.")
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup: ${e.message}", e)
            return RestoreResult(false, 0, "Restore failed: ${e.localizedMessage}")
        } finally {
            db.endTransaction()
        }
    }

    fun purgeDomainTables(dbHelper: DexterDatabaseHelper, tables: List<String>): Int {
        val db = dbHelper.writableDatabase
        var deletedCount = 0
        db.beginTransaction()
        try {
            for (table in tables) {
                if (ALL_EXPORTABLE_TABLES.contains(table)) {
                    deletedCount += db.delete(table, null, null)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return deletedCount
    }

    fun factoryReset(context: Context, dbHelper: DexterDatabaseHelper): Boolean {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            for (table in ALL_EXPORTABLE_TABLES) {
                try {
                    db.delete(table, null, null)
                } catch (e: Exception) {
                    Log.w(TAG, "Notice clearing table during reset: ${e.message}")
                }
            }
            // Seed baseline account
            db.execSQL("INSERT OR IGNORE INTO finance_accounts (id, name, balance_minor, currency) VALUES (1, 'Primary Account', 4500000, 'INR')")
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "Error during factory reset: ${e.message}")
            return false
        } finally {
            db.endTransaction()
        }

        // Reset user preferences
        UserPreferencesRepository.getInstance(context).resetToDefaults()
        return true
    }

    fun getTableRecordCounts(dbHelper: DexterDatabaseHelper): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        val db = dbHelper.readableDatabase
        for (table in ALL_EXPORTABLE_TABLES) {
            try {
                val c = db.rawQuery("SELECT COUNT(*) FROM $table", null)
                c.use {
                    if (it.moveToFirst()) {
                        map[table] = it.getInt(0)
                    }
                }
            } catch (e: Exception) {
                map[table] = 0
            }
        }
        return map
    }

    fun getDomainCounts(dbHelper: DexterDatabaseHelper): Map<String, Int> {
        val tableCounts = getTableRecordCounts(dbHelper)
        val domainCounts = mutableMapOf<String, Int>()
        for ((domain, tables) in DOMAIN_MAP) {
            domainCounts[domain] = tables.sumOf { tableCounts[it] ?: 0 }
        }
        return domainCounts
    }
}
