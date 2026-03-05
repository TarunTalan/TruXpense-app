package com.example.truxpense.presentation.screens.dashboard.addincome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.income.Income
import com.example.truxpense.data.repository.income.IncomeRepository
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

    val sourceOptions = listOf("Salary", "Freelance", "Business", "Investment", "Gift", "Other")

    private val _rawAmount = MutableStateFlow("")
    val rawAmount: StateFlow<String> = _rawAmount.asStateFlow()

    private val _selectedSource = MutableStateFlow(sourceOptions.first())
    val selectedSource: StateFlow<String> = _selectedSource.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _selectedDate = MutableStateFlow(
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())
    )
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val formattedAmount: StateFlow<String> = _rawAmount.map { raw ->
        val num = raw.toDoubleOrNull()
        if (num == null || raw.isEmpty()) "₹0" else "₹${"%,.0f".format(num)}"
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "₹0")

    val isFormValid: StateFlow<Boolean> = _rawAmount.map { raw ->
        raw.isNotBlank() && raw.toDoubleOrNull() != null && raw.toDouble() > 0
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _saveComplete = MutableSharedFlow<Unit>(replay = 0)
    val saveComplete: SharedFlow<Unit> = _saveComplete

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun setRawAmount(v: String) { _rawAmount.value = v.filter { it.isDigit() || it == '.' } }
    fun setSource(v: String)    { _selectedSource.value = v }
    fun setNotes(v: String)     { _notes.value = v }
    fun setDate(d: String)      { _selectedDate.value = d }

    fun saveIncome() {
        val amount = _rawAmount.value.toDoubleOrNull() ?: return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.addIncome(
                    Income(
                        amount = amount,
                        source = _selectedSource.value,
                        notes = _notes.value.trim(),
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
        _notes.value = ""
        _selectedSource.value = sourceOptions.first()
        _selectedDate.value = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())
    }
}

