package com.example.truxpense.presentation.screens.dashboard.savings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.savings.ContributeFrequency
import com.example.truxpense.data.repository.savings.SavingsGoalUi
import com.example.truxpense.data.repository.savings.SavingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// Available goal icons (use label keys instead of emojis)
val GOAL_ICONS = listOf(
    "iphone", "camera", "women_bag", "gaming_controller", "laptop", "pager", "car", "cruise_ship",
    "gift", "home_icon", "reading_books", "fitness",
)

@HiltViewModel
class CreateGoalViewModel @Inject constructor(
    private val repository: SavingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val editGoalId: Long = savedStateHandle.get<Long>("goalId") ?: -1L

    // ── Form fields ───────────────────────────────────────────────────────────
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _icon = MutableStateFlow("iphone")
    val icon: StateFlow<String> = _icon.asStateFlow()

    private val _colorHex = MutableStateFlow("#2FA4A9")
    val colorHex: StateFlow<String> = _colorHex.asStateFlow()

    private val _targetAmount = MutableStateFlow("")
    val targetAmount: StateFlow<String> = _targetAmount.asStateFlow()

    // Use calendar to compute epoch-day (days since 1970-01-01) to avoid java.time APIs
    private fun epochDayForMonthsFromNow(months: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, months)
        // normalize to start of day
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / 86_400_000L
    }

    private val _targetDateEpoch = MutableStateFlow(
        epochDayForMonthsFromNow(6)
    )
    val targetDateEpoch: StateFlow<Long> = _targetDateEpoch.asStateFlow()

    private val _autoContribute = MutableStateFlow(true)
    val autoContribute: StateFlow<Boolean> = _autoContribute.asStateFlow()

    private val _dailyAmount = MutableStateFlow("500")
    val dailyAmount: StateFlow<String> = _dailyAmount.asStateFlow()

    private val _frequency = MutableStateFlow(ContributeFrequency.DAILY)
    val frequency: StateFlow<ContributeFrequency> = _frequency.asStateFlow()

    private val _showAllIcons = MutableStateFlow(false)
    val showAllIcons: StateFlow<Boolean> = _showAllIcons.asStateFlow()

    val isEditMode = editGoalId != -1L

    val canCreate: StateFlow<Boolean> = combine(_name, _targetAmount) { n, a ->
        n.isNotBlank() && (a.toDoubleOrNull() ?: 0.0) > 0
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // events
    private val _created = MutableSharedFlow<Long>()
    val created: SharedFlow<Long> = _created.asSharedFlow()

    init {
        if (isEditMode) loadGoalForEdit()
    }

    private fun loadGoalForEdit() = viewModelScope.launch {
        repository.goal(editGoalId).firstOrNull()?.let { g ->
            _name.value = g.name
            _icon.value = g.icon
            _colorHex.value = g.colorHex
            _targetAmount.value = g.targetAmount.toInt().toString()
            _targetDateEpoch.value = g.targetDateEpoch
            _autoContribute.value = g.autoContribute
            _dailyAmount.value = g.autoContributeAmount.toInt().toString()
            _frequency.value = g.autoContributeFrequency
        }
    }

    fun onNameChange(v: String) {
        _name.value = v
    }

    fun onIconSelect(v: String) {
        _icon.value = v
    }

    fun onColorSelect(v: String) {
        _colorHex.value = v
    }

    fun onAmountChange(v: String) {
        _targetAmount.value = v.filter { it.isDigit() }
    }

    fun onDateChange(epoch: Long) {
        _targetDateEpoch.value = epoch
    }

    fun onAutoContributeToggle() {
        _autoContribute.value = !_autoContribute.value
    }

    fun onDailyAmountChange(v: String) {
        _dailyAmount.value = v.filter { it.isDigit() }
    }

    fun onFrequencyChange(f: ContributeFrequency) {
        _frequency.value = f
    }

    fun onToggleShowAllIcons() {
        _showAllIcons.value = !_showAllIcons.value
    }

    fun createOrUpdate() = viewModelScope.launch {
        val goal = SavingsGoalUi(
            id = if (isEditMode) editGoalId else 0L,
            name = _name.value.trim(),
            icon = _icon.value,
            colorHex = _colorHex.value,
            targetAmount = _targetAmount.value.toDoubleOrNull() ?: return@launch,
            savedAmount = if (isEditMode) {
                repository.goal(editGoalId).firstOrNull()?.savedAmount ?: 0.0
            } else 0.0,
            targetDateEpoch = _targetDateEpoch.value,
            autoContribute = _autoContribute.value,
            autoContributeAmount = _dailyAmount.value.toDoubleOrNull() ?: 500.0,
            autoContributeFrequency = _frequency.value,
            isCompleted = false,
        )
        val id = repository.createGoal(goal)
        _created.emit(id)
    }
}