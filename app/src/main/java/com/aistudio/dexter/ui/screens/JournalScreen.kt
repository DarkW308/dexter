package com.aistudio.dexter.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.dexter.R
import com.aistudio.dexter.data.model.JournalEntry
import com.aistudio.dexter.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    notes: List<JournalEntry>,
    totalEntries: Int,
    writingStreakDays: Int,
    totalWordsWritten: Int,
    onBack: () -> Unit,
    onVoiceClick: () -> Unit,
    onAddEntry: (content: String, title: String, mood: String, category: String) -> Unit,
    onDeleteEntry: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMoodFilter by remember { mutableStateOf("All") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // Editor dialog state
    var entryTitleInput by remember { mutableStateOf("") }
    var entryContentInput by remember { mutableStateOf("") }
    var entryMoodInput by remember { mutableStateOf("Reflective") }
    var entryCategoryInput by remember { mutableStateOf("Reflections") }

    val moodOptions = listOf(
        "Grateful" to "😊 Grateful",
        "Focused" to "⚡ Focused",
        "Calm" to "🌿 Calm",
        "Reflective" to "💭 Reflective",
        "Energized" to "🚀 Energized",
        "Tired" to "😴 Tired"
    )

    val categoryOptions = listOf(
        "Reflections",
        "Gratitude",
        "Daily Log",
        "Ideas",
        "Milestones",
        "Work"
    )

    // Filter notes
    val filteredNotes = remember(notes, searchQuery, selectedMoodFilter, selectedCategoryFilter) {
        notes.filter { entry ->
            val matchesMood = selectedMoodFilter == "All" || entry.mood.equals(selectedMoodFilter, ignoreCase = true)
            val matchesCategory = selectedCategoryFilter == "All" || entry.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    entry.title.contains(searchQuery, ignoreCase = true) ||
                    entry.content.contains(searchQuery, ignoreCase = true) ||
                    entry.category.contains(searchQuery, ignoreCase = true) ||
                    entry.mood.contains(searchQuery, ignoreCase = true)
            matchesMood && matchesCategory && matchesSearch
        }
    }

    // Group notes by date for diary format
    val groupedByDate = remember(filteredNotes) {
        filteredNotes.groupBy { entry ->
            val date = Date(entry.createdAt * 1000)
            val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val entryStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(date)
            when (entryStr) {
                todayStr -> "Today · " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(date)
                (todayStr.toIntOrNull()?.minus(1)?.toString() ?: "") -> "Yesterday · " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(date)
                else -> SimpleDateFormat("EEEE · d MMMM yyyy", Locale.getDefault()).format(date)
            }
        }
    }

    Scaffold(
        containerColor = DexterBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MY PERSONAL DIARY",
                            color = DexterOnBackground,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Private Reflections & Daily Thoughts",
                            color = DexterOnSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("journal_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DexterOnBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onVoiceClick,
                        modifier = Modifier.testTag("journal_voice_button")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_mic),
                            contentDescription = "Voice Dictate Diary Entry",
                            tint = DexterPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DexterSurface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = DexterPrimary,
                contentColor = DexterOnPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Write Entry") },
                text = { Text("Write in Diary", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("write_diary_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Diary Overview & Streak Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("diary_overview_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DAILY REFLECTION & DIARY STATS",
                                color = DexterPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DexterPrimary.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = "🔥", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$writingStreakDays Day Streak",
                                        color = DexterPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DiaryMetricItem(
                                value = "$totalEntries",
                                label = "Total Entries",
                                modifier = Modifier.weight(1f)
                            )
                            DiaryMetricItem(
                                value = "$totalWordsWritten",
                                label = "Words Written",
                                modifier = Modifier.weight(1f)
                            )
                            val dominantMood = remember(notes) {
                                if (notes.isNotEmpty()) notes.groupBy { it.mood }.maxByOrNull { it.value.size }?.key ?: "Reflective" else "Calm"
                            }
                            DiaryMetricItem(
                                value = dominantMood,
                                label = "Dominant Mood",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Search & Filter Section
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search diary memories, thoughts, reflections...", fontSize = 13.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = DexterOnSurfaceVariant) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = DexterOnSurfaceVariant)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary,
                            unfocusedBorderColor = DexterSurfaceVariant,
                            focusedContainerColor = DexterSurface,
                            unfocusedContainerColor = DexterSurface
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("journal_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mood Filter Row
                    Text(text = "Filter by Mood", color = DexterOnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedMoodFilter == "All",
                                onClick = { selectedMoodFilter = "All" },
                                label = { Text("All Moods", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DexterPrimary,
                                    selectedLabelColor = DexterOnPrimary,
                                    containerColor = DexterSurface,
                                    labelColor = DexterOnSurface
                                )
                            )
                        }
                        items(moodOptions) { (key, label) ->
                            FilterChip(
                                selected = selectedMoodFilter == key,
                                onClick = { selectedMoodFilter = if (selectedMoodFilter == key) "All" else key },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DexterPrimary,
                                    selectedLabelColor = DexterOnPrimary,
                                    containerColor = DexterSurface,
                                    labelColor = DexterOnSurface
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Filter Row
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == "All",
                                onClick = { selectedCategoryFilter = "All" },
                                label = { Text("All Topics", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DexterPrimary,
                                    selectedLabelColor = DexterOnPrimary,
                                    containerColor = DexterSurface,
                                    labelColor = DexterOnSurface
                                )
                            )
                        }
                        items(categoryOptions) { cat ->
                            FilterChip(
                                selected = selectedCategoryFilter == cat,
                                onClick = { selectedCategoryFilter = if (selectedCategoryFilter == cat) "All" else cat },
                                label = { Text("#$cat", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DexterPrimary,
                                    selectedLabelColor = DexterOnPrimary,
                                    containerColor = DexterSurface,
                                    labelColor = DexterOnSurface
                                )
                            )
                        }
                    }
                }
            }

            // Diary Timeline Entries
            if (filteredNotes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "📖", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Your Diary is Ready",
                                color = DexterOnBackground,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedMoodFilter != "All")
                                    "No entries matched your search. Tap clear to see all memories."
                                else
                                    "Tap \"Write in Diary\" or say \"Take a note...\" to write your first reflection.",
                                color = DexterOnSurfaceVariant,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                groupedByDate.forEach { (dateGroup, groupEntries) ->
                    item {
                        Surface(
                            color = DexterBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateGroup.uppercase(),
                                    color = DexterPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "${groupEntries.size} reflections",
                                    color = DexterOnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    items(groupEntries, key = { it.id }) { entry ->
                        DiaryPageCard(
                            entry = entry,
                            onDelete = { onDeleteEntry(entry.id) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal: Write in Diary
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "New Diary Entry",
                    color = DexterOnBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mood Picker
                    Text(text = "How are you feeling?", color = DexterOnSurfaceVariant, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(moodOptions) { (key, label) ->
                            FilterChip(
                                selected = entryMoodInput == key,
                                onClick = { entryMoodInput = key },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DexterPrimary,
                                    selectedLabelColor = DexterOnPrimary,
                                    containerColor = DexterSurfaceVariant,
                                    labelColor = DexterOnSurface
                                )
                            )
                        }
                    }

                    // Entry Title Input
                    OutlinedTextField(
                        value = entryTitleInput,
                        onValueChange = { entryTitleInput = it },
                        label = { Text("Entry Title (Optional)") },
                        placeholder = { Text("e.g. Breakthroughs, Evening Walk") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary,
                            unfocusedBorderColor = DexterSurfaceVariant
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_diary_title_input")
                    )

                    // Category Picker
                    Text(text = "Category / Topic", color = DexterOnSurfaceVariant, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categoryOptions) { cat ->
                            FilterChip(
                                selected = entryCategoryInput == cat,
                                onClick = { entryCategoryInput = cat },
                                label = { Text("#$cat", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DexterPrimary,
                                    selectedLabelColor = DexterOnPrimary,
                                    containerColor = DexterSurfaceVariant,
                                    labelColor = DexterOnSurface
                                )
                            )
                        }
                    }

                    // Diary Thoughts Content Multiline Area
                    OutlinedTextField(
                        value = entryContentInput,
                        onValueChange = { entryContentInput = it },
                        label = { Text("Dear Diary...") },
                        placeholder = { Text("Write your thoughts, reflections, milestones, ideas...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary,
                            unfocusedBorderColor = DexterSurfaceVariant
                        ),
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_diary_content_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (entryContentInput.isNotBlank()) {
                            onAddEntry(
                                entryContentInput.trim(),
                                entryTitleInput.trim(),
                                entryMoodInput,
                                entryCategoryInput
                            )
                            entryTitleInput = ""
                            entryContentInput = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DexterPrimary,
                        contentColor = DexterOnPrimary
                    ),
                    modifier = Modifier.testTag("dialog_save_diary_button")
                ) {
                    Text("Save to Diary")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = DexterOnSurfaceVariant)
                }
            },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun DiaryMetricItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            color = DexterOnBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = DexterOnSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
fun DiaryPageCard(
    entry: JournalEntry,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val moodColor = when (entry.mood.lowercase()) {
        "grateful" -> Color(0xFFFFB300)
        "focused" -> DexterPrimary
        "calm" -> DexterSuccess
        "reflective" -> Color(0xFFBA68C8)
        "energized" -> Color(0xFFFF7043)
        "tired" -> Color(0xFF78909C)
        else -> DexterPrimary
    }

    val moodEmoji = when (entry.mood.lowercase()) {
        "grateful" -> "😊"
        "focused" -> "⚡"
        "calm" -> "🌿"
        "reflective" -> "💭"
        "energized" -> "🚀"
        "tired" -> "😴"
        else -> "📝"
    }

    val timeStr = remember(entry.createdAt) {
        val date = Date(entry.createdAt * 1000)
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
    }

    val wordsCount = remember(entry.content) {
        entry.content.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("diary_card_${entry.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DexterSurface)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Diary left color accent ribbon corresponding to mood
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(moodColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header: Time, Mood badge, Category, and Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = moodColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$moodEmoji ${entry.mood}",
                                color = moodColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DexterSurfaceVariant
                        ) {
                            Text(
                                text = "#${entry.category}",
                                color = DexterOnSurfaceVariant,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeStr,
                            color = DexterOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete entry",
                                tint = DexterOnSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title (if present or extracted)
                if (entry.title.isNotBlank()) {
                    Text(
                        text = entry.title,
                        color = DexterOnBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Diary Thought Body (generous line spacing like diary ruled paper)
                Text(
                    text = entry.content,
                    color = DexterOnSurface,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DexterSurfaceVariant.copy(alpha = 0.5f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Footer: Source and word count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (entry.source.equals("VOICE", ignoreCase = true)) "🎙️ Voice Captured" else "✍️ Written Entry",
                            color = DexterPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "$wordsCount words",
                        color = DexterOnSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
