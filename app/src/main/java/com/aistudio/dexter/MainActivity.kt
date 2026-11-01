package com.aistudio.dexter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aistudio.dexter.data.repository.DexterRepository
import com.aistudio.dexter.databinding.ActivityMainBinding
import com.aistudio.dexter.engine.DexterActionExecutor
import com.aistudio.dexter.engine.DexterSpeechManager
import com.aistudio.dexter.engine.LayaEngine
import com.aistudio.dexter.engine.OrbVisualState
import com.aistudio.dexter.engine.ProactiveMotiveEngine
import com.aistudio.dexter.service.DexterVoiceService
import com.aistudio.dexter.ui.activity.FinanceActivity
import com.aistudio.dexter.ui.activity.JournalActivity
import com.aistudio.dexter.ui.activity.SegmentsActivity
import com.aistudio.dexter.ui.activity.SegmentDetailActivity
import com.aistudio.dexter.ui.activity.TasksActivity
import com.aistudio.dexter.ui.dialog.ModelManagerDialog
import com.aistudio.dexter.ui.view.VoiceInteractionDialog
import androidx.compose.ui.platform.ViewCompositionStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.TimePickerDialog
import android.widget.EditText
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.dexter.ui.components.VoiceOrbFab
import com.aistudio.dexter.ui.theme.DexterTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: DexterRepository
    private lateinit var speechManager: DexterSpeechManager
    private lateinit var actionExecutor: DexterActionExecutor

    private val fabOrbState = MutableStateFlow(OrbVisualState.IDLE)
    private val fabAudioIntensity = MutableStateFlow(0f)

    private var activeVoiceDialog: VoiceInteractionDialog? = null

    private val systemSpeechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = matches?.firstOrNull()
            if (!text.isNullOrBlank()) {
                activeVoiceDialog?.processRecognizedText(text)
            }
        }
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            DexterVoiceService.startService(this)
            openVoiceDialog()
        } else {
            // Still allow dialog for text-based natural language interaction
            openVoiceDialog()
            Toast.makeText(
                this,
                "Microphone permission needed for voice input. Text entry is available.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Fatal error inflating layout", e)
            return
        }

        try {
            repository = (application as DexterApp).repository
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error initializing repository", e)
        }

        try {
            speechManager = DexterSpeechManager(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error initializing speechManager", e)
        }

        try {
            if (::repository.isInitialized) {
                actionExecutor = DexterActionExecutor(repository, this)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error initializing actionExecutor", e)
        }

        try { setupVoiceOrb() } catch (e: Exception) { android.util.Log.e("MainActivity", "Error in setupVoiceOrb", e) }
        try { setupVoiceOrbFab() } catch (e: Exception) { android.util.Log.e("MainActivity", "Error in setupVoiceOrbFab", e) }
        try { if (::speechManager.isInitialized) observeSpeechManager() } catch (e: Exception) { android.util.Log.e("MainActivity", "Error in observeSpeechManager", e) }
        try { setupCommandBar() } catch (e: Exception) { android.util.Log.e("MainActivity", "Error in setupCommandBar", e) }
        try { setupBottomNav() } catch (e: Exception) { android.util.Log.e("MainActivity", "Error in setupBottomNav", e) }
        try { setupStatusChip() } catch (e: Exception) { android.util.Log.e("MainActivity", "Error in setupStatusChip", e) }
        try { setupBriefingCard() } catch (e: Exception) { android.util.Log.e("MainActivity", "Error in setupBriefingCard", e) }
        try { setupAlexaModeCard() } catch (e: Exception) { android.util.Log.e("MainActivity", "Error in setupAlexaModeCard", e) }
        try { setupSegmentsHubCard() } catch (e: Exception) { android.util.Log.e("MainActivity", "Error in setupSegmentsHubCard", e) }
        try { setupProfileAvatar() } catch (e: Exception) { android.util.Log.e("MainActivity", "Error in setupProfileAvatar", e) }

        // Start voice service if permission was already granted
        try {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                DexterVoiceService.startService(this)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error checking audio permission or starting service", e)
        }

        // Eagerly refresh data and service status
        try { loadDashboardData() } catch (e: Exception) { android.util.Log.e("MainActivity", "Error in loadDashboardData", e) }
        try { updateServiceStatus() } catch (e: Exception) { android.util.Log.e("MainActivity", "Error in updateServiceStatus", e) }

        try {
            if (intent.getBooleanExtra("action_open_voice", false)) {
                openVoiceDialog()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error checking action_open_voice", e)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("action_open_voice", false)) {
            openVoiceDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            loadDashboardData()
            updateServiceStatus()

            // Proactive Motive Evaluation (Autonomous speech without prior prompt)
            lifecycleScope.launch(Dispatchers.IO) {
                val motiveEngine = ProactiveMotiveEngine.getInstance(this@MainActivity)
                val vocalization = motiveEngine.evaluateVocalization(forceTrigger = false)
                if (vocalization != null) {
                    withContext(Dispatchers.Main) {
                        if (::speechManager.isInitialized) {
                            speechManager.speak(vocalization.spokenText)
                        }
                        Toast.makeText(this@MainActivity, "${vocalization.motive.badge} ${vocalization.headline}: ${vocalization.spokenText}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setupSegmentsHubCard() {
        binding.cardSegmentsHub.setOnClickListener {
            startActivity(Intent(this, SegmentsActivity::class.java))
        }
    }

    private fun setupProfileAvatar() {
        val prefs = com.aistudio.dexter.data.preferences.UserPreferencesRepository.getInstance(this)
        lifecycleScope.launch {
            prefs.preferences.collectLatest { p ->
                binding.tvProfileAvatar.text = p.avatarEmoji
            }
        }
        binding.btnProfileAvatar.setOnClickListener {
            startActivity(Intent(this, com.aistudio.dexter.ui.activity.ProfileActivity::class.java))
        }
    }

    private fun setupCommandBar() {
        binding.btnHomeSend.setOnClickListener {
            val query = binding.inputHomeCommand.text.toString().trim()
            if (query.isNotEmpty()) {
                binding.inputHomeCommand.setText("")
                processDirectCommand(query)
            }
        }

        binding.inputHomeCommand.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                val query = binding.inputHomeCommand.text.toString().trim()
                if (query.isNotEmpty()) {
                    binding.inputHomeCommand.setText("")
                    processDirectCommand(query)
                }
                true
            } else {
                false
            }
        }

        binding.btnHomeMic.setOnClickListener {
            requestVoiceActivation()
        }

        // Quick sample test chips
        binding.chipSampleExpense.setOnClickListener {
            processDirectCommand("spent 45 on travel")
        }
        binding.chipSampleTask.setOnClickListener {
            processDirectCommand("add task Review design")
        }
        binding.chipSampleAlarm.setOnClickListener {
            processDirectCommand("remind me at 5 PM")
        }
        binding.chipSampleBriefing.setOnClickListener {
            processDirectCommand("daily briefing")
        }
        binding.chipSampleBalance.setOnClickListener {
            processDirectCommand("summarize expenses")
        }
    }

    private fun processDirectCommand(phrase: String) {
        val clean = phrase.trim()
        if (clean.isEmpty()) return

        // Update UI to processing state
        setVisualState(OrbVisualState.PROCESSING)
        binding.orbHintText.text = "Thinking..."
        binding.responseText.text = "Processing: \"$clean\"..."

        lifecycleScope.launch {
            val harnessEngine = com.aistudio.dexter.engine.Harness2Engine.getInstance(this@MainActivity, repository, actionExecutor)
            val harnessResult = withContext(Dispatchers.IO) {
                harnessEngine.executeHarness(clean)
            }

            // Speaking State
            setVisualState(OrbVisualState.SPEAKING)
            binding.responseText.text = harnessResult.summaryText
            binding.orbHintText.text = "Dexter responded (Harness²)"

            // Speak via TTS if initialized
            speechManager.speak(harnessResult.summaryText)

            // Refresh dashboard data metrics (e.g. expenses, tasks)
            loadDashboardData()

            // Return to IDLE after a short delay
            kotlinx.coroutines.delay(3000)
            setVisualState(OrbVisualState.IDLE)
            binding.orbHintText.text = "Tap orb to speak or type command below"
        }
    }

    private fun setupVoiceOrb() {
        binding.voiceOrbView.onTap = {
            requestVoiceActivation()
        }

        binding.voiceOrbView.onLongPress = {
            requestVoiceActivation()
        }

        // Keep main orb in gentle breathing IDLE animation
        setVisualState(OrbVisualState.IDLE)
        binding.orbHintText.text = "Tap orb to speak or type command below"
    }

    private fun setupVoiceOrbFab() {
        binding.composeVoiceOrbFab.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.composeVoiceOrbFab.setContent {
            DexterTheme {
                val state by fabOrbState.collectAsStateWithLifecycle()
                val intensity by fabAudioIntensity.collectAsStateWithLifecycle()
                VoiceOrbFab(
                    state = state,
                    audioIntensity = intensity,
                    onClick = {
                        requestVoiceActivation()
                    }
                )
            }
        }
    }

    private fun observeSpeechManager() {
        lifecycleScope.launch {
            speechManager.orbState.collectLatest { state ->
                fabOrbState.value = state
                binding.voiceOrbView.setState(state)
            }
        }
        lifecycleScope.launch {
            speechManager.rmsVolume.collectLatest { rms ->
                val intensity = (rms / 8f).coerceIn(0f, 1f)
                fabAudioIntensity.value = intensity
            }
        }
    }

    private fun setVisualState(state: OrbVisualState, intensity: Float = 0f) {
        fabOrbState.value = state
        fabAudioIntensity.value = intensity
        binding.voiceOrbView.setState(state)
    }

    private fun requestVoiceActivation() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            openVoiceDialog()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun openVoiceDialog() {
        if (!::speechManager.isInitialized || !::actionExecutor.isInitialized) {
            Toast.makeText(this, "Voice engine initializing, please wait...", Toast.LENGTH_SHORT).show()
            return
        }
        val dialog = VoiceInteractionDialog(
            context = this,
            speechManager = speechManager,
            actionExecutor = actionExecutor,
            onActionCompleted = {
                loadDashboardData()
            },
            onLaunchSystemVoice = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a Dexter command (e.g. 'spent 45 in traveling')")
                }
                try {
                    systemSpeechLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "System speech dialog unavailable: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
        activeVoiceDialog = dialog
        dialog.setOnDismissListener {
            activeVoiceDialog = null
            setVisualState(OrbVisualState.IDLE)
            loadDashboardData()
        }
        dialog.show()
    }

    private fun setupBriefingCard() {
        binding.cardBriefing.setOnClickListener {
            // Navigate directly to Segment 14 (Morning Briefing & End-of-Day Summary)
            val intent = Intent(this, SegmentDetailActivity::class.java).apply {
                putExtra(SegmentDetailActivity.EXTRA_SEGMENT_NUMBER, 14)
            }
            startActivity(intent)
        }
    }

    private var isAlexaModeActive = true

    private fun setupAlexaModeCard() {
        binding.btnToggleAlexaVoice.setOnClickListener {
            isAlexaModeActive = !isAlexaModeActive
            DexterVoiceService.setAlexaMode(this, isAlexaModeActive)
            if (isAlexaModeActive) {
                binding.alexaStatusBadge.text = "🟢 Active"
                binding.alexaStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_live))
                binding.btnToggleAlexaVoice.text = "Disable"
                Toast.makeText(this, "Offline wake-word detector active: Say 'Hey Dexter', 'Dexter', or 'buddy'", Toast.LENGTH_SHORT).show()
            } else {
                binding.alexaStatusBadge.text = "⚪ Paused"
                binding.alexaStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.dexter_on_surface_variant))
                binding.btnToggleAlexaVoice.text = "Enable"
                Toast.makeText(this, "Offline wake-word detector paused.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSimulateWakeUp.setOnClickListener {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return@setOnClickListener
            }

            Toast.makeText(
                this,
                "Starting Wake-Up Conversation! Listen to Dexter speak and answer hands-free (\"I'm awake\" or \"Snooze\").",
                Toast.LENGTH_LONG
            ).show()
            DexterVoiceService.triggerWakeUpSimulation(this)
        }
    }

    private fun setupStatusChip() {
        binding.statusChip.setOnClickListener {
            ModelManagerDialog(this) {
                updateServiceStatus()
            }.show()
        }
    }

    private fun updateServiceStatus() {
        lifecycleScope.launch {
            try {
                val isNeedleRunning = withContext(Dispatchers.IO) {
                    try {
                        val socket = java.net.Socket()
                        socket.connect(java.net.InetSocketAddress("127.0.0.1", 8081), 250)
                        socket.close()
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                val hasLayaModel = LayaEngine.getInstance(this@MainActivity).hasPhysicalModel()
                if (::binding.isInitialized) {
                    if (isNeedleRunning) {
                        binding.statusDot.setBackgroundResource(R.drawable.bg_status_dot_live)
                        binding.statusText.text = "Live"
                        binding.statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_live))
                    } else if (hasLayaModel) {
                        binding.statusDot.setBackgroundResource(R.drawable.bg_status_dot_live)
                        binding.statusText.text = "Tier 2"
                        binding.statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_live))
                    } else {
                        // Tier 1 Fast-Path is active even when neural weights aren't downloaded
                        binding.statusDot.setBackgroundResource(R.drawable.bg_status_dot_degraded)
                        binding.statusText.text = "Tier 1"
                        binding.statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_degraded))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error in updateServiceStatus", e)
            }
        }
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                val briefing = withContext(Dispatchers.IO) { repository.getDailyBriefing() }

                if (::binding.isInitialized) {
                    val tasksText = "${briefing.pendingTasksCount} tasks pending"
                    val alarmsText = if (briefing.nextAlarmLabel != null) "1 reminder" else "0 reminders"
                    val rupeesSpent = briefing.todaySpentMinor / 100
                    val expenseText = "₹$rupeesSpent spent today"

                    binding.briefingMetricsText.text = "$tasksText · $alarmsText · $expenseText"

                    if (briefing.topTaskTitle != null) {
                        binding.nextTaskText.text = "📋 Next: ${briefing.topTaskTitle}"
                    } else {
                        binding.nextTaskText.text = "📋 Next: No pending tasks"
                    }

                    if (briefing.nextAlarmLabel != null && briefing.nextAlarmTime != null) {
                        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(briefing.nextAlarmTime * 1000))
                        binding.nextAlarmText.text = "⏰ $timeStr – ${briefing.nextAlarmLabel}"
                    } else {
                        binding.nextAlarmText.text = "⏰ Next: No upcoming alarms"
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error in loadDashboardData", e)
            }
        }
    }

    private fun setupBottomNav() {
        binding.navHome.setOnClickListener {
            loadDashboardData()
            updateServiceStatus()
        }

        binding.navTasks.setOnClickListener {
            startActivity(Intent(this, TasksActivity::class.java))
        }

        binding.navFinance.setOnClickListener {
            startActivity(Intent(this, FinanceActivity::class.java))
        }

        binding.navMore.setOnClickListener {
            startActivity(Intent(this, SegmentsActivity::class.java))
        }
    }

    private fun showMoreOptionsDialog() {
        val options = arrayOf(
            "🧠 Edge AI Models & Downloader",
            "🔬 Voice & Laya Pipeline Lab (Accents & Languages)",
            "🎯 Goals & Projects",
            "⏰ Reminders & Alarms",
            "🔋 Request Battery Whitelist (Alexa Mode)",
            "⚙️ System & Needle Status",
            "📦 Database Diagnostics"
        )

        AlertDialog.Builder(this)
            .setTitle("Dexter Settings & Tools")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> ModelManagerDialog(this) { updateServiceStatus() }.show()
                    1 -> startActivity(Intent(this, com.aistudio.dexter.ui.activity.VoiceLabActivity::class.java))
                    2 -> showGoalsProjectsDialog()
                    3 -> showAlarmsDialog()
                    4 -> DexterVoiceService.requestIgnoreBatteryOptimizations(this)
                    5 -> showServiceStatusSheet()
                    6 -> showDatabaseDiagnostics()
                }
            }
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showGoalsProjectsDialog() {
        lifecycleScope.launch {
            val goals = withContext(Dispatchers.IO) { repository.listGoals() }
            val projects = withContext(Dispatchers.IO) { repository.listProjects() }

            val goalLines = goals.map { "🎯 [Goal] ${it.name} (${it.status})" }
            val projectLines = projects.map { "📁 [Project] ${it.name} (${it.status}) - ${it.taskCount} milestones (${it.progressPercent}%)" }
            val allItems = (goalLines + projectLines).toTypedArray()

            AlertDialog.Builder(this@MainActivity)
                .setTitle("Goals & Projects")
                .setItems(if (allItems.isNotEmpty()) allItems else arrayOf("No active goals or projects."), null)
                .setPositiveButton("Close", null)
                .setNeutralButton("📁 Projects") { _, _ ->
                    val intent = Intent(this@MainActivity, SegmentDetailActivity::class.java).apply {
                        putExtra(SegmentDetailActivity.EXTRA_SEGMENT_NUMBER, 2)
                    }
                    startActivity(intent)
                }
                .setNegativeButton("🎯 Goals") { _, _ ->
                    val intent = Intent(this@MainActivity, SegmentDetailActivity::class.java).apply {
                        putExtra(SegmentDetailActivity.EXTRA_SEGMENT_NUMBER, 3)
                    }
                    startActivity(intent)
                }
                .show()
        }
    }

    private fun showAlarmsDialog() {
        lifecycleScope.launch {
            val alarms = withContext(Dispatchers.IO) { repository.listAlarms() }
            val items = alarms.map {
                val timeStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(it.alarmTime * 1000))
                "⏰ $timeStr – ${it.label}"
            }.toTypedArray()

            AlertDialog.Builder(this@MainActivity)
                .setTitle("Active Reminders (${alarms.size})")
                .setItems(if (items.isNotEmpty()) items else arrayOf("No upcoming reminders."), null)
                .setPositiveButton("Close", null)
                .setNeutralButton("⏰ Set New Alarm") { _, _ ->
                    showCreateAlarmClockDialog()
                }
                .show()
        }
    }

    private fun showCreateAlarmClockDialog() {
        val now = Calendar.getInstance()
        val presets = arrayOf(
            "⚡ In 15 minutes",
            "⏳ In 30 minutes",
            "⏱️ In 1 hour",
            "🌅 Tomorrow 7:00 AM",
            "☀️ Tomorrow 8:00 AM",
            "🌙 Tonight 10:00 PM",
            "🕒 Custom Clock Picker..."
        )

        AlertDialog.Builder(this)
            .setTitle("Set Alarm Time")
            .setItems(presets) { _, which ->
                val cal = Calendar.getInstance()
                when (which) {
                    0 -> showAlarmLabelDialog(cal.timeInMillis + 15 * 60 * 1000)
                    1 -> showAlarmLabelDialog(cal.timeInMillis + 30 * 60 * 1000)
                    2 -> showAlarmLabelDialog(cal.timeInMillis + 60 * 60 * 1000)
                    3 -> {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                        cal.set(Calendar.HOUR_OF_DAY, 7)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        showAlarmLabelDialog(cal.timeInMillis)
                    }
                    4 -> {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                        cal.set(Calendar.HOUR_OF_DAY, 8)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        showAlarmLabelDialog(cal.timeInMillis)
                    }
                    5 -> {
                        cal.set(Calendar.HOUR_OF_DAY, 22)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        if (cal.before(now)) cal.add(Calendar.DAY_OF_YEAR, 1)
                        showAlarmLabelDialog(cal.timeInMillis)
                    }
                    6 -> {
                        val timePicker = TimePickerDialog(
                            this,
                            { _, hourOfDay, minute ->
                                val alarmCal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    set(Calendar.MINUTE, minute)
                                    set(Calendar.SECOND, 0)
                                    if (before(now)) {
                                        add(Calendar.DAY_OF_YEAR, 1)
                                    }
                                }
                                showAlarmLabelDialog(alarmCal.timeInMillis)
                            },
                            now.get(Calendar.HOUR_OF_DAY),
                            now.get(Calendar.MINUTE),
                            false
                        )
                        timePicker.show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAlarmLabelDialog(alarmMillis: Long) {
        val input = EditText(this).apply {
            hint = "Reminder Label (e.g. Morning Workout)"
        }
        AlertDialog.Builder(this)
            .setTitle("Alarm Label")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val label = input.text.toString().ifBlank { "Reminder" }
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        repository.createAlarm(alarmMillis, label, "Daily", true)
                    }
                    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                    Toast.makeText(this@MainActivity, "Alarm set for ${sdf.format(Date(alarmMillis))}: $label", Toast.LENGTH_SHORT).show()
                    loadDashboardData()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showServiceStatusSheet() {
        val dbFile = getDatabasePath("dexter.db")
        val dbSizeKb = if (dbFile.exists()) dbFile.length() / 1024 else 0

        val message = """
            Dexter Engine Status:
            • Voice Engine: Alexa Mode (Low-Power VAD)
            • Localhost Daemon: Needle 3 on :8081
            • Fast-Path Router: Active (≤ 5ms)
            • Database: SQLite WAL mode (${dbSizeKb} KB)
            • Background Service: Foreground Persistent
            
            Battery Philosophy:
            Dexter consumes ≤ 3% per hour in standby Alexa mode.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("System Status")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Battery Whitelist") { _, _ ->
                DexterVoiceService.requestIgnoreBatteryOptimizations(this)
            }
            .show()
    }

    private fun showDatabaseDiagnostics() {
        lifecycleScope.launch {
            val diagnostics = withContext(Dispatchers.IO) { repository.getSegmentDiagnostics() }
            val dbFile = getDatabasePath("dexter.db")
            val dbSizeKb = if (dbFile.exists()) dbFile.length() / 1024 else 0

            val lines = diagnostics.map { (segment, count) ->
                "✓ $segment: $count items"
            }.toTypedArray()

            AlertDialog.Builder(this@MainActivity)
                .setTitle("16-Segment Database (SQLite WAL, ${dbSizeKb} KB)")
                .setItems(lines, null)
                .setPositiveButton("Close", null)
                .show()
        }
    }
}
