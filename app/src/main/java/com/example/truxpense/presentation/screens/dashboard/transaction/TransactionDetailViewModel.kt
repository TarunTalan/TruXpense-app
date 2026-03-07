package com.example.truxpense.presentation.screens.dashboard.transaction


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.expense.ExpenseRepository
import com.example.truxpense.data.repository.income.IncomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val repo: ExpenseRepository,
    private val incomeRepo: IncomeRepository,
) : ViewModel() {


    private val _detail = MutableStateFlow<TransactionDetail?>(null)
    val detail: StateFlow<TransactionDetail?> = _detail.asStateFlow()

    /** Expose the loaded id so the caller can build the edit route. */
    val currentTransactionId: String?
        get() = _detail.value?.id

    /** Notes typed by the user (edit-in-place on this screen). */
    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    /** Whether the notes input is expanded. */
    private val _notesExpanded = MutableStateFlow(false)
    val notesExpanded: StateFlow<Boolean> = _notesExpanded.asStateFlow()

    /** Non-null once a delete action is confirmed — parent nav pops on this. */
    private val _deleteComplete = MutableStateFlow(false)
    val deleteComplete: StateFlow<Boolean> = _deleteComplete.asStateFlow()

    /** Exposed so the screen can route Edit → EditIncomeScreen vs EditExpenseScreen. */
    private val _isIncome = MutableStateFlow(false)
    val isIncome: StateFlow<Boolean> = _isIncome.asStateFlow()

    /** Track whether the currently loaded entry is an income record. */
    private var isIncomeEntry = false


    fun loadTransaction(transactionId: String) {
        viewModelScope.launch {
            val txList = repo.transactions.firstOrNull() ?: emptyList()
            val match = txList.firstOrNull { it.id == transactionId }

            if (match != null) {
                isIncomeEntry = false
                _isIncome.value = false
                val cal = Calendar.getInstance().apply { timeInMillis = match.timestamp }
                val dateFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
                _detail.value = TransactionDetail(
                    id = match.id,
                    merchant = match.merchant,
                    category = match.category,
                    account = match.paymentMethod.ifBlank { "UPI" },
                    date = dateFmt.format(cal.time),
                    time = timeFmt.format(cal.time),
                    amount = match.amount,
                    type = if (match.amount < 0) "Expense" else "Income",
                    source = if (match.source == "sms") "Detected from SMS" else "Added manually",
                    notes = match.notes,
                )
            } else {
                // Not an expense — check income repository
                val incomeList = incomeRepo.allIncome.firstOrNull() ?: emptyList()
                val income = incomeList.firstOrNull { it.id == transactionId }
                if (income != null) {
                    isIncomeEntry = true
                    _isIncome.value = true
                    val cal = Calendar.getInstance().apply { timeInMillis = income.timestamp }
                    val dateFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
                    _detail.value = TransactionDetail(
                        id = income.id,
                        merchant = income.source,
                        category = income.source,
                        account = income.paymentMethod.ifBlank { "—" },
                        date = dateFmt.format(cal.time),
                        time = timeFmt.format(cal.time),
                        amount = income.amount,          // positive = income
                        type = "Income",
                        source = "Added manually",
                        notes = income.notes,
                    )
                } else {
                    isIncomeEntry = false
                    _isIncome.value = false
                    _detail.value = stubDetail(transactionId)
                }
            }

            _notes.value = _detail.value?.notes ?: ""
        }
    }

    fun setNotes(text: String) {
        _notes.value = text
    }

    fun saveNotes() {
        viewModelScope.launch {
            val current = _detail.value ?: return@launch
            if (isIncomeEntry) {
                val income = incomeRepo.getById(current.id) ?: return@launch
                incomeRepo.updateIncome(income.copy(notes = _notes.value.trim()))
            } else {
                val txList = repo.transactions.firstOrNull() ?: emptyList()
                val tx = txList.firstOrNull { it.id == current.id } ?: return@launch
                repo.updateExpense(tx.copy(notes = _notes.value.trim()))
            }
            _detail.value = current.copy(notes = _notes.value.trim())
        }
    }

    fun toggleNotes() {
        _notesExpanded.value = !_notesExpanded.value
    }

    fun deleteTransaction() {
        viewModelScope.launch {
            val id = _detail.value?.id ?: return@launch
            if (isIncomeEntry) {
                incomeRepo.deleteIncome(id)
            } else {
                repo.deleteExpense(id)
            }
            _deleteComplete.value = true
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun stubDetail(id: String) = TransactionDetail(
        id = id,
        merchant = "Swiggy",
        category = "Food",
        account = "HDFC Bank",
        date = "12 Feb 2026",
        time = "8:45 PM",
        amount = -450.0,
        type = "Expense",
        source = "Added manually",
        notes = "",
    )
}