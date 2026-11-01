package com.aistudio.dexter

import com.aistudio.dexter.engine.DexterIntentRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DexterIntentRouterTest {

    @Test
    fun testIsInfoQuery_identifiesQuestionKeywords() {
        assertTrue(DexterIntentRouter.isInterrogativeQuery("what are all the pending tasks today"))
        assertTrue(DexterIntentRouter.isInterrogativeQuery("what do i have to do"))
        assertTrue(DexterIntentRouter.isInterrogativeQuery("show me my tasks"))
        assertTrue(DexterIntentRouter.isInterrogativeQuery("show tasks"))
        assertTrue(DexterIntentRouter.isInterrogativeQuery("list pending tasks"))
        assertTrue(DexterIntentRouter.isInterrogativeQuery("do i have any tasks"))
        assertTrue(DexterIntentRouter.isInterrogativeQuery("do i have any alarms set"))
        assertTrue(DexterIntentRouter.isInterrogativeQuery("tell me my schedule"))
        assertTrue(DexterIntentRouter.isInterrogativeQuery("how much did i spend"))
    }

    @Test
    fun testInterrogativeQueryGuard_directCalls() {
        val res1 = DexterIntentRouter.interrogativeQueryGuard("what are all the pending tasks today", "what are all the pending tasks today")
        assertEquals("LIST_PENDING_TASKS", res1?.intent)

        val res2 = DexterIntentRouter.interrogativeQueryGuard("show my tasks", "show my tasks")
        assertEquals("LIST_PENDING_TASKS", res2?.intent)

        val res3 = DexterIntentRouter.interrogativeQueryGuard("list all pending tasks", "list all pending tasks")
        assertEquals("LIST_PENDING_TASKS", res3?.intent)

        val res4 = DexterIntentRouter.interrogativeQueryGuard("do i have any tasks today", "do i have any tasks today")
        assertEquals("LIST_PENDING_TASKS", res4?.intent)

        val res5 = DexterIntentRouter.interrogativeQueryGuard("buy groceries", "buy groceries")
        assertEquals(null, res5)
    }

    @Test
    fun testRouteInfoQuery_routesTasksQueries() {
        val intent1 = DexterIntentRouter.parse("what are all the pending tasks today")
        assertEquals("LIST_PENDING_TASKS", intent1.intent)

        val intent2 = DexterIntentRouter.parse("show my tasks")
        assertEquals("LIST_PENDING_TASKS", intent2.intent)

        val intent3 = DexterIntentRouter.parse("list pending tasks")
        assertEquals("LIST_PENDING_TASKS", intent3.intent)

        val intent4 = DexterIntentRouter.parse("do i have any tasks today")
        assertEquals("LIST_PENDING_TASKS", intent4.intent)

        val intent5 = DexterIntentRouter.parse("what have i finished")
        assertEquals("LIST_COMPLETED_TASKS", intent5.intent)
    }

    @Test
    fun testRouteInfoQuery_routesAlarmsAndRemindersQueries() {
        val intent1 = DexterIntentRouter.parse("what alarms do i have")
        assertEquals("LIST_ALARMS", intent1.intent)

        val intent2 = DexterIntentRouter.parse("show my reminders")
        assertEquals("LIST_ALARMS", intent2.intent)

        val intent3 = DexterIntentRouter.parse("do i have any alarms set")
        assertEquals("LIST_ALARMS", intent3.intent)

        val intent4 = DexterIntentRouter.parse("list all reminders")
        assertEquals("LIST_ALARMS", intent4.intent)
    }

    @Test
    fun testRouteInfoQuery_routesFinanceQueries() {
        val intent1 = DexterIntentRouter.parse("how much did i spend today")
        assertEquals("SUMMARIZE_EXPENSES", intent1.intent)

        val intent2 = DexterIntentRouter.parse("what are my total expenses")
        assertEquals("SUMMARIZE_EXPENSES", intent2.intent)

        val intent3 = DexterIntentRouter.parse("show my spending")
        assertEquals("SUMMARIZE_EXPENSES", intent3.intent)
    }

    @Test
    fun testRouteInfoQuery_routesJournalAndNotesQueries() {
        val intent1 = DexterIntentRouter.parse("show my notes")
        assertEquals("LIST_JOURNAL", intent1.intent)

        val intent2 = DexterIntentRouter.parse("what did i journal today")
        assertEquals("LIST_JOURNAL", intent2.intent)

        val intent3 = DexterIntentRouter.parse("list notes")
        assertEquals("LIST_JOURNAL", intent3.intent)
    }

    @Test
    fun testRouteInfoQuery_routesGoalsAndProjectsQueries() {
        val intent1 = DexterIntentRouter.parse("what are my goals")
        assertEquals("LIST_GOALS", intent1.intent)

        val intent2 = DexterIntentRouter.parse("show projects")
        assertEquals("LIST_PROJECTS", intent2.intent)
    }

    @Test
    fun testRouteInfoQuery_routesBriefingAndScheduleQueries() {
        val intent1 = DexterIntentRouter.parse("what is my schedule today")
        assertEquals("DAILY_BRIEFING", intent1.intent)

        val intent2 = DexterIntentRouter.parse("show me the morning summary")
        assertEquals("DAILY_BRIEFING", intent2.intent)
    }

    @Test
    fun testInterrogativeNeverSwallowedAsTask() {
        // Raw speech queries without punctuation should NEVER fall into CREATE_TASK
        val queries = listOf(
            "what are all the pending tasks today",
            "show all tasks",
            "list pending task",
            "do i have any meetings today",
            "what do i have"
        )

        for (q in queries) {
            val parsed = DexterIntentRouter.parse(q)
            assertNotEquals("CREATE_TASK", parsed.intent)
        }
    }

    @Test
    fun testCommandsStillCreateProperActions() {
        val taskIntent = DexterIntentRouter.parse("buy groceries")
        assertEquals("CREATE_TASK", taskIntent.intent)
        assertEquals("Buy groceries", taskIntent.parameters["title"])

        val expenseIntent = DexterIntentRouter.parse("spent 45 on traveling")
        assertEquals("LOG_EXPENSE", expenseIntent.intent)

        val reminderTimeFirst = DexterIntentRouter.parse("remind me at 5 PM regarding a team meeting")
        assertEquals("CREATE_ALARM", reminderTimeFirst.intent)
        assertEquals("A team meeting", reminderTimeFirst.parameters["label"])

        val reminderActionFirst = DexterIntentRouter.parse("remind me to call doctor at 6 PM")
        assertEquals("CREATE_ALARM", reminderActionFirst.intent)
        assertEquals("Call doctor", reminderActionFirst.parameters["label"])
    }

    @Test
    fun testWakeWordStripping_alexaAndDexter() {
        val q1 = DexterIntentRouter.parse("Alexa, what are all the pending tasks today")
        assertEquals("LIST_PENDING_TASKS", q1.intent)

        val q2 = DexterIntentRouter.parse("Hey Dexter, what is my schedule today")
        assertEquals("DAILY_BRIEFING", q2.intent)

        val q3 = DexterIntentRouter.parse("Alexa create project Website with deadline 2026-12-31")
        assertEquals("CREATE_PROJECT", q3.intent)
        assertEquals("Website", q3.parameters["title"])
        assertEquals("2026-12-31", q3.parameters["deadline"])
    }

    @Test
    fun testProjectAndMilestonesVoiceCommands() {
        val createMilestone = DexterIntentRouter.parse("add milestone UI Design to project Website")
        assertEquals("CREATE_MILESTONE", createMilestone.intent)
        assertEquals("Website", createMilestone.parameters["project_name"])
        assertEquals("UI Design", createMilestone.parameters["title"])

        val completeMilestone = DexterIntentRouter.parse("mark milestone UI Design done in project Website")
        assertEquals("COMPLETE_MILESTONE", completeMilestone.intent)
        assertEquals("Website", completeMilestone.parameters["project_name"])
        assertEquals("UI Design", completeMilestone.parameters["milestone_name"])

        val updateDeadline = DexterIntentRouter.parse("update deadline of project Website to 2026-11-30")
        assertEquals("UPDATE_PROJECT_DEADLINE", updateDeadline.intent)
        assertEquals("Website", updateDeadline.parameters["project_name"])
        assertEquals("2026-11-30", updateDeadline.parameters["deadline"])

        val getStatus = DexterIntentRouter.parse("what is the status of project Website")
        assertEquals("GET_PROJECT_STATUS", getStatus.intent)
        assertEquals("Website", getStatus.parameters["project_name"])
    }

    @Test
    fun testAlarmWakeUpConversationFlow() {
        val awake1 = DexterIntentRouter.parse("I'm awake")
        assertEquals("ALARM_I_AM_AWAKE", awake1.intent)

        val awake2 = DexterIntentRouter.parse("Alexa, I am awake now")
        assertEquals("ALARM_I_AM_AWAKE", awake2.intent)

        val awake3 = DexterIntentRouter.parse("Good morning")
        assertEquals("ALARM_I_AM_AWAKE", awake3.intent)

        val snooze1 = DexterIntentRouter.parse("Snooze")
        assertEquals("ALARM_SNOOZE", snooze1.intent)
        assertEquals(5, snooze1.parameters["minutes"])

        val snooze2 = DexterIntentRouter.parse("Alexa, give me 5 more minutes")
        assertEquals("ALARM_SNOOZE", snooze2.intent)

        val snooze3 = DexterIntentRouter.parse("Snooze 10 minutes")
        assertEquals("ALARM_SNOOZE", snooze3.intent)
        assertEquals(10, snooze3.parameters["minutes"])

        val dismiss1 = DexterIntentRouter.parse("Turn off alarm")
        assertEquals("ALARM_DISMISS", dismiss1.intent)

        val dismiss2 = DexterIntentRouter.parse("Alexa stop alarm")
        assertEquals("ALARM_DISMISS", dismiss2.intent)
    }

    @Test
    fun testGoalVoiceCommands_multiDomainSupport() {
        val eduGoal = DexterIntentRouter.parse("set an education goal to read 12 books by Dec 31")
        assertEquals("CREATE_GOAL", eduGoal.intent)
        assertEquals("Education & Learning", eduGoal.parameters["category"])
        assertEquals(12.0, eduGoal.parameters["target_amount"] as Double, 0.001)
        assertEquals("books", eduGoal.parameters["metric_unit"])
        assertEquals("Dec 31", eduGoal.parameters["target_date"])
        assertTrue(eduGoal.parameters["title"].toString().contains("read 12 books", ignoreCase = true))

        val healthGoal = DexterIntentRouter.parse("create a health goal to run 50 km by Dec 31 2026")
        assertEquals("CREATE_GOAL", healthGoal.intent)
        assertEquals("Health & Fitness", healthGoal.parameters["category"])
        assertEquals(50.0, healthGoal.parameters["target_amount"] as Double, 0.001)
        assertEquals("km", healthGoal.parameters["metric_unit"])
        assertEquals("Dec 31 2026", healthGoal.parameters["target_date"])

        val workGoal = DexterIntentRouter.parse("set a work goal to finish 5 projects")
        assertEquals("CREATE_GOAL", workGoal.intent)
        assertEquals("Work & Career", workGoal.parameters["category"])
        assertEquals(5.0, workGoal.parameters["target_amount"] as Double, 0.001)
        assertEquals("projects", workGoal.parameters["metric_unit"])

        val financeGoal = DexterIntentRouter.parse("set a goal to save 50000 rupees by Dec 2026")
        assertEquals("CREATE_GOAL", financeGoal.intent)
        assertEquals("Financial", financeGoal.parameters["category"])
        assertEquals(50000.0, financeGoal.parameters["target_amount"] as Double, 0.001)
        assertEquals("₹", financeGoal.parameters["metric_unit"])

        val personalGoal = DexterIntentRouter.parse("my goal is to meditate for 30 days")
        assertEquals("CREATE_GOAL", personalGoal.intent)
        assertEquals(30.0, personalGoal.parameters["target_amount"] as Double, 0.001)
        assertEquals("days", personalGoal.parameters["metric_unit"])
    }
}
