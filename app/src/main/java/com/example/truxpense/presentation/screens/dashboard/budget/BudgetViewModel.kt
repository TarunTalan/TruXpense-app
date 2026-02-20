package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.budget.Budget
import com.example.truxpense.data.budget.BudgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BudgetViewModel : ViewModel() {
    val budgets: StateFlow<List<Budget>> = BudgetRepository.budgets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // Expose categories so UI can render dynamic category list
    private val _categories = MutableStateFlow(
        listOf(
            "Food", "Transport", "Bills", "Shopping", "Travel",
            "Health", "Education", "Entertainment", "Groceries", "Other"
        )
    )
    val categories: StateFlow<List<String>> = _categories

    fun addBudget(category: String, amount: Double) {
        viewModelScope.launch {
            BudgetRepository.addBudget(Budget(category = category, amount = amount))
        }
    }

    fun clear() {
        viewModelScope.launch { BudgetRepository.clear() }
    }
}
