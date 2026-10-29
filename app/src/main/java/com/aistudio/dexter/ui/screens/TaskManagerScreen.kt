package com.aistudio.dexter.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.dexter.data.model.Priority
import com.aistudio.dexter.data.model.TaskItem
import com.aistudio.dexter.data.model.TaskStatus
import com.aistudio.dexter.ui.theme.*
import com.aistudio.dexter.ui.viewmodel.TaskFilter
import com.aistudio.dexter.ui.viewmodel.TaskManagerUiState
import com.aistudio.dexter.ui.viewmodel.TaskManagerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskManagerScreen(
    viewModel: TaskManagerViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        val msg = uiState.userMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier.testTag("task_manager_screen"),
        containerColor = DexterBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Task Manager",
                            color = DexterOnBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "${uiState.pendingCount} pending · ${uiState.completedCount} completed",
                            color = DexterOnSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("task_manager_back_button")
                        ) {
                            Text("←", color = DexterOnBackground, fontSize = 22.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DexterBackground,
                    titleContentColor = DexterOnBackground
                )
            )
        }
    ) { innerPadding ->
        TaskManagerContent(
            uiState = uiState,
            onAddTask = { title, priority -> viewModel.addTask(title, priority) },
            onToggleCompletion = { taskId -> viewModel.toggleTaskCompletion(taskId) },
            onDeleteTask = { taskId -> viewModel.deleteTask(taskId) },
            onFilterChange = { filter -> viewModel.setFilter(filter) },
            onSearchQueryChange = { query -> viewModel.setSearchQuery(query) },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun TaskManagerContent(
    uiState: TaskManagerUiState,
    onAddTask: (String, Priority) -> Unit,
    onToggleCompletion: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onFilterChange: (TaskFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newTaskTitle by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.NORMAL) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Quick Add Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("add_task_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DexterSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        placeholder = {
                            Text(
                                "Add a new task locally...",
                                color = DexterOnSurfaceVariant,
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary,
                            unfocusedBorderColor = DexterSurfaceVariant,
                            cursorColor = DexterPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("task_input_field")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (newTaskTitle.isNotBlank()) {
                                onAddTask(newTaskTitle, selectedPriority)
                                newTaskTitle = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DexterPrimary,
                            contentColor = DexterOnPrimary
                        ),
                        modifier = Modifier
                            .height(52.dp)
                            .defaultMinSize(minWidth = 52.dp)
                            .testTag("add_task_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Task"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Priority Selection row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Priority:",
                        color = DexterOnSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    listOf(Priority.LOW, Priority.NORMAL, Priority.HIGH, Priority.CRITICAL).forEach { prio ->
                        val isSelected = selectedPriority == prio
                        val chipColor = when (prio) {
                            Priority.CRITICAL -> PriorityCritical
                            Priority.HIGH -> PriorityHigh
                            Priority.NORMAL -> PriorityNormal
                            Priority.LOW -> PriorityLow
                            Priority.SOMEDAY -> PrioritySomeday
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPriority = prio },
                            label = {
                                Text(
                                    text = prio.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipColor.copy(alpha = 0.25f),
                                selectedLabelColor = chipColor,
                                containerColor = DexterSurfaceVariant.copy(alpha = 0.5f),
                                labelColor = DexterOnSurfaceVariant
                            ),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("priority_chip_${prio.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Search & Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search tasks...", color = DexterOnSurfaceVariant, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = DexterOnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = DexterOnSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = DexterOnBackground,
                    unfocusedTextColor = DexterOnBackground,
                    focusedBorderColor = DexterPrimary.copy(alpha = 0.6f),
                    unfocusedBorderColor = DexterSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("task_search_field")
            )
        }

        // Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.filter == TaskFilter.ALL,
                onClick = { onFilterChange(TaskFilter.ALL) },
                label = { Text("All (${uiState.totalCount})", fontSize = 12.sp) },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DexterPrimary,
                    selectedLabelColor = DexterOnPrimary,
                    containerColor = DexterSurface,
                    labelColor = DexterOnSurface
                ),
                modifier = Modifier.testTag("task_filter_all")
            )

            FilterChip(
                selected = uiState.filter == TaskFilter.PENDING,
                onClick = { onFilterChange(TaskFilter.PENDING) },
                label = { Text("Pending (${uiState.pendingCount})", fontSize = 12.sp) },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DexterPrimary,
                    selectedLabelColor = DexterOnPrimary,
                    containerColor = DexterSurface,
                    labelColor = DexterOnSurface
                ),
                modifier = Modifier.testTag("task_filter_pending")
            )

            FilterChip(
                selected = uiState.filter == TaskFilter.COMPLETED,
                onClick = { onFilterChange(TaskFilter.COMPLETED) },
                label = { Text("Completed (${uiState.completedCount})", fontSize = 12.sp) },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DexterPrimary,
                    selectedLabelColor = DexterOnPrimary,
                    containerColor = DexterSurface,
                    labelColor = DexterOnSurface
                ),
                modifier = Modifier.testTag("task_filter_completed")
            )
        }

        val displayList = uiState.filteredTasks

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = DexterPrimary,
                    modifier = Modifier.testTag("task_loading_indicator")
                )
            }
        } else if (displayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .testTag("task_empty_view"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📋",
                        fontSize = 44.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when (uiState.filter) {
                            TaskFilter.ALL -> "No tasks found"
                            TaskFilter.PENDING -> "No pending tasks! All caught up."
                            TaskFilter.COMPLETED -> "No completed tasks yet."
                        },
                        color = DexterOnSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add a task using the bar above to get started.",
                        color = DexterOnSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("task_list_column"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
            ) {
                items(
                    items = displayList,
                    key = { it.id }
                ) { task ->
                    TaskItemRow(
                        task = task,
                        onToggle = { onToggleCompletion(task.id) },
                        onDelete = { onDeleteTask(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskItemRow(
    task: TaskItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = task.status == TaskStatus.COMPLETED
    val priorityBarColor = when (task.priority) {
        Priority.CRITICAL -> PriorityCritical
        Priority.HIGH -> PriorityHigh
        Priority.NORMAL -> PriorityNormal
        Priority.LOW -> PriorityLow
        Priority.SOMEDAY -> PrioritySomeday
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) DexterSurface.copy(alpha = 0.6f) else DexterSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority Accent Line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(priorityBarColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Checkbox Button (Minimum 48dp touch target)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onToggle)
                        .testTag("task_checkbox_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCompleted) DexterSuccess else DexterSurfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Title and Meta Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onToggle)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = priorityBarColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = task.priority.name,
                                color = priorityBarColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (task.category.isNotBlank() && task.category != "General") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = task.category,
                                color = DexterOnSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = task.title,
                        color = if (isCompleted) DexterOnSurfaceVariant else DexterOnBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(task.createdAt)),
                        color = DexterOnSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                // Delete Button (Minimum 48dp touch target)
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete task",
                        tint = DexterOnSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
