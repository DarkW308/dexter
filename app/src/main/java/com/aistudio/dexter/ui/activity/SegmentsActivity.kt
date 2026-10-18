package com.aistudio.dexter.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aistudio.dexter.DexterApp
import com.aistudio.dexter.R
import com.aistudio.dexter.data.model.SegmentInfo
import com.aistudio.dexter.data.repository.DexterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SegmentsActivity : AppCompatActivity() {

    private lateinit var repository: DexterRepository
    private lateinit var adapter: SegmentsAdapter
    private var allSegments: List<SegmentInfo> = emptyList()
    private var currentFilter: String = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_segments)

        repository = (application as DexterApp).repository

        val btnBack: ImageView = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        val rvSegments: RecyclerView = findViewById(R.id.rvSegments)
        rvSegments.layoutManager = LinearLayoutManager(this)
        adapter = SegmentsAdapter(emptyList()) { selectedSegment ->
            when (selectedSegment.segmentNumber) {
                1 -> startActivity(Intent(this, TasksActivity::class.java))
                4 -> startActivity(Intent(this, FinanceActivity::class.java))
                5 -> startActivity(Intent(this, JournalActivity::class.java))
                else -> {
                    val intent = Intent(this, SegmentDetailActivity::class.java).apply {
                        putExtra(SegmentDetailActivity.EXTRA_SEGMENT_NUMBER, selectedSegment.segmentNumber)
                    }
                    startActivity(intent)
                }
            }
        }
        rvSegments.adapter = adapter

        setupFilterChips()
        loadSegments()
    }

    override fun onResume() {
        super.onResume()
        loadSegments()
    }

    private fun setupFilterChips() {
        val chipAll: TextView = findViewById(R.id.chipAll)
        val chipProductivity: TextView = findViewById(R.id.chipProductivity)
        val chipLife: TextView = findViewById(R.id.chipLife)
        val chipIntelligence: TextView = findViewById(R.id.chipIntelligence)
        val chipSystem: TextView = findViewById(R.id.chipSystem)

        val chips = listOf(
            chipAll to "All",
            chipProductivity to "Productivity",
            chipLife to "Life Tracking",
            chipIntelligence to "Intelligence",
            chipSystem to "System & Security"
        )

        chips.forEach { (view, category) ->
            view.setOnClickListener {
                currentFilter = category
                chips.forEach { (v, cat) ->
                    if (cat == currentFilter) {
                        v.setTextColor(ContextCompat.getColor(this, R.color.dexter_on_background))
                        v.setBackgroundResource(R.drawable.bg_surface_rounded)
                    } else {
                        v.setTextColor(ContextCompat.getColor(this, R.color.dexter_on_surface_variant))
                        v.setBackgroundResource(R.drawable.bg_chip)
                    }
                }
                applyFilter()
            }
        }
    }

    private fun loadSegments() {
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) { repository.getAllSegments() }
            allSegments = list
            val dbFile = getDatabasePath("dexter.db")
            val dbSizeKb = if (dbFile.exists()) dbFile.length() / 1024 else 0
            findViewById<TextView>(R.id.tvTotalDbSize).text = "WAL Mode · ${dbSizeKb} KB"

            // Update chip counts
            findViewById<TextView>(R.id.chipAll).text = "All (${allSegments.size})"
            findViewById<TextView>(R.id.chipProductivity).text = "Productivity (${allSegments.count { it.category == "Productivity" || it.category == "Finance" }})"
            findViewById<TextView>(R.id.chipLife).text = "Life & Health (${allSegments.count { it.category == "Life Tracking" || it.category == "Reflection" }})"
            findViewById<TextView>(R.id.chipIntelligence).text = "Intelligence (${allSegments.count { it.category == "Intelligence" }})"
            findViewById<TextView>(R.id.chipSystem).text = "System & Audit (${allSegments.count { it.category == "System & Security" }})"

            applyFilter()
        }
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            "All" -> allSegments
            "Productivity" -> allSegments.filter { it.category == "Productivity" || it.category == "Finance" }
            "Life Tracking" -> allSegments.filter { it.category == "Life Tracking" || it.category == "Reflection" }
            "Intelligence" -> allSegments.filter { it.category == "Intelligence" }
            "System & Security" -> allSegments.filter { it.category == "System & Security" }
            else -> allSegments.filter { it.category == currentFilter }
        }
        adapter.updateList(filtered)
    }
}
