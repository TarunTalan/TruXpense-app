package com.example.truxpense.presentation.screens.dashboard.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.R
import com.example.truxpense.data.repository.budget.BudgetRepository
import com.example.truxpense.data.local.datastore.AuthPreferences
import com.example.truxpense.data.repository.expense.ExpenseRepository
import com.example.truxpense.data.repository.expense.Transaction
import com.example.truxpense.data.repository.income.IncomeRepository
import com.example.truxpense.data.repository.savings.SavingsRepository
import com.example.truxpense.data.repository.sms.PendingTransactionRepository
import com.google.android.gms.auth.api.identity.Identity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prefs: AuthPreferences,
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val incomeRepository: IncomeRepository,
    private val savingsRepository: SavingsRepository,
    private val pendingTransactionRepository: PendingTransactionRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // ── Auth / user ───────────────────────────────────────────────────────────

    val username: Flow<String?> = prefs.username

    // ── SMS permission ────────────────────────────────────────────────────────

    private val _hasSmsPermission = MutableStateFlow(
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    )
    val hasSmsPermission: StateFlow<Boolean> = _hasSmsPermission.asStateFlow()

    private val _requestSmsPermission = MutableSharedFlow<Unit>(replay = 0)
    val requestSmsPermission = _requestSmsPermission

    fun refreshSmsPermission() {
        _hasSmsPermission.value = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onSmsPermissionResult(granted: Boolean) { _hasSmsPermission.value = granted }

    fun emitRequestSmsPermission() {
        viewModelScope.launch { _requestSmsPermission.emit(Unit) }
    }

    // ── Pending SMS transactions ──────────────────────────────────────────────

    val pendingCount: StateFlow<Int> =
        pendingTransactionRepository.pendingCount
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val pendingTransactions =
        pendingTransactionRepository.pendingTransactions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun confirmPending(id: String) {
        viewModelScope.launch { pendingTransactionRepository.confirm(id) }
    }

    fun rejectPending(id: String) {
        viewModelScope.launch { pendingTransactionRepository.reject(id) }
    }

    // ── Recent transactions (top 5, expenses + income merged) ───────────────

    val recentTransactions: StateFlow<List<HomeTransactionItem>> =
        combine(
            expenseRepository.transactions,
            incomeRepository.allIncome,
        ) { expenses, incomes ->
            val expenseItems = expenses.map { t ->
                HomeTransactionItem(
                    id = t.id,
                    title = t.merchant,
                    category = t.category,
                    amount = t.amount,
                    currencyCode = "INR",
                    isExpense = true,
                )
            }
            val incomeItems = incomes.map { i ->
                HomeTransactionItem(
                    id = i.id,
                    title = i.source,
                    category = i.source,
                    amount = i.amount,
                    currencyCode = "INR",
                    isExpense = false,
                )
            }
            (expenseItems + incomeItems)
                .sortedByDescending { item ->
                    // Use original timestamp for sorting; look it up from each repo list
                    expenses.firstOrNull { it.id == item.id }?.timestamp
                        ?: incomes.firstOrNull { it.id == item.id }?.timestamp
                        ?: 0L
                }
                .take(5)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Aggregated totals ─────────────────────────────────────────────────────

    val monthlySpend: StateFlow<Double> =
        expenseRepository.transactions.map { it.sumOf { t -> t.amount } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    /** Sum of expenses for the current calendar month only. */
    val currentMonthExpenses: StateFlow<Double> =
        expenseRepository.transactions.map { list ->
            val start = monthStartMs()
            list.filter { it.timestamp >= start }.sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    /**
     * Compares spending this month vs the same-length window of last month.
     * percentChange is null when last month had zero spend (no comparison possible).
     */
    val monthOverMonthChange: StateFlow<MonthlyChangeData> =
        expenseRepository.transactions.map { list ->
            val cal = Calendar.getInstance()
            val thisYear = cal.get(Calendar.YEAR)
            val thisMonth = cal.get(Calendar.MONTH)
            val todayDay = cal.get(Calendar.DAY_OF_MONTH)

            // Previous month boundaries
            val prevCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, -1)
            }
            val prevYear = prevCal.get(Calendar.YEAR)
            val prevMonth = prevCal.get(Calendar.MONTH)
            val prevMonthLabel = prevCal.getDisplayName(
                Calendar.MONTH, Calendar.SHORT, Locale.getDefault()
            ) ?: ""

            // Start of current month
            val thisMonthStart = Calendar.getInstance().apply {
                set(Calendar.YEAR, thisYear)
                set(Calendar.MONTH, thisMonth)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            // Same-length window in previous month (days 1..todayDay)
            val prevWindowStart = Calendar.getInstance().apply {
                set(Calendar.YEAR, prevYear)
                set(Calendar.MONTH, prevMonth)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val prevWindowEnd = Calendar.getInstance().apply {
                set(Calendar.YEAR, prevYear)
                set(Calendar.MONTH, prevMonth)
                set(Calendar.DAY_OF_MONTH, minOf(todayDay,
                    prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)))
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val thisMonthSpend = list.filter { it.timestamp >= thisMonthStart }.sumOf { it.amount }
            val prevMonthSpend = list.filter { it.timestamp in prevWindowStart..prevWindowEnd }.sumOf { it.amount }

            val pct = if (prevMonthSpend > 0)
                ((thisMonthSpend - prevMonthSpend) / prevMonthSpend) * 100.0
            else
                null

            MonthlyChangeData(percentChange = pct, prevMonthLabel = prevMonthLabel)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlyChangeData(null, ""))

    /** Sum of income entries for the current calendar month. */
    val monthlyIncome: StateFlow<Double> =
        incomeRepository.totalIncomeBetween(monthStartMs(), monthEndMs())
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    /** Sum of actual savings entries recorded in the current calendar month. */
    val monthlySavings: StateFlow<Double> =
        savingsRepository.totalSavingsBetween(monthStartMs(), monthEndMs())
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val expenseCount: StateFlow<Int> =
        expenseRepository.transactions.map { it.size }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    /**
     * Per-day spending for the current calendar month.
     * Index 0 = day 1, index N-1 = last day of month.
     * Size equals the number of days in the current month.
     */
    val dailySpendPoints: StateFlow<FloatArray> =
        expenseRepository.transactions.map { list ->
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val points = FloatArray(daysInMonth)
            list.forEach { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                if (txCal.get(Calendar.YEAR) == year && txCal.get(Calendar.MONTH) == month) {
                    val dayIndex = txCal.get(Calendar.DAY_OF_MONTH) - 1
                    points[dayIndex] += tx.amount.toFloat()
                }
            }
            points
        }.stateIn(viewModelScope, SharingStarted.Eagerly, FloatArray(0))

    /** True once the repository has emitted its first value (avoids UI flashing). */
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            expenseRepository.transactions.first()
            _isLoaded.value = true
        }
    }

    val budgetLimit: StateFlow<Double> =
        budgetRepository.budgets.map { it.sumOf { b -> b.amount } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val budgetLeft: StateFlow<Double> =
        combine(budgetLimit, monthlySpend) { limit, spent -> limit - spent }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val budgetProgress: StateFlow<Float> =
        combine(budgetLimit, monthlySpend) { limit, spent ->
            if (limit > 0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f
        }.stateIn(viewModelScope, SharingStarted.Eagerly, 0f)

    // ── Dynamic home-screen insight ───────────────────────────────────────────

    /**
     * Produces a single human-readable insight based on the current month's
     * expense data. Priority order:
     *  1. Budget nearly exhausted (≥ 90 %)
     *  2. Large recent single transaction (unusual)
     *  3. Weekly spike (last 7d vs previous 7d)
     *  4. Top merchant/category dominance (> 50 % category or >40% merchant)
     *  5. Short-term upward trend (last 3d vs prior 3d)
     *  6. Spending accelerating vs. pace-to-date
     *  7. No expenses yet → encouraging prompt
     */
    val spendingInsight: StateFlow<SpendingInsight> =
        combine(
            expenseRepository.transactions,
            budgetRepository.budgets,
        ) { allTx, budgets ->
            val nowCal = Calendar.getInstance()
            val year = nowCal.get(Calendar.YEAR)
            val month = nowCal.get(Calendar.MONTH)
            val today = nowCal.get(Calendar.DAY_OF_MONTH)
            val daysInMonth = nowCal.getActualMaximum(Calendar.DAY_OF_MONTH)

            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val thisMo = allTx.filter { tx ->
                val c = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
            }

            val lastMo = allTx.filter { tx ->
                val c = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                val prevMonth = if (month == 0) 11 else month - 1
                val prevYear  = if (month == 0) year - 1 else year
                c.get(Calendar.YEAR) == prevYear && c.get(Calendar.MONTH) == prevMonth
            }

            val totalSpent = thisMo.sumOf { it.amount }
            val totalBudget = budgets.sumOf { it.amount }

            // Helper: timeframe windows
            fun millisDaysAgo(days: Int): Long = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }.timeInMillis

            val sevenDaysAgo = millisDaysAgo(7)
            val fourteenDaysAgo = millisDaysAgo(14)
            val threeDaysAgo = millisDaysAgo(3)
            val sixDaysAgo = millisDaysAgo(6)

            val last7 = allTx.filter { it.timestamp >= sevenDaysAgo }
            val prev7 = allTx.filter { it.timestamp in (fourteenDaysAgo..(sevenDaysAgo - 1)) }
            val last3 = allTx.filter { it.timestamp >= threeDaysAgo }
            val prev3 = allTx.filter { it.timestamp in (sixDaysAgo..(threeDaysAgo - 1)) }

            // 1. Budget nearly exhausted
            if (totalBudget > 0) {
                val pct = totalSpent / totalBudget
                if (pct >= 0.9) {
                    val remaining = totalBudget - totalSpent
                    return@combine SpendingInsight(
                        message = "You've used ${(pct * 100).toInt()}% of your budget — only ₹${remaining.toLong()} left this month.",
                        actionText = "Review budget",
                        target = InsightTarget.AnalyticsRoot,
                    )
                }
            }

            // 2. Large single transaction in recent period (last 7 days)
            if (last7.isNotEmpty()) {
                val avg = last7.map { it.amount }.average().takeIf { !it.isNaN() && it > 0 } ?: 0.0
                val maxTx = last7.maxByOrNull { it.amount }
                if (maxTx != null) {
                    // large means > 3x average AND absolute > 1000 (tunable)
                    if (avg > 0 && maxTx.amount > avg * 3 && maxTx.amount > 1000) {
                        val d = Calendar.getInstance().apply { timeInMillis = maxTx.timestamp }
                        val dateStr = "${d.get(Calendar.DAY_OF_MONTH)} ${d.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())}"
                        return@combine SpendingInsight(
                            message = "Large purchase: ₹${maxTx.amount.toLong()} at ${maxTx.merchant} on $dateStr.",
                            actionText = "View transaction",
                            target = InsightTarget.TransactionDetail(maxTx.id),
                        )
                    }
                }
            }

            // 3. Weekly spike: last 7 days vs previous 7 days
            if (prev7.isNotEmpty()) {
                val last7Sum = last7.sumOf { it.amount }
                val prev7Sum = prev7.sumOf { it.amount }
                if (prev7Sum > 0 && last7Sum > prev7Sum * 1.5) {
                    val changePct = (((last7Sum / prev7Sum) - 1) * 100).toInt()
                    return@combine SpendingInsight(
                        message = "Your spending rose $changePct% this week compared to the previous week.",
                        actionText = "See weekly trend",
                        target = InsightTarget.AnalyticsRoot,
                    )
                }
            }

            // 4. Top merchant / category dominance
            if (thisMo.isNotEmpty()) {
                val byMerchant = thisMo.groupBy { it.merchant }.mapValues { it.value.sumOf { tx -> tx.amount } }
                val topMerchant = byMerchant.maxByOrNull { it.value }
                if (topMerchant != null && totalSpent > 0) {
                    val share = topMerchant.value / totalSpent
                    if (share > 0.4) {
                        return@combine SpendingInsight(
                            message = "You spent ${(share * 100).toInt()}% of this month's total at ${topMerchant.key}.",
                            actionText = "View merchant breakdown",
                            target = InsightTarget.AnalyticsMerchant(topMerchant.key),
                        )
                    }
                }

                val byCat = thisMo.groupBy { it.category }.mapValues { it.value.sumOf { tx -> tx.amount } }
                val topCat = byCat.maxByOrNull { it.value }
                if (topCat != null && totalSpent > 0) {
                    val share = topCat.value / totalSpent
                    if (share > 0.5) {
                        return@combine SpendingInsight(
                            message = "${topCat.key} makes up ${(share * 100).toInt()}% of your spending this month.",
                            actionText = "View ${topCat.key} breakdown",
                            target = InsightTarget.AnalyticsCategory(topCat.key),
                        )
                    }
                }
            }

            // 5. Short-term upward trend (3-day window)
            if (prev3.isNotEmpty()) {
                val last3Sum = last3.sumOf { it.amount }
                val prev3Sum = prev3.sumOf { it.amount }
                if (prev3Sum > 0 && last3Sum > prev3Sum * 1.3) {
                    val changePct = (((last3Sum / prev3Sum) - 1) * 100).toInt()
                    return@combine SpendingInsight(
                        message = "Spending up $changePct% in the last 3 days compared to the previous 3 days.",
                        actionText = "See recent trend",
                        target = InsightTarget.AnalyticsRoot,
                    )
                }
            }

            // 6. Spending pace vs last month (original logic)
            if (lastMo.isNotEmpty() && today > 1) {
                val lastTotal = lastMo.sumOf { it.amount }
                val projected = if (today > 0) (totalSpent / today) * daysInMonth else 0.0
                if (lastTotal > 0 && projected > lastTotal * 1.2) {
                    return@combine SpendingInsight(
                        message = "You're on pace to spend ${((projected / lastTotal - 1) * 100).toInt()}% more than last month.",
                        actionText = "See spending trend",
                        target = InsightTarget.AnalyticsRoot,
                    )
                }
                if (lastTotal > 0 && projected < lastTotal * 0.8) {
                    return@combine SpendingInsight(
                        message = "Great job! You're spending less than last month so far.",
                        actionText = "See spending trend",
                        target = InsightTarget.AnalyticsRoot,
                    )
                }
            }

            // 7. No spend yet this month
            if (thisMo.isEmpty()) {
                return@combine SpendingInsight(
                    message = "No expenses logged yet this month. Start tracking to see insights here.",
                    actionText = "Add first expense",
                    target = InsightTarget.AnalyticsRoot,
                )
            }

            // 8. Generic fallback – show top category
            val byCat = thisMo.groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }
            val topCat = byCat.maxByOrNull { it.value }
            SpendingInsight(
                message = if (topCat != null)
                    "Your top spending category this month is ${topCat.key} (₹${topCat.value.toLong()})."
                else
                    "Keep tracking your expenses to unlock personalised insights.",
                actionText = "View analytics",
                target = InsightTarget.AnalyticsRoot,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpendingInsight("Loading insights…"))

    // ── Top spending categories ───────────────────────────────────────────────

    val topCategories: StateFlow<List<HomeSpendingCategory>> =
        combine(expenseRepository.transactions, monthlySpend) { list, total ->
            list.groupBy { it.category }.entries
                .map { (cat, items) ->
                    val amt = items.sumOf { it.amount }
                    HomeSpendingCategory(cat, amt, if (total > 0) (amt / total).toFloat() else 0f)
                }
                .sortedByDescending { it.amount }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Deep-link helpers ─────────────────────────────────────────────────────

    /**
     * Returns (budgetName, monthlyLimit, spent) for the given [category],
     * or null if no budget is found. Called from the notification deep-link
     * handler to build the correct Budget.detailRoute.
     */
    suspend fun getBudgetDetailArgs(category: String): Triple<String, Double, Double>? {
        val budgets = budgetRepository.budgets.first()
        val budget  = budgets.firstOrNull { it.category.equals(category, ignoreCase = true) }
                       ?: return null

        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val txns  = expenseRepository.transactions.first()
        val spent = txns
            .filter { it.category.equals(category, ignoreCase = true) && it.timestamp >= monthStart }
            .sumOf { it.amount }

        return Triple(budget.category, budget.amount, spent)
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun addTransaction(tx: HomeTransactionItem) {
        viewModelScope.launch {
            expenseRepository.addExpense(
                Transaction(
                    id = tx.id,
                    amount = tx.amount,
                    category = tx.category,
                    paymentMethod = "",
                    merchant = tx.title,
                    timestamp = System.currentTimeMillis(),
                )
            )
        }
    }

    fun logout(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                prefs.clear()
                try {
                    val clientId = context.getString(R.string.default_web_client_id)
                    if (!clientId.isNullOrBlank() && !clientId.startsWith("REPLACE_WITH")) {
                        Identity.getSignInClient(context).signOut()
                    }
                } catch (_: Throwable) { /* ignore */ }
            } finally {
                onComplete?.invoke()
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun monthStartMs(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun monthEndMs(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}