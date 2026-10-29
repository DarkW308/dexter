package com.aistudio.dexter.ui.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.aistudio.dexter.data.db.DexterDatabaseHelper
import com.aistudio.dexter.data.repository.TaskRepository
import com.aistudio.dexter.ui.screens.TaskManagerScreen
import com.aistudio.dexter.ui.theme.DexterTheme
import com.aistudio.dexter.ui.viewmodel.TaskManagerViewModel

class TasksActivity : AppCompatActivity() {

    private val viewModel: TaskManagerViewModel by viewModels {
        TaskManagerViewModel.Factory(
            repository = TaskRepository(DexterDatabaseHelper.getInstance(this.applicationContext))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContent {
                DexterTheme {
                    TaskManagerScreen(
                        viewModel = viewModel,
                        onBack = { finish() }
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("TasksActivity", "Error setting Compose content in TasksActivity", e)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            viewModel.loadTasks()
        } catch (e: Exception) {
            Log.e("TasksActivity", "Error loading tasks in onResume", e)
        }
    }
}
