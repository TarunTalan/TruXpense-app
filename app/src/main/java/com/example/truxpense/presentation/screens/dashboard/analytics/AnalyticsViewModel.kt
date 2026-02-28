package com.example.truxpense.presentation.screens.dashboard.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.dashboard.BudgetRepository
import com.example.truxpense.data.repository.dashboard.ExpenseRepository
import com.example.truxpense.data.repository.dashboard.Transaction
import com.example.truxpense.presentation.screens.dashboard.budget.budgetColorForCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

// ── UI models ─────────────────────────────────────────────────────────────────

data class CategorySpend(
    val name: String,
    val amount: Double,
    val color: androidx.compose.ui.graphics.Color,
)

/**
 * A single point on the trend chart.
 * [label]       — x-axis label (e.g. "Mon", "W1", "Jan")
 * [amount]      — total spend for this bucket
 * [tooltipDate] — human-readable date/range for press-and-hold tooltip
 */
data class TrendPoint(
    val label: String,
    val amount: Double,
    val tooltipDate: String = label,
)

// ── Period selector ───────────────────────────────────────────────────────────

enum class AnalyticsPeriod { WEEK, MONTH, YEAR }

// ── Analytics state ───────────────────────────────────────────────────────────

data class AnalyticsUiState(
    val period: AnalyticsPeriod = AnalyticsPeriod.MONTH,
    val offset: Int = 0,
    val periodLabel: String = "",
    val canGoBack: Boolean = true,
    val canGoForward: Boolean = false,
    val totalSpent: Double = 0.0,
    val totalBudget: Double = 0.0,
    /** +N → spent more than previous period, -N → spent less, 0 → no previous data */
    val changePercent: Int = 0,
    val hasComparison: Boolean = false,
    val categories: List<CategorySpend> = emptyList(),
    val trendPoints: List<TrendPoint> = emptyList(),
    val topMerchant: Pair<String, Double>? = null,
    val topCategory: Pair<String, Double>? = null,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {

    private val _period = MutableStateFlow(AnalyticsPeriod.MONTH)
    private val _offset = MutableStateFlow(0)

    val selectedPeriod: StateFlow<AnalyticsPeriod> = _period.asStateFlow()

    fun setPeriod(p: AnalyticsPeriod) {
        _period.value = p; _offset.value = 0
    }

    fun goBack() {
        _offset.value--
    }

    fun goForward() {
        if (_offset.value < 0) _offset.value++
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        expenseRepository.transactions,
        budgetRepository.budgets,
        _period,
        _offset,
    ) { txs, budgets, period, offset ->

        val now = Calendar.getInstance()
        val (winStart, winEnd) = periodWindow(period, offset, now)
        val duration = winEnd - winStart
        val prevEnd = winStart - 1L
        val prevStart = prevEnd - duration

        val current = txs.filter { it.timestamp in winStart..winEnd }
        val previous = txs.filter { it.timestamp in prevStart..prevEnd }

        val totalSpent = current.sumOf { it.amount }
        val prevSpent = previous.sumOf { it.amount }
        val totalBudget = budgets.sumOf { it.amount }
        val changePercent = if (prevSpent > 0) (((totalSpent - prevSpent) / prevSpent) * 100).toInt() else 0

        val categories = current.groupBy { it.category }
            .map { (cat, items) -> CategorySpend(cat, items.sumOf { it.amount }, budgetColorForCategory(cat)) }
            .sortedByDescending { it.amount }

        val trendPoints = buildTrendPoints(current, period, offset, now)

        val topMerchant =
            current.groupBy { it.merchant }.mapValues { (_, v) -> v.sumOf { it.amount } }.maxByOrNull { it.value }
                ?.let { it.key to it.value }

        val topCategory = categories.firstOrNull()?.let { it.name to it.amount }

        AnalyticsUiState(
            period = period,
            offset = offset,
            periodLabel = periodLabel(period, offset, now),
            canGoBack = offset > -24,
            canGoForward = offset < 0,
            totalSpent = totalSpent,
            totalBudget = totalBudget,
            changePercent = changePercent,
            hasComparison = prevSpent > 0,
            categories = categories,
            trendPoints = trendPoints,
            topMerchant = topMerchant,
            topCategory = topCategory,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    // ── Period window ─────────────────────────────────────────────────────────

    private fun periodWindow(
        period: AnalyticsPeriod, offset: Int, now: Calendar
    ): Pair<Long, Long> {
        val cal = now.clone() as Calendar
        return when (period) {
            AnalyticsPeriod.WEEK -> {
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                val toMon = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
                cal.add(Calendar.DAY_OF_YEAR, -toMon + (offset * 7))
                cal.zeroTime()
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 7)
                start to cal.timeInMillis - 1L
            }

            AnalyticsPeriod.MONTH -> {
                cal.add(Calendar.MONTH, offset)
                cal.set(Calendar.DAY_OF_MONTH, 1); cal.zeroTime()
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                start to cal.timeInMillis - 1L
            }

            AnalyticsPeriod.YEAR -> {
                cal.add(Calendar.YEAR, offset)
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1); cal.zeroTime()
                val start = cal.timeInMillis
                cal.add(Calendar.YEAR, 1)
                start to cal.timeInMillis - 1L
            }
        }
    }

    // ── Trend points ──────────────────────────────────────────────────────────

    private fun buildTrendPoints(
        txs: List<Transaction>,
        period: AnalyticsPeriod,
        offset: Int,
        now: Calendar,
    ): List<TrendPoint> = when (period) {

        // WEEK — 7 daily buckets, Mon → Sun
        AnalyticsPeriod.WEEK -> {
            val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val cal = now.clone() as Calendar
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val toMon = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
            cal.add(Calendar.DAY_OF_YEAR, -toMon + (offset * 7)); cal.zeroTime()

            (0..6).map { i ->
                val dayStart = cal.timeInMillis
                val dayEnd = dayStart + 86_399_999L
                val dateStr = "${cal.get(Calendar.DAY_OF_MONTH)} ${monthShort(cal.get(Calendar.MONTH))}"
                val amt = txs.filter { it.timestamp in dayStart..dayEnd }.sumOf { it.amount }
                cal.add(Calendar.DAY_OF_YEAR, 1)
                TrendPoint(label = labels[i], amount = amt, tooltipDate = "${labels[i]}, $dateStr")
            }
        }

        // MONTH — weekly buckets W1..W4/W5
        AnalyticsPeriod.MONTH -> {
            val cal = now.clone() as Calendar
            cal.add(Calendar.MONTH, offset)
            cal.set(Calendar.DAY_OF_MONTH, 1); cal.zeroTime()
            val monthStart = cal.timeInMillis
            val monthYear = "${monthFull(cal.get(Calendar.MONTH))} ${cal.get(Calendar.YEAR)}"
            cal.add(Calendar.MONTH, 1)
            val monthEnd = cal.timeInMillis - 1L

            val points = mutableListOf<TrendPoint>()
            val cursor = (now.clone() as Calendar).apply { timeInMillis = monthStart }
            var week = 1
            while (cursor.timeInMillis <= monthEnd) {
                val bStart = cursor.timeInMillis
                val d1 = cursor.get(Calendar.DAY_OF_MONTH)
                cursor.add(Calendar.DAY_OF_YEAR, 7)
                val bEnd = minOf(cursor.timeInMillis - 1L, monthEnd)
                val d2 = (now.clone() as Calendar).apply { timeInMillis = bEnd }.get(Calendar.DAY_OF_MONTH)
                val amt = txs.filter { it.timestamp in bStart..bEnd }.sumOf { it.amount }
                points.add(
                    TrendPoint(
                        label = "W$week",
                        amount = amt,
                        tooltipDate = "$d1–$d2 $monthYear",
                    )
                )
                week++
            }
            points
        }

        // YEAR — 12 monthly buckets
        AnalyticsPeriod.YEAR -> {
            val cal = now.clone() as Calendar
            cal.add(Calendar.YEAR, offset)
            val year = cal.get(Calendar.YEAR)
            cal.set(Calendar.MONTH, Calendar.JANUARY)
            cal.set(Calendar.DAY_OF_MONTH, 1); cal.zeroTime()

            (0..11).map { m ->
                cal.set(Calendar.MONTH, m)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                val end = cal.timeInMillis - 1L
                val amt = txs.filter { it.timestamp in start..end }.sumOf { it.amount }
                TrendPoint(
                    label = monthShort(m),
                    amount = amt,
                    tooltipDate = "${monthFull(m)} $year",
                )
            }
        }
    }

    // ── Period label ──────────────────────────────────────────────────────────

    private fun periodLabel(period: AnalyticsPeriod, offset: Int, now: Calendar): String {
        val cal = now.clone() as Calendar
        return when (period) {
            AnalyticsPeriod.WEEK -> {
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                val toMon = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
                cal.add(Calendar.DAY_OF_YEAR, -toMon + (offset * 7))
                val d1 = cal.get(Calendar.DAY_OF_MONTH);
                val m1 = monthShort(cal.get(Calendar.MONTH))
                cal.add(Calendar.DAY_OF_YEAR, 6)
                val d2 = cal.get(Calendar.DAY_OF_MONTH);
                val m2 = monthShort(cal.get(Calendar.MONTH))
                if (m1 == m2) "$d1–$d2 $m1" else "$d1 $m1 – $d2 $m2"
            }

            AnalyticsPeriod.MONTH -> {
                cal.add(Calendar.MONTH, offset)
                "${monthFull(cal.get(Calendar.MONTH))} ${cal.get(Calendar.YEAR)}"
            }

            AnalyticsPeriod.YEAR -> {
                cal.add(Calendar.YEAR, offset)
                "${cal.get(Calendar.YEAR)}"
            }
        }
    }

    // ── Util ──────────────────────────────────────────────────────────────────

    private fun Calendar.zeroTime() {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    private fun monthShort(m: Int) =
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[m]

    private fun monthFull(m: Int) = listOf(
        "January",
        "February",
        "March",
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December"
    )[m]
}