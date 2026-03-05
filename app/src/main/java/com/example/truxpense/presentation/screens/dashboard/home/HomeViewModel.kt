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
import com.example.truxpense.data.repository.sms.PendingTransactionRepository
import com.google.android.gms.auth.api.identity.Identity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prefs: AuthPreferences,
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val incomeRepository: IncomeRepository,
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

    // ── Recent transactions (top 4) ───────────────────────────────────────────

    val recentTransactions: StateFlow<List<HomeTransactionItem>> =
        expenseRepository.transactions.map { list ->
            list.sortedByDescending { it.timestamp }.take(4).map { t ->
                HomeTransactionItem(
                    id = t.id,
                    title = t.merchant,
                    category = t.category,
                    amount = t.amount,
                    currencyCode = "INR",
                )
            }
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

    /** Sum of income entries for the current calendar month. */
    val monthlyIncome: StateFlow<Double> =
        incomeRepository.totalIncomeBetween(monthStartMs(), monthEndMs())
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    /** Savings = income – current-month expenses (clamped to ≥ 0). */
    val monthlySavings: StateFlow<Double> =
        combine(monthlyIncome, currentMonthExpenses) { income, spent ->
            (income - spent).coerceAtLeast(0.0)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

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