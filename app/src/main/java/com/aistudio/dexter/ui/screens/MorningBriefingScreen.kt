package com.aistudio.dexter.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.dexter.data.model.GoalItem
import com.aistudio.dexter.data.model.MorningBriefingReport
import com.aistudio.dexter.data.model.ProjectItem
import com.aistudio.dexter.data.model.ScheduleBlockItem
import com.aistudio.dexter.data.model.TaskItem
import com.aistudio.dexter.data.repository.DexterRepository
import com.aistudio.dexter.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * MorningBriefingScreen: Apple HIG 2026 Sovereign Card-Based Morning Executive Briefing.
 * Fetches and displays:
 * 1. Today's tasks & pending backlog with 1-tap instant completion.
 * 2. Active goals and projects progress radar with completion bars.
 * 3. Scheduled timetable slots with "NEXT UP" highlight.
 * 4. Previous day's summarized inputs (closed tasks, spend, sleep/hydration, reflection).
 * 5. Motivational catalyst quote and audio narration trigger.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorningBriefingScreen(
    repository: DexterRepository,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onNavigateToTasks: (() -> Unit)? = null,
    onNavigateToSchedule: (() -> Unit)? = null,
    onNavigateToProjects: (() -> Unit)? = null,
    onNavigateToGoals: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var refreshTrigger by remember { mutableStateOf(0L) }
    var isLoading by remember { mutableStateOf(false) }

    var report by remember { mutableStateOf<MorningBriefingReport?>(null) }
    var isSpeaking by remember { mutableStateOf(false) }
    var intentionText by remember { mutableStateOf("") }
    var isSavingIntention by remember { mutableStateOf(false) }

    // Fetch morning briefing data on trigger update
    LaunchedEffect(refreshTrigger) {
        isLoading = true
        val fetched = withContext(Dispatchers.IO) {
            repository.getComprehensiveMorningBriefing()
        }
        report = fetched
        isLoading = false
    }

    Scaffold(
        modifier = modifier.testTag("morning_briefing_screen"),
        containerColor = DexterBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌅", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Morning Briefing",
                                color = DexterOnBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = report?.dateString ?: "Today's Executive Intelligence",
                                color = Color(0xFFA0A5BD),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("btn_morning_briefing_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = DexterOnBackground
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    repository.synthesizeMorningBriefingFromIntake()
                                }
                                refreshTrigger++
                                Toast.makeText(context, "Synthesized fresh briefing from latest telemetry", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("btn_morning_refresh")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Briefing",
                            tint = DexterPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DexterBackground,
                    titleContentColor = DexterOnBackground
                )
            )
        }
    ) { innerPadding ->
        val currentReport = report
        if (currentReport == null && isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = DexterPrimary,
                    modifier = Modifier.testTag("loading_indicator")
                )
            }
        } else if (currentReport != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Catalyst & Weather Hero Card
                item {
                    MorningCatalystCard(
                        report = currentReport,
                        isSpeaking = isSpeaking,
                        onToggleNarration = {
                            isSpeaking = !isSpeaking
                            val msg = if (isSpeaking) "Narrating morning dispatch..." else "Narration paused."
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        onCopy = {
                            copyBriefingToClipboard(context, currentReport)
                        },
                        onRefresh = {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    repository.synthesizeMorningBriefingFromIntake()
                                }
                                refreshTrigger++
                                Toast.makeText(context, "Synthesized fresh briefing", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // 2. Executive Dispatch Card
                item {
                    ExecutiveDispatchCard(
                        summary = currentReport.executiveSummary,
                        onSaveSnapshot = {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    repository.synthesizeMorningBriefingFromIntake()
                                }
                                refreshTrigger++
                                Toast.makeText(context, "Saved dispatch snapshot to archive", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // 3. Priority Tasks Sprint Board Card
                item {
                    PriorityTasksCard(
                        tasks = currentReport.todayPriorityTasks,
                        yesterdayPendingCount = currentReport.yesterdayPendingCount,
                        onCompleteTask = { taskId, taskTitle ->
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    repository.completeTask(taskId)
                                }
                                refreshTrigger++
                                Toast.makeText(context, "Completed: $taskTitle", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenAllTasks = onNavigateToTasks
                    )
                }

                // 4. Goals & Projects Progress Radar Card
                item {
                    ProjectsAndGoalsCard(
                        projects = currentReport.activeProjects,
                        goals = currentReport.activeGoals,
                        onNavigateToProjects = onNavigateToProjects,
                        onNavigateToGoals = onNavigateToGoals
                    )
                }

                // 5. Scheduled Timetable Slots Card
                item {
                    ScheduledSlotsCard(
                        scheduleBlocks = currentReport.todayScheduleBlocks,
                        onNavigateToSchedule = onNavigateToSchedule
                    )
                }

                // 6. Previous Day's Telemetry & Inputs Card
                item {
                    PreviousDayTelemetryCard(
                        report = currentReport
                    )
                }

                // 7. Today's Anchor Intention Card
                item {
                    AnchorIntentionCard(
                        intentionText = intentionText,
                        onIntentionChange = { intentionText = it },
                        isSaving = isSavingIntention,
                        onSave = {
                            val clean = intentionText.trim()
                            if (clean.isNotEmpty()) {
                                isSavingIntention = true
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        repository.logJournal(
                                            content = "Logged from Morning Briefing. Primary anchor for the day's focus sprint.",
                                            title = "🌅 Morning Intention: $clean",
                                            mood = "Focused",
                                            category = "Intentions"
                                        )
                                    }
                                    isSavingIntention = false
                                    intentionText = ""
                                    refreshTrigger++
                                    Toast.makeText(context, "Intention recorded in Sovereign Journal", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * 1. Catalyst & Weather Hero Card
 */
@Composable
private fun MorningCatalystCard(
    report: MorningBriefingReport,
    isSpeaking: Boolean,
    onToggleNarration: () -> Unit,
    onCopy: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_morning_catalyst"),
        colors = CardDefaults.cardColors(containerColor = DexterSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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

            // Motivational Quote Sub-Card
            Surface(
                color = Color(0xFF1E2337),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
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

            // Action Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleNarration,
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
                    onClick = onCopy,
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
                    onClick = onRefresh,
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

/**
 * 2. Executive Dispatch Card
 */
@Composable
private fun ExecutiveDispatchCard(
    summary: String,
    onSaveSnapshot: () -> Unit
) {
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
                text = summary,
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
                    onClick = onSaveSnapshot,
                    modifier = Modifier.testTag("btn_save_dispatch_snapshot")
                ) {
                    Text("📥 Snapshot to Archive", color = DexterPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * 3. Priority Tasks Sprint Board Card
 */
@Composable
private fun PriorityTasksCard(
    tasks: List<TaskItem>,
    yesterdayPendingCount: Int,
    onCompleteTask: (Long, String) -> Unit,
    onOpenAllTasks: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_priority_tasks_board"),
        colors = CardDefaults.cardColors(containerColor = DexterSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Priority Tasks (${tasks.size})",
                        color = DexterOnBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "$yesterdayPendingCount pending across all lists",
                        color = Color(0xFFA0A5BD),
                        fontSize = 11.sp
                    )
                }
                if (onOpenAllTasks != null) {
                    TextButton(
                        onClick = onOpenAllTasks,
                        modifier = Modifier.testTag("btn_view_all_tasks")
                    ) {
                        Text("View All", color = DexterPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (tasks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tasks.forEach { task ->
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
                                        onClick = { onCompleteTask(task.id, task.title) },
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
                Surface(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✨ All priority sprint tasks are clear! Tap 'View All' to add new deliverables.",
                        color = Color(0xFFA0A5BD),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * 4. Goals & Projects Progress Radar Card
 */
@Composable
private fun ProjectsAndGoalsCard(
    projects: List<ProjectItem>,
    goals: List<GoalItem>,
    onNavigateToProjects: (() -> Unit)?,
    onNavigateToGoals: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_projects_goals"),
        colors = CardDefaults.cardColors(containerColor = DexterSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 Projects & Goals Radar",
                    color = DexterOnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (onNavigateToGoals != null) {
                    TextButton(onClick = onNavigateToGoals) {
                        Text("Milestones", color = DexterPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Projects Sub-Section
            Text(
                text = "ACTIVE PROJECTS (${projects.size})",
                color = DexterPrimaryGlow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (projects.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    projects.forEach { proj ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = proj.name,
                                    color = DexterOnBackground,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
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
                Text(
                    text = "No active projects. Projects in Segment 2 feed automatically into morning summaries.",
                    color = Color(0xFFA0A5BD),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Goals Sub-Section
            Text(
                text = "STRATEGIC GOALS (${goals.size})",
                color = Color(0xFF10B981),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Circular Progress Bar Data Visualization Component for User Goals
            GoalsCircularProgressVisualization(
                goals = goals,
                onNavigateToGoals = onNavigateToGoals
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (goals.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    goals.forEach { goal ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = goal.title,
                                    color = DexterOnBackground,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
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
                Text(
                    text = "No active milestones. Add quarterly or lifetime goals in Segment 3.",
                    color = Color(0xFFA0A5BD),
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * GoalsCircularProgressBar:
 * Apple HIG 2026 data visualization circular gauge.
 * Draws an animated sweep-gradient progress arc with rounded caps,
 * subtle outer glow, and a center metrics readout.
 */
@Composable
fun GoalsCircularProgressBar(
    progressPercent: Int,
    modifier: Modifier = Modifier,
    sizeDp: Int = 124,
    strokeWidthDp: Int = 11,
    primaryColor: Color = Color(0xFF10B981),
    centerLabel: String = "ON TRACK",
    subLabel: String? = null
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (progressPercent.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = 850),
        label = "GoalsCircularProgressAnim"
    )

    val gradientBrush = Brush.sweepGradient(
        listOf(
            primaryColor.copy(alpha = 0.6f),
            primaryColor,
            Color(0xFF34D399),
            primaryColor
        )
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(sizeDp.dp)
            .testTag("canvas_goals_progress_ring")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidthDp.dp.toPx()
            val padding = strokePx / 2f + 4.dp.toPx()
            val arcSize = Size(size.width - padding * 2, size.height - padding * 2)
            val topLeft = Offset(padding, padding)

            // Background Track
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Subtle outer accent glow ring
            drawArc(
                color = primaryColor.copy(alpha = 0.12f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx + 4.dp.toPx())
            )

            // Animated Foreground Arc
            if (animatedProgress > 0f) {
                drawArc(
                    brush = gradientBrush,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // Center Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = "${progressPercent.coerceIn(0, 100)}%",
                color = DexterOnBackground,
                fontSize = if (sizeDp >= 120) 24.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("text_goals_overall_percent")
            )
            Text(
                text = centerLabel,
                color = primaryColor,
                fontSize = if (sizeDp >= 120) 9.sp else 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            if (subLabel != null) {
                Text(
                    text = subLabel,
                    color = Color(0xFFA0A5BD),
                    fontSize = 8.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * GoalsCircularProgressVisualization:
 * Interactive component displaying aggregate and individual circular progress data visualization
 * for user-defined goals.
 */
@Composable
fun GoalsCircularProgressVisualization(
    goals: List<GoalItem>,
    modifier: Modifier = Modifier,
    onNavigateToGoals: (() -> Unit)? = null
) {
    var selectedGoalId by remember { mutableStateOf<Long?>(null) }
    val selectedGoal = goals.find { it.id == selectedGoalId }

    val aggregatePercent = if (goals.isNotEmpty()) {
        goals.map { it.progressPercent.coerceIn(0, 100) }.average().toInt()
    } else 0

    val displayPercent = selectedGoal?.progressPercent ?: aggregatePercent
    val activeCount = goals.count { it.status.equals("ACTIVE", ignoreCase = true) }
    val onTrackCount = goals.count { it.progressPercent >= 50 }

    val primaryRingColor = when {
        displayPercent >= 75 -> Color(0xFF10B981) // Emerald
        displayPercent >= 45 -> DexterPrimaryGlow // Gold/Amber
        else -> Color(0xFF38BDF8) // Cyan/Sky Blue
    }

    Surface(
        color = Color(0xFF121624),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, primaryRingColor.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_goals_circular_progress")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📊", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedGoal != null) "MILESTONE VELOCITY" else "STRATEGIC GOALS VELOCITY",
                        color = primaryRingColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Surface(
                    color = primaryRingColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, primaryRingColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = if (selectedGoal != null) "Selected: ${selectedGoal.title.take(12)}" else "${goals.size} Goals",
                        color = primaryRingColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Gauge + Metrics Split Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular Progress Bar
                GoalsCircularProgressBar(
                    progressPercent = displayPercent,
                    sizeDp = 124,
                    strokeWidthDp = 11,
                    primaryColor = primaryRingColor,
                    centerLabel = if (selectedGoal != null) "MILESTONE" else "AVERAGE",
                    subLabel = if (selectedGoal != null) selectedGoal.progressLabel else "$onTrackCount on track"
                )

                Spacer(modifier = Modifier.width(14.dp))

                // Status Telemetry Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.03f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pacing Ratio", color = Color(0xFFA0A5BD), fontSize = 11.sp)
                            Text(
                                text = if (goals.isEmpty()) "0%" else "$onTrackCount/${goals.size} Paced",
                                color = primaryRingColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.03f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Active Status", color = Color(0xFFA0A5BD), fontSize = 11.sp)
                            Text(
                                text = if (selectedGoal != null) selectedGoal.status else "$activeCount Active",
                                color = DexterOnBackground,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (selectedGoal != null) {
                        TextButton(
                            onClick = { selectedGoalId = null },
                            modifier = Modifier.align(Alignment.End),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("↺ Show Overall", color = primaryRingColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else if (onNavigateToGoals != null) {
                        TextButton(
                            onClick = onNavigateToGoals,
                            modifier = Modifier.align(Alignment.End),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Milestones Hub →", color = DexterPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Interactive Mini Goal Selector Chips
            if (goals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "TAP GOAL TO INSPECT PROGRESS RING",
                    color = Color(0xFFA0A5BD),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    goals.take(3).forEach { goal ->
                        val isSelected = goal.id == selectedGoalId
                        Surface(
                            onClick = {
                                selectedGoalId = if (isSelected) null else goal.id
                            },
                            color = if (isSelected) primaryRingColor.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) primaryRingColor else Color.White.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = goal.title,
                                    color = DexterOnBackground,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${goal.progressPercent}%",
                                        color = if (isSelected) primaryRingColor else Color(0xFFA0A5BD),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (goal.progressPercent >= 50) Color(0xFF10B981) else Color(0xFFF59E0B)
                                            )
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

/**
 * 5. Scheduled Timetable Slots Card
 */
@Composable
private fun ScheduledSlotsCard(
    scheduleBlocks: List<ScheduleBlockItem>,
    onNavigateToSchedule: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_today_schedule_blocks"),
        colors = CardDefaults.cardColors(containerColor = DexterSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🗓️ Timetable Slots (${scheduleBlocks.size})",
                    color = DexterOnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (onNavigateToSchedule != null) {
                    TextButton(onClick = onNavigateToSchedule) {
                        Text("Timetable", color = DexterPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (scheduleBlocks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    scheduleBlocks.forEachIndexed { index, block ->
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
                Surface(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No scheduled blocks configured for today. Head to Segment 7 (Schedule & Timetable) to allocate your slots.",
                        color = Color(0xFFA0A5BD),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * 6. Previous Day's Telemetry & Inputs Card
 */
@Composable
private fun PreviousDayTelemetryCard(
    report: MorningBriefingReport
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_previous_day_telemetry"),
        colors = CardDefaults.cardColors(containerColor = DexterSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "⏮️ Previous Day's Telemetry",
                color = DexterOnBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 4-Metric Grid (2x2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("✅ Tasks Closed", color = Color(0xFFA0A5BD), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${report.yesterdayCompletedCount}",
                            color = DexterOnBackground,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "of ${report.yesterdayCompletedCount + report.yesterdayPendingCount} items",
                            color = Color(0xFFA0A5BD),
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("💸 Spend (Minor)", color = Color(0xFFA0A5BD), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${report.yesterdaySpentMinor / 100}",
                            color = DexterOnBackground,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Offline ledger", color = Color(0xFFA0A5BD), fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("💧 Hydration", color = Color(0xFFA0A5BD), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${report.yesterdayWaterMl} ml",
                            color = Color(0xFF60A5FA),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Target: 2500 ml", color = Color(0xFFA0A5BD), fontSize = 10.sp)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🔥 Habits Kept", color = Color(0xFFA0A5BD), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${report.habitsMaintainedCount}",
                            color = DexterPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Sovereign streak", color = Color(0xFFA0A5BD), fontSize = 10.sp)
                    }
                }
            }

            if (report.yesterdayEveningReflection.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("🌙 Previous Evening Reflection", color = Color(0xFFA0A5BD), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${report.yesterdayEveningReflection}\"",
                            color = DexterOnBackground,
                            fontSize = 12.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

/**
 * 7. Today's Anchor Intention Card
 */
@Composable
private fun AnchorIntentionCard(
    intentionText: String,
    onIntentionChange: (String) -> Unit,
    isSaving: Boolean,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_anchor_intention"),
        colors = CardDefaults.cardColors(containerColor = DexterSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "🎯 Today's Anchor Intention",
                color = DexterOnBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "Anchor your focus sprint before daily distractions hit.",
                color = Color(0xFFA0A5BD),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = intentionText,
                onValueChange = onIntentionChange,
                placeholder = { Text("e.g., Finalize v2 database schema & complete 3 deep work sprints", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_anchor_intention"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DexterPrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onSave,
                enabled = intentionText.trim().isNotEmpty() && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("btn_save_anchor_intention"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DexterPrimary,
                    disabledContainerColor = DexterPrimary.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Commit Intention to Journal",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Utility function to copy the comprehensive briefing report to system clipboard.
 */
private fun copyBriefingToClipboard(context: Context, report: MorningBriefingReport) {
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
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Dexter Morning Briefing", textToCopy))
    Toast.makeText(context, "Morning Briefing copied to clipboard", Toast.LENGTH_SHORT).show()
}
