package com.aistudio.dexter.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aistudio.dexter.DexterApp
import com.aistudio.dexter.MainActivity
import com.aistudio.dexter.R
import com.aistudio.dexter.data.repository.DexterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SegmentDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SEGMENT_NUMBER = "extra_segment_number"
    }

    private lateinit var repository: DexterRepository
    private var segmentNumber: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_segment_detail)

        repository = (application as DexterApp).repository
        segmentNumber = intent.getIntExtra(EXTRA_SEGMENT_NUMBER, 1)

        val btnBack: ImageView = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        val btnQuickAction: Button = findViewById(R.id.btnQuickAction)
        when (segmentNumber) {
            8 -> btnQuickAction.text = "Open Bank Statement Ledger"
            10 -> btnQuickAction.text = "Open Personal Diary"
            1, 2 -> btnQuickAction.text = "Open Tasks Page"
            else -> btnQuickAction.text = "Voice Action"
        }

        btnQuickAction.setOnClickListener {
            when (segmentNumber) {
                8 -> startActivity(Intent(this, FinanceActivity::class.java))
                10 -> startActivity(Intent(this, JournalActivity::class.java))
                1, 2 -> startActivity(Intent(this, TasksActivity::class.java))
                else -> {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("action_open_voice", true)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }

        loadSegmentData()
    }

    private fun loadSegmentData() {
        lifecycleScope.launch {
            val segments = withContext(Dispatchers.IO) { repository.getAllSegments() }
            val seg = segments.find { it.segmentNumber == segmentNumber } ?: return@launch
            val rows = withContext(Dispatchers.IO) { repository.getSegmentDetails(segmentNumber) }

            findViewById<TextView>(R.id.tvDetailTitle).text = "${seg.segmentNumber}. ${seg.title}"
            findViewById<TextView>(R.id.tvDetailSubtitle).text = "${seg.category} · ${seg.allTables.size} Tables"
            findViewById<TextView>(R.id.tvItemCountBadge).text = "${seg.itemCount} items"

            findViewById<TextView>(R.id.tvHeroIcon).text = seg.icon
            findViewById<TextView>(R.id.tvHeroName).text = "${seg.segmentNumber}. ${seg.title}"
            findViewById<TextView>(R.id.tvHeroCategory).text = "${seg.category} Architecture Domain"
            findViewById<TextView>(R.id.tvHeroDescription).text = seg.description
            findViewById<TextView>(R.id.tvHeroTables).text = "SQLite Tables: ${seg.allTables.joinToString(", ")}"

            val rowsText = if (rows.isNotEmpty()) {
                rows.joinToString("\n\n")
            } else {
                "No rows currently recorded in this segment's tables."
            }
            findViewById<TextView>(R.id.tvLiveRowsContent).text = rowsText
        }
    }
}
