package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.budget.Budget
import com.example.truxpense.data.repository.budget.BudgetRepository
import com.example.truxpense.data.repository.expense.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// ── Month window helpers ──────────────────────────────────────────────────────

/**
 * Returns the inclusive [start, end] millisecond range for the calendar month
 * at [offset] months relative to now.
 * offset = 0  → current month
 * offset = -1 → last month
 */
private fun monthWindow(offset: Int): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, offset)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val start = cal.timeInMillis
    cal.add(Calendar.MONTH, 1)
    val end = cal.timeInMillis - 1L
    return start to end
}

/**
 * Produces a human-readable "Month YYYY" label for [offset] months from now.
 */
private fun monthLabel(offset: Int): String {
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, offset)
    return "${months[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
}

// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {

    // ── Month navigator ───────────────────────────────────────────────────────
    // offset = 0 → current month, -1 → last month, -2 → two months ago, etc.
    // canGoForward is true only when offset < 0 (can't navigate into the future).
    // canGoBack allows up to 24 months of history.

    private val _monthOffset = MutableStateFlow(0)

    val currentMonth: StateFlow<String> =
        _monthOffset.map { monthLabel(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, monthLabel(0))

    val canGoBack: StateFlow<Boolean> =
        _monthOffset.map { it > -24 }
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val canGoForward: StateFlow<Boolean> =
        _monthOffset.map { it < 0 }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun previousMonth() {
        if (_monthOffset.value > -24) _monthOffset.value--
    }

    fun nextMonth() {
        if (_monthOffset.value < 0) _monthOffset.value++
    }

    // ── Live categories derived from Room flows ───────────────────────────────
    // Transactions are filtered to the selected month window so that totalSpent,
    // progress bars, and exceeded amounts all reflect the correct time period.

    val categories: StateFlow<List<BudgetCategory>> =
        combine(budgetRepository.budgets, expenseRepository.transactions, _monthOffset) { budgets, txs, offset ->
            val (winStart, winEnd) = monthWindow(offset)
            val spentByCategory = txs
                .filter { it.timestamp in winStart..winEnd }
                .groupBy { it.category }
                .mapValues { (_, items) -> items.sumOf { it.amount }.toInt() }

            val isPastMonth = offset < 0

            budgets.mapIndexed { index, b ->
                BudgetCategory(
                    id = 1_000 + index,
                    name = b.category,
                    spent = spentByCategory[b.category] ?: 0,
                    total = b.amount.toInt(),
                    barColor = budgetColorForCategory(b.category),
                )
            }.filter { cat ->
                // For past months only surface categories that had actual transactions.
                // A category with zero spend in a historical month means the budget
                // didn't exist yet — showing it would bleed current limits into the past.
                // The current month always shows all categories (including new empty ones).
                !isPastMonth || cat.spent > 0
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Single source of truth whether any budget exists ─────────────────────

    val hasBudgets: StateFlow<Boolean> =
        categories.map { it.isNotEmpty() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── isLoaded ──────────────────────────────────────────────────────────────

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            budgetRepository.budgets.first()
            _isLoaded.value = true
        }
    }

    // ── Display items (pre-formatted for the UI row) ──────────────────────────

    val categoryDisplayItems: StateFlow<List<BudgetCategoryDisplay>> =
        categories.map { list ->
            list.map { cat ->
                BudgetCategoryDisplay(
                    category = cat,
                    amountText = formatBudgetAmounts(cat),
                    progress = computeProgress(cat),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Aggregated totals ─────────────────────────────────────────────────────

    val totalBudget: StateFlow<Int> =
        categories.map { it.sumOf { c -> c.total } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val totalSpent: StateFlow<Int> =
        categories.map { it.sumOf { c -> c.spent } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ── Actions ───────────────────────────────────────────────────────────────

    fun addBudget(category: String, amount: Double) {
        viewModelScope.launch {
            budgetRepository.addBudget(Budget(category = category, amount = amount))
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch { budgetRepository.deleteBudget(id) }
    }

    fun clear() {
        viewModelScope.launch { budgetRepository.clear() }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun computeProgress(cat: BudgetCategory): Float =
        if (cat.total > 0) (cat.spent.toFloat() / cat.total.toFloat()).coerceIn(0f, 1f) else 0f

    private fun formatBudgetAmounts(cat: BudgetCategory): String {
        val fmt = runCatching {
            val locale = java.util.Locale.Builder().setLanguage("en").setRegion("IN").build()
            java.text.NumberFormat.getCurrencyInstance(locale).apply {
                currency = java.util.Currency.getInstance("INR")
            }
        }.getOrElse { java.text.NumberFormat.getCurrencyInstance() }
        val spentStr = runCatching { fmt.format(cat.spent.toDouble()) }.getOrDefault("${cat.spent}")
        val totalStr = runCatching { fmt.format(cat.total.toDouble()) }.getOrDefault("${cat.total}")
        return "$spentStr / $totalStr"
    }
}

// UI helper model: formatted display for a budget category
data class BudgetCategoryDisplay(
    val category: BudgetCategory,
    val amountText: String,
    val progress: Float,
)