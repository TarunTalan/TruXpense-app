package com.example.truxpense.presentation.screens.dashboard.analytic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.dashboard.BudgetRepository
import com.example.truxpense.data.repository.dashboard.ExpenseRepository
import com.example.truxpense.presentation.screens.dashboard.budget.budgetColorForCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class CategorySpend(val name: String, val amount: Double, val color: androidx.compose.ui.graphics.Color)
data class TrendPoint(val label: String, val amount: Double)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {

    // ── Category breakdown ────────────────────────────────────────────────────

    val categories: StateFlow<List<CategorySpend>> =
        expenseRepository.transactions.map { txs ->
            txs.groupBy { it.category }
                .entries
                .map { (cat, items) ->
                    CategorySpend(cat, items.sumOf { it.amount }, budgetColorForCategory(cat))
                }
                .sortedByDescending { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Totals ────────────────────────────────────────────────────────────────

    val totalSpent: StateFlow<Double> =
        expenseRepository.transactions.map { it.sumOf { t -> t.amount } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val totalBudget: StateFlow<Double> =
        budgetRepository.budgets.map { it.sumOf { b -> b.amount } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val budgetUtilisation: StateFlow<Float> =
        combine(totalBudget, totalSpent) { budget, spent ->
            if (budget > 0) (spent / budget).toFloat().coerceIn(0f, 1f) else 0f
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    // ── Trend — monthly (chunked into 5 "weeks") ──────────────────────────────

    val trendMonth: StateFlow<List<TrendPoint>> =
        expenseRepository.transactions.map { txs ->
            if (txs.isEmpty()) return@map emptyList()
            val sorted = txs.sortedBy { it.timestamp }
            val perChunk = maxOf(1, (sorted.size + 4) / 5)
            sorted.chunked(perChunk).mapIndexed { idx, chunk ->
                TrendPoint("${(idx + 1) * 7}d", chunk.sumOf { it.amount })
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Trend — weekly (chunked into 7 days) ─────────────────────────────────

    val trendWeek: StateFlow<List<TrendPoint>> =
        expenseRepository.transactions.map { txs ->
            if (txs.isEmpty()) return@map emptyList()
            // Bucket each transaction into its day-of-week
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val buckets = MutableList(7) { 0.0 }
            val cal = java.util.Calendar.getInstance()
            txs.forEach { t ->
                cal.timeInMillis = t.timestamp
                val dow = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7  // Mon=0
                buckets[dow] += t.amount
            }
            days.mapIndexed { i, label -> TrendPoint(label, buckets[i]) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Top merchant (insight) ────────────────────────────────────────────────

    val topMerchant: StateFlow<Pair<String, Double>?> =
        expenseRepository.transactions.map { txs ->
            txs.groupBy { it.merchant }
                .mapValues { (_, items) -> items.sumOf { it.amount } }
                .entries
                .maxByOrNull { it.value }
                ?.let { it.key to it.value }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── Category with highest spend (insight) ─────────────────────────────────

    val topCategory: StateFlow<Pair<String, Double>?> =
        categories.map { cats -> cats.firstOrNull()?.let { it.name to it.amount } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}