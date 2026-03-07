package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.budget.Budget
import com.example.truxpense.data.repository.budget.BudgetRepository
import com.example.truxpense.data.repository.expense.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

val budgetMonths = listOf("January 2026", "February 2026", "March 2026")

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {

    // ── Live categories derived from Room flows ───────────────────────────────

    val categories: StateFlow<List<BudgetCategory>> =
        combine(budgetRepository.budgets, expenseRepository.transactions) { budgets, txs ->
            val spentByCategory = txs
                .groupBy { it.category }
                .mapValues { (_, items) -> items.sumOf { it.amount }.toInt() }

            budgets.mapIndexed { index, b ->
                BudgetCategory(
                    id = 1_000 + index,
                    name = b.category,
                    spent = spentByCategory[b.category] ?: 0,
                    total = b.amount.toInt(),
                    barColor = budgetColorForCategory(b.category),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // New: single source of truth whether any budget exists
    val hasBudgets: StateFlow<Boolean> =
        categories.map { it.isNotEmpty() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** True once Room has emitted its first value — prevents empty-screen flash on cold start. */
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

    // ── Month navigator ───────────────────────────────────────────────────────

    private val _monthIndex = MutableStateFlow(1)   // default → February 2026
    val monthIndex: StateFlow<Int> = _monthIndex.asStateFlow()

    val currentMonth: StateFlow<String> =
        _monthIndex.map { budgetMonths.getOrElse(it) { budgetMonths.first() } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, budgetMonths[1])

    val canGoBack: StateFlow<Boolean> =
        _monthIndex.map { it > 0 }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val canGoForward: StateFlow<Boolean> =
        _monthIndex.map { it < budgetMonths.lastIndex }
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun previousMonth() { if (_monthIndex.value > 0) _monthIndex.value-- }
    fun nextMonth()     { if (_monthIndex.value < budgetMonths.lastIndex) _monthIndex.value++ }

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
