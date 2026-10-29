package com.aistudio.dexter

import com.aistudio.dexter.data.backup.DexterBackupManager
import com.aistudio.dexter.data.preferences.SupportedPreferences
import com.aistudio.dexter.data.preferences.UserPreferences
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class DexterProfileAndBackupTest {

    @Test
    fun testLanguageSelection_includesChineseAndRussian() {
        val languages = SupportedPreferences.languages
        val zhCn = languages.find { it.code == "zh-CN" }
        assertNotNull("Chinese Simplified should be supported", zhCn)
        assertEquals("Chinese (Simplified)", zhCn?.englishName)
        assertEquals("中文 (简体)", zhCn?.nativeName)

        val zhTw = languages.find { it.code == "zh-TW" }
        assertNotNull("Chinese Traditional should be supported", zhTw)

        val ru = languages.find { it.code == "ru" }
        assertNotNull("Russian should be supported", ru)
        assertEquals("Russian", ru?.englishName)
        assertEquals("Русский", ru?.nativeName)

        val hi = languages.find { it.code == "hi" }
        assertNotNull("Hindi should be supported", hi)

        val en = languages.find { it.code == "en" }
        assertNotNull("English should be supported", en)

        assertTrue("Languages list should have at least 10 entries", languages.size >= 10)
    }

    @Test
    fun testCurrencySelection_includesChineseYuanAndRussianRuble() {
        val currencies = SupportedPreferences.currencies

        val cny = currencies.find { it.code == "CNY" }
        assertNotNull("Chinese Yuan (CNY) should be supported", cny)
        assertEquals("¥", cny?.symbol)

        val rub = currencies.find { it.code == "RUB" }
        assertNotNull("Russian Ruble (RUB) should be supported", rub)
        assertEquals("₽", rub?.symbol)

        val inr = currencies.find { it.code == "INR" }
        assertNotNull("Indian Rupee (INR) should be supported", inr)
        assertEquals("₹", inr?.symbol)

        val usd = currencies.find { it.code == "USD" }
        assertNotNull("US Dollar (USD) should be supported", usd)
        assertEquals("$", usd?.symbol)

        val eur = currencies.find { it.code == "EUR" }
        assertNotNull("Euro (EUR) should be supported", eur)
        assertEquals("€", eur?.symbol)

        assertTrue("Currencies list should have at least 10 entries", currencies.size >= 10)
    }

    @Test
    fun testCurrencyFormatting_globalCurrencies() {
        val cnyFormatted = SupportedPreferences.formatCurrency(8500.50, "¥")
        assertEquals("¥8,500.50", cnyFormatted)

        val rubFormatted = SupportedPreferences.formatCurrency(250000.0, "₽")
        assertEquals("₽250,000.00", rubFormatted)

        val inrFormatted = SupportedPreferences.formatCurrency(1499.0, "₹")
        assertEquals("₹1,499.00", inrFormatted)

        val usdFormatted = SupportedPreferences.formatCurrency(99.99, "$")
        assertEquals("$99.99", usdFormatted)
    }

    @Test
    fun testDateFormatting_allConfiguredPatterns() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 28, 14, 30, 0)
        }
        val millis = cal.timeInMillis

        val iso = SupportedPreferences.formatDate(millis, "yyyy-MM-dd")
        assertEquals("2026-10-28", iso)

        val dotFormat = SupportedPreferences.formatDate(millis, "dd.MM.yyyy")
        assertEquals("28.10.2026", dotFormat)

        val dayFirst = SupportedPreferences.formatDate(millis, "dd/MM/yyyy")
        assertEquals("28/10/2026", dayFirst)

        val monthFirst = SupportedPreferences.formatDate(millis, "MM/dd/yyyy")
        assertEquals("10/28/2026", monthFirst)
    }

    @Test
    fun testTimeFormatting_12hVs24h() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 30)
        }
        val millis = cal.timeInMillis

        val time24h = SupportedPreferences.formatTime(millis, is24Hour = true)
        assertEquals("20:30", time24h)

        val time12h = SupportedPreferences.formatTime(millis, is24Hour = false)
        assertTrue("12h format should contain 08:30 or 8:30", time12h.contains("08:30") || time12h.contains("8:30"))
        assertTrue("12h format should contain PM", time12h.uppercase().contains("PM"))
    }

    @Test
    fun testBackupValidation_detectsMalformedAndValidPayloads() {
        // Test malformed
        val invalidResult = DexterBackupManager.validateBackupJson("not valid json")
        assertFalse("Malformed JSON should be invalid", invalidResult.isValid)
        assertNotNull(invalidResult.errorMessage)

        // Test missing data or metadata
        val missingDataResult = DexterBackupManager.validateBackupJson("{\"metadata\": {}}")
        assertFalse("Missing data should be invalid", missingDataResult.isValid)

        // Test valid backup payload
        val validJson = JSONObject().apply {
            put("metadata", JSONObject().apply {
                put("version", 1)
                put("appName", "Dexter")
                put("timestamp", 1793200000000L)
                put("totalRecords", 42)
                put("tableCounts", JSONObject().apply {
                    put("tasks", 12)
                    put("goals", 5)
                    put("finance_transactions", 25)
                })
            })
            put("data", JSONObject().apply {
                put("tasks", JSONArray())
                put("goals", JSONArray())
                put("finance_transactions", JSONArray())
            })
        }.toString()

        val validResult = DexterBackupManager.validateBackupJson(validJson)
        assertTrue("Complete payload should be valid", validResult.isValid)
        assertNotNull(validResult.metadata)
        assertEquals(1, validResult.metadata?.version)
        assertEquals("Dexter", validResult.metadata?.appName)
        assertEquals(42, validResult.metadata?.totalRecords)
        assertEquals(12, validResult.metadata?.tableCounts?.get("tasks"))
        assertEquals(5, validResult.metadata?.tableCounts?.get("goals"))
        assertEquals(25, validResult.metadata?.tableCounts?.get("finance_transactions"))
    }

    @Test
    fun testDomainMappingCompleteness_allDomainsCovered() {
        val domainMap = DexterBackupManager.DOMAIN_MAP
        val exportableTables = DexterBackupManager.ALL_EXPORTABLE_TABLES

        assertEquals("All 16 domains should be present in domain map", 16, domainMap.size)

        for ((domain, tables) in domainMap) {
            assertTrue("Domain $domain should map to at least one table", tables.isNotEmpty())
            for (table in tables) {
                assertTrue("Table $table in domain $domain must be in ALL_EXPORTABLE_TABLES", exportableTables.contains(table))
            }
        }
    }
}
