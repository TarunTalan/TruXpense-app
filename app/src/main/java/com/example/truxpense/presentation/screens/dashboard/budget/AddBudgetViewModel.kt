package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.budget.Budget
import com.example.truxpense.data.budget.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddBudgetViewModel @Inject constructor() : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _amountInput = MutableStateFlow("")
    val amountInput: StateFlow<String> = _amountInput

    // Use a local static list of categories (same as BudgetViewModel) so we don't inject a ViewModel here
    private val _categoryNames = MutableStateFlow(
        listOf(
            "Food", "Transport", "Bills", "Shopping", "Travel",
            "Health", "Education", "Entertainment", "Groceries", "Other"
        )
    )
    val categories: StateFlow<List<String>> = _categoryNames

    fun setQuery(q: String) {
        _query.value = q
    }

    fun setSelected(cat: String?) {
        _selectedCategory.value = cat
    }

    fun setAmountInput(v: String) {
        _amountInput.value = v
    }

    fun createBudget(onComplete: () -> Unit) {
        val cat = selectedCategory.value ?: return
        val amt = amountInput.value.toDoubleOrNull() ?: 0.0
        viewModelScope.launch {
            // Use the repository directly to add budget
            BudgetRepository.addBudget(Budget(category = cat, amount = amt))
            onComplete()
        }
    }
}
