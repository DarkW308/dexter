package com.aistudio.dexter

import com.aistudio.dexter.data.model.AttentionItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DexterAttentionTest {

    @Test
    fun testAttentionItem_criticalUrgencyProperties() {
        val criticalAlert = AttentionItem(
            id = 1L,
            title = "Electricity Bill Due Tomorrow",
            urgency = "Critical",
            category = "FINANCE",
            detail = "Utility payment due in 24 hours.",
            actionTarget = "finance_transactions",
            isAcknowledged = false,
            snoozedUntil = 0L,
            createdAt = System.currentTimeMillis()
        )

        assertEquals("Critical", criticalAlert.urgency)
        assertEquals("#FF1744", criticalAlert.urgencyColorHex)
        assertEquals("💳", criticalAlert.categoryIcon)
        assertEquals("FINANCE", criticalAlert.category)
        assertFalse(criticalAlert.isAcknowledged)
        assertFalse(criticalAlert.isSnoozed)
        assertNotNull(criticalAlert.humanTime)
    }

    @Test
    fun testAttentionItem_snoozeCalculation() {
        val futureSnooze = System.currentTimeMillis() + (120 * 60_000L) // 2 hours
        val snoozedAlert = AttentionItem(
            id = 2L,
            title = "Follow up with Rahul",
            urgency = "High",
            category = "DEADLINE",
            detail = "Check contract milestones.",
            isAcknowledged = false,
            snoozedUntil = futureSnooze
        )

        assertTrue(snoozedAlert.isSnoozed)
        assertTrue(snoozedAlert.remainingSnoozeMinutes in 118L..121L)
        assertEquals("#FF9100", snoozedAlert.urgencyColorHex)
        assertEquals("⏳", snoozedAlert.categoryIcon)
    }

    @Test
    fun testAttentionItem_acknowledgedState() {
        val resolvedAlert = AttentionItem(
            id = 3L,
            title = "Dexter Local Backup Confirmed",
            urgency = "Info",
            category = "SECURITY",
            detail = "Encrypted SQLite WAL verified.",
            isAcknowledged = true,
            snoozedUntil = 0L
        )

        assertTrue(resolvedAlert.isAcknowledged)
        assertFalse(resolvedAlert.isSnoozed)
        assertEquals("🛡️", resolvedAlert.categoryIcon)
        assertEquals("#A0A5BD", resolvedAlert.urgencyColorHex)
    }
}
