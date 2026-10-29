package com.aistudio.dexter.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
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
import com.aistudio.dexter.data.model.FinanceTransaction
import com.aistudio.dexter.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FinanceSortField(val label: String) {
    DATE("Date"),
    AMOUNT("Amount"),
    DESCRIPTION("Narration"),
    CATEGORY("Category")
}

enum class TableDensity(val label: String, val rowHeightDp: Int) {
    COMPACT("Compact", 38),
    REGULAR("Normal", 46)
}

enum class FinanceViewMode {
    TABLE,
    CARDS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    transactions: List<FinanceTransaction>,
    netBalanceMinor: Long,
    totalCreditsMinor: Long,
    totalDebitsMinor: Long,
    todaySpentMinor: Long,
    monthSpentMinor: Long,
    categoryBreakdown: Map<String, Long>,
    onBack: () -> Unit,
    onVoiceClick: () -> Unit,
    onRecordTransaction: (amountMinor: Long, description: String, type: String, category: String, paymentMode: String) -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFilterTab by remember { mutableStateOf("ALL") } // ALL, DEBIT, CREDIT
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // Excel Table State
    var sortField by remember { mutableStateOf(FinanceSortField.DATE) }
    var sortAscending by remember { mutableStateOf(false) } // Default newest first
    var tableDensity by remember { mutableStateOf(TableDensity.COMPACT) }
    var viewMode by remember { mutableStateOf(FinanceViewMode.TABLE) }
    var selectedTxForDetails by remember { mutableStateOf<FinanceTransaction?>(null) }
    var txToDeleteConfirm by remember { mutableStateOf<FinanceTransaction?>(null) }

    // Dialog state for adding entries
    var txType by remember { mutableStateOf("DEBIT") }
    var amountInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("Food & Dining") }
    var modeInput by remember { mutableStateOf("UPI") }

    val categoriesList = listOf(
        "Food & Dining",
        "Groceries",
        "Utilities",
        "Salary",
        "Consulting",
        "Shopping",
        "Healthcare",
        "Transport",
        "General"
    )

    val paymentModesList = listOf("UPI", "Cash", "Card", "Net Banking")

    // Filter transactions
    val filteredTransactions = remember(transactions, selectedFilterTab, searchQuery, selectedCategoryFilter) {
        transactions.filter { tx ->
            val matchesTab = when (selectedFilterTab) {
                "DEBIT" -> tx.isDebit
                "CREDIT" -> !tx.isDebit
                else -> true
            }
            val matchesCategory = selectedCategoryFilter == "All" || tx.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    tx.description.contains(searchQuery, ignoreCase = true) ||
                    tx.category.contains(searchQuery, ignoreCase = true) ||
                    tx.referenceId.contains(searchQuery, ignoreCase = true) ||
                    tx.paymentMode.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesCategory && matchesSearch
        }
    }

    // Sort transactions
    val sortedTransactions = remember(filteredTransactions, sortField, sortAscending) {
        when (sortField) {
            FinanceSortField.DATE -> if (sortAscending) filteredTransactions.sortedBy { it.createdAt } else filteredTransactions.sortedByDescending { it.createdAt }
            FinanceSortField.AMOUNT -> if (sortAscending) filteredTransactions.sortedBy { it.amountMinor } else filteredTransactions.sortedByDescending { it.amountMinor }
            FinanceSortField.DESCRIPTION -> if (sortAscending) filteredTransactions.sortedBy { it.description.lowercase() } else filteredTransactions.sortedByDescending { it.description.lowercase() }
            FinanceSortField.CATEGORY -> if (sortAscending) filteredTransactions.sortedBy { it.category.lowercase() } else filteredTransactions.sortedByDescending { it.category.lowercase() }
        }
    }

    // Dynamic sum metrics for visible / filtered transactions
    val visibleDebitsMinor = remember(sortedTransactions) {
        sortedTransactions.filter { it.isDebit }.sumOf { it.amountMinor }
    }
    val visibleCreditsMinor = remember(sortedTransactions) {
        sortedTransactions.filter { !it.isDebit }.sumOf { it.amountMinor }
    }
    val visibleNetMinor = remember(visibleCreditsMinor, visibleDebitsMinor) {
        visibleCreditsMinor - visibleDebitsMinor
    }

    // Group transactions by date for statement presentation in cards mode
    val groupedByDate = remember(sortedTransactions) {
        sortedTransactions.groupBy { tx ->
            val date = parseTransactionDate(tx.createdAt)
            val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val entryStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(date)
            when (entryStr) {
                todayStr -> "Today · " + SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(date)
                (todayStr.toIntOrNull()?.minus(1)?.toString() ?: "") -> "Yesterday · " + SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(date)
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
                            text = "FINANCIAL LEDGER & STATEMENTS",
                            color = DexterOnBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Dexter Secure Wallet · Excel-Dense Ledger View",
                            color = DexterOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("finance_back_button")
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
                        modifier = Modifier.testTag("finance_voice_button")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_mic),
                            contentDescription = "Voice Log",
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
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Record Entry") },
                text = { Text("Record Entry", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("record_transaction_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Statement Summary Banner Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("bank_statement_summary_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DexterSurface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OFFICIAL STATEMENT SUMMARY",
                                color = DexterPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (netBalanceMinor >= 0) DexterSuccess.copy(alpha = 0.15f) else DexterError.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (netBalanceMinor >= 0) "SURPLUS" else "DEFICIT",
                                    color = if (netBalanceMinor >= 0) DexterSuccess else DexterError,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Net Closing Balance",
                            color = DexterOnSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "₹${formatCurrency(netBalanceMinor)}",
                            color = DexterOnBackground,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = DexterSurfaceVariant, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Credits and Debits Columns
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Total Credits
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(DexterSuccess.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "↑", color = DexterSuccess, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Total Inflow (CR)",
                                        color = DexterOnSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "+ ₹${formatCurrency(totalCreditsMinor)}",
                                    color = DexterSuccess,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Total Debits
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(DexterError.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "↓", color = DexterError, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Total Outflow (DR)",
                                        color = DexterOnSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "- ₹${formatCurrency(totalDebitsMinor)}",
                                    color = DexterError,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Today's Outflow: ₹${formatCurrency(todaySpentMinor)}",
                                color = DexterOnSurface,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Month's Outflow: ₹${formatCurrency(monthSpentMinor)}",
                                color = DexterOnSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Spending by Category Breakdown Mini Card
            if (categoryBreakdown.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DexterSurface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "EXPENSE BREAKDOWN BY CATEGORY",
                                color = DexterPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val topCategories = categoryBreakdown.entries.take(4)
                            topCategories.forEach { (cat, amt) ->
                                val pct = if (totalDebitsMinor > 0) ((amt.toFloat() / totalDebitsMinor) * 100).toInt() else 0
                                Column(modifier = Modifier.padding(vertical = 3.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = cat, color = DexterOnSurface, fontSize = 12.sp)
                                        Text(
                                            text = "₹${amt / 100} ($pct%)",
                                            color = DexterOnSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    LinearProgressIndicator(
                                        progress = { if (totalDebitsMinor > 0) (amt.toFloat() / totalDebitsMinor).coerceIn(0f, 1f) else 0f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = DexterPrimary,
                                        trackColor = DexterSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Filter Tabs & Search
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter ledger: payee, category, ref, mode...", fontSize = 13.sp) },
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
                            .testTag("finance_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // DR / CR / ALL Filter Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatementFilterPill(
                            label = "All (${transactions.size})",
                            isSelected = selectedFilterTab == "ALL",
                            onClick = { selectedFilterTab = "ALL" },
                            modifier = Modifier.weight(1f)
                        )
                        StatementFilterPill(
                            label = "Debits [DR]",
                            isSelected = selectedFilterTab == "DEBIT",
                            onClick = { selectedFilterTab = "DEBIT" },
                            modifier = Modifier.weight(1f)
                        )
                        StatementFilterPill(
                            label = "Credits [CR]",
                            isSelected = selectedFilterTab == "CREDIT",
                            onClick = { selectedFilterTab = "CREDIT" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Pill Filters
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == "All",
                                onClick = { selectedCategoryFilter = "All" },
                                label = { Text("All Categories", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DexterPrimary,
                                    selectedLabelColor = DexterOnPrimary,
                                    containerColor = DexterSurface,
                                    labelColor = DexterOnSurface
                                )
                            )
                        }
                        items(categoriesList) { cat ->
                            FilterChip(
                                selected = selectedCategoryFilter == cat,
                                onClick = { selectedCategoryFilter = if (selectedCategoryFilter == cat) "All" else cat },
                                label = { Text(cat, fontSize = 11.sp) },
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

            // EXCEL SPREADSHEET TABLE CONTROLS & FORMULA BAR
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    // Excel Sheet Header Toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1B5E20) // Excel Green
                            ) {
                                Text(
                                    text = "XLSX",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TRANSACTION_LEDGER",
                                color = DexterOnBackground,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DexterSurfaceVariant
                            ) {
                                Text(
                                    text = "${sortedTransactions.size} rows",
                                    color = DexterPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Right Controls: Density & View Mode toggles
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Density Toggle
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DexterSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, DexterSurfaceVariant),
                                modifier = Modifier.clickable {
                                    tableDensity = if (tableDensity == TableDensity.COMPACT) TableDensity.REGULAR else TableDensity.COMPACT
                                }
                            ) {
                                Text(
                                    text = if (tableDensity == TableDensity.COMPACT) "Compact" else "Normal",
                                    color = DexterOnSurface,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // View Mode Toggle (Table vs Cards)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (viewMode == FinanceViewMode.TABLE) DexterPrimary.copy(alpha = 0.2f) else DexterSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (viewMode == FinanceViewMode.TABLE) DexterPrimary else DexterSurfaceVariant
                                ),
                                modifier = Modifier.clickable {
                                    viewMode = if (viewMode == FinanceViewMode.TABLE) FinanceViewMode.CARDS else FinanceViewMode.TABLE
                                }
                            ) {
                                Text(
                                    text = if (viewMode == FinanceViewMode.TABLE) "📊 Table" else "📑 Cards",
                                    color = if (viewMode == FinanceViewMode.TABLE) DexterPrimary else DexterOnSurfaceVariant,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Authentic Excel Formula Bar (fx)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF2E3250), RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF17192A)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "fx",
                                color = Color(0xFF69F0AE),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            VerticalDivider(
                                color = Color(0xFF2E3250),
                                modifier = Modifier
                                    .height(14.dp)
                                    .padding(end = 8.dp)
                            )
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "=SUM(DR): -₹${formatCurrency(visibleDebitsMinor)}",
                                    color = DexterError,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "=SUM(CR): +₹${formatCurrency(visibleCreditsMinor)}",
                                    color = DexterSuccess,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "NET: ₹${formatCurrency(visibleNetMinor)}",
                                    color = DexterOnBackground,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (viewMode == FinanceViewMode.TABLE && sortedTransactions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⟵ Pan left/right to view all columns · Tap row for details ⟶",
                                color = DexterOnSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Sorted by: ${sortField.label} ${if (sortAscending) "▲" else "▼"}",
                                color = DexterPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // PRIMARY TRANSACTION PRESENTATION: EXCEL TABLE vs CARDS
            if (sortedTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "📊", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Ledger Entries Found",
                                color = DexterOnBackground,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedCategoryFilter != "All")
                                    "Try clearing search filters or logging a transaction."
                                else
                                    "Record an expense or credit to begin your offline bank statement.",
                                color = DexterOnSurfaceVariant,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (viewMode == FinanceViewMode.TABLE) {
                // EXCEL DATA TABLE VIEW (SPACE-EFFICIENT & READABLE)
                item {
                    val horizontalScrollState = rememberScrollState()
                    val totalTableWidth = 716.dp

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("excel_transaction_table"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF181A2D)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2E4A))
                    ) {
                        Column {
                            // Horizontal scrollable table container
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(horizontalScrollState)
                            ) {
                                Column(modifier = Modifier.width(totalTableWidth)) {
                                    // 1. Table Header Row (Excel Column Headers)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .background(Color(0xFF131524))
                                            .border(width = 0.5.dp, color = Color(0xFF2C3150)),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Column 0: Index #
                                        ExcelHeaderCell(
                                            text = "#",
                                            width = 38.dp,
                                            isSorted = false,
                                            isAscending = false,
                                            onClick = null
                                        )
                                        // Column 1: Date & Time
                                        ExcelHeaderCell(
                                            text = "DATE",
                                            width = 96.dp,
                                            isSorted = sortField == FinanceSortField.DATE,
                                            isAscending = sortAscending,
                                            onClick = {
                                                if (sortField == FinanceSortField.DATE) {
                                                    sortAscending = !sortAscending
                                                } else {
                                                    sortField = FinanceSortField.DATE
                                                    sortAscending = false
                                                }
                                            }
                                        )
                                        // Column 2: Narration / Description
                                        ExcelHeaderCell(
                                            text = "DESCRIPTION / PAYEE",
                                            width = 180.dp,
                                            isSorted = sortField == FinanceSortField.DESCRIPTION,
                                            isAscending = sortAscending,
                                            onClick = {
                                                if (sortField == FinanceSortField.DESCRIPTION) {
                                                    sortAscending = !sortAscending
                                                } else {
                                                    sortField = FinanceSortField.DESCRIPTION
                                                    sortAscending = true
                                                }
                                            }
                                        )
                                        // Column 3: Category
                                        ExcelHeaderCell(
                                            text = "CATEGORY",
                                            width = 110.dp,
                                            isSorted = sortField == FinanceSortField.CATEGORY,
                                            isAscending = sortAscending,
                                            onClick = {
                                                if (sortField == FinanceSortField.CATEGORY) {
                                                    sortAscending = !sortAscending
                                                } else {
                                                    sortField = FinanceSortField.CATEGORY
                                                    sortAscending = true
                                                }
                                            }
                                        )
                                        // Column 4: Payment Mode
                                        ExcelHeaderCell(
                                            text = "MODE",
                                            width = 68.dp,
                                            isSorted = false,
                                            isAscending = false,
                                            onClick = null
                                        )
                                        // Column 5: Type (DR/CR)
                                        ExcelHeaderCell(
                                            text = "TYPE",
                                            width = 58.dp,
                                            isSorted = false,
                                            isAscending = false,
                                            onClick = null
                                        )
                                        // Column 6: Amount in ₹
                                        ExcelHeaderCell(
                                            text = "AMOUNT (₹)",
                                            width = 118.dp,
                                            textAlign = TextAlign.End,
                                            isSorted = sortField == FinanceSortField.AMOUNT,
                                            isAscending = sortAscending,
                                            onClick = {
                                                if (sortField == FinanceSortField.AMOUNT) {
                                                    sortAscending = !sortAscending
                                                } else {
                                                    sortField = FinanceSortField.AMOUNT
                                                    sortAscending = false
                                                }
                                            }
                                        )
                                        // Column 7: Actions
                                        ExcelHeaderCell(
                                            text = "ACT",
                                            width = 48.dp,
                                            isSorted = false,
                                            isAscending = false,
                                            onClick = null
                                        )
                                    }

                                    // 2. Data Rows (Compact & Zebra-Striped)
                                    sortedTransactions.forEachIndexed { index, tx ->
                                        val isEven = index % 2 == 0
                                        val rowBg = if (isEven) Color(0xFF1E2137) else Color(0xFF17192C)
                                        val isSelected = selectedTxForDetails?.id == tx.id

                                        val rowModifier = Modifier
                                            .fillMaxWidth()
                                            .height(tableDensity.rowHeightDp.dp)
                                            .background(if (isSelected) Color(0xFF293256) else rowBg)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) DexterPrimary else Color(0xFF282C48)
                                            )
                                            .clickable { selectedTxForDetails = tx }
                                            .testTag("excel_row_${tx.id}")

                                        Row(
                                            modifier = rowModifier,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Col 0: Index Number
                                            Box(
                                                modifier = Modifier
                                                    .width(38.dp)
                                                    .fillMaxHeight()
                                                    .border(width = 0.5.dp, color = Color(0xFF282C48)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    color = DexterOnSurfaceVariant.copy(alpha = 0.8f),
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            // Col 1: Date & Time
                                            val dateFormatted = remember(tx.createdAt) {
                                                val date = parseTransactionDate(tx.createdAt)
                                                SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(date)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .width(96.dp)
                                                    .fillMaxHeight()
                                                    .padding(horizontal = 6.dp)
                                                    .border(width = 0.5.dp, color = Color(0xFF282C48)),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Text(
                                                    text = dateFormatted,
                                                    color = DexterOnSurface,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    maxLines = 1
                                                )
                                            }

                                            // Col 2: Narration / Description + Ref ID subtitle
                                            Box(
                                                modifier = Modifier
                                                    .width(180.dp)
                                                    .fillMaxHeight()
                                                    .padding(horizontal = 8.dp)
                                                    .border(width = 0.5.dp, color = Color(0xFF282C48)),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Column(verticalArrangement = Arrangement.Center) {
                                                    Text(
                                                        text = tx.description.ifBlank { "Untitled TXN" },
                                                        color = DexterOnBackground,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (tableDensity == TableDensity.REGULAR) {
                                                        Text(
                                                            text = tx.formattedRef,
                                                            color = DexterOnSurfaceVariant,
                                                            fontSize = 9.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }

                                            // Col 3: Category Badge
                                            Box(
                                                modifier = Modifier
                                                    .width(110.dp)
                                                    .fillMaxHeight()
                                                    .padding(horizontal = 6.dp)
                                                    .border(width = 0.5.dp, color = Color(0xFF282C48)),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = DexterSurfaceVariant.copy(alpha = 0.8f)
                                                ) {
                                                    Text(
                                                        text = tx.category,
                                                        color = DexterPrimary,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            // Col 4: Payment Mode
                                            Box(
                                                modifier = Modifier
                                                    .width(68.dp)
                                                    .fillMaxHeight()
                                                    .border(width = 0.5.dp, color = Color(0xFF282C48)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = tx.paymentMode,
                                                    color = DexterOnSurfaceVariant,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    maxLines = 1
                                                )
                                            }

                                            // Col 5: Type Pill (DR / CR)
                                            Box(
                                                modifier = Modifier
                                                    .width(58.dp)
                                                    .fillMaxHeight()
                                                    .border(width = 0.5.dp, color = Color(0xFF282C48)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (tx.isDebit) DexterError.copy(alpha = 0.15f) else DexterSuccess.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = if (tx.isDebit) "DR" else "CR",
                                                        color = if (tx.isDebit) DexterError else DexterSuccess,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontFamily = FontFamily.Monospace,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            // Col 6: Monospace Amount in ₹
                                            Box(
                                                modifier = Modifier
                                                    .width(118.dp)
                                                    .fillMaxHeight()
                                                    .padding(horizontal = 8.dp)
                                                    .border(width = 0.5.dp, color = Color(0xFF282C48)),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                Text(
                                                    text = "${if (tx.isDebit) "- " else "+ "}₹${formatCurrency(tx.amountMinor)}",
                                                    color = if (tx.isDebit) DexterError else DexterSuccess,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    textAlign = TextAlign.End,
                                                    maxLines = 1
                                                )
                                            }

                                            // Col 7: Quick Actions (Delete)
                                            Box(
                                                modifier = Modifier
                                                    .width(48.dp)
                                                    .fillMaxHeight()
                                                    .border(width = 0.5.dp, color = Color(0xFF282C48)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                IconButton(
                                                    onClick = { txToDeleteConfirm = tx },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete entry",
                                                        tint = DexterOnSurfaceVariant.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Excel Worksheet Status Bar (Bottom of Table)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF131524))
                                    .border(width = 0.5.dp, color = Color(0xFF2C3150))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF69F0AE))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "READY",
                                        color = Color(0xFF69F0AE),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Count: ${sortedTransactions.size}",
                                        color = DexterOnSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "DR: -₹${formatCurrency(visibleDebitsMinor)}",
                                        color = DexterError,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "CR: +₹${formatCurrency(visibleCreditsMinor)}",
                                        color = DexterSuccess,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // ALTERNATIVE VIEW: DETAILED CARDS VIEW (GROUPED BY DATE)
                groupedByDate.forEach { (dateGroup, groupTxs) ->
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
                                    text = "${groupTxs.size} entries",
                                    color = DexterOnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    items(groupTxs, key = { it.id }) { tx ->
                        BankStatementRow(
                            transaction = tx,
                            onDelete = { txToDeleteConfirm = tx },
                            onClick = { selectedTxForDetails = tx },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal: Transaction Detail Inspection Dialog (Tapped on Row)
    if (selectedTxForDetails != null) {
        val tx = selectedTxForDetails!!
        val isDebit = tx.isDebit
        val fullDate = remember(tx.createdAt) {
            val date = parseTransactionDate(tx.createdAt)
            SimpleDateFormat("EEEE, d MMMM yyyy · h:mm:ss a", Locale.getDefault()).format(date)
        }

        AlertDialog(
            onDismissRequest = { selectedTxForDetails = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ledger Record Details",
                            color = DexterOnBackground,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tx.formattedRef,
                            color = DexterPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDebit) DexterError.copy(alpha = 0.15f) else DexterSuccess.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isDebit) "DEBIT (DR)" else "CREDIT (CR)",
                            color = if (isDebit) DexterError else DexterSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Big Amount Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E2138)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Transaction Value",
                                color = DexterOnSurfaceVariant,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${if (isDebit) "- " else "+ "}₹${formatCurrency(tx.amountMinor)}",
                                color = if (isDebit) DexterError else DexterSuccess,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Key-Value Grid
                    DetailRow(label = "Narration / Description", value = tx.description.ifBlank { "None" })
                    DetailRow(label = "Category", value = tx.category)
                    DetailRow(label = "Payment Mode", value = tx.paymentMode)
                    DetailRow(label = "Timestamp", value = fullDate)
                    DetailRow(label = "Actor / Origin", value = tx.actorType)
                    if (tx.notes.isNotBlank()) {
                        DetailRow(label = "Notes", value = tx.notes)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDel = tx
                        selectedTxForDetails = null
                        txToDeleteConfirm = toDel
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DexterError.copy(alpha = 0.2f),
                        contentColor = DexterError
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Entry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTxForDetails = null }) {
                    Text("Close", color = DexterOnSurfaceVariant)
                }
            },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Modal: Confirm Delete Dialog
    if (txToDeleteConfirm != null) {
        val tx = txToDeleteConfirm!!
        AlertDialog(
            onDismissRequest = { txToDeleteConfirm = null },
            title = {
                Text(
                    text = "Confirm Entry Deletion",
                    color = DexterOnBackground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this record?\n\n• ${tx.description}\n• ${if (tx.isDebit) "Expense -₹" else "Income +₹"}${formatCurrency(tx.amountMinor)}\n\nThis will permanently update your bank balance.",
                    color = DexterOnSurface,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTransaction(tx.id)
                        txToDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DexterError,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDeleteConfirm = null }) {
                    Text("Cancel", color = DexterOnSurfaceVariant)
                }
            },
            containerColor = DexterSurface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Modal: Record Transaction (Debit / Credit)
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Record Statement Entry",
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
                    // Type Toggle (Debit vs Credit)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { txType = "DEBIT" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (txType == "DEBIT") DexterError else DexterSurface,
                                contentColor = DexterOnBackground
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Expense (DR)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { txType = "CREDIT" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (txType == "CREDIT") DexterSuccess else DexterSurface,
                                contentColor = if (txType == "CREDIT") DexterOnPrimary else DexterOnBackground
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Income (CR)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Amount Input
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Amount in Rupees (₹)") },
                        placeholder = { Text("e.g. 450") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary,
                            unfocusedBorderColor = DexterSurfaceVariant
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_expense_amount_input")
                    )

                    // Narration / Description
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Narration / Description") },
                        placeholder = { Text("e.g. Swiggy Lunch, Client Project Fee") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground,
                            focusedBorderColor = DexterPrimary,
                            unfocusedBorderColor = DexterSurfaceVariant
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_expense_desc_input")
                    )

                    // Category selection
                    Text(text = "Category", color = DexterOnSurfaceVariant, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categoriesList) { cat ->
                            FilterChip(
                                selected = categoryInput == cat,
                                onClick = { categoryInput = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DexterPrimary,
                                    selectedLabelColor = DexterOnPrimary,
                                    containerColor = DexterSurfaceVariant,
                                    labelColor = DexterOnSurface
                                )
                            )
                        }
                    }

                    // Payment Mode selection
                    Text(text = "Payment Mode", color = DexterOnSurfaceVariant, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        paymentModesList.forEach { mode ->
                            FilterChip(
                                selected = modeInput == mode,
                                onClick = { modeInput = mode },
                                label = { Text(mode, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DexterPrimary,
                                    selectedLabelColor = DexterOnPrimary,
                                    containerColor = DexterSurfaceVariant,
                                    labelColor = DexterOnSurface
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountVal = amountInput.toDoubleOrNull() ?: 0.0
                        if (amountVal > 0 && descInput.isNotBlank()) {
                            val minor = (amountVal * 100).toLong()
                            onRecordTransaction(minor, descInput.trim(), txType, categoryInput, modeInput)
                            amountInput = ""
                            descInput = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DexterPrimary,
                        contentColor = DexterOnPrimary
                    ),
                    modifier = Modifier.testTag("dialog_confirm_transaction_button")
                ) {
                    Text("Post to Statement")
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
private fun ExcelHeaderCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    textAlign: TextAlign = TextAlign.Start,
    isSorted: Boolean,
    isAscending: Boolean,
    onClick: (() -> Unit)?
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else Modifier

    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .border(width = 0.5.dp, color = Color(0xFF2C3150))
            .background(if (isSorted) DexterPrimary.copy(alpha = 0.15f) else Color.Transparent)
            .then(clickableModifier)
            .padding(horizontal = 6.dp),
        contentAlignment = when (textAlign) {
            TextAlign.End -> Alignment.CenterEnd
            TextAlign.Center -> Alignment.Center
            else -> Alignment.CenterStart
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (textAlign == TextAlign.End) Arrangement.End else Arrangement.Start
        ) {
            Text(
                text = text,
                color = if (isSorted) DexterPrimary else DexterOnSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.4.sp,
                textAlign = textAlign,
                maxLines = 1
            )
            if (isSorted) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isAscending) "▲" else "▼",
                    color = DexterPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            color = DexterOnSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = DexterOnBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = DexterSurfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
    }
}

@Composable
private fun StatementFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) DexterPrimary else DexterSurface,
        modifier = modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (isSelected) DexterOnPrimary else DexterOnSurface,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun BankStatementRow(
    transaction: FinanceTransaction,
    onDelete: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDebit = transaction.isDebit
    val timeStr = remember(transaction.createdAt) {
        val date = parseTransactionDate(transaction.createdAt)
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("statement_row_${transaction.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DexterSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // DR / CR indicator badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDebit) DexterError.copy(alpha = 0.15f) else DexterSuccess.copy(alpha = 0.15f),
                modifier = Modifier.size(width = 38.dp, height = 38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isDebit) "DR" else "CR",
                        color = if (isDebit) DexterError else DexterSuccess,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Narration & Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    color = DexterOnBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transaction.formattedRef,
                        color = DexterOnSurfaceVariant,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(text = "·", color = DexterOnSurfaceVariant, fontSize = 11.sp)
                    Text(
                        text = transaction.category,
                        color = DexterPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(text = "·", color = DexterOnSurfaceVariant, fontSize = 11.sp)
                    Text(
                        text = transaction.paymentMode,
                        color = DexterOnSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Text(text = "·", color = DexterOnSurfaceVariant, fontSize = 10.sp)
                    Text(
                        text = timeStr,
                        color = DexterOnSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount Column (styled like bank ledger)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isDebit) "- " else "+ "}₹${formatCurrency(transaction.amountMinor)}",
                    color = if (isDebit) DexterOnBackground else DexterSuccess,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete entry",
                        tint = DexterOnSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

private fun parseTransactionDate(rawTimestamp: Long): Date {
    val millis = if (rawTimestamp in 1..9_999_999_999L) rawTimestamp * 1000L else rawTimestamp
    return Date(millis)
}

private fun formatCurrency(minor: Long): String {
    val rupees = minor / 100.0
    return String.format(Locale.getDefault(), "%,.2f", rupees)
}
