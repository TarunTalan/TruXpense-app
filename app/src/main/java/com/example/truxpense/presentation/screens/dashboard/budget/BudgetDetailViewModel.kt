package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.budget.Budget
import com.example.truxpense.data.repository.budget.BudgetRepository
import com.example.truxpense.data.repository.expense.ExpenseRepository
import com.example.truxpense.data.repository.expense.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── NOTE ─────────────────────────────────────────────────────────────────────
// If SpendPoint is defined in a separate models file, add the `tooltipDate`
// field there:
//   data class SpendPoint(val dayLabel: String, val amount: Double, val tooltipDate: String = "")
// The field is used by the interactive trend chart tooltip.
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class BudgetDetailViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {

    // ── Navigation args ───────────────────────────────────────────────────────

    private val _categoryName = MutableStateFlow("")
    val budgetName: StateFlow<String> = _categoryName.asStateFlow()

    // ── Period selection (Week / Month) ───────────────────────────────────────

    private val _selectedPeriod = MutableStateFlow(PeriodTab.WEEK)
    val selectedPeriod: StateFlow<PeriodTab> = _selectedPeriod.asStateFlow()

    fun setPeriod(tab: PeriodTab) {
        _selectedPeriod.value = tab
        _periodOffset.value = 0   // reset to current period when tab changes
    }

    // ── Period offset (0 = current, -1 = previous, etc.) ─────────────────────

    private val _periodOffset = MutableStateFlow(0)
    val periodOffset: StateFlow<Int> = _periodOffset.asStateFlow()

    fun goBack() {
        _periodOffset.value--
    }

    fun goForward() {
        if (_periodOffset.value < 0) _periodOffset.value++
    }

    // ── Derived navigation state ──────────────────────────────────────────────

    /** True when the user can navigate forward (i.e. not already at the current period). */
    val canGoForward: StateFlow<Boolean> =
        _periodOffset.map { it < 0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Human-readable label for the currently displayed period.
     * WEEK  → "1–7 Feb 2026"
     * MONTH → "February 2026"
     */
    val periodLabel: StateFlow<String> = combine(_selectedPeriod, _periodOffset) { period, offset ->
        val cal = java.util.Calendar.getInstance()
        when (period) {
            PeriodTab.WEEK -> {
                // Snap to Monday of the current week, then shift by `offset` weeks
                val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
                val daysToMon = (dow - java.util.Calendar.MONDAY + 7) % 7
                cal.add(java.util.Calendar.DAY_OF_YEAR, -daysToMon)   // now at Monday
                cal.add(java.util.Calendar.WEEK_OF_YEAR, offset)       // shift by offset
                val monDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
                val monMonth = monthAbbr(cal.get(java.util.Calendar.MONTH))
                val year = cal.get(java.util.Calendar.YEAR)
                cal.add(java.util.Calendar.DAY_OF_YEAR, 6)             // Sunday
                val sunDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
                val sunMonth = monthAbbr(cal.get(java.util.Calendar.MONTH))
                if (monMonth == sunMonth) "$monDay–$sunDay $monMonth $year"
                else "$monDay $monMonth – $sunDay $sunMonth $year"
            }

            PeriodTab.MONTH -> {
                cal.add(java.util.Calendar.MONTH, offset)
                val monthName = listOf(
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
                )[cal.get(java.util.Calendar.MONTH)]
                "$monthName ${cal.get(java.util.Calendar.YEAR)}"
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    // ── Live budget limit from Room ───────────────────────────────────────────

    val monthlyLimit: StateFlow<Double> = combine(_categoryName, budgetRepository.budgets) { cat, budgets ->
        budgets.firstOrNull { it.category == cat }?.amount ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    // ── Live spent amount from Room ───────────────────────────────────────────

    val spent: StateFlow<Double> = combine(_categoryName, expenseRepository.transactions) { cat, txs ->
        txs.filter { it.category == cat }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    // ── Derived ───────────────────────────────────────────────────────────────

    val progress: StateFlow<Float> = combine(monthlyLimit, spent) { limit, sp ->
        if (limit > 0) (sp / limit).toFloat().coerceIn(0f, 1f) else 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    val remaining: StateFlow<Double> = combine(monthlyLimit, spent) { limit, sp -> limit - sp }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0
    )

    // ── Transactions for this category ────────────────────────────────────────

    val transactions: StateFlow<List<BudgetTransaction>> =
        combine(_categoryName, expenseRepository.transactions) { cat, txs ->
            txs.filter { it.category == cat }.sortedByDescending { it.timestamp }.map { t ->
                BudgetTransaction(
                    id = t.id,
                    amount = t.amount,
                    type = "Expense",
                    addedFrom = "ADDED MANUALLY",
                    merchant = t.merchant,
                    category = t.category,
                    account = t.paymentMethod.ifBlank { "—" },
                    date = formatDate(t.timestamp),
                    time = formatTime(t.timestamp),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Spend chart points (period-aware + offset-aware) ──────────────────────

    /**
     * Produces chart points based on the selected [PeriodTab] and [periodOffset].
     *
     * • WEEK  → 7 daily buckets (Mon–Sun) for the target week.
     *           Each [SpendPoint.dayLabel] is short ("Mon") and
     *           [SpendPoint.tooltipDate] is long ("Mon 3 Feb").
     *
     * • MONTH → 4–5 weekly buckets for the target calendar month.
     *           Label is "W1"…"W5", tooltip is "1–7 Feb" etc.
     */
    val spendPoints: StateFlow<List<SpendPoint>> = combine(
        _categoryName, expenseRepository.transactions, _selectedPeriod, _periodOffset
    ) { cat, txs, period, offset ->
        val filtered = txs.filter { it.category == cat }
        when (period) {
            PeriodTab.WEEK -> buildWeekPoints(filtered, offset)
            PeriodTab.MONTH -> buildMonthPoints(filtered, offset)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Edit / Delete signals ─────────────────────────────────────────────────

    private val _deleteComplete = MutableStateFlow(false)
    val deleteComplete: StateFlow<Boolean> = _deleteComplete.asStateFlow()

    private val _updateComplete = MutableStateFlow(false)
    val updateComplete: StateFlow<Boolean> = _updateComplete.asStateFlow()

    // ── Actions ───────────────────────────────────────────────────────────────

    fun loadBudget(categoryName: String) {
        _categoryName.value = categoryName
    }

    fun updateBudgetLimit(newLimit: Double) {
        val cat = _categoryName.value.ifBlank { return }
        viewModelScope.launch {
            // Use the direct suspend DAO query — avoids Flow.firstOrNull() race
            // where no subscriber is active and null is returned, causing a new UUID.
            val existing = budgetRepository.getBudgetByCategory(cat)
            val updated = existing?.copy(amount = newLimit)
                ?: Budget(category = cat, amount = newLimit)
            budgetRepository.addBudget(updated)
            _updateComplete.value = true
        }
    }

    fun resetUpdateComplete() {
        _updateComplete.value = false
    }

    fun deleteBudget() {
        val cat = _categoryName.value.ifBlank { return }
        viewModelScope.launch {
            val budget = budgetRepository.getBudgetByCategory(cat)
            if (budget != null) budgetRepository.deleteBudget(budget.id)
            _deleteComplete.value = true
        }
    }

    fun resetDeleteComplete() {
        _deleteComplete.value = false
    }

    // ── Chart computation helpers ─────────────────────────────────────────────

    /**
     * Builds 7 [SpendPoint] values (Mon→Sun) for the week identified by [weekOffset].
     * weekOffset = 0 → current week; -1 → last week; etc.
     */
    private fun buildWeekPoints(
        txs: List<Transaction>,
        weekOffset: Int,
    ): List<SpendPoint> {
        val cal = java.util.Calendar.getInstance()
        // Snap to Monday of the current week
        val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val daysToMon = (dow - java.util.Calendar.MONDAY + 7) % 7
        cal.add(java.util.Calendar.DAY_OF_YEAR, -daysToMon)
        cal.add(java.util.Calendar.WEEK_OF_YEAR, weekOffset)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val weekStart = cal.timeInMillis   // Monday 00:00

        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val buckets = DoubleArray(7)

        txs.forEach { t ->
            val dayIndex = ((t.timestamp - weekStart) / (24L * 60 * 60 * 1_000)).toInt()
            if (dayIndex in 0..6) buckets[dayIndex] += t.amount
        }

        return dayNames.mapIndexed { i, name ->
            val dayCal = java.util.Calendar.getInstance().apply { timeInMillis = weekStart + i * 24L * 60 * 60 * 1_000 }
            val dayNum = dayCal.get(java.util.Calendar.DAY_OF_MONTH)
            val mon = monthAbbr(dayCal.get(java.util.Calendar.MONTH))
            SpendPoint(dayLabel = name, amount = buckets[i], tooltipDate = "$name $dayNum $mon")
        }
    }

    /**
     * Builds 4–5 weekly [SpendPoint] values for the calendar month identified by [monthOffset].
     * monthOffset = 0 → current month; -1 → last month; etc.
     */
    private fun buildMonthPoints(
        txs: List<Transaction>,
        monthOffset: Int,
    ): List<SpendPoint> {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.MONTH, monthOffset)
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)

        val year = cal.get(java.util.Calendar.YEAR)
        val month = cal.get(java.util.Calendar.MONTH)
        val monAbbr = monthAbbr(month)
        val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        val monthStartMs = cal.timeInMillis

        // Split month into chunks of 7 days: 1–7, 8–14, 15–21, 22–28, 29–end
        val weekBoundaries = mutableListOf<Pair<Int, Int>>() // (startDay, endDay) 1-based
        var d = 1
        while (d <= daysInMonth) {
            val end = minOf(d + 6, daysInMonth)
            weekBoundaries.add(d to end)
            d += 7
        }

        val buckets = DoubleArray(weekBoundaries.size)
        txs.forEach { t ->
            val txCal = java.util.Calendar.getInstance().apply { timeInMillis = t.timestamp }
            if (txCal.get(java.util.Calendar.YEAR) == year && txCal.get(java.util.Calendar.MONTH) == month) {
                val day = txCal.get(java.util.Calendar.DAY_OF_MONTH)
                val idx = weekBoundaries.indexOfFirst { (s, e) -> day in s..e }
                if (idx >= 0) buckets[idx] += t.amount
            }
        }

        return weekBoundaries.mapIndexed { i, (startDay, endDay) ->
            val label = "W${i + 1}"
            val tooltip = if (startDay == endDay) "$startDay $monAbbr"
            else "$startDay–$endDay $monAbbr"
            SpendPoint(dayLabel = label, amount = buckets[i], tooltipDate = tooltip)
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun monthAbbr(month: Int) =
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[month]

    private fun formatDate(ts: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
        return "${cal.get(java.util.Calendar.DAY_OF_MONTH)} ${monthAbbr(cal.get(java.util.Calendar.MONTH))} ${
            cal.get(
                java.util.Calendar.YEAR
            )
        }"
    }

    private fun formatTime(ts: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
        val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val m = cal.get(java.util.Calendar.MINUTE)
        val ampm = if (h < 12) "AM" else "PM"
        val hour = if (h % 12 == 0) 12 else h % 12
        return "${hour}:${m.toString().padStart(2, '0')} $ampm"
    }
}