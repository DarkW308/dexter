package com.aistudio.dexter.engine

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * Adaptive Multi-Device Hardware Scaling Profile.
 *
 * Implements hardware-aware thread allocation and memory eviction rules:
 * - Vivo V11 Floor (MediaTek Helio P60, API 27, <= 6GB RAM): 2 performance threads.
 * - Modern 8-core Flagships (API >= 31, > 6GB RAM): Scales up to 4 performance threads.
 * - Dynamic Gemma 3 RAM watchdog:
 *     - Low/moderate RAM (< 1.5 GB free or lowMemory flag): 45-second aggressive eviction.
 *     - High RAM (>= 2.5 GB free): 180-second keep-warm cache for instant follow-up dialogue.
 */
object DeviceHardwareProfile {

    fun getOptimalThreadCount(context: Context?): Int {
        val availableCores = Runtime.getRuntime().availableProcessors()

        // Older Android or Helio P60 mid-tier (API < 31): Pin to 2 Cortex-A73 big cores
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return 2
        }

        val totalRamBytes = getTotalRamBytes(context)
        val ramGb = totalRamBytes / (1024L * 1024L * 1024L)

        // Only scale above 2 threads if device is modern (API 31+) AND has > 6GB physical RAM
        return if (ramGb > 6) {
            (availableCores / 2).coerceIn(2, 4)
        } else {
            2
        }
    }

    fun getTotalRamBytes(context: Context?): Long {
        if (context == null) return 4L * 1024 * 1024 * 1024
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            memInfo.totalMem
        } catch (_: Exception) {
            4L * 1024 * 1024 * 1024
        }
    }

    fun getAvailableRamBytes(context: Context?): Long {
        if (context == null) return 2L * 1024 * 1024 * 1024
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            memInfo.availMem
        } catch (_: Exception) {
            2L * 1024 * 1024 * 1024
        }
    }

    fun getAdaptiveGemmaTimeoutMs(context: Context?): Long {
        if (context == null) return 45_000L
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            val freeBytes = memInfo?.availMem ?: (2L * 1024 * 1024 * 1024)
            val isLowMem = memInfo?.lowMemory == true

            if (!isLowMem && freeBytes >= 2500L * 1024 * 1024) {
                180_000L // 3 minutes warm cache on generous RAM
            } else {
                45_000L  // 45 seconds aggressive eviction on Helio P60 / constrained RAM
            }
        } catch (_: Exception) {
            45_000L
        }
    }
}
