package com.aistudio.dexter

import android.app.Application
import com.aistudio.dexter.data.db.DexterDatabaseHelper

class DexterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize SQLite database eagerly to run WAL mode and pre-seed initial records
        DexterDatabaseHelper.getInstance(this).writableDatabase
    }
}
