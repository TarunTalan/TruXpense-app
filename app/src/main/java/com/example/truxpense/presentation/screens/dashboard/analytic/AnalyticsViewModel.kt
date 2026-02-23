package com.example.truxpense.presentation.screens.dashboard.analytic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.budget.BudgetRepository
import com.example.truxpense.data.repository.dashboard.RepositoryProvider
import com.example.truxpense.presentation.screens.dashboard.budget.budgetColorForCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

    private val repo = RepositoryProvider.expenseRepository

    init {
        // Combine transactions and budgets so analytics reflect both sources
        viewModelScope.launch {
            combine(repo.transactions, BudgetRepository.budgets) { txs, budgets ->
                Pair(txs, budgets)
            }.collect { (txs, budgets) ->
                // categories aggregated from transactions
                val grouped = txs.groupBy { it.category }
                val cats = grouped.entries.map { (cat, items) ->
                    val amt = items.sumOf { it.amount }
                    CategorySpend(cat, amt, budgetColorForCategory(cat))
                }.sortedByDescending { it.amount }
                _categories.value = cats

                // totals
                _totalSpent.value = txs.sumOf { it.amount }
                _totalBudget.value = budgets.sumOf { it.amount }

                // trend points (simple chunking logic)
                _trendMonth.value = if (txs.isEmpty()) emptyList() else {
                    val per = (txs.size + 4) / 5
                    txs.chunked(per).mapIndexed { idx, ch -> TrendPoint("${(idx + 1) * 7}", ch.sumOf { it.amount }) }
                }

                _trendWeek.value = if (txs.isEmpty()) emptyList() else {
                    val per = (txs.size + 6) / 7
                    txs.chunked(per).mapIndexed { idx, ch -> TrendPoint(listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")[idx.coerceAtMost(6)], ch.sumOf { it.amount }) }
                }
            }
        }
    }
}
