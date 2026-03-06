package com.example.truxpense.presentation.screens.dashboard.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.savings.SavingsEntry
import com.example.truxpense.data.repository.savings.SavingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class SavingsListItem(
    val id: String,
    val goalName: String,
    val amount: Double,
    val notes: String,
    val dateLabel: String,
    val timestamp: Long,
)

data class SavingsMonthGroup(
    val monthLabel: String,
    val total: Double,
    val items: List<SavingsListItem>,
)

@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val repository: SavingsRepository,
) : ViewModel() {

    // ── Add / Edit form state ─────────────────────────────────────────────────
    val goalOptions = listOf(
        "Emergency Fund", "Vacation", "New Phone", "Home Down Payment",
        "Car", "Education", "Investment", "Wedding", "Retirement", "Other"
    )

    private val _rawAmount = MutableStateFlow("")
    val rawAmount: StateFlow<String> = _rawAmount.asStateFlow()

    private val _selectedGoal = MutableStateFlow(goalOptions.first())
    val selectedGoal: StateFlow<String> = _selectedGoal.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _selectedDate = MutableStateFlow(
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())
    )
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()
    private var _timestampMs: Long = System.currentTimeMillis()

    // Edit mode
    private var _editingId: String? = null
    val isEditing: StateFlow<Boolean> = MutableStateFlow(false).also { it.value = false }
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

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

    // ── History state ─────────────────────────────────────────────────────────
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allSavings.first()
            _isLoaded.value = true
        }
    }

    val monthGroups: StateFlow<List<SavingsMonthGroup>> = repository.allSavings
        .map { list ->
            list.map { it.toListItem() }
                .groupBy { groupKey(it.timestamp) }
                .map { (key, items) ->
                    SavingsMonthGroup(monthLabel = key, total = items.sumOf { it.amount }, items = items)
                }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totalSavings: StateFlow<Double> = monthGroups
        .map { it.sumOf { g -> g.total } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val availableGoals: StateFlow<List<String>> = repository.allSavings
        .map { list -> list.map { it.goalName }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Actions ───────────────────────────────────────────────────────────────

    fun setRawAmount(v: String)  { _rawAmount.value = v.filter { it.isDigit() || it == '.' } }
    fun setGoal(v: String)       { _selectedGoal.value = v }
    fun setNotes(v: String)      { _notes.value = v }
    fun setDateFromTimestamp(ts: Long) {
        _timestampMs = ts
        _selectedDate.value = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ts))
    }

    fun loadForEdit(id: String) {
        _editingId = id
        _isEditMode.value = true
        viewModelScope.launch {
            val entry = repository.getById(id) ?: return@launch
            _rawAmount.value = entry.amount.toLong().toString()
            _selectedGoal.value = entry.goalName
            _notes.value = entry.notes
            _timestampMs = entry.timestamp
            _selectedDate.value = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(entry.timestamp))
        }
    }

    fun resetForm() {
        _editingId = null
        _isEditMode.value = false
        _rawAmount.value = ""
        _selectedGoal.value = goalOptions.first()
        _notes.value = ""
        _timestampMs = System.currentTimeMillis()
        _selectedDate.value = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())
    }

    fun save() {
        val amount = _rawAmount.value.toDoubleOrNull() ?: return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val id = _editingId
                if (id != null) {
                    repository.updateSavings(
                        SavingsEntry(id = id, goalName = _selectedGoal.value,
                            amount = amount, notes = _notes.value.trim(), timestamp = _timestampMs)
                    )
                } else {
                    repository.addSavings(
                        SavingsEntry(goalName = _selectedGoal.value,
                            amount = amount, notes = _notes.value.trim(), timestamp = _timestampMs)
                    )
                }
                _saveComplete.emit(Unit)
                resetForm()
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.deleteSavings(id) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private val monthNames = listOf("January","February","March","April","May","June",
        "July","August","September","October","November","December")
    private val monthShort = listOf("Jan","Feb","Mar","Apr","May","Jun",
        "Jul","Aug","Sep","Oct","Nov","Dec")

    private fun groupKey(ts: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        return "${monthNames[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
    }

    private fun SavingsEntry.toListItem(): SavingsListItem {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val mon = monthShort[cal.get(Calendar.MONTH)]
        return SavingsListItem(
            id = id, goalName = goalName, amount = amount, notes = notes,
            dateLabel = "$mon $day", timestamp = timestamp,
        )
    }
}

