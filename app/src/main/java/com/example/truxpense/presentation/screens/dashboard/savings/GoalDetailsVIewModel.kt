package com.example.truxpense.presentation.screens.dashboard.savings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.savings.SavingsContributionUi
import com.example.truxpense.data.repository.savings.SavingsGoalUi
import com.example.truxpense.data.repository.savings.SavingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalDetailUiState(
    val goal: SavingsGoalUi? = null,
    val contributions: List<SavingsContributionUi> = emptyList(),
    val isLoading: Boolean = true,
    val justCompleted: Boolean = false,
)

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    private val repository: SavingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val goalId: Long = checkNotNull(savedStateHandle["goalId"])

    // Monthly bar chart data — last 7 months
    val monthlyData: StateFlow<List<Pair<String, Double>>> =
        repository.contributions(goalId).map { buildMonthlyData(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<GoalDetailUiState> = combine(
        repository.goal(goalId),
        repository.contributions(goalId),
    ) { goal, contribs ->
        GoalDetailUiState(
            goal = goal,
            contributions = contribs.take(3),
            isLoading = false,
            justCompleted = goal?.isCompleted == true,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        GoalDetailUiState(),
    )

    // ── One-shot event: goal completed (fires only once — celebration not yet shown) ──
    private val _completedEvent = MutableSharedFlow<Unit>()
    val completedEvent: SharedFlow<Unit> = _completedEvent.asSharedFlow()

    // ── One-shot event: goal deleted — navigate back BEFORE Room nulls out the goal ──
    private val _deleteEvent = MutableSharedFlow<Unit>()
    val deleteEvent: SharedFlow<Unit> = _deleteEvent.asSharedFlow()

    fun addContribution(amount: Double, label: String = "Manual add") = viewModelScope.launch {
        val prev = uiState.value.goal ?: return@launch

        // Compute completion locally — don't re-query Room, because firstOrNull()
        // on a Room Flow can return the stale pre-write value due to emission ordering.
        val willComplete = !prev.isCompleted && (prev.savedAmount + amount >= prev.targetAmount)
        val shouldCelebrate = willComplete && !prev.celebrationShown

        repository.addContribution(goalId, amount, label)

        if (shouldCelebrate) {
            repository.markCelebrationShown(goalId)
            _completedEvent.emit(Unit)
        }
    }

    /**
     * Emits [deleteEvent] first so the screen navigates away immediately,
     * then deletes from the database. This prevents the `goal ?: return`
     * guard in the composable from rendering a blank screen before navigation.
     */
    fun deleteGoal() = viewModelScope.launch {
        _deleteEvent.emit(Unit)          // navigate back first
        repository.deleteGoal(goalId)   // delete in background
    }

    private fun buildMonthlyData(contribs: List<SavingsContributionUi>): List<Pair<String, Double>> {
        val months = (6 downTo 0).map { offset ->
            val c = java.util.Calendar.getInstance()
            c.add(java.util.Calendar.MONTH, -offset)
            val label = c.getDisplayName(
                java.util.Calendar.MONTH, java.util.Calendar.SHORT, java.util.Locale.getDefault()
            ) ?: ""
            val year = c.get(java.util.Calendar.YEAR)
            val month = c.get(java.util.Calendar.MONTH)
            Triple(label, year, month)
        }
        val grouped = contribs.groupBy { entry ->
            val c = java.util.Calendar.getInstance().also { it.timeInMillis = entry.timestampMs }
            Pair(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH))
        }
        return months.map { (label, year, month) ->
            val total = grouped[Pair(year, month)]?.sumOf { it.amount } ?: 0.0
            Pair(label, total)
        }
    }
}