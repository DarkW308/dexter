package com.aistudio.dexter.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.aistudio.dexter.data.db.DexterDatabaseHelper
import com.aistudio.dexter.data.model.FinanceTransaction
import java.util.Calendar

class FinanceRepository(dbHelper: DexterDatabaseHelper) : BaseRepository(dbHelper) {

    constructor(context: Context) : this(DexterDatabaseHelper.getInstance(context))

    fun logTransaction(
        amountMinor: Long,
        desc: String,
        type: String,
        category: String,
        paymentMode: String = "UPI",
        actorType: String = "USER"
    ): FinanceTransaction {
        val db = writableDb
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("amount_minor", amountMinor)
            put("type", type.uppercase().trim())
            put("category", category.trim())
            put("description", desc.trim())
            put("payment_mode", paymentMode.uppercase().trim())
            put("created_at", now)
            put("actor_type", actorType)
        }

        val id = db.insertOrThrow("finance_transactions", null, values)

        val delta = if (type.uppercase().trim() == "CREDIT") amountMinor else -amountMinor
        db.execSQL("UPDATE finance_accounts SET balance_minor = balance_minor + ? WHERE name = 'Primary Account'", arrayOf(delta))

        return FinanceTransaction(
            id = id,
            amountMinor = amountMinor,
            type = type.uppercase().trim(),
            category = category.trim(),
            description = desc.trim(),
            paymentMode = paymentMode.uppercase().trim(),
            actorType = actorType,
            createdAt = now
        )
    }

    fun listTransactions(limit: Int = 100): List<FinanceTransaction> {
        val list = mutableListOf<FinanceTransaction>()
        val db = readableDb
        val cursor = db.rawQuery(
            "SELECT id, amount_minor, type, category, description, payment_mode, reference_id, notes, created_at, actor_type FROM finance_transactions ORDER BY created_at DESC LIMIT ?",
            arrayOf(limit.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    FinanceTransaction(
                        id = it.getLong(0),
                        amountMinor = it.getLong(1),
                        type = it.getString(2) ?: "DEBIT",
                        category = it.getString(3) ?: "General",
                        description = it.getString(4) ?: "",
                        paymentMode = it.getString(5) ?: "UPI",
                        referenceId = it.getString(6) ?: "",
                        notes = it.getString(7) ?: "",
                        createdAt = it.getLong(8),
                        actorType = it.getString(9) ?: "USER"
                    )
                )
            }
        }
        return list
    }

    fun getNetBalance(): Long {
        val db = readableDb
        var credits = 0L
        var debits = 0L

        val cursor = db.rawQuery("SELECT type, SUM(amount_minor) FROM finance_transactions GROUP BY type", null)
        cursor.use {
            while (it.moveToNext()) {
                val type = it.getString(0) ?: ""
                val sum = it.getLong(1)
                if (type.equals("CREDIT", true)) credits = sum
                else if (type.equals("DEBIT", true)) debits = sum
            }
        }
        return credits - debits
    }

    fun getTotalInflow(): Long {
        val db = readableDb
        var credits = 0L
        val cursor = db.rawQuery("SELECT SUM(amount_minor) FROM finance_transactions WHERE type = 'CREDIT'", null)
        cursor.use {
            if (it.moveToFirst() && !it.isNull(0)) {
                credits = it.getLong(0)
            }
        }
        return credits
    }

    fun getTotalOutflow(): Long {
        val db = readableDb
        var debits = 0L
        val cursor = db.rawQuery("SELECT SUM(amount_minor) FROM finance_transactions WHERE type = 'DEBIT'", null)
        cursor.use {
            if (it.moveToFirst() && !it.isNull(0)) {
                debits = it.getLong(0)
            }
        }
        return debits
    }

    fun summarizeExpenses(): Pair<Long, Long> {
        val db = readableDb
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = cal.timeInMillis

        var todaySpent = 0L
        var monthSpent = 0L

        val cursorToday = db.rawQuery(
            "SELECT SUM(amount_minor) FROM finance_transactions WHERE type = 'DEBIT' AND created_at >= ?",
            arrayOf(startOfToday.toString())
        )
        cursorToday.use {
            if (it.moveToFirst() && !it.isNull(0)) todaySpent = it.getLong(0)
        }

        val cursorMonth = db.rawQuery(
            "SELECT SUM(amount_minor) FROM finance_transactions WHERE type = 'DEBIT' AND created_at >= ?",
            arrayOf(startOfMonth.toString())
        )
        cursorMonth.use {
            if (it.moveToFirst() && !it.isNull(0)) monthSpent = it.getLong(0)
        }

        return Pair(todaySpent, monthSpent)
    }

    fun getCategoryBreakdown(): Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        val db = readableDb
        val cursor = db.rawQuery(
            "SELECT category, SUM(amount_minor) FROM finance_transactions WHERE type = 'DEBIT' GROUP BY category ORDER BY SUM(amount_minor) DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                val cat = it.getString(0) ?: "General"
                val sum = it.getLong(1)
                map[cat] = sum
            }
        }
        return map
    }

    fun getTodayExpenseTotal(): Double {
        return summarizeExpenses().first / 100.0
    }

    fun getRecentTransactions(limit: Int = 10): List<FinanceTransaction> {
        return listTransactions(limit)
    }
}
