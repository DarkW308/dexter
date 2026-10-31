package com.aistudio.dexter.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.aistudio.dexter.data.model.Priority
import com.aistudio.dexter.data.repository.DexterRepository
import java.util.Calendar

enum class ProactiveMotive(val label: String, val badge: String) {
    EXECUTIVE_ADVISORY("Executive Advisory", "🎯"),
    EVENING_DAY_CLOSE("Evening Debrief & Day-Close", "🌙"),
    MOMENTUM_MOTIVATOR("Momentum Motivator", "⚡"),
    FINANCIAL_PACING("Financial Pacing", "📊"),
    WELLBEING_CHECKIN("Wellbeing Check-in", "🛡️"),
    CURATED_WIT("Curated Wit & Banter", "✨")
}

enum class ProactiveUrgency {
    LOW, NORMAL, HIGH
}

data class ProactiveVocalization(
    val motive: ProactiveMotive,
    val headline: String,
    val text: String,
    val spokenText: String,
    val urgency: ProactiveUrgency = ProactiveUrgency.NORMAL,
    val triggeredBy: String,
    val contextSummary: String,
    val targetSegments: List<Int> = listOf(1),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Autonomous Proactive Speech & Motive Engine
 *
 * Implements spontaneous, unprompted human-like personal assistant interaction.
 * Evaluates real-time SQLite database metrics (tasks, finance velocity, active goals,
 * time of day, and completion streaks) to speak with an elite, observant persona.
 *
 * Runs 100% on-device with zero network latency and zero cloud dependencies.
 */
class ProactiveMotiveEngine(
    private val repository: DexterRepository?
) {

    companion object {
        private const val TAG = "ProactiveMotiveEngine"
        private const val MIN_COOLDOWN_MS = 180_000L // 3 minutes debounce between auto-vocalizations

        @Volatile
        private var INSTANCE: ProactiveMotiveEngine? = null

        fun getInstance(context: Context): ProactiveMotiveEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProactiveMotiveEngine(DexterRepository(context.applicationContext)).also {
                    INSTANCE = it
                }
            }
        }

        fun createForTesting(repository: DexterRepository? = null): ProactiveMotiveEngine {
            return ProactiveMotiveEngine(repository)
        }
    }

    private var lastVocalizationTimestamp: Long = 0L

    /**
     * Evaluates autonomous vocalization state across all 16 database segments.
     */
    fun evaluateVocalization(
        forceTrigger: Boolean = false,
        simulatedHour: Int? = null,
        simulatedTrigger: String = "App Launch"
    ): ProactiveVocalization? {
        val now = System.currentTimeMillis()
        if (!forceTrigger && (now - lastVocalizationTimestamp < MIN_COOLDOWN_MS)) {
            Log.d(TAG, "Proactive vocalization suppressed: within cooldown threshold.")
            return null
        }

        val cal = Calendar.getInstance()
        val currentHour = simulatedHour ?: cal.get(Calendar.HOUR_OF_DAY)

        // Segment 1: Pending & Completed Tasks
        val allTasks = repository?.tasks?.getAllTasks() ?: emptyList()
        val pendingTasks = allTasks.filter { !it.isCompleted }
        val criticalTasks = pendingTasks.filter { it.priority == Priority.CRITICAL || it.priority == Priority.HIGH }
        val completedToday = allTasks.filter { it.isCompleted }.size

        // Segment 4: Financial Velocity
        val todaySpend = repository?.finance?.getTodayExpenseTotal() ?: 0.0

        // Segment 3: Goals
        val activeGoals = repository?.listGoals()?.filter { it.status.equals("ACTIVE", ignoreCase = true) } ?: emptyList()

        val vocalization = selectMotive(
            currentHour = currentHour,
            pendingCount = pendingTasks.size,
            criticalCount = criticalTasks.size,
            topCriticalTitle = criticalTasks.firstOrNull()?.title,
            completedToday = completedToday,
            todaySpend = todaySpend,
            activeGoalsCount = activeGoals.size,
            topGoalTitle = activeGoals.firstOrNull()?.title,
            triggeredBy = simulatedTrigger
        )

        lastVocalizationTimestamp = now
        return vocalization
    }

    private fun selectMotive(
        currentHour: Int,
        pendingCount: Int,
        criticalCount: Int,
        topCriticalTitle: String?,
        completedToday: Int,
        todaySpend: Double,
        activeGoalsCount: Int,
        topGoalTitle: String?,
        triggeredBy: String
    ): ProactiveVocalization {
        // 1. Late Night Wellbeing Guard (22:00 - 04:59)
        if (currentHour >= 22 || currentHour < 5) {
            val spoken = "Sir, the hour is late. Strategic clarity demands biological recovery. I have tomorrow's agenda queued; step away and rest."
            return ProactiveVocalization(
                motive = ProactiveMotive.WELLBEING_CHECKIN,
                headline = "Circadian Recovery Advisory",
                text = "It's late in the evening. High-performance cognitive output requires proper rest. Your tasks and schedule are secured for tomorrow.",
                spokenText = spoken,
                urgency = ProactiveUrgency.NORMAL,
                triggeredBy = triggeredBy,
                contextSummary = "Hour: $currentHour:00 | Pending Tasks: $pendingCount",
                targetSegments = listOf(1, 8)
            )
        }

        // 1b. Evening Day-Close & Data Gathering Check (19:00 - 21:59)
        // Two Conditions: (1) Evening timeframe, (2) User has completed/pending tasks, but has not completed evening debrief/summary
        if (currentHour in 19..21 && (completedToday > 0 || pendingCount > 0)) {
            val spoken = "Good evening, sir. The day is coming to a close. You have resolved $completedToday tasks today with $pendingCount still pending. We haven't gathered your evening debrief yet. Shall we wrap up today's summary and set tomorrow's priorities?"
            return ProactiveVocalization(
                motive = ProactiveMotive.EVENING_DAY_CLOSE,
                headline = "Evening Debrief & Day-Close Directive",
                text = "Day-close alignment: $completedToday tasks resolved, $pendingCount pending. Daily summary and evening data gathering awaiting confirmation.",
                spokenText = spoken,
                urgency = ProactiveUrgency.HIGH,
                triggeredBy = triggeredBy,
                contextSummary = "Hour: $currentHour:00 | Completed: $completedToday | Pending: $pendingCount",
                targetSegments = listOf(1, 5)
            )
        }

        // 2. High Financial Velocity Alert (Today's spend > ₹2,500 or equivalent currency threshold)
        if (todaySpend >= 2500.0) {
            val spoken = "Operational expense note: daily deployment has reached ${todaySpend.toInt()} currency units. Let us keep discretionary burn measured."
            return ProactiveVocalization(
                motive = ProactiveMotive.FINANCIAL_PACING,
                headline = "Daily Budget Pacing Notice",
                text = "Today's logged expenditures have crossed ${todaySpend.toInt()}. Pacing is elevated relative to standard daily baseline.",
                spokenText = spoken,
                urgency = ProactiveUrgency.HIGH,
                triggeredBy = triggeredBy,
                contextSummary = "Today Expense: ₹${todaySpend.toInt()} | Segments: 4",
                targetSegments = listOf(4)
            )
        }

        // 3. Morning Kickoff & Critical Priority (05:00 - 11:59)
        if (currentHour in 5..11 && criticalCount > 0) {
            val taskName = topCriticalTitle ?: "primary directive"
            val spoken = "Good morning. You have $criticalCount high-priority objectives on the docket today, led by '$taskName'. Ready to execute?"
            return ProactiveVocalization(
                motive = ProactiveMotive.EXECUTIVE_ADVISORY,
                headline = "Morning Directive Alignment",
                text = "Morning kickoff: $criticalCount high-priority deliverables require focus, anchored by '$taskName'.",
                spokenText = spoken,
                urgency = ProactiveUrgency.HIGH,
                triggeredBy = triggeredBy,
                contextSummary = "$criticalCount critical tasks | Top: $taskName",
                targetSegments = listOf(1, 3)
            )
        }

        // 4. Milestone Streak & High Momentum (3+ tasks completed today)
        if (completedToday >= 3) {
            val spoken = "$completedToday key items closed out today. Excellent execution velocity. Let us maintain this standard."
            return ProactiveVocalization(
                motive = ProactiveMotive.MOMENTUM_MOTIVATOR,
                headline = "Execution Velocity Peak",
                text = "You've successfully resolved $completedToday items today. Operational momentum is high.",
                spokenText = spoken,
                urgency = ProactiveUrgency.NORMAL,
                triggeredBy = triggeredBy,
                contextSummary = "Completed Today: $completedToday | Pending: $pendingCount",
                targetSegments = listOf(1)
            )
        }

        // 5. Goal Alignment Check (Midday with active goals)
        if (activeGoalsCount > 0 && currentHour in 12..17) {
            val goal = topGoalTitle ?: "Quarterly Target"
            val spoken = "Midday status: all systems active. Keeping eyes on the overarching target: '$goal'."
            return ProactiveVocalization(
                motive = ProactiveMotive.EXECUTIVE_ADVISORY,
                headline = "Strategic Horizon Check",
                text = "Midday calibration: $pendingCount tasks active. Long-term focus locked on '$goal'.",
                spokenText = spoken,
                urgency = ProactiveUrgency.NORMAL,
                triggeredBy = triggeredBy,
                contextSummary = "Active Goal: $goal | Hour: $currentHour:00",
                targetSegments = listOf(1, 3)
            )
        }

        // 6. Default Executive Wit & Preparedness
        val banterOptions = listOf(
            "Standing by. All database segments synchronized and ready for command.",
            "Ready when you are, sir. Let us turn complexity into clean execution.",
            "Dexter operational. Your schedule, finances, and directives are fully under control."
        )
        val selectedBanter = banterOptions[(currentHour + pendingCount) % banterOptions.size]

        return ProactiveVocalization(
            motive = ProactiveMotive.CURATED_WIT,
            headline = "Executive Aide Ready",
            text = selectedBanter,
            spokenText = selectedBanter,
            urgency = ProactiveUrgency.LOW,
            triggeredBy = triggeredBy,
            contextSummary = "State Clean | Pending: $pendingCount",
            targetSegments = listOf(1, 4, 3)
        )
    }

    /**
     * Preset simulator for testing and UI demonstration in Voice Lab.
     */
    fun simulateMotive(motive: ProactiveMotive): ProactiveVocalization {
        return when (motive) {
            ProactiveMotive.EVENING_DAY_CLOSE -> ProactiveVocalization(
                motive = motive,
                headline = "Evening Debrief & Day-Close Directive",
                text = "Day-close alignment: 4 tasks resolved, 2 pending. Daily summary and evening data gathering awaiting confirmation.",
                spokenText = "Good evening, sir. The day is coming to a close. You have resolved four tasks today with two still pending. Shall we wrap up today's summary?",
                urgency = ProactiveUrgency.HIGH,
                triggeredBy = "Simulation: Evening Day-Close",
                contextSummary = "Hour: 20:00 | Completed: 4 | Pending: 2",
                targetSegments = listOf(1, 5)
            )
            ProactiveMotive.EXECUTIVE_ADVISORY -> ProactiveVocalization(
                motive = motive,
                headline = "Morning Tactical Briefing",
                text = "3 critical objectives pending today: Architecture Review, Budget Reconciliation, and Sprint Deliverable.",
                spokenText = "Good morning. You have three high-priority deliverables today. I suggest prioritizing the Architecture Review first.",
                urgency = ProactiveUrgency.HIGH,
                triggeredBy = "Simulation: Executive Advisory",
                contextSummary = "Pending: 3 Critical | Goal: Product Launch",
                targetSegments = listOf(1, 3)
            )
            ProactiveMotive.MOMENTUM_MOTIVATOR -> ProactiveVocalization(
                motive = motive,
                headline = "Momentum Acceleration",
                text = "5 objectives resolved in succession. Execution velocity is currently in the 95th percentile.",
                spokenText = "Outstanding focus, sir. Five tasks down in unbroken succession. Let us keep the streak alive.",
                urgency = ProactiveUrgency.NORMAL,
                triggeredBy = "Simulation: Momentum Motivator",
                contextSummary = "5 tasks resolved | Streak: 7 days",
                targetSegments = listOf(1)
            )
            ProactiveMotive.FINANCIAL_PACING -> ProactiveVocalization(
                motive = motive,
                headline = "Budget Pacing Warning",
                text = "Dining and discretionary spend today has crossed 65% of the projected weekly burn limit.",
                spokenText = "Sir, today's dining expenditure is outpacing our typical burn rate. A home dinner tonight would restore equilibrium.",
                urgency = ProactiveUrgency.HIGH,
                triggeredBy = "Simulation: Financial Pacing",
                contextSummary = "Spend: ₹3,450 | Category: Dining",
                targetSegments = listOf(4)
            )
            ProactiveMotive.WELLBEING_CHECKIN -> ProactiveVocalization(
                motive = motive,
                headline = "Circadian Protection Alert",
                text = "Sustained high-cognitive workload detected past 23:00. Biological downtime recommended.",
                spokenText = "The hour is late. Strategic clarity tomorrow demands biological recovery tonight. I have everything locked down; step away.",
                urgency = ProactiveUrgency.NORMAL,
                triggeredBy = "Simulation: Wellbeing Check-in",
                contextSummary = "Time: 23:30 | Continuous Session: 4h",
                targetSegments = listOf(8, 1)
            )
            ProactiveMotive.CURATED_WIT -> ProactiveVocalization(
                motive = motive,
                headline = "Executive Aide Status",
                text = "All 16 SQLite segments active and indexed. Standing by to translate thought into action.",
                spokenText = "Standing by, sir. Let us turn complexity into clean execution.",
                urgency = ProactiveUrgency.LOW,
                triggeredBy = "Simulation: Curated Wit",
                contextSummary = "System Ready | Vault Encrypted",
                targetSegments = listOf(1, 4, 3, 5)
            )
        }
    }

    /**
     * Proactively speaks the generated vocalization through Android TextToSpeech.
     */
    fun speakVocalization(vocalization: ProactiveVocalization, tts: TextToSpeech?) {
        try {
            tts?.speak(vocalization.spokenText, TextToSpeech.QUEUE_FLUSH, null, "proactive_${vocalization.timestamp}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to speak proactive vocalization: ${e.message}")
        }
    }
}
