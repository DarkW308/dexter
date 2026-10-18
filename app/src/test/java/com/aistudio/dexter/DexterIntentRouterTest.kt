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
        assertEquals("buy groceries", taskIntent.parameters["title"])

        val expenseIntent = DexterIntentRouter.parse("spent 45 on traveling")
        assertEquals("LOG_EXPENSE", expenseIntent.intent)

        val reminderTimeFirst = DexterIntentRouter.parse("remind me at 5 PM regarding a team meeting")
        assertEquals("CREATE_ALARM", reminderTimeFirst.intent)
        assertEquals("A team meeting", reminderTimeFirst.parameters["label"])

        val reminderActionFirst = DexterIntentRouter.parse("remind me to call doctor at 6 PM")
        assertEquals("CREATE_ALARM", reminderActionFirst.intent)
        assertEquals("Call doctor", reminderActionFirst.parameters["label"])
    }
}
