package com.example.truxpense.presentation.screens.dashboard.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.savings.SavingsEntryUi
import com.example.truxpense.data.repository.savings.SavingsGoalUi
import com.example.truxpense.data.repository.savings.SavingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavingsUiState(
    val totalSavings: Double = 0.0,
    val activeGoals: List<SavingsGoalUi> = emptyList(),
    val completedGoals: List<SavingsGoalUi> = emptyList(),
    val recentEntries: List<SavingsEntryUi> = emptyList(),
    val isLoading: Boolean = true,
) {
    // Kept for any existing callers — resolved from the two lists
    val goals: List<SavingsGoalUi> get() = activeGoals + completedGoals
}

@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val repository: SavingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SavingsUiState> = combine(
        repository.totalSavings,
        repository.goals,
        repository.recentSavingsEntries,
    ) { total, goals, entries ->
        SavingsUiState(
            totalSavings = total,
            activeGoals = goals.filter { !it.isCompleted },
            completedGoals = goals.filter { it.isCompleted },
            recentEntries = entries,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SavingsUiState(),
    )

    // ── Snackbar events ───────────────────────────────────────────────────────

    private val _snackbar = MutableSharedFlow<String>()
    val snackbar: SharedFlow<String> = _snackbar

    // ── Goal completed event (fires once per goal — celebration not yet shown) ─

    private val _goalCompleted = MutableSharedFlow<SavingsGoalUi>()
    val goalCompleted: SharedFlow<SavingsGoalUi> = _goalCompleted

    // ── Quick-add ─────────────────────────────────────────────────────────────

    fun quickAddToGoal(goal: SavingsGoalUi, unallocated: Double, requestedAmount: Double? = null) {
        viewModelScope.launch {
            val desired = when {
                requestedAmount != null -> requestedAmount.coerceAtLeast(0.0)
                else -> minOf(goal.autoContributeAmount, goal.remaining)
            }

            if (desired <= 0.0) {
                _snackbar.emit("Goal already completed")
                return@launch
            }

            // actual amount is the minimum of desired, remaining and unallocated
            val actual = minOf(desired, goal.remaining, unallocated)

            if (actual <= 0.0) {
                val available = "₹${"%.0f".format(unallocated)}"
                _snackbar.emit("Not enough unallocated savings ($available available)")
                return@launch
            }

            repository.addContribution(goal.id, actual, "Quick add")
            val added = "₹${"%.0f".format(actual)}"
            _snackbar.emit("Added $added to ${goal.name}")

            // If this contribution filled the goal and celebration hasn't been shown yet
            val isNowComplete = actual >= goal.remaining
            if (isNowComplete && !goal.celebrationShown) {
                repository.markCelebrationShown(goal.id)
                _goalCompleted.emit(goal.copy(savedAmount = goal.savedAmount + actual))
            }
        }
    }
}