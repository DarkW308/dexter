package com.aistudio.dexter.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aistudio.dexter.data.model.*
import com.aistudio.dexter.data.repository.DexterRepository
import com.aistudio.dexter.receiver.DexterAlarmReceiver
import com.aistudio.dexter.ui.components.ClockTime
import com.aistudio.dexter.ui.components.ModernClockTimePicker
import com.aistudio.dexter.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    var selectedFilter by remember { mutableStateOf("All") }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<ProjectItem?>(null) }
    var expandedProjectIds by remember { mutableStateOf(setOf<Long>()) }

    // Aggregate statistics across projects
    val totalMilestones = remember(projects) { projects.sumOf { it.taskCount } }
    val totalCompletedMilestones = remember(projects) {
        projects.sumOf { (it.taskCount * (it.progressPercent / 100f)).toInt() }
    }
    val avgVelocity = remember(projects) {
        if (projects.isNotEmpty()) projects.map { it.progressPercent }.average().toInt() else 0
    }

    val filteredProjects = remember(projects, selectedFilter) {
        when (selectedFilter) {
            "Active" -> projects.filter { !it.status.equals("COMPLETED", true) && it.progressPercent < 100 }
            "Completed" -> projects.filter { it.status.equals("COMPLETED", true) || it.progressPercent == 100 }
            else -> projects
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewProjectDialog = true },
                containerColor = DexterPrimary,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_project")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Initiative", tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Project", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                }
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Metric Overview Summary Strip (Purpose & Craft)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📁", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Initiatives & Deliverables",
                                        color = DexterOnBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        text = "High-level initiatives decomposed into sequential milestones",
                                        color = Color(0xFFA0A5BD),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3 Apple-Style Metric Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricPill(
                                label = "Initiatives",
                                value = "${projects.size}",
                                icon = "📁",
                                modifier = Modifier.weight(1f)
                            )
                            MetricPill(
                                label = "Milestones",
                                value = "$totalCompletedMilestones / $totalMilestones",
                                icon = "🏁",
                                modifier = Modifier.weight(1f)
                            )
                            MetricPill(
                                label = "Velocity",
                                value = "$avgVelocity%",
                                icon = "⚡",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Filter Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filters = listOf(
                                "All" to "All (${projects.size})",
                                "Active" to "Active (${projects.count { !it.status.equals("COMPLETED", true) && it.progressPercent < 100 }})",
                                "Completed" to "Completed (${projects.count { it.status.equals("COMPLETED", true) || it.progressPercent == 100 }})"
                            )
                            filters.forEach { (key, label) ->
                                val isSelected = selectedFilter == key
                                Surface(
                                    onClick = { selectedFilter = key },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) DexterPrimary.copy(alpha = 0.20f) else DexterSurfaceVariant,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, DexterPrimary) else null,
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) DexterPrimary else Color(0xFFA0A5BD)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Empty State
            if (filteredProjects.isEmpty()) {
                item {
                    AppleEmptyState(
                        icon = "📁",
                        title = if (projects.isEmpty()) "No Initiatives Yet" else "No Matching Projects",
                        description = if (projects.isEmpty())
                            "Break down strategic outcomes into clear, sequential milestones and track delivery velocity."
                        else
                            "No projects match the '$selectedFilter' filter. Tap 'All' or create a new initiative.",
                        actionLabel = if (projects.isEmpty()) "+ Create First Project" else null,
                        onAction = { showNewProjectDialog = true }
                    )
                }
            }

            // 3. Project Detail Cards
            items(filteredProjects, key = { it.id }) { project ->
                val isExpanded = expandedProjectIds.contains(project.id)
                ProjectDetailCard(
                    project = project,
                    isExpanded = isExpanded,
                    repository = repository,
                    onToggleExpand = {
                        expandedProjectIds = if (isExpanded) {
                            expandedProjectIds - project.id
                        } else {
                            expandedProjectIds + project.id
                        }
                    },
                    onDelete = { projectToDelete = project },
                    onStatusChange = { newStatus ->
                        repository.updateProjectStatus(project.id, newStatus)
                        onRefresh()
                    },
                    onMilestoneChanged = {
                        onRefresh()
                    }
                )
            }
        }
    }

    // Delete Confirmation Dialog (Responsibility Lens)
    projectToDelete?.let { proj ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Delete Project?", color = DexterOnBackground, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${proj.name}\" and all of its associated milestones? This action cannot be undone.",
                    color = Color(0xFFA0A5BD),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteProject(proj.id)
                        projectToDelete = null
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF6679))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // New Project Dialog
    if (showNewProjectDialog) {
        CreateProjectDialog(
            repository = repository,
            onDismiss = { showNewProjectDialog = false },
            onProjectCreated = {
                showNewProjectDialog = false
                onRefresh()
            }
        )
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = DexterSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(label, color = Color(0xFFA0A5BD), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = DexterOnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AppleEmptyState(
    icon: String,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DexterSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(DexterPrimary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 32.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = DexterOnBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                color = Color(0xFFA0A5BD),
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
            if (actionLabel != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(actionLabel, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun ProjectDetailCard(
    project: ProjectItem,
    isExpanded: Boolean,
    repository: DexterRepository,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (String) -> Unit,
    onMilestoneChanged: () -> Unit
) {
    var milestoneTrigger by remember { mutableStateOf(0) }
    val milestones = remember(isExpanded, milestoneTrigger) {
        if (isExpanded) repository.listMilestones(project.id) else emptyList()
    }
    var newMilestoneInput by remember { mutableStateOf("") }

    // Smooth animated progress bar
    val animatedProgress by animateFloatAsState(
        targetValue = (project.progressPercent / 100f).coerceIn(0f, 1f),
        label = "ProjectProgress"
    )

    // Status colors
    val statusColor = when (project.status.uppercase()) {
        "COMPLETED" -> Color(0xFF00E5FF)
        "ON TRACK", "ACTIVE" -> Color(0xFF34D399)
        "AT RISK" -> Color(0xFFFBBF24)
        "BLOCKED" -> Color(0xFFF87171)
        else -> DexterPrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("project_card_${project.id}"),
        colors = CardDefaults.cardColors(containerColor = DexterSurface),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top Row: Title, Status Badge, and Expand Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        color = DexterOnBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 22.sp
                    )

                    if (project.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = project.description,
                            color = Color(0xFFA0A5BD),
                            fontSize = 13.sp,
                            maxLines = if (isExpanded) 10 else 2,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Interactive Status Pill
                Surface(
                    onClick = {
                        val nextStatus = when (project.status.uppercase()) {
                            "ACTIVE", "ON TRACK" -> "AT RISK"
                            "AT RISK" -> "COMPLETED"
                            "COMPLETED" -> "ACTIVE"
                            else -> "ACTIVE"
                        }
                        onStatusChange(nextStatus)
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                    modifier = Modifier.height(28.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(statusColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = project.status.uppercase(),
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Deadline Chip (if specified)
            if (project.deadline.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(DexterSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Target Deadline",
                        tint = DexterPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Target: ${project.deadline}",
                        color = DexterOnBackground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Metrics & Track
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${project.taskCount} milestones total",
                    color = Color(0xFFA0A5BD),
                    fontSize = 12.sp
                )
                Text(
                    text = "${project.progressPercent}%",
                    color = if (project.progressPercent == 100) Color(0xFF34D399) else DexterPrimaryGlow,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (project.progressPercent == 100) Color(0xFF34D399) else DexterPrimary,
                trackColor = DexterSurfaceVariant
            )

            // Celebration chip if 100% complete
            if (project.progressPercent == 100 && project.taskCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF34D399).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎉", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "All Milestones Delivered! Initiative Completed.",
                        color = Color(0xFF34D399),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expand/Collapse Chevron Button (Agency & Familiarity)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Hide Milestones" else "Show Milestones (${project.taskCount})",
                    color = DexterPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = DexterPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expanded In-Card Milestone Checklist
            if (isExpanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color.White.copy(alpha = 0.08f)
                )

                Text(
                    text = "Milestones & Deliverables",
                    color = DexterOnBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Checklist Items
                if (milestones.isEmpty()) {
                    Text(
                        text = "No milestones added yet. Add checklist items below to track execution.",
                        color = Color(0xFFA0A5BD),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        milestones.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DexterSurfaceVariant, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Apple-style circular checkbox
                                IconButton(
                                    onClick = {
                                        repository.toggleMilestone(item.id, !item.isCompleted, project.id)
                                        milestoneTrigger++
                                        onMilestoneChanged()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    if (item.isCompleted) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .background(DexterPrimary, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Completed",
                                                tint = Color.Black,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .border(2.dp, Color(0xFFA0A5BD), CircleShape)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = item.title,
                                    color = if (item.isCompleted) Color(0xFFA0A5BD) else DexterOnBackground,
                                    fontSize = 14.sp,
                                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = {
                                        repository.deleteMilestone(item.id, project.id)
                                        milestoneTrigger++
                                        onMilestoneChanged()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Milestone",
                                        tint = Color(0xFFCF6679),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Inline Milestone Addition (Simplicity & Craft)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newMilestoneInput,
                        onValueChange = { newMilestoneInput = it },
                        placeholder = { Text("+ Add milestone...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_new_milestone_${project.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (newMilestoneInput.isNotBlank()) {
                                repository.createMilestone(project.id, newMilestoneInput)
                                newMilestoneInput = ""
                                milestoneTrigger++
                                onMilestoneChanged()
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .background(DexterPrimary, CircleShape)
                            .testTag("btn_add_milestone_${project.id}")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Project Action Buttons: Delete project
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCF6679))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete Initiative", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateProjectDialog(
    repository: DexterRepository,
    onDismiss: () -> Unit,
    onProjectCreated: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("ACTIVE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DexterSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📁", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Initiative", color = DexterOnBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project Title") },
                    placeholder = { Text("e.g. Dexter Offline Engine V2") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DexterOnBackground,
                        unfocusedTextColor = DexterOnBackground,
                        focusedBorderColor = DexterPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_project_title")
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Deliverable Scope / Summary") },
                    placeholder = { Text("Key outcomes and success criteria...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DexterOnBackground,
                        unfocusedTextColor = DexterOnBackground,
                        focusedBorderColor = DexterPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_project_desc")
                )

                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Target Deadline") },
                    placeholder = { Text("e.g. Dec 15, 2026") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DexterOnBackground,
                        unfocusedTextColor = DexterOnBackground,
                        focusedBorderColor = DexterPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_project_deadline")
                )

                // Starting Status Selector
                Column {
                    Text("Initial Health Status", color = Color(0xFFA0A5BD), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ACTIVE", "ON TRACK", "AT RISK").forEach { st ->
                            val isSel = status == st
                            Surface(
                                onClick = { status = st },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) DexterPrimary.copy(alpha = 0.25f) else DexterSurfaceVariant,
                                border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, DexterPrimary) else null,
                                modifier = Modifier.height(30.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = st,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) DexterPrimary else Color(0xFFA0A5BD)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val created = repository.createProject(title, desc, deadline)
                        if (status != "ACTIVE") {
                            repository.updateProjectStatus(created.id, status)
                        }
                        onProjectCreated()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create Project", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFA0A5BD))
            }
        }
    )
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
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalToUpdate by remember { mutableStateOf<GoalItem?>(null) }
    var goalToDelete by remember { mutableStateOf<GoalItem?>(null) }

    val totalGoals = goals.size
    val completedGoals = remember(goals) { goals.count { it.progressPercent >= 100 || it.status.equals("COMPLETED", true) } }
    val avgVelocity = remember(goals) {
        if (goals.isNotEmpty()) goals.map { it.progressPercent }.average().toInt() else 0
    }

    val filteredGoals = remember(goals, selectedCategoryFilter) {
        when (selectedCategoryFilter) {
            "All" -> goals
            "Completed" -> goals.filter { it.progressPercent >= 100 || it.status.equals("COMPLETED", true) }
            else -> goals.filter { it.category.equals(selectedCategoryFilter, true) }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddGoalDialog = true },
                containerColor = DexterPrimary,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_goal")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Goal", tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Goal", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                }
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Metric Overview Summary Strip (Purpose & Craft)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎯", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Long-Term Goals & Vision",
                                        color = DexterOnBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        text = "High-stakes quarterly and annual outcomes tracked incrementally",
                                        color = Color(0xFFA0A5BD),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3 Apple-Style Metric Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricPill(
                                label = "Total Goals",
                                value = "$totalGoals",
                                icon = "🎯",
                                modifier = Modifier.weight(1f)
                            )
                            MetricPill(
                                label = "Achieved",
                                value = "$completedGoals",
                                icon = "🏆",
                                modifier = Modifier.weight(1f)
                            )
                            MetricPill(
                                label = "Velocity",
                                value = "$avgVelocity%",
                                icon = "📊",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Category Filter Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val categories = listOf(
                                "All" to "All (${goals.size})",
                                "Financial" to "Financial",
                                "Health" to "Health",
                                "Career" to "Career",
                                "Personal" to "Personal"
                            )
                            categories.forEach { (catKey, catLabel) ->
                                val isSelected = selectedCategoryFilter == catKey
                                Surface(
                                    onClick = { selectedCategoryFilter = catKey },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) DexterPrimary.copy(alpha = 0.20f) else DexterSurfaceVariant,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, DexterPrimary) else null,
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = catLabel,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) DexterPrimary else Color(0xFFA0A5BD)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Empty State
            if (filteredGoals.isEmpty()) {
                item {
                    AppleEmptyState(
                        icon = "🎯",
                        title = if (goals.isEmpty()) "No Goals Set Yet" else "No Matching Goals",
                        description = if (goals.isEmpty())
                            "Define meaningful financial, health, career, or personal targets and measure your incremental progress over time."
                        else
                            "No goals found in the '$selectedCategoryFilter' category. Tap 'All' or create a new goal.",
                        actionLabel = if (goals.isEmpty()) "+ Set First Goal" else null,
                        onAction = { showAddGoalDialog = true }
                    )
                }
            }

            // 3. Goal Cards
            items(filteredGoals, key = { it.id }) { goal ->
                GoalCardItem(
                    goal = goal,
                    onUpdateClick = { goalToUpdate = goal },
                    onDeleteClick = { goalToDelete = goal },
                    onQuickIncrement = { delta ->
                        repository.quickIncrementGoal(goal.id, delta)
                        onRefresh()
                    }
                )
            }
        }
    }

    // Delete Confirmation Guardrail (Responsibility Lens)
    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Delete Goal?", color = DexterOnBackground, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${goal.name}\"? All logged progress towards this target will be removed.",
                    color = Color(0xFFA0A5BD),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteGoal(goal.id)
                        goalToDelete = null
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF6679))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // Create Goal Dialog
    if (showAddGoalDialog) {
        CreateGoalDialog(
            repository = repository,
            onDismiss = { showAddGoalDialog = false },
            onGoalCreated = {
                showAddGoalDialog = false
                onRefresh()
            }
        )
    }

    // Update Progress Dialog
    goalToUpdate?.let { goal ->
        UpdateGoalProgressDialog(
            goal = goal,
            repository = repository,
            onDismiss = { goalToUpdate = null },
            onUpdated = {
                goalToUpdate = null
                onRefresh()
            }
        )
    }
}

@Composable
private fun GoalCardItem(
    goal: GoalItem,
    onUpdateClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onQuickIncrement: (Double) -> Unit
) {
    // Smooth animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = (goal.progressPercent / 100f).coerceIn(0f, 1f),
        label = "GoalProgress"
    )

    val categoryColor = when (goal.category.lowercase()) {
        "financial" -> Color(0xFF34D399) // Emerald
        "health" -> Color(0xFFFB7185)    // Coral Rose
        "career" -> Color(0xFF818CF8)    // Violet Indigo
        "personal" -> Color(0xFF38BDF8)  // Electric Sky Blue
        else -> DexterPrimary
    }

    val deficit = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_card_${goal.id}"),
        colors = CardDefaults.cardColors(containerColor = DexterSurface),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top Section: Apple-Style Activity Ring + Title & Meta
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Activity Ring Meter
                GoalProgressRing(
                    progress = animatedProgress,
                    color = categoryColor,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = goal.name,
                            color = DexterOnBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )

                        // Category Pill Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = categoryColor.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.35f)),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = goal.category.uppercase(),
                                    color = categoryColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (goal.targetDate.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = Color(0xFFA0A5BD),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Target: ${goal.targetDate}",
                                color = Color(0xFFA0A5BD),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Numbers Display: Current vs Target + Deficit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Current Progress",
                        color = Color(0xFFA0A5BD),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${goal.metricUnit}${formatNumber(goal.currentAmount)} / ${goal.metricUnit}${formatNumber(goal.targetAmount)}",
                        color = DexterOnBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (goal.progressPercent >= 100) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF34D399).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "🏆 Target Met!",
                            color = Color(0xFF34D399),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "${goal.metricUnit}${formatNumber(deficit)} to go",
                        color = DexterPrimaryGlow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Linear Progress Track
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = categoryColor,
                trackColor = DexterSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Direct-Action Increment Pills (Agency & Delight Lens)
            val isCurrency = goal.metricUnit == "₹" || goal.metricUnit == "$"
            val quickIncrements = if (isCurrency) listOf(500.0, 1000.0, 5000.0) else listOf(1.0, 5.0, 10.0)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    quickIncrements.forEach { delta ->
                        val label = if (isCurrency) {
                            "+${goal.metricUnit}${if (delta >= 1000) "${(delta / 1000).toInt()}k" else "${delta.toInt()}"}"
                        } else {
                            "+${delta.toInt()} ${goal.metricUnit}"
                        }
                        Surface(
                            onClick = { onQuickIncrement(delta) },
                            shape = RoundedCornerShape(10.dp),
                            color = DexterSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = DexterPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Goal",
                            tint = Color(0xFFCF6679),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Button(
                        onClick = onUpdateClick,
                        colors = ButtonDefaults.buttonColors(containerColor = DexterSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("btn_update_goal_${goal.id}")
                    ) {
                        Text("Update", color = DexterPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = 6.5.dp.toPx()
            val radius = (size.minDimension - strokeWidthPx) / 2f
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)

            // Background Track Ring
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidthPx)
            )

            // Foreground Animated Progress Ring
            val sweepAngle = (progress * 360f).coerceIn(0f, 360f)
            if (sweepAngle > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
        }

        // Center Percentage Text
        Text(
            text = "${(progress * 100).toInt()}%",
            color = DexterOnBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CreateGoalDialog(
    repository: DexterRepository,
    onDismiss: () -> Unit,
    onGoalCreated: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Financial") }
    var targetAmount by remember { mutableStateOf("50000") }
    var targetDate by remember { mutableStateOf("Dec 31, 2026") }
    var metricUnit by remember { mutableStateOf("₹") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DexterSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎯", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set Long-Term Goal", color = DexterOnBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Title") },
                    placeholder = { Text("e.g. Emergency Fund or 10k Run") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DexterOnBackground,
                        unfocusedTextColor = DexterOnBackground,
                        focusedBorderColor = DexterPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_goal_title")
                )

                // Category Selector Chips
                Column {
                    Text("Category", color = Color(0xFFA0A5BD), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Financial" to "💰", "Health" to "🏃", "Career" to "🚀", "Personal" to "🌟").forEach { (cat, emoji) ->
                            val isSel = category == cat
                            Surface(
                                onClick = {
                                    category = cat
                                    if (cat == "Financial" && metricUnit != "₹") metricUnit = "₹"
                                    if (cat == "Health" && metricUnit == "₹") metricUnit = "km"
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) DexterPrimary.copy(alpha = 0.25f) else DexterSurfaceVariant,
                                border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, DexterPrimary) else null,
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(emoji, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) DexterPrimary else Color(0xFFA0A5BD)
                                    )
                                }
                            }
                        }
                    }
                }

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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(2f)
                            .testTag("input_goal_target_amount")
                    )
                    OutlinedTextField(
                        value = metricUnit,
                        onValueChange = { metricUnit = it },
                        label = { Text("Unit") },
                        placeholder = { Text("₹ / km") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_goal_unit")
                    )
                }

                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("Target Deadline") },
                    placeholder = { Text("e.g. Dec 31, 2026") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DexterOnBackground,
                        unfocusedTextColor = DexterOnBackground,
                        focusedBorderColor = DexterPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_goal_target_date")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val amount = targetAmount.toDoubleOrNull() ?: 0.0
                        repository.createGoal(name, category, amount, targetDate, metricUnit)
                        onGoalCreated()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Set Goal", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFA0A5BD))
            }
        }
    )
}

@Composable
private fun UpdateGoalProgressDialog(
    goal: GoalItem,
    repository: DexterRepository,
    onDismiss: () -> Unit,
    onUpdated: () -> Unit
) {
    var currentInput by remember { mutableStateOf(goal.currentAmount.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DexterSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📈", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Update Progress", color = DexterOnBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Goal: ${goal.name}", color = Color(0xFFA0A5BD), fontSize = 13.sp)

                OutlinedTextField(
                    value = currentInput,
                    onValueChange = { currentInput = it },
                    label = { Text("Current ${goal.metricUnit} Achieved") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DexterOnBackground,
                        unfocusedTextColor = DexterOnBackground,
                        focusedBorderColor = DexterPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_update_goal_amount")
                )

                // Percentage Quick Presets
                Column {
                    Text("Or set directly to percentage:", color = Color(0xFFA0A5BD), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(25, 50, 75, 100).forEach { pct ->
                            Surface(
                                onClick = {
                                    val computed = (goal.targetAmount * (pct / 100.0)).toInt()
                                    currentInput = computed.toString()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = DexterSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("$pct%", color = DexterPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val curr = currentInput.toDoubleOrNull() ?: goal.currentAmount
                    repository.updateGoalProgress(goal.id, curr, goal.targetAmount)
                    onUpdated()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFA0A5BD))
            }
        }
    )
}

private fun formatNumber(value: Double): String {
    return if (value >= 1_000_000) {
        String.format(java.util.Locale.US, "%.1fM", value / 1_000_000)
    } else if (value >= 1_000) {
        if (value % 1_000 == 0.0) {
            String.format(java.util.Locale.US, "%,d", value.toLong())
        } else {
            String.format(java.util.Locale.US, "%,.1f", value)
        }
    } else {
        if (value % 1.0 == 0.0) "${value.toLong()}" else String.format(java.util.Locale.US, "%.1f", value)
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
    val context = LocalContext.current
    val alarms = remember(trigger) { repository.listAlarms() }
    var showSetAlarmSheet by remember { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<AlarmItem?>(null) }
    var alarmToDelete by remember { mutableStateOf<AlarmItem?>(null) }
    var selectedFilter by remember { mutableStateOf("All") }

    fun computeNextTriggerMillis(alarmTimeMillis: Long): Long {
        val calNow = Calendar.getInstance()
        val calAlarm = Calendar.getInstance().apply { timeInMillis = alarmTimeMillis }
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, calAlarm.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, calAlarm.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= calNow.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    fun formatCountdown(targetMillis: Long): String {
        val diff = targetMillis - System.currentTimeMillis()
        if (diff <= 0) return "Due now"
        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff / (1000 * 60)) % 60
        return when {
            hours > 0 && minutes > 0 -> "In ${hours}h ${minutes}m"
            hours > 0 -> "In ${hours}h"
            minutes > 0 -> "In ${minutes}m"
            else -> "In <1m"
        }
    }

    val activeAlarms = remember(alarms) { alarms.filter { it.isEnabled } }
    val nextUpcomingAlarm = remember(activeAlarms) {
        activeAlarms.minByOrNull { computeNextTriggerMillis(it.alarmTime) }
    }

    val filteredAlarms = remember(alarms, selectedFilter) {
        when (selectedFilter) {
            "Active" -> alarms.filter { it.isEnabled }
            "Repeating" -> alarms.filter {
                !it.repeatDays.equals("Once", true) &&
                !it.repeatDays.equals("One-time", true) &&
                it.repeatDays.isNotBlank()
            }
            else -> alarms
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    alarmToEdit = null
                    showSetAlarmSheet = true
                },
                containerColor = DexterPrimary,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_alarm")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Set Alarm", tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Set Alarm", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                }
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Metric Overview Summary Strip (Apple Clock HIG)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(DexterPrimary.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⏰", fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Alarms & Timers",
                                        color = DexterOnBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        text = "Doze-safe offline wake-up & timers",
                                        color = Color(0xFFA0A5BD),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (nextUpcomingAlarm != null) {
                                val nextTrigger = computeNextTriggerMillis(nextUpcomingAlarm.alarmTime)
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = DexterPrimary.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DexterPrimary.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(DexterPrimary)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = formatCountdown(nextTrigger),
                                            color = DexterPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Pill Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Total
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = DexterBackground.copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("TOTAL", color = Color(0xFFA0A5BD), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${alarms.size}", color = DexterOnBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Active
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = DexterBackground.copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("ACTIVE", color = Color(0xFFA0A5BD), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "${activeAlarms.size}",
                                        color = if (activeAlarms.isNotEmpty()) DexterPrimary else DexterOnBackground,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Next Ring Time
                            Surface(
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(14.dp),
                                color = DexterBackground.copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("NEXT ALARM", color = Color(0xFFA0A5BD), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = nextUpcomingAlarm?.humanTime ?: "None Active",
                                        color = if (nextUpcomingAlarm != null) Color(0xFFFF9F0A) else Color(0xFFA0A5BD),
                                        fontSize = if (nextUpcomingAlarm != null) 16.sp else 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. 1-Tap Quick Presets Strip (Apple Agency & Delight)
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QUICK TIMED PRESETS",
                            color = Color(0xFFA0A5BD),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "1-Tap Schedule",
                            color = DexterPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val presets = listOf(
                        Triple("⚡ +15m Break", 15, "Quick Break"),
                        Triple("☕ +30m Power Nap", 30, "Power Nap"),
                        Triple("🎯 +45m Deep Work", 45, "Deep Work Sprint"),
                        Triple("⏱️ +1h Focus", 60, "1-Hour Focus"),
                        Triple("🌅 Tomorrow 7:00 AM", -1, "Morning Wake Up"),
                        Triple("🌙 Tonight 10:30 PM", -2, "Night Wind Down")
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(presets) { (title, offsetMin, label) ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = DexterSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .clickable {
                                        val cal = Calendar.getInstance()
                                        val targetMillis = when (offsetMin) {
                                            -1 -> {
                                                cal.apply {
                                                    add(Calendar.DAY_OF_YEAR, 1)
                                                    set(Calendar.HOUR_OF_DAY, 7)
                                                    set(Calendar.MINUTE, 0)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }.timeInMillis
                                            }
                                            -2 -> {
                                                cal.apply {
                                                    set(Calendar.HOUR_OF_DAY, 22)
                                                    set(Calendar.MINUTE, 30)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                    if (timeInMillis <= System.currentTimeMillis()) {
                                                        add(Calendar.DAY_OF_YEAR, 1)
                                                    }
                                                }.timeInMillis
                                            }
                                            else -> {
                                                cal.apply {
                                                    add(Calendar.MINUTE, offsetMin)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }.timeInMillis
                                            }
                                        }

                                        val repeat = if (offsetMin < 0) "Daily" else "Once"
                                        val alarmId = repository.createAlarm(targetMillis, label, repeat, vibrate = true)
                                        DexterAlarmReceiver.scheduleExactAlarm(context, alarmId, label, targetMillis / 1000L)
                                        Toast.makeText(context, "Alarm set: $label", Toast.LENGTH_SHORT).show()
                                        onRefresh()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = title,
                                        color = DexterOnBackground,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Segmented Filter Tabs (Apple HIG Familiarity)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Active", "Repeating").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        val count = when (filter) {
                            "Active" -> activeAlarms.size
                            "Repeating" -> alarms.count { !it.repeatDays.equals("Once", true) && it.repeatDays.isNotBlank() }
                            else -> alarms.size
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFilter = filter },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) DexterPrimary else DexterSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) DexterPrimary else Color.White.copy(alpha = 0.08f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$filter ($count)",
                                    color = if (isSelected) Color.Black else DexterOnBackground,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 4. Alarm Cards List
            if (filteredAlarms.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = DexterSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(DexterBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⏰", fontSize = 32.sp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedFilter == "All") "No Alarms Set" else "No $selectedFilter Alarms",
                                color = DexterOnBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Set doze-safe alarms and timed reminders that trigger reliably offline.",
                                color = Color(0xFFA0A5BD),
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    alarmToEdit = null
                                    showSetAlarmSheet = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Set First Alarm", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(filteredAlarms, key = { it.id }) { alarm ->
                    val isEnabled = alarm.isEnabled
                    val nextTrigger = computeNextTriggerMillis(alarm.alarmTime)
                    val rawHuman = alarm.humanTime
                    val timeParts = rawHuman.split(" ")
                    val timeString = if (timeParts.isNotEmpty()) timeParts[0] else rawHuman
                    val amPmString = if (timeParts.size > 1) timeParts[1] else ""

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("alarm_card_${alarm.id}")
                            .clickable {
                                alarmToEdit = alarm
                                showSetAlarmSheet = true
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEnabled) DexterSurface else DexterSurface.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isEnabled) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.04f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                // Large Digital Clock Typography (Apple Clock Style)
                                Row(
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = timeString,
                                        color = if (isEnabled) DexterOnBackground else Color.White.copy(alpha = 0.35f),
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp
                                    )
                                    if (amPmString.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = amPmString,
                                            color = if (isEnabled) Color(0xFFFF9F0A) else Color.White.copy(alpha = 0.30f),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Alarm Label
                                Text(
                                    text = alarm.label.ifBlank { "Alarm" },
                                    color = if (isEnabled) DexterOnBackground.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.35f),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Meta Tags Row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Repeat chip
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = DexterBackground.copy(alpha = 0.8f)
                                    ) {
                                        Text(
                                            text = "🔁 ${alarm.repeatDays}",
                                            color = if (isEnabled) Color(0xFFA0A5BD) else Color.White.copy(alpha = 0.30f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }

                                    // Vibrate chip
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = DexterBackground.copy(alpha = 0.8f)
                                    ) {
                                        Text(
                                            text = if (alarm.vibrate) "📳 Vibrate" else "🔕 Silent",
                                            color = if (isEnabled) Color(0xFFA0A5BD) else Color.White.copy(alpha = 0.30f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }

                                    // Countdown pill if enabled
                                    if (isEnabled) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = DexterPrimary.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "⏱️ ${formatCountdown(nextTrigger)}",
                                                color = DexterPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Controls Right Column
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { checked ->
                                        repository.toggleAlarm(alarm.id, checked)
                                        if (checked) {
                                            val triggerSec = computeNextTriggerMillis(alarm.alarmTime) / 1000L
                                            DexterAlarmReceiver.scheduleExactAlarm(context, alarm.id, alarm.label, triggerSec)
                                            Toast.makeText(context, "Alarm enabled: ${alarm.label}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            DexterAlarmReceiver.cancelExactAlarm(context, alarm.id)
                                            Toast.makeText(context, "Alarm disabled", Toast.LENGTH_SHORT).show()
                                        }
                                        onRefresh()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = DexterPrimary,
                                        uncheckedThumbColor = Color(0xFFA0A5BD),
                                        uncheckedTrackColor = DexterBackground
                                    ),
                                    modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            alarmToEdit = alarm
                                            showSetAlarmSheet = true
                                        },
                                        modifier = Modifier.size(36.dp).testTag("alarm_edit_${alarm.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Alarm",
                                            tint = Color(0xFFA0A5BD),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { alarmToDelete = alarm },
                                        modifier = Modifier.size(36.dp).testTag("alarm_delete_${alarm.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Alarm",
                                            tint = Color(0xFFFF453A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 5. Deletion Guardrail Dialog (Apple Responsibility)
    alarmToDelete?.let { alarm ->
        AlertDialog(
            onDismissRequest = { alarmToDelete = null },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Delete Alarm?",
                    color = DexterOnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${alarm.label}' set for ${alarm.humanTime}? This cannot be undone.",
                    color = Color(0xFFA0A5BD),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        DexterAlarmReceiver.cancelExactAlarm(context, alarm.id)
                        repository.deleteAlarm(alarm.id)
                        alarmToDelete = null
                        Toast.makeText(context, "Alarm deleted", Toast.LENGTH_SHORT).show()
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_confirm_delete_alarm")
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { alarmToDelete = null },
                    modifier = Modifier.testTag("btn_cancel_delete_alarm")
                ) {
                    Text("Keep Alarm", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // 6. Set / Edit Alarm Sheet Dialog (Apple Craft & Progressive Disclosure)
    if (showSetAlarmSheet) {
        val editing = alarmToEdit
        var label by remember { mutableStateOf(editing?.label ?: "") }
        var alarmTime by remember {
            mutableStateOf(
                if (editing != null) {
                    val cal = Calendar.getInstance().apply { timeInMillis = editing.alarmTime }
                    ClockTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                } else {
                    ClockTime(7, 0)
                }
            )
        }
        var repeatDays by remember { mutableStateOf(editing?.repeatDays ?: "Mon, Wed, Fri") }
        var vibrate by remember { mutableStateOf(editing?.vibrate ?: true) }

        val weekDaysList = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        AlertDialog(
            onDismissRequest = {
                showSetAlarmSheet = false
                alarmToEdit = null
            },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (editing != null) "Edit Alarm" else "Set New Alarm",
                        color = DexterOnBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                    Text(
                        text = alarmTime.formattedString,
                        color = DexterPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Time Picker
                    ModernClockTimePicker(
                        label = "ALARM TIME",
                        time = alarmTime,
                        onTimeChanged = { alarmTime = it },
                        presets = listOf(
                            "⏰ 06:30 AM" to ClockTime(6, 30),
                            "⏰ 07:00 AM" to ClockTime(7, 0),
                            "⏰ 08:00 AM" to ClockTime(8, 0),
                            "⏰ 09:30 AM" to ClockTime(9, 30),
                            "🌙 10:30 PM" to ClockTime(22, 30)
                        )
                    )

                    // Label Input
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Alarm Label") },
                        placeholder = { Text("e.g. Morning Workout, Daily Standup") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_alarm_label"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedContainerColor = DexterBackground.copy(alpha = 0.5f),
                            unfocusedContainerColor = DexterBackground.copy(alpha = 0.5f)
                        ),
                        singleLine = true
                    )

                    // Repeat Days Section
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "REPEAT SCHEDULE",
                            color = Color(0xFFA0A5BD),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        // Quick Presets
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Everyday", "Weekdays", "Weekends", "Once").forEach { preset ->
                                val isSelected = when (preset) {
                                    "Everyday" -> repeatDays == "Everyday" || repeatDays == "Mon, Tue, Wed, Thu, Fri, Sat, Sun"
                                    "Weekdays" -> repeatDays == "Weekdays" || repeatDays == "Mon, Tue, Wed, Thu, Fri"
                                    "Weekends" -> repeatDays == "Weekends" || repeatDays == "Sat, Sun"
                                    "Once" -> repeatDays == "Once" || repeatDays == "One-time"
                                    else -> false
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) DexterPrimary.copy(alpha = 0.2f) else DexterBackground.copy(alpha = 0.6f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) DexterPrimary else Color.White.copy(alpha = 0.08f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { repeatDays = preset }
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = preset,
                                            color = if (isSelected) DexterPrimary else DexterOnBackground,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // Day of week individual chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            weekDaysList.forEach { day ->
                                val active = repeatDays.contains(day) ||
                                        (repeatDays == "Everyday") ||
                                        (repeatDays == "Weekdays" && day !in listOf("Sat", "Sun")) ||
                                        (repeatDays == "Weekends" && day in listOf("Sat", "Sun"))

                                Surface(
                                    shape = CircleShape,
                                    color = if (active) DexterPrimary else DexterBackground,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (active) DexterPrimary else Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clickable {
                                            val currentDays = when (repeatDays) {
                                                "Everyday" -> weekDaysList.toMutableList()
                                                "Weekdays" -> mutableListOf("Mon", "Tue", "Wed", "Thu", "Fri")
                                                "Weekends" -> mutableListOf("Sat", "Sun")
                                                "Once" -> mutableListOf()
                                                else -> repeatDays.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                                            }

                                            if (currentDays.contains(day)) {
                                                currentDays.remove(day)
                                            } else {
                                                currentDays.add(day)
                                            }

                                            repeatDays = when {
                                                currentDays.isEmpty() -> "Once"
                                                currentDays.size == 7 -> "Everyday"
                                                currentDays.size == 5 && currentDays.containsAll(listOf("Mon", "Tue", "Wed", "Thu", "Fri")) -> "Weekdays"
                                                currentDays.size == 2 && currentDays.containsAll(listOf("Sat", "Sun")) -> "Weekends"
                                                else -> weekDaysList.filter { currentDays.contains(it) }.joinToString(", ")
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = day.take(1),
                                            color = if (active) Color.Black else Color(0xFFA0A5BD),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Vibration Toggle
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DexterBackground.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📳", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Vibrate on Ring", color = DexterOnBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("Tactile haptic alarm alert", color = Color(0xFFA0A5BD), fontSize = 11.sp)
                                }
                            }
                            Switch(
                                checked = vibrate,
                                onCheckedChange = { vibrate = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = DexterPrimary
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val alarmMillis = alarmTime.toMillisToday()
                        val finalLabel = label.ifBlank { "Alarm" }
                        if (editing != null) {
                            repository.updateAlarm(editing.id, alarmMillis, finalLabel, repeatDays, vibrate)
                            val triggerSec = computeNextTriggerMillis(alarmMillis) / 1000L
                            DexterAlarmReceiver.scheduleExactAlarm(context, editing.id, finalLabel, triggerSec)
                            Toast.makeText(context, "Alarm updated", Toast.LENGTH_SHORT).show()
                        } else {
                            val id = repository.createAlarm(alarmMillis, finalLabel, repeatDays, vibrate)
                            val triggerSec = computeNextTriggerMillis(alarmMillis) / 1000L
                            DexterAlarmReceiver.scheduleExactAlarm(context, id, finalLabel, triggerSec)
                            Toast.makeText(context, "Alarm set for ${alarmTime.formattedString}", Toast.LENGTH_SHORT).show()
                        }
                        showSetAlarmSheet = false
                        alarmToEdit = null
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_save_alarm")
                ) {
                    Text(
                        text = if (editing != null) "Update Alarm" else "Save Alarm",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSetAlarmSheet = false
                        alarmToEdit = null
                    }
                ) {
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
