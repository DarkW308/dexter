package com.aistudio.dexter.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.dexter.data.model.*
import com.aistudio.dexter.data.repository.DexterRepository
import com.aistudio.dexter.engine.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DexterViewModel(application: Application) : AndroidViewModel(application) {

    val repository = DexterRepository(application)
    val speechManager = DexterSpeechManager(application)
    val actionExecutor = DexterActionExecutor(repository, application)

    // Navigation Tab (0: Home, 1: Tasks, 2: Finance, 3: Journal, 4: More)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Data States
    private val _pendingTasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val pendingTasks: StateFlow<List<TaskItem>> = _pendingTasks.asStateFlow()

    private val _completedTasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val completedTasks: StateFlow<List<TaskItem>> = _completedTasks.asStateFlow()

    private val _transactions = MutableStateFlow<List<FinanceTransaction>>(emptyList())
    val transactions: StateFlow<List<FinanceTransaction>> = _transactions.asStateFlow()

    private val _todaySpentMinor = MutableStateFlow(0L)
    val todaySpentMinor: StateFlow<Long> = _todaySpentMinor.asStateFlow()

    private val _monthSpentMinor = MutableStateFlow(0L)
    val monthSpentMinor: StateFlow<Long> = _monthSpentMinor.asStateFlow()

    private val _journalNotes = MutableStateFlow<List<JournalEntry>>(emptyList())
    val journalNotes: StateFlow<List<JournalEntry>> = _journalNotes.asStateFlow()

    private val _goals = MutableStateFlow<List<GoalItem>>(emptyList())
    val goals: StateFlow<List<GoalItem>> = _goals.asStateFlow()

    private val _projects = MutableStateFlow<List<ProjectItem>>(emptyList())
    val projects: StateFlow<List<ProjectItem>> = _projects.asStateFlow()

    private val _alarms = MutableStateFlow<List<AlarmItem>>(emptyList())
    val alarms: StateFlow<List<AlarmItem>> = _alarms.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

    private val _dailyBriefing = MutableStateFlow<DailyBriefing?>(null)
    val dailyBriefing: StateFlow<DailyBriefing?> = _dailyBriefing.asStateFlow()

    // Voice Modal & Interactions
    private val _isVoiceModalOpen = MutableStateFlow(false)
    val isVoiceModalOpen: StateFlow<Boolean> = _isVoiceModalOpen.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<ParsedIntent?>(null)
    val pendingConfirmation: StateFlow<ParsedIntent?> = _pendingConfirmation.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        speechManager.onSpeechResult = { recognizedText ->
            processUserPhrase(recognizedText)
        }

        // Reactively reload on data changes
        viewModelScope.launch {
            repository.dataRevision.collect {
                refreshAllData()
            }
        }

        refreshAllData()
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun openVoiceModal() {
        _isVoiceModalOpen.value = true
        speechManager.startListening()
    }

    fun dismissVoiceModal() {
        _isVoiceModalOpen.value = false
        _pendingConfirmation.value = null
        speechManager.stopListening()
        speechManager.stopSpeaking()
        speechManager.setVisualState(OrbVisualState.IDLE)
    }

    fun processUserPhrase(phrase: String) {
        viewModelScope.launch {
            speechManager.setVisualState(OrbVisualState.PROCESSING)
            val parsed = DexterIntentRouter.parse(phrase)

            if (parsed.requiresConfirmation && !parsed.isNegationOrClarification) {
                _pendingConfirmation.value = parsed
                speechManager.setVisualState(OrbVisualState.IDLE)
                speechManager.speak(parsed.confirmationPrompt ?: "Confirm action?")
            } else {
                executeIntent(parsed)
            }
        }
    }

    fun confirmPendingAction(intent: ParsedIntent) {
        _pendingConfirmation.value = null
        viewModelScope.launch {
            executeIntent(intent)
        }
    }

    fun cancelPendingAction() {
        _pendingConfirmation.value = null
        speechManager.speak("Cancelled.")
        speechManager.setVisualState(OrbVisualState.IDLE)
    }

    private suspend fun executeIntent(intent: ParsedIntent) {
        val result = actionExecutor.execute(intent)
        if (result.success) {
            speechManager.speak(result.message)
            _snackbarMessage.value = result.message
        } else {
            speechManager.setVisualState(OrbVisualState.ERROR)
            speechManager.speak(result.message)
            _snackbarMessage.value = result.message
        }
    }

    fun playBriefing() {
        viewModelScope.launch {
            val briefing = repository.getDailyBriefing()
            _dailyBriefing.value = briefing
            speechManager.speak(briefing.summaryText)
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // Direct UI Action Helpers
    fun completeTask(taskId: Long) {
        viewModelScope.launch {
            val success = repository.completeTask(taskId)
            if (success) {
                _snackbarMessage.value = "Task #$taskId completed."
            }
        }
    }

    fun cancelTask(taskId: Long) {
        viewModelScope.launch {
            val success = repository.cancelTask(taskId)
            if (success) {
                _snackbarMessage.value = "Task #$taskId cancelled."
            }
        }
    }

    fun createTask(title: String, priority: Priority) {
        viewModelScope.launch {
            val task = repository.createTask(title, priority)
            _snackbarMessage.value = "Created task #${task.id}: '${task.title}'"
        }
    }

    fun logExpense(amountMinor: Long, desc: String, category: String) {
        viewModelScope.launch {
            val tx = repository.logExpense(amountMinor, desc, category)
            _snackbarMessage.value = "Logged ₹${tx.amountRupees} for ${tx.description}"
        }
    }

    fun addJournalNote(content: String) {
        viewModelScope.launch {
            repository.logJournal(content, source = "TEXT")
            _snackbarMessage.value = "Note saved."
        }
    }

    fun cancelAllAlarms() {
        viewModelScope.launch {
            val count = repository.cancelAlarms()
            _snackbarMessage.value = if (count > 0) "Cancelled $count alarms." else "No active alarms."
        }
    }

    private fun refreshAllData() {
        viewModelScope.launch {
            _pendingTasks.value = repository.listPendingTasks()
            _completedTasks.value = repository.listCompletedTasks()
            _transactions.value = repository.listTransactions()
            val (today, month) = repository.summarizeExpenses()
            _todaySpentMinor.value = today
            _monthSpentMinor.value = month
            _journalNotes.value = repository.listJournal()
            _goals.value = repository.listGoals()
            _projects.value = repository.listProjects()
            _alarms.value = repository.listAlarms()
            _auditLogs.value = repository.getAuditLogs()
            _dailyBriefing.value = repository.getDailyBriefing()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.shutdown()
    }
}
