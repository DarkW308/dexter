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
        type: String = "DEBIT",
        category: String = "General",
        paymentMode: String = "UPI",
        referenceId: String = "",
        actorType: String = "USER"
    ): FinanceTransaction {
        val db = writableDb
        val now = System.currentTimeMillis() / 1000
        val ref = if (referenceId.isNotBlank()) referenceId else "TXN-${(100000..999999).random()}"

        val cv = ContentValues().apply {
            put("account_id", 1)
            put("amount_minor", amountMinor)
            put("description", desc.trim())
            put("type", type.uppercase())
            put("category", category.trim())
            put("payment_mode", paymentMode.trim())
            put("reference_id", ref)
            put("created_at", now)
        }
        val id = db.insertOrThrow("finance_transactions", null, cv)
        return FinanceTransaction(
            id = id,
            accountId = 1,
            amountMinor = amountMinor,
            description = desc.trim(),
            type = type.uppercase(),
            category = category.trim(),
            paymentMode = paymentMode.trim(),
            referenceId = ref,
            createdAt = now
        )
    }

    fun logExpense(
        amountMinor: Long,
        desc: String,
        category: String = "General",
        actorType: String = "USER"
    ): FinanceTransaction {
        return logTransaction(
            amountMinor = amountMinor,
            desc = desc,
            type = "DEBIT",
            category = category,
            paymentMode = "UPI",
            actorType = actorType
        )
    }

    fun listTransactions(limit: Int = 100): List<FinanceTransaction> {
        val list = mutableListOf<FinanceTransaction>()
        val db = readableDb
        val cursor = db.rawQuery(
            """
            SELECT id, account_id, amount_minor, description, type, category, 
                   COALESCE(payment_mode, 'UPI') AS payment_mode, 
                   COALESCE(reference_id, '') AS reference_id, 
                   created_at
            FROM finance_transactions
            ORDER BY created_at DESC, id DESC
            LIMIT ?
            """,
            arrayOf(limit.toString())
        )
        while (cursor.moveToNext()) {
            list.add(mapRowToTransaction(cursor))
        }
        cursor.close()
        return list
    }

    fun deleteTransaction(id: Long): Boolean {
        val db = writableDb
        return db.delete("finance_transactions", "id = ?", arrayOf(id.toString())) > 0
    }

    fun summarizeExpenses(): Pair<Long, Long> {
        val db = readableDb
        val cal = Calendar.getInstance()

        // Start of today in seconds
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis / 1000

        // Start of month in seconds
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = cal.timeInMillis / 1000

        var todaySpent = 0L
        var monthSpent = 0L

        val cursor = db.rawQuery(
            """
            SELECT 
                SUM(CASE WHEN created_at >= ? AND type = 'DEBIT' THEN amount_minor ELSE 0 END) AS today_sum,
                SUM(CASE WHEN created_at >= ? AND type = 'DEBIT' THEN amount_minor ELSE 0 END) AS month_sum
            FROM finance_transactions
            """,
            arrayOf(startOfToday.toString(), startOfMonth.toString())
        )

        if (cursor.moveToFirst()) {
            todaySpent = cursor.getLong(0)
            monthSpent = cursor.getLong(1)
        }
        cursor.close()

        return Pair(todaySpent, monthSpent)
    }

    fun getFinanceStatement(): Triple<Long, Long, Long> {
        val db = readableDb
        var totalCredits = 0L
        var totalDebits = 0L

        val cursor = db.rawQuery(
            """
            SELECT 
                SUM(CASE WHEN type = 'CREDIT' THEN amount_minor ELSE 0 END) AS total_credits,
                SUM(CASE WHEN type = 'DEBIT' THEN amount_minor ELSE 0 END) AS total_debits
            FROM finance_transactions
            """,
            null
        )
        if (cursor.moveToFirst()) {
            totalCredits = cursor.getLong(0)
            totalDebits = cursor.getLong(1)
        }
        cursor.close()

        // Starting balance seeded at 45,000.00 INR (4,500,000 minor)
        val netBalance = 4500000L + totalCredits - totalDebits
        return Triple(netBalance, totalCredits, totalDebits)
    }

    fun getFinanceCategoryBreakdown(): Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        val db = readableDb
        val cursor = db.rawQuery(
            """
            SELECT category, SUM(amount_minor) as total
            FROM finance_transactions
            WHERE type = 'DEBIT'
            GROUP BY category
            ORDER BY total DESC
            """,
            null
        )
        while (cursor.moveToNext()) {
            val cat = cursor.getString(0) ?: "General"
            val total = cursor.getLong(1)
            map[cat] = total
        }
        cursor.close()
        return map
    }

    private fun mapRowToTransaction(cursor: Cursor): FinanceTransaction {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
        val accId = cursor.getLong(cursor.getColumnIndexOrThrow("account_id"))
        val amount = cursor.getLong(cursor.getColumnIndexOrThrow("amount_minor"))
        val desc = cursor.getString(cursor.getColumnIndexOrThrow("description"))
        val type = cursor.getString(cursor.getColumnIndexOrThrow("type"))
        val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
        val modeCol = cursor.getColumnIndex("payment_mode")
        val paymentMode = if (modeCol != -1 && !cursor.isNull(modeCol)) cursor.getString(modeCol) else "UPI"
        val refCol = cursor.getColumnIndex("reference_id")
        val ref = if (refCol != -1 && !cursor.isNull(refCol)) cursor.getString(refCol) else ""
        val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))

        return FinanceTransaction(
            id = id,
            accountId = accId,
            amountMinor = amount,
            description = desc,
            type = type,
            category = category,
            paymentMode = paymentMode,
            referenceId = ref,
            createdAt = createdAt
        )
    }
}
