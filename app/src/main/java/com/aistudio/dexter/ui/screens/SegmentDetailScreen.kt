package com.aistudio.dexter.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aistudio.dexter.data.model.*
import com.aistudio.dexter.data.repository.DexterRepository
import com.aistudio.dexter.ui.components.ClockTime
import com.aistudio.dexter.ui.components.ModernClockTimePicker
import com.aistudio.dexter.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentDetailScreen(
    segmentNumber: Int,
    repository: DexterRepository,
    onBack: () -> Unit,
    onOpenTasks: () -> Unit = {},
    onOpenFinance: () -> Unit = {},
    onOpenJournal: () -> Unit = {}
) {
    val context = LocalContext.current
    var dataTrigger by remember { mutableStateOf(0L) }

    val segments = remember { repository.getAllSegments() }
    val currentSegment = segments.find { it.segmentNumber == segmentNumber }
        ?: SegmentInfo(segmentNumber, "Segment $segmentNumber", "General", "⚡", "Dexter Local Segment", emptyList())

    Scaffold(
        containerColor = DexterBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(currentSegment.icon, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${currentSegment.segmentNumber}. ${currentSegment.title}",
                                color = DexterOnBackground,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${currentSegment.category} · Fully Offline",
                                color = Color(0xFFA0A5BD),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_segment_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DexterOnBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DexterSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (segmentNumber) {
                1 -> TasksReferenceView(onOpenTasks)
                2 -> ProjectsMilestonesView(repository, dataTrigger) { dataTrigger++ }
                3 -> GoalsOutcomesView(repository, dataTrigger) { dataTrigger++ }
                4 -> FinanceReferenceView(onOpenFinance)
                5 -> JournalReferenceView(onOpenJournal)
                6 -> AlarmsRemindersView(repository, dataTrigger) { dataTrigger++ }
                7 -> ScheduleTimetableView(repository, dataTrigger) { dataTrigger++ }
                8 -> PeopleRelationshipsView(repository, dataTrigger) { dataTrigger++ }
                9 -> HealthWellbeingView(repository, dataTrigger) { dataTrigger++ }
                10 -> HabitsStreaksView(repository, dataTrigger) { dataTrigger++ }
                11 -> KnowledgeVaultView(repository, dataTrigger) { dataTrigger++ }
                12 -> QuickCaptureView(repository, dataTrigger) { dataTrigger++ }
                13 -> AttentionFeedView(repository, dataTrigger) { dataTrigger++ }
                14 -> DailyBriefingsView(repository, dataTrigger) { dataTrigger++ }
                15 -> SafetyRulesView(repository, dataTrigger) { dataTrigger++ }
                16 -> SystemDiagnosticsView(repository, dataTrigger) { dataTrigger++ }
                else -> GenericSegmentView(currentSegment)
            }
        }
    }
}

// ==========================================
// SEGMENT 1: Tasks (Reference)
// ==========================================
@Composable
private fun TasksReferenceView(onOpenTasks: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("✅", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tasks & Execution Engine",
            color = DexterOnBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Dedicated high-performance GTD sprint tracker with priority tags, recurring occurrences, and checkmark gestures.",
            color = Color(0xFFA0A5BD),
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onOpenTasks,
            colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
            modifier = Modifier.testTag("btn_open_tasks_screen")
        ) {
            Text("Open Tasks Hub", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// SEGMENT 2: Projects & Milestones
// ==========================================
@Composable
private fun ProjectsMilestonesView(
    repository: DexterRepository,
    trigger: Long,
    onRefresh: () -> Unit
) {
    val projects = remember(trigger) { repository.listProjects() }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var selectedProjectForMilestones by remember { mutableStateOf<ProjectItem?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewProjectDialog = true },
                containerColor = DexterPrimary,
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_add_project")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Project")
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📁", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Active Projects (${projects.size})",
                                color = DexterOnBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap any project to view & complete sub-milestones.",
                            color = Color(0xFFA0A5BD),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            items(projects, key = { it.id }) { project ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedProjectForMilestones = project }
                        .testTag("project_card_${project.id}"),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = project.name,
                                color = DexterOnBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    repository.deleteProject(project.id)
                                    onRefresh()
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFCF6679))
                            }
                        }

                        if (project.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = project.description,
                                color = Color(0xFFA0A5BD),
                                fontSize = 13.sp
                            )
                        }

                        if (project.deadline.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Deadline: ${project.deadline}",
                                color = DexterPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Progress (${project.taskCount} milestones)",
                                color = Color(0xFFA0A5BD),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${project.progressPercent}%",
                                color = DexterPrimaryGlow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (project.progressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = DexterPrimary,
                            trackColor = DexterSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showNewProjectDialog) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var deadline by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            containerColor = DexterSurface,
            title = { Text("New Project", color = DexterOnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Project Title") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_project_title")
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_project_desc")
                    )
                    OutlinedTextField(
                        value = deadline,
                        onValueChange = { deadline = it },
                        label = { Text("Target Deadline (e.g. Nov 30, 2026)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_project_deadline")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            repository.createProject(title, desc, deadline)
                            showNewProjectDialog = false
                            onRefresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary)
                ) {
                    Text("Create", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    selectedProjectForMilestones?.let { project ->
        ProjectMilestonesDialog(
            project = project,
            repository = repository,
            onDismiss = {
                selectedProjectForMilestones = null
                onRefresh()
            }
        )
    }
}

@Composable
private fun ProjectMilestonesDialog(
    project: ProjectItem,
    repository: DexterRepository,
    onDismiss: () -> Unit
) {
    var trigger by remember { mutableStateOf(0) }
    val milestones = remember(trigger) { repository.listMilestones(project.id) }
    var newMilestoneTitle by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(containerColor = DexterSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = project.name,
                            color = DexterOnBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Milestones & Deliverables",
                            color = Color(0xFFA0A5BD),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DexterOnBackground)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newMilestoneTitle,
                        onValueChange = { newMilestoneTitle = it },
                        placeholder = { Text("+ Add milestone...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_new_milestone"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newMilestoneTitle.isNotBlank()) {
                                repository.createMilestone(project.id, newMilestoneTitle)
                                newMilestoneTitle = ""
                                trigger++
                            }
                        },
                        modifier = Modifier
                            .background(DexterPrimary, CircleShape)
                            .testTag("btn_add_milestone")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (milestones.isEmpty()) {
                        item {
                            Text(
                                text = "No milestones added yet. Add checklist items above.",
                                color = Color(0xFFA0A5BD),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }
                    items(milestones, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DexterSurfaceVariant, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isCompleted,
                                onCheckedChange = { checked ->
                                    repository.toggleMilestone(item.id, checked, project.id)
                                    trigger++
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = DexterPrimary,
                                    uncheckedColor = Color(0xFFA0A5BD)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.title,
                                color = if (item.isCompleted) Color(0xFFA0A5BD) else DexterOnBackground,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    repository.deleteMilestone(item.id, project.id)
                                    trigger++
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFCF6679), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SEGMENT 3: Goals & Long-Term Outcomes
// ==========================================
@Composable
private fun GoalsOutcomesView(
    repository: DexterRepository,
    trigger: Long,
    onRefresh: () -> Unit
) {
    val goals = remember(trigger) { repository.listGoals() }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalToUpdate by remember { mutableStateOf<GoalItem?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddGoalDialog = true },
                containerColor = DexterPrimary,
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_add_goal")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎯", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Long-Term Goals & Vision (${goals.size})",
                                color = DexterOnBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Track high-stakes milestones with custom metrics & target dates.",
                            color = Color(0xFFA0A5BD),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            items(goals, key = { it.id }) { goal ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_card_${goal.id}"),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = goal.name,
                                color = DexterOnBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .background(DexterPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(goal.category, color = DexterPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = goal.progressLabel,
                                color = DexterPrimaryGlow,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (goal.targetDate.isNotBlank()) {
                                Text(
                                    text = "Target: ${goal.targetDate}",
                                    color = Color(0xFFA0A5BD),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { (goal.progressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = DexterPrimary,
                            trackColor = DexterSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    repository.deleteGoal(goal.id)
                                    onRefresh()
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFCF6679))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { goalToUpdate = goal },
                                colors = ButtonDefaults.buttonColors(containerColor = DexterSurfaceVariant),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_update_goal_${goal.id}")
                            ) {
                                Text("Update Progress", color = DexterPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddGoalDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Financial") }
        var targetAmount by remember { mutableStateOf("50000") }
        var targetDate by remember { mutableStateOf("Dec 31, 2026") }
        var metricUnit by remember { mutableStateOf("₹") }

        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            containerColor = DexterSurface,
            title = { Text("Add Goal", color = DexterOnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Goal Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category (Financial, Health, Career, Personal)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = targetAmount,
                            onValueChange = { targetAmount = it },
                            label = { Text("Target Amount") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = DexterOnBackground,
                                unfocusedTextColor = DexterOnBackground,
                                focusedBorderColor = DexterPrimary
                            ),
                            modifier = Modifier.weight(2f)
                        )
                        OutlinedTextField(
                            value = metricUnit,
                            onValueChange = { metricUnit = it },
                            label = { Text("Unit (₹/km)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = DexterOnBackground,
                                unfocusedTextColor = DexterOnBackground,
                                focusedBorderColor = DexterPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = targetDate,
                        onValueChange = { targetDate = it },
                        label = { Text("Target Date (e.g. Dec 31, 2026)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val amount = targetAmount.toDoubleOrNull() ?: 0.0
                            repository.createGoal(name, category, amount, targetDate, metricUnit)
                            showAddGoalDialog = false
                            onRefresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary)
                ) {
                    Text("Save Goal", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    goalToUpdate?.let { goal ->
        var currentInput by remember { mutableStateOf(goal.currentAmount.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { goalToUpdate = null },
            containerColor = DexterSurface,
            title = { Text("Update Progress", color = DexterOnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Goal: ${goal.name}", color = Color(0xFFA0A5BD), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = currentInput,
                        onValueChange = { currentInput = it },
                        label = { Text("Current ${goal.metricUnit} achieved") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val curr = currentInput.toDoubleOrNull() ?: goal.currentAmount
                        repository.updateGoalProgress(goal.id, curr, goal.targetAmount)
                        goalToUpdate = null
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary)
                ) {
                    Text("Save", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToUpdate = null }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }
}

// ==========================================
// SEGMENT 4: Finance (Reference)
// ==========================================
@Composable
private fun FinanceReferenceView(onOpenFinance: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💳", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Personal Finance & Ledger",
            color = DexterOnBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Bank-statement ledger, running balances, credit/debit filters, category breakdowns, and ₹ Rupee tracking.",
            color = Color(0xFFA0A5BD),
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onOpenFinance,
            colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
            modifier = Modifier.testTag("btn_open_finance_screen")
        ) {
            Text("Open Bank Ledger", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// SEGMENT 5: Journal (Reference)
// ==========================================
@Composable
private fun JournalReferenceView(onOpenJournal: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📖", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Personal Diary & Journal",
            color = DexterOnBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Reflections, voice memos, mood selectors, rich text notes, and writing streaks.",
            color = Color(0xFFA0A5BD),
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onOpenJournal,
            colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
            modifier = Modifier.testTag("btn_open_journal_screen")
        ) {
            Text("Open Personal Diary", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// SEGMENT 6: Alarms & Timed Reminders
// ==========================================
@Composable
private fun AlarmsRemindersView(
    repository: DexterRepository,
    trigger: Long,
    onRefresh: () -> Unit
) {
    val alarms = remember(trigger) { repository.listAlarms() }
    var showSetAlarmSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSetAlarmSheet = true },
                containerColor = DexterPrimary,
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_add_alarm")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Set Alarm")
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⏰", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Alarms & Timed Reminders (${alarms.size})",
                                color = DexterOnBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Doze-resistant offline audio alarms with digital time display & repeats.",
                            color = Color(0xFFA0A5BD),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            items(alarms, key = { it.id }) { alarm ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alarm_card_${alarm.id}"),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = alarm.humanTime,
                                color = DexterOnBackground,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = alarm.label,
                                color = DexterPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${alarm.repeatDays} · Vibrate: ${if (alarm.vibrate) "On" else "Off"}",
                                color = Color(0xFFA0A5BD),
                                fontSize = 12.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = alarm.isEnabled,
                                onCheckedChange = { checked ->
                                    repository.toggleAlarm(alarm.id, checked)
                                    onRefresh()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = DexterPrimary
                                )
                            )
                            IconButton(
                                onClick = {
                                    repository.deleteAlarm(alarm.id)
                                    onRefresh()
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFCF6679))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSetAlarmSheet) {
        var label by remember { mutableStateOf("") }
        var alarmTime by remember { mutableStateOf(ClockTime(6, 30)) }
        var repeatDays by remember { mutableStateOf("Mon, Wed, Fri") }
        var vibrate by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showSetAlarmSheet = false },
            containerColor = DexterSurface,
            title = { Text("Set Alarm / Reminder", color = DexterOnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ModernClockTimePicker(
                        label = "SET ALARM TIME",
                        time = alarmTime,
                        onTimeChanged = { alarmTime = it },
                        presets = listOf(
                            "⏰ 06:30 AM" to ClockTime(6, 30),
                            "⏰ 07:00 AM" to ClockTime(7, 0),
                            "⏰ 08:00 AM" to ClockTime(8, 0),
                            "⏰ 09:30 AM" to ClockTime(9, 30),
                            "🌙 10:00 PM" to ClockTime(22, 0)
                        )
                    )

                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Label (e.g. Morning Workout)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )

                    OutlinedTextField(
                        value = repeatDays,
                        onValueChange = { repeatDays = it },
                        label = { Text("Repeats (Daily / Mon, Wed, Fri / One-time)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vibration", color = DexterOnBackground)
                        Switch(
                            checked = vibrate,
                            onCheckedChange = { vibrate = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = DexterPrimary)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val alarmMillis = alarmTime.toMillisToday()
                        repository.createAlarm(alarmMillis, label.ifBlank { "Alarm" }, repeatDays, vibrate)
                        showSetAlarmSheet = false
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary)
                ) {
                    Text("Save Alarm", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetAlarmSheet = false }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }
}

// ==========================================
// SEGMENT 7: Time & Schedule (Daily Timetable)
// ==========================================
@Composable
private fun ScheduleTimetableView(
    repository: DexterRepository,
    trigger: Long,
    onRefresh: () -> Unit
) {
    val blocks = remember(trigger) { repository.listScheduleBlocks() }
    var showAddBlockDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddBlockDialog = true },
                containerColor = DexterPrimary,
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_add_schedule_block")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🗓️", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Today's Agenda Timetable",
                                color = DexterOnBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Vertical schedule blocks with timeline indicator.",
                            color = Color(0xFFA0A5BD),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Current hour indicator line
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(DexterPrimaryGlow, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("NOW", color = DexterPrimaryGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    HorizontalDivider(modifier = Modifier.weight(1f), color = DexterPrimaryGlow.copy(alpha = 0.5f), thickness = 1.dp)
                }
            }

            items(blocks, key = { it.id }) { block ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_block_${block.id}"),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(50.dp)
                                .background(Color(android.graphics.Color.parseColor(block.colorHex)), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${block.startTime} – ${block.endTime}",
                                color = DexterPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = block.title,
                                color = DexterOnBackground,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (block.location.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "📍 ${block.location}",
                                    color = Color(0xFFA0A5BD),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                repository.deleteScheduleBlock(block.id)
                                onRefresh()
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFCF6679))
                        }
                    }
                }
            }
        }
    }

    if (showAddBlockDialog) {
        var title by remember { mutableStateOf("") }
        var startClock by remember { mutableStateOf(ClockTime(10, 0)) }
        var endClock by remember { mutableStateOf(ClockTime(11, 0)) }
        var category by remember { mutableStateOf("Deep Work") }
        var location by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddBlockDialog = false },
            containerColor = DexterSurface,
            title = { Text("Add Schedule Block", color = DexterOnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Block Title (e.g. Deep Work: Coding)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )

                    ModernClockTimePicker(
                        label = "START TIME",
                        time = startClock,
                        onTimeChanged = { picked ->
                            startClock = picked
                            if (endClock.hour24 < picked.hour24 || (endClock.hour24 == picked.hour24 && endClock.minute <= picked.minute)) {
                                endClock = ClockTime((picked.hour24 + 1) % 24, picked.minute)
                            }
                        },
                        presets = listOf(
                            "Now" to ClockTime.now(),
                            "09:00 AM" to ClockTime(9, 0),
                            "10:00 AM" to ClockTime(10, 0),
                            "02:00 PM" to ClockTime(14, 0),
                            "04:30 PM" to ClockTime(16, 30)
                        )
                    )

                    ModernClockTimePicker(
                        label = "END TIME",
                        time = endClock,
                        onTimeChanged = { endClock = it },
                        presets = listOf(
                            "+30m" to ClockTime(startClock.hour24 + (if (startClock.minute + 30 >= 60) 1 else 0), (startClock.minute + 30) % 60),
                            "+1h" to ClockTime((startClock.hour24 + 1) % 24, startClock.minute),
                            "+2h" to ClockTime((startClock.hour24 + 2) % 24, startClock.minute),
                            "05:00 PM" to ClockTime(17, 0)
                        )
                    )

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category Tag") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location / Link (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val colorHex = when (category.lowercase()) {
                                "wellness", "break" -> "#69F0AE"
                                "meeting" -> "#FFAB40"
                                else -> "#4FC3F7"
                            }
                            repository.createScheduleBlock(
                                title,
                                startClock.formattedString,
                                endClock.formattedString,
                                category,
                                colorHex,
                                location
                            )
                            showAddBlockDialog = false
                            onRefresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary)
                ) {
                    Text("Add Block", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBlockDialog = false }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }
}

// ==========================================
// SEGMENT 8: People & Relationships
// ==========================================
@Composable
private fun PeopleRelationshipsView(
    repository: DexterRepository,
    trigger: Long,
    onRefresh: () -> Unit
) {
    val people = remember(trigger) { repository.listPeople() }
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var personForInteractions by remember { mutableStateOf<PersonItem?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPersonDialog = true },
                containerColor = DexterPrimary,
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_add_contact")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact")
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👥", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Personal Network (${people.size})",
                                color = DexterOnBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap contact to log interactions, calls, and meetings.",
                            color = Color(0xFFA0A5BD),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            items(people, key = { it.id }) { person ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { personForInteractions = person }
                        .testTag("person_card_${person.id}"),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(DexterPrimary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = person.initial,
                                color = DexterPrimaryGlow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = person.name,
                                    color = DexterOnBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(DexterSurfaceVariant, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(person.relationType, color = DexterPrimary, fontSize = 10.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Last contact: ${person.lastContactedText}",
                                color = Color(0xFFA0A5BD),
                                fontSize = 12.sp
                            )
                            if (person.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = person.notes,
                                    color = Color(0xFFA0A5BD),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                repository.deletePerson(person.id)
                                onRefresh()
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFCF6679))
                        }
                    }
                }
            }
        }
    }

    if (showAddPersonDialog) {
        var name by remember { mutableStateOf("") }
        var relationType by remember { mutableStateOf("Close Friend") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var birthday by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddPersonDialog = false },
            containerColor = DexterSurface,
            title = { Text("Add Contact", color = DexterOnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )
                    OutlinedTextField(
                        value = relationType,
                        onValueChange = { relationType = it },
                        label = { Text("Relation Circle (Family, Friend, Doctor, Work)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )
                    OutlinedTextField(
                        value = birthday,
                        onValueChange = { birthday = it },
                        label = { Text("Birthday (e.g. Nov 14)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Important Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            repository.createPerson(name, relationType, phone, email, birthday, notes)
                            showAddPersonDialog = false
                            onRefresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary)
                ) {
                    Text("Save Contact", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPersonDialog = false }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    personForInteractions?.let { person ->
        PersonInteractionsDialog(
            person = person,
            repository = repository,
            onDismiss = {
                personForInteractions = null
                onRefresh()
            }
        )
    }
}

@Composable
private fun PersonInteractionsDialog(
    person: PersonItem,
    repository: DexterRepository,
    onDismiss: () -> Unit
) {
    var trigger by remember { mutableStateOf(0) }
    val interactions = remember(trigger) { repository.listInteractions(person.id) }
    var noteInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),
            colors = CardDefaults.cardColors(containerColor = DexterSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(person.name, color = DexterOnBackground, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Interaction History · ${person.relationType}", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DexterOnBackground)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        placeholder = { Text("Log call/meeting note...", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (noteInput.isNotBlank()) {
                                repository.createInteraction(person.id, noteInput, "Meeting", "Today")
                                noteInput = ""
                                trigger++
                            }
                        },
                        modifier = Modifier.background(DexterPrimary, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Log", tint = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (interactions.isEmpty()) {
                        item {
                            Text("No interactions logged yet.", color = Color(0xFFA0A5BD), fontSize = 13.sp)
                        }
                    }
                    items(interactions, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DexterSurfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.interactionType, color = DexterPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(item.interactionDate, color = Color(0xFFA0A5BD), fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(item.note, color = DexterOnBackground, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SEGMENT 9: Health & Wellbeing
// ==========================================
@Composable
private fun HealthWellbeingView(
    repository: DexterRepository,
    trigger: Long,
    onRefresh: () -> Unit
) {
    val hydrationMl = remember(trigger) { repository.getHydrationTodayMl() }
    val sleepLogs = remember(trigger) { repository.listSleep() }
    val moodLogs = remember(trigger) { repository.listMoods() }
    var showSleepDialog by remember { mutableStateOf(false) }

    val moods = listOf("Great", "Good", "Neutral", "Low", "Stressed")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick Hydration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💧", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Hydration Tracker", color = DexterOnBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Daily Target: 2,500 ml", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                            }
                        }
                        Button(
                            onClick = {
                                repository.logHydration(250)
                                onRefresh()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                            modifier = Modifier.testTag("btn_log_water")
                        ) {
                            Text("+250ml Glass", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${hydrationMl} ml / 2,500 ml (${hydrationMl / 250} glasses)",
                            color = DexterPrimaryGlow,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${((hydrationMl / 2500f) * 100).toInt()}%",
                            color = DexterOnBackground,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (hydrationMl / 2500f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = DexterPrimary,
                        trackColor = DexterSurfaceVariant
                    )
                }
            }
        }

        // Sleep Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("😴", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Sleep Telemetry", color = DexterOnBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                val latest = sleepLogs.firstOrNull()
                                Text(
                                    text = if (latest != null) "${latest.durationText} (${latest.bedtime} – ${latest.waketime})" else "No sleep logged",
                                    color = DexterPrimary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Button(
                            onClick = { showSleepDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DexterSurfaceVariant),
                            modifier = Modifier.testTag("btn_log_sleep_dialog")
                        ) {
                            Text("Log Sleep", color = DexterPrimary)
                        }
                    }

                    sleepLogs.firstOrNull()?.let { s ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Rating: ", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                            repeat(s.qualityRating) {
                                Text("⭐", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("· ${s.qualityLabel}", color = DexterOnBackground, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Mood Tracker Strip
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("😊 Mood & Energy Pulse", color = DexterOnBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("One-tap daily mood check-in", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        moods.forEach { m ->
                            SuggestionChip(
                                onClick = {
                                    repository.logMood(m, "Medium", "Quick tap check-in")
                                    onRefresh()
                                },
                                label = { Text(m, fontSize = 12.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = DexterSurfaceVariant,
                                    labelColor = DexterOnBackground
                                ),
                                border = null,
                                modifier = Modifier.testTag("chip_mood_$m")
                            )
                        }
                    }

                    moodLogs.firstOrNull()?.let { latestMood ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Last logged: \"${latestMood.mood}\" at ${latestMood.humanTime}",
                            color = DexterPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    if (showSleepDialog) {
        var bedClock by remember { mutableStateOf(ClockTime(23, 30)) }
        var wakeClock by remember { mutableStateOf(ClockTime(6, 50)) }
        var rating by remember { mutableStateOf(5) }
        var qualityLabel by remember { mutableStateOf("Restful") }

        val computedDuration by remember(bedClock, wakeClock) {
            derivedStateOf {
                var diffMinutes = (wakeClock.hour24 * 60 + wakeClock.minute) - (bedClock.hour24 * 60 + bedClock.minute)
                if (diffMinutes < 0) diffMinutes += 24 * 60
                val h = diffMinutes / 60
                val m = diffMinutes % 60
                "${h}h ${m}m"
            }
        }

        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            containerColor = DexterSurface,
            title = { Text("Log Sleep", color = DexterOnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModernClockTimePicker(
                        label = "BEDTIME",
                        time = bedClock,
                        onTimeChanged = { bedClock = it },
                        presets = listOf(
                            "10:30 PM" to ClockTime(22, 30),
                            "11:00 PM" to ClockTime(23, 0),
                            "11:30 PM" to ClockTime(23, 30),
                            "12:00 AM" to ClockTime(0, 0)
                        )
                    )

                    ModernClockTimePicker(
                        label = "WAKE TIME",
                        time = wakeClock,
                        onTimeChanged = { wakeClock = it },
                        presets = listOf(
                            "06:00 AM" to ClockTime(6, 0),
                            "06:30 AM" to ClockTime(6, 30),
                            "07:00 AM" to ClockTime(7, 0),
                            "07:30 AM" to ClockTime(7, 30)
                        )
                    )

                    // Auto-calculated duration badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DexterPrimary.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Calculated Duration:", color = Color(0xFFA0A5BD), fontSize = 13.sp)
                        Text(
                            text = "⏱ $computedDuration",
                            color = DexterPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    OutlinedTextField(
                        value = qualityLabel,
                        onValueChange = { qualityLabel = it },
                        label = { Text("Quality (Restful / Light / Deep)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rating (1-5): ", color = DexterOnBackground)
                        (1..5).forEach { star ->
                            Text(
                                text = if (star <= rating) "⭐" else "☆",
                                modifier = Modifier
                                    .clickable { rating = star }
                                    .padding(4.dp),
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.logSleep(bedClock.formattedString, wakeClock.formattedString, computedDuration, rating, qualityLabel, "")
                        showSleepDialog = false
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary)
                ) {
                    Text("Save", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSleepDialog = false }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }
}

// ==========================================
// SEGMENT 10: Habits & Streaks
// ==========================================
@Composable
private fun HabitsStreaksView(
    repository: DexterRepository,
    trigger: Long,
    onRefresh: () -> Unit
) {
    val habits = remember(trigger) { repository.listHabits() }
    var showAddHabitDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddHabitDialog = true },
                containerColor = DexterPrimary,
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_add_habit")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Habit")
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Consistency Habits (${habits.size})",
                                color = DexterOnBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the circular checkmark to complete for today & build your flame streak.",
                            color = Color(0xFFA0A5BD),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            items(habits, key = { it.id }) { habit ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_card_${habit.id}"),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color(0xFF2E2E50), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔥 ${habit.streakCount}",
                                color = Color(0xFFFFAB40),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = habit.name,
                                color = DexterOnBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${habit.frequency} · Reminder: ${habit.reminderTime}",
                                color = Color(0xFFA0A5BD),
                                fontSize = 12.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                repository.toggleHabitDoneToday(habit.id)
                                onRefresh()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (habit.isDoneToday) DexterSuccess else DexterSurfaceVariant,
                                    CircleShape
                                )
                                .testTag("btn_toggle_habit_${habit.id}")
                        ) {
                            Icon(
                                imageVector = if (habit.isDoneToday) Icons.Default.Check else Icons.Default.Done,
                                contentDescription = "Complete",
                                tint = if (habit.isDoneToday) Color.Black else Color(0xFFA0A5BD)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                repository.deleteHabit(habit.id)
                                onRefresh()
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFCF6679), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddHabitDialog) {
        var name by remember { mutableStateOf("") }
        var frequency by remember { mutableStateOf("Daily") }
        var reminderClock by remember { mutableStateOf(ClockTime(8, 0)) }

        AlertDialog(
            onDismissRequest = { showAddHabitDialog = false },
            containerColor = DexterSurface,
            title = { Text("New Habit", color = DexterOnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Habit Name (e.g. Read 15 pages)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )

                    OutlinedTextField(
                        value = frequency,
                        onValueChange = { frequency = it },
                        label = { Text("Frequency (Daily / 3x a week)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )

                    ModernClockTimePicker(
                        label = "REMINDER TIME",
                        time = reminderClock,
                        onTimeChanged = { reminderClock = it },
                        presets = listOf(
                            "🌅 07:00 AM" to ClockTime(7, 0),
                            "☀️ 08:00 AM" to ClockTime(8, 0),
                            "🥪 12:30 PM" to ClockTime(12, 30),
                            "🌆 06:00 PM" to ClockTime(18, 0),
                            "🌙 09:30 PM" to ClockTime(21, 30)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            repository.createHabit(name, frequency, reminderClock.formattedString)
                            showAddHabitDialog = false
                            onRefresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary)
                ) {
                    Text("Add Habit", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddHabitDialog = false }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }
}

// ==========================================
// SEGMENT 11: Reading & Knowledge Vault
// ==========================================
@Composable
private fun KnowledgeVaultView(
    repository: DexterRepository,
    trigger: Long,
    onRefresh: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val docs = remember(trigger, searchQuery) { repository.listDocuments(searchQuery) }
    var showAddDocDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDocDialog = true },
                containerColor = DexterPrimary,
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_add_doc")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Document")
            }
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search local passages & notes...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = DexterPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFFA0A5BD))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_knowledge"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = DexterOnBackground,
                    unfocusedTextColor = DexterOnBackground,
                    focusedBorderColor = DexterPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(docs, key = { it.id }) { doc ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("doc_card_${doc.id}"),
                        colors = CardDefaults.cardColors(containerColor = DexterSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .background(DexterPrimary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(doc.fileType, color = DexterPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = doc.title,
                                        color = DexterOnBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        repository.deleteDocument(doc.id)
                                        onRefresh()
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFCF6679))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = doc.excerpt,
                                color = Color(0xFFA0A5BD),
                                fontSize = 13.sp,
                                maxLines = 3
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Indexed ${doc.chunkCount} passages",
                                    color = DexterPrimaryGlow,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = doc.humanDate,
                                    color = Color(0xFFA0A5BD),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDocDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var fileType by remember { mutableStateOf("NOTE") }

        AlertDialog(
            onDismissRequest = { showAddDocDialog = false },
            containerColor = DexterSurface,
            title = { Text("Add Document / Note", color = DexterOnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Document Title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Paste Text / Notes Directly") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            repository.createDocument(title, content, "Manual Entry", fileType)
                            showAddDocDialog = false
                            onRefresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary)
                ) {
                    Text("Index Document", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDocDialog = false }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }
}

// ==========================================
// SEGMENT 12: Quick Capture & Inbox
// ==========================================
@Composable
private fun QuickCaptureView(
    repository: DexterRepository,
    trigger: Long,
    onRefresh: () -> Unit
) {
    val notes = remember(trigger) { repository.listCaptureNotes() }
    var quickInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Sticky rapid-entry text field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DexterSurface, RoundedCornerShape(16.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = quickInput,
                onValueChange = { quickInput = it },
                placeholder = { Text("Quick thought or note...", fontSize = 14.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_quick_capture"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = DexterOnBackground,
                    unfocusedTextColor = DexterOnBackground,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
            IconButton(
                onClick = {
                    if (quickInput.isNotBlank()) {
                        repository.createCaptureNote(quickInput)
                        quickInput = ""
                        onRefresh()
                    }
                },
                modifier = Modifier
                    .background(DexterPrimary, CircleShape)
                    .testTag("btn_send_capture")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Unprocessed Inbox (${notes.size})",
                    color = Color(0xFFA0A5BD),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            if (notes.isEmpty()) {
                item {
                    Text(
                        text = "Inbox clear! Rapidly capture ideas above without stopping to organize.",
                        color = Color(0xFFA0A5BD),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
            }

            items(notes, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("capture_card_${item.id}"),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = item.noteText,
                            color = DexterOnBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Captured: ${item.humanTime}",
                            color = Color(0xFFA0A5BD),
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    repository.dismissCaptureNote(item.id)
                                    onRefresh()
                                }
                            ) {
                                Text("Dismiss / Archive", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    repository.convertCaptureToTask(item.id, item.noteText)
                                    onRefresh()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_convert_task_${item.id}")
                            ) {
                                Text("Convert to Task", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SEGMENT 13: Attention & Priority Feed
// ==========================================
@Composable
private fun AttentionFeedView(
    repository: DexterRepository,
    trigger: Long,
    onRefresh: () -> Unit
) {
    val alerts = remember(trigger) { repository.listAttentionEvents() }
    var showAddAlertDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddAlertDialog = true },
                containerColor = DexterPrimary,
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_add_alert")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alert")
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🚨", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Attention & Priority Feed (${alerts.size})",
                                color = DexterOnBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tiered high-urgency notifications. Acknowledge or snooze 1 hour.",
                            color = Color(0xFFA0A5BD),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (alerts.isEmpty()) {
                item {
                    Text(
                        text = "All alerts clear! No critical attention items currently pending.",
                        color = Color(0xFFA0A5BD),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }

            items(alerts, key = { it.id }) { alert ->
                val borderColor = when (alert.urgency.lowercase()) {
                    "critical" -> Color(0xFFFF1744)
                    "medium", "amber" -> Color(0xFFFFAB40)
                    else -> DexterPrimaryGlow
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                        .testTag("alert_card_${alert.id}"),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = alert.title,
                                color = DexterOnBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .background(borderColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = alert.urgency.uppercase(),
                                    color = borderColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    repository.snoozeAttention(alert.id)
                                    onRefresh()
                                }
                            ) {
                                Text("Snooze 1 Hour", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    repository.acknowledgeAttention(alert.id)
                                    onRefresh()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_ack_alert_${alert.id}")
                            ) {
                                Text("Acknowledge", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddAlertDialog) {
        var title by remember { mutableStateOf("") }
        var urgency by remember { mutableStateOf("Critical") }

        AlertDialog(
            onDismissRequest = { showAddAlertDialog = false },
            containerColor = DexterSurface,
            title = { Text("Add Priority Alert", color = DexterOnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Alert Description") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )
                    OutlinedTextField(
                        value = urgency,
                        onValueChange = { urgency = it },
                        label = { Text("Urgency (Critical / Medium / Info)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            repository.createAttentionEvent(title, urgency)
                            showAddAlertDialog = false
                            onRefresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary)
                ) {
                    Text("Save Alert", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAlertDialog = false }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }
}

// ==========================================
// SEGMENT 14: Daily Briefings & Summaries
// ==========================================
@Composable
private fun DailyBriefingsView(
    repository: DexterRepository,
    trigger: Long,
    onRefresh: () -> Unit
) {
    val briefings = remember(trigger) { repository.listBriefings() }
    var selectedBriefing by remember { mutableStateOf<BriefingItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Header with Manual "Generate Now" Action
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DexterSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌅", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Daily Briefing Archive",
                            color = DexterOnBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Button(
                        onClick = {
                            repository.generateBriefingNow()
                            onRefresh()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                        modifier = Modifier.testTag("btn_generate_briefing_now")
                    ) {
                        Text("Generate Now", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Deterministic, offline executive summary generated without requiring voice or cloud LLM.",
                    color = Color(0xFFA0A5BD),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(briefings, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedBriefing = item }
                        .testTag("briefing_card_${item.id}"),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                color = DexterOnBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = item.weatherText,
                                color = DexterPrimaryGlow,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.summaryText,
                            color = Color(0xFFA0A5BD),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tasks: ${item.tasksCount} pending · Spent: ${item.formattedSpent}",
                            color = DexterPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    selectedBriefing?.let { b ->
        AlertDialog(
            onDismissRequest = { selectedBriefing = null },
            containerColor = DexterSurface,
            title = { Text(b.title, color = DexterOnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(b.summaryText, color = DexterOnBackground, fontSize = 14.sp, lineHeight = 20.sp)
                    HorizontalDivider(color = DexterSurfaceVariant)
                    Text("Date: ${b.dateString}", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                    Text("Weather telemetry: ${b.weatherText}", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                    Text("Pending items at generation: ${b.tasksCount}", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedBriefing = null },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary)
                ) {
                    Text("Close", color = Color.Black)
                }
            }
        )
    }
}

// ==========================================
// SEGMENT 15: Safety & System Rules
// ==========================================
@Composable
private fun SafetyRulesView(
    repository: DexterRepository,
    trigger: Long,
    onRefresh: () -> Unit
) {
    val rules = remember(trigger) { repository.listRules() }
    var showAddRuleDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddRuleDialog = true },
                containerColor = DexterPrimary,
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_add_rule")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛡️", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Local Safety Policies & Rules (${rules.size})",
                                color = DexterOnBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enforce hardware, storage, and privacy boundaries locally.",
                            color = Color(0xFFA0A5BD),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            items(rules, key = { it.id }) { rule ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_card_${rule.id}"),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (rule.enforcementLevel == "Strict") Color(0xFFFF1744).copy(alpha = 0.2f) else Color(0xFFFFAB40).copy(alpha = 0.2f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = rule.enforcementLevel.uppercase(),
                                        color = if (rule.enforcementLevel == "Strict") Color(0xFFFF1744) else Color(0xFFFFAB40),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = rule.ruleText,
                                color = DexterOnBackground,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Switch(
                            checked = rule.isEnabled,
                            onCheckedChange = { checked ->
                                repository.toggleRule(rule.id, checked)
                                onRefresh()
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = DexterPrimary)
                        )

                        IconButton(
                            onClick = {
                                repository.deleteRule(rule.id)
                                onRefresh()
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFCF6679), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddRuleDialog) {
        var ruleText by remember { mutableStateOf("") }
        var level by remember { mutableStateOf("Strict") }

        AlertDialog(
            onDismissRequest = { showAddRuleDialog = false },
            containerColor = DexterSurface,
            title = { Text("Add Safety Policy", color = DexterOnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = ruleText,
                        onValueChange = { ruleText = it },
                        label = { Text("Rule Description (e.g. Max 5 notifications)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )
                    OutlinedTextField(
                        value = level,
                        onValueChange = { level = it },
                        label = { Text("Enforcement (Strict / Warn)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DexterOnBackground, unfocusedTextColor = DexterOnBackground)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ruleText.isNotBlank()) {
                            repository.createRule(ruleText, level)
                            showAddRuleDialog = false
                            onRefresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary)
                ) {
                    Text("Add Rule", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRuleDialog = false }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }
}

// ==========================================
// SEGMENT 16: System Diagnostics & Local Engine
// ==========================================
@Composable
private fun SystemDiagnosticsView(
    repository: DexterRepository,
    trigger: Long,
    onRefresh: () -> Unit
) {
    val diag = remember(trigger) { repository.getDiagnostics() }
    val logs = remember(trigger) { repository.listAuditLogs(20) }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚙️", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Vivo V11 Hardware Telemetry", color = DexterOnBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("RAM Budget: ≤ 120 MB", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                            }
                        }
                        Button(
                            onClick = {
                                val ok = repository.vacuumDatabase()
                                if (ok) {
                                    Toast.makeText(context, "Database vacuumed & optimized successfully", Toast.LENGTH_SHORT).show()
                                    onRefresh()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                            modifier = Modifier.testTag("btn_vacuum_db")
                        ) {
                            Text("Vacuum & Optimize DB", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // RAM Gauge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active App Memory", color = DexterOnBackground, fontSize = 13.sp)
                        Text(
                            text = "${diag.ramUsedMb} MB / ${diag.ramBudgetMb} MB (${((diag.ramUsedMb.toFloat() / diag.ramBudgetMb) * 100).toInt()}%)",
                            color = if (diag.ramUsedMb <= 100) DexterSuccess else DexterWarning,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (diag.ramUsedMb.toFloat() / diag.ramBudgetMb).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (diag.ramUsedMb <= 100) DexterSuccess else DexterWarning,
                        trackColor = DexterSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DexterSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SQLite Database Size: ${diag.dbSizeKb} KB", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                        Text("Mode: Write-Ahead Logging (WAL)", color = DexterPrimary, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Model Status: ${diag.modelStatus}", color = DexterPrimaryGlow, fontSize = 12.sp)
                }
            }
        }

        item {
            Text("Recent System Audit Events", color = Color(0xFFA0A5BD), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }

        items(logs, key = { it.id }) { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(log.action, color = DexterPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(log.details, color = DexterOnBackground, fontSize = 12.sp)
                    }
                    Text(log.humanTime, color = Color(0xFFA0A5BD), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun GenericSegmentView(segment: SegmentInfo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(segment.icon, fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(segment.title, color = DexterOnBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(segment.description, color = Color(0xFFA0A5BD), fontSize = 14.sp)
    }
}
