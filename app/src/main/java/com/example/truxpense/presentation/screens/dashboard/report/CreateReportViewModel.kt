package com.example.truxpense.presentation.screens.dashboard.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.expense.ExpenseRepository
import com.example.truxpense.data.repository.income.IncomeRepository
import com.example.truxpense.data.repository.report.Report
import com.example.truxpense.data.repository.report.ReportRepository
import com.example.truxpense.data.repository.report.ReportType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// ── UI state ──────────────────────────────────────────────────────────────────

data class CreateReportUiState(
    // ── Form fields ───────────────────────────────────────────────────────────
    val title: String = "",
    val fromDate: Long = monthStartMs(),
    val toDate: Long = System.currentTimeMillis(),
    val reportType: ReportType = ReportType.EXPENSE,
    /** Empty set means "all categories". */
    val selectedCategories: Set<String> = emptySet(),

    // ── Supporting data ───────────────────────────────────────────────────────
    val availableCategories: List<String> = emptyList(),

    // ── Validation ────────────────────────────────────────────────────────────
    val titleError: String? = null,
    val dateError: String? = null,

    // ── Async state ───────────────────────────────────────────────────────────
    val isSaving: Boolean = false,
    /** Non-null when the report was saved successfully — the ID to navigate to. */
    val savedReportId: String? = null,
) {
    val isValid: Boolean
        get() = title.isNotBlank() && fromDate <= toDate && !isSaving

    val allCategoriesSelected: Boolean
        get() = selectedCategories.isEmpty()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class CreateReportViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val reportRepository: ReportRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateReportUiState())
    val uiState: StateFlow<CreateReportUiState> = _uiState.asStateFlow()

    // ── Load available categories on init ─────────────────────────────────────

    init {
        viewModelScope.launch {
            // Merge expense and income categories so the user can filter either.
            combine(
                expenseRepository.transactions,
                incomeRepository.allIncome,
            ) { expenses, incomes ->
                val expenseCats = expenses.map { it.category }
                val incomeSources = incomes.map { it.source }
                (expenseCats + incomeSources).distinct().sorted()
            }.collect { cats ->
                _uiState.update { it.copy(availableCategories = cats) }
            }
        }
    }

    // ── Form field setters ────────────────────────────────────────────────────

    fun setTitle(value: String) {
        _uiState.update { it.copy(title = value, titleError = null) }
    }

    fun setFromDate(epochMs: Long) {
        _uiState.update { it.copy(fromDate = epochMs, dateError = null) }
    }

    fun setToDate(epochMs: Long) {
        _uiState.update { it.copy(toDate = epochMs, dateError = null) }
    }

    fun setReportType(type: ReportType) {
        _uiState.update { it.copy(reportType = type) }
    }

    /** Toggle a single category in/out of [selectedCategories]. */
    fun toggleCategory(category: String) {
        _uiState.update { state ->
            val current = state.selectedCategories.toMutableSet()
            if (category in current) current.remove(category) else current.add(category)
            state.copy(selectedCategories = current)
        }
    }

    /** Clear all category selections → include all categories in the report. */
    fun selectAllCategories() {
        _uiState.update { it.copy(selectedCategories = emptySet()) }
    }

    // ── Generate default title ────────────────────────────────────────────────

    /**
     * Populates [CreateReportUiState.title] with a sensible default based on the
     * current form state, e.g. "Expense Report – March 2026".
     * Only sets the title if the field is still blank.
     */
    fun suggestTitle() {
        val state = _uiState.value
        if (state.title.isNotBlank()) return

        val cal = Calendar.getInstance().apply { timeInMillis = state.fromDate }
        val monthFull = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December",
        )[cal.get(Calendar.MONTH)]
        val year = cal.get(Calendar.YEAR)

        val typeLabel = when (state.reportType) {
            ReportType.EXPENSE -> "Expense Report"
            ReportType.INCOME  -> "Income Report"
            ReportType.ALL     -> "Full Report"
        }
        _uiState.update { it.copy(title = "$typeLabel – $monthFull $year") }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun saveReport() {
        val state = _uiState.value

        // Client-side validation
        var hasError = false
        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "Report title is required") }
            hasError = true
        }
        if (state.fromDate > state.toDate) {
            _uiState.update { it.copy(dateError = "Start date must be before end date") }
            hasError = true
        }
        if (hasError) return

        _uiState.update { it.copy(isSaving = true) }

        val categoriesStr = state.selectedCategories
            .sorted()
            .joinToString("|")

        val report = Report(
            title      = state.title.trim(),
            fromDate   = state.fromDate,
            toDate     = state.toDate,
            reportType = state.reportType.name,
            categories = categoriesStr,
        )

        viewModelScope.launch {
            reportRepository.saveReport(report)
            _uiState.update { it.copy(isSaving = false, savedReportId = report.id) }
        }
    }

    /** Call this after consuming [CreateReportUiState.savedReportId] (i.e. after navigation). */
    fun onNavigatedToDetail() {
        _uiState.update { it.copy(savedReportId = null) }
    }
}

// ── Private util ──────────────────────────────────────────────────────────────

private fun monthStartMs(): Long = Calendar.getInstance().apply {
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis