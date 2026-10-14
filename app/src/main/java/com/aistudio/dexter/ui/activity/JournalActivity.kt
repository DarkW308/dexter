package com.aistudio.dexter.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.aistudio.dexter.DexterApp
import com.aistudio.dexter.data.model.JournalEntry
import com.aistudio.dexter.data.repository.DexterRepository
import com.aistudio.dexter.engine.DexterActionExecutor
import com.aistudio.dexter.engine.DexterSpeechManager
import com.aistudio.dexter.ui.screens.JournalScreen
import com.aistudio.dexter.ui.theme.DexterTheme
import com.aistudio.dexter.ui.view.VoiceInteractionDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JournalActivity : ComponentActivity() {

    private lateinit var repository: DexterRepository
    private lateinit var speechManager: DexterSpeechManager
    private lateinit var actionExecutor: DexterActionExecutor

    private val notesState = MutableStateFlow<List<JournalEntry>>(emptyList())
    private val statsState = MutableStateFlow(Triple(0, 0, 0)) // (totalEntries, streak, totalWords)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = (application as DexterApp).repository
        speechManager = DexterSpeechManager(this)
        actionExecutor = DexterActionExecutor(repository, this)

        lifecycleScope.launch {
            DexterRepository.dataRevision.collectLatest {
                loadJournalData()
            }
        }

        setContent {
            DexterTheme {
                val notes = notesState.value
                val (totalEntries, streak, totalWords) = statsState.value

                JournalScreen(
                    notes = notes,
                    totalEntries = totalEntries,
                    writingStreakDays = streak,
                    totalWordsWritten = totalWords,
                    onBack = { finish() },
                    onVoiceClick = { openVoiceDialog() },
                    onAddEntry = { content, title, mood, category ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            repository.logJournal(
                                content = content,
                                title = title,
                                mood = mood,
                                category = category,
                                source = "TEXT"
                            )
                            loadJournalData()
                        }
                    },
                    onDeleteEntry = { id ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            repository.deleteJournal(id)
                            loadJournalData()
                        }
                    }
                )
            }
        }

        loadJournalData()
    }

    private fun loadJournalData() {
        lifecycleScope.launch {
            val notes = withContext(Dispatchers.IO) { repository.listJournal(limit = 100) }
            val stats = withContext(Dispatchers.IO) { repository.getJournalStats() }

            notesState.value = notes
            statsState.value = stats
        }
    }

    private fun openVoiceDialog() {
        val dialog = VoiceInteractionDialog(
            context = this,
            speechManager = speechManager,
            actionExecutor = actionExecutor,
            onActionCompleted = {
                loadJournalData()
            }
        )
        dialog.show()
    }
}
