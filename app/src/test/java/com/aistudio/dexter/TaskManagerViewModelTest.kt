package com.aistudio.dexter

import com.aistudio.dexter.data.model.Priority
import com.aistudio.dexter.data.model.TaskCounts
import com.aistudio.dexter.data.model.TaskItem
import com.aistudio.dexter.data.model.TaskStatus
import com.aistudio.dexter.data.repository.TaskDataSource
import com.aistudio.dexter.ui.viewmodel.TaskFilter
import com.aistudio.dexter.ui.viewmodel.TaskManagerViewModel
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTaskRepository : TaskDataSource {
    val taskList = mutableListOf<TaskItem>()
    private var nextId = 1L

    override fun getTaskById(taskId: Long): TaskItem? {
        return taskList.find { it.id == taskId }
    }

    override fun createTask(
        title: String,
        priority: Priority,
        projectId: Long?,
        dueAt: Long?,
        scheduledStart: Long?,
        scheduledEnd: Long?,
        durationSeconds: Long?,
        actorType: String,
        notes: String,
        category: String
    ): TaskItem {
        val task = TaskItem(
            id = nextId++,
            title = title,
            priority = priority,
            projectId = projectId,
            status = TaskStatus.PENDING,
            dueAt = dueAt,
            dueTimestamp = dueAt,
            scheduledStart = scheduledStart,
            scheduledEnd = scheduledEnd,
            estimatedDurationSeconds = durationSeconds,
            durationSeconds = durationSeconds,
            notes = notes,
            category = category,
            createdAt = System.currentTimeMillis()
        )
        taskList.add(task)
        return task
    }

    override fun completeTask(taskId: Long, completionSource: String, actorType: String): Boolean {
        val index = taskList.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            val old = taskList[index]
            taskList[index] = old.copy(status = TaskStatus.COMPLETED, completionSource = completionSource)
            return true
        }
        return false
    }

    override fun markTaskPending(taskId: Long): Boolean {
        val index = taskList.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            val old = taskList[index]
            taskList[index] = old.copy(status = TaskStatus.PENDING, completionSource = null)
            return true
        }
        return false
    }

    override fun toggleTaskCompletion(taskId: Long): Boolean {
        val task = getTaskById(taskId) ?: return false
        return if (task.status == TaskStatus.COMPLETED) {
            markTaskPending(taskId)
        } else {
            completeTask(taskId)
        }
    }

    override fun deleteTask(taskId: Long): Boolean {
        return taskList.removeIf { it.id == taskId }
    }

    override fun getAllTasks(limit: Int): List<TaskItem> {
        return taskList.filter { it.status != TaskStatus.CANCELLED }
    }

    override fun listPendingTasks(limit: Int): List<TaskItem> {
        return taskList.filter { it.status == TaskStatus.PENDING }
    }

    override fun listCompletedTasks(limit: Int): List<TaskItem> {
        return taskList.filter { it.status == TaskStatus.COMPLETED }
    }

    override fun getTaskCounts(): TaskCounts {
        val pending = taskList.count { it.status == TaskStatus.PENDING }
        val completed = taskList.count { it.status == TaskStatus.COMPLETED }
        return TaskCounts(pending = pending, completed = completed, total = pending + completed)
    }
}

class TaskManagerViewModelTest {

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var viewModel: TaskManagerViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeTaskRepository()
        viewModel = TaskManagerViewModel(fakeRepository, Dispatchers.Unconfined)
    }

    @Test
    fun testInitialState_emptyTasks() {
        val state = viewModel.uiState.value
        assertEquals(0, state.tasks.size)
        assertEquals(0, state.totalCount)
        assertEquals(0, state.pendingCount)
        assertEquals(0, state.completedCount)
        assertFalse(state.isLoading)
        assertEquals(TaskFilter.ALL, state.filter)
    }

    @Test
    fun testAddTask_addsTaskSuccessfully() {
        viewModel.addTask(title = "Review Architecture Draft", priority = Priority.HIGH, category = "Work")

        val state = viewModel.uiState.value
        assertEquals(1, state.tasks.size)
        assertEquals("Review Architecture Draft", state.tasks[0].title)
        assertEquals(Priority.HIGH, state.tasks[0].priority)
        assertEquals(TaskStatus.PENDING, state.tasks[0].status)
        assertEquals(1, state.pendingCount)
        assertEquals(0, state.completedCount)
        assertEquals(1, state.totalCount)
        assertNotNull(state.userMessage)
    }

    @Test
    fun testToggleTaskCompletion_pendingToCompletedAndBack() {
        viewModel.addTask(title = "Buy Groceries", priority = Priority.NORMAL)

        val taskId = viewModel.uiState.value.tasks[0].id

        // Toggle from PENDING to COMPLETED
        viewModel.toggleTaskCompletion(taskId)

        val completedState = viewModel.uiState.value
        assertEquals(TaskStatus.COMPLETED, completedState.tasks[0].status)
        assertEquals(0, completedState.pendingCount)
        assertEquals(1, completedState.completedCount)

        // Toggle back from COMPLETED to PENDING
        viewModel.toggleTaskCompletion(taskId)

        val reopenedState = viewModel.uiState.value
        assertEquals(TaskStatus.PENDING, reopenedState.tasks[0].status)
        assertEquals(1, reopenedState.pendingCount)
        assertEquals(0, reopenedState.completedCount)
    }

    @Test
    fun testFilters_all_pending_completed() {
        viewModel.addTask(title = "Task 1", priority = Priority.LOW)
        viewModel.addTask(title = "Task 2", priority = Priority.HIGH)

        val task1Id = viewModel.uiState.value.tasks.first { it.title == "Task 1" }.id
        viewModel.toggleTaskCompletion(task1Id)

        // Filter ALL
        viewModel.setFilter(TaskFilter.ALL)
        assertEquals(2, viewModel.uiState.value.filteredTasks.size)

        // Filter PENDING
        viewModel.setFilter(TaskFilter.PENDING)
        val pending = viewModel.uiState.value.filteredTasks
        assertEquals(1, pending.size)
        assertEquals("Task 2", pending[0].title)

        // Filter COMPLETED
        viewModel.setFilter(TaskFilter.COMPLETED)
        val completed = viewModel.uiState.value.filteredTasks
        assertEquals(1, completed.size)
        assertEquals("Task 1", completed[0].title)
    }

    @Test
    fun testSearchQuery_filtersMatchingTasks() {
        viewModel.addTask(title = "Call Dentist", category = "Health")
        viewModel.addTask(title = "Submit Report", category = "Work")

        viewModel.setSearchQuery("Dentist")
        val searchResults = viewModel.uiState.value.filteredTasks
        assertEquals(1, searchResults.size)
        assertEquals("Call Dentist", searchResults[0].title)

        viewModel.setSearchQuery("Work")
        val workResults = viewModel.uiState.value.filteredTasks
        assertEquals(1, workResults.size)
        assertEquals("Submit Report", workResults[0].title)
    }

    @Test
    fun testDeleteTask_removesTaskFromList() {
        viewModel.addTask(title = "Temporary task")

        val taskId = viewModel.uiState.value.tasks[0].id
        assertEquals(1, viewModel.uiState.value.tasks.size)

        viewModel.deleteTask(taskId)

        assertEquals(0, viewModel.uiState.value.tasks.size)
        assertEquals(0, viewModel.uiState.value.totalCount)
    }

    @Test
    fun testClearUserMessage() {
        viewModel.addTask(title = "Message test")

        assertNotNull(viewModel.uiState.value.userMessage)
        viewModel.clearUserMessage()
        assertNull(viewModel.uiState.value.userMessage)
    }
}
