package com.example.truxpense.presentation.screens.dashboard.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.report.Report
import com.example.truxpense.data.repository.report.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ReportsUiState(
    val reports: List<Report> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
) : ViewModel() {

    val uiState: StateFlow<ReportsUiState> =
        reportRepository.reports
            .map { list -> ReportsUiState(reports = list, isLoading = false) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ReportsUiState(),
            )
}