package com.example.truxpense.presentation.screens.dashboard.addexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.dashboard.ExpenseRepository
import com.example.truxpense.data.repository.dashboard.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository,
) : ViewModel() {

    // ── 1. Raw inputs ─────────────────────────────────────────────────────────

    private val _rawAmount = MutableStateFlow("")
    val rawAmount: StateFlow<String> = _rawAmount.asStateFlow()

    private val _merchant = MutableStateFlow("")
    val merchant: StateFlow<String> = _merchant.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    // ── 2. Derived display ────────────────────────────────────────────────────

    val formattedAmount: StateFlow<String> = _rawAmount.map { raw ->
        val num = raw.toDoubleOrNull()
        if (num == null || raw.isEmpty()) "₹0"
        else "₹${"%,.0f".format(num)}"
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "₹0")

    // ── 3. Selections (dropdown open/close now handled by components) ─────────

    val categories = listOf("Food", "Transport", "Shopping", "Bills", "Health", "Entertainment", "Groceries", "Other")
    val accountList = listOf("HDFC Bank", "SBI", "ICICI Bank", "Axis Bank", "Cash", "UPI")

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedAccount = MutableStateFlow<String?>(null)
    val selectedAccount: StateFlow<String?> = _selectedAccount.asStateFlow()

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    // ── 4. Save state ─────────────────────────────────────────────────────────

    private val _saveComplete = MutableSharedFlow<Unit>(replay = 0)
    val saveComplete: SharedFlow<Unit> = _saveComplete

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // ── 5. Form validity ──────────────────────────────────────────────────────

    val isFormValid: StateFlow<Boolean> = combine(_rawAmount, _selectedCategory) { amt, cat ->
        amt.isNotBlank() && amt.toDoubleOrNull() != null && cat != null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── Events ────────────────────────────────────────────────────────────────

    fun setRawAmount(v: String) {
        _rawAmount.value = v.filter { it.isDigit() || it == '.' }
    }

    fun setMerchant(v: String) {
        _merchant.value = v
    }

    fun setNotes(v: String) {
        _notes.value = v
    }

    fun selectCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun selectAccount(acc: String) {
        _selectedAccount.value = acc
    }

    fun setDate(date: String) {
        _selectedDate.value = date
    }

    fun saveExpense() {
        val amount = _rawAmount.value.toDoubleOrNull() ?: return
        val category = _selectedCategory.value ?: return
        val paymentMethod = _selectedAccount.value ?: "UPI"
        val merchantName = _merchant.value.trim().ifBlank { category }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.addExpense(
                    Transaction(
                        amount = amount,
                        category = category,
                        paymentMethod = paymentMethod,
                        merchant = merchantName,
                    )
                )
                _saveComplete.emit(Unit)
                resetForm()
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun resetForm() {
        _rawAmount.value = ""
        _merchant.value = ""
        _notes.value = ""
        _selectedCategory.value = null
        _selectedAccount.value = null
        _selectedDate.value = null
    }
}