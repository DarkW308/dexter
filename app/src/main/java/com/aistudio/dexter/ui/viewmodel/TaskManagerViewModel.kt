package com.aistudio.dexter.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aistudio.dexter.data.db.DexterDatabaseHelper
import com.aistudio.dexter.data.model.Priority
import com.aistudio.dexter.data.model.TaskItem
import com.aistudio.dexter.data.model.TaskStatus
import com.aistudio.dexter.data.repository.TaskDataSource
import com.aistudio.dexter.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TaskFilter {
    ALL,
    PENDING,
    COMPLETED
}

data class TaskManagerUiState(
    val tasks: List<TaskItem> = emptyList(),
    val filter: TaskFilter = TaskFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val totalCount: Int = 0,
    val pendingCount: Int = 0,
    val completedCount: Int = 0,
    val userMessage: String? = null
) {
    val filteredTasks: List<TaskItem>
        get() {
            val base = when (filter) {
                TaskFilter.ALL -> tasks
                TaskFilter.PENDING -> tasks.filter { it.status == TaskStatus.PENDING || it.status == TaskStatus.IN_PROGRESS }
                TaskFilter.COMPLETED -> tasks.filter { it.status == TaskStatus.COMPLETED }
            }
            return if (searchQuery.isBlank()) {
                base
            } else {
                base.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                        it.notes.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true)
                }
            }
        }
}

class TaskManagerViewModel(
    private val repository: TaskDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskManagerUiState())
    val uiState: StateFlow<TaskManagerUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    constructor() : this(
        TaskRepository(DexterDatabaseHelper.getInstance(com.aistudio.dexter.DexterApp.instance))
    )

    /**
     * Secondary constructor for Android ViewModelProvider / Activity default factory
     */
    constructor(application: Application) : this(
        TaskRepository(DexterDatabaseHelper.getInstance(application))
    )

    fun loadTasks() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(ioDispatcher) {
            val taskList = repository.getAllTasks(200)
            val counts = repository.getTaskCounts()
            _uiState.update {
                it.copy(
                    tasks = taskList,
                    isLoading = false,
                    totalCount = counts.total,
                    pendingCount = counts.pending,
                    completedCount = counts.completed
                )
            }
        }
    }

    fun addTask(
        title: String,
        priority: Priority = Priority.NORMAL,
        notes: String = "",
        category: String = "General",
        dueTimestamp: Long? = null
    ) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) return

        viewModelScope.launch(ioDispatcher) {
            repository.createTask(
                title = trimmedTitle,
                priority = priority,
                projectId = null,
                dueAt = dueTimestamp,
                scheduledStart = null,
                scheduledEnd = null,
                durationSeconds = null,
                actorType = "USER",
                notes = notes,
                category = category
            )
            _uiState.update { it.copy(userMessage = "Task created: \"$trimmedTitle\"") }
            val taskList = repository.getAllTasks(200)
            val counts = repository.getTaskCounts()
            _uiState.update {
                it.copy(
                    tasks = taskList,
                    totalCount = counts.total,
                    pendingCount = counts.pending,
                    completedCount = counts.completed
                )
            }
        }
    }

    fun toggleTaskCompletion(taskId: Long) {
        viewModelScope.launch(ioDispatcher) {
            val success = repository.toggleTaskCompletion(taskId)
            if (success) {
                val updatedTask = repository.getTaskById(taskId)
                val msg = if (updatedTask?.isCompleted == true) {
                    "Completed: \"${updatedTask.title}\""
                } else {
                    "Reopened: \"${updatedTask?.title ?: "Task #$taskId"}\""
                }
                val taskList = repository.getAllTasks(200)
                val counts = repository.getTaskCounts()
                _uiState.update {
                    it.copy(
                        userMessage = msg,
                        tasks = taskList,
                        totalCount = counts.total,
                        pendingCount = counts.pending,
                        completedCount = counts.completed
                    )
                }
            }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch(ioDispatcher) {
            val task = repository.getTaskById(taskId)
            val success = repository.deleteTask(taskId)
            if (success) {
                val taskList = repository.getAllTasks(200)
                val counts = repository.getTaskCounts()
                _uiState.update {
                    it.copy(
                        userMessage = "Deleted: \"${task?.title ?: "Task #$taskId"}\"",
                        tasks = taskList,
                        totalCount = counts.total,
                        pendingCount = counts.pending,
                        completedCount = counts.completed
                    )
                }
            }
        }
    }

    fun setFilter(filter: TaskFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    class Factory(
        private val repository: TaskDataSource,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskManagerViewModel::class.java)) {
                return TaskManagerViewModel(repository, ioDispatcher) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
