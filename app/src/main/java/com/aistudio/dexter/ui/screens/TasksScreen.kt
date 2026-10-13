package com.aistudio.dexter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.dexter.data.model.Priority
import com.aistudio.dexter.data.model.TaskItem
import com.aistudio.dexter.ui.components.TaskCard
import com.aistudio.dexter.ui.theme.*

@Composable
fun TasksScreen(
    pendingTasks: List<TaskItem>,
    completedTasks: List<TaskItem>,
    onCompleteTask: (Long) -> Unit,
    onCancelTask: (Long) -> Unit,
    onCreateTask: (String, Priority) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Pending, 1 = Completed
    var showAddDialog by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskPriority by remember { mutableStateOf(Priority.NORMAL) }

    Scaffold(
        containerColor = DexterBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = DexterPrimary,
                contentColor = DexterOnPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Tab Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Pending (${pendingTasks.size})", fontWeight = FontWeight.Medium) },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DexterPrimary,
                        selectedLabelColor = DexterOnPrimary,
                        containerColor = DexterSurface,
                        labelColor = DexterOnSurface
                    ),
                    modifier = Modifier.testTag("pending_tab_chip")
                )

                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("Completed (${completedTasks.size})", fontWeight = FontWeight.Medium) },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DexterPrimary,
                        selectedLabelColor = DexterOnPrimary,
                        containerColor = DexterSurface,
                        labelColor = DexterOnSurface
                    ),
                    modifier = Modifier.testTag("completed_tab_chip")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val currentList = if (selectedTab == 0) pendingTasks else completedTasks

            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Empty",
                            tint = DexterPrimary.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTab == 0) "No pending tasks! All caught up." else "No completed tasks yet.",
                            color = DexterOnSurfaceVariant,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Say \"Create a task to buy groceries\" anytime.",
                            color = DexterOnSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(currentList, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggleComplete = { onCompleteTask(task.id) },
                            onCancelTask = { onCancelTask(task.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Task", color = DexterOnBackground) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        placeholder = { Text("Task title...", color = DexterOnSurfaceVariant) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary,
                            unfocusedBorderColor = DexterSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_task_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Priority:", color = DexterOnSurface, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Priority.NORMAL, Priority.HIGH, Priority.CRITICAL).forEach { prio ->
                            FilterChip(
                                selected = newTaskPriority == prio,
                                onClick = { newTaskPriority = prio },
                                label = { Text(prio.name, fontSize = 11.sp) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            onCreateTask(newTaskTitle, newTaskPriority)
                            newTaskTitle = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary, contentColor = DexterOnPrimary),
                    modifier = Modifier.testTag("save_new_task_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = DexterOnSurfaceVariant)
                }
            },
            containerColor = DexterSurface
        )
    }
}
