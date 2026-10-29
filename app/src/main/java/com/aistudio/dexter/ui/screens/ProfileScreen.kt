package com.aistudio.dexter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aistudio.dexter.data.backup.BackupMetadata
import com.aistudio.dexter.data.preferences.*
import com.aistudio.dexter.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    preferences: UserPreferences,
    domainCounts: Map<String, Int>,
    totalRecords: Int,
    onBack: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateAvatar: (String) -> Unit,
    onUpdateLanguage: (String) -> Unit,
    onUpdateCurrency: (code: String, symbol: String) -> Unit,
    onUpdateTimeFormat: (is24Hour: Boolean) -> Unit,
    onUpdateDateFormat: (pattern: String) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onPurgeDomains: (domains: List<String>) -> Unit,
    onFactoryReset: () -> Unit,
    onOpenVoiceLab: () -> Unit = {}
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showDatePatternDialog by remember { mutableStateOf(false) }
    var showPurgeDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val currentLang = remember(preferences.languageCode) {
        SupportedPreferences.languages.find { it.code.equals(preferences.languageCode, ignoreCase = true) }
            ?: SupportedPreferences.languages.first()
    }

    val currentCurr = remember(preferences.currencyCode) {
        SupportedPreferences.currencies.find { it.code.equals(preferences.currencyCode, ignoreCase = true) }
            ?: SupportedPreferences.currencies.first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile & Preferences",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = DexterOnBackground,
                        modifier = Modifier.testTag("profile_title")
                    )
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
        modifier = Modifier.testTag("profile_screen")
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
        ) {
            // Identity Header Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_user_identity"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Circle
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(DexterSurfaceVariant)
                                .clickable { showAvatarDialog = true }
                                .testTag("btn_avatar_picker"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preferences.avatarEmoji,
                                fontSize = 32.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = preferences.userName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DexterOnBackground,
                                    modifier = Modifier.testTag("text_user_name")
                                )
                                IconButton(
                                    onClick = { showNameDialog = true },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .testTag("btn_edit_name")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Name",
                                        tint = DexterPrimaryGlow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Personal Assistant Profile · ${currentLang.nativeName}",
                                fontSize = 12.sp,
                                color = DexterOnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Localization & Formatting Section
            item {
                Text(
                    text = "REGIONAL & FORMATTING",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DexterPrimaryGlow,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Language Item
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showLanguageDialog = true }
                                .padding(vertical = 6.dp)
                                .testTag("btn_select_language"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(currentLang.flagEmoji, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Language", color = DexterOnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Text("${currentLang.englishName} (${currentLang.nativeName})", color = DexterOnSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Select", tint = DexterOnSurfaceVariant)
                        }

                        HorizontalDivider(color = DexterSurfaceVariant.copy(alpha = 0.5f))

                        // Currency Item
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showCurrencyDialog = true }
                                .padding(vertical = 6.dp)
                                .testTag("btn_select_currency"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(currentCurr.flagEmoji, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Global Currency", color = DexterOnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Text("${currentCurr.symbol} · ${currentCurr.name}", color = DexterOnSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Select", tint = DexterOnSurfaceVariant)
                        }

                        HorizontalDivider(color = DexterSurfaceVariant.copy(alpha = 0.5f))

                        // Time Format Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("24-Hour Time Format", color = DexterOnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(
                                    if (preferences.is24HourFormat) "20:30 (24h format enabled)" else "08:30 PM (12h AM/PM format)",
                                    color = DexterOnSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = preferences.is24HourFormat,
                                onCheckedChange = { onUpdateTimeFormat(it) },
                                modifier = Modifier.testTag("switch_time_format"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DexterOnPrimary,
                                    checkedTrackColor = DexterPrimary,
                                    uncheckedThumbColor = DexterOnSurfaceVariant,
                                    uncheckedTrackColor = DexterSurfaceVariant
                                )
                            )
                        }

                        HorizontalDivider(color = DexterSurfaceVariant.copy(alpha = 0.5f))

                        // Date Format Dropdown
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showDatePatternDialog = true }
                                .padding(vertical = 6.dp)
                                .testTag("btn_select_date_format"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Date Format", color = DexterOnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(
                                    SupportedPreferences.formatDate(System.currentTimeMillis(), preferences.dateFormatPattern) +
                                            " (${preferences.dateFormatPattern})",
                                    color = DexterOnSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Select", tint = DexterOnSurfaceVariant)
                        }

                        HorizontalDivider(color = DexterSurfaceVariant.copy(alpha = 0.5f))

                        // Voice Diagnostics & Laya Pipeline Lab
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenVoiceLab() }
                                .padding(vertical = 6.dp)
                                .testTag("btn_open_voice_lab"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔬", fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Voice & Laya Pipeline Lab", color = DexterPrimaryGlow, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Test accents, Chinese, Russian, Hindi & Laya slots", color = DexterOnSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Open Lab", tint = DexterPrimaryGlow)
                        }
                    }
                }
            }

            // Data Migration & Backup Section
            item {
                Text(
                    text = "DATA BACKUP & MIGRATION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DexterPrimaryGlow,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📦", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Cross-Device Migration Bundle", color = DexterOnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("Total $totalRecords records across 16 segments", color = DexterOnSurfaceVariant, fontSize = 12.sp)
                            }
                        }

                        Text(
                            "Export your entire database into an encrypted, portable JSON file. You can easily share it to Google Drive, Telegram, or import it to another device to restore all tasks, finance, and goals.",
                            color = DexterOnSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onExportBackup,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_export_backup"),
                                colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export File", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onImportBackup,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_import_backup"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DexterPrimaryGlow),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DexterPrimaryGlow)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import File", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Data Deletion & Factory Reset (Danger Zone)
            item {
                Text(
                    text = "DATA MANAGEMENT & PRIVACY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DexterError,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Selective Purge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showPurgeDialog = true }
                                .padding(vertical = 4.dp)
                                .testTag("btn_selective_purge"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Selective Domain Purge", color = DexterOnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("Selectively wipe individual segments (e.g., clear only expenses, tasks, or diary)", color = DexterOnSurfaceVariant, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Purge", tint = DexterOnSurfaceVariant)
                        }

                        HorizontalDivider(color = DexterSurfaceVariant.copy(alpha = 0.5f))

                        // Factory Reset
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showResetDialog = true }
                                .padding(vertical = 4.dp)
                                .testTag("btn_factory_reset"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Factory Reset Dexter", color = DexterError, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Permanently delete all 16 database segments and reset preferences to default", color = DexterOnSurfaceVariant, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.Delete, contentDescription = "Reset", tint = DexterError)
                        }
                    }
                }
            }
        }
    }

    // Edit Name Dialog
    if (showNameDialog) {
        var tempName by remember { mutableStateOf(preferences.userName) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Edit Display Name", color = DexterOnBackground, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DexterOnBackground,
                        unfocusedTextColor = DexterOnBackground,
                        focusedBorderColor = DexterPrimary,
                        unfocusedBorderColor = DexterSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_user_name")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateName(tempName)
                        showNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                    modifier = Modifier.testTag("btn_save_name")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel", color = DexterOnSurfaceVariant)
                }
            },
            containerColor = DexterSurface
        )
    }

    // Avatar Picker Dialog
    if (showAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            title = { Text("Choose Avatar", color = DexterOnBackground, fontWeight = FontWeight.Bold) },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(SupportedPreferences.avatarEmojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (preferences.avatarEmoji == emoji) DexterPrimary.copy(alpha = 0.3f) else DexterSurfaceVariant)
                                .clickable {
                                    onUpdateAvatar(emoji)
                                    showAvatarDialog = false
                                }
                                .testTag("avatar_option_$emoji"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 26.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAvatarDialog = false }) {
                    Text("Close", color = DexterOnSurfaceVariant)
                }
            },
            containerColor = DexterSurface
        )
    }

    // Searchable Language Dialog
    if (showLanguageDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val filtered = remember(searchQuery) {
            SupportedPreferences.languages.filter {
                it.englishName.contains(searchQuery, ignoreCase = true) ||
                        it.nativeName.contains(searchQuery, ignoreCase = true) ||
                        it.code.contains(searchQuery, ignoreCase = true)
            }
        }

        Dialog(onDismissRequest = { showLanguageDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .testTag("dialog_language")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Select Language",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DexterOnBackground
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search language or code...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DexterOnSurfaceVariant) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary,
                            unfocusedBorderColor = DexterSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_language_search")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered) { lang ->
                            val isSelected = preferences.languageCode.equals(lang.code, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) DexterPrimary.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable {
                                        onUpdateLanguage(lang.code)
                                        showLanguageDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("lang_item_${lang.code}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(lang.flagEmoji, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(lang.englishName, color = DexterOnBackground, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text(lang.nativeName, color = DexterOnSurfaceVariant, fontSize = 12.sp)
                                    }
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = DexterPrimaryGlow)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showLanguageDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", color = DexterOnSurfaceVariant)
                    }
                }
            }
        }
    }

    // Searchable Currency Dialog
    if (showCurrencyDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val filtered = remember(searchQuery) {
            SupportedPreferences.currencies.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.code.contains(searchQuery, ignoreCase = true) ||
                        it.symbol.contains(searchQuery, ignoreCase = true)
            }
        }

        Dialog(onDismissRequest = { showCurrencyDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DexterSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .testTag("dialog_currency")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Select Global Currency",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DexterOnBackground
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search currency e.g. CNY, RUB, USD...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DexterOnSurfaceVariant) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary,
                            unfocusedBorderColor = DexterSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_currency_search")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered) { curr ->
                            val isSelected = preferences.currencyCode.equals(curr.code, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) DexterPrimary.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable {
                                        onUpdateCurrency(curr.code, curr.symbol)
                                        showCurrencyDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("curr_item_${curr.code}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(curr.flagEmoji, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("${curr.symbol}  ${curr.code}", color = DexterOnBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(curr.name, color = DexterOnSurfaceVariant, fontSize = 12.sp)
                                    }
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = DexterPrimaryGlow)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showCurrencyDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", color = DexterOnSurfaceVariant)
                    }
                }
            }
        }
    }

    // Date Pattern Dialog
    if (showDatePatternDialog) {
        AlertDialog(
            onDismissRequest = { showDatePatternDialog = false },
            title = { Text("Select Date Format", color = DexterOnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SupportedPreferences.datePatterns.forEach { (pattern, label) ->
                        val isSelected = preferences.dateFormatPattern == pattern
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) DexterPrimary.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    onUpdateDateFormat(pattern)
                                    showDatePatternDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("date_pattern_$pattern"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(label, color = DexterOnBackground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    SupportedPreferences.formatDate(System.currentTimeMillis(), pattern),
                                    color = DexterOnSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = DexterPrimaryGlow)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDatePatternDialog = false }) {
                    Text("Cancel", color = DexterOnSurfaceVariant)
                }
            },
            containerColor = DexterSurface
        )
    }

    // Selective Domain Purge Dialog
    if (showPurgeDialog) {
        val selectedDomains = remember { mutableStateMapOf<String, Boolean>() }
        domainCounts.keys.forEach { domain ->
            if (!selectedDomains.containsKey(domain)) {
                selectedDomains[domain] = false
            }
        }

        AlertDialog(
            onDismissRequest = { showPurgeDialog = false },
            title = { Text("Selective Domain Purge", color = DexterOnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Check the database segments you wish to wipe permanently. Other domains will not be touched.",
                        fontSize = 12.sp,
                        color = DexterOnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(domainCounts.toList()) { (domain, count) ->
                            val isChecked = selectedDomains[domain] == true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedDomains[domain] = !isChecked }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { selectedDomains[domain] = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = DexterError,
                                            checkmarkColor = DexterOnPrimary
                                        )
                                    )
                                    Text(domain, color = DexterOnBackground, fontSize = 13.sp)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (count > 0) DexterSurfaceVariant else Color.Transparent
                                ) {
                                    Text(
                                        "$count items",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        color = if (count > 0) DexterOnSurface else DexterOnSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val countSelected = selectedDomains.count { it.value }
                Button(
                    onClick = {
                        val toPurge = selectedDomains.filter { it.value }.keys.toList()
                        if (toPurge.isNotEmpty()) {
                            onPurgeDomains(toPurge)
                        }
                        showPurgeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterError),
                    enabled = countSelected > 0,
                    modifier = Modifier.testTag("btn_confirm_purge")
                ) {
                    Text("Purge $countSelected Selected")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeDialog = false }) {
                    Text("Cancel", color = DexterOnSurfaceVariant)
                }
            },
            containerColor = DexterSurface
        )
    }

    // Factory Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = DexterError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Factory Reset Dexter?", color = DexterError, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "This action is permanent and cannot be undone.\n\nAll 16 database segments (tasks, transactions, journals, goals, habits, alarms) will be wiped, and user preferences will be restored to factory default settings.",
                    color = DexterOnBackground,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onFactoryReset()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterError),
                    modifier = Modifier.testTag("btn_confirm_factory_reset")
                ) {
                    Text("Yes, Reset Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = DexterOnSurfaceVariant)
                }
            },
            containerColor = DexterSurface
        )
    }
}
