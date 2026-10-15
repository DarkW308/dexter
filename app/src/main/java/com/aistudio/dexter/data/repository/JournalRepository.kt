package com.aistudio.dexter.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.aistudio.dexter.data.db.DexterDatabaseHelper
import com.aistudio.dexter.data.model.JournalEntry
import java.util.Calendar

class JournalRepository(dbHelper: DexterDatabaseHelper) : BaseRepository(dbHelper) {

    constructor(context: Context) : this(DexterDatabaseHelper.getInstance(context))

    fun logJournal(
        content: String,
        title: String = "",
        mood: String = "Calm",
        category: String = "Reflections",
        source: String = "APP",
        actorType: String = "USER"
    ): JournalEntry {
        val db = writableDb
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("title", title.trim())
            put("content", content.trim())
            put("mood", mood.trim())
            put("category", category.trim())
            put("source", source)
            put("created_at", now)
            put("actor_type", actorType)
        }

        val id = db.insertOrThrow("journal_entries", null, values)
        return JournalEntry(
            id = id,
            title = title.trim(),
            content = content.trim(),
            mood = mood.trim(),
            category = category.trim(),
            source = source,
            actorType = actorType,
            createdAt = now
        )
    }

    fun insertNote(
        title: String,
        content: String,
        mood: String = "Calm",
        category: String = "General"
    ): Long {
        val entry = logJournal(
            content = content,
            title = title,
            mood = mood,
            category = category,
            source = "APP",
            actorType = "USER"
        )
        return entry.id
    }

    fun listNotes(limit: Int = 100): List<JournalEntry> {
        val list = mutableListOf<JournalEntry>()
        val db = readableDb
        val cursor = db.rawQuery(
            "SELECT id, title, content, mood, category, source, created_at, actor_type FROM journal_entries ORDER BY created_at DESC LIMIT ?",
            arrayOf(limit.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    JournalEntry(
                        id = it.getLong(0),
                        title = it.getString(1) ?: "",
                        content = it.getString(2) ?: "",
                        mood = it.getString(3) ?: "Calm",
                        category = it.getString(4) ?: "General",
                        source = it.getString(5) ?: "APP",
                        createdAt = it.getLong(6),
                        actorType = it.getString(7) ?: "USER"
                    )
                )
            }
        }
        return list
    }

    fun getWordCountToday(): Int {
        val db = readableDb
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        var totalWords = 0
        val cursor = db.rawQuery(
            "SELECT content FROM journal_entries WHERE created_at >= ?",
            arrayOf(startOfToday.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                val text = it.getString(0) ?: ""
                totalWords += text.trim().split(Regex("\\s+")).count { word -> word.isNotBlank() }
            }
        }
        return totalWords
    }

    fun getWritingStreakDays(): Int {
        val db = readableDb
        val cursor = db.rawQuery(
            "SELECT DISTINCT strftime('%Y-%m-%d', created_at / 1000, 'unixepoch') FROM journal_entries ORDER BY created_at DESC LIMIT 30",
            null
        )
        val dates = mutableListOf<String>()
        cursor.use {
            while (it.moveToNext()) {
                dates.add(it.getString(0) ?: "")
            }
        }
        return dates.size.coerceAtLeast(1)
    }
}
