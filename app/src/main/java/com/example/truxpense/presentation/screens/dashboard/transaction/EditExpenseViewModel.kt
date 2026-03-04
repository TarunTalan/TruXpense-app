package com.example.truxpense.presentation.screens.dashboard.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.expense.ExpenseRepository
import com.example.truxpense.data.repository.expense.Transaction
import com.example.truxpense.presentation.utils.AppCategories
import com.example.truxpense.presentation.utils.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EditExpenseViewModel @Inject constructor(
    private val repo: ExpenseRepository,
) : ViewModel() {

    // ── Form state ────────────────────────────────────────────────────────────

    private val _rawAmount = MutableStateFlow("")
    val rawAmount: StateFlow<String> = _rawAmount.asStateFlow()

    private val _merchant = MutableStateFlow("")
    val merchant: StateFlow<String> = _merchant.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedAccount = MutableStateFlow<String?>(null)
    val selectedAccount: StateFlow<String?> = _selectedAccount.asStateFlow()

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    private val _selectedTime = MutableStateFlow<String?>(null)
    val selectedTime: StateFlow<String?> = _selectedTime.asStateFlow()

    /** Original timestamp (preserved on save so the date grouping stays correct
     *  unless the user explicitly picks a new date, in which case we update it). */
    private var originalTimestamp: Long = System.currentTimeMillis()
    private var transactionId: String = ""

    // ── Static option lists (same as AddExpenseViewModel) ─────────────────────

    val categories = AppCategories.all
    val accountList = listOf("HDFC Bank", "SBI", "ICICI Bank", "Axis Bank", "Cash", "UPI")

    // ── Derived validity ──────────────────────────────────────────────────────

    val isFormValid: StateFlow<Boolean> = combine(_rawAmount, _selectedCategory) { amt, cat ->
        amt.isNotBlank() && amt.toDoubleOrNull() != null && cat != null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── Save / update completion signal ───────────────────────────────────────

    private val _updateComplete = MutableSharedFlow<Unit>(replay = 0)
    val updateComplete: SharedFlow<Unit> = _updateComplete

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // ── Load ──────────────────────────────────────────────────────────────────

    /**
     * Call once with the transactionId when the screen is first composed.
     * Pre-fills all form fields from the stored transaction.
     */
    fun loadTransaction(id: String) {
        viewModelScope.launch {
            val txList = repo.transactions.firstOrNull() ?: emptyList()
            val tx = txList.firstOrNull { it.id == id } ?: return@launch

            transactionId = tx.id
            originalTimestamp = tx.timestamp

            _rawAmount.value = "%.0f".format(kotlin.math.abs(tx.amount))
            _merchant.value = tx.merchant
            _notes.value = tx.notes
            _selectedCategory.value = tx.category
            _selectedAccount.value = tx.paymentMethod.ifBlank { null }

            // Format stored timestamp as a display date and time
            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            _selectedDate.value = SimpleDateFormat("MMM d", Locale.getDefault()).format(cal.time)
            _selectedTime.value = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
        }
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    fun setRawAmount(v: String) { _rawAmount.value = v.filter { it.isDigit() || it == '.' } }
    fun setMerchant(v: String) { _merchant.value = v }
    fun setNotes(v: String) { _notes.value = v }
    fun selectCategory(cat: String) { _selectedCategory.value = cat }
    fun selectAccount(acc: String) { _selectedAccount.value = acc }
    fun setDate(date: String) { _selectedDate.value = date }
    fun setTime(time: String) { _selectedTime.value = time }

    fun saveChanges() {
        val amount = _rawAmount.value.toDoubleOrNull() ?: return
        val category = _selectedCategory.value ?: return
        val paymentMethod = _selectedAccount.value ?: "UPI"
        val merchantName = _merchant.value.trim().ifBlank { "Anonymous" }
        val timestamp = _selectedDate.value?.let { date ->
            DateTimeUtils.parseDateTimeToMillis(date, _selectedTime.value ?: "12:00 AM")
        } ?: originalTimestamp

        viewModelScope.launch {
            _isSaving.value = true
            try {
                repo.updateExpense(
                    Transaction(
                        id = transactionId,
                        amount = amount,
                        category = category,
                        paymentMethod = paymentMethod,
                        merchant = merchantName,
                        notes = _notes.value.trim(),
                        timestamp = timestamp,
                    )
                )
                _updateComplete.emit(Unit)
            } finally {
                _isSaving.value = false
            }
        }
    }
}