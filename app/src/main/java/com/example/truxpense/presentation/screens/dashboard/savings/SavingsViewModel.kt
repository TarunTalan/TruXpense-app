package com.example.truxpense.presentation.screens.dashboard.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.savings.SavingsEntryUi
import com.example.truxpense.data.repository.savings.SavingsGoalUi
import com.example.truxpense.data.repository.savings.SavingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SavingsUiState(
    val totalSavings: Double = 0.0,
    val goals: List<SavingsGoalUi> = emptyList(),
    val recentEntries: List<SavingsEntryUi> = emptyList(),
    val isLoading: Boolean = true,
)

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
            goals = goals,
            recentEntries = entries,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SavingsUiState(),
    )
}