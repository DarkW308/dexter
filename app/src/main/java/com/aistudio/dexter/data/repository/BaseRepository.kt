package com.aistudio.dexter.data.repository

import android.database.sqlite.SQLiteDatabase
import com.aistudio.dexter.data.db.DexterDatabaseHelper

abstract class BaseRepository(protected val dbHelper: DexterDatabaseHelper) {
    protected val readableDb: SQLiteDatabase
        get() = dbHelper.readableDatabase

    protected val writableDb: SQLiteDatabase
        get() = dbHelper.writableDatabase
}
