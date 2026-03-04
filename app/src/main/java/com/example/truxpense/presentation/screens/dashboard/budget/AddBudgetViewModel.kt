package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.R
import com.example.truxpense.data.repository.budget.Budget
import com.example.truxpense.data.repository.budget.BudgetRepository
import com.example.truxpense.presentation.utils.AppCategories
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddBudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
) : ViewModel() {

    // ── Raw inputs ────────────────────────────────────────────────────────────

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _amountInput = MutableStateFlow("")
    val amountInput: StateFlow<String> = _amountInput.asStateFlow()

    // ── Master category list ──────────────────────────────────────────────────

    private val allCategories = AppCategories.all

    // ── Already-budgeted categories (live from Room) ──────────────────────────

    // made public so UI can observe and pass to CategoryDropdown
    val existingBudgetedCategories: StateFlow<Set<String>> =
        budgetRepository.budgets
            .map { list -> list.map { it.category.trim().lowercase() }.toSet() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    // ── Available categories = master list minus already-budgeted ones ─────────

    val categories: StateFlow<List<String>> =
        existingBudgetedCategories
            .map { budgeted ->
                allCategories.filter { it.trim().lowercase() !in budgeted }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, allCategories)

    // ── Duplicate guard: true when selected category already has a budget ──────

    val isDuplicateCategory: StateFlow<Boolean> =
        combine(_selectedCategory, existingBudgetedCategories) { selected, budgeted ->
            selected != null && selected.trim().lowercase() in budgeted
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── Derived: filtered list ────────────────────────────────────────────────

    val filteredCategories: StateFlow<List<String>> =
        combine(_query, categories) { q, available ->
            val trimmed = q.trim().lowercase()
            if (trimmed.isEmpty()) available else available.filter { it.lowercase().contains(trimmed) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, allCategories)

    // ── Derived: form validity ────────────────────────────────────────────────

    val isFormValid: StateFlow<Boolean> =
        combine(_amountInput, _selectedCategory, isDuplicateCategory) { amt, cat, isDup ->
            amt.isNotBlank() && (amt.toDoubleOrNull() ?: 0.0) > 0.0 && cat != null && !isDup
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── Events ────────────────────────────────────────────────────────────────

    fun setQuery(q: String) { _query.value = q }
    fun setSelected(cat: String?) { _selectedCategory.value = cat }
    fun setAmountInput(v: String) { _amountInput.value = v }

    /** Persists the new budget to Room then calls [onComplete]. Silently guards duplicates. */
    fun createBudget(onComplete: () -> Unit) {
        val cat = _selectedCategory.value ?: return
        val amt = _amountInput.value.toDoubleOrNull() ?: return
        // Double-check at save time in case of a race condition
        if (isDuplicateCategory.value) return
        viewModelScope.launch {
            budgetRepository.addBudget(Budget(category = cat, amount = amt))
            onComplete()
        }
    }

    // ── Static mapping: category name → icon resource ─────────────────────────

    @DrawableRes
    fun iconForCategory(category: String): Int = when (category.trim().lowercase()) {
        "food"          -> R.drawable.food
        "transport"     -> R.drawable.transport
        "bills"         -> R.drawable.bills
        "shopping"      -> R.drawable.shopping
        "travel"        -> R.drawable.category_icon
        "health"        -> R.drawable.health
        "education"     -> R.drawable.category_icon
        "entertainment" -> R.drawable.entertainment
        "groceries"     -> R.drawable.groceries
        else            -> R.drawable.category_icon
    }
}
