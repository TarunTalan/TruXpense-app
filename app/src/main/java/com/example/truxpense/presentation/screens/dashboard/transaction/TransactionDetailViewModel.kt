package com.example.truxpense.presentation.screens.dashboard.transaction


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.expense.ExpenseRepository
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


    fun loadTransaction(transactionId: String) {
        viewModelScope.launch {
            // `transactions` is a Flow<List<Transaction>> — read the latest list
            // and then search the list for the id.
            val txList = repo.transactions.firstOrNull() ?: emptyList()
            val match = txList.firstOrNull { it.id == transactionId }

            _detail.value = if (match != null) {
                // Map repository Transaction → TransactionDetail
                val cal = Calendar.getInstance().apply { timeInMillis = match.timestamp }
                val dateFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
                TransactionDetail(
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
                // Preview / not-yet-persisted stub
                stubDetail(transactionId)
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
            val txList = repo.transactions.firstOrNull() ?: emptyList()
            val tx = txList.firstOrNull { it.id == current.id } ?: return@launch
            repo.updateExpense(tx.copy(notes = _notes.value.trim()))
            _detail.value = current.copy(notes = _notes.value.trim())
        }
    }

    fun toggleNotes() {
        _notesExpanded.value = !_notesExpanded.value
    }

    fun deleteTransaction() {
        viewModelScope.launch {
            val id = _detail.value?.id ?: return@launch
            repo.deleteExpense(id)
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