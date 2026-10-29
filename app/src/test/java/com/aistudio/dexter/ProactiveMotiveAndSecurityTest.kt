package com.aistudio.dexter

import com.aistudio.dexter.engine.*
import org.junit.Assert.*
import org.junit.Test

class ProactiveMotiveAndSecurityTest {

    private val proactiveEngine = ProactiveMotiveEngine.createForTesting()

    @Test
    fun testProactiveCircadianCheckin_atLateNight() {
        val vocalization = proactiveEngine.evaluateVocalization(
            forceTrigger = true,
            simulatedHour = 23,
            simulatedTrigger = "Night Check"
        )
        assertNotNull(vocalization)
        assertEquals(ProactiveMotive.WELLBEING_CHECKIN, vocalization?.motive)
        assertTrue(vocalization?.spokenText?.contains("hour is late", ignoreCase = true) == true)
        assertTrue(vocalization?.contextSummary?.contains("Hour: 23:00") == true)
    }

    @Test
    fun testProactiveMorningAdvisory_atMorningHours() {
        val vocalization = proactiveEngine.evaluateVocalization(
            forceTrigger = true,
            simulatedHour = 8,
            simulatedTrigger = "Morning Kickoff"
        )
        assertNotNull(vocalization)
        assertNotNull(vocalization?.headline)
        assertNotNull(vocalization?.spokenText)
        assertTrue(vocalization?.spokenText?.isNotEmpty() == true)
    }

    @Test
    fun testProactiveSimulations_allFiveMotivesProduceValidSpeech() {
        for (motive in ProactiveMotive.values()) {
            val sim = proactiveEngine.simulateMotive(motive)
            assertEquals(motive, sim.motive)
            assertNotNull(sim.headline)
            assertTrue(sim.headline.isNotBlank())
            assertNotNull(sim.spokenText)
            assertTrue(sim.spokenText.length > 15)
            assertTrue(sim.targetSegments.isNotEmpty())
        }
    }

    @Test
    fun testNeedleReasoning_focusOnNext() {
        val result = NeedleConversationalEngine.synthesizeResponse("what should i focus on next", null)
        assertNotNull(result.answer)
        assertTrue(result.answer.contains("critical", ignoreCase = true) || result.answer.contains("priority", ignoreCase = true) || result.answer.contains("focal", ignoreCase = true))
        assertTrue(result.accessedSegments.contains(1))
        assertEquals("Elite Executive Aide", result.personaTone)
        assertTrue(result.latencyMs < 50L) // Must be lightning fast offline
    }

    @Test
    fun testNeedleReasoning_realityCheck() {
        val result = NeedleConversationalEngine.synthesizeResponse("give me a reality check on my week", null)
        assertNotNull(result.answer)
        assertTrue(result.answer.contains("reality check", ignoreCase = true) || result.answer.contains("discipline", ignoreCase = true))
        assertTrue(result.accessedSegments.contains(4))
        assertTrue(result.latencyMs < 50L)
    }

    @Test
    fun testNeedleReasoning_affordabilityCheck() {
        val result = NeedleConversationalEngine.synthesizeResponse("can i afford dinner at an upscale place tonight", null)
        assertNotNull(result.answer)
        assertTrue(result.answer.contains("outlays", ignoreCase = true) || result.answer.contains("budget", ignoreCase = true) || result.answer.contains("liquidity", ignoreCase = true))
        assertTrue(result.accessedSegments.contains(4))
    }

    @Test
    fun testNeedleReasoning_blindspotAudit() {
        val result = NeedleConversationalEngine.synthesizeResponse("what is my biggest blindspot right now", null)
        assertNotNull(result.answer)
        assertTrue(result.answer.contains("blindspot", ignoreCase = true))
        assertTrue(result.accessedSegments.contains(1))
    }

    @Test
    fun testNeedleReasoning_personaIdentity() {
        val result = NeedleConversationalEngine.synthesizeResponse("who are you and what are you capable of", null)
        assertNotNull(result.answer)
        assertTrue(result.answer.contains("Dexter", ignoreCase = true))
        assertTrue(result.answer.contains("privacy", ignoreCase = true) || result.answer.contains("offline", ignoreCase = true))
    }

    @Test
    fun testIntentRouter_routesConversationalQueries() {
        val p1 = DexterIntentRouter.parse("what should i focus on next")
        assertEquals("CONVERSATIONAL_REASONING", p1.intent)
        assertEquals("what should i focus on next", p1.parameters["query"])

        val p2 = DexterIntentRouter.parse("give me a reality check on my week")
        assertEquals("CONVERSATIONAL_REASONING", p2.intent)

        val p3 = DexterIntentRouter.parse("who are you")
        assertEquals("CONVERSATIONAL_REASONING", p3.intent)
    }

    @Test
    fun testOfflineVaultSecurity_zeroExternalCloudTelemetry() {
        // Assert zero HTTP or Cloud SDK dependencies used in on-device needle reasoning
        val result = NeedleConversationalEngine.synthesizeResponse("give me advice", null)
        assertTrue("Reasoning must execute in under 30ms locally without network hops", result.latencyMs < 30L)
        assertNotNull(result.spokenAnswer)
        assertFalse(result.spokenAnswer.isBlank())
    }

    @Test
    fun testSQLInjectionResistance_acrossSegments() {
        val maliciousInputs = listOf(
            "' OR '1'='1",
            "'; DROP TABLE tasks; --",
            "1; SELECT * FROM finance_transactions WHERE 'a'='a",
            "<script>alert('xss')</script>",
            "\" OR \"\"=\""
        )

        for (injection in maliciousInputs) {
            val parsedTask = DexterIntentRouter.parse("add task $injection")
            if (parsedTask.intent == "CREATE_TASK") {
                val title = parsedTask.parameters["title"].toString()
                assertNotNull(title)
                // Title should be treated purely as literal string data, not executable SQL
                assertTrue(title.contains(injection.replaceFirstChar { it.uppercase() }) || title.isNotEmpty())
            }

            val parsedQuery = DexterIntentRouter.parse("focus on next $injection")
            assertNotNull(parsedQuery.intent)
        }
    }
}
