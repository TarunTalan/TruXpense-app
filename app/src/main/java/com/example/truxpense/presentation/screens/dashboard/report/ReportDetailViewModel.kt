package com.example.truxpense.presentation.screens.dashboard.report

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.expense.ExpenseRepository
import com.example.truxpense.data.repository.income.IncomeRepository
import com.example.truxpense.data.repository.report.ReportCategoryRow
import com.example.truxpense.data.repository.report.ReportRepository
import com.example.truxpense.data.repository.report.ReportTransactionRow
import com.example.truxpense.data.repository.report.ReportTrendPoint
import com.example.truxpense.data.repository.report.ReportType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// ── Granularity of the trend chart ────────────────────────────────────────────

enum class ReportTrendGranularity { DAILY, WEEKLY, MONTHLY }

// ── UI state ──────────────────────────────────────────────────────────────────

data class ReportDetailUiState(
    // ── Metadata ──────────────────────────────────────────────────────────────
    val title: String = "",
    val dateRangeLabel: String = "",        // e.g. "1 Jan – 31 Mar 2026"
    val reportType: ReportType = ReportType.EXPENSE,

    // ── Totals ────────────────────────────────────────────────────────────────
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netAmount: Double = 0.0,            // income − expenses (positive = surplus)
    val transactionCount: Int = 0,

    // ── Category breakdown ────────────────────────────────────────────────────
    val categoryRows: List<ReportCategoryRow> = emptyList(),

    // ── Trend chart ───────────────────────────────────────────────────────────
    val trendGranularity: ReportTrendGranularity = ReportTrendGranularity.WEEKLY,
    val trendPoints: List<ReportTrendPoint> = emptyList(),

    // ── Transaction list ──────────────────────────────────────────────────────
    val transactions: List<ReportTransactionRow> = emptyList(),

    // ── Top stats ─────────────────────────────────────────────────────────────
    val topMerchant: Pair<String, Double>? = null,      // name → total
    val topCategory: Pair<String, Double>? = null,      // category → total
    val avgDailySpend: Double = 0.0,

    // ── Async guards ─────────────────────────────────────────────────────────
    /** True once Room has emitted the first value — prevents the blank-screen flash. */
    val isLoaded: Boolean = false,
    val notFound: Boolean = false,

    // ── Delete ────────────────────────────────────────────────────────────────
    val deleteComplete: Boolean = false,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ReportDetailViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
) : ViewModel() {

    // ── Navigation arg ────────────────────────────────────────────────────────

    private val _reportId = MutableStateFlow<String?>(null)

    // ── Trend granularity (user can toggle on the screen) ─────────────────────

    private val _granularity = MutableStateFlow(ReportTrendGranularity.WEEKLY)
    val granularity: StateFlow<ReportTrendGranularity> = _granularity.asStateFlow()

    fun setGranularity(g: ReportTrendGranularity) {
        _granularity.value = g
    }

    // ── Main UI state ─────────────────────────────────────────────────────────

    val uiState: StateFlow<ReportDetailUiState> = combine(
        _reportId,
        expenseRepository.transactions,
        incomeRepository.allIncome,
        _granularity,
    ) { reportId, allExpenses, allIncomes, granularity ->

        // Guard: ID not set yet
        val id = reportId ?: return@combine ReportDetailUiState()

        // Load the report config from Room
        val report = reportRepository.getReportByIdOnce(id)
            ?: return@combine ReportDetailUiState(isLoaded = true, notFound = true)

        val type       = report.parsedType()
        val filterCats = report.parsedCategories()  // empty → all categories

        // ── Filter transactions to the report window ──────────────────────────
        fun inWindow(ts: Long) = ts in report.fromDate..report.toDate
        fun inCategories(cat: String) = filterCats.isEmpty() || cat in filterCats

        val expenses = allExpenses.filter { inWindow(it.timestamp) && inCategories(it.category) }
        val incomes  = allIncomes.filter  { inWindow(it.timestamp) && inCategories(it.source) }

        val showExpenses = type == ReportType.EXPENSE || type == ReportType.ALL
        val showIncome   = type == ReportType.INCOME  || type == ReportType.ALL

        val usedExpenses = if (showExpenses) expenses else emptyList()
        val usedIncomes  = if (showIncome)  incomes  else emptyList()

        val totalExpenses = usedExpenses.sumOf { it.amount }
        val totalIncome   = usedIncomes.sumOf  { it.amount }
        val netAmount     = totalIncome - totalExpenses

        // ── Date range label ──────────────────────────────────────────────────
        val dateRangeLabel = formatDateRange(report.fromDate, report.toDate)

        // ── Category breakdown ────────────────────────────────────────────────
        val categoryRows = buildCategoryRows(
            expenses      = usedExpenses,
            incomes       = usedIncomes,
            type          = type,
            totalExpenses = totalExpenses,
            totalIncome   = totalIncome,
        )

        // ── Trend chart ───────────────────────────────────────────────────────
        val trendPoints = buildTrendPoints(
            expenses    = usedExpenses.map { it.timestamp to it.amount },
            incomes     = usedIncomes.map  { it.timestamp to it.amount },
            type        = type,
            fromDate    = report.fromDate,
            toDate      = report.toDate,
            granularity = granularity,
        )

        // ── Transaction rows ──────────────────────────────────────────────────
        val expenseRows = usedExpenses.map { t ->
            ReportTransactionRow(
                id            = t.id,
                merchant      = t.merchant,
                category      = t.category,
                amount        = t.amount,
                isExpense     = true,
                dateLabel     = formatDate(t.timestamp),
                timeLabel     = formatTime(t.timestamp),
                paymentMethod = t.paymentMethod.ifBlank { "—" },
            )
        }
        val incomeRows = usedIncomes.map { i ->
            ReportTransactionRow(
                id            = i.id,
                merchant      = i.source,
                category      = i.source,
                amount        = i.amount,
                isExpense     = false,
                dateLabel     = formatDate(i.timestamp),
                timeLabel     = formatTime(i.timestamp),
                paymentMethod = "—",
            )
        }
        val transactions = (expenseRows + incomeRows).sortedByDescending { row ->
            usedExpenses.firstOrNull { it.id == row.id }?.timestamp
                ?: usedIncomes.firstOrNull { it.id == row.id }?.timestamp ?: 0L
        }

        // ── Top merchant / category ───────────────────────────────────────────
        val topMerchant = usedExpenses
            .groupBy { it.merchant }
            .mapValues { e -> e.value.sumOf { it.amount } }
            .maxByOrNull { it.value }
            ?.let { it.key to it.value }

        val topCategory = buildTopCategory(
            expenses = usedExpenses,
            incomes  = usedIncomes,
            type     = type,
        )

        // ── Average daily spend ───────────────────────────────────────────────
        val days = ((report.toDate - report.fromDate) / (24L * 60 * 60 * 1_000)).toInt().coerceAtLeast(1)
        val avgDailySpend = totalExpenses / days

        ReportDetailUiState(
            title             = report.title,
            dateRangeLabel    = dateRangeLabel,
            reportType        = type,
            totalExpenses     = totalExpenses,
            totalIncome       = totalIncome,
            netAmount         = netAmount,
            transactionCount  = transactions.size,
            categoryRows      = categoryRows,
            trendGranularity  = granularity,
            trendPoints       = trendPoints,
            transactions      = transactions,
            topMerchant       = topMerchant,
            topCategory       = topCategory,
            avgDailySpend     = avgDailySpend,
            isLoaded          = true,
            notFound          = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportDetailUiState())

    // ── Actions ───────────────────────────────────────────────────────────────

    /** Must be called by the composable as soon as the reportId nav-arg is available. */
    fun loadReport(reportId: String) {
        _reportId.value = reportId
    }

    fun deleteReport() {
        val id = _reportId.value ?: return
        viewModelScope.launch {
            reportRepository.deleteReport(id)
            // Signal navigation-back via uiState.deleteComplete
            // We update the shared flow indirectly by emitting into a separate flag below.
            _deleteComplete.value = true
        }
    }

    // ── Delete completion flag (not part of the main combine to avoid races) ──

    private val _deleteComplete = MutableStateFlow(false)
    val deleteComplete: StateFlow<Boolean> = _deleteComplete.asStateFlow()

    fun onDeleteHandled() { _deleteComplete.value = false }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Build per-category rows with share and colour. */
    private fun buildCategoryRows(
        expenses: List<com.example.truxpense.data.repository.expense.Transaction>,
        incomes: List<com.example.truxpense.data.repository.income.Income>,
        type: ReportType,
        totalExpenses: Double,
        totalIncome: Double,
    ): List<ReportCategoryRow> {
        val rows = mutableListOf<ReportCategoryRow>()
        val grandTotal = when (type) {
            ReportType.EXPENSE -> totalExpenses
            ReportType.INCOME  -> totalIncome
            ReportType.ALL     -> totalExpenses + totalIncome
        }
        if (grandTotal <= 0) return emptyList()

        if (type == ReportType.EXPENSE || type == ReportType.ALL) {
            expenses.groupBy { it.category }
                .mapValues { e -> e.value.sumOf { it.amount } }
                .entries.sortedByDescending { it.value }
                .forEachIndexed { idx, (cat, amount) ->
                    rows.add(
                        ReportCategoryRow(
                            name   = cat,
                            amount = amount,
                            share  = (amount / grandTotal).toFloat().coerceIn(0f, 1f),
                            color  = paletteColor(idx),
                        )
                    )
                }
        }

        if (type == ReportType.INCOME || type == ReportType.ALL) {
            incomes.groupBy { it.source }
                .mapValues { e -> e.value.sumOf { it.amount } }
                .entries.sortedByDescending { it.value }
                .forEachIndexed { idx, (src, amount) ->
                    rows.add(
                        ReportCategoryRow(
                            name   = src,
                            amount = amount,
                            share  = (amount / grandTotal).toFloat().coerceIn(0f, 1f),
                            color  = paletteColor(rows.size + idx),
                        )
                    )
                }
        }

        return rows
    }

    /** Top category by spend (or income source by receipt). */
    private fun buildTopCategory(
        expenses: List<com.example.truxpense.data.repository.expense.Transaction>,
        incomes: List<com.example.truxpense.data.repository.income.Income>,
        type: ReportType,
    ): Pair<String, Double>? = when (type) {
        ReportType.EXPENSE -> expenses
            .groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amount } }
            .maxByOrNull { it.value }
            ?.let { it.key to it.value }

        ReportType.INCOME -> incomes
            .groupBy { it.source }
            .mapValues { e -> e.value.sumOf { it.amount } }
            .maxByOrNull { it.value }
            ?.let { it.key to it.value }

        ReportType.ALL -> {
            val combined = (
                expenses.groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } } +
                incomes.groupBy  { it.source  }.mapValues { e -> e.value.sumOf { it.amount } }
            )
            combined.maxByOrNull { it.value }?.let { it.key to it.value }
        }
    }

    // ── Trend chart ───────────────────────────────────────────────────────────

    /**
     * Produces chart buckets for the report's date range at the chosen [granularity].
     *
     * DAILY   → one bucket per calendar day (suitable for ranges ≤ 31 days)
     * WEEKLY  → one bucket per 7-day window starting at [fromDate]
     * MONTHLY → one bucket per calendar month (suitable for ranges > 3 months)
     */
    private fun buildTrendPoints(
        expenses: List<Pair<Long, Double>>,
        incomes: List<Pair<Long, Double>>,
        type: ReportType,
        fromDate: Long,
        toDate: Long,
        granularity: ReportTrendGranularity,
    ): List<ReportTrendPoint> {
        val points = mutableListOf<ReportTrendPoint>()

        fun totalAt(bucketStart: Long, bucketEnd: Long): Double {
            val expAmt = if (type == ReportType.INCOME) 0.0
            else expenses.filter { it.first in bucketStart..bucketEnd }.sumOf { it.second }
            val incAmt = if (type == ReportType.EXPENSE) 0.0
            else incomes.filter { it.first in bucketStart..bucketEnd }.sumOf { it.second }
            return expAmt + incAmt
        }

        when (granularity) {

            ReportTrendGranularity.DAILY -> {
                val cursor = Calendar.getInstance().apply { timeInMillis = fromDate; zeroTime() }
                val end    = Calendar.getInstance().apply { timeInMillis = toDate;   zeroTime() }
                while (!cursor.after(end)) {
                    val dayStart = cursor.timeInMillis
                    val dayEnd   = dayStart + 86_399_999L
                    val d = cursor.get(Calendar.DAY_OF_MONTH)
                    val m = monthShort(cursor.get(Calendar.MONTH))
                    points.add(ReportTrendPoint(
                        label       = "$d $m",
                        amount      = totalAt(dayStart, dayEnd),
                        tooltipDate = "$d $m ${cursor.get(Calendar.YEAR)}",
                    ))
                    cursor.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            ReportTrendGranularity.WEEKLY -> {
                val cursor = Calendar.getInstance().apply { timeInMillis = fromDate; zeroTime() }
                val end    = Calendar.getInstance().apply { timeInMillis = toDate }
                var week = 1
                while (cursor.timeInMillis <= end.timeInMillis) {
                    val bucketStart = cursor.timeInMillis
                    cursor.add(Calendar.DAY_OF_YEAR, 7)
                    val bucketEnd = minOf(cursor.timeInMillis - 1L, end.timeInMillis)
                    val d1 = Calendar.getInstance().apply { timeInMillis = bucketStart }.get(Calendar.DAY_OF_MONTH)
                    val d2 = Calendar.getInstance().apply { timeInMillis = bucketEnd   }.get(Calendar.DAY_OF_MONTH)
                    val m  = monthShort(Calendar.getInstance().apply { timeInMillis = bucketStart }.get(Calendar.MONTH))
                    points.add(ReportTrendPoint(
                        label       = "W$week",
                        amount      = totalAt(bucketStart, bucketEnd),
                        tooltipDate = "$d1–$d2 $m",
                    ))
                    week++
                }
            }

            ReportTrendGranularity.MONTHLY -> {
                val cursor = Calendar.getInstance().apply {
                    timeInMillis = fromDate
                    set(Calendar.DAY_OF_MONTH, 1)
                    zeroTime()
                }
                val endMs = toDate
                while (cursor.timeInMillis <= endMs) {
                    val bucketStart = cursor.timeInMillis
                    val month = cursor.get(Calendar.MONTH)
                    val year  = cursor.get(Calendar.YEAR)
                    cursor.add(Calendar.MONTH, 1)
                    val bucketEnd = minOf(cursor.timeInMillis - 1L, endMs)
                    points.add(ReportTrendPoint(
                        label       = monthShort(month),
                        amount      = totalAt(bucketStart, bucketEnd),
                        tooltipDate = "${monthFull(month)} $year",
                    ))
                }
            }
        }

        return points
    }

    // ── Formatting helpers ────────────────────────────────────────────────────

    private fun formatDate(ts: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        return "${c.get(Calendar.DAY_OF_MONTH)} ${monthShort(c.get(Calendar.MONTH))} ${c.get(Calendar.YEAR)}"
    }

    private fun formatTime(ts: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        val h = c.get(Calendar.HOUR_OF_DAY)
        val m = c.get(Calendar.MINUTE)
        val ampm = if (h < 12) "AM" else "PM"
        val hour = if (h % 12 == 0) 12 else h % 12
        return "$hour:${m.toString().padStart(2, '0')} $ampm"
    }

    private fun formatDateRange(from: Long, to: Long): String {
        val c1 = Calendar.getInstance().apply { timeInMillis = from }
        val c2 = Calendar.getInstance().apply { timeInMillis = to }
        val d1 = c1.get(Calendar.DAY_OF_MONTH)
        val m1 = monthShort(c1.get(Calendar.MONTH))
        val y1 = c1.get(Calendar.YEAR)
        val d2 = c2.get(Calendar.DAY_OF_MONTH)
        val m2 = monthShort(c2.get(Calendar.MONTH))
        val y2 = c2.get(Calendar.YEAR)
        return if (y1 == y2) "$d1 $m1 – $d2 $m2 $y2"
        else "$d1 $m1 $y1 – $d2 $m2 $y2"
    }

    private fun Calendar.zeroTime() {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    private fun monthShort(m: Int) =
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[m]

    private fun monthFull(m: Int) = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )[m]

    // ── Colour palette (cycles if > 12 categories) ────────────────────────────

    private val palette = listOf(
        Color(0xFF6C63FF), Color(0xFFFF6584), Color(0xFF43AA8B), Color(0xFFFFBE0B),
        Color(0xFF3A86FF), Color(0xFFFF006E), Color(0xFF8338EC), Color(0xFFFB5607),
        Color(0xFF06D6A0), Color(0xFFEF476F), Color(0xFF118AB2), Color(0xFFFFD166),
    )

    private fun paletteColor(index: Int): Color = palette[index % palette.size]
}

// ── NOTE ──────────────────────────────────────────────────────────────────────
// The Income data class in IncomeRepository is assumed to have:
//   val id: String
//   val source: String
//   val amount: Double
//   val timestamp: Long
// Adjust field names if your Income model differs.