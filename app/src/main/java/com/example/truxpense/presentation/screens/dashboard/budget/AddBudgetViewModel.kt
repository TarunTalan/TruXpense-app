package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.R
import com.example.truxpense.data.budget.Budget
import com.example.truxpense.data.budget.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [AddBudgetScreen].
 *
 * Additions vs the original:
 *  - [filteredCategories] — derived from query + master list, computed here not in the
 *    composable's `remember` block.
 *  - [isFormValid] — derived boolean, removes the `formValid` inline expression from
 *    the `bottomBar` slot.
 *  - [iconForCategory] — mapping from category name to drawable resource, previously
 *    a `when` block inside a `LazyColumn` item composable.
 */
@HiltViewModel
class AddBudgetViewModel @Inject constructor() : ViewModel() {

    // ── Raw inputs ────────────────────────────────────────────────────────────

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _amountInput = MutableStateFlow("")
    val amountInput: StateFlow<String> = _amountInput.asStateFlow()

    // ── Master category list ──────────────────────────────────────────────────

    private val _allCategories = MutableStateFlow(
        listOf(
            "Food", "Transport", "Bills", "Shopping", "Travel",
            "Health", "Education", "Entertainment", "Groceries", "Other",
        )
    )
    val categories: StateFlow<List<String>> = _allCategories.asStateFlow()

    // ── Derived: filtered list (was `remember(query, categories) { … }` in screen) ─

    val filteredCategories: StateFlow<List<String>> = combine(_query, _allCategories) { q, all ->
        val trimmed = q.trim().lowercase()
        if (trimmed.isEmpty()) all else all.filter { it.lowercase().contains(trimmed) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _allCategories.value)

    // ── Derived: form validity (was inline `val formValid = …` in bottomBar) ─

    val isFormValid: StateFlow<Boolean> = combine(_amountInput, _selectedCategory) { amt, cat ->
        amt.isNotBlank() && cat != null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── Events ────────────────────────────────────────────────────────────────

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
            BudgetRepository.addBudget(Budget(category = cat, amount = amt))
            onComplete()
        }
    }

    // ── Static mapping: category name → icon resource ─────────────────────────
    //    Was a `when` block inside a composable `LazyColumn` item — logic belongs here.

    @DrawableRes
    fun iconForCategory(category: String): Int = when (category.trim().lowercase()) {
        "food" -> R.drawable.food
        "transport" -> R.drawable.transport
        "bills" -> R.drawable.bills
        "shopping" -> R.drawable.shopping
        "travel" -> R.drawable.category_icon
        "health" -> R.drawable.health
        "education" -> R.drawable.category_icon
        "entertainment" -> R.drawable.entertainment
        "groceries" -> R.drawable.groceries
        else -> R.drawable.category_icon
    }
}