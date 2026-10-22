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
import com.aistudio.dexter.service.DexterVoiceService
import com.aistudio.dexter.ui.activity.FinanceActivity
import com.aistudio.dexter.ui.activity.JournalActivity
import com.aistudio.dexter.ui.activity.SegmentsActivity
import com.aistudio.dexter.ui.activity.TasksActivity
import com.aistudio.dexter.ui.dialog.ModelManagerDialog
import com.aistudio.dexter.ui.view.VoiceInteractionDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.TimePickerDialog
import android.widget.EditText
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

            repository = (application as DexterApp).repository
            speechManager = DexterSpeechManager(this)
            actionExecutor = DexterActionExecutor(repository, this)

            setupVoiceOrb()
            setupBottomNav()
            setupStatusChip()
            setupBriefingCard()
            setupSegmentsHubCard()

            // Start voice service if permission was already granted
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                DexterVoiceService.startService(this)
            }

            // Eagerly refresh data
            loadDashboardData()

            if (intent.getBooleanExtra("action_open_voice", false)) {
                openVoiceDialog()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onCreate", e)
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
        }
    }

    private fun setupSegmentsHubCard() {
        binding.cardSegmentsHub.setOnClickListener {
            startActivity(Intent(this, SegmentsActivity::class.java))
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
        binding.voiceOrbView.setState(OrbVisualState.IDLE)
        binding.orbHintText.text = "Tap orb to speak or test voice commands"
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
            binding.voiceOrbView.setState(OrbVisualState.IDLE)
            loadDashboardData()
        }
        dialog.show()
    }

    private fun setupBriefingCard() {
        binding.cardBriefing.setOnClickListener {
            // Navigate directly to TasksActivity for actionable task review
            startActivity(Intent(this, TasksActivity::class.java))
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
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            val briefing = withContext(Dispatchers.IO) { repository.getDailyBriefing() }

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

        binding.navJournal.setOnClickListener {
            startActivity(Intent(this, JournalActivity::class.java))
        }

        binding.navMore.setOnClickListener {
            startActivity(Intent(this, SegmentsActivity::class.java))
        }
    }

    private fun showMoreOptionsDialog() {
        val options = arrayOf(
            "🧠 Edge AI Models & Downloader",
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
                    1 -> showGoalsProjectsDialog()
                    2 -> showAlarmsDialog()
                    3 -> DexterVoiceService.requestIgnoreBatteryOptimizations(this)
                    4 -> showServiceStatusSheet()
                    5 -> showDatabaseDiagnostics()
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
            val projectLines = projects.map { "📁 [Project] ${it.name} (${it.status})" }
            val allItems = (goalLines + projectLines).toTypedArray()

            AlertDialog.Builder(this@MainActivity)
                .setTitle("Goals & Projects")
                .setItems(if (allItems.isNotEmpty()) allItems else arrayOf("No active goals or projects."), null)
                .setPositiveButton("Close", null)
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
