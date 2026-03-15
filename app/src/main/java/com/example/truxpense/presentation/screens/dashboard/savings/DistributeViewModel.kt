package com.example.truxpense.presentation.screens.dashboard.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.savings.SavingsGoalUi
import com.example.truxpense.data.repository.savings.SavingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DistributeUiState(
    val totalAvailable: Double = 0.0,
    val goals: List<SavingsGoalUi> = emptyList(),
    /** goalId -> allocated amount (starts at current savedAmount) */
    val allocations: Map<Long, Double> = emptyMap(),
    val isLoading: Boolean = true,
    val confirmed: Boolean = false,
)

@HiltViewModel
class DistributeViewModel @Inject constructor(
    private val repository: SavingsRepository,
) : ViewModel() {

    private val _allocations = MutableStateFlow<Map<Long, Double>>(emptyMap())

    val uiState: StateFlow<DistributeUiState> = combine(
        repository.totalSavings,
        repository.goals,
        _allocations,
    ) { total, goals, allocs ->
        // seed allocations for new goals and ensure values are clamped between savedAmount and targetAmount
        val seeded = goals.associate { g ->
            val raw = allocs[g.id] ?: g.savedAmount
            val lower = minOf(g.savedAmount, g.targetAmount)
            val upper = maxOf(g.savedAmount, g.targetAmount)
            g.id to raw.coerceIn(lower, upper)
        }
        if (allocs.isEmpty() && goals.isNotEmpty()) {
            _allocations.value = seeded
        }
        DistributeUiState(
            totalAvailable = total,
            goals = goals,
            allocations = seeded,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DistributeUiState())

    val totalDistributed: StateFlow<Double> = combine(
        repository.goals,
        _allocations,
    ) { goals, allocs ->
        goals.sumOf { g ->
            val raw = allocs[g.id] ?: g.savedAmount
            val lower = minOf(g.savedAmount, g.targetAmount)
            val upper = maxOf(g.savedAmount, g.targetAmount)
            val clamped = raw.coerceIn(lower, upper)
            (clamped - g.savedAmount)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    fun adjust(goalId: Long, delta: Double) {
        val goal = uiState.value.goals.find { it.id == goalId } ?: return
        val current = _allocations.value[goalId] ?: goal.savedAmount
        val next = (current + delta).coerceIn(goal.savedAmount, goal.targetAmount)
        _allocations.value = _allocations.value + (goalId to next)
    }

    fun setAllocation(goalId: Long, amount: Double) {
        val goal = uiState.value.goals.find { it.id == goalId } ?: return
        val clamped = amount.coerceIn(goal.savedAmount, goal.targetAmount)
        _allocations.value = _allocations.value + (goalId to clamped)
    }

    fun confirmDistribution() = viewModelScope.launch {
        val allocs = _allocations.value
        repository.distributeGoals(allocs)
    }
}