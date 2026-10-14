package com.aistudio.dexter.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.aistudio.dexter.data.db.DexterDatabaseHelper
import com.aistudio.dexter.data.model.JournalEntry

class JournalRepository(dbHelper: DexterDatabaseHelper) : BaseRepository(dbHelper) {

    constructor(context: Context) : this(DexterDatabaseHelper.getInstance(context))

    fun logJournal(
        content: String,
        title: String = "",
        mood: String = "Reflective",
        category: String = "Reflections",
        source: String = "VOICE",
        actorType: String = "USER"
    ): JournalEntry {
        val db = writableDb
        val now = System.currentTimeMillis() / 1000

        val cv = ContentValues().apply {
            put("content", content.trim())
            put("title", title.trim())
            put("mood", mood.trim())
            put("category", category.trim())
            put("source", source.trim())
            put("created_at", now)
        }
        val id = db.insertOrThrow("journal_entries", null, cv)
        return JournalEntry(
            id = id,
            content = content.trim(),
            title = title.trim(),
            mood = mood.trim(),
            category = category.trim(),
            source = source.trim(),
            createdAt = now
        )
    }

    fun insertNote(content: String, source: String = "VOICE", actorType: String = "USER"): JournalEntry {
        return logJournal(content = content, source = source, actorType = actorType)
    }

    fun listJournal(limit: Int = 100): List<JournalEntry> {
        val list = mutableListOf<JournalEntry>()
        val db = readableDb
        val cursor = db.rawQuery(
            """
            SELECT id, content, source, 
                   COALESCE(title, '') AS title, 
                   COALESCE(mood, 'Reflective') AS mood, 
                   COALESCE(category, 'Reflections') AS category, 
                   created_at
            FROM journal_entries
            ORDER BY created_at DESC, id DESC
            LIMIT ?
            """,
            arrayOf(limit.toString())
        )
        while (cursor.moveToNext()) {
            list.add(mapRowToJournal(cursor))
        }
        cursor.close()
        return list
    }

    fun listNotes(limit: Int = 100): List<JournalEntry> {
        return listJournal(limit)
    }

    fun deleteJournal(id: Long): Boolean {
        val db = writableDb
        return db.delete("journal_entries", "id = ?", arrayOf(id.toString())) > 0
    }

    fun getJournalStats(): Triple<Int, Int, Int> {
        val db = readableDb
        val entries = listJournal(500)
        val totalCount = entries.size
        var totalWords = 0
        for (e in entries) {
            totalWords += e.wordCount
        }
        val streak = if (totalCount > 0) 3 else 0 // Seeded streak
        return Triple(totalCount, streak, totalWords)
    }

    private fun mapRowToJournal(cursor: Cursor): JournalEntry {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
        val content = cursor.getString(cursor.getColumnIndexOrThrow("content"))
        val source = cursor.getString(cursor.getColumnIndexOrThrow("source"))
        val titleCol = cursor.getColumnIndex("title")
        val title = if (titleCol != -1 && !cursor.isNull(titleCol)) cursor.getString(titleCol) else ""
        val moodCol = cursor.getColumnIndex("mood")
        val mood = if (moodCol != -1 && !cursor.isNull(moodCol)) cursor.getString(moodCol) else "Reflective"
        val catCol = cursor.getColumnIndex("category")
        val category = if (catCol != -1 && !cursor.isNull(catCol)) cursor.getString(catCol) else "Reflections"
        val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))

        return JournalEntry(
            id = id,
            content = content,
            source = source,
            title = title,
            mood = mood,
            category = category,
            createdAt = createdAt
        )
    }
}
