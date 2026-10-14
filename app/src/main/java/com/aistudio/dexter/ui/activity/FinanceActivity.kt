package com.aistudio.dexter.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.aistudio.dexter.DexterApp
import com.aistudio.dexter.data.model.FinanceTransaction
import com.aistudio.dexter.data.repository.DexterRepository
import com.aistudio.dexter.engine.DexterActionExecutor
import com.aistudio.dexter.engine.DexterSpeechManager
import com.aistudio.dexter.ui.screens.FinanceScreen
import com.aistudio.dexter.ui.theme.DexterTheme
import com.aistudio.dexter.ui.view.VoiceInteractionDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FinanceActivity : ComponentActivity() {

    private lateinit var repository: DexterRepository
    private lateinit var speechManager: DexterSpeechManager
    private lateinit var actionExecutor: DexterActionExecutor

    private val transactionsState = MutableStateFlow<List<FinanceTransaction>>(emptyList())
    private val balanceState = MutableStateFlow(0L)
    private val creditsState = MutableStateFlow(0L)
    private val debitsState = MutableStateFlow(0L)
    private val todaySpentState = MutableStateFlow(0L)
    private val monthSpentState = MutableStateFlow(0L)
    private val categoryBreakdownState = MutableStateFlow<Map<String, Long>>(emptyMap())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = (application as DexterApp).repository
        speechManager = DexterSpeechManager(this)
        actionExecutor = DexterActionExecutor(repository, this)

        lifecycleScope.launch {
            DexterRepository.dataRevision.collectLatest {
                loadFinanceData()
            }
        }

        setContent {
            DexterTheme {
                val transactions = transactionsState.value
                val netBalance = balanceState.value
                val totalCredits = creditsState.value
                val totalDebits = debitsState.value
                val todaySpent = todaySpentState.value
                val monthSpent = monthSpentState.value
                val categoryBreakdown = categoryBreakdownState.value

                FinanceScreen(
                    transactions = transactions,
                    netBalanceMinor = netBalance,
                    totalCreditsMinor = totalCredits,
                    totalDebitsMinor = totalDebits,
                    todaySpentMinor = todaySpent,
                    monthSpentMinor = monthSpent,
                    categoryBreakdown = categoryBreakdown,
                    onBack = { finish() },
                    onVoiceClick = { openVoiceDialog() },
                    onRecordTransaction = { amountMinor, description, type, category, paymentMode ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            repository.logTransaction(
                                amountMinor = amountMinor,
                                desc = description,
                                type = type,
                                category = category,
                                paymentMode = paymentMode
                            )
                            loadFinanceData()
                        }
                    },
                    onDeleteTransaction = { id ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            repository.deleteTransaction(id)
                            loadFinanceData()
                        }
                    }
                )
            }
        }

        loadFinanceData()
    }

    private fun loadFinanceData() {
        lifecycleScope.launch {
            val txs = withContext(Dispatchers.IO) { repository.listTransactions(limit = 100) }
            val (balance, credits, debits) = withContext(Dispatchers.IO) { repository.getFinanceStatement() }
            val (todaySpent, monthSpent) = withContext(Dispatchers.IO) { repository.summarizeExpenses() }
            val breakdown = withContext(Dispatchers.IO) { repository.getFinanceCategoryBreakdown() }

            transactionsState.value = txs
            balanceState.value = balance
            creditsState.value = credits
            debitsState.value = debits
            todaySpentState.value = todaySpent
            monthSpentState.value = monthSpent
            categoryBreakdownState.value = breakdown
        }
    }

    private fun openVoiceDialog() {
        val dialog = VoiceInteractionDialog(
            context = this,
            speechManager = speechManager,
            actionExecutor = actionExecutor,
            onActionCompleted = {
                loadFinanceData()
            }
        )
        dialog.show()
    }
}
