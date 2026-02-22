package com.example.truxpense.presentation.screens.dashboard.analytic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.budget.BudgetRepository
import com.example.truxpense.presentation.screens.dashboard.budget.budgetColorForCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor() : ViewModel() {
    private val _categories = MutableStateFlow<List<CategorySpend>>(emptyList())
    val categories: StateFlow<List<CategorySpend>> = _categories

    private val _trendMonth = MutableStateFlow<List<TrendPoint>>(emptyList())
    val trendMonth: StateFlow<List<TrendPoint>> = _trendMonth

    private val _trendWeek = MutableStateFlow<List<TrendPoint>>(emptyList())
    val trendWeek: StateFlow<List<TrendPoint>> = _trendWeek

    private val _totalSpent = MutableStateFlow(0.0)
    val totalSpent: StateFlow<Double> = _totalSpent

    private val _totalBudget = MutableStateFlow(0.0)
    val totalBudget: StateFlow<Double> = _totalBudget

    init {
        // initialize from repository (simple mapping)
        viewModelScope.launch {
            val budgets = BudgetRepository.budgets
            // compute categories aggregate
            budgets.collect { list ->
                val grouped = list.groupBy { it.category }
                val cats = grouped.entries.map { (cat, items) ->
                    val amt = items.sumOf { it.amount }
                    CategorySpend(cat, amt, budgetColorForCategory(cat))
                }
                _categories.value = cats
                _totalSpent.value = cats.sumOf { it.amount }
                _totalBudget.value = list.sumOf { it.amount }

                // simple synthetic trends for demo: monthly points per added budget
                _trendMonth.value = list.mapIndexed { idx, b -> TrendPoint("${(idx + 1) * 7}", b.amount) }
                _trendWeek.value = list.mapIndexed { idx, b -> TrendPoint("Day${idx + 1}", b.amount / 4.0) }
            }
        }
    }
}
