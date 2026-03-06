package com.example.truxpense.presentation.screens.dashboard.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.income.IncomeRepository
import com.example.truxpense.presentation.utils.DateTimeUtils
import com.example.truxpense.presentation.utils.sanitizeAmountInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EditIncomeViewModel @Inject constructor(
    private val repo: IncomeRepository,
) : ViewModel() {

    val sourceOptions = listOf(
        "Salary", "Freelance", "Business", "Investment",
        "Gift", "Rental", "Refund", "Other",
    )
    val accountList = listOf("Bank Transfer", "Cash", "UPI", "Card")

    private val _rawAmount = MutableStateFlow("")
    val rawAmount: StateFlow<String> = _rawAmount.asStateFlow()

    private val _sourceName = MutableStateFlow("")
    val sourceName: StateFlow<String> = _sourceName.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _selectedSource = MutableStateFlow<String?>(null)
    val selectedSource: StateFlow<String?> = _selectedSource.asStateFlow()

    private val _selectedAccount = MutableStateFlow<String?>(null)
    val selectedAccount: StateFlow<String?> = _selectedAccount.asStateFlow()

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    private val _selectedTime = MutableStateFlow<String?>(null)
    val selectedTime: StateFlow<String?> = _selectedTime.asStateFlow()

    private var originalTimestamp: Long = System.currentTimeMillis()
    private var incomeId: String = ""

    val isFormValid: StateFlow<Boolean> =
        combine(_rawAmount, _selectedSource) { amt, src ->
            amt.isNotBlank() && amt.toDoubleOrNull() != null && src != null
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _updateComplete = MutableSharedFlow<Unit>(replay = 0)
    val updateComplete: SharedFlow<Unit> = _updateComplete

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun loadIncome(id: String) {
        viewModelScope.launch {
            val income = repo.getById(id) ?: return@launch
            incomeId = income.id
            originalTimestamp = income.timestamp
            _rawAmount.value = "%.0f".format(income.amount)
            _sourceName.value = income.source
            _notes.value = income.notes
            _selectedAccount.value = income.paymentMethod.ifBlank { null }
            val matchedOption = sourceOptions.firstOrNull {
                it.equals(income.source, ignoreCase = true)
            }
            _selectedSource.value = matchedOption ?: "Other"
            val cal = Calendar.getInstance().apply { timeInMillis = income.timestamp }
            _selectedDate.value = SimpleDateFormat("MMM d", Locale.getDefault()).format(cal.time)
            _selectedTime.value = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
        }
    }

    fun setRawAmount(v: String) { _rawAmount.value = sanitizeAmountInput(v) }
    fun setSourceName(v: String) { _sourceName.value = v }
    fun selectSource(v: String) { _selectedSource.value = v }
    fun selectAccount(v: String) { _selectedAccount.value = v }
    fun setNotes(v: String) { _notes.value = v }
    fun setDate(d: String) { _selectedDate.value = d }
    fun setTime(t: String) { _selectedTime.value = t }

    fun saveChanges() {
        val amount = _rawAmount.value.toDoubleOrNull() ?: return
        val source = _selectedSource.value ?: return
        val displaySource = _sourceName.value.trim().ifBlank { source }
        val timestamp = if (_selectedDate.value != null)
            DateTimeUtils.parseDateTimeToMillis(_selectedDate.value!!, _selectedTime.value ?: "12:00 AM")
        else originalTimestamp
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val existing = repo.getById(incomeId) ?: return@launch
                repo.updateIncome(
                    existing.copy(
                        amount = amount,
                        source = displaySource,
                        notes = _notes.value.trim(),
                        timestamp = timestamp,
                        paymentMethod = _selectedAccount.value ?: "",
                    )
                )
                _updateComplete.emit(Unit)
            } finally {
                _isSaving.value = false
            }
        }
    }
}

