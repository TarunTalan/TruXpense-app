package com.example.truxpense.presentation.screens.dashboard.report

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.expense.ExpenseRepository
import com.example.truxpense.data.repository.income.IncomeRepository
import com.example.truxpense.data.repository.report.*
import com.example.truxpense.data.repository.vault.ReportVaultRepository
import com.example.truxpense.data.repository.vault.SaveResult
import com.example.truxpense.data.repository.vault.StorageOption
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

// ── Trend granularity ─────────────────────────────────────────────────────────

enum class ReportTrendGranularity { DAILY, WEEKLY, MONTHLY }

// ── Vault save status ─────────────────────────────────────────────────────────

enum class VaultSaveStatus { IDLE, SAVING, SUCCESS, ERROR }


// ── UI state ──────────────────────────────────────────────────────────────────

data class ReportDetailUiState(
    // ── Identity ──────────────────────────────────────────────────────────────
    val reportId: String = "",
    val title: String = "",
    val dateRangeLabel: String = "",
    val reportType: ReportType = ReportType.EXPENSE,

    // ── Raw date range (needed by vault save) ──────────────────────────────────
    val fromDate: Long = 0L,
    val toDate: Long = 0L,
    val spanDays: Int = 0,  // (toDate - fromDate).inWholeDays.toInt()

    // ── KPIs ──────────────────────────────────────────────────────────────────
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netAmount: Double = 0.0,
    val transactionCount: Int = 0,
    val avgDailySpend: Double = 0.0,

    // ── Breakdown ─────────────────────────────────────────────────────────────
    val categoryRows: List<ReportCategoryRow> = emptyList(),

    // ── Trend ─────────────────────────────────────────────────────────────────
    val trendGranularity: ReportTrendGranularity = ReportTrendGranularity.WEEKLY,
    val trendPoints: List<ReportTrendPoint> = emptyList(),

    // ── Transactions ──────────────────────────────────────────────────────────
    val transactions: List<ReportTransactionRow> = emptyList(),

    // ── Insights ──────────────────────────────────────────────────────────────
    val topMerchant: Pair<String, Double>? = null,
    val topCategory: Pair<String, Double>? = null,
    val highestSpendDay: Pair<String, Double>? = null,

    // ── Page state ────────────────────────────────────────────────────────────
    val isLoaded: Boolean = false,
    val notFound: Boolean = false,
)

// ── Category trend series model ──────────────────────────────────────────────
data class CategoryTrendSeries(
    val name: String,
    val color: Color,
    val amounts: List<Double>,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ReportDetailViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val vaultRepository: ReportVaultRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // ── Nav arg ───────────────────────────────────────────────────────────────

    private val _reportId = MutableStateFlow<String?>(null)
    private val _granularity = MutableStateFlow(ReportTrendGranularity.WEEKLY)

    fun loadReport(id: String) {
        _reportId.value = id
    }

    fun setGranularity(g: ReportTrendGranularity) {
        _granularity.value = g
    }

    // ── Show all transactions ─────────────────────────────────────────────────

    private val _showAllTx = MutableStateFlow(false)
    val showAllTransactions: StateFlow<Boolean> = _showAllTx.asStateFlow()
    fun toggleShowAll() {
        _showAllTx.value = !_showAllTx.value
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    private val _deleteComplete = MutableStateFlow(false)
    val deleteComplete: StateFlow<Boolean> = _deleteComplete.asStateFlow()

    fun deleteReport() {
        val id = _reportId.value ?: return
        viewModelScope.launch {
            reportRepository.deleteReport(id)
            _deleteComplete.value = true
        }
    }

    fun onDeleteHandled() {
        _deleteComplete.value = false
    }

    // ── Export (direct share) ─────────────────────────────────────────────────

    private val _exportStatus = MutableStateFlow(ExportStatus.IDLE)
    val exportStatus: StateFlow<ExportStatus> = _exportStatus.asStateFlow()

    private val _exportResult = MutableStateFlow<ExportResult?>(null)
    val exportResult: StateFlow<ExportResult?> = _exportResult.asStateFlow()

    fun export(format: ExportFormat) {
        val state = uiState.value
        if (!state.isLoaded) return
        _exportStatus.value = ExportStatus.EXPORTING
        viewModelScope.launch {
            val result = ReportExporter.export(context, format, state)
            _exportResult.value = result
            _exportStatus.value = if (result.error == null) ExportStatus.SUCCESS else ExportStatus.ERROR
        }
    }

    fun consumeExportResult(): ExportResult? {
        val r = _exportResult.value
        _exportResult.value = null
        _exportStatus.value = ExportStatus.IDLE
        return r
    }

    // ── Vault save ────────────────────────────────────────────────────────────

    private val _vaultSaveStatus = MutableStateFlow(VaultSaveStatus.IDLE)
    val vaultSaveStatus: StateFlow<VaultSaveStatus> = _vaultSaveStatus.asStateFlow()

    private val _vaultSaveError = MutableStateFlow<String?>(null)
    val vaultSaveError: StateFlow<String?> = _vaultSaveError.asStateFlow()

    /**
     * Called by [VaultSaveBottomSheet] when user taps Save.
     * Runs export → local write → optional Firebase upload, all in background.
     */
    fun saveToVault(
        format: ExportFormat,
        storageOption: StorageOption,
        tags: List<String>,
    ) {
        val state = uiState.value
        if (!state.isLoaded) return

        _vaultSaveStatus.value = VaultSaveStatus.SAVING
        _vaultSaveError.value = null

        viewModelScope.launch {
            when (val result = vaultRepository.save(
                reportState = state,
                format = format,
                storageOption = storageOption,
                tags = tags,
                reportId = state.reportId,
            )) {
                is SaveResult.Success -> {
                    _vaultSaveStatus.value = VaultSaveStatus.SUCCESS
                }

                is SaveResult.Error -> {
                    _vaultSaveStatus.value = VaultSaveStatus.ERROR
                    _vaultSaveError.value = result.message
                }
            }
        }
    }

    fun resetVaultSaveStatus() {
        _vaultSaveStatus.value = VaultSaveStatus.IDLE
        _vaultSaveError.value = null
    }

    // ── Main UI state (reactive) ──────────────────────────────────────────────

    val uiState: StateFlow<ReportDetailUiState> = combine(
        _reportId,
        expenseRepository.transactions,
        incomeRepository.allIncome,
        _granularity,
    ) { reportId, allExpenses, allIncomes, granularity ->

        val id = reportId ?: return@combine ReportDetailUiState()
        val report = reportRepository.getReportByIdOnce(id) ?: return@combine ReportDetailUiState(
            isLoaded = true, notFound = true
        )

        val type = report.parsedType()
        val filterCats = report.parsedCategories()

        fun inWindow(ts: Long) = ts in report.fromDate..report.toDate
        fun inCategories(cat: String) = filterCats.isEmpty() || cat in filterCats

        val expenses = allExpenses.filter { inWindow(it.timestamp) && inCategories(it.category) }
        val incomes = allIncomes.filter { inWindow(it.timestamp) && inCategories(it.source) }

        val showExpenses = type == ReportType.EXPENSE || type == ReportType.ALL
        val showIncome = type == ReportType.INCOME || type == ReportType.ALL
        val usedExpenses = if (showExpenses) expenses else emptyList()
        val usedIncomes = if (showIncome) incomes else emptyList()

        val totalExpenses = usedExpenses.sumOf { it.amount }
        val totalIncome = usedIncomes.sumOf { it.amount }
        val netAmount = totalIncome - totalExpenses
        val days = ((report.toDate - report.fromDate) / 86_400_000L).toInt().coerceAtLeast(1)

        val categoryRows = buildCategoryRows(usedExpenses, usedIncomes, type, totalExpenses, totalIncome)
        val trendPoints = buildTrendPoints(
            expenses = usedExpenses.map { it.timestamp to it.amount },
            incomes = usedIncomes.map { it.timestamp to it.amount },
            type = type,
            fromDate = report.fromDate,
            toDate = report.toDate,
            granularity = granularity,
        )

        val expenseRows = usedExpenses.map { t ->
            ReportTransactionRow(
                id = t.id,
                merchant = t.merchant,
                category = t.category,
                amount = t.amount,
                isExpense = true,
                dateLabel = formatDate(t.timestamp),
                timeLabel = formatTime(t.timestamp),
                paymentMethod = t.paymentMethod.ifBlank { "—" },
            )
        }
        val incomeRows = usedIncomes.map { i ->
            ReportTransactionRow(
                id = i.id,
                merchant = i.source,
                category = i.source,
                amount = i.amount,
                isExpense = false,
                dateLabel = formatDate(i.timestamp),
                timeLabel = formatTime(i.timestamp),
                paymentMethod = "—",
            )
        }
        val transactions = (expenseRows + incomeRows).sortedByDescending { row ->
            usedExpenses.firstOrNull { it.id == row.id }?.timestamp
                ?: usedIncomes.firstOrNull { it.id == row.id }?.timestamp ?: 0L
        }

        val topMerchant =
            usedExpenses.groupBy { it.merchant }.mapValues { e -> e.value.sumOf { it.amount } }.maxByOrNull { it.value }
                ?.let { it.key to it.value }

        val topCategory = buildTopCategory(usedExpenses, usedIncomes, type)
        val highestDay =
            usedExpenses.groupBy { formatDate(it.timestamp) }.mapValues { e -> e.value.sumOf { it.amount } }
                .maxByOrNull { it.value }?.let { it.key to it.value }

        ReportDetailUiState(
            reportId = id,
            title = report.title,
            dateRangeLabel = formatDateRange(report.fromDate, report.toDate),
            reportType = type,
            fromDate = report.fromDate,
            toDate = report.toDate,
            spanDays = ((report.toDate - report.fromDate) / 86_400_000L).toInt().coerceAtLeast(1),
            totalExpenses = totalExpenses,
            totalIncome = totalIncome,
            netAmount = netAmount,
            transactionCount = transactions.size,
            avgDailySpend = totalExpenses / days,
            categoryRows = categoryRows,
            trendGranularity = granularity,
            trendPoints = trendPoints,
            transactions = transactions,
            topMerchant = topMerchant,
            topCategory = topCategory,
            highestSpendDay = highestDay,
            isLoaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportDetailUiState())

    // ── Category trend data (stacked/grouped by trend periods) ─────────────────

    val categoryTrendData: StateFlow<List<CategoryTrendSeries>> =
        combine(
            uiState,
            expenseRepository.transactions,
            incomeRepository.allIncome,
            _granularity,
        ) { state: ReportDetailUiState, allExpenses: List<com.example.truxpense.data.repository.expense.Transaction>, allIncomes: List<com.example.truxpense.data.repository.income.Income>, granularity: ReportTrendGranularity ->
            if (!state.isLoaded || state.trendPoints.isEmpty()) return@combine emptyList<CategoryTrendSeries>()

            // Filter transactions to report date range and categories
            val filterCats = state.categoryRows.map { it.name }.toSet()
            fun inWindow(ts: Long) = ts in state.fromDate..state.toDate
            fun inCategories(cat: String) = filterCats.isEmpty() || cat in filterCats

            val expenses = allExpenses.filter { inWindow(it.timestamp) && inCategories(it.category) }
            val incomes = allIncomes.filter { inWindow(it.timestamp) && inCategories(it.source) }

            // Compute amount per trend point per category
            buildCategoryTrendSeries(
                expenses = expenses,
                incomes = incomes,
                type = state.reportType,
                trendPoints = state.trendPoints,
                granularity = granularity,
                fromDate = state.fromDate,
                toDate = state.toDate,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<CategoryTrendSeries>())

    // ── Private helpers ───────────────────────────────────────────────────────

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
            ReportType.INCOME -> totalIncome
            ReportType.ALL -> totalExpenses + totalIncome
        }
        if (grandTotal <= 0) return emptyList()

        if (type != ReportType.INCOME) {
            expenses.groupBy { it.category }
                .mapValues { e -> e.value.sumOf { it.amount } }.entries.sortedByDescending { it.value }
                .forEachIndexed { idx, (cat, amt) ->
                    rows.add(
                        ReportCategoryRow(
                            cat, amt, (amt / grandTotal).toFloat().coerceIn(0f, 1f), paletteColor(idx)
                        )
                    )
                }
        }
        if (type != ReportType.EXPENSE) {
            incomes.groupBy { it.source }
                .mapValues { e -> e.value.sumOf { it.amount } }.entries.sortedByDescending { it.value }
                .forEachIndexed { idx, (src, amt) ->
                    rows.add(
                        ReportCategoryRow(
                            src, amt, (amt / grandTotal).toFloat().coerceIn(0f, 1f), paletteColor(rows.size + idx)
                        )
                    )
                }
        }
        return rows
    }

    private fun buildTopCategory(
        expenses: List<com.example.truxpense.data.repository.expense.Transaction>,
        incomes: List<com.example.truxpense.data.repository.income.Income>,
        type: ReportType,
    ): Pair<String, Double>? = when (type) {
        ReportType.EXPENSE -> expenses.groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }
            .maxByOrNull { it.value }?.let { it.key to it.value }

        ReportType.INCOME -> incomes.groupBy { it.source }.mapValues { e -> e.value.sumOf { it.amount } }
            .maxByOrNull { it.value }?.let { it.key to it.value }

        ReportType.ALL -> (expenses.groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amount } } + incomes.groupBy { it.source }
            .mapValues { e -> e.value.sumOf { it.amount } }).maxByOrNull { it.value }?.let { it.key to it.value }
    }

    private fun buildTrendPoints(
        expenses: List<Pair<Long, Double>>,
        incomes: List<Pair<Long, Double>>,
        type: ReportType,
        fromDate: Long,
        toDate: Long,
        granularity: ReportTrendGranularity,
    ): List<ReportTrendPoint> {
        val points = mutableListOf<ReportTrendPoint>()
        fun totalAt(s: Long, e: Long): Double {
            val exp = if (type == ReportType.INCOME) 0.0 else expenses.filter { it.first in s..e }.sumOf { it.second }
            val inc = if (type == ReportType.EXPENSE) 0.0 else incomes.filter { it.first in s..e }.sumOf { it.second }
            return exp + inc
        }
        when (granularity) {
            ReportTrendGranularity.DAILY -> {
                val cursor = Calendar.getInstance().apply { timeInMillis = fromDate; zeroTime() }
                val end = Calendar.getInstance().apply { timeInMillis = toDate; zeroTime() }
                while (!cursor.after(end)) {
                    val s = cursor.timeInMillis
                    points.add(
                        ReportTrendPoint(
                            label = "${cursor.get(Calendar.DAY_OF_MONTH)} ${monthShort(cursor.get(Calendar.MONTH))}",
                            amount = totalAt(s, s + 86_399_999L),
                            tooltipDate = "${cursor.get(Calendar.DAY_OF_MONTH)} ${monthShort(cursor.get(Calendar.MONTH))} ${
                                cursor.get(
                                    Calendar.YEAR
                                )
                            }",
                        )
                    )
                    cursor.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            ReportTrendGranularity.WEEKLY -> {
                val cursor = Calendar.getInstance().apply { timeInMillis = fromDate; zeroTime() }
                val end = Calendar.getInstance().apply { timeInMillis = toDate }
                var week = 1
                while (cursor.timeInMillis <= end.timeInMillis) {
                    val bStart = cursor.timeInMillis; cursor.add(Calendar.DAY_OF_YEAR, 7)
                    val bEnd = minOf(cursor.timeInMillis - 1L, end.timeInMillis)
                    val d1 = Calendar.getInstance().apply { timeInMillis = bStart }.get(Calendar.DAY_OF_MONTH)
                    val d2 = Calendar.getInstance().apply { timeInMillis = bEnd }.get(Calendar.DAY_OF_MONTH)
                    val m = monthShort(Calendar.getInstance().apply { timeInMillis = bStart }.get(Calendar.MONTH))
                    points.add(ReportTrendPoint("W$week", totalAt(bStart, bEnd), "$d1–$d2 $m"))
                    week++
                }
            }

            ReportTrendGranularity.MONTHLY -> {
                val cursor =
                    Calendar.getInstance().apply { timeInMillis = fromDate; set(Calendar.DAY_OF_MONTH, 1); zeroTime() }
                while (cursor.timeInMillis <= toDate) {
                    val bStart = cursor.timeInMillis
                    val month = cursor.get(Calendar.MONTH)
                    val year = cursor.get(Calendar.YEAR)
                    cursor.add(Calendar.MONTH, 1)
                    val bEnd = minOf(cursor.timeInMillis - 1L, toDate)
                    points.add(ReportTrendPoint(monthShort(month), totalAt(bStart, bEnd), "${monthFull(month)} $year"))
                }
            }
        }
        return points
    }

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
        val d1 = c1.get(Calendar.DAY_OF_MONTH);
        val m1 = monthShort(c1.get(Calendar.MONTH));
        val y1 = c1.get(Calendar.YEAR)
        val d2 = c2.get(Calendar.DAY_OF_MONTH);
        val m2 = monthShort(c2.get(Calendar.MONTH));
        val y2 = c2.get(Calendar.YEAR)
        return if (y1 == y2) "$d1 $m1 – $d2 $m2 $y2" else "$d1 $m1 $y1 – $d2 $m2 $y2"
    }

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

    private val palette = listOf(
        Color(0xFF6C63FF), Color(0xFFFF6584), Color(0xFF43AA8B), Color(0xFFFFBE0B),
        Color(0xFF3A86FF), Color(0xFFFF006E), Color(0xFF8338EC), Color(0xFFFB5607),
        Color(0xFF06D6A0), Color(0xFFEF476F), Color(0xFF118AB2), Color(0xFFFFD166),
    )

    private fun paletteColor(i: Int) = palette[i % palette.size]

    private fun buildCategoryTrendSeries(
        expenses: List<com.example.truxpense.data.repository.expense.Transaction>,
        incomes: List<com.example.truxpense.data.repository.income.Income>,
        type: ReportType,
        trendPoints: List<ReportTrendPoint>,
        granularity: ReportTrendGranularity,
        fromDate: Long,
        toDate: Long,
    ): List<CategoryTrendSeries> {
        if (trendPoints.isEmpty()) return emptyList()

        // Collect all unique categories
        val allCategories = mutableSetOf<String>()
        if (type != ReportType.INCOME) {
            allCategories.addAll(expenses.map { it.category })
        }
        if (type != ReportType.EXPENSE) {
            allCategories.addAll(incomes.map { it.source })
        }

        // For each category, compute amounts per trend period
        val seriesList = mutableListOf<CategoryTrendSeries>()
        allCategories.forEachIndexed { idx, category ->
            val amounts = mutableListOf<Double>()

            // For each trend period, sum transactions in that category within that period
            when (granularity) {
                ReportTrendGranularity.DAILY -> {
                    val cursor = Calendar.getInstance().apply { timeInMillis = fromDate; zeroTime() }
                    val end = Calendar.getInstance().apply { timeInMillis = toDate; zeroTime() }
                    while (!cursor.after(end)) {
                        val s = cursor.timeInMillis
                        val e = s + 86_399_999L
                        val sum = expenses.filter {
                            it.timestamp in s..e && it.category == category
                        }.sumOf { it.amount }
                        amounts.add(sum)
                        cursor.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                ReportTrendGranularity.WEEKLY -> {
                    val cursor = Calendar.getInstance().apply { timeInMillis = fromDate; zeroTime() }
                    val end = Calendar.getInstance().apply { timeInMillis = toDate }
                    while (cursor.timeInMillis <= end.timeInMillis) {
                        val bStart = cursor.timeInMillis
                        cursor.add(Calendar.DAY_OF_YEAR, 7)
                        val bEnd = minOf(cursor.timeInMillis - 1L, end.timeInMillis)
                        val sum = expenses.filter {
                            it.timestamp in bStart..bEnd && it.category == category
                        }.sumOf { it.amount }
                        amounts.add(sum)
                    }
                }

                ReportTrendGranularity.MONTHLY -> {
                    val cursor = Calendar.getInstance().apply {
                        timeInMillis = fromDate
                        set(Calendar.DAY_OF_MONTH, 1)
                        zeroTime()
                    }
                    while (cursor.timeInMillis <= toDate) {
                        val bStart = cursor.timeInMillis
                        cursor.add(Calendar.MONTH, 1)
                        val bEnd = minOf(cursor.timeInMillis - 1L, toDate)
                        val sum = expenses.filter {
                            it.timestamp in bStart..bEnd && it.category == category
                        }.sumOf { it.amount }
                        amounts.add(sum)
                    }
                }
            }

            seriesList.add(
                CategoryTrendSeries(
                    name = category,
                    color = paletteColor(idx),
                    amounts = amounts,
                )
            )
        }

        return seriesList
    }
}