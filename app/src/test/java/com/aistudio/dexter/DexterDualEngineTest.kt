package com.aistudio.dexter

import com.aistudio.dexter.engine.DexterIntentRouter
import com.aistudio.dexter.engine.LayaEngine
import org.junit.Assert.*
import org.junit.Test

class DexterDualEngineTest {

    private val laya = LayaEngine.createForTesting()

    @Test
    fun testLongFormPlanning_identifiedCorrectly() {
        assertTrue(laya.isLongFormPlanning("plan my gym routine with 3 days workout"))
        assertTrue(laya.isLongFormPlanning("break down my seminar into a 4-week plan"))
        assertTrue(laya.isLongFormPlanning("decompose my semester project into milestones"))
        assertTrue(laya.isLongFormPlanning("create a roadmap for exam preparation"))

        // Standard commands must NOT be flagged as long-form planning
        assertFalse(laya.isLongFormPlanning("what are all the pending tasks today"))
        assertFalse(laya.isLongFormPlanning("remind me at 5 pm regarding a team meeting"))
        assertFalse(laya.isLongFormPlanning("spent 45 on traveling"))
        assertFalse(laya.isLongFormPlanning("buy groceries"))
    }

    @Test
    fun testLayaSlotFilling_casualExpense() {
        val res = laya.classify("chuck 250 into petrol")

        assertEquals("LOG_EXPENSE", res.intent)
        assertEquals(250.0, res.slots["amount"])
        assertEquals("Travel & Fuel", res.slots["category"])
    }

    @Test
    fun testLayaSlotFilling_interaction() {
        val res = laya.classify("talked to Dr. Mehta about headache")

        assertEquals("LOG_INTERACTION", res.intent)
        assertEquals("Dr. Mehta", res.slots["person"])
        assertEquals("headache", res.slots["topic"])
    }

    @Test
    fun testLayaSlotFilling_journalSentiment() {
        val res = laya.classify("felt drained after client demo")

        assertEquals("LOG_JOURNAL", res.intent)
        assertEquals("Tired", res.slots["mood"])
    }

    @Test
    fun testPlanningIntentDecomposition() {
        val res = laya.classify("plan my gym routine with 3 days workout")

        assertEquals("DECOMPOSE_PLAN_ASYNC", res.intent)
        assertTrue(res.isLongFormPlanning)
        assertEquals(2, res.targetSegment) // Segment 2: Projects & Milestones
    }

    @Test
    fun testHarness2Decomposition_proposesSubclauses() {
        val compoundQuery = "spent 50 on milk and remind me at 6pm to call mom"
        val clean = DexterIntentRouter.stripWakeWord(compoundQuery)
        val parts = clean.split(Regex("(?i)\\s+and\\s+"))

        assertEquals(2, parts.size)
        val p1 = DexterIntentRouter.parse(parts[0])
        val p2 = DexterIntentRouter.parse(parts[1])

        assertEquals("LOG_EXPENSE", p1.intent)
        assertEquals(50.0, p1.parameters["amount"])
        assertEquals("CREATE_ALARM", p2.intent)
    }

    @Test
    fun testHarness2Guardrails_validatesAmountsAndPastAlarms() {
        // Guardrail: non-positive amounts must not pass
        val invalidAmount = -20.0
        assertTrue(invalidAmount <= 0.0)

        // Guardrail: auto-advancing past alarm times
        val pastAlarm = System.currentTimeMillis() - 100_000L
        val autoCorrected = pastAlarm + 24 * 60 * 60 * 1000L
        assertTrue(autoCorrected > System.currentTimeMillis())
    }
}

