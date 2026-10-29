package com.aistudio.dexter

import com.aistudio.dexter.data.model.CaptureItem
import com.aistudio.dexter.data.model.IntakeScheduleItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DexterDataGatheringTest {

    @Test
    fun testCaptureItem_voiceSourceAndHumanTime() {
        val voiceCapture = CaptureItem(
            id = 1L,
            noteText = "Logged ₹450 for team lunch at cafe",
            status = "UNPROCESSED",
            sourceType = "VOICE",
            categoryTag = "Expense",
            scheduledWindow = "Ad-hoc",
            capturedAt = System.currentTimeMillis()
        )

        assertEquals("VOICE", voiceCapture.sourceType)
        assertEquals("🎙️", voiceCapture.sourceIcon)
        assertEquals("Voice Command", voiceCapture.sourceLabel)
        assertEquals("Expense", voiceCapture.categoryTag)
        assertFalse(voiceCapture.isProcessed)
        assertNotNull(voiceCapture.humanTime)
    }

    @Test
    fun testCaptureItem_scheduledMorningIntake() {
        val morningCapture = CaptureItem(
            id = 2L,
            noteText = "Complete Segment 12 Apple HIG redesign before 12 PM",
            status = "ROUTED_TASK",
            sourceType = "SCHEDULED_MORNING",
            categoryTag = "Priority",
            scheduledWindow = "Morning Intake",
            routedTo = "tasks"
        )

        assertEquals("🌅", morningCapture.sourceIcon)
        assertEquals("Morning Intake", morningCapture.sourceLabel)
        assertEquals("Morning Intake", morningCapture.scheduledWindow)
        assertTrue(morningCapture.isProcessed)
        assertEquals("ROUTED_TASK", morningCapture.status)
    }

    @Test
    fun testCaptureItem_scheduledEveningReflection() {
        val eveningCapture = CaptureItem(
            id = 3L,
            noteText = "Shipped data gathering hub; 2,500ml water logged; zero late-night snacking.",
            status = "PROCESSED",
            sourceType = "SCHEDULED_EVENING",
            categoryTag = "Reflection",
            scheduledWindow = "Evening Reflection"
        )

        assertEquals("🌙", eveningCapture.sourceIcon)
        assertEquals("Evening Reflection", eveningCapture.sourceLabel)
        assertTrue(eveningCapture.isProcessed)
    }

    @Test
    fun testIntakeScheduleItem_propertiesAndIcons() {
        val morningSchedule = IntakeScheduleItem(
            id = 1L,
            name = "Morning Intake",
            scheduledTime = "07:30 AM",
            targetType = "MORNING_BRIEFING",
            isEnabled = true,
            promptQuestion = "What are your top 3 objectives for today?"
        )

        assertEquals("🌅", morningSchedule.icon)
        assertEquals("07:30 AM", morningSchedule.scheduledTime)
        assertTrue(morningSchedule.isEnabled)

        val eveningSchedule = IntakeScheduleItem(
            id = 3L,
            name = "Evening Reflection",
            scheduledTime = "09:30 PM",
            targetType = "END_OF_DAY_SUMMARY",
            isEnabled = true,
            promptQuestion = "Accomplishments, expenses, and learnings today?"
        )

        assertEquals("🌙", eveningSchedule.icon)
        assertEquals("09:30 PM", eveningSchedule.scheduledTime)
    }
}
