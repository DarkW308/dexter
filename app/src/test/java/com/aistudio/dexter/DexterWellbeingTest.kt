package com.aistudio.dexter

import com.aistudio.dexter.data.model.HydrationItem
import com.aistudio.dexter.data.model.MoodItem
import com.aistudio.dexter.data.model.SleepItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DexterWellbeingTest {

    @Test
    fun testHydrationItem_propertiesAndFormatting() {
        val hydration = HydrationItem(id = 1L, amountMl = 500, loggedAt = 1700000000000L)
        assertEquals(1L, hydration.id)
        assertEquals(500, hydration.amountMl)
        assertNotNull(hydration.humanTime)
        assertTrue(hydration.humanTime.isNotBlank())
    }

    @Test
    fun testSleepItem_properties() {
        val sleep = SleepItem(
            id = 2L,
            bedtime = "11:30 PM",
            waketime = "07:00 AM",
            durationText = "7h 30m",
            qualityRating = 5,
            qualityLabel = "Restful",
            notes = "Deep refreshing recovery"
        )
        assertEquals("11:30 PM", sleep.bedtime)
        assertEquals("07:00 AM", sleep.waketime)
        assertEquals("7h 30m", sleep.durationText)
        assertEquals(5, sleep.qualityRating)
        assertEquals("Restful", sleep.qualityLabel)
        assertEquals("Deep refreshing recovery", sleep.notes)
    }

    @Test
    fun testMoodItem_propertiesAndHumanTime() {
        val mood = MoodItem(
            id = 3L,
            mood = "😊 Joyful",
            energyLevel = "High",
            note = "Shipped Segment 9 on schedule",
            loggedAt = System.currentTimeMillis()
        )
        assertEquals("😊 Joyful", mood.mood)
        assertEquals("High", mood.energyLevel)
        assertEquals("Shipped Segment 9 on schedule", mood.note)
        assertNotNull(mood.humanTime)
        assertTrue(mood.humanTime.isNotBlank())
    }

    @Test
    fun testHabitItem_buildHabitProperties() {
        val habit = com.aistudio.dexter.data.model.HabitItem(
            id = 4L,
            name = "Read 15 pages of book",
            frequency = "Daily",
            reminderTime = "09:30 PM",
            streakCount = 12,
            isDoneToday = true,
            habitType = "BUILD",
            category = "Focus"
        )
        assertEquals("Read 15 pages of book", habit.name)
        assertEquals(12, habit.streakCount)
        assertTrue(habit.isDoneToday)
        assertEquals("BUILD", habit.habitType)
        org.junit.Assert.assertFalse(habit.isQuitHabit)
    }

    @Test
    fun testHabitItem_quitBadHabitProperties() {
        val habit = com.aistudio.dexter.data.model.HabitItem(
            id = 5L,
            name = "No Late Night Junk Food",
            frequency = "Daily",
            reminderTime = "10:00 PM",
            streakCount = 18,
            isDoneToday = true,
            habitType = "QUIT",
            category = "Health",
            quitReason = "Better sleep and physical energy"
        )
        assertEquals("No Late Night Junk Food", habit.name)
        assertEquals(18, habit.streakCount)
        assertTrue(habit.isDoneToday)
        assertEquals("QUIT", habit.habitType)
        assertTrue(habit.isQuitHabit)
        assertEquals("Better sleep and physical energy", habit.quitReason)
    }

    @Test
    fun testDocumentItem_coreMemoryAndMetadata() {
        val doc = com.aistudio.dexter.data.model.DocumentItem(
            id = 1L,
            title = "Peak Focus & Working Hours Routine",
            source = "Preferences",
            fileType = "PREF",
            excerpt = "Primary deep work block is 09:00 AM to 01:00 PM...",
            chunkCount = 1,
            content = "Primary deep work block is 09:00 AM to 01:00 PM for coding and architecture. No non-critical interruptions during this window.",
            category = "Preferences",
            isCoreMemory = true,
            tags = "#routine #focus #calendar"
        )
        assertEquals("Peak Focus & Working Hours Routine", doc.title)
        assertTrue(doc.isCoreMemory)
        assertEquals("Preferences", doc.category)
        assertTrue(doc.wordCount > 10)
        assertTrue(doc.estimatedReadMinutes >= 1)
        assertTrue(doc.cleanPreview.startsWith("Primary deep work block"))
        assertTrue(doc.tags.contains("#focus"))
    }
}
