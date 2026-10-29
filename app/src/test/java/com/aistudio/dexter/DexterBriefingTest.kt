package com.aistudio.dexter

import com.aistudio.dexter.data.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DexterBriefingTest {

    @Test
    fun testMorningBriefingReport_metricsAndCalculations() {
        val sleep = SleepItem(
            id = 1L,
            bedtime = "11:00 PM",
            waketime = "06:40 AM",
            durationText = "7h 40m",
            qualityRating = 5,
            qualityLabel = "Restful",
            notes = "Deep continuous rest"
        )

        val project = ProjectItem(
            id = 1L,
            name = "Dexter Sovereign OS",
            progressPercent = 75,
            status = "ACTIVE"
        )

        val goal = GoalItem(
            id = 1L,
            title = "Annual Savings Fund",
            targetAmount = 200000.0,
            currentAmount = 140000.0,
            progressPercent = 70,
            status = "ACTIVE"
        )

        val report = MorningBriefingReport(
            title = "Morning Briefing — Monday, October 26, 2026",
            dateString = "Monday, October 26, 2026",
            executiveSummary = "Good morning. Calibrated after 7h 40m restful sleep.",
            weatherText = "28°C · Sunny & Crisp",
            yesterdayCompletedCount = 6,
            yesterdayPendingCount = 3,
            yesterdayEveningReflection = "Shipped Segment 12 & 13 redesign; zero defects.",
            yesterdayIntakeCount = 5,
            yesterdaySpentMinor = 745000L, // ₹7,450.00
            lastNightSleep = sleep,
            yesterdayWaterMl = 2800,
            habitsMaintainedCount = 4,
            todayScheduleBlocks = listOf(
                ScheduleBlockItem(
                    id = 1L,
                    title = "Deep Architecture Sprint",
                    startTime = "09:00 AM",
                    endTime = "11:30 AM",
                    category = "Deep Work"
                )
            ),
            todayPriorityTasks = listOf(
                TaskItem(
                    id = 10L,
                    title = "Deploy Segment 14 Morning Briefing",
                    priority = Priority.CRITICAL,
                    category = "Engineering"
                )
            ),
            activeAlarms = listOf(
                AlarmItem(
                    id = 101L,
                    label = "Morning Calibrate",
                    alarmTime = 1700000000000L,
                    isEnabled = true
                )
            ),
            criticalAlerts = listOf(
                AttentionItem(
                    id = 201L,
                    title = "Cloud sync bypass active",
                    urgency = "Critical"
                )
            ),
            activeProjects = listOf(project),
            activeGoals = listOf(goal),
            motivationalQuote = "Discipline is choosing between what you want now and what you want most.",
            motivationalAuthor = "Abraham Lincoln"
        )

        assertEquals("Morning Briefing — Monday, October 26, 2026", report.title)
        assertEquals("28°C · Sunny & Crisp", report.weatherText)
        assertEquals("₹7,450.00", report.formattedSpent)
        assertTrue(report.sleepSummary.contains("7h 40m"))
        assertTrue(report.sleepSummary.contains("5★"))
        assertEquals(6, report.yesterdayCompletedCount)
        assertEquals(3, report.yesterdayPendingCount)
        assertEquals("Shipped Segment 12 & 13 redesign; zero defects.", report.yesterdayEveningReflection)
        assertEquals(1, report.todayScheduleBlocks.size)
        assertEquals("09:00 AM", report.todayScheduleBlocks.first().startTime)
        assertEquals(1, report.todayPriorityTasks.size)
        assertEquals("Deploy Segment 14 Morning Briefing", report.todayPriorityTasks.first().title)
        assertEquals(1, report.activeAlarms.size)
        assertEquals(1, report.criticalAlerts.size)
        assertEquals(1, report.activeProjects.size)
        assertEquals(75, report.activeProjects.first().progressPercent)
        assertEquals(25, 100 - report.activeProjects.first().progressPercent) // Pending percent
        assertEquals(1, report.activeGoals.size)
        assertEquals(70, report.activeGoals.first().progressPercent)
        assertEquals("Abraham Lincoln", report.motivationalAuthor)
    }

    @Test
    fun testEndOfDaySummaryReport_metricsAndCalculations() {
        val completedTask = TaskItem(
            id = 201L,
            title = "Finalize Executive Dispatch Layout",
            category = "Design",
            status = TaskStatus.COMPLETED
        )

        val project = ProjectItem(
            id = 2L,
            name = "Offline Knowledge Vault",
            progressPercent = 60,
            status = "ACTIVE"
        )

        val goal = GoalItem(
            id = 2L,
            title = "Read 24 Books",
            progressPercent = 50,
            status = "ACTIVE"
        )

        val mood = MoodItem(
            id = 5L,
            mood = "Calm & Centered",
            energyLevel = "High"
        )

        val eod = EndOfDaySummaryReport(
            title = "End-of-Day Summary — Monday, October 26, 2026",
            dateString = "Monday, October 26, 2026",
            executiveReflection = "Mission accomplished. 8 tasks completed today with 80% completion rate.",
            tasksCompletedToday = listOf(completedTask),
            completedTasksCount = 8,
            pendingTasksCount = 2,
            completionPercentage = 80,
            activeProjects = listOf(project),
            activeGoals = listOf(goal),
            waterMl = 2900,
            mood = mood,
            habitsMaintainedCount = 5,
            eveningReflectionNote = "Built Morning Briefing and Evening Summary with zero defects.",
            spentMinorToday = 42000L // ₹420.00
        )

        assertEquals("End-of-Day Summary — Monday, October 26, 2026", eod.title)
        assertEquals("₹420.00", eod.formattedSpent)
        assertEquals(8, eod.completedTasksCount)
        assertEquals(2, eod.pendingTasksCount)
        assertEquals(80, eod.completionPercentage)
        assertEquals(1, eod.tasksCompletedToday.size)
        assertEquals("Finalize Executive Dispatch Layout", eod.tasksCompletedToday.first().title)
        assertEquals(1, eod.activeProjects.size)
        assertEquals(40, 100 - eod.activeProjects.first().progressPercent) // 40% pending
        assertEquals(1, eod.activeGoals.size)
        assertEquals(50, 100 - eod.activeGoals.first().progressPercent) // 50% pending
        assertEquals(2900, eod.waterMl)
        assertEquals("Calm & Centered", eod.mood?.mood)
        assertEquals(5, eod.habitsMaintainedCount)
    }

    @Test
    fun testMorningBriefingReport_defaults() {
        val defaultReport = MorningBriefingReport()

        assertEquals("🌅 Morning Briefing", defaultReport.title)
        assertEquals("28°C Sunny & Clear", defaultReport.weatherText)
        assertEquals("₹0.00", defaultReport.formattedSpent)
        assertEquals("7h 20m (Restful, 5★)", defaultReport.sleepSummary)
        assertTrue(defaultReport.yesterdayCompletedTasks.isEmpty())
        assertTrue(defaultReport.todayScheduleBlocks.isEmpty())
        assertEquals(2500, defaultReport.yesterdayWaterMl)
        assertNotNull(defaultReport.motivationalQuote)
    }

    @Test
    fun testBriefingItem_modelIntegrity() {
        val briefing = BriefingItem(
            id = 42L,
            title = "Executive Briefing — Oct 26, 2026",
            summaryText = "5 tasks pending. ₹1,200.00 spent. High priority: Core sprint.",
            dateString = "Oct 26, 2026",
            tasksCount = 5,
            spentMinor = 120000L,
            weatherText = "26°C Clear"
        )

        assertEquals(42L, briefing.id)
        assertEquals("Executive Briefing — Oct 26, 2026", briefing.title)
        assertEquals("₹1,200.00", briefing.formattedSpent)
        assertEquals(5, briefing.tasksCount)
        assertEquals("26°C Clear", briefing.weatherText)
    }

    @Test
    fun testMorningBriefingScreen_cardLayoutDataRequirements() {
        val tasks = listOf(
            TaskItem(id = 1L, title = "Task 1", priority = Priority.HIGH, category = "Work"),
            TaskItem(id = 2L, title = "Task 2", priority = Priority.NORMAL, category = "Personal")
        )
        val goals = listOf(
            GoalItem(id = 1L, title = "Quarterly Target", currentAmount = 50.0, targetAmount = 100.0, progressPercent = 50)
        )
        val projects = listOf(
            ProjectItem(id = 1L, name = "Dexter Assistant", progressPercent = 80, status = "ACTIVE")
        )
        val slots = listOf(
            ScheduleBlockItem(id = 1L, title = "Sprint", startTime = "10:00 AM", endTime = "12:00 PM", category = "Deep Work")
        )

        val report = MorningBriefingReport(
            todayPriorityTasks = tasks,
            activeGoals = goals,
            activeProjects = projects,
            todayScheduleBlocks = slots,
            yesterdayCompletedCount = 4,
            yesterdayPendingCount = 2,
            yesterdayWaterMl = 2500,
            habitsMaintainedCount = 3
        )

        assertEquals(2, report.todayPriorityTasks.size)
        assertEquals("Task 1", report.todayPriorityTasks.first().title)
        assertEquals(1, report.activeGoals.size)
        assertEquals(50, report.activeGoals.first().progressPercent)
        assertEquals(1, report.activeProjects.size)
        assertEquals(80, report.activeProjects.first().progressPercent)
        assertEquals(1, report.todayScheduleBlocks.size)
        assertEquals("10:00 AM", report.todayScheduleBlocks.first().startTime)
        assertEquals(4, report.yesterdayCompletedCount)
        assertEquals(2, report.yesterdayPendingCount)
    }
}
