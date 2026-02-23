package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.dashboard.BudgetRepository
import com.example.truxpense.data.repository.dashboard.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetDetailViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {

    // ── Navigation args (set from screen once the nav arg is received) ────────

    private val _categoryName = MutableStateFlow("")
    val budgetName: StateFlow<String> = _categoryName.asStateFlow()

    // ── Live budget limit from Room ───────────────────────────────────────────

    val monthlyLimit: StateFlow<Double> =
        combine(_categoryName, budgetRepository.budgets) { cat, budgets ->
            budgets.firstOrNull { it.category == cat }?.amount ?: 0.0
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    // ── Live spent amount from Room (only matching category) ──────────────────

    val spent: StateFlow<Double> =
        combine(_categoryName, expenseRepository.transactions) { cat, txs ->
            txs.filter { it.category == cat }.sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    // ── Budget progress ───────────────────────────────────────────────────────

    val progress: StateFlow<Float> =
        combine(monthlyLimit, spent) { limit, sp ->
            if (limit > 0) (sp / limit).toFloat().coerceIn(0f, 1f) else 0f
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    val remaining: StateFlow<Double> =
        combine(monthlyLimit, spent) { limit, sp -> limit - sp }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    // ── Transactions for this category (for the detail list) ──────────────────

    val transactions: StateFlow<List<BudgetTransaction>> =
        combine(_categoryName, expenseRepository.transactions) { cat, txs ->
            txs.filter { it.category == cat }
                .sortedByDescending { it.timestamp }
                .map { t ->
                    BudgetTransaction(
                        id        = t.id,
                        amount    = t.amount,
                        type      = "Expense",
                        addedFrom = "ADDED MANUALLY",
                        merchant  = t.merchant,
                        category  = t.category,
                        account   = t.paymentMethod.ifBlank { "—" },
                        date      = formatDate(t.timestamp),
                        time      = formatTime(t.timestamp),
                    )
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Spend chart points (one per day across last 7 days) ───────────────────

    val spendPoints: StateFlow<List<SpendPoint>> =
        combine(_categoryName, expenseRepository.transactions) { cat, txs ->
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val cal = java.util.Calendar.getInstance()
            // Zero-initialise all 7 buckets
            val buckets = MutableList(7) { 0.0 }
            val now = System.currentTimeMillis()
            txs.filter { it.category == cat }
                .filter { now - it.timestamp < 7L * 24 * 60 * 60 * 1_000 }
                .forEach { t ->
                    cal.timeInMillis = t.timestamp
                    // Calendar.DAY_OF_WEEK: 1=Sun … 7=Sat  →  map to Mon=0 … Sun=6
                    val dow = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
                    buckets[dow] += t.amount
                }
            buckets.mapIndexed { i, amt -> SpendPoint(days[i], amt) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Load (called from NavBackStackEntry / screen init) ────────────────────

    fun loadBudget(categoryName: String) {
        _categoryName.value = categoryName
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun formatDate(ts: Long): String {
        val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
        return "${cal.get(java.util.Calendar.DAY_OF_MONTH)} ${months[cal.get(java.util.Calendar.MONTH)]} ${cal.get(java.util.Calendar.YEAR)}"
    }

    private fun formatTime(ts: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
        val h   = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val m   = cal.get(java.util.Calendar.MINUTE)
        val ampm = if (h < 12) "AM" else "PM"
        val hour = if (h % 12 == 0) 12 else h % 12
        return "${hour}:${m.toString().padStart(2, '0')} $ampm"
    }
}