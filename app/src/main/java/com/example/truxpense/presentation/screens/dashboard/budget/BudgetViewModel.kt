package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.budget.Budget
import com.example.truxpense.data.budget.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

val budgetMonths = listOf("January 2026", "February 2026", "March 2026")

@HiltViewModel
class BudgetViewModel @Inject constructor() : ViewModel() {

    // Repository source

    private val repoBudgets: StateFlow<List<Budget>> = BudgetRepository.budgets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    // UI categories with progress computed

    val categories: StateFlow<List<BudgetCategory>> = repoBudgets.map { list ->
        list.mapIndexed { index, b ->
            BudgetCategory(
                id = 1000 + index,
                name = b.category,
                spent = 0,             // TODO: wire to transactions repository
                total = b.amount.toInt(),
                barColor = budgetColorForCategory(b.category),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // UI category display items (formatted strings + progress)
    // Screens should use this instead of formatting inside composable lambdas.

    val categoryDisplayItems: StateFlow<List<BudgetCategoryDisplay>> = categories.map { list ->
        list.map { cat ->
            BudgetCategoryDisplay(
                category = cat,
                amountText = formatBudgetAmounts(cat),
                progress = computeProgress(cat),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Aggregated totals

    val totalBudget: StateFlow<Int> =
        categories.map { it.sumOf { c -> c.total } }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val totalSpent: StateFlow<Int> =
        categories.map { it.sumOf { c -> c.spent } }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // Month navigator (was `var monthIndex by remember` in BudgetScreen) ───

    private val _monthIndex = MutableStateFlow(1)  // default → February
    val monthIndex: StateFlow<Int> = _monthIndex.asStateFlow()

    val currentMonth: StateFlow<String> = _monthIndex.map { budgetMonths.getOrElse(it) { budgetMonths.first() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, budgetMonths[1])

    val canGoBack: StateFlow<Boolean> =
        _monthIndex.map { it > 0 }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val canGoForward: StateFlow<Boolean> =
        _monthIndex.map { it < budgetMonths.lastIndex }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun previousMonth() {
        if (_monthIndex.value > 0) _monthIndex.value--
    }

    fun nextMonth() {
        if (_monthIndex.value < budgetMonths.lastIndex) _monthIndex.value++
    }

    // Category names for AddBudgetScreen

    val categoryNames: StateFlow<List<String>> = MutableStateFlow(
        listOf(
            "Food", "Transport", "Bills", "Shopping", "Travel",
            "Health", "Education", "Entertainment", "Groceries", "Other",
        )
    ).asStateFlow()

    // Actions

    fun addBudget(category: String, amount: Double) {
        viewModelScope.launch {
            BudgetRepository.addBudget(Budget(category = category, amount = amount))
        }
    }

    fun clear() {
        viewModelScope.launch { BudgetRepository.clear() }
    }

    // Private helpers

    private fun computeProgress(cat: BudgetCategory): Float =
        if (cat.total > 0) (cat.spent.toFloat() / cat.total.toFloat()).coerceIn(0f, 1f) else 0f

    /**
     * Returns "₹X,XXX / ₹X,XXX" — formatted by a shared util so every screen
     * uses the same currency formatting without duplicating a NumberFormat factory.
     */
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

/** Pre-formatted display model for a single budget category row. */
data class BudgetCategoryDisplay(
    val category: BudgetCategory,
    val amountText: String,
    val progress: Float,
)