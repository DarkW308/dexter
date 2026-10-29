package com.aistudio.dexter.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.aistudio.dexter.ui.components.ModernDeadlinePickerField
import com.aistudio.dexter.ui.components.ModernDatePickerDialog
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
    androidx.compose.runtime.LaunchedEffect(Unit) {
        onOpenTasks()
    }
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
    var showEditDeadlineDialog by remember { mutableStateOf(false) }
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

            // Target Deadline Chip (Interactive Calendar Picker)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                onClick = { showEditDeadlineDialog = true },
                shape = RoundedCornerShape(8.dp),
                color = DexterSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.height(28.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Target Deadline",
                        tint = DexterPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (project.deadline.isNotBlank()) "Target: ${project.deadline}" else "+ Set Target Deadline",
                        color = if (project.deadline.isNotBlank()) DexterOnBackground else DexterPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Deadline",
                        tint = Color(0xFFA0A5BD),
                        modifier = Modifier.size(11.dp)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Milestones & Deliverables",
                        color = DexterOnBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DexterPrimary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Manual & AI Managed",
                            color = DexterPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Track deliverables step-by-step. Add milestones below manually, or speak to Dexter AI to auto-plan them. Checking items updates project progress immediately.",
                    color = Color(0xFFA0A5BD),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

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

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Milestone Phase Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Requirements", "Architecture", "Core Build", "QA & Polish", "Release").forEach { phase ->
                        Surface(
                            onClick = {
                                repository.createMilestone(project.id, phase)
                                milestoneTrigger++
                                onMilestoneChanged()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = DexterSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+ $phase",
                                    fontSize = 11.sp,
                                    color = DexterPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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

    if (showEditDeadlineDialog) {
        ModernDatePickerDialog(
            currentDateString = project.deadline,
            onDateSelected = { _, formattedDate ->
                repository.updateProjectDeadline(project.id, formattedDate)
                onMilestoneChanged()
            },
            onDismiss = { showEditDeadlineDialog = false }
        )
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
    var newMilestoneText by remember { mutableStateOf("") }
    val initialMilestones = remember { mutableStateListOf<String>() }

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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

                // Modern Calendar & Year Deadline Picker
                ModernDeadlinePickerField(
                    deadline = deadline,
                    onDeadlineChange = { deadline = it },
                    label = "Target Deadline (Calendar & Year)",
                    placeholder = "Tap calendar to select date & year...",
                    testTagPrefix = "input_project_deadline"
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

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Initial Milestones Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏁 Initial Milestones (${initialMilestones.size})",
                            color = DexterOnBackground,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DexterPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Optional",
                                color = DexterPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "You can add milestones now or create them anytime after the project is created. Dexter AI can also auto-plan them via voice.",
                        color = Color(0xFFA0A5BD),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    // Quick Suggestion Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Requirements", "Architecture", "Core Build", "QA & Polish", "Launch").forEach { phase ->
                            Surface(
                                onClick = {
                                    if (!initialMilestones.contains(phase)) {
                                        initialMilestones.add(phase)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = DexterSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+ $phase",
                                        fontSize = 11.sp,
                                        color = DexterPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Milestone Input Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newMilestoneText,
                            onValueChange = { newMilestoneText = it },
                            placeholder = { Text("Milestone title...", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_create_milestone"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = DexterOnBackground,
                                unfocusedTextColor = DexterOnBackground,
                                focusedBorderColor = DexterPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (newMilestoneText.isNotBlank()) {
                                    initialMilestones.add(newMilestoneText.trim())
                                    newMilestoneText = ""
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(DexterPrimary, CircleShape)
                                .testTag("btn_add_create_milestone")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Milestone", tint = Color.Black)
                        }
                    }

                    // Added Milestones List
                    if (initialMilestones.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            initialMilestones.forEachIndexed { idx, m ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DexterSurfaceVariant, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${idx + 1}.", color = DexterPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(m, color = DexterOnBackground, fontSize = 12.sp)
                                    }
                                    IconButton(
                                        onClick = { initialMilestones.removeAt(idx) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color(0xFFA0A5BD),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
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
                        for (milestone in initialMilestones) {
                            if (milestone.isNotBlank()) {
                                repository.createMilestone(created.id, milestone)
                            }
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
            else -> goals.filter {
                it.category.contains(selectedCategoryFilter, ignoreCase = true) ||
                (selectedCategoryFilter.equals("Work", ignoreCase = true) && it.category.contains("Career", ignoreCase = true)) ||
                (selectedCategoryFilter.equals("Health", ignoreCase = true) && it.category.contains("Fitness", ignoreCase = true)) ||
                (selectedCategoryFilter.equals("Education", ignoreCase = true) && it.category.contains("Learning", ignoreCase = true)) ||
                (selectedCategoryFilter.equals("Financial", ignoreCase = true) && it.category.contains("Finance", ignoreCase = true))
            }
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

                        // Category Filter Pills (Scrollable with all domains)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val categories = listOf(
                                "All" to "All (${goals.size})",
                                "Financial" to "💰 Finance",
                                "Health" to "🏃 Health & Fitness",
                                "Work" to "💼 Work & Career",
                                "Education" to "📚 Education & Learning",
                                "Personal" to "🌟 Personal & Habits",
                                "Wellbeing" to "🧘 Wellbeing"
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
                    },
                    onDateUpdated = { newDate ->
                        repository.updateGoalTargetDate(goal.id, newDate)
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
    onQuickIncrement: (Double) -> Unit,
    onDateUpdated: ((String) -> Unit)? = null
) {
    var showGoalDatePicker by remember { mutableStateOf(false) }

    // Smooth animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = (goal.progressPercent / 100f).coerceIn(0f, 1f),
        label = "GoalProgress"
    )

    val categoryColor = when (goal.category.lowercase()) {
        "financial", "finance" -> Color(0xFF34D399) // Emerald
        "health", "fitness", "health & fitness" -> Color(0xFFFB7185) // Coral Rose
        "career", "work", "work & career" -> Color(0xFF818CF8) // Violet Indigo
        "education", "learning", "education & learning" -> Color(0xFF38BDF8) // Electric Sky Blue
        "personal", "habits", "personal & habits" -> Color(0xFFA78BFA) // Lavender Purple
        "wellbeing", "lifestyle", "wellbeing & lifestyle" -> Color(0xFF2DD4BF) // Mint Teal
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

                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        onClick = { showGoalDatePicker = true },
                        shape = RoundedCornerShape(6.dp),
                        color = DexterSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "Target Deadline",
                                tint = DexterPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (goal.targetDate.isNotBlank()) "Target: ${goal.targetDate}" else "+ Set Target Date",
                                color = if (goal.targetDate.isNotBlank()) Color(0xFFA0A5BD) else DexterPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Target Date",
                                tint = Color(0xFFA0A5BD),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }

                    if (showGoalDatePicker) {
                        ModernDatePickerDialog(
                            currentDateString = goal.targetDate,
                            onDateSelected = { _, newDate ->
                                onDateUpdated?.invoke(newDate)
                            },
                            onDismiss = { showGoalDatePicker = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val isCurrency = goal.metricUnit == "₹" || goal.metricUnit == "$" || goal.metricUnit == "€" || goal.metricUnit == "£"
            val currentFormatted = if (isCurrency) "${goal.metricUnit}${formatNumber(goal.currentAmount)}" else "${formatNumber(goal.currentAmount)} ${goal.metricUnit}"
            val targetFormatted = if (isCurrency) "${goal.metricUnit}${formatNumber(goal.targetAmount)}" else "${formatNumber(goal.targetAmount)} ${goal.metricUnit}"
            val deficitFormatted = if (isCurrency) "${goal.metricUnit}${formatNumber(deficit)}" else "${formatNumber(deficit)} ${goal.metricUnit}"

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
                        text = "$currentFormatted / $targetFormatted",
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
                        text = "$deficitFormatted to go",
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
            val quickIncrements = if (isCurrency) {
                listOf(500.0, 1000.0, 5000.0)
            } else if (goal.targetAmount <= 15) {
                listOf(1.0, 2.0, 5.0)
            } else if (goal.targetAmount <= 100) {
                listOf(1.0, 5.0, 10.0)
            } else {
                listOf(5.0, 25.0, 100.0)
            }

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

private data class GoalCategoryOption(
    val category: String,
    val label: String,
    val icon: String,
    val defaultUnit: String,
    val defaultTarget: String,
    val placeholder: String,
    val unitSuggestions: List<String>
)

private val GOAL_CATEGORY_OPTIONS = listOf(
    GoalCategoryOption("Financial", "Finance", "💰", "₹", "50000", "e.g. Save Emergency Fund, Buy Index Funds", listOf("₹", "$", "€", "£")),
    GoalCategoryOption("Health & Fitness", "Health", "🏃", "km", "100", "e.g. Run 100 km, Reach 70 kg, 20 Gym Sessions", listOf("km", "kg", "lbs", "workouts", "steps")),
    GoalCategoryOption("Work & Career", "Work", "💼", "projects", "5", "e.g. Deliver 5 Projects, Acquire 10 Clients", listOf("projects", "clients", "reviews", "sales", "hours")),
    GoalCategoryOption("Education & Learning", "Education", "📚", "books", "12", "e.g. Read 12 Books, Complete 3 Courses", listOf("books", "courses", "hours", "certs", "chapters")),
    GoalCategoryOption("Personal & Habits", "Personal", "🌟", "days", "30", "e.g. 30-Day Morning Routine, Journal Habit", listOf("days", "habits", "sessions", "hours", "entries")),
    GoalCategoryOption("Wellbeing & Lifestyle", "Wellbeing", "🧘", "sessions", "20", "e.g. 20 Meditation Sessions, 8h Sleep Streak", listOf("sessions", "days", "hours", "weeks", "streaks"))
)

@Composable
private fun CreateGoalDialog(
    repository: DexterRepository,
    onDismiss: () -> Unit,
    onGoalCreated: () -> Unit
) {
    var selectedOption by remember { mutableStateOf(GOAL_CATEGORY_OPTIONS[0]) }
    var name by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf(selectedOption.defaultTarget) }
    var targetDate by remember { mutableStateOf("Dec 31, 2026") }
    var metricUnit by remember { mutableStateOf(selectedOption.defaultUnit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DexterSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(selectedOption.icon, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set Long-Term Goal", color = DexterOnBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Category Selector Chips (Horizontally Scrollable)
                Column {
                    Text("Select Goal Domain", color = Color(0xFFA0A5BD), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GOAL_CATEGORY_OPTIONS.forEach { opt ->
                            val isSel = selectedOption.category == opt.category
                            Surface(
                                onClick = {
                                    val previousOpt = selectedOption
                                    selectedOption = opt
                                    // Auto-adjust default target and unit if unchanged from prior default
                                    if (targetAmount.isBlank() || targetAmount == previousOpt.defaultTarget) {
                                        targetAmount = opt.defaultTarget
                                    }
                                    if (metricUnit.isBlank() || metricUnit == previousOpt.defaultUnit || previousOpt.unitSuggestions.contains(metricUnit)) {
                                        metricUnit = opt.defaultUnit
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) DexterPrimary.copy(alpha = 0.25f) else DexterSurfaceVariant,
                                border = if (isSel) BorderStroke(1.5.dp, DexterPrimary) else null,
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("goal_category_${opt.category.replace(" ", "_")}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(opt.icon, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = opt.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) DexterPrimary else Color(0xFFA0A5BD)
                                    )
                                }
                            }
                        }
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Title (${selectedOption.label})") },
                    placeholder = { Text(selectedOption.placeholder) },
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

                // Target Amount & Metric Unit
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetAmount,
                        onValueChange = { targetAmount = it },
                        label = { Text("Target Quantity") },
                        placeholder = { Text(selectedOption.defaultTarget) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.8f)
                            .testTag("input_goal_target_amount")
                    )
                    OutlinedTextField(
                        value = metricUnit,
                        onValueChange = { metricUnit = it },
                        label = { Text("Unit") },
                        placeholder = { Text(selectedOption.defaultUnit) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("input_goal_unit")
                    )
                }

                // Quick Unit Suggestions for chosen category
                Column {
                    Text("Suggested Units for ${selectedOption.label}:", color = Color(0xFFA0A5BD), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedOption.unitSuggestions.forEach { unitSuggestion ->
                            val isUnitSel = metricUnit.equals(unitSuggestion, ignoreCase = true)
                            Surface(
                                onClick = { metricUnit = unitSuggestion },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isUnitSel) DexterPrimary.copy(alpha = 0.2f) else DexterSurfaceVariant.copy(alpha = 0.7f),
                                border = if (isUnitSel) BorderStroke(1.dp, DexterPrimary) else null,
                                modifier = Modifier.height(28.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = unitSuggestion,
                                        fontSize = 11.sp,
                                        color = if (isUnitSel) DexterPrimary else DexterOnBackground.copy(alpha = 0.8f),
                                        fontWeight = if (isUnitSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // Modern Calendar & Year Deadline Picker
                ModernDeadlinePickerField(
                    deadline = targetDate,
                    onDeadlineChange = { targetDate = it },
                    label = "Target Completion Date (Calendar & Year)",
                    placeholder = "Select target completion date...",
                    testTagPrefix = "input_goal_target_date"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val amount = targetAmount.toDoubleOrNull() ?: 0.0
                        val chosenUnit = metricUnit.ifBlank { selectedOption.defaultUnit }
                        repository.createGoal(name.trim(), selectedOption.category, amount, targetDate, chosenUnit)
                        onGoalCreated()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_submit_create_goal")
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
    androidx.compose.runtime.LaunchedEffect(Unit) {
        onOpenFinance()
    }
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
    androidx.compose.runtime.LaunchedEffect(Unit) {
        onOpenJournal()
    }
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
    val context = LocalContext.current
    val blocks = remember(trigger) { repository.listScheduleBlocks() }
    var showAddBlockDialog by remember { mutableStateOf(false) }
    var blockToEdit by remember { mutableStateOf<ScheduleBlockItem?>(null) }
    var blockToDelete by remember { mutableStateOf<ScheduleBlockItem?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    fun parseTimeToMinutes(timeStr: String): Int {
        if (timeStr.isBlank()) return 0
        return try {
            val clean = timeStr.trim().uppercase(Locale.US)
            val isPm = clean.endsWith("PM")
            val isAm = clean.endsWith("AM")
            val justTime = clean.replace("AM", "").replace("PM", "").trim()
            val parts = justTime.split(":")
            var h = parts[0].toIntOrNull() ?: 0
            val m = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
            if (isPm && h < 12) h += 12
            if (isAm && h == 12) h = 0
            h * 60 + m
        } catch (_: Exception) { 0 }
    }

    fun calculateDurationMinutes(startTime: String, endTime: String): Int {
        val startMin = parseTimeToMinutes(startTime)
        val endMin = parseTimeToMinutes(endTime)
        val diff = endMin - startMin
        return if (diff > 0) diff else 60
    }

    fun formatDuration(durationMinutes: Int): String {
        val h = durationMinutes / 60
        val m = durationMinutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    fun isBlockActiveNow(startTime: String, endTime: String): Boolean {
        val cal = Calendar.getInstance()
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMin = parseTimeToMinutes(startTime)
        val endMin = parseTimeToMinutes(endTime)
        return nowMin in startMin..endMin
    }

    fun getCategoryAttributes(category: String): Triple<Color, String, String> {
        return when (category.lowercase(Locale.US)) {
            "deep work", "coding", "focus" -> Triple(Color(0xFF00E5FF), "🧠", "#00E5FF")
            "meeting", "sync", "call", "standup" -> Triple(Color(0xFFFF9F0A), "🤝", "#FF9F0A")
            "wellness", "health", "break", "rest", "lunch", "workout" -> Triple(Color(0xFF30D158), "🌿", "#30D158")
            "personal", "study", "learning", "reading" -> Triple(Color(0xFFBF5AF2), "🎯", "#BF5AF2")
            else -> Triple(Color(0xFF4FC3F7), "🗓️", "#4FC3F7")
        }
    }

    // Sort chronologically
    val sortedBlocks = remember(blocks) {
        blocks.sortedBy { parseTimeToMinutes(it.startTime) }
    }

    val activeBlock = remember(sortedBlocks) {
        sortedBlocks.firstOrNull { isBlockActiveNow(it.startTime, it.endTime) }
    }

    val nextUpcomingBlock = remember(sortedBlocks) {
        val cal = Calendar.getInstance()
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        sortedBlocks.firstOrNull { parseTimeToMinutes(it.startTime) > nowMin }
    }

    val totalMinutes = remember(sortedBlocks) {
        sortedBlocks.sumOf { calculateDurationMinutes(it.startTime, it.endTime) }
    }

    val deepWorkMinutes = remember(sortedBlocks) {
        sortedBlocks
            .filter { it.category.equals("Deep Work", true) || it.category.equals("Coding", true) || it.category.equals("Focus", true) }
            .sumOf { calculateDurationMinutes(it.startTime, it.endTime) }
    }

    val filteredBlocks = remember(sortedBlocks, selectedCategoryFilter) {
        when (selectedCategoryFilter) {
            "All" -> sortedBlocks
            "Deep Work" -> sortedBlocks.filter { it.category.equals("Deep Work", true) || it.category.equals("Coding", true) || it.category.equals("Focus", true) }
            "Meetings" -> sortedBlocks.filter { it.category.equals("Meeting", true) || it.category.equals("Sync", true) || it.category.equals("Call", true) }
            "Wellness" -> sortedBlocks.filter { it.category.equals("Wellness", true) || it.category.equals("Health", true) || it.category.equals("Break", true) }
            "Personal" -> sortedBlocks.filter { it.category.equals("Personal", true) || it.category.equals("Study", true) }
            else -> sortedBlocks.filter { it.category.equals(selectedCategoryFilter, true) }
        }
    }

    val todayFormatted = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.US).format(Date())
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    blockToEdit = null
                    showAddBlockDialog = true
                },
                containerColor = DexterPrimary,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_schedule_block")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Time Block", tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Block", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
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
            // 1. Metric Overview Summary Strip (Apple Calendar HIG)
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
                                    Text("🗓️", fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = todayFormatted,
                                        color = DexterOnBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        text = "Today's Agenda & Focus Timetable",
                                        color = Color(0xFFA0A5BD),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Live status badge
                            if (activeBlock != null) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF30D158).copy(alpha = 0.14f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30D158).copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF30D158))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Active Now",
                                            color = Color(0xFF30D158),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            } else if (nextUpcomingBlock != null) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = DexterPrimary.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DexterPrimary.copy(alpha = 0.30f))
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
                                            text = "Next: ${nextUpcomingBlock.startTime}",
                                            color = DexterPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Total Hours
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = DexterBackground.copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("SCHEDULED", color = Color(0xFFA0A5BD), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatDuration(totalMinutes),
                                        color = DexterOnBackground,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Blocks Count
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = DexterBackground.copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("BLOCKS", color = Color(0xFFA0A5BD), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${sortedBlocks.size}",
                                        color = DexterPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Deep Work
                            Surface(
                                modifier = Modifier.weight(1.1f),
                                shape = RoundedCornerShape(14.dp),
                                color = DexterBackground.copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("DEEP WORK", color = Color(0xFFA0A5BD), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatDuration(deepWorkMinutes),
                                        color = Color(0xFF00E5FF),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Rapid 1-Tap Quick Schedule Bar (Apple Agency & Delight)
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
                            text = "RAPID TIME BLOCKING",
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

                    val quickPresets = listOf(
                        Triple("⚡ 30m Sprint", 30, "Deep Work: Quick Sprint" to "Deep Work"),
                        Triple("🧠 1h Deep Work", 60, "Deep Work: Focused Session" to "Deep Work"),
                        Triple("☕ 15m Break", 15, "Coffee & Rest Break" to "Wellness"),
                        Triple("🤝 30m Team Sync", 30, "Team Check-in Sync" to "Meeting"),
                        Triple("🏋️ 45m Workout", 45, "Health & Fitness Workout" to "Wellness"),
                        Triple("🎯 2h Big Project", 120, "Core Project Architecture" to "Deep Work")
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(quickPresets) { (chipTitle, durationMin, details) ->
                            val (title, category) = details
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = DexterSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .clickable {
                                        val cal = Calendar.getInstance()
                                        // Round start to next 5 minutes
                                        val curMin = cal.get(Calendar.MINUTE)
                                        val roundedMin = ((curMin + 4) / 5) * 5
                                        cal.set(Calendar.MINUTE, roundedMin % 60)
                                        if (roundedMin >= 60) cal.add(Calendar.HOUR_OF_DAY, 1)
                                        cal.set(Calendar.SECOND, 0)

                                        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
                                        val startStr = sdf.format(cal.time)
                                        cal.add(Calendar.MINUTE, durationMin)
                                        val endStr = sdf.format(cal.time)

                                        val (_, _, colorHex) = getCategoryAttributes(category)
                                        repository.createScheduleBlock(
                                            title = title,
                                            startTime = startStr,
                                            endTime = endStr,
                                            category = category,
                                            colorHex = colorHex,
                                            location = "Office / Local"
                                        )
                                        Toast.makeText(context, "Scheduled: $title ($startStr)", Toast.LENGTH_SHORT).show()
                                        onRefresh()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = chipTitle,
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

            // 3. Segmented Category Filter Pills
            item {
                val categories = listOf("All", "Deep Work", "Meetings", "Wellness", "Personal")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategoryFilter == category
                        val count = when (category) {
                            "All" -> sortedBlocks.size
                            "Deep Work" -> sortedBlocks.count { it.category.equals("Deep Work", true) || it.category.equals("Coding", true) }
                            "Meetings" -> sortedBlocks.count { it.category.equals("Meeting", true) || it.category.equals("Sync", true) }
                            "Wellness" -> sortedBlocks.count { it.category.equals("Wellness", true) || it.category.equals("Break", true) }
                            "Personal" -> sortedBlocks.count { it.category.equals("Personal", true) || it.category.equals("Study", true) }
                            else -> sortedBlocks.count { it.category.equals(category, true) }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) DexterPrimary else DexterSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) DexterPrimary else Color.White.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier.clickable { selectedCategoryFilter = category }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$category ($count)",
                                    color = if (isSelected) Color.Black else DexterOnBackground,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 4. Apple Calendar Timeline Cards List
            if (filteredBlocks.isEmpty()) {
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
                                Text("🗓️", fontSize = 32.sp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedCategoryFilter == "All") "Today's Agenda is Clear" else "No $selectedCategoryFilter Blocks",
                                color = DexterOnBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Take control of your focus by scheduling time blocks for deep work, client meetings, or recovery.",
                                color = Color(0xFFA0A5BD),
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    blockToEdit = null
                                    showAddBlockDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Plan First Time Block", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(filteredBlocks, key = { it.id }) { block ->
                    val isActive = isBlockActiveNow(block.startTime, block.endTime)
                    val durationMin = calculateDurationMinutes(block.startTime, block.endTime)
                    val (catColor, catIcon, _) = getCategoryAttributes(block.category)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("schedule_block_${block.id}")
                            .clickable {
                                blockToEdit = block
                                showAddBlockDialog = true
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) DexterSurface else DexterSurface.copy(alpha = 0.85f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isActive) 1.5.dp else 1.dp,
                            if (isActive) catColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Column: Start Time, Duration Pill, End Time
                            Column(
                                modifier = Modifier.width(76.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = block.startTime,
                                    color = if (isActive) catColor else DexterOnBackground,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = catColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = formatDuration(durationMin),
                                        color = catColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = block.endTime,
                                    color = Color(0xFFA0A5BD),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            // 4dp Vertical Indicator Bar
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(catColor)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            // Middle Column: Title, Category, Location, Active Badge
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "$catIcon ${block.category}",
                                        color = catColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    if (isActive) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = catColor.copy(alpha = 0.2f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, catColor.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = "HAPPENING NOW",
                                                color = catColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = block.title,
                                    color = DexterOnBackground,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (block.location.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "📍 ${block.location}",
                                        color = Color(0xFFA0A5BD),
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Action Controls
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        blockToEdit = block
                                        showAddBlockDialog = true
                                    },
                                    modifier = Modifier.size(36.dp).testTag("block_edit_${block.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Block",
                                        tint = Color(0xFFA0A5BD),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { blockToDelete = block },
                                    modifier = Modifier.size(36.dp).testTag("block_delete_${block.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Block",
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

    // 5. Deletion Guardrail Dialog (Apple Responsibility)
    blockToDelete?.let { block ->
        AlertDialog(
            onDismissRequest = { blockToDelete = null },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Delete Time Block?",
                    color = DexterOnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${block.title}' (${block.startTime} – ${block.endTime})? This will free up this time slot.",
                    color = Color(0xFFA0A5BD),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteScheduleBlock(block.id)
                        blockToDelete = null
                        Toast.makeText(context, "Time block deleted", Toast.LENGTH_SHORT).show()
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_confirm_delete_schedule_block")
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { blockToDelete = null },
                    modifier = Modifier.testTag("btn_cancel_delete_schedule_block")
                ) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // 6. Add / Edit Block Dialog (Apple Craft & Progressive Disclosure)
    if (showAddBlockDialog) {
        val editing = blockToEdit
        var title by remember { mutableStateOf(editing?.title ?: "") }
        var category by remember { mutableStateOf(editing?.category ?: "Deep Work") }
        var location by remember { mutableStateOf(editing?.location ?: "") }

        var startClock by remember {
            mutableStateOf(
                if (editing != null) {
                    val m = parseTimeToMinutes(editing.startTime)
                    ClockTime(m / 60, m % 60)
                } else {
                    ClockTime.now()
                }
            )
        }

        var endClock by remember {
            mutableStateOf(
                if (editing != null) {
                    val m = parseTimeToMinutes(editing.endTime)
                    ClockTime(m / 60, m % 60)
                } else {
                    val startNow = ClockTime.now()
                    ClockTime((startNow.hour24 + 1) % 24, startNow.minute)
                }
            )
        }

        val categoryOptions = listOf(
            "Deep Work" to "🧠",
            "Meeting" to "🤝",
            "Wellness" to "🌿",
            "Personal" to "🎯"
        )

        AlertDialog(
            onDismissRequest = {
                showAddBlockDialog = false
                blockToEdit = null
            },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (editing != null) "Edit Time Block" else "Schedule Time Block",
                    color = DexterOnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Block Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Block Title") },
                        placeholder = { Text("e.g. Architecture Sprint, Client Sync") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_block_title"),
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

                    // Quick Title Suggestions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Coding", "Team Sync", "Break", "Workout").forEach { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DexterBackground,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.clickable {
                                    title = suggestion
                                    when (suggestion) {
                                        "Coding" -> category = "Deep Work"
                                        "Team Sync" -> category = "Meeting"
                                        "Break", "Workout" -> category = "Wellness"
                                    }
                                }
                            ) {
                                Text(
                                    text = suggestion,
                                    color = Color(0xFFA0A5BD),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Category Pill Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CATEGORY",
                            color = Color(0xFFA0A5BD),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categoryOptions.forEach { (catName, icon) ->
                                val isSelected = category.equals(catName, true)
                                val (catColor, _, _) = getCategoryAttributes(catName)

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) catColor.copy(alpha = 0.2f) else DexterBackground.copy(alpha = 0.6f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) catColor else Color.White.copy(alpha = 0.08f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { category = catName }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$icon $catName",
                                            color = if (isSelected) catColor else DexterOnBackground,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Start Time Picker
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
                            "11:00 AM" to ClockTime(11, 0),
                            "02:00 PM" to ClockTime(14, 0),
                            "04:30 PM" to ClockTime(16, 30)
                        )
                    )

                    // End Time Picker
                    ModernClockTimePicker(
                        label = "END TIME",
                        time = endClock,
                        onTimeChanged = { endClock = it },
                        presets = listOf(
                            "+30m" to ClockTime(startClock.hour24 + (if (startClock.minute + 30 >= 60) 1 else 0), (startClock.minute + 30) % 60),
                            "+1h" to ClockTime((startClock.hour24 + 1) % 24, startClock.minute),
                            "+1.5h" to ClockTime((startClock.hour24 + 1 + (if (startClock.minute + 30 >= 60) 1 else 0)) % 24, (startClock.minute + 30) % 60),
                            "+2h" to ClockTime((startClock.hour24 + 2) % 24, startClock.minute)
                        )
                    )

                    // Location / Link
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location / Link (Optional)") },
                        placeholder = { Text("e.g. Meeting Room 2, Zoom URL") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_block_location"),
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalTitle = title.ifBlank { "Focus Block" }
                        val (_, _, colorHex) = getCategoryAttributes(category)
                        if (editing != null) {
                            repository.updateScheduleBlock(
                                id = editing.id,
                                title = finalTitle,
                                startTime = startClock.formattedString,
                                endTime = endClock.formattedString,
                                category = category,
                                colorHex = colorHex,
                                location = location
                            )
                            Toast.makeText(context, "Time block updated", Toast.LENGTH_SHORT).show()
                        } else {
                            repository.createScheduleBlock(
                                title = finalTitle,
                                startTime = startClock.formattedString,
                                endTime = endClock.formattedString,
                                category = category,
                                colorHex = colorHex,
                                location = location
                            )
                            Toast.makeText(context, "Time block scheduled", Toast.LENGTH_SHORT).show()
                        }
                        showAddBlockDialog = false
                        blockToEdit = null
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_save_schedule_block")
                ) {
                    Text(
                        text = if (editing != null) "Update Block" else "Save Block",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddBlockDialog = false
                        blockToEdit = null
                    }
                ) {
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
    val context = LocalContext.current
    val people = remember(trigger) { repository.listPeople() }
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var personToEdit by remember { mutableStateOf<PersonItem?>(null) }
    var personToDelete by remember { mutableStateOf<PersonItem?>(null) }
    var personForInteractions by remember { mutableStateOf<PersonItem?>(null) }
    var showDexterManifesto by remember { mutableStateOf(false) }
    var selectedCircleFilter by remember { mutableStateOf("All") }

    fun getCircleAttributes(relationType: String): Triple<Color, Brush, String> {
        return when (relationType.lowercase(Locale.US)) {
            "family" -> Triple(
                Color(0xFFFF375F),
                Brush.linearGradient(listOf(Color(0xFFFF375F), Color(0xFFFF9F0A))),
                "❤️"
            )
            "close friend", "friend", "best friend" -> Triple(
                Color(0xFF00E5FF),
                Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))),
                "⭐"
            )
            "doctor", "physician", "therapist", "mentor" -> Triple(
                Color(0xFF30D158),
                Brush.linearGradient(listOf(Color(0xFF30D158), Color(0xFF00C853))),
                "🩺"
            )
            "colleague", "work", "coworker", "client" -> Triple(
                Color(0xFFBF5AF2),
                Brush.linearGradient(listOf(Color(0xFFBF5AF2), Color(0xFF5E5CE6))),
                "💼"
            )
            else -> Triple(
                Color(0xFF4FC3F7),
                Brush.linearGradient(listOf(Color(0xFF4FC3F7), Color(0xFF0288D1))),
                "👥"
            )
        }
    }

    val filteredPeople = remember(people, selectedCircleFilter) {
        when (selectedCircleFilter) {
            "All" -> people
            "Family" -> people.filter { it.relationType.equals("Family", true) }
            "Friends" -> people.filter { it.relationType.contains("Friend", true) }
            "Work" -> people.filter { it.relationType.equals("Colleague", true) || it.relationType.equals("Work", true) || it.relationType.equals("Client", true) }
            "Doctor/Mentor" -> people.filter { it.relationType.equals("Doctor", true) || it.relationType.equals("Mentor", true) }
            else -> people.filter { it.relationType.equals(selectedCircleFilter, true) }
        }
    }

    val circlesCount = remember(people) {
        people.map { it.relationType }.distinct().size
    }

    val dueForContact = remember(people) {
        people.filter {
            it.lastContactedText.contains("week", true) ||
            it.lastContactedText.contains("month", true) ||
            it.lastContactedText.contains("ago", true)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    personToEdit = null
                    showAddPersonDialog = true
                },
                containerColor = DexterPrimary,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_contact")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Contact", tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Contact", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
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
            // 1. Metric Overview Summary Strip with Dexter Manifesto Link
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
                                    Text("👥", fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Personal Network",
                                        color = DexterOnBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        text = "Meaningful bonds & shared backstory",
                                        color = Color(0xFFA0A5BD),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Dexter Manifesto Pill Button
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = DexterPrimary.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DexterPrimary.copy(alpha = 0.35f)),
                                modifier = Modifier.clickable { showDexterManifesto = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📖", fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Dexter Ethos",
                                        color = DexterPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Total People
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = DexterBackground.copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("NETWORK", color = Color(0xFFA0A5BD), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${people.size}",
                                        color = DexterOnBackground,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Circles
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = DexterBackground.copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("CIRCLES", color = Color(0xFFA0A5BD), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$circlesCount",
                                        color = DexterPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Touchpoint Alert
                            Surface(
                                modifier = Modifier.weight(1.1f),
                                shape = RoundedCornerShape(14.dp),
                                color = DexterBackground.copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("CATCH-UP", color = Color(0xFFA0A5BD), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${dueForContact.size} Due",
                                        color = if (dueForContact.isNotEmpty()) Color(0xFFFF9F0A) else Color(0xFF30D158),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Relation Circle Filter Tabs
            item {
                val circleTabs = listOf("All", "Friends", "Family", "Work", "Doctor/Mentor")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(circleTabs) { tab ->
                        val isSelected = selectedCircleFilter == tab
                        val count = when (tab) {
                            "All" -> people.size
                            "Family" -> people.count { it.relationType.equals("Family", true) }
                            "Friends" -> people.count { it.relationType.contains("Friend", true) }
                            "Work" -> people.count { it.relationType.equals("Colleague", true) || it.relationType.equals("Work", true) || it.relationType.equals("Client", true) }
                            "Doctor/Mentor" -> people.count { it.relationType.equals("Doctor", true) || it.relationType.equals("Mentor", true) }
                            else -> people.count { it.relationType.equals(tab, true) }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) DexterPrimary else DexterSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) DexterPrimary else Color.White.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier.clickable { selectedCircleFilter = tab }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$tab ($count)",
                                    color = if (isSelected) Color.Black else DexterOnBackground,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 3. Contact Cards with Backstory & Monogram
            if (filteredPeople.isEmpty()) {
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
                                Text("👥", fontSize = 32.sp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedCircleFilter == "All") "No Contacts Added" else "No $selectedCircleFilter Contacts",
                                color = DexterOnBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Keep track of the people who matter most, their backstory, how you met, and touchpoint continuity.",
                                color = Color(0xFFA0A5BD),
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    personToEdit = null
                                    showAddPersonDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add First Contact", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(filteredPeople, key = { it.id }) { person ->
                    val (circleColor, circleGradient, circleIcon) = getCircleAttributes(person.relationType)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("person_card_${person.id}")
                            .clickable { personForInteractions = person },
                        colors = CardDefaults.cardColors(containerColor = DexterSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            // Header Row: Avatar, Name, Circle Badge, and Edit/Delete Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Monogram Avatar with Gradient
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(circleGradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = person.initial,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = person.name,
                                            color = DexterOnBackground,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = circleColor.copy(alpha = 0.15f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, circleColor.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = "$circleIcon ${person.relationType}",
                                                color = circleColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    // Last Contacted & Birthday
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Last: ${person.lastContactedText}",
                                            color = Color(0xFFA0A5BD),
                                            fontSize = 11.sp
                                        )

                                        if (person.birthday.isNotBlank()) {
                                            Text(
                                                text = "· 🎂 ${person.birthday}",
                                                color = Color(0xFFFF9F0A),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            personToEdit = person
                                            showAddPersonDialog = true
                                        },
                                        modifier = Modifier.size(36.dp).testTag("person_edit_${person.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Contact",
                                            tint = Color(0xFFA0A5BD),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { personToDelete = person },
                                        modifier = Modifier.size(36.dp).testTag("person_delete_${person.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Contact",
                                            tint = Color(0xFFFF453A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Backstory & How We Met Section (The feature requested by user!)
                            Spacer(modifier = Modifier.height(12.dp))
                            if (person.backstory.isNotBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = DexterBackground.copy(alpha = 0.6f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("📖", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "BACKSTORY & HOW WE MET",
                                                color = Color(0xFFA0A5BD),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.8.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = person.backstory,
                                            color = DexterOnBackground.copy(alpha = 0.9f),
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            personToEdit = person
                                            showAddPersonDialog = true
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = DexterBackground.copy(alpha = 0.3f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("📖", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "+ Add backstory / how you met...",
                                            color = DexterPrimary.copy(alpha = 0.8f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Contact Communication Channels (Phone, Email, Notes)
                            if (person.phone.isNotBlank() || person.email.isNotBlank() || person.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (person.phone.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = DexterBackground.copy(alpha = 0.7f),
                                            modifier = Modifier.clickable {
                                                try {
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${person.phone}"))
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {}
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("📞", fontSize = 11.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = person.phone,
                                                    color = DexterPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    if (person.email.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = DexterBackground.copy(alpha = 0.7f),
                                            modifier = Modifier.clickable {
                                                try {
                                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${person.email}"))
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {}
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("✉️", fontSize = 11.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = person.email,
                                                    color = Color(0xFFA0A5BD),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (person.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "💡 ${person.notes}",
                                    color = Color(0xFFA0A5BD),
                                    fontSize = 12.sp,
                                    maxLines = 2
                                )
                            }

                            // 1-Tap Quick Touchpoint Logging Bar (Apple Agency & Delight)
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val touchpoints = listOf(
                                    "📞 Call" to "Call",
                                    "☕ Catch-up" to "Meeting",
                                    "🤝 Meeting" to "Meeting",
                                    "💬 Text" to "Message"
                                )

                                touchpoints.forEach { (label, type) ->
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = DexterBackground,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                repository.createInteraction(
                                                    personId = person.id,
                                                    note = "Logged $label touchpoint",
                                                    interactionType = type,
                                                    date = "Today"
                                                )
                                                Toast.makeText(context, "$label logged for ${person.name}", Toast.LENGTH_SHORT).show()
                                                onRefresh()
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = DexterOnBackground,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = DexterPrimary.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DexterPrimary.copy(alpha = 0.3f)),
                                    modifier = Modifier.clickable { personForInteractions = person }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "History →",
                                            color = DexterPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
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

    // 4. Deletion Guardrail Dialog (Apple Responsibility)
    personToDelete?.let { person ->
        AlertDialog(
            onDismissRequest = { personToDelete = null },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Delete Contact?",
                    color = DexterOnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${person.name}'? All backstory notes and touchpoint history for this person will also be permanently removed.",
                    color = Color(0xFFA0A5BD),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deletePerson(person.id)
                        personToDelete = null
                        Toast.makeText(context, "Contact deleted", Toast.LENGTH_SHORT).show()
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_confirm_delete_contact")
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { personToDelete = null },
                    modifier = Modifier.testTag("btn_cancel_delete_contact")
                ) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // 5. Dexter Backstory & Manifesto Dialog (Requested by User!)
    if (showDexterManifesto) {
        val backstory = remember { repository.getDexterBackstory() }
        Dialog(onDismissRequest = { showDexterManifesto = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DexterPrimary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DexterPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚡", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = backstory.codename,
                                    color = DexterPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = backstory.version,
                                    color = Color(0xFFA0A5BD),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        IconButton(onClick = { showDexterManifesto = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = DexterOnBackground)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = DexterBackground.copy(alpha = 0.7f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "THE DEXTER MANIFESTO",
                                        color = DexterPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = backstory.manifesto,
                                        color = DexterOnBackground,
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = "CORE ARCHITECTURAL PILLARS",
                                color = Color(0xFFA0A5BD),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        items(backstory.pillars) { (title, desc) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DexterBackground.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = title,
                                        color = DexterOnBackground,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = desc,
                                        color = Color(0xFFA0A5BD),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showDexterManifesto = false },
                        colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Understood", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 6. Add / Edit Contact Dialog (Includes Backstory Field!)
    if (showAddPersonDialog) {
        val editing = personToEdit
        var name by remember { mutableStateOf(editing?.name ?: "") }
        var relationType by remember { mutableStateOf(editing?.relationType ?: "Close Friend") }
        var backstory by remember { mutableStateOf(editing?.backstory ?: "") }
        var phone by remember { mutableStateOf(editing?.phone ?: "") }
        var email by remember { mutableStateOf(editing?.email ?: "") }
        var birthday by remember { mutableStateOf(editing?.birthday ?: "") }
        var notes by remember { mutableStateOf(editing?.notes ?: "") }

        val circles = listOf("Close Friend", "Family", "Colleague", "Doctor", "Mentor", "Other")

        AlertDialog(
            onDismissRequest = {
                showAddPersonDialog = false
                personToEdit = null
            },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (editing != null) "Edit Contact" else "New Connection",
                    color = DexterOnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("e.g. Rahul Verma") },
                        modifier = Modifier.fillMaxWidth().testTag("input_contact_name"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                        ),
                        singleLine = true
                    )

                    // Relationship Circle Chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "RELATION CIRCLE",
                            color = Color(0xFFA0A5BD),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(circles) { circle ->
                                val isSelected = relationType == circle
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) DexterPrimary.copy(alpha = 0.2f) else DexterBackground,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) DexterPrimary else Color.White.copy(alpha = 0.08f)
                                    ),
                                    modifier = Modifier.clickable { relationType = circle }
                                ) {
                                    Text(
                                        text = circle,
                                        color = if (isSelected) DexterPrimary else DexterOnBackground,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Backstory & How We Met Field (Core User Request!)
                    OutlinedTextField(
                        value = backstory,
                        onValueChange = { backstory = it },
                        label = { Text("📖 Backstory & How We Met") },
                        placeholder = { Text("How you first connected, shared history, key memories...") },
                        modifier = Modifier.fillMaxWidth().testTag("input_contact_backstory"),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                        )
                    )

                    // Phone & Birthday Row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") },
                            placeholder = { Text("+91 ...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = DexterOnBackground,
                                unfocusedTextColor = DexterOnBackground,
                                focusedBorderColor = DexterPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = birthday,
                            onValueChange = { birthday = it },
                            label = { Text("Birthday") },
                            placeholder = { Text("Nov 14") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = DexterOnBackground,
                                unfocusedTextColor = DexterOnBackground,
                                focusedBorderColor = DexterPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                            ),
                            singleLine = true
                        )
                    }

                    // Email & Important Notes
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Important Notes & Topics") },
                        placeholder = { Text("e.g. Favorite book, upcoming trip...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalName = name.trim()
                        if (finalName.isNotBlank()) {
                            if (editing != null) {
                                repository.updatePerson(
                                    id = editing.id,
                                    name = finalName,
                                    relationType = relationType,
                                    phone = phone,
                                    email = email,
                                    birthday = birthday,
                                    notes = notes,
                                    backstory = backstory
                                )
                                Toast.makeText(context, "Contact updated", Toast.LENGTH_SHORT).show()
                            } else {
                                repository.createPerson(
                                    name = finalName,
                                    relationType = relationType,
                                    phone = phone,
                                    email = email,
                                    birthday = birthday,
                                    notes = notes,
                                    backstory = backstory
                                )
                                Toast.makeText(context, "Contact saved with backstory", Toast.LENGTH_SHORT).show()
                            }
                            showAddPersonDialog = false
                            personToEdit = null
                            onRefresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_save_contact")
                ) {
                    Text(
                        text = if (editing != null) "Update Contact" else "Save Contact",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddPersonDialog = false
                        personToEdit = null
                    }
                ) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // 7. Interaction History Sheet
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
    val context = LocalContext.current
    var trigger by remember { mutableStateOf(0) }
    val interactions = remember(trigger) { repository.listInteractions(person.id) }
    var noteInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Meeting") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.80f),
            colors = CardDefaults.cardColors(containerColor = DexterSurface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with contact summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(DexterPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(person.initial, color = DexterPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(person.name, color = DexterOnBackground, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("${person.relationType} · Touchpoint History", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DexterOnBackground)
                    }
                }

                // Backstory reminder pill if present
                if (person.backstory.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DexterBackground.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📖", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = person.backstory,
                                color = Color(0xFFA0A5BD),
                                fontSize = 11.sp,
                                maxLines = 2
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Rapid Touchpoint Logger Input
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Type selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("📞 Call", "☕ Coffee", "🤝 Meeting", "💬 Message").forEach { chip ->
                            val cleanType = chip.substringAfter(" ")
                            val isSelected = selectedType == cleanType
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) DexterPrimary.copy(alpha = 0.2f) else DexterBackground,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) DexterPrimary else Color.White.copy(alpha = 0.06f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedType = cleanType }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = chip,
                                        color = if (isSelected) DexterPrimary else DexterOnBackground,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            placeholder = { Text("Log notes from your conversation...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = DexterOnBackground,
                                unfocusedTextColor = DexterOnBackground,
                                focusedBorderColor = DexterPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (noteInput.isNotBlank()) {
                                    repository.createInteraction(person.id, noteInput.trim(), selectedType, "Today")
                                    noteInput = ""
                                    trigger++
                                    Toast.makeText(context, "Touchpoint logged", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DexterPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Log", tint = Color.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Timeline List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (interactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No touchpoints logged yet. Record your calls and catch-ups above.",
                                    color = Color(0xFFA0A5BD),
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    items(interactions, key = { it.id }) { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = DexterBackground.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = DexterPrimary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = item.interactionType,
                                            color = DexterPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Text(
                                        text = item.interactionDate,
                                        color = Color(0xFFA0A5BD),
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.note,
                                    color = DexterOnBackground,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// ==========================================
// SEGMENT 9: Health & Wellbeing (Apple Health HIG 2026 Redesign)
// ==========================================
@Composable
private fun HealthWellbeingView(
    repository: DexterRepository,
    trigger: Long,
    onRefresh: () -> Unit
) {
    val hydrationMl = remember(trigger) { repository.getHydrationTodayMl() }
    val hydrationLogs = remember(trigger) { repository.listHydrationLogs(10) }
    val sleepLogs = remember(trigger) { repository.listSleep() }
    val moodLogs = remember(trigger) { repository.listMoods() }

    var showSleepDialog by remember { mutableStateOf(false) }
    var showMoodDialog by remember { mutableStateOf(false) }
    var selectedMoodForCheckin by remember { mutableStateOf("😌 Calm & Content") }
    var showResetHydrationConfirm by remember { mutableStateOf(false) }
    var sleepToDelete by remember { mutableStateOf<SleepItem?>(null) }
    var moodToDelete by remember { mutableStateOf<MoodItem?>(null) }
    var showHydrationHistory by remember { mutableStateOf(false) }

    val dailyTargetMl = 2500
    val progressPercent = ((hydrationMl.toFloat() / dailyTargetMl.toFloat()) * 100).toInt()
    val glassesCount = hydrationMl / 250

    val latestSleep = sleepLogs.firstOrNull()
    val latestMood = moodLogs.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Metric Summary Strip (Apple Health Glance)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Hydration Stat Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💧", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Water", color = Color(0xFFA0A5BD), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${hydrationMl}ml",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$progressPercent% of goal",
                            color = if (progressPercent >= 100) Color(0xFF34D399) else Color(0xFFA0A5BD),
                            fontSize = 11.sp
                        )
                    }
                }

                // Sleep Stat Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("😴", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sleep", color = Color(0xFFA0A5BD), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = latestSleep?.durationText ?: "None",
                            color = Color(0xFF818CF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = latestSleep?.qualityLabel ?: "No logs",
                            color = Color(0xFFA0A5BD),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                // Mood Stat Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🧠", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mind", color = Color(0xFFA0A5BD), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = latestMood?.mood ?: "Unset",
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = latestMood?.energyLevel?.let { "$it Energy" } ?: "No check-in",
                            color = Color(0xFFA0A5BD),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 2. Hydration Hero Card (Apple Health Water Metric)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hydration_card"),
                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("💧", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Daily Hydration", color = DexterOnBackground, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text("Target: ${dailyTargetMl} ml ($glassesCount glasses)", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                            }
                        }

                        // Percentage Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (progressPercent >= 100) Color(0xFF34D399).copy(alpha = 0.15f) else Color(0xFF38BDF8).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (progressPercent >= 100) Color(0xFF34D399).copy(alpha = 0.4f) else Color(0xFF38BDF8).copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = "$progressPercent%",
                                color = if (progressPercent >= 100) Color(0xFF34D399) else Color(0xFF38BDF8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Apple Health Ring & Stats Centerpiece
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Canvas Circular Water Ring
                        val animatedRingProgress by animateFloatAsState(
                            targetValue = (hydrationMl.toFloat() / dailyTargetMl.toFloat()).coerceIn(0f, 1f),
                            label = "WaterRingProgress"
                        )
                        Box(
                            modifier = Modifier.size(96.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidthPx = 10.dp.toPx()
                                val arcSize = size.minDimension - strokeWidthPx
                                val topLeft = Offset((size.width - arcSize) / 2f, (size.height - arcSize) / 2f)

                                // Track circle
                                drawArc(
                                    color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = Size(arcSize, arcSize),
                                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                )

                                // Progress arc
                                drawArc(
                                    color = if (animatedRingProgress >= 1f) Color(0xFF34D399) else Color(0xFF38BDF8),
                                    startAngle = -90f,
                                    sweepAngle = 360f * animatedRingProgress,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = Size(arcSize, arcSize),
                                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                )
                            }
                            Text("💧", fontSize = 22.sp)
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${hydrationMl} ml",
                                color = DexterOnBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val remaining = (dailyTargetMl - hydrationMl).coerceAtLeast(0)
                            Text(
                                text = if (remaining > 0) "${remaining} ml remaining to goal" else "🎉 Daily goal completed!",
                                color = if (remaining > 0) Color(0xFFA0A5BD) else Color(0xFF34D399),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            // Visual glasses indicators (10 glasses of 250ml)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (1..10).forEach { glassIndex ->
                                    val isFilled = hydrationMl >= glassIndex * 250
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isFilled) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.15f)
                                            )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "1-TAP RAPID HYDRATION",
                        color = Color(0xFFA0A5BD),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4 Quick Increment Chips (48dp height minimum for accessibility)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("+250ml", "Glass", 250),
                            Triple("+330ml", "Can", 330),
                            Triple("+500ml", "Bottle", 500),
                            Triple("+750ml", "Flask", 750)
                        ).forEach { (amountLabel, typeLabel, amount) ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DexterSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clickable {
                                        repository.logHydration(amount)
                                        onRefresh()
                                    }
                                    .testTag("btn_log_water_$amount")
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = amountLabel,
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = typeLabel,
                                        color = Color(0xFFA0A5BD),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Controls: Undo last, History toggle, Reset day
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Undo button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                modifier = Modifier
                                    .height(34.dp)
                                    .clickable(enabled = hydrationMl > 0) {
                                        repository.deleteLastHydration()
                                        onRefresh()
                                    }
                                    .testTag("btn_undo_hydration")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "↩ Undo Last",
                                        color = if (hydrationMl > 0) DexterOnBackground else Color(0xFFA0A5BD).copy(alpha = 0.4f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // History toggle
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                modifier = Modifier
                                    .height(34.dp)
                                    .clickable { showHydrationHistory = !showHydrationHistory }
                                    .testTag("btn_toggle_hydration_history")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (showHydrationHistory) "▲ Hide Logs" else "▼ ${hydrationLogs.size} Logs",
                                        color = Color(0xFFA0A5BD),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Reset button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.10f),
                            modifier = Modifier
                                .height(34.dp)
                                .clickable(enabled = hydrationMl > 0) { showResetHydrationConfirm = true }
                                .testTag("btn_reset_hydration")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reset Day",
                                    color = if (hydrationMl > 0) Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.4f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Optional Expanded Hydration History
                    if (showHydrationHistory && hydrationLogs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(DexterSurfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "TODAY'S INTAKE LOGS",
                                color = Color(0xFFA0A5BD),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            hydrationLogs.forEach { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("💧", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("+${log.amountMl} ml", color = DexterOnBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Text(log.humanTime, color = Color(0xFFA0A5BD), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Sleep Telemetry Hero Card (Apple Sleep Stages)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sleep_card"),
                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF818CF8).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("😴", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Sleep Architecture", color = DexterOnBackground, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text("Nightly rest & recovery cycles", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = { showSleepDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF818CF8)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("btn_log_sleep_dialog")
                        ) {
                            Text("+ Log Sleep", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (latestSleep != null) {
                        // Visual Bedtime to Waketime schedule bar
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DexterSurfaceVariant)
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("🌙 BEDTIME", color = Color(0xFFA0A5BD), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(latestSleep.bedtime, color = DexterOnBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF818CF8).copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = "⏱ ${latestSleep.durationText}",
                                        color = Color(0xFF818CF8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("☀️ WAKE TIME", color = Color(0xFFA0A5BD), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(latestSleep.waketime, color = DexterOnBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Quality & Star Rating Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    repeat(latestSleep.qualityRating) {
                                        Text("⭐", fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "· ${latestSleep.qualityLabel}",
                                        color = DexterOnBackground,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                IconButton(
                                    onClick = { sleepToDelete = latestSleep },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Sleep",
                                        tint = Color(0xFFA0A5BD),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (latestSleep.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "\"${latestSleep.notes}\"",
                                    color = Color(0xFFA0A5BD),
                                    fontSize = 12.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    } else {
                        // Empty State for sleep
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DexterSurfaceVariant.copy(alpha = 0.5f))
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🌙", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No sleep session recorded yet", color = DexterOnBackground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Log your bedtime and wake schedule to monitor recovery", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 4. Apple Health State of Mind & Emotional Energy Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mood_card"),
                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFBBF24).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🧠", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("State of Mind", color = DexterOnBackground, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text("How are you feeling right now?", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                            }
                        }

                        latestMood?.let { m ->
                            Text(
                                text = "Last: ${m.humanTime}",
                                color = Color(0xFFA0A5BD),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "1-TAP STATE OF MIND CHECK-IN",
                        color = Color(0xFFA0A5BD),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 6 Emotional State Buttons in 2x3 Grid
                    val moodStates = listOf(
                        Triple("😊 Joyful", "High", Color(0xFFFBBF24)),
                        Triple("😌 Calm", "Medium", Color(0xFF38BDF8)),
                        Triple("⚡ Focused", "High", Color(0xFF818CF8)),
                        Triple("😐 Steady", "Medium", Color(0xFF94A3B8)),
                        Triple("🌧 Low", "Low", Color(0xFF60A5FA)),
                        Triple("🔥 Stressed", "High", Color(0xFFFB7185))
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            moodStates.take(3).forEach { (label, defaultEnergy, tint) ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = tint.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clickable {
                                            selectedMoodForCheckin = label
                                            showMoodDialog = true
                                        }
                                        .testTag("btn_mood_${label.filter { it.isLetter() }}")
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(label, color = tint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            moodStates.drop(3).forEach { (label, defaultEnergy, tint) ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = tint.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clickable {
                                            selectedMoodForCheckin = label
                                            showMoodDialog = true
                                        }
                                        .testTag("btn_mood_${label.filter { it.isLetter() }}")
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(label, color = tint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Recent Mood Logs Timeline
                    if (moodLogs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "RECENT CHECK-INS",
                            color = Color(0xFFA0A5BD),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DexterSurfaceVariant)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            moodLogs.take(5).forEach { moodItem ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(moodItem.mood, color = DexterOnBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color.White.copy(alpha = 0.08f),
                                                modifier = Modifier.height(18.dp)
                                            ) {
                                                Box(modifier = Modifier.padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                                                    Text("${moodItem.energyLevel} Energy", color = Color(0xFFA0A5BD), fontSize = 10.sp)
                                                }
                                            }
                                        }
                                        if (moodItem.note.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(moodItem.note, color = Color(0xFFA0A5BD), fontSize = 12.sp)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(moodItem.humanTime, color = Color(0xFFA0A5BD), fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { moodToDelete = moodItem },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete Mood Log",
                                                tint = Color(0xFFA0A5BD).copy(alpha = 0.6f),
                                                modifier = Modifier.size(14.dp)
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
    }

    // --- Dialogs ---

    // 1. Log Sleep Dialog
    if (showSleepDialog) {
        var bedClock by remember { mutableStateOf(ClockTime(23, 30)) }
        var wakeClock by remember { mutableStateOf(ClockTime(6, 50)) }
        var rating by remember { mutableStateOf(5) }
        var qualityLabel by remember { mutableStateOf("Restful") }
        var notesText by remember { mutableStateOf("") }

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
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("😴", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Log Sleep Session", color = DexterOnBackground, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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

                    // Auto-calculated duration pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF818CF8).copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Calculated Duration:", color = Color(0xFFA0A5BD), fontSize = 13.sp)
                        Text(
                            text = "⏱ $computedDuration",
                            color = Color(0xFF818CF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Quality Chips
                    Column {
                        Text("QUALITY STATE", color = Color(0xFFA0A5BD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Restful", "Deep", "Light", "Restless").forEach { q ->
                                val isSelected = qualityLabel.equals(q, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF818CF8).copy(alpha = 0.2f) else DexterSurfaceVariant,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8)) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clickable { qualityLabel = q }
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = q,
                                            color = if (isSelected) Color(0xFF818CF8) else DexterOnBackground,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Rating Stars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sleep Rating:", color = DexterOnBackground, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { star ->
                                Text(
                                    text = if (star <= rating) "⭐" else "☆",
                                    modifier = Modifier
                                        .clickable { rating = star }
                                        .padding(4.dp),
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }

                    // Notes
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Sleep Notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = Color(0xFF818CF8),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.logSleep(
                            bedClock.formattedString,
                            wakeClock.formattedString,
                            computedDuration,
                            rating,
                            qualityLabel,
                            notesText
                        )
                        showSleepDialog = false
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF818CF8)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_save_sleep")
                ) {
                    Text("Save Sleep", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSleepDialog = false }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // 2. Check-In State of Mind Dialog
    if (showMoodDialog) {
        var energy by remember { mutableStateOf("High") }
        var moodNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showMoodDialog = false },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧠", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("State of Mind Check-In", color = DexterOnBackground, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Chosen mood indicator
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFBBF24).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedMoodForCheckin,
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Energy Level Selector
                    Column {
                        Text("ENERGY LEVEL", color = Color(0xFFA0A5BD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "High" to "⚡ High",
                                "Medium" to "⚖️ Medium",
                                "Low" to "🔋 Low"
                            ).forEach { (value, label) ->
                                val isSelected = energy == value
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) DexterPrimary.copy(alpha = 0.18f) else DexterSurfaceVariant,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, DexterPrimary) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clickable { energy = value }
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) DexterPrimary else DexterOnBackground,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Optional Reflection Note
                    OutlinedTextField(
                        value = moodNote,
                        onValueChange = { moodNote = it },
                        label = { Text("Reflection / Note (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.logMood(selectedMoodForCheckin, energy, moodNote)
                        showMoodDialog = false
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_save_mood")
                ) {
                    Text("Check In", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoodDialog = false }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // 3. Reset Hydration Confirmation Guardrail
    if (showResetHydrationConfirm) {
        AlertDialog(
            onDismissRequest = { showResetHydrationConfirm = false },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Reset Today's Hydration?", color = DexterOnBackground, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will clear all $glassesCount glasses (${hydrationMl} ml) logged today. This action cannot be undone.",
                    color = Color(0xFFA0A5BD),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.resetHydrationToday()
                        showResetHydrationConfirm = false
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_confirm_reset_hydration")
                ) {
                    Text("Reset All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetHydrationConfirm = false }) {
                    Text("Keep Hydration", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // 4. Delete Sleep Log Confirmation Guardrail
    sleepToDelete?.let { sleepItem ->
        AlertDialog(
            onDismissRequest = { sleepToDelete = null },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Delete Sleep Record?", color = DexterOnBackground, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Delete sleep log of ${sleepItem.durationText} (${sleepItem.bedtime} – ${sleepItem.waketime})?",
                    color = Color(0xFFA0A5BD),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteSleepLog(sleepItem.id)
                        sleepToDelete = null
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sleepToDelete = null }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // 5. Delete Mood Log Confirmation Guardrail
    moodToDelete?.let { moodItem ->
        AlertDialog(
            onDismissRequest = { moodToDelete = null },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Delete Check-In?", color = DexterOnBackground, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Delete \"${moodItem.mood}\" logged at ${moodItem.humanTime}?",
                    color = Color(0xFFA0A5BD),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteMoodLog(moodItem.id)
                        moodToDelete = null
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { moodToDelete = null }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }
}

// ==========================================
// SEGMENT 10: Habits & Streaks (Apple HIG 2026 Redesign - Building & Breaking Habits)
// ==========================================
@Composable
private fun HabitsStreaksView(
    repository: DexterRepository,
    trigger: Long,
    onRefresh: () -> Unit
) {
    val habits = remember(trigger) { repository.listHabits() }
    var showAddHabitDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("ALL") } // "ALL", "BUILD", "QUIT"
    var habitToDelete by remember { mutableStateOf<HabitItem?>(null) }
    var habitToResetSlip by remember { mutableStateOf<HabitItem?>(null) }

    val buildHabits = remember(habits) { habits.filter { !it.isQuitHabit } }
    val quitHabits = remember(habits) { habits.filter { it.isQuitHabit } }

    val filteredHabits = remember(habits, selectedTab) {
        when (selectedTab) {
            "BUILD" -> buildHabits
            "QUIT" -> quitHabits
            else -> habits
        }
    }

    val maxStreak = remember(habits) { habits.maxOfOrNull { it.streakCount } ?: 0 }
    val buildDoneCount = remember(buildHabits) { buildHabits.count { it.isDoneToday } }
    val quitCleanCount = remember(quitHabits) { quitHabits.count { it.isDoneToday } }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddHabitDialog = true },
                containerColor = DexterPrimary,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp),
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Apple Fitness Style Summary Metric Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Flame Streak Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DexterSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔥", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Best Streak", color = Color(0xFFA0A5BD), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$maxStreak Days",
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Peak momentum",
                                color = Color(0xFFA0A5BD),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Building Habits Stat Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DexterSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌱", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Building", color = Color(0xFFA0A5BD), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$buildDoneCount/${buildHabits.size} Done",
                                color = Color(0xFF34D399),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Cultivating today",
                                color = Color(0xFFA0A5BD),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Quitting Bad Habits Stat Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DexterSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🛡️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Breaking", color = Color(0xFFA0A5BD), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$quitCleanCount/${quitHabits.size} Clean",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Bad habits held off",
                                color = Color(0xFFA0A5BD),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // 2. Apple HIG Segmented Filter Tabs (All / Build Good / Break Bad)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DexterSurface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "ALL" to "🌟 All (${habits.size})",
                        "BUILD" to "🌱 Build (${buildHabits.size})",
                        "QUIT" to "🛡️ Break Bad (${quitHabits.size})"
                    ).forEach { (key, label) ->
                        val isSelected = selectedTab == key
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) DexterSurfaceVariant else Color.Transparent,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable { selectedTab = key }
                                .testTag("tab_habit_$key")
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    color = if (isSelected) DexterOnBackground else Color(0xFFA0A5BD),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 3. Habit Cards List
            if (filteredHabits.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(DexterSurface)
                            .padding(vertical = 40.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (selectedTab == "QUIT") "🛡️" else "🌱", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (selectedTab == "QUIT") "No bad habits tracked yet" else "No habits found",
                                color = DexterOnBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (selectedTab == "QUIT")
                                    "Tap '+' to add a bad habit you want to quit and protect your clean streak."
                                else
                                    "Tap '+' to set up positive routines or bad habits to break.",
                                color = Color(0xFFA0A5BD),
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredHabits, key = { it.id }) { habit ->
                    if (habit.isQuitHabit) {
                        // Bad Habit Card (Breaking Bad Habits)
                        QuitBadHabitCard(
                            habit = habit,
                            onToggleCleanToday = {
                                repository.toggleHabitDoneToday(habit.id)
                                onRefresh()
                            },
                            onResetSlip = { habitToResetSlip = habit },
                            onDelete = { habitToDelete = habit }
                        )
                    } else {
                        // Good Habit Card (Building Habits)
                        BuildGoodHabitCard(
                            habit = habit,
                            onToggleDoneToday = {
                                repository.toggleHabitDoneToday(habit.id)
                                onRefresh()
                            },
                            onDelete = { habitToDelete = habit }
                        )
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    // 1. Add Habit Dialog (Supports both Build Good & Break Bad)
    if (showAddHabitDialog) {
        var habitType by remember { mutableStateOf("BUILD") } // "BUILD" vs "QUIT"
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Health") }
        var frequency by remember { mutableStateOf("Daily") }
        var quitReason by remember { mutableStateOf("") }
        var reminderClock by remember { mutableStateOf(ClockTime(8, 0)) }

        AlertDialog(
            onDismissRequest = { showAddHabitDialog = false },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (habitType == "BUILD") "🌱 Build New Habit" else "🛡️ Break Bad Habit",
                    color = DexterOnBackground,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Type Selector Toggle (Build vs Break)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DexterSurfaceVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (habitType == "BUILD") Color(0xFF34D399).copy(alpha = 0.2f) else Color.Transparent,
                            border = if (habitType == "BUILD") androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable { habitType = "BUILD" }
                                .testTag("btn_type_build")
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "🌱 Cultivate (Good)",
                                    color = if (habitType == "BUILD") Color(0xFF34D399) else Color(0xFFA0A5BD),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (habitType == "QUIT") Color(0xFF38BDF8).copy(alpha = 0.2f) else Color.Transparent,
                            border = if (habitType == "QUIT") androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable { habitType = "QUIT" }
                                .testTag("btn_type_quit")
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "🛡️ Break (Bad Habit)",
                                    color = if (habitType == "QUIT") Color(0xFF38BDF8) else Color(0xFFA0A5BD),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Habit Name Input
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = {
                            Text(
                                if (habitType == "BUILD") "Habit Name (e.g. Read 15 pages)"
                                else "Bad Habit to Quit (e.g. No late night junk food)"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_habit_name"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = if (habitType == "BUILD") Color(0xFF34D399) else Color(0xFF38BDF8),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // If Break Bad Habit: Reason to Quit
                    if (habitType == "QUIT") {
                        OutlinedTextField(
                            value = quitReason,
                            onValueChange = { quitReason = it },
                            label = { Text("Why do you want to quit? (Motivation)") },
                            placeholder = { Text("e.g. Better sleep, more energy, save money") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = DexterOnBackground,
                                unfocusedTextColor = DexterOnBackground,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Category Selector Chips
                    Column {
                        Text("CATEGORY", color = Color(0xFFA0A5BD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Health", "Focus", "Mind", "Fitness").forEach { cat ->
                                val isSelected = category.equals(cat, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) DexterPrimary.copy(alpha = 0.2f) else DexterSurfaceVariant,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, DexterPrimary) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clickable { category = cat }
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = cat,
                                            color = if (isSelected) DexterPrimary else DexterOnBackground,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Reminder Clock
                    ModernClockTimePicker(
                        label = "DAILY REMINDER / CHECK-IN",
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
                            repository.createHabit(
                                name = name,
                                frequency = frequency,
                                reminderTime = reminderClock.formattedString,
                                habitType = habitType,
                                category = category,
                                quitReason = quitReason
                            )
                            showAddHabitDialog = false
                            onRefresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (habitType == "BUILD") Color(0xFF34D399) else Color(0xFF38BDF8)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_save_habit")
                ) {
                    Text(
                        text = if (habitType == "BUILD") "Start Habit" else "Commit to Quit",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddHabitDialog = false }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // 2. Slip / Reset Streak Confirmation Guardrail (For Quitting Bad Habits)
    habitToResetSlip?.let { habit ->
        AlertDialog(
            onDismissRequest = { habitToResetSlip = null },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Reset Clean Streak?", color = DexterOnBackground, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Slip-ups happen on the journey of breaking habits. Recovery is not a straight line.",
                        color = Color(0xFFA0A5BD),
                        fontSize = 13.sp
                    )
                    Text(
                        "Resetting \"${habit.name}\" streak of ${habit.streakCount} days back to 0. You can start fresh today!",
                        color = DexterOnBackground,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.resetQuitHabitStreak(habit.id)
                        habitToResetSlip = null
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_confirm_slip_reset")
                ) {
                    Text("Reset Streak & Restart Fresh", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { habitToResetSlip = null }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // 3. Delete Habit Confirmation Guardrail
    habitToDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Delete Habit?", color = DexterOnBackground, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${habit.name}\"? Your ${habit.streakCount}-day streak will be permanently removed.",
                    color = Color(0xFFA0A5BD),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteHabit(habit.id)
                        habitToDelete = null
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { habitToDelete = null }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }
}

// -------------------------------------------------------------
// Component: Build Good Habit Card (Positive Habit Cultivation)
// -------------------------------------------------------------
@Composable
private fun BuildGoodHabitCard(
    habit: HabitItem,
    onToggleDoneToday: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}"),
        colors = CardDefaults.cardColors(containerColor = DexterSurface),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flame Badge
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (habit.streakCount > 0) Color(0xFFF59E0B).copy(alpha = 0.15f)
                        else DexterSurfaceVariant
                    )
                    .border(
                        1.dp,
                        if (habit.streakCount > 0) Color(0xFFF59E0B).copy(alpha = 0.35f)
                        else Color.White.copy(alpha = 0.05f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔥", fontSize = 16.sp)
                    Text(
                        text = "${habit.streakCount}d",
                        color = if (habit.streakCount > 0) Color(0xFFF59E0B) else Color(0xFFA0A5BD),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    color = DexterOnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Category pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF34D399).copy(alpha = 0.12f),
                        modifier = Modifier.height(18.dp)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                            Text(habit.category, color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "· ⏰ ${habit.reminderTime}",
                        color = Color(0xFFA0A5BD),
                        fontSize = 11.sp
                    )
                }
            }

            // 1-Tap Completion Checkmark (48dp touch target)
            Surface(
                shape = CircleShape,
                color = if (habit.isDoneToday) Color(0xFF34D399) else DexterSurfaceVariant,
                border = if (!habit.isDoneToday) androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.2f)) else null,
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onToggleDoneToday() }
                    .testTag("btn_toggle_habit_${habit.id}")
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Mark Habit Done",
                        tint = if (habit.isDoneToday) Color.Black else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Habit",
                    tint = Color(0xFFA0A5BD).copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Quit Bad Habit Card (Breaking / Avoiding Bad Habits)
// -------------------------------------------------------------
@Composable
private fun QuitBadHabitCard(
    habit: HabitItem,
    onToggleCleanToday: () -> Unit,
    onResetSlip: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}"),
        colors = CardDefaults.cardColors(containerColor = DexterSurface),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shield Clean Streak Badge
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🛡️", fontSize = 16.sp)
                        Text(
                            text = "${habit.streakCount}d",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = habit.name,
                            color = DexterOnBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF38BDF8).copy(alpha = 0.12f),
                            modifier = Modifier.height(18.dp)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                                Text("QUIT", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (habit.quitReason.isNotBlank()) {
                        Text(
                            text = "\"${habit.quitReason}\"",
                            color = Color(0xFFA0A5BD),
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            maxLines = 1
                        )
                    } else {
                        Text(
                            text = "Check-in at ${habit.reminderTime}",
                            color = Color(0xFFA0A5BD),
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Habit",
                        tint = Color(0xFFA0A5BD).copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Row: Clean Today Check vs I Slipped (Reset)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stayed Clean Button (48dp height for touch accessibility)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (habit.isDoneToday) Color(0xFF34D399).copy(alpha = 0.2f) else DexterSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (habit.isDoneToday) Color(0xFF34D399) else Color.White.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable { onToggleCleanToday() }
                        .testTag("btn_clean_today_${habit.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(if (habit.isDoneToday) "✅" else "🛡️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (habit.isDoneToday) "Clean Today!" else "Stayed Clean Today",
                            color = if (habit.isDoneToday) Color(0xFF34D399) else DexterOnBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Slipped button (trigger guardrail dialog)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f)),
                    modifier = Modifier
                        .height(44.dp)
                        .clickable { onResetSlip() }
                        .testTag("btn_slipped_${habit.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚡", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("I Slipped", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// SEGMENT 11: Reading & Knowledge Vault (Apple HIG 2026 Redesign - Personal Second Brain & RAG)
// ==========================================
@Composable
private fun KnowledgeVaultView(
    repository: DexterRepository,
    trigger: Long,
    onRefresh: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryTab by remember { mutableStateOf("ALL") } // "ALL", "CORE_MEMORY", "About Me", "Preferences", "Health & Rules", "Technical Notes"
    val docs = remember(trigger, searchQuery, selectedCategoryTab) {
        repository.listDocuments(query = searchQuery, categoryFilter = selectedCategoryTab)
    }
    val allDocs = remember(trigger) { repository.listDocuments() }
    val coreMemories = remember(allDocs) { allDocs.filter { it.isCoreMemory } }
    val totalWords = remember(allDocs) { allDocs.sumOf { it.wordCount } }

    var showAddDocDialog by remember { mutableStateOf(false) }
    var viewingDoc by remember { mutableStateOf<DocumentItem?>(null) }
    var docToDelete by remember { mutableStateOf<DocumentItem?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDocDialog = true },
                containerColor = DexterPrimary,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_doc")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Document or Personal Fact")
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

            // 1. Metric Summary Strip (Apple Books / AI Memory Glance)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Core AI Memory Stat Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🧠", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Memory", color = Color(0xFFA0A5BD), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${coreMemories.size} Facts",
                            color = Color(0xFF818CF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Injected in model",
                            color = Color(0xFFA0A5BD),
                            fontSize = 11.sp
                        )
                    }
                }

                // Total Knowledge Documents Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📚", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Vault", color = Color(0xFFA0A5BD), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${allDocs.size} Items",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Notes & Guides",
                            color = Color(0xFFA0A5BD),
                            fontSize = 11.sp
                        )
                    }
                }

                // Total Words Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📖", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Volume", color = Color(0xFFA0A5BD), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${totalWords}w",
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "≈ ${totalWords / 200}m read",
                            color = Color(0xFFA0A5BD),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Apple Spotlight Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search personal facts, notes, tags...", fontSize = 13.sp) },
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
                    focusedBorderColor = DexterPrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Apple HIG Horizontal Segmented Filter Bar
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val categories = listOf(
                    "ALL" to "🌟 All (${allDocs.size})",
                    "CORE_MEMORY" to "🧠 Core Memory (${coreMemories.size})",
                    "About Me" to "👤 About Me",
                    "Preferences" to "⚙️ Preferences",
                    "Health & Rules" to "🌿 Health & Rules",
                    "Technical Notes" to "💻 Tech Notes"
                )
                items(categories) { (key, label) ->
                    val isSelected = selectedCategoryTab == key
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) DexterPrimary.copy(alpha = 0.15f) else DexterSurface,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, DexterPrimary) else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier
                            .height(36.dp)
                            .clickable { selectedCategoryTab = key }
                            .testTag("tab_knowledge_$key")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) DexterPrimary else Color(0xFFA0A5BD),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Document / Personal Fact Cards
            if (docs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DexterSurface)
                        .padding(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📚", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matches for \"$searchQuery\"" else "No documents found",
                            color = DexterOnBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap '+' to add personal knowledge, profile rules, or technical notes to your vault.",
                            color = Color(0xFFA0A5BD),
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(docs, key = { it.id }) { doc ->
                        KnowledgeDocumentCard(
                            doc = doc,
                            onToggleCoreMemory = {
                                repository.toggleCoreMemory(doc.id)
                                onRefresh()
                            },
                            onOpenReader = { viewingDoc = doc },
                            onDelete = { docToDelete = doc }
                        )
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    // 1. Add Knowledge Document / Personal Fact Dialog
    if (showAddDocDialog) {
        var isCoreMemory by remember { mutableStateOf(selectedCategoryTab == "CORE_MEMORY" || selectedCategoryTab == "About Me") }
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(if (isCoreMemory) "About Me" else "Personal Profile") }
        var tags by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDocDialog = false },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (isCoreMemory) "🧠 Add Personal AI Memory" else "📚 Add Knowledge Document",
                    color = DexterOnBackground,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Core Memory Toggle Strip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCoreMemory) Color(0xFF818CF8).copy(alpha = 0.15f) else DexterSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCoreMemory) Color(0xFF818CF8) else Color.White.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCoreMemory = !isCoreMemory }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Pin as Core AI Model Memory",
                                    color = if (isCoreMemory) Color(0xFF818CF8) else DexterOnBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "Dexter always references this when answering prompts",
                                    color = Color(0xFFA0A5BD),
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = isCoreMemory,
                                onCheckedChange = { isCoreMemory = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF818CF8)
                                )
                            )
                        }
                    }

                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = {
                            Text(
                                if (isCoreMemory) "Title (e.g. Work Hours & Focus Preferences)"
                                else "Document Title (e.g. Edge Engine Architecture)"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = if (isCoreMemory) Color(0xFF818CF8) else DexterPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Chips
                    Column {
                        Text("CATEGORY", color = Color(0xFFA0A5BD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("About Me", "Preferences", "Health & Rules", "Work & Code").forEach { cat ->
                                val isSelected = category.equals(cat, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) DexterPrimary.copy(alpha = 0.2f) else DexterSurfaceVariant,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, DexterPrimary) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clickable { category = cat }
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = cat,
                                            color = if (isSelected) DexterPrimary else DexterOnBackground,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Content Body
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Full Content / Memory Text") },
                        placeholder = {
                            Text(
                                if (isCoreMemory) "e.g. My primary coding hours are 9 AM to 1 PM. I prefer concise bullet points..."
                                else "Write or paste notes and markdown here..."
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Tags
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Tags (e.g. #profile #routine #goals)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            repository.createDocument(
                                title = title,
                                content = content,
                                category = category,
                                source = if (isCoreMemory) "Personal Memory" else "Direct Entry",
                                fileType = if (isCoreMemory) "MEMORY" else "DOC",
                                isCoreMemory = isCoreMemory,
                                tags = tags
                            )
                            showAddDocDialog = false
                            onRefresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCoreMemory) Color(0xFF818CF8) else DexterPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_save_document")
                ) {
                    Text(
                        text = if (isCoreMemory) "Remember Fact" else "Save Document",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDocDialog = false }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // 2. Full-Screen Reader & Editor Modal
    viewingDoc?.let { doc ->
        var editableContent by remember(doc) { mutableStateOf(if (doc.content.isNotBlank()) doc.content else doc.excerpt) }
        var isEditing by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { viewingDoc = null },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (doc.isCoreMemory) Color(0xFF818CF8).copy(alpha = 0.2f) else DexterPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (doc.isCoreMemory) "🧠 CORE AI MEMORY" else doc.category.uppercase(),
                                color = if (doc.isCoreMemory) Color(0xFF818CF8) else DexterPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(onClick = { viewingDoc = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFA0A5BD))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(doc.title, color = DexterOnBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Indexed on ${doc.humanDate} · ${doc.wordCount} words (≈ ${doc.estimatedReadMinutes}m read)",
                        color = Color(0xFFA0A5BD),
                        fontSize = 11.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isEditing) {
                        OutlinedTextField(
                            value = editableContent,
                            onValueChange = { editableContent = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = DexterOnBackground,
                                unfocusedTextColor = DexterOnBackground,
                                focusedBorderColor = DexterPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DexterSurfaceVariant.copy(alpha = 0.5f))
                                .padding(14.dp)
                        ) {
                            LazyColumn {
                                item {
                                    Text(
                                        text = editableContent,
                                        color = DexterOnBackground,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }

                    if (doc.tags.isNotBlank()) {
                        Text(
                            text = "Tags: ${doc.tags}",
                            color = DexterPrimaryGlow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                if (isEditing) {
                    Button(
                        onClick = {
                            repository.updateDocument(
                                id = doc.id,
                                title = doc.title,
                                content = editableContent,
                                category = doc.category,
                                isCoreMemory = doc.isCoreMemory,
                                tags = doc.tags
                            )
                            isEditing = false
                            onRefresh()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { isEditing = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DexterSurfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Edit Note", color = DexterPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewingDoc = null }) {
                    Text("Close", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // 3. Delete Document Confirmation Guardrail
    docToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { docToDelete = null },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Delete Knowledge Item?", color = DexterOnBackground, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to remove \"${doc.title}\"? If this is part of your AI Core Memory, Dexter will no longer reference it.",
                    color = Color(0xFFA0A5BD),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteDocument(doc.id)
                        docToDelete = null
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { docToDelete = null }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }
}

// -------------------------------------------------------------
// Component: Apple Notes Styled Knowledge Document Card
// -------------------------------------------------------------
@Composable
private fun KnowledgeDocumentCard(
    doc: DocumentItem,
    onToggleCoreMemory: () -> Unit,
    onOpenReader: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("doc_card_${doc.id}")
            .clickable { onOpenReader() },
        colors = CardDefaults.cardColors(containerColor = DexterSurface),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (doc.isCoreMemory) Color(0xFF818CF8).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Category Badge + Core Memory Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Category Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.height(20.dp)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                            Text(doc.category, color = Color(0xFFA0A5BD), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (doc.isCoreMemory) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF818CF8).copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.4f)),
                            modifier = Modifier.height(20.dp)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                Text("🧠 AI MEMORY", color = Color(0xFF818CF8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Text(
                    text = "${doc.wordCount} words · ≈${doc.estimatedReadMinutes}m read",
                    color = Color(0xFFA0A5BD),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = doc.title,
                color = DexterOnBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Excerpt / Preview text
            Text(
                text = doc.cleanPreview,
                color = Color(0xFFA0A5BD),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 3
            )

            if (doc.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = doc.tags,
                    color = DexterPrimaryGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pin / Unpin from Core Memory
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (doc.isCoreMemory) Color(0xFF818CF8).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (doc.isCoreMemory) Color(0xFF818CF8).copy(alpha = 0.3f) else Color.Transparent
                    ),
                    modifier = Modifier
                        .height(34.dp)
                        .clickable { onToggleCoreMemory() }
                        .testTag("btn_toggle_memory_${doc.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (doc.isCoreMemory) "📌 In AI Context" else "Pin to Memory", color = if (doc.isCoreMemory) Color(0xFF818CF8) else Color(0xFFA0A5BD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Read & Edit button
                    TextButton(onClick = onOpenReader) {
                        Text("📖 Read & Edit", color = DexterPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFA0A5BD).copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SEGMENT 12: Data Gathering & Raw Intake Hub
// ==========================================
@Composable
private fun QuickCaptureView(
    repository: DexterRepository,
    trigger: Long,
    onRefresh: () -> Unit
) {
    DataGatheringHubView(repository, trigger, onRefresh)
}

@Composable
private fun DataGatheringHubView(
    repository: DexterRepository,
    trigger: Long,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val allNotes = remember(trigger) { repository.listCaptureNotes(includeProcessed = true) }
    val schedules = remember(trigger) { repository.listIntakeSchedules() }

    var selectedTab by remember { mutableStateOf("ALL") }
    var quickInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("General") }

    // Dialog & Action States
    var showVoiceDialog by remember { mutableStateOf(false) }
    var activeIntakeSession by remember { mutableStateOf<IntakeScheduleItem?>(null) }
    var editingScheduleTime by remember { mutableStateOf<IntakeScheduleItem?>(null) }
    var itemToPromoteToTask by remember { mutableStateOf<CaptureItem?>(null) }
    var itemToPromoteToKnowledge by remember { mutableStateOf<CaptureItem?>(null) }
    var itemToPromoteToSchedule by remember { mutableStateOf<CaptureItem?>(null) }
    var itemToDelete by remember { mutableStateOf<CaptureItem?>(null) }
    var synthesisResult by remember { mutableStateOf<BriefingItem?>(null) }

    val unprocessedCount = allNotes.count { !it.isProcessed }
    val voiceCount = allNotes.count { it.sourceType.equals("VOICE", ignoreCase = true) }
    val scheduledCount = allNotes.count { it.sourceType.startsWith("SCHEDULED", ignoreCase = true) }
    val processedCount = allNotes.count { it.isProcessed }

    val filteredNotes = remember(allNotes, selectedTab) {
        when (selectedTab) {
            "UNPROCESSED" -> allNotes.filter { !it.isProcessed }
            "VOICE" -> allNotes.filter { it.sourceType.equals("VOICE", ignoreCase = true) }
            "SCHEDULED" -> allNotes.filter { it.sourceType.startsWith("SCHEDULED", ignoreCase = true) }
            "PROCESSED" -> allNotes.filter { it.isProcessed }
            else -> allNotes
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))

            // HIG Metric Summary Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("RAW INTAKE", fontSize = 10.sp, color = Color(0xFFA0A5BD), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$unprocessedCount", fontSize = 22.sp, color = DexterPrimary, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("active", fontSize = 11.sp, color = Color(0xFFA0A5BD))
                        }
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("VOICE DUMPS", fontSize = 10.sp, color = Color(0xFFA0A5BD), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$voiceCount", fontSize = 22.sp, color = Color(0xFF64B5F6), fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("spoken", fontSize = 11.sp, color = Color(0xFFA0A5BD))
                        }
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("WINDOWS", fontSize = 10.sp, color = Color(0xFFA0A5BD), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            val activeWindows = schedules.count { it.isEnabled }
                            Text("$activeWindows/3", fontSize = 22.sp, color = Color(0xFF81C784), fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("daily", fontSize = 11.sp, color = Color(0xFFA0A5BD))
                        }
                    }
                }
            }
        }

        // ==========================================
        // 1. INTAKE TRIGGER CENTER (VOICE & SCHEDULES)
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Intake Trigger Center",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DexterOnBackground
                            )
                            Text(
                                text = "Collect unformatted thoughts via voice command or time triggers",
                                fontSize = 12.sp,
                                color = Color(0xFFA0A5BD)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Apple Voice Memos Hero Button
                    Button(
                        onClick = { showVoiceDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_voice_intake_trigger"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E2638)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF64B5F6).copy(alpha = 0.35f))
                    ) {
                        Text("🎙️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Record Voice Command / Thought Dump",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE3F2FD)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scheduled Daily Ingestion Windows
                    Text(
                        text = "SCHEDULED GATHERING WINDOWS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA0A5BD),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    schedules.forEach { schedule ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF10131B)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(schedule.icon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = schedule.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DexterOnBackground
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = Color.White.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.clickable { editingScheduleTime = schedule }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = schedule.scheduledTime,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = DexterPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Time",
                                                        tint = DexterPrimary,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = schedule.promptQuestion.take(45) + "...",
                                            fontSize = 11.sp,
                                            color = Color(0xFFA0A5BD),
                                            maxLines = 1
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { activeIntakeSession = schedule },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(Color(0xFF2A344A), CircleShape)
                                            .testTag("btn_trigger_schedule_${schedule.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Trigger Intake Now",
                                            tint = DexterPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = schedule.isEnabled,
                                        onCheckedChange = { checked ->
                                            repository.toggleIntakeSchedule(schedule.id, checked)
                                            onRefresh()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = DexterPrimary,
                                            uncheckedThumbColor = Color(0xFFA0A5BD),
                                            uncheckedTrackColor = Color(0xFF232838)
                                        ),
                                        modifier = Modifier.size(width = 44.dp, height = 24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. DAILY INTELLIGENCE SYNTHESIS ENGINE
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141926)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✨", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Intake Synthesis Engine",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DexterOnBackground
                            )
                            Text(
                                text = "Transforms raw voice dumps & daily gathering into executive briefs",
                                fontSize = 11.sp,
                                color = Color(0xFFA0A5BD)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val run = repository.synthesizeMorningBriefingFromIntake()
                                synthesisResult = run
                                onRefresh()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_synthesize_morning_briefing"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF253328)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.4f))
                        ) {
                            Text("🌅 Morning Brief", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5D6A7))
                        }

                        Button(
                            onClick = {
                                val run = repository.synthesizeEndOfDaySummaryFromIntake()
                                synthesisResult = run
                                onRefresh()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_synthesize_evening_summary"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A283E)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFB39DDB).copy(alpha = 0.4f))
                        ) {
                            Text("🌙 End-of-Day Summary", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD1C4E9))
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. RAPID TEXT & CLIPBOARD INGESTION BAR
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "MANUAL RAW INTAKE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA0A5BD),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = quickInput,
                        onValueChange = { quickInput = it },
                        placeholder = { Text("Quick raw thought, sensory observation, or note...", fontSize = 13.sp, color = Color(0xFF6B7280)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_quick_capture"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color(0xFF0F1117),
                            unfocusedContainerColor = Color(0xFF0F1117)
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Pill Selector
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val categories = listOf("General", "Thought", "Task", "Expense", "Meeting", "Reflection")
                        items(categories) { cat ->
                            val isSelected = selectedCategory == cat
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) DexterPrimary else Color(0xFF1F2430),
                                modifier = Modifier.clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else Color(0xFFA0A5BD),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                if (clipText.isNotBlank()) {
                                    quickInput = clipText
                                    Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Clipboard empty", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("📋", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste Clipboard", fontSize = 12.sp, color = Color(0xFFA0A5BD))
                        }

                        Button(
                            onClick = {
                                if (quickInput.isNotBlank()) {
                                    repository.createCaptureNote(
                                        noteText = quickInput,
                                        sourceType = "TEXT",
                                        categoryTag = selectedCategory,
                                        scheduledWindow = "Ad-hoc"
                                    )
                                    quickInput = ""
                                    onRefresh()
                                }
                            },
                            enabled = quickInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("btn_send_capture")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ingest Raw Note", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. TRIAGE STREAM & FILTER PILLS
        // ==========================================
        item {
            Text(
                text = "RAW INTAKE STREAM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA0A5BD),
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val tabs = listOf(
                    "ALL" to "All (${allNotes.size})",
                    "UNPROCESSED" to "📥 Raw (${unprocessedCount})",
                    "VOICE" to "🎙️ Voice (${voiceCount})",
                    "SCHEDULED" to "⏰ Scheduled (${scheduledCount})",
                    "PROCESSED" to "✓ Routed (${processedCount})"
                )
                items(tabs) { (key, label) ->
                    val isSelected = selectedTab == key
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) DexterPrimary else Color(0xFF1E2330),
                        modifier = Modifier
                            .clickable { selectedTab = key }
                            .testTag("tab_filter_$key")
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else Color(0xFFA0A5BD),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        if (filteredNotes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📥", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No intake notes in this stream",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DexterOnBackground
                        )
                        Text(
                            text = "Record a voice dump or use scheduled checkpoints above.",
                            fontSize = 12.sp,
                            color = Color(0xFFA0A5BD)
                        )
                    }
                }
            }
        }

        items(filteredNotes, key = { it.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("capture_card_${item.id}"),
                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header metadata row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF22293A)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.sourceIcon, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = item.sourceLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF90CAF9)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.05f)
                            ) {
                                Text(
                                    text = item.categoryTag,
                                    fontSize = 10.sp,
                                    color = Color(0xFFA0A5BD),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = item.humanTime,
                            fontSize = 11.sp,
                            color = Color(0xFFA0A5BD)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = item.noteText,
                        color = DexterOnBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 20.sp
                    )

                    if (item.isProcessed) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1B3222)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Processed",
                                    tint = Color(0xFF81C784),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val routeLabel = when (item.status) {
                                    "ROUTED_TASK" -> "Routed to Task"
                                    "ROUTED_KNOWLEDGE" -> "Routed to Knowledge Vault"
                                    "ROUTED_SCHEDULE" -> "Routed to Schedule"
                                    "ROUTED_FINANCE" -> "Routed to Finance Ledger"
                                    else -> "Processed & Archived"
                                }
                                Text(routeLabel, fontSize = 11.sp, color = Color(0xFFA5D6A7), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1-Tap Triage Action Row (48dp Touch Targets)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { itemToDelete = item },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("btn_delete_capture_${item.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Discard Note", tint = Color(0xFFEF5350).copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!item.isProcessed) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E2838),
                                    border = BorderStroke(1.dp, Color(0xFF64B5F6).copy(alpha = 0.3f)),
                                    modifier = Modifier.clickable { itemToPromoteToTask = item }
                                ) {
                                    Text(
                                        text = "➡️ To Task",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF90CAF9),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF2C243B),
                                    border = BorderStroke(1.dp, Color(0xFFBA68C8).copy(alpha = 0.3f)),
                                    modifier = Modifier.clickable { itemToPromoteToKnowledge = item }
                                ) {
                                    Text(
                                        text = "🧠 Memory",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFCE93D8),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF2B2F23),
                                    border = BorderStroke(1.dp, Color(0xFFAED581).copy(alpha = 0.3f)),
                                    modifier = Modifier.clickable { itemToPromoteToSchedule = item }
                                ) {
                                    Text(
                                        text = "🗓️ Schedule",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC5E1A5),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        repository.markCaptureProcessed(item.id)
                                        onRefresh()
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF263238), CircleShape)
                                        .testTag("btn_mark_processed_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Mark Processed",
                                        tint = DexterPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOGS & OVERLAYS
    // ==========================================

    // 1. Voice Intake Dialog
    if (showVoiceDialog) {
        VoiceIntakeSheetDialog(
            onDismiss = { showVoiceDialog = false },
            onSaveVoiceNote = { text ->
                repository.createCaptureNote(
                    noteText = text,
                    sourceType = "VOICE",
                    categoryTag = "Thought",
                    scheduledWindow = "Ad-hoc"
                )
                showVoiceDialog = false
                onRefresh()
            }
        )
    }

    // 2. Scheduled Intake Session Dialog
    activeIntakeSession?.let { schedule ->
        ScheduledIntakeSessionDialog(
            schedule = schedule,
            onDismiss = { activeIntakeSession = null },
            onSaveIntake = { text, tag ->
                val sourceType = when (schedule.targetType) {
                    "MORNING_BRIEFING" -> "SCHEDULED_MORNING"
                    "END_OF_DAY_SUMMARY" -> "SCHEDULED_EVENING"
                    else -> "SCHEDULED_MIDDAY"
                }
                repository.createCaptureNote(
                    noteText = text,
                    sourceType = sourceType,
                    categoryTag = tag,
                    scheduledWindow = schedule.name
                )
                activeIntakeSession = null
                onRefresh()
            }
        )
    }

    // 3. Edit Schedule Time Dialog
    editingScheduleTime?.let { schedule ->
        var tempTime by remember { mutableStateOf(ClockTime.fromFormattedString(schedule.scheduledTime)) }

        AlertDialog(
            onDismissRequest = { editingScheduleTime = null },
            containerColor = DexterSurface,
            title = {
                Text("Edit ${schedule.name} Time", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DexterOnBackground)
            },
            text = {
                Column {
                    ModernClockTimePicker(
                        label = "GATHERING WINDOW TIME",
                        time = tempTime,
                        onTimeChanged = { tempTime = it },
                        presets = listOf(
                            "🌅 07:00 AM" to ClockTime(7, 0),
                            "☀️ 08:00 AM" to ClockTime(8, 0),
                            "🥪 01:30 PM" to ClockTime(13, 30),
                            "🌆 08:30 PM" to ClockTime(20, 30),
                            "🌙 09:30 PM" to ClockTime(21, 30)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.updateIntakeScheduleTime(schedule.id, tempTime.formattedString)
                        editingScheduleTime = null
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary)
                ) {
                    Text("Save Time", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingScheduleTime = null }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // 4. Promote To Task Dialog
    itemToPromoteToTask?.let { capture ->
        var taskTitle by remember { mutableStateOf(capture.noteText) }
        var selectedPriority by remember { mutableStateOf(Priority.NORMAL) }
        var selectedCategory by remember { mutableStateOf("General") }

        AlertDialog(
            onDismissRequest = { itemToPromoteToTask = null },
            containerColor = DexterSurface,
            title = { Text("Promote to Task", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DexterOnBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Title / Action:", fontSize = 11.sp, color = Color(0xFFA0A5BD))
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        )
                    )

                    Text("Priority:", fontSize = 11.sp, color = Color(0xFFA0A5BD))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(Priority.LOW, Priority.NORMAL, Priority.HIGH, Priority.CRITICAL).forEach { prio ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedPriority == prio) DexterPrimary else Color(0xFF222838),
                                modifier = Modifier.clickable { selectedPriority = prio }
                            ) {
                                Text(
                                    text = prio.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedPriority == prio) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedPriority == prio) Color.Black else Color(0xFFA0A5BD),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.promoteCaptureToTask(capture.id, taskTitle, selectedPriority, selectedCategory)
                        itemToPromoteToTask = null
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary)
                ) {
                    Text("Create Task", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToPromoteToTask = null }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // 5. Promote To Knowledge Vault Dialog
    itemToPromoteToKnowledge?.let { capture ->
        var docTitle by remember { mutableStateOf(capture.noteText.take(40)) }
        var docContent by remember { mutableStateOf(capture.noteText) }
        var isCoreMemory by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { itemToPromoteToKnowledge = null },
            containerColor = DexterSurface,
            title = { Text("Promote to Knowledge Vault", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DexterOnBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Document Title:", fontSize = 11.sp, color = Color(0xFFA0A5BD))
                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = { docTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground
                        )
                    )

                    Text("Knowledge Content:", fontSize = 11.sp, color = Color(0xFFA0A5BD))
                    OutlinedTextField(
                        value = docContent,
                        onValueChange = { docContent = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isCoreMemory = !isCoreMemory }
                    ) {
                        Checkbox(
                            checked = isCoreMemory,
                            onCheckedChange = { isCoreMemory = it },
                            colors = CheckboxDefaults.colors(checkedColor = DexterPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pin as Core Memory for AI Context", fontSize = 12.sp, color = DexterOnBackground)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.promoteCaptureToKnowledge(
                            captureId = capture.id,
                            title = docTitle,
                            content = docContent,
                            category = "Personal Knowledge",
                            isCoreMemory = isCoreMemory
                        )
                        itemToPromoteToKnowledge = null
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary)
                ) {
                    Text("Save to Vault", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToPromoteToKnowledge = null }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // 6. Promote To Schedule Dialog
    itemToPromoteToSchedule?.let { capture ->
        var blockTitle by remember { mutableStateOf(capture.noteText.take(40)) }
        var startTime by remember { mutableStateOf("02:00 PM") }
        var endTime by remember { mutableStateOf("03:00 PM") }

        AlertDialog(
            onDismissRequest = { itemToPromoteToSchedule = null },
            containerColor = DexterSurface,
            title = { Text("Promote to Schedule Block", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DexterOnBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Block Title:", fontSize = 11.sp, color = Color(0xFFA0A5BD))
                    OutlinedTextField(
                        value = blockTitle,
                        onValueChange = { blockTitle = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Start Time:", fontSize = 11.sp, color = Color(0xFFA0A5BD))
                            OutlinedTextField(value = startTime, onValueChange = { startTime = it })
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("End Time:", fontSize = 11.sp, color = Color(0xFFA0A5BD))
                            OutlinedTextField(value = endTime, onValueChange = { endTime = it })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.promoteCaptureToSchedule(capture.id, blockTitle, startTime, endTime, "Deep Work")
                        itemToPromoteToSchedule = null
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary)
                ) {
                    Text("Create Block", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToPromoteToSchedule = null }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // 7. Synthesis Result Dialog (Morning Briefing & End-of-Day Summary)
    synthesisResult?.let { briefing ->
        AlertDialog(
            onDismissRequest = { synthesisResult = null },
            containerColor = Color(0xFF141722),
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(briefing.title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = DexterOnBackground)
                }
            },
            text = {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1A1F2E),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "DETERMINISTIC INTELLIGENCE SYNTHESIS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DexterPrimary,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = briefing.summaryText,
                                fontSize = 13.sp,
                                color = DexterOnBackground,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tasks: ${briefing.tasksCount}", fontSize = 11.sp, color = Color(0xFFA0A5BD))
                                Text("Weather: ${briefing.weatherText}", fontSize = 11.sp, color = Color(0xFFA0A5BD))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { synthesisResult = null },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary)
                ) {
                    Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 8. Delete Confirmation Guardrail
    itemToDelete?.let { capture ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = DexterSurface,
            title = { Text("Discard Raw Intake?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DexterOnBackground) },
            text = {
                Text(
                    text = "Are you sure you want to discard this raw note? This action cannot be undone.\n\n\"${capture.noteText.take(60)}...\"",
                    fontSize = 13.sp,
                    color = Color(0xFFA0A5BD)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteCapture(capture.id)
                        itemToDelete = null
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                ) {
                    Text("Discard", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Keep Note", color = Color(0xFFA0A5BD))
                }
            }
        )
    }
}

// ==========================================
// VOICE INTAKE MODAL (Waveforms & Dictation)
// ==========================================
@Composable
private fun VoiceIntakeSheetDialog(
    onDismiss: () -> Unit,
    onSaveVoiceNote: (String) -> Unit
) {
    var recognizedText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
            border = BorderStroke(1.dp, Color(0xFF64B5F6).copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Command Intake",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DexterOnBackground
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFA0A5BD))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pulsing Audio Visualizer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color(0xFF0C0F17), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                        val barCount = 18
                        val barWidth = 6.dp.toPx()
                        val spacing = (size.width - (barCount * barWidth)) / (barCount - 1)
                        for (i in 0 until barCount) {
                            val factor = if (isRecording) {
                                kotlin.math.sin(i * 0.4 + System.currentTimeMillis() / 200.0).toFloat().coerceIn(0.2f, 1.0f)
                            } else {
                                0.25f + (i % 3) * 0.15f
                            }
                            val barHeight = size.height * 0.7f * factor
                            val x = i * (barWidth + spacing)
                            val y = (size.height - barHeight) / 2
                            drawRoundRect(
                                color = if (isRecording) Color(0xFF64B5F6) else Color(0xFF455A64),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Spoken Transcript Input Area
                OutlinedTextField(
                    value = recognizedText,
                    onValueChange = { recognizedText = it },
                    placeholder = { Text("Speak or type spoken command...", fontSize = 13.sp, color = Color(0xFF6B7280)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DexterOnBackground,
                        unfocusedTextColor = DexterOnBackground,
                        focusedBorderColor = Color(0xFF64B5F6).copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = Color(0xFF0C0F17),
                        unfocusedContainerColor = Color(0xFF0C0F17)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Spoken simulation quick pills (Ideal for emulator testing)
                Text("QUICK TEST VOICE COMMANDS:", fontSize = 10.sp, color = Color(0xFFA0A5BD), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                val sampleVoiceNotes = listOf(
                    "Logged ₹450 for team lunch at cafe",
                    "Need to finalize architecture proposal tomorrow",
                    "Follow up with Vikram about client contract",
                    "Schedule deep focus sprint at 3:00 PM"
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    sampleVoiceNotes.forEach { sample ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1B2232),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    recognizedText = sample
                                    isRecording = true
                                }
                        ) {
                            Text(
                                text = "“$sample”",
                                fontSize = 11.sp,
                                color = Color(0xFF90CAF9),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isRecording = !isRecording
                            if (isRecording && recognizedText.isBlank()) {
                                recognizedText = "Voice observation: Synchronized local intent fast-path."
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text(if (isRecording) "Listening..." else "🎙️ Start Mic", fontSize = 12.sp, color = Color(0xFF64B5F6))
                    }

                    Button(
                        onClick = {
                            if (recognizedText.isNotBlank()) {
                                onSaveVoiceNote(recognizedText)
                            }
                        },
                        enabled = recognizedText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Save Intake", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ==========================================
// SCHEDULED INTAKE SESSION DIALOG
// ==========================================
@Composable
private fun ScheduledIntakeSessionDialog(
    schedule: IntakeScheduleItem,
    onDismiss: () -> Unit,
    onSaveIntake: (String, String) -> Unit
) {
    var sessionText by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("Reflection") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141824)),
            border = BorderStroke(1.dp, DexterPrimary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(schedule.icon, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = schedule.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DexterOnBackground
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFA0A5BD))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Guided Question Prompt Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1D2334),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("CHECKPOINT PROMPT:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DexterPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = schedule.promptQuestion,
                            fontSize = 13.sp,
                            color = DexterOnBackground,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = sessionText,
                    onValueChange = { sessionText = it },
                    placeholder = { Text("Log your reflections, tasks, or state...", fontSize = 13.sp, color = Color(0xFF6B7280)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DexterOnBackground,
                        unfocusedTextColor = DexterOnBackground,
                        focusedBorderColor = DexterPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedContainerColor = Color(0xFF0C0E15),
                        unfocusedContainerColor = Color(0xFF0C0E15)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("INTAKE TAG:", fontSize = 10.sp, color = Color(0xFFA0A5BD), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Reflection", "Priority", "Expense", "Accomplishment", "Blocker").forEach { tag ->
                        val isSelected = selectedTag == tag
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) DexterPrimary else Color(0xFF1F2536),
                            modifier = Modifier.clickable { selectedTag = tag }
                        ) {
                            Text(
                                text = tag,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.Black else Color(0xFFA0A5BD),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (sessionText.isNotBlank()) {
                            onSaveIntake(sessionText, selectedTag)
                        }
                    },
                    enabled = sessionText.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Ingest & Synchronize Intake", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
    val context = LocalContext.current
    val allAlerts = remember(trigger) { repository.listAttentionEvents(includeAcknowledged = true, includeSnoozed = true) }

    var selectedFilter by remember { mutableStateOf("ACTIVE") }
    var showAddAlertDialog by remember { mutableStateOf(false) }
    var alertToSnooze by remember { mutableStateOf<AttentionItem?>(null) }
    var alertToDelete by remember { mutableStateOf<AttentionItem?>(null) }

    val activeAlerts = allAlerts.filter { !it.isAcknowledged && !it.isSnoozed }
    val snoozedAlerts = allAlerts.filter { !it.isAcknowledged && it.isSnoozed }
    val resolvedAlerts = allAlerts.filter { it.isAcknowledged }
    val criticalAlerts = allAlerts.filter { !it.isAcknowledged && it.urgency.equals("CRITICAL", ignoreCase = true) }
    val highAlerts = allAlerts.filter { !it.isAcknowledged && (it.urgency.equals("HIGH", ignoreCase = true) || it.urgency.equals("URGENT", ignoreCase = true)) }

    val displayedAlerts = remember(allAlerts, selectedFilter) {
        when (selectedFilter) {
            "ACTIVE" -> activeAlerts
            "CRITICAL" -> criticalAlerts
            "HIGH" -> highAlerts
            "SNOOZED" -> snoozedAlerts
            "RESOLVED" -> resolvedAlerts
            else -> allAlerts
        }
    }

    // Cognitive Load Meter Evaluation
    val cognitiveLoadTitle = when {
        activeAlerts.isEmpty() -> "🟢 CALM MIND"
        activeAlerts.size <= 2 -> "🟢 OPTIMAL BANDWIDTH"
        activeAlerts.size <= 4 -> "🟡 HEIGHTENED FOCUS"
        else -> "🔴 ATTENTION OVERLOAD"
    }

    val cognitiveLoadSubtitle = when {
        activeAlerts.isEmpty() -> "Zero active bottlenecks. Peak deep work bandwidth available."
        activeAlerts.size <= 2 -> "${activeAlerts.size} actionable items. Cognitive pressure minimal."
        activeAlerts.size <= 4 -> "${activeAlerts.size} active priorities require timely triage."
        else -> "${activeAlerts.size} alerts pending! Snooze or resolve non-essentials to prevent cognitive burnout."
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddAlertDialog = true },
                containerColor = DexterPrimary,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("fab_add_alert")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alert", modifier = Modifier.size(24.dp))
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ==========================================
            // 1. COGNITIVE LOAD GAUGE (HERO CARD)
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cognitiveLoadTitle,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        activeAlerts.size <= 2 -> Color(0xFF81C784)
                                        activeAlerts.size <= 4 -> Color(0xFFFFB74D)
                                        else -> Color(0xFFFF5252)
                                    },
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Attention & Priority Dispatch",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DexterOnBackground
                                )
                            }

                            // 1-Tap OS Scan Button
                            Button(
                                onClick = {
                                    val count = repository.scanAndSynthesizeOperatingAlerts()
                                    Toast.makeText(
                                        context,
                                        if (count > 0) "Detected and ingested $count alerts across segments" else "Operating scan complete: 0 new bottlenecks",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onRefresh()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF64B5F6).copy(alpha = 0.35f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_scan_attention")
                            ) {
                                Text("⚡ Scan OS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90CAF9))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = cognitiveLoadSubtitle,
                            fontSize = 12.sp,
                            color = Color(0xFFA0A5BD),
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Visual Cognitive Load Gauge Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF232838))
                        ) {
                            val activeRatio = (activeAlerts.size / 6f).coerceIn(0.05f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(activeRatio)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF81C784),
                                                if (activeAlerts.size > 2) Color(0xFFFFB74D) else Color(0xFF81C784),
                                                if (activeAlerts.size > 4) Color(0xFFFF5252) else Color(0xFFFFB74D)
                                            )
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Metric pills row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF261D22),
                                border = BorderStroke(1.dp, Color(0xFFFF1744).copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🚨 CRITICAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                                    Text("${criticalAlerts.size}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF26231C),
                                border = BorderStroke(1.dp, Color(0xFFFF9100).copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("⚡ HIGH", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D))
                                    Text("${highAlerts.size}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E222F),
                                border = BorderStroke(1.dp, Color(0xFF64B5F6).copy(alpha = 0.25f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("⏰ SNOOZED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90CAF9))
                                    Text("${snoozedAlerts.size}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1B261F),
                                border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.25f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("✓ RESOLVED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5D6A7))
                                    Text("${resolvedAlerts.size}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 2. FILTER PILLS ROW
            // ==========================================
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filters = listOf(
                        "ACTIVE" to "Active (${activeAlerts.size})",
                        "CRITICAL" to "🚨 Critical (${criticalAlerts.size})",
                        "HIGH" to "⚡ High (${highAlerts.size})",
                        "SNOOZED" to "⏰ Snoozed (${snoozedAlerts.size})",
                        "RESOLVED" to "✓ Resolved (${resolvedAlerts.size})",
                        "ALL" to "All (${allAlerts.size})"
                    )
                    items(filters) { (key, label) ->
                        val isSelected = selectedFilter == key
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) DexterPrimary else Color(0xFF1C212E),
                            modifier = Modifier
                                .clickable { selectedFilter = key }
                                .testTag("filter_tab_$key")
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else Color(0xFFA0A5BD),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // Empty state
            if (displayedAlerts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DexterSurface),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🛡️", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Alerts in this View",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DexterOnBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Attention stream is fully synchronized and verified healthy.",
                                fontSize = 12.sp,
                                color = Color(0xFFA0A5BD)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 3. ALERT CARDS STREAM (APPLE HIG 2026)
            // ==========================================
            items(displayedAlerts, key = { it.id }) { alert ->
                val cardBorderColor = when (alert.urgency.uppercase()) {
                    "CRITICAL" -> Color(0xFFFF1744)
                    "HIGH", "URGENT" -> Color(0xFFFF9100)
                    "NORMAL" -> Color(0xFF4FC3F7)
                    else -> Color.White.copy(alpha = 0.12f)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alert_card_${alert.id}"),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.2.dp, cardBorderColor.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header row with category & urgency
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1E2434)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(alert.categoryIcon, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = alert.category,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF90CAF9)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = cardBorderColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = alert.urgency.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = cardBorderColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Text(
                                text = alert.humanTime,
                                fontSize = 11.sp,
                                color = Color(0xFFA0A5BD)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Title
                        Text(
                            text = alert.title,
                            color = DexterOnBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 21.sp
                        )

                        if (alert.detail.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = alert.detail,
                                color = Color(0xFFA0A5BD),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }

                        // Status banners: Snoozed or Resolved
                        if (alert.isSnoozed) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF282318),
                                border = BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⏰", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Snoozed · ${alert.remainingSnoozeMinutes}m remaining",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFFFCC80)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF382F1C),
                                        modifier = Modifier.clickable {
                                            repository.unsnoozeAttention(alert.id)
                                            onRefresh()
                                        }
                                    ) {
                                        Text(
                                            text = "Wake Up",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DexterPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        } else if (alert.isAcknowledged) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1B281E),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Resolved", tint = Color(0xFF81C784), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Resolved & Acknowledged",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFA5D6A7)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Row (48dp touch targets)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { alertToDelete = alert },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("btn_delete_alert_${alert.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Discard Alert", tint = Color(0xFFEF5350).copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (!alert.isAcknowledged) {
                                    OutlinedButton(
                                        onClick = { alertToSnooze = alert },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .height(44.dp)
                                            .testTag("btn_snooze_alert_${alert.id}")
                                    ) {
                                        Text("⏰ Snooze", fontSize = 12.sp, color = Color(0xFFA0A5BD), fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = {
                                            repository.acknowledgeAttention(alert.id)
                                            onRefresh()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .height(44.dp)
                                            .testTag("btn_ack_alert_${alert.id}")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Acknowledge", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // 4. DIALOGS & OVERLAYS
    // ==========================================

    // Snooze Presets Dialog
    alertToSnooze?.let { alert ->
        AlertDialog(
            onDismissRequest = { alertToSnooze = null },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text("Snooze Alert", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DexterOnBackground)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Defer “${alert.title.take(40)}...” from active cognitive load:",
                        fontSize = 12.sp,
                        color = Color(0xFFA0A5BD)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val presets = listOf(
                        "⚡ 30 Minutes" to 30L,
                        "🥪 1 Hour" to 60L,
                        "🧠 2 Hours" to 120L,
                        "🌆 This Evening (4 Hours)" to 240L,
                        "🌅 Tomorrow Morning" to 720L
                    )

                    presets.forEach { (label, minutes) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E2332),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repository.snoozeAttention(alert.id, minutes)
                                    alertToSnooze = null
                                    onRefresh()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DexterOnBackground)
                                Text("+${minutes}m", fontSize = 11.sp, color = DexterPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { alertToSnooze = null }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // Add Alert Dialog
    if (showAddAlertDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newDetail by remember { mutableStateOf("") }
        var selectedUrgency by remember { mutableStateOf("High") }
        var selectedCategory by remember { mutableStateOf("SYSTEM") }

        AlertDialog(
            onDismissRequest = { showAddAlertDialog = false },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(22.dp),
            title = { Text("Create Attention Alert", color = DexterOnBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ALERT TITLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA0A5BD))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        placeholder = { Text("e.g. Server migration review due", fontSize = 13.sp, color = Color(0xFF6B7280)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_alert_title"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground
                        )
                    )

                    Text("DETAIL / CONTEXT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA0A5BD))
                    OutlinedTextField(
                        value = newDetail,
                        onValueChange = { newDetail = it },
                        placeholder = { Text("Optional context, implications, or deadline info...", fontSize = 13.sp, color = Color(0xFF6B7280)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground
                        )
                    )

                    Text("URGENCY TIER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA0A5BD))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Critical", "High", "Normal", "Info").forEach { tier ->
                            val isSelected = selectedUrgency.equals(tier, ignoreCase = true)
                            val tierColor = when (tier) {
                                "Critical" -> Color(0xFFFF1744)
                                "High" -> Color(0xFFFF9100)
                                "Normal" -> Color(0xFF4FC3F7)
                                else -> Color(0xFFA0A5BD)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) tierColor else Color(0xFF1E2330),
                                modifier = Modifier.clickable { selectedUrgency = tier }
                            ) {
                                Text(
                                    text = tier,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else Color(0xFFA0A5BD),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Text("CATEGORY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA0A5BD))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("DEADLINE", "FINANCE", "HEALTH", "SECURITY", "SYSTEM").forEach { cat ->
                            val isSelected = selectedCategory == cat
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) DexterPrimary else Color(0xFF1E2330),
                                modifier = Modifier.clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else Color(0xFFA0A5BD),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            repository.createAttentionEvent(
                                title = newTitle,
                                urgency = selectedUrgency,
                                category = selectedCategory,
                                detail = newDetail
                            )
                            showAddAlertDialog = false
                            onRefresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                    enabled = newTitle.isNotBlank()
                ) {
                    Text("Save Alert", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAlertDialog = false }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
                }
            }
        )
    }

    // Discard Confirmation Dialog
    alertToDelete?.let { alert ->
        AlertDialog(
            onDismissRequest = { alertToDelete = null },
            containerColor = DexterSurface,
            title = { Text("Discard Attention Alert?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DexterOnBackground) },
            text = {
                Text(
                    text = "Are you sure you want to permanently discard this alert?\n\n“${alert.title}”",
                    fontSize = 13.sp,
                    color = Color(0xFFA0A5BD)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteAttention(alert.id)
                        alertToDelete = null
                        onRefresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                ) {
                    Text("Discard", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { alertToDelete = null }) {
                    Text("Keep", color = Color(0xFFA0A5BD))
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
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0: Morning Briefing, 1: End-of-Day Summary, 2: Archive
    val report = remember(trigger) { repository.getComprehensiveMorningBriefing() }
    val eodReport = remember(trigger) { repository.getComprehensiveEndOfDaySummary() }
    val briefings = remember(trigger) { repository.listBriefings() }

    var selectedBriefing by remember { mutableStateOf<BriefingItem?>(null) }
    var briefingToDelete by remember { mutableStateOf<BriefingItem?>(null) }
    var isSpeaking by remember { mutableStateOf(false) }
    var quickIntentionText by remember { mutableStateOf("") }
    var eveningReflectionInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Apple HIG 2026 3-Tab Segmented Control Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DexterSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab 0: Morning Briefing
                Button(
                    onClick = { activeTab = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == 0) DexterPrimary else Color.Transparent,
                        contentColor = if (activeTab == 0) Color.Black else Color(0xFFA0A5BD)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("tab_morning_briefing")
                ) {
                    Text(
                        text = "🌅 Morning",
                        fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }

                // Tab 1: End of Day Summary
                Button(
                    onClick = { activeTab = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == 1) DexterPrimary else Color.Transparent,
                        contentColor = if (activeTab == 1) Color.Black else Color(0xFFA0A5BD)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("tab_evening_summary")
                ) {
                    Text(
                        text = "🌙 Evening",
                        fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }

                // Tab 2: Historical Archive
                Button(
                    onClick = { activeTab = 2 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == 2) DexterPrimary else Color.Transparent,
                        contentColor = if (activeTab == 2) Color.Black else Color(0xFFA0A5BD)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("tab_briefing_archive")
                ) {
                    Text(
                        text = "📜 Archive (${briefings.size})",
                        fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (activeTab) {
            0 -> {
                // ==========================================
                // TAB 0: TODAY'S MORNING BRIEFING
                // ==========================================
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Date & Motivational Catalyst Header
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DexterSurface),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = report.dateString.uppercase(Locale.getDefault()),
                                            color = Color(0xFFF59E0B),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = report.weatherText,
                                        color = DexterPrimaryGlow,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Morning Executive Briefing",
                                    color = DexterOnBackground,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Dexter sovereign intelligence calibrated for today's sprints, timetable slots & active projects.",
                                    color = Color(0xFFA0A5BD),
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Motivational Quote Card
                                Surface(
                                    color = Color(0xFF1E2337),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("💡", fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "SOVEREIGN CATALYST",
                                                color = Color(0xFFF59E0B),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "\"${report.motivationalQuote}\"",
                                            color = DexterOnBackground,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "— ${report.motivationalAuthor}",
                                            color = Color(0xFFA0A5BD),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Action Pills Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            isSpeaking = !isSpeaking
                                            val msg = if (isSpeaking) "Narrating morning dispatch..." else "Narration paused."
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("btn_narrate_briefing"),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSpeaking) DexterPrimary else Color.White.copy(alpha = 0.15f)
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSpeaking) DexterPrimary.copy(alpha = 0.15f) else Color.Transparent
                                        )
                                    ) {
                                        Text(
                                            text = if (isSpeaking) "⏸️ Narration" else "🎙️ Listen",
                                            color = if (isSpeaking) DexterPrimary else DexterOnBackground,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val textToCopy = buildString {
                                                appendLine(report.title)
                                                appendLine(report.dateString)
                                                appendLine("Quote: \"${report.motivationalQuote}\" — ${report.motivationalAuthor}")
                                                appendLine()
                                                appendLine("--- EXECUTIVE SUMMARY ---")
                                                appendLine(report.executiveSummary)
                                                appendLine()
                                                appendLine("--- TODAY'S AGENDA ---")
                                                report.todayScheduleBlocks.forEach {
                                                    appendLine("• ${it.startTime} - ${it.endTime}: ${it.title} [${it.category}]")
                                                }
                                                appendLine()
                                                appendLine("--- PROJECTS & GOALS ---")
                                                report.activeProjects.forEach {
                                                    appendLine("Project: ${it.name} (${it.progressPercent}% completed, ${100 - it.progressPercent}% pending)")
                                                }
                                                report.activeGoals.forEach {
                                                    appendLine("Goal: ${it.title} (${it.progressPercent}% on track)")
                                                }
                                            }
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                            clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Dexter Morning Briefing", textToCopy))
                                            Toast.makeText(context, "Briefing copied to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("btn_copy_briefing"),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                                    ) {
                                        Text(
                                            text = "📋 Copy",
                                            color = DexterOnBackground,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            repository.synthesizeMorningBriefingFromIntake()
                                            onRefresh()
                                            Toast.makeText(context, "Synthesized fresh briefing from latest telemetry", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("btn_resynthesize_briefing"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary)
                                    ) {
                                        Text(
                                            text = "⚡ Refresh",
                                            color = Color.Black,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = isSpeaking) {
                                    Column(modifier = Modifier.padding(top = 12.dp)) {
                                        Surface(
                                            color = DexterPrimary.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, DexterPrimary.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("🔊", fontSize = 16.sp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Narrating: \"${report.executiveSummary.take(45)}...\"",
                                                        color = DexterPrimary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                Text("1.0x", color = Color(0xFFA0A5BD), fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Executive Dispatch Hero Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_executive_dispatch"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B2E)),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⚡", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "EXECUTIVE DISPATCH",
                                            color = Color(0xFFF59E0B),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Surface(
                                        color = Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Deterministic",
                                            color = Color(0xFFA0A5BD),
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = report.executiveSummary,
                                    color = DexterOnBackground,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Normal
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            repository.synthesizeMorningBriefingFromIntake()
                                            onRefresh()
                                            Toast.makeText(context, "Saved dispatch snapshot to archive", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("btn_save_dispatch_snapshot")
                                    ) {
                                        Text("📥 Snapshot to Archive", color = DexterPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Projects & Goals Progress Overview (New from user brief!)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🎯 PROJECTS & GOALS RADAR",
                                    color = DexterPrimaryGlow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Active Projects Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Active Projects (${report.activeProjects.size})",
                                            color = DexterOnBackground,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text("Segment 2", color = Color(0xFFA0A5BD), fontSize = 11.sp)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (report.activeProjects.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            report.activeProjects.forEach { proj ->
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(proj.name, color = DexterOnBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                        Text(
                                                            text = "${proj.progressPercent}% complete · ${100 - proj.progressPercent}% pending",
                                                            color = DexterPrimary,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    LinearProgressIndicator(
                                                        progress = { (proj.progressPercent / 100f).coerceIn(0f, 1f) },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(6.dp)
                                                            .clip(RoundedCornerShape(3.dp)),
                                                        color = DexterPrimary,
                                                        trackColor = Color.White.copy(alpha = 0.08f)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Text("No active projects. Add projects in Segment 2 to track progress.", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                                    }
                                }
                            }

                            // Active Goals Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Active Goals (${report.activeGoals.size})",
                                            color = DexterOnBackground,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text("Segment 3", color = Color(0xFFA0A5BD), fontSize = 11.sp)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (report.activeGoals.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            report.activeGoals.forEach { goal ->
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(goal.title, color = DexterOnBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                        Text(
                                                            text = "${goal.progressPercent}% on track (${goal.progressLabel})",
                                                            color = Color(0xFF10B981),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    LinearProgressIndicator(
                                                        progress = { (goal.progressPercent / 100f).coerceIn(0f, 1f) },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(6.dp)
                                                            .clip(RoundedCornerShape(3.dp)),
                                                        color = Color(0xFF10B981),
                                                        trackColor = Color.White.copy(alpha = 0.08f)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Text("No active goals. Add sovereign milestones in Segment 3.", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Important Timetable Slots & Focus
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🗓️ IMPORTANT TIMETABLE SLOTS",
                                    color = DexterPrimaryGlow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("card_today_schedule_blocks"),
                                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Scheduled Time Slots (${report.todayScheduleBlocks.size})",
                                            color = DexterOnBackground,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text("Segment 7", color = Color(0xFFA0A5BD), fontSize = 11.sp)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (report.todayScheduleBlocks.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            report.todayScheduleBlocks.forEachIndexed { index, block ->
                                                Surface(
                                                    color = if (index == 0) DexterPrimary.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (index == 0) DexterPrimary.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f)
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                    text = "${block.startTime} – ${block.endTime}",
                                                                    color = if (index == 0) DexterPrimary else Color(0xFFA0A5BD),
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    fontSize = 12.sp
                                                                )
                                                                if (index == 0) {
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    Surface(
                                                                        color = DexterPrimary,
                                                                        shape = RoundedCornerShape(4.dp)
                                                                    ) {
                                                                        Text(
                                                                            text = "NEXT UP",
                                                                            color = Color.Black,
                                                                            fontWeight = FontWeight.Bold,
                                                                            fontSize = 9.sp,
                                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = block.title,
                                                                color = DexterOnBackground,
                                                                fontWeight = FontWeight.Medium,
                                                                fontSize = 14.sp
                                                            )
                                                        }
                                                        Surface(
                                                            color = Color.White.copy(alpha = 0.08f),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                text = block.category,
                                                                color = Color(0xFFA0A5BD),
                                                                fontSize = 11.sp,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "No scheduled blocks configured for today. Head to Segment 7 (Schedule & Timetable) to allocate your slots.",
                                            color = Color(0xFFA0A5BD),
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }

                            // Priority Sprint Board with 1-tap completion
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("card_priority_tasks_board"),
                                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Today's Priority Tasks (${report.todayPriorityTasks.size})",
                                            color = DexterOnBackground,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${report.yesterdayPendingCount} Pending Total",
                                            color = DexterPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (report.todayPriorityTasks.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            report.todayPriorityTasks.forEach { task ->
                                                Surface(
                                                    color = Color.White.copy(alpha = 0.03f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            IconButton(
                                                                onClick = {
                                                                    repository.completeTask(task.id)
                                                                    onRefresh()
                                                                    Toast.makeText(context, "Completed: ${task.title}", Toast.LENGTH_SHORT).show()
                                                                },
                                                                modifier = Modifier
                                                                    .size(36.dp)
                                                                    .testTag("btn_complete_briefing_task_${task.id}")
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = "Complete Task",
                                                                    tint = DexterPrimary
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Column {
                                                                Text(
                                                                    text = task.title,
                                                                    color = DexterOnBackground,
                                                                    fontWeight = FontWeight.Medium,
                                                                    fontSize = 13.sp
                                                                )
                                                                Text(
                                                                    text = "Priority: ${task.priority.name} · Category: ${task.category}",
                                                                    color = Color(0xFFA0A5BD),
                                                                    fontSize = 11.sp
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "All priority sprint tasks cleared! High performance.",
                                            color = Color(0xFFA0A5BD),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Previous Day's Summarized Inputs
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⏮️ PREVIOUS DAY'S INPUTS & TELEMETRY",
                                    color = DexterPrimaryGlow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // 4-Metric Grid (2x2)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("metric_tasks_closed"),
                                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("✅ Tasks Closed", color = Color(0xFFA0A5BD), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "${report.yesterdayCompletedCount}",
                                            color = DexterOnBackground,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${report.yesterdayPendingCount} active remaining",
                                            color = Color(0xFFA0A5BD),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("metric_spent_outflow"),
                                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("💸 Capital Outflow", color = Color(0xFFA0A5BD), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = report.formattedSpent,
                                            color = if (report.yesterdaySpentMinor > 0) Color(0xFFF59E0B) else DexterOnBackground,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Logged in ledger",
                                            color = Color(0xFFA0A5BD),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("metric_sleep_recovery"),
                                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("🛌 Sleep Recovery", color = Color(0xFFA0A5BD), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = report.sleepSummary,
                                            color = DexterOnBackground,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Circadian restoration",
                                            color = Color(0xFFA0A5BD),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("metric_hydration_intake"),
                                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("💧 Hydration & Intake", color = Color(0xFFA0A5BD), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "${report.yesterdayWaterMl} mL",
                                            color = DexterOnBackground,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${report.yesterdayIntakeCount} intake notes captured",
                                            color = Color(0xFFA0A5BD),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Yesterday's Evening Reflection Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("card_evening_reflection"),
                                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("🌙", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Evening Reflection (Segment 12)",
                                                color = DexterOnBackground,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Text("Intake Hub", color = Color(0xFFA0A5BD), fontSize = 11.sp)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (report.yesterdayEveningReflection.isNotBlank()) {
                                        Text(
                                            text = "\"${report.yesterdayEveningReflection}\"",
                                            color = DexterOnBackground,
                                            fontSize = 13.sp,
                                            lineHeight = 19.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else {
                                        Text(
                                            text = "No dedicated evening reflection logged yesterday. ${report.yesterdayIntakeCount} raw intake notes are safely indexed in the Sovereign Hub.",
                                            color = Color(0xFFA0A5BD),
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 6. Morning Intention Rapid Ingest
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_morning_intention_ingest"),
                            colors = CardDefaults.cardColors(containerColor = DexterSurface),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🎯", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Set Today's Core Intention",
                                        color = DexterOnBackground,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Instantly records to Segment 12 Data Intake Hub under the Morning Window.",
                                    color = Color(0xFFA0A5BD),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = quickIntentionText,
                                    onValueChange = { quickIntentionText = it },
                                    placeholder = { Text("e.g. Finish core architecture & execute deep work slots...", color = Color(0xFF6B7280)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_quick_morning_intention"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DexterPrimary,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                        focusedTextColor = DexterOnBackground,
                                        unfocusedTextColor = DexterOnBackground
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    maxLines = 3
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            if (quickIntentionText.isNotBlank()) {
                                                repository.createCaptureNote(
                                                    noteText = quickIntentionText.trim(),
                                                    sourceType = "SCHEDULED_MORNING",
                                                    categoryTag = "Intention",
                                                    scheduledWindow = "Morning Intake"
                                                )
                                                quickIntentionText = ""
                                                onRefresh()
                                                Toast.makeText(context, "Morning intention ingested into Segment 12", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = quickIntentionText.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .height(44.dp)
                                            .testTag("btn_submit_morning_intention")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Submit",
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ingest to Hub", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // ==========================================
                // TAB 1: END-OF-DAY SUMMARY (New from user brief!)
                // ==========================================
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Nocturnal Header Bar
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DexterSurface),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color(0xFF60A5FA).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = eodReport.dateString.uppercase(Locale.getDefault()),
                                            color = Color(0xFF60A5FA),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = "24°C · Nocturnal Calm",
                                        color = Color(0xFFA0A5BD),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "End-of-Day Executive Summary",
                                    color = DexterOnBackground,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Reviewing completed sprints, project and goal progress, and circadian well-being.",
                                    color = Color(0xFFA0A5BD),
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            isSpeaking = !isSpeaking
                                            val msg = if (isSpeaking) "Narrating evening review..." else "Narration paused."
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("btn_narrate_evening_summary"),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSpeaking) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.15f)
                                        )
                                    ) {
                                        Text(
                                            text = if (isSpeaking) "⏸️ Narration" else "🎙️ Listen",
                                            color = if (isSpeaking) Color(0xFF60A5FA) else DexterOnBackground,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val textToCopy = buildString {
                                                appendLine(eodReport.title)
                                                appendLine(eodReport.dateString)
                                                appendLine()
                                                appendLine("--- EXECUTIVE REVIEW ---")
                                                appendLine(eodReport.executiveReflection)
                                                appendLine()
                                                appendLine("--- DELIVERABLES ---")
                                                appendLine("Tasks Closed: ${eodReport.completedTasksCount} (${eodReport.completionPercentage}% completion rate)")
                                                appendLine("Tasks Pending: ${eodReport.pendingTasksCount}")
                                                appendLine()
                                                appendLine("--- WELL-BEING & RECOVERY ---")
                                                appendLine("Hydration: ${eodReport.waterMl} mL")
                                                appendLine("Sleep Architecture: ${eodReport.sleepSummary}")
                                                appendLine("Mood: ${eodReport.mood?.mood ?: "Calm"}")
                                                appendLine("Outflow: ${eodReport.formattedSpent}")
                                                appendLine()
                                                appendLine("--- PROJECTS & GOALS ---")
                                                eodReport.activeProjects.forEach {
                                                    appendLine("• ${it.name}: ${it.progressPercent}% completed, ${100 - it.progressPercent}% pending")
                                                }
                                                eodReport.activeGoals.forEach {
                                                    appendLine("• ${it.title}: ${it.progressPercent}% on track (${100 - it.progressPercent}% pending)")
                                                }
                                            }
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                            clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Dexter Evening Summary", textToCopy))
                                            Toast.makeText(context, "Evening summary copied to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("btn_copy_evening_summary"),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                                    ) {
                                        Text("📋 Copy", color = DexterOnBackground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = {
                                            repository.synthesizeEndOfDaySummaryFromIntake()
                                            onRefresh()
                                            Toast.makeText(context, "Saved evening summary snapshot to archive", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("btn_save_evening_snapshot"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                                    ) {
                                        Text("📥 Archive", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Evening Executive Reflection Hero Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_evening_executive_reflection"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🌙", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "EXECUTIVE CLOSEOUT",
                                            color = Color(0xFF60A5FA),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Surface(
                                        color = Color(0xFF60A5FA).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${eodReport.completionPercentage}% Rate",
                                            color = Color(0xFF60A5FA),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = eodReport.executiveReflection,
                                    color = DexterOnBackground,
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp,
                                    fontWeight = FontWeight.Normal
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Surface(
                                    color = Color.White.copy(alpha = 0.04f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "\"${eodReport.motivationalClosing}\"",
                                        color = Color(0xFFA0A5BD),
                                        fontSize = 12.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. What We Did Today & Deliverables
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🏆 WHAT WE DID TODAY & DELIVERABLES",
                                    color = Color(0xFF60A5FA),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // 4-Metric Grid (2x2)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("✅ Tasks Closed", color = Color(0xFFA0A5BD), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "${eodReport.completedTasksCount}",
                                            color = Color(0xFF10B981),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${eodReport.pendingTasksCount} tasks pending",
                                            color = Color(0xFFA0A5BD),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("📈 Completion Rate", color = Color(0xFFA0A5BD), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "${eodReport.completionPercentage}%",
                                            color = Color(0xFF60A5FA),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Sprint efficiency",
                                            color = Color(0xFFA0A5BD),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Completed Tasks List
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Tasks Closed Today (${eodReport.tasksCompletedToday.size})",
                                        color = DexterOnBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (eodReport.tasksCompletedToday.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            eodReport.tasksCompletedToday.forEach { task ->
                                                Surface(
                                                    color = Color(0xFF10B981).copy(alpha = 0.06f),
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("✓", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = task.title,
                                                                color = DexterOnBackground,
                                                                fontWeight = FontWeight.Medium,
                                                                fontSize = 13.sp
                                                            )
                                                            Text(
                                                                text = "Category: ${task.category}",
                                                                color = Color(0xFFA0A5BD),
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Text("Zero tasks checked off today. Review pending sprint items.", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Pending Percentage for Goals & Projects (Explicitly requested by user!)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📊 PENDING PERCENTAGE: GOALS & PROJECTS",
                                    color = Color(0xFF60A5FA),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Projects Pending Percentage Breakdown
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Projects Remaining & Pending Status",
                                        color = DexterOnBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (eodReport.activeProjects.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            eodReport.activeProjects.forEach { proj ->
                                                val pendingPercent = 100 - proj.progressPercent
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(proj.name, color = DexterOnBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                        Text(
                                                            text = "${pendingPercent}% Pending (${proj.progressPercent}% Done)",
                                                            color = if (pendingPercent > 50) Color(0xFFF59E0B) else Color(0xFF10B981),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    LinearProgressIndicator(
                                                        progress = { (proj.progressPercent / 100f).coerceIn(0f, 1f) },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(6.dp)
                                                            .clip(RoundedCornerShape(3.dp)),
                                                        color = if (pendingPercent > 50) Color(0xFFF59E0B) else Color(0xFF10B981),
                                                        trackColor = Color.White.copy(alpha = 0.08f)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Text("No active projects currently tracked.", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                                    }
                                }
                            }

                            // Goals Pending Percentage Breakdown
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Goals Remaining & Pending Status",
                                        color = DexterOnBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (eodReport.activeGoals.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            eodReport.activeGoals.forEach { goal ->
                                                val pendingPercent = 100 - goal.progressPercent
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(goal.title, color = DexterOnBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                        Text(
                                                            text = "${pendingPercent}% Pending (${goal.progressPercent}% Achieved)",
                                                            color = if (pendingPercent > 50) Color(0xFFF59E0B) else Color(0xFF10B981),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    LinearProgressIndicator(
                                                        progress = { (goal.progressPercent / 100f).coerceIn(0f, 1f) },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(6.dp)
                                                            .clip(RoundedCornerShape(3.dp)),
                                                        color = if (pendingPercent > 50) Color(0xFFF59E0B) else Color(0xFF10B981),
                                                        trackColor = Color.White.copy(alpha = 0.08f)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Text("No active goals currently logged.", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 5. Summary in Well-Being
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🌱 WELL-BEING & CIRCADIAN SUMMARY",
                                    color = Color(0xFF60A5FA),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("💧 Hydration Target", color = Color(0xFFA0A5BD), fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("${eodReport.waterMl} mL", color = DexterOnBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("🛌 Last Night's Sleep", color = Color(0xFFA0A5BD), fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(eodReport.sleepSummary, color = DexterOnBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("🧘 Mind & Energy State", color = Color(0xFFA0A5BD), fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${eodReport.mood?.mood ?: "Calm"} · Energy: ${eodReport.mood?.energyLevel ?: "Balanced"}",
                                                color = DexterOnBackground,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("🔥 Habits Maintained", color = Color(0xFFA0A5BD), fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("${eodReport.habitsMaintainedCount} Streaks", color = DexterPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. Evening Reflection & Intake Ingest
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_evening_reflection_ingest"),
                            colors = CardDefaults.cardColors(containerColor = DexterSurface),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📝", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Record Tonight's Evening Reflection",
                                        color = DexterOnBackground,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Feeds into tomorrow morning's briefing and saves directly to Segment 12 (Evening Intake Window).",
                                    color = Color(0xFFA0A5BD),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = eveningReflectionInput,
                                    onValueChange = { eveningReflectionInput = it },
                                    placeholder = { Text("What did you accomplish today? Any learnings, wins, or gratitudes?", color = Color(0xFF6B7280)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_evening_reflection_text"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF60A5FA),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                        focusedTextColor = DexterOnBackground,
                                        unfocusedTextColor = DexterOnBackground
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    maxLines = 4
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            if (eveningReflectionInput.isNotBlank()) {
                                                repository.createCaptureNote(
                                                    noteText = eveningReflectionInput.trim(),
                                                    sourceType = "SCHEDULED_EVENING",
                                                    categoryTag = "Reflection",
                                                    scheduledWindow = "Evening Reflection"
                                                )
                                                eveningReflectionInput = ""
                                                onRefresh()
                                                Toast.makeText(context, "Evening reflection captured to Segment 12", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = eveningReflectionInput.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .height(44.dp)
                                            .testTag("btn_submit_evening_reflection")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Submit",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save Reflection", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // ==========================================
                // TAB 2: HISTORICAL BRIEFING ARCHIVE
                // ==========================================
                Column(modifier = Modifier.fillMaxSize()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DexterSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Briefing Runs Archive",
                                        color = DexterOnBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${briefings.size} snapshots recorded in SQLite",
                                        color = Color(0xFFA0A5BD),
                                        fontSize = 11.sp
                                    )
                                }

                                Button(
                                    onClick = {
                                        repository.generateBriefingNow()
                                        onRefresh()
                                        Toast.makeText(context, "New briefing run generated", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(40.dp)
                                        .testTag("btn_generate_briefing_now")
                                ) {
                                    Text("Generate Now", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (briefings.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(briefings, key = { it.id }) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedBriefing = item }
                                        .testTag("briefing_card_${item.id}"),
                                    colors = CardDefaults.cardColors(containerColor = DexterSurface),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
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
                                                fontSize = 14.sp
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = item.weatherText,
                                                    color = DexterPrimaryGlow,
                                                    fontSize = 11.sp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                IconButton(
                                                    onClick = { briefingToDelete = item },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Briefing",
                                                        tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = item.summaryText,
                                            color = Color(0xFFA0A5BD),
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            maxLines = 3
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Tasks: ${item.tasksCount} pending · Spent: ${item.formattedSpent}",
                                                color = DexterPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = item.dateString,
                                                color = Color(0xFFA0A5BD),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DexterSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📜", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No Historical Briefings",
                                    color = DexterOnBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap 'Generate Now' or 'Snapshot to Archive' to persist daily executive runs.",
                                    color = Color(0xFFA0A5BD),
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Briefing Reader Dialog
    selectedBriefing?.let { b ->
        AlertDialog(
            onDismissRequest = { selectedBriefing = null },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = b.title,
                    color = DexterOnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(b.summaryText, color = DexterOnBackground, fontSize = 14.sp, lineHeight = 21.sp)
                    HorizontalDivider(color = DexterSurfaceVariant)
                    Text("Date: ${b.dateString}", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                    Text("Weather telemetry: ${b.weatherText}", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                    Text("Pending tasks at generation: ${b.tasksCount}", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                    Text("Financial spent at generation: ${b.formattedSpent}", color = Color(0xFFA0A5BD), fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedBriefing = null },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Deletion Confirmation Dialog (Apple HIG Guardrail)
    briefingToDelete?.let { b ->
        AlertDialog(
            onDismissRequest = { briefingToDelete = null },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Delete Briefing Run?",
                    color = DexterOnBackground,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${b.title}'? This action cannot be undone.",
                    color = Color(0xFFA0A5BD),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteBriefing(b.id)
                        briefingToDelete = null
                        onRefresh()
                        Toast.makeText(context, "Briefing run deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { briefingToDelete = null }) {
                    Text("Cancel", color = Color(0xFFA0A5BD))
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
