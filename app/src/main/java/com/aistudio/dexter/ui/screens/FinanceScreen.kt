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
import com.aistudio.dexter.data.model.FinanceTransaction
import com.aistudio.dexter.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // Dialog state
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
                    tx.referenceId.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesCategory && matchesSearch
        }
    }

    // Group transactions by date for statement presentation
    val groupedByDate = remember(filteredTransactions) {
        filteredTransactions.groupBy { tx ->
            val date = Date(tx.createdAt * 1000)
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
                            text = "BANK STATEMENT & LEDGER",
                            color = DexterOnBackground,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Dexter Secure Wallet · Single-Device Sandbox",
                            color = DexterOnSurfaceVariant,
                            fontSize = 12.sp
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
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("bank_statement_summary_card"),
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

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Net Closing Balance",
                            color = DexterOnSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "₹${formatCurrency(netBalanceMinor)}",
                            color = DexterOnBackground,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = DexterSurfaceVariant, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

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
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(DexterSuccess.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "↑", color = DexterSuccess, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Total Inflow (CR)",
                                        color = DexterOnSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+ ₹${formatCurrency(totalCreditsMinor)}",
                                    color = DexterSuccess,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Total Debits
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(DexterError.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "↓", color = DexterError, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Total Outflow (DR)",
                                        color = DexterOnSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "- ₹${formatCurrency(totalDebitsMinor)}",
                                    color = DexterError,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Today's Outflow: ₹${formatCurrency(todaySpentMinor)}",
                                color = DexterOnSurface,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Month's Outflow: ₹${formatCurrency(monthSpentMinor)}",
                                color = DexterOnSurfaceVariant,
                                fontSize = 12.sp
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
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "EXPENSE BREAKDOWN BY CATEGORY",
                                color = DexterPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            val topCategories = categoryBreakdown.entries.take(4)
                            topCategories.forEach { (cat, amt) ->
                                val pct = if (totalDebitsMinor > 0) ((amt.toFloat() / totalDebitsMinor) * 100).toInt() else 0
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = cat, color = DexterOnSurface, fontSize = 13.sp)
                                        Text(
                                            text = "₹${amt / 100} ($pct%)",
                                            color = DexterOnSurfaceVariant,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { if (totalDebitsMinor > 0) (amt.toFloat() / totalDebitsMinor).coerceIn(0f, 1f) else 0f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
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
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search description, category, TXN ref...", fontSize = 13.sp) },
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // DR / CR / ALL Filter Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatementFilterPill(
                            label = "All Statements (${transactions.size})",
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
                                label = { Text("All Categories", fontSize = 12.sp) },
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
                                label = { Text(cat, fontSize = 12.sp) },
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

            // Statement Ledger Section
            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "💳", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Statement Records Found",
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
            } else {
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
                            onDelete = { onDeleteTransaction(tx.id) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
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
    modifier: Modifier = Modifier
) {
    val isDebit = transaction.isDebit
    val timeStr = remember(transaction.createdAt) {
        val date = Date(transaction.createdAt * 1000)
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
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

private fun formatCurrency(minor: Long): String {
    val rupees = minor / 100.0
    return String.format(Locale.getDefault(), "%,.2f", rupees)
}
