package com.aistudio.dexter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.dexter.engine.OrbVisualState
import com.aistudio.dexter.ui.components.VoiceInteractionModal
import com.aistudio.dexter.ui.screens.*
import com.aistudio.dexter.ui.theme.*
import com.aistudio.dexter.ui.viewmodel.DexterViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DexterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DexterTheme {
                DexterMainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DexterMainApp(viewModel: DexterViewModel) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val orbState by viewModel.speechManager.orbState.collectAsStateWithLifecycle()
    val isVoiceModalOpen by viewModel.isVoiceModalOpen.collectAsStateWithLifecycle()
    val transcript by viewModel.speechManager.partialTranscript.collectAsStateWithLifecycle()
    val lastResponse by viewModel.speechManager.lastSpokenResponse.collectAsStateWithLifecycle()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val pendingTasks by viewModel.pendingTasks.collectAsStateWithLifecycle()
    val completedTasks by viewModel.completedTasks.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val todaySpent by viewModel.todaySpentMinor.collectAsStateWithLifecycle()
    val monthSpent by viewModel.monthSpentMinor.collectAsStateWithLifecycle()
    val journalNotes by viewModel.journalNotes.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val briefing by viewModel.dailyBriefing.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Audio Permission Launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.openVoiceModal()
        } else {
            // Still open modal so user can use the zero-barrier fallback keyboard natural language input
            viewModel.openVoiceModal()
        }
    }

    val onRequestVoiceInput = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.openVoiceModal()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        containerColor = DexterBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DEXTER",
                            color = DexterOnBackground,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )

                        // Service Status Pill (Live / Offline)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DexterSurfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(DexterSuccess, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Live",
                                    color = DexterOnSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DexterBackground
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DexterSurface,
                contentColor = DexterOnSurface,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DexterPrimary,
                        selectedTextColor = DexterPrimary,
                        indicatorColor = DexterSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_home")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (pendingTasks.isNotEmpty()) {
                                    Badge(containerColor = DexterPrimary) {
                                        Text(pendingTasks.size.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Tasks")
                        }
                    },
                    label = { Text("Tasks", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DexterPrimary,
                        selectedTextColor = DexterPrimary,
                        indicatorColor = DexterSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_tasks")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Finance") },
                    label = { Text("Finance", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DexterPrimary,
                        selectedTextColor = DexterPrimary,
                        indicatorColor = DexterSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_finance")
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Journal") },
                    label = { Text("Journal", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DexterPrimary,
                        selectedTextColor = DexterPrimary,
                        indicatorColor = DexterSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_journal")
                )

                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.selectTab(4) },
                    icon = { Icon(Icons.Default.MoreVert, contentDescription = "More") },
                    label = { Text("More", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DexterPrimary,
                        selectedTextColor = DexterPrimary,
                        indicatorColor = DexterSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_more")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    orbState = orbState,
                    briefing = briefing,
                    onTapOrb = onRequestVoiceInput,
                    onPlayBriefing = { viewModel.playBriefing() },
                    onQuickPrompt = { prompt ->
                        viewModel.openVoiceModal()
                        viewModel.processUserPhrase(prompt)
                    }
                )

                1 -> TasksScreen(
                    pendingTasks = pendingTasks,
                    completedTasks = completedTasks,
                    onCompleteTask = { taskId -> viewModel.completeTask(taskId) },
                    onCancelTask = { taskId -> viewModel.cancelTask(taskId) },
                    onCreateTask = { title, priority -> viewModel.createTask(title, priority) }
                )

                2 -> FinanceScreen(
                    transactions = transactions,
                    todaySpentMinor = todaySpent,
                    monthSpentMinor = monthSpent,
                    onLogExpense = { amtMinor, desc, cat -> viewModel.logExpense(amtMinor, desc, cat) }
                )

                3 -> JournalScreen(
                    notes = journalNotes,
                    onAddNote = { note -> viewModel.addJournalNote(note) }
                )

                4 -> MoreScreen(
                    goals = goals,
                    projects = projects,
                    alarms = alarms,
                    auditLogs = auditLogs,
                    onCancelAlarms = { viewModel.cancelAllAlarms() }
                )
            }

            // Voice Interaction Overlay Modal
            VoiceInteractionModal(
                visible = isVoiceModalOpen,
                orbState = orbState,
                transcript = transcript,
                lastResponse = lastResponse,
                pendingConfirmation = pendingConfirmation,
                onDismiss = { viewModel.dismissVoiceModal() },
                onConfirmAction = { intent -> viewModel.confirmPendingAction(intent) },
                onCancelConfirmation = { viewModel.cancelPendingAction() },
                onSubmitText = { text -> viewModel.processUserPhrase(text) }
            )
        }
    }
}
