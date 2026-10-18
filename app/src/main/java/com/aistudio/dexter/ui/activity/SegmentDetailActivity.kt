package com.aistudio.dexter.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aistudio.dexter.DexterApp
import com.aistudio.dexter.data.repository.DexterRepository
import com.aistudio.dexter.ui.screens.SegmentDetailScreen
import com.aistudio.dexter.ui.theme.DexterTheme

class SegmentDetailActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SEGMENT_NUMBER = "extra_segment_number"
    }

    private lateinit var repository: DexterRepository
    private var segmentNumber: Int = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = (application as DexterApp).repository
        segmentNumber = intent.getIntExtra(EXTRA_SEGMENT_NUMBER, 2)

        setContent {
            DexterTheme {
                SegmentDetailScreen(
                    segmentNumber = segmentNumber,
                    repository = repository,
                    onBack = { finish() },
                    onOpenTasks = { startActivity(Intent(this, TasksActivity::class.java)) },
                    onOpenFinance = { startActivity(Intent(this, FinanceActivity::class.java)) },
                    onOpenJournal = { startActivity(Intent(this, JournalActivity::class.java)) }
                )
            }
        }
    }
}
