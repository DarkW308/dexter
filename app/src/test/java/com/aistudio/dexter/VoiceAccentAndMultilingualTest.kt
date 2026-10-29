package com.aistudio.dexter

import com.aistudio.dexter.engine.DexterIntentRouter
import com.aistudio.dexter.engine.LayaEngine
import com.aistudio.dexter.engine.ParsedIntent
import org.junit.Assert.*
import org.junit.Test

class VoiceAccentAndMultilingualTest {

    private val laya = LayaEngine.createForTesting()

    @Test
    fun testPhoneticAndAccentTolerances_expenses() {
        // Phonetic misrecognition / accent: "spen" instead of "spent"
        val p1 = DexterIntentRouter.parse("spen 50 bucks on fuel")
        assertEquals("LOG_EXPENSE", p1.intent)
        assertEquals(50.0, p1.parameters["amountRupees"])
        assertEquals("Transportation", p1.parameters["category"])

        // Casual slang: "drop thirty on boba"
        val p2 = DexterIntentRouter.parse("drop 30 on boba tea")
        assertEquals("LOG_EXPENSE", p2.intent)
        assertEquals(30.0, p2.parameters["amountRupees"])
        assertEquals("Food & Dining", p2.parameters["category"])

        // Colloquial verb: "chuck 250 into petrol"
        val p3 = DexterIntentRouter.parse("chuck 250 into petrol")
        assertEquals("LOG_EXPENSE", p3.intent)
        assertEquals(250.0, p3.parameters["amountRupees"])
        assertEquals("Transportation", p3.parameters["category"])

        // "blew 40 on drinks"
        val p4 = DexterIntentRouter.parse("blew 40 on drinks")
        assertEquals("LOG_EXPENSE", p4.intent)
        assertEquals(40.0, p4.parameters["amountRupees"])
    }

    @Test
    fun testPhoneticAndAccentTolerances_tasks() {
        // Phonetic misrecognition: "crate task" instead of "create task"
        val p1 = DexterIntentRouter.parse("crate task review PR tomorrow")
        assertEquals("CREATE_TASK", p1.intent)
        assertTrue(p1.parameters["title"].toString().contains("Review PR tomorrow", ignoreCase = true))

        // Phonetic: "ad task"
        val p2 = DexterIntentRouter.parse("ad task buy almond milk")
        assertEquals("CREATE_TASK", p2.intent)
        assertTrue(p2.parameters["title"].toString().contains("Buy almond milk", ignoreCase = true))

        // "new task finish report"
        val p3 = DexterIntentRouter.parse("new task finish quarterly report")
        assertEquals("CREATE_TASK", p3.intent)
        assertTrue(p3.parameters["title"].toString().contains("Finish quarterly report", ignoreCase = true))
    }

    @Test
    fun testChineseVoiceSlotExtraction() {
        // Chinese Expense (Prefix verb)
        val p1 = DexterIntentRouter.parse("花了50元买咖啡")
        assertEquals("LOG_EXPENSE", p1.intent)
        assertEquals(50.0, p1.parameters["amountRupees"])
        assertEquals("Food & Dining", p1.parameters["category"])

        // Chinese Transport Expense
        val p2 = DexterIntentRouter.parse("花费100块打车")
        assertEquals("LOG_EXPENSE", p2.intent)
        assertEquals(100.0, p2.parameters["amountRupees"])
        assertEquals("Transportation", p2.parameters["category"])

        // Chinese Expense (Noun first)
        val p3 = DexterIntentRouter.parse("买咖啡花了35元")
        assertEquals("LOG_EXPENSE", p3.intent)
        assertEquals(35.0, p3.parameters["amountRupees"])

        // Chinese Task
        val p4 = DexterIntentRouter.parse("新建任务下午三点开会")
        assertEquals("CREATE_TASK", p4.intent)
        assertTrue(p4.parameters["title"].toString().contains("下午三点开会"))

        // Chinese Alarm
        val p5 = DexterIntentRouter.parse("定个早上7点的闹钟")
        assertEquals("CREATE_ALARM", p5.intent)
        assertNotNull(p5.parameters["alarmTime"])

        // Chinese Goal
        val p6 = DexterIntentRouter.parse("设定目标存一万元")
        assertEquals("CREATE_GOAL", p6.intent)
    }

    @Test
    fun testRussianVoiceSlotExtraction() {
        // Russian Transport Expense: "потратил 500 рублей на такси"
        val p1 = DexterIntentRouter.parse("потратил 500 рублей на такси")
        assertEquals("LOG_EXPENSE", p1.intent)
        assertEquals(500.0, p1.parameters["amountRupees"])
        assertEquals("Transportation", p1.parameters["category"])

        // Russian Food Expense: "купил кофе за 250 рублей"
        val p2 = DexterIntentRouter.parse("купил кофе за 250 рублей")
        assertEquals("LOG_EXPENSE", p2.intent)
        assertEquals(250.0, p2.parameters["amountRupees"])
        assertEquals("Food & Dining", p2.parameters["category"])

        // Russian Task: "добавь задачу купить молоко завтра"
        val p3 = DexterIntentRouter.parse("добавь задачу купить молоко завтра")
        assertEquals("CREATE_TASK", p3.intent)
        assertTrue(p3.parameters["title"].toString().contains("купить молоко завтра", ignoreCase = true))

        // Russian Alarm: "поставь будильник на 7 утра"
        val p4 = DexterIntentRouter.parse("поставь будильник на 7 утра")
        assertEquals("CREATE_ALARM", p4.intent)

        // Russian Goal: "поставь цель накопить 100000 рублей"
        val p5 = DexterIntentRouter.parse("поставь цель накопить 100000 рублей")
        assertEquals("CREATE_GOAL", p5.intent)
    }

    @Test
    fun testHindiHinglishSlotExtraction() {
        // Hinglish Expense: "500 rupaye lunch pe kharch kiye"
        val p1 = DexterIntentRouter.parse("500 rupaye lunch pe kharch kiye")
        assertEquals("LOG_EXPENSE", p1.intent)
        assertEquals(500.0, p1.parameters["amountRupees"])
        assertEquals("Food & Dining", p1.parameters["category"])

        // Hinglish Tea Expense: "kharcha hua 200 chai pe"
        val p2 = DexterIntentRouter.parse("kharcha hua 200 chai pe")
        assertEquals("LOG_EXPENSE", p2.intent)
        assertEquals(200.0, p2.parameters["amountRupees"])
        assertEquals("Food & Dining", p2.parameters["category"])

        // Hinglish Task: "report submit karne ka task banao"
        val p3 = DexterIntentRouter.parse("report submit karne ka task banao")
        assertEquals("CREATE_TASK", p3.intent)
        assertTrue(p3.parameters["title"].toString().contains("report submit", ignoreCase = true))

        // Hinglish Alarm: "subah 7 baje ka alarm lagao"
        val p4 = DexterIntentRouter.parse("subah 7 baje ka alarm lagao")
        assertEquals("CREATE_ALARM", p4.intent)
    }

    @Test
    fun testChainedMultiSegmentCommands() {
        // Dual command: Task (Segment 1) + Expense (Segment 4)
        val p1 = DexterIntentRouter.parse("add task review pull request and spent 35 on coffee")
        assertEquals("CHAINED_MULTI_COMMAND", p1.intent)

        @Suppress("UNCHECKED_CAST")
        val subIntents = p1.parameters["subIntents"] as? List<ParsedIntent>
        assertNotNull("Sub-intents should not be null", subIntents)
        assertEquals(2, subIntents?.size)

        assertEquals("CREATE_TASK", subIntents?.get(0)?.intent)
        assertTrue(subIntents?.get(0)?.parameters?.get("title").toString().contains("Review pull request", ignoreCase = true))

        assertEquals("LOG_EXPENSE", subIntents?.get(1)?.intent)
        assertEquals(35.0, subIntents?.get(1)?.parameters?.get("amountRupees"))
    }

    @Test
    fun testRussianChainedCommands() {
        val pRu = DexterIntentRouter.parse("добавь задачу купить хлеб и потратил 200 рублей на кофе")
        assertEquals("CHAINED_MULTI_COMMAND", pRu.intent)

        @Suppress("UNCHECKED_CAST")
        val sub = pRu.parameters["subIntents"] as? List<ParsedIntent>
        assertEquals(2, sub?.size)
        assertEquals("CREATE_TASK", sub?.get(0)?.intent)
        assertEquals("LOG_EXPENSE", sub?.get(1)?.intent)
    }

    @Test
    fun testChineseChainedCommands() {
        val pZh = DexterIntentRouter.parse("新建任务准备演讲而且花了50元买书")
        assertEquals("CHAINED_MULTI_COMMAND", pZh.intent)

        @Suppress("UNCHECKED_CAST")
        val sub = pZh.parameters["subIntents"] as? List<ParsedIntent>
        assertEquals(2, sub?.size)
        assertEquals("CREATE_TASK", sub?.get(0)?.intent)
        assertEquals("LOG_EXPENSE", sub?.get(1)?.intent)
    }

    @Test
    fun testVoicePipelineTraceAnalyzer_generatesCorrectTrace() {
        // Test Chinese Transport trace
        val traceZh = laya.analyzePipeline("花费100块打车")
        assertTrue(traceZh.isSuccess)
        assertEquals("Chinese (中文)", traceZh.tokenTrace.languageTag)
        assertEquals("LOG_EXPENSE", traceZh.intent)
        assertEquals(4, traceZh.targetSegment)
        assertEquals("finance_transactions", traceZh.dbTraces.firstOrNull()?.tableName)

        // Test Slang Accent trace
        val traceSlang = laya.analyzePipeline("spen 50 bucks on fuel")
        assertTrue(traceSlang.isSuccess)
        assertEquals("Colloquial / Accented English", traceSlang.tokenTrace.languageTag)
        assertEquals("LOG_EXPENSE", traceSlang.intent)
        assertEquals(4, traceSlang.targetSegment)

        // Test Chained multi-command trace
        val traceChained = laya.analyzePipeline("add task review code and spent 35 on coffee")
        assertTrue(traceChained.isSuccess)
        assertEquals("CHAINED_MULTI_COMMAND", traceChained.intent)
        assertEquals(2, traceChained.dbTraces.size)
        assertEquals("tasks", traceChained.dbTraces[0].tableName)
        assertEquals("finance_transactions", traceChained.dbTraces[1].tableName)
    }
}
