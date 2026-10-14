package com.aistudio.dexter

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.aistudio.dexter.data.db.DexterDatabaseHelper
import com.aistudio.dexter.data.repository.DexterRepository

class DexterApp : Application() {

    companion object {
        private const val TAG = "DexterApp"
        lateinit var instance: DexterApp
            private set
    }

    val repository: DexterRepository by lazy {
        DexterRepository(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Enable VectorDrawable backward compatibility for Android 5.0 (API 21+)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        // Setup global uncaught exception logging
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "FATAL EXCEPTION in thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        Log.i(TAG, "Initializing Dexter Application (API Level: ${android.os.Build.VERSION.SDK_INT})...")

        // Initialize SQLite database eagerly with WAL mode and PRAGMA foreign_keys = ON
        try {
            DexterDatabaseHelper.getInstance(this).writableDatabase
            Log.i(TAG, "Dexter SQLite Database initialized with WAL mode and foreign keys.")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SQLite database", e)
        }
        // Background voice service is safely started from MainActivity after permission check
    }
}
