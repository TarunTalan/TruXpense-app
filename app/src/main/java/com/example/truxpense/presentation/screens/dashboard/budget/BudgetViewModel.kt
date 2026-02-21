package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.budget.Budget
import com.example.truxpense.data.budget.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor() : ViewModel() {
    private val repoBudgets: StateFlow<List<Budget>> = BudgetRepository.budgets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // Map repository budgets to UI categories
    val categories: StateFlow<List<BudgetCategory>> = repoBudgets.map { list ->
        list.mapIndexed { index, b ->
            BudgetCategory(
                id = 1000 + index,
                name = b.category,
                spent = 0, // TODO: compute from transactions repository when available
                total = b.amount.toInt(),
                barColor = budgetColorForCategory(b.category)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Totals computed from categories
    val totalBudget: StateFlow<Int> =
        categories.map { it.sumOf { c -> c.total } }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val totalSpent: StateFlow<Int> =
        categories.map { it.sumOf { c -> c.spent } }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // Keep a static list of category names for selection UIs (AddBudgetScreen)
    private val _categoryNames = MutableStateFlow(
        listOf(
            "Food", "Transport", "Bills", "Shopping", "Travel",
            "Health", "Education", "Entertainment", "Groceries", "Other"
        )
    )
    val categoryNames: StateFlow<List<String>> = _categoryNames

    fun addBudget(category: String, amount: Double) {
        viewModelScope.launch {
            BudgetRepository.addBudget(Budget(category = category, amount = amount))
        }
    }

    fun clear() {
        viewModelScope.launch { BudgetRepository.clear() }
    }
}
