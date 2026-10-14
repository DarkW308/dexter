package com.aistudio.dexter.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aistudio.dexter.R
import com.aistudio.dexter.data.model.Priority
import com.aistudio.dexter.data.model.TaskItem
import com.aistudio.dexter.data.model.TaskStatus
import com.aistudio.dexter.data.repository.DexterRepository
import com.aistudio.dexter.databinding.ActivityTasksBinding
import com.aistudio.dexter.engine.DexterActionExecutor
import com.aistudio.dexter.engine.DexterSpeechManager
import com.aistudio.dexter.ui.view.VoiceInteractionDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TasksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTasksBinding
    private lateinit var repository: DexterRepository
    private lateinit var adapter: TasksAdapter
    private var isShowingPending = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTasksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = DexterRepository(this)

        setupRecyclerView()
        setupListeners()
        loadTasks()
    }

    private fun setupRecyclerView() {
        adapter = TasksAdapter(
            onTaskChecked = { task ->
                handleTaskCheckToggle(task)
            },
            onTaskLongClick = { task ->
                showTaskOptionsDialog(task)
            }
        )

        binding.tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.tasksRecyclerView.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.chipPending.setOnClickListener {
            isShowingPending = true
            updateTabStyles()
            loadTasks()
        }

        binding.chipCompleted.setOnClickListener {
            isShowingPending = false
            updateTabStyles()
            loadTasks()
        }

        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }

        binding.btnVoiceTask.setOnClickListener {
            val speechManager = DexterSpeechManager(this)
            val actionExecutor = DexterActionExecutor(this)
            val dialog = VoiceInteractionDialog(
                context = this,
                speechManager = speechManager,
                actionExecutor = actionExecutor,
                onActionCompleted = {
                    loadTasks()
                }
            )
            dialog.show()
        }
    }

    private fun updateTabStyles() {
        if (isShowingPending) {
            binding.chipPending.setBackgroundResource(R.drawable.bg_circle_primary)
            binding.chipPending.setTextColor(ContextCompat.getColor(this, R.color.dexter_on_primary))

            binding.chipCompleted.setBackgroundResource(R.drawable.bg_surface_rounded)
            binding.chipCompleted.setTextColor(ContextCompat.getColor(this, R.color.dexter_on_surface))
        } else {
            binding.chipPending.setBackgroundResource(R.drawable.bg_surface_rounded)
            binding.chipPending.setTextColor(ContextCompat.getColor(this, R.color.dexter_on_surface))

            binding.chipCompleted.setBackgroundResource(R.drawable.bg_circle_primary)
            binding.chipCompleted.setTextColor(ContextCompat.getColor(this, R.color.dexter_on_primary))
        }
    }

    private fun loadTasks() {
        lifecycleScope.launch {
            val pendingList = withContext(Dispatchers.IO) { repository.listPendingTasks() }
            val completedList = withContext(Dispatchers.IO) { repository.listCompletedTasks() }

            binding.chipPending.text = "Pending (${pendingList.size})"
            binding.chipCompleted.text = "Completed (${completedList.size})"

            val currentList = if (isShowingPending) pendingList else completedList

            if (currentList.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.tasksRecyclerView.visibility = View.GONE
                binding.emptyStateText.text = if (isShowingPending) {
                    "All clear! No pending tasks."
                } else {
                    "Nothing completed yet."
                }
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.tasksRecyclerView.visibility = View.VISIBLE
                adapter.submitList(currentList)
            }
        }
    }

    private fun handleTaskCheckToggle(task: TaskItem) {
        lifecycleScope.launch {
            if (task.status == TaskStatus.PENDING) {
                // Complete task with UNDO support via Snackbar
                withContext(Dispatchers.IO) {
                    repository.completeTask(task.id)
                }
                loadTasks()

                Snackbar.make(
                    binding.coordinatorLayout,
                    "Marked '${task.title}' complete",
                    Snackbar.LENGTH_LONG
                ).setAction("UNDO") {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            // Restore task to pending
                            val db = com.aistudio.dexter.data.db.DexterDatabaseHelper.getInstance(this@TasksActivity).writableDatabase
                            val values = android.content.ContentValues().apply {
                                put("status", "PENDING")
                                putNull("completed_at")
                            }
                            db.update("tasks", values, "id = ?", arrayOf(task.id.toString()))
                        }
                        loadTasks()
                    }
                }.setActionTextColor(ContextCompat.getColor(this@TasksActivity, R.color.dexter_primary))
                 .show()

            } else {
                // Uncomplete / Restore task
                withContext(Dispatchers.IO) {
                    val db = com.aistudio.dexter.data.db.DexterDatabaseHelper.getInstance(this@TasksActivity).writableDatabase
                    val values = android.content.ContentValues().apply {
                        put("status", "PENDING")
                        putNull("completed_at")
                    }
                    db.update("tasks", values, "id = ?", arrayOf(task.id.toString()))
                }
                loadTasks()
            }
        }
    }

    private fun showAddTaskDialog() {
        val input = EditText(this).apply {
            hint = "e.g. Call the doctor or Buy milk"
            setTextColor(ContextCompat.getColor(context, R.color.dexter_on_background))
            setHintTextColor(ContextCompat.getColor(context, R.color.dexter_on_surface_variant))
            setPadding(48, 36, 48, 36)
        }

        AlertDialog.Builder(this)
            .setTitle("Create Task")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val title = input.text.toString().trim()
                if (title.isNotEmpty()) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            repository.createTask(title = title, priority = Priority.NORMAL)
                        }
                        loadTasks()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTaskOptionsDialog(task: TaskItem) {
        val options = arrayOf("Delete / Cancel Task", "Set High Priority", "Set Normal Priority")
        AlertDialog.Builder(this)
            .setTitle(task.title)
            .setItems(options) { _, which ->
                lifecycleScope.launch {
                    when (which) {
                        0 -> withContext(Dispatchers.IO) { repository.cancelTask(task.id) }
                        1 -> withContext(Dispatchers.IO) {
                            val db = com.aistudio.dexter.data.db.DexterDatabaseHelper.getInstance(this@TasksActivity).writableDatabase
                            val values = android.content.ContentValues().apply { put("priority", "HIGH") }
                            db.update("tasks", values, "id = ?", arrayOf(task.id.toString()))
                        }
                        2 -> withContext(Dispatchers.IO) {
                            val db = com.aistudio.dexter.data.db.DexterDatabaseHelper.getInstance(this@TasksActivity).writableDatabase
                            val values = android.content.ContentValues().apply { put("priority", "NORMAL") }
                            db.update("tasks", values, "id = ?", arrayOf(task.id.toString()))
                        }
                    }
                    loadTasks()
                }
            }
            .show()
    }
}
