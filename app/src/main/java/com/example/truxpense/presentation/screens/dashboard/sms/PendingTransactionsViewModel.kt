package com.example.truxpense.presentation.screens.dashboard.sms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.expense.ExpenseRepository
import com.example.truxpense.data.repository.expense.Transaction
import com.example.truxpense.data.repository.sms.PendingTransactionRepository
import com.example.truxpense.data.sms.model.Category
import com.example.truxpense.data.sms.model.ParsedTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PendingTransactionsViewModel @Inject constructor(
    private val repository: PendingTransactionRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    val pendingTransactions: StateFlow<List<ParsedTransaction>> =
        repository.pendingTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingCount: StateFlow<Int> =
        repository.pendingCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun confirm(id: String, category: Category? = null) {
        viewModelScope.launch {
            repository.confirm(id, category)
            val tx = pendingTransactions.value.firstOrNull { it.id == id } ?: return@launch
            val isCredit = tx.type == com.example.truxpense.data.sms.model.TxnType.CREDIT
            // Credits saved as positive, debits as negative
            val signedAmount = if (isCredit) kotlin.math.abs(tx.amount) else -kotlin.math.abs(tx.amount)
            expenseRepository.addExpense(
                Transaction(
                    id = tx.id,
                    amount = signedAmount,
                    category = category?.name?.lowercase()?.replaceFirstChar { it.uppercaseChar() }
                        ?: tx.category.name.lowercase().replaceFirstChar { it.uppercaseChar() },
                    paymentMethod = tx.bank.ifBlank { "UPI" },
                    merchant = tx.merchant ?: "Unknown",
                    notes = if (isCredit) "UPI Credit" else "",
                    timestamp = tx.timestamp,
                    source = "sms",
                )
            )
        }
    }

    fun reject(id: String) {
        viewModelScope.launch { repository.reject(id) }
    }

    fun confirmAll() {
        viewModelScope.launch {
            pendingTransactions.value.forEach { tx ->
                repository.confirm(tx.id)
                val isCredit = tx.type == com.example.truxpense.data.sms.model.TxnType.CREDIT
                val signedAmount = if (isCredit) kotlin.math.abs(tx.amount) else -kotlin.math.abs(tx.amount)
                expenseRepository.addExpense(
                    Transaction(
                        id = tx.id,
                        amount = signedAmount,
                        category = tx.category.name.lowercase().replaceFirstChar { it.uppercaseChar() },
                        paymentMethod = tx.bank.ifBlank { "UPI" },
                        merchant = tx.merchant ?: "Unknown",
                        notes = if (isCredit) "UPI Credit" else "",
                        timestamp = tx.timestamp,
                        source = "sms",
                    )
                )
            }
        }
    }

    fun rejectAll() {
        viewModelScope.launch {
            pendingTransactions.value.forEach { repository.reject(it.id) }
        }
    }
}