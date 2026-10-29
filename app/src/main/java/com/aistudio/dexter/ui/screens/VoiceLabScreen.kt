package com.aistudio.dexter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.dexter.engine.VoicePipelineTrace
import com.aistudio.dexter.ui.components.*
import com.aistudio.dexter.ui.theme.*

data class LabPreset(
    val title: String,
    val phrase: String,
    val category: String,
    val expectedSegment: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceLabScreen(
    currentTrace: VoicePipelineTrace?,
    history: List<VoicePipelineTrace>,
    onBack: () -> Unit,
    onExecuteQuery: (String) -> Unit,
    onVoiceClick: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val presets = remember {
        listOf(
            // Accents & Slang
            LabPreset("Acoustic Slang Expense", "spen 50 bucks on fuel", "Accents & Slang", "Segment 4 (Finance)"),
            LabPreset("Phonetic Accent Task", "crate task review PR tomorrow", "Accents & Slang", "Segment 1 (Tasks)"),
            LabPreset("Colloquial Verb Spend", "drop 30 on boba tea", "Accents & Slang", "Segment 4 (Finance)"),
            LabPreset("Casual Slang Cash", "chuck 250 into petrol", "Accents & Slang", "Segment 4 (Finance)"),
            LabPreset("Colloquial Blow", "blew 40 on drinks", "Accents & Slang", "Segment 4 (Finance)"),

            // Chinese (中文)
            LabPreset("Chinese Food Expense", "花了50元买咖啡", "Chinese (中文)", "Segment 4 (Finance)"),
            LabPreset("Chinese Transport", "花费100块打车", "Chinese (中文)", "Segment 4 (Finance)"),
            LabPreset("Chinese Task Creation", "新建任务下午三点开会", "Chinese (中文)", "Segment 1 (Tasks)"),
            LabPreset("Chinese Alarm Trigger", "定个早上7点的闹钟", "Chinese (中文)", "Segment 6 (Alarms)"),
            LabPreset("Chinese Financial Goal", "设定目标存一万元", "Chinese (中文)", "Segment 3 (Goals)"),

            // Russian (Русский)
            LabPreset("Russian Transport", "потратил 500 рублей на такси", "Russian (Русский)", "Segment 4 (Finance)"),
            LabPreset("Russian Task Creation", "добавь задачу купить молоко завтра", "Russian (Русский)", "Segment 1 (Tasks)"),
            LabPreset("Russian Food Expense", "купил кофе за 250 рублей", "Russian (Русский)", "Segment 4 (Finance)"),
            LabPreset("Russian Alarm Trigger", "поставь будильник на 7 утра", "Russian (Русский)", "Segment 6 (Alarms)"),
            LabPreset("Russian Target Goal", "поставь цель накопить 100000 рублей", "Russian (Русский)", "Segment 3 (Goals)"),

            // Hindi / Hinglish
            LabPreset("Hinglish Food Spend", "500 rupaye lunch pe kharch kiye", "Hindi (हिन्दी)", "Segment 4 (Finance)"),
            LabPreset("Hindi Task Suffix", "report submit karne ka task banao", "Hindi (हिन्दी)", "Segment 1 (Tasks)"),
            LabPreset("Hindi Alarm Command", "subah 7 baje ka alarm lagao", "Hindi (हिन्दी)", "Segment 6 (Alarms)"),
            LabPreset("Hindi Tea Expense", "kharcha hua 200 chai pe", "Hindi (हिन्दी)", "Segment 4 (Finance)"),

            // Chained Multi-Commands
            LabPreset("Dual Task & Expense", "add task review code and spent 35 on coffee", "Chained Commands", "Segments 1 & 4"),
            LabPreset("Russian Chained Action", "добавь задачу купить хлеб и потратил 200 рублей на кофе", "Chained Commands", "Segments 1 & 4"),
            LabPreset("Chinese Chained Action", "新建任务准备演讲而且花了50元买书", "Chained Commands", "Segments 1 & 4"),

            // Needle-3 Conversational Reasoning
            LabPreset("Next Strategic Leverage", "what should i focus on next", "Needle-3 Reasoning", "Cross-Segments 1 & 3"),
            LabPreset("Objective Reality Check", "give me a reality check on my week", "Needle-3 Reasoning", "Cross-Segments 1, 4 & 3"),
            LabPreset("Financial Feasibility", "can i afford dinner at an upscale place tonight", "Needle-3 Reasoning", "Segment 4 (Finance)"),
            LabPreset("Risk & Blindspot Audit", "what is my biggest blindspot right now", "Needle-3 Reasoning", "Cross-Segments 1 & 3"),
            LabPreset("Executive Persona & Identity", "who are you and what are you capable of", "Needle-3 Reasoning", "Offline Persona"),

            // Proactive Speaking & Motives
            LabPreset("Morning Kickoff Advisory", "[PROACTIVE] Morning Kickoff", "Proactive Speaking", "Autonomous Segment 1"),
            LabPreset("Velocity Peak Momentum", "[PROACTIVE] Execution Velocity", "Proactive Speaking", "Autonomous Segment 1"),
            LabPreset("Daily Budget Pacing", "[PROACTIVE] Budget Pacing", "Proactive Speaking", "Autonomous Segment 4"),
            LabPreset("Circadian Recovery", "[PROACTIVE] Wellbeing Recovery", "Proactive Speaking", "Autonomous Segment 8"),
            LabPreset("Curated Executive Wit", "[PROACTIVE] Curated Wit", "Proactive Speaking", "Autonomous Persona")
        )
    }

    val categories = listOf("All", "Accents & Slang", "Needle-3 Reasoning", "Proactive Speaking", "Chinese (中文)", "Russian (Русский)", "Hindi (हिन्दी)", "Chained Commands")

    val filteredPresets = remember(selectedCategory) {
        if (selectedCategory == "All") presets else presets.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Voice & Laya Pipeline Lab",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = DexterOnBackground
                        )
                        Text(
                            "Accents · Multilingual · Slot Extractor · SQL Mapper",
                            fontSize = 11.sp,
                            color = DexterPrimaryGlow
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DexterOnBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DexterBackground
                )
            )
        },
        containerColor = DexterBackground,
        modifier = Modifier.testTag("voice_lab_screen")
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    AppleSegmentedPill(
                        label = cat,
                        isSelected = selectedCategory == cat,
                        onClick = { selectedCategory = cat }
                    )
                }
            }

            // Quick Preset Prompts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filteredPresets.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppleElevatedSurface)
                            .border(0.75.dp, AppleHairline, RoundedCornerShape(16.dp))
                            .clickable {
                                inputText = preset.phrase
                                onExecuteQuery(preset.phrase)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(preset.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppleLabelPrimary)
                            Text("\"${preset.phrase}\"", fontSize = 11.sp, color = AppleAccentCyan)
                        }
                    }
                }
            }

            // Main Scrollable Body
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
            ) {
                // Visual Pipeline Card
                if (currentTrace != null) {
                    item {
                        AppleInsetCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_pipeline_trace"),
                            cornerRadius = 22.dp,
                            backgroundColor = AppleCardBackground,
                            borderColor = AppleHairline
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🔬", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Laya Pipeline Inspector", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DexterOnBackground)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (currentTrace.isSuccess) DexterSuccess.copy(alpha = 0.2f) else DexterError.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            "${currentTrace.executionTimeMs} ms · ${if (currentTrace.isSuccess) "PASSED" else "UNROUTED"}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = if (currentTrace.isSuccess) DexterSuccess else DexterError,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                HorizontalDivider(color = DexterSurfaceVariant.copy(alpha = 0.6f))

                                // Step 1: Acoustic Tokens & Language
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("STEP 1: ACOUSTIC TOKENS & RECOGNITION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DexterPrimaryGlow)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(shape = RoundedCornerShape(6.dp), color = DexterSurfaceVariant) {
                                            Text(
                                                currentTrace.tokenTrace.languageTag,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 11.sp,
                                                color = DexterOnBackground,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Surface(shape = RoundedCornerShape(6.dp), color = DexterPrimary.copy(alpha = 0.2f)) {
                                            Text(
                                                currentTrace.tokenTrace.accentNote,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 11.sp,
                                                color = DexterPrimaryGlow
                                            )
                                        }
                                    }
                                    Text(
                                        "\"${currentTrace.tokenTrace.normalized}\"",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        color = DexterOnSurface
                                    )
                                }

                                HorizontalDivider(color = DexterSurfaceVariant.copy(alpha = 0.6f))

                                // Step 2: Laya Model Slot Extraction
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("STEP 2: LAYA SLOTS & INTENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DexterPrimaryGlow)
                                        Text("Intent: ${currentTrace.intent}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DexterPrimary)
                                    }

                                    if (currentTrace.slots.isEmpty()) {
                                        Text("No explicit slots extracted", fontSize = 12.sp, color = DexterOnSurfaceVariant)
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            currentTrace.slots.forEach { slot ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(DexterSurfaceVariant.copy(alpha = 0.7f))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(slot.key, fontSize = 12.sp, color = DexterOnSurfaceVariant, fontFamily = FontFamily.Monospace)
                                                    Text(slot.value, fontSize = 12.sp, color = DexterOnBackground, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = DexterSurfaceVariant.copy(alpha = 0.6f))

                                // Step 3: SQLite Database Routing
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("STEP 3: SQLITE DATABASE SEGMENT ROUTING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DexterPrimaryGlow)
                                    if (currentTrace.dbTraces.isEmpty()) {
                                        Text("No database mutation required for query", fontSize = 12.sp, color = DexterOnSurfaceVariant)
                                    } else {
                                        currentTrace.dbTraces.forEach { dbTrace ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = DexterBackground,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Segment #${dbTrace.segmentNumber}: ${dbTrace.segmentTitle}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DexterPrimary)
                                                        Text("TABLE: ${dbTrace.tableName}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = DexterOnSurfaceVariant)
                                                    }
                                                    Text(
                                                        "SQL ${dbTrace.operation}: " + dbTrace.rowData.entries.joinToString(", ") { "${it.key}=${it.value}" },
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = DexterOnSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = DexterSurfaceVariant.copy(alpha = 0.6f))

                                // Step 4: Assistant Speech Response
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("STEP 4: ASSISTANT VERBAL FEEDBACK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DexterPrimaryGlow)
                                    Text(
                                        currentTrace.responseText,
                                        fontSize = 13.sp,
                                        color = DexterOnBackground,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Recent Diagnostics History
                if (history.isNotEmpty()) {
                    item {
                        Text(
                            "EVALUATION HISTORY (${history.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DexterPrimaryGlow,
                            letterSpacing = 1.sp
                        )
                    }

                    items(history) { trace ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onExecuteQuery(trace.query) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DexterSurface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(trace.query, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = DexterOnBackground)
                                    Text("${trace.tokenTrace.languageTag} · ${trace.intent} → ${trace.segmentName}", fontSize = 11.sp, color = DexterOnSurfaceVariant)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (trace.isSuccess) DexterSuccess.copy(alpha = 0.2f) else DexterError.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        if (trace.isSuccess) "PASS" else "FAIL",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = if (trace.isSuccess) DexterSuccess else DexterError,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input Bar at Bottom
            Surface(
                color = DexterSurface,
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onVoiceClick,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DexterPrimary.copy(alpha = 0.2f))
                            .testTag("btn_lab_mic")
                    ) {
                        Text("🎙️", fontSize = 20.sp)
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Enter accented or multilingual command...", fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary,
                            unfocusedBorderColor = DexterSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_lab_query")
                    )

                    IconButton(
                        onClick = {
                            val q = inputText.trim()
                            if (q.isNotEmpty()) {
                                onExecuteQuery(q)
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DexterPrimary)
                            .testTag("btn_lab_send")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Execute Query", tint = DexterOnPrimary)
                    }
                }
            }
        }
    }
}
