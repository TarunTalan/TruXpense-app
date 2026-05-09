package com.example.truxpense.presentation.screens.dashboard.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.income.Income
import com.example.truxpense.data.repository.income.IncomeRepository
import com.example.truxpense.presentation.utils.DateTimeUtils
import com.example.truxpense.presentation.utils.AppCategories
import com.example.truxpense.presentation.utils.sanitizeAmountInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AddIncomeViewModel @Inject constructor(
    private val repository: IncomeRepository,
) : ViewModel() {

    /** Category-style list shown in the picker grid. */
    val sourceOptions = listOf(
        "Salary", "Freelance", "Business", "Investment",
        "Gift", "Rental", "Refund", "Other",
        AppCategories.CUSTOM,
    )

    /** Payment method options — same as expense for consistency. */
    val accountList = listOf("Bank Transfer", "Cash", "UPI", "Card")

    // ── Raw inputs ────────────────────────────────────────────────────────────

    private val _rawAmount = MutableStateFlow("")
    val rawAmount: StateFlow<String> = _rawAmount.asStateFlow()

    /** Free-text source name (e.g. company name, client name). */
    private val _sourceName = MutableStateFlow("")
    val sourceName: StateFlow<String> = _sourceName.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    // ── Category / source picker ──────────────────────────────────────────────

    private val _selectedSource = MutableStateFlow<String?>(null)
    val selectedSource: StateFlow<String?> = _selectedSource.asStateFlow()

    // ── Payment method ────────────────────────────────────────────────────────

    private val _selectedAccount = MutableStateFlow<String?>(null)
    val selectedAccount: StateFlow<String?> = _selectedAccount.asStateFlow()

    // ── Date / time ───────────────────────────────────────────────────────────

    private val _selectedDate = MutableStateFlow(
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())
    )
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedTime = MutableStateFlow(
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    )
    val selectedTime: StateFlow<String> = _selectedTime.asStateFlow()

    // ── Derived ───────────────────────────────────────────────────────────────

    val isFormValid: StateFlow<Boolean> = combine(_rawAmount, _selectedSource) { amt, src ->
        amt.isNotBlank() && amt.toDoubleOrNull() != null && (amt.toDoubleOrNull() ?: 0.0) > 0 && src != null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _saveComplete = MutableSharedFlow<Unit>(replay = 0)
    val saveComplete: SharedFlow<Unit> = _saveComplete

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // ── Setters ───────────────────────────────────────────────────────────────

    fun setRawAmount(v: String) { _rawAmount.value = sanitizeAmountInput(v) }
    fun setSourceName(v: String) { _sourceName.value = v }
    fun selectSource(v: String) {
        _selectedSource.value = v
        // Don't overwrite free-text when Custom is tapped — user will type their own name
        if (v != AppCategories.CUSTOM) _sourceName.value = v
    }
    fun selectAccount(v: String) { _selectedAccount.value = v }
    fun setNotes(v: String) { _notes.value = v }
    fun setDate(d: String) { _selectedDate.value = d }
    fun setTime(t: String) { _selectedTime.value = t }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun saveIncome() {
        val amount = _rawAmount.value.toDoubleOrNull() ?: return
        val source = _selectedSource.value ?: return
        val displaySource = _sourceName.value.trim().ifBlank { source }
        val timestamp = DateTimeUtils.parseDateTimeToMillis(_selectedDate.value, _selectedTime.value)
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.addIncome(
                    Income(
                        amount = amount,
                        source = displaySource,
                        notes = _notes.value.trim(),
                        timestamp = timestamp,
                        paymentMethod = _selectedAccount.value ?: "",
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
        _sourceName.value = ""
        _notes.value = ""
        _selectedSource.value = null
        _selectedAccount.value = null
        _selectedDate.value = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())
        _selectedTime.value = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }
}

