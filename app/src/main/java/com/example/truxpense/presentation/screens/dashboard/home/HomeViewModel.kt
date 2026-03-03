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
import com.google.android.gms.auth.api.identity.Identity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prefs: AuthPreferences,
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
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
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Aggregated totals ─────────────────────────────────────────────────────

    val monthlySpend: StateFlow<Double> =
        expenseRepository.transactions.map { it.sumOf { t -> t.amount } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val expenseCount: StateFlow<Int> =
        expenseRepository.transactions.map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** True once the repository has emitted its first value (avoids UI flashing). */
    val isLoaded: StateFlow<Boolean> =
        expenseRepository.transactions
            .map { true }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val budgetLimit: StateFlow<Double> =
        budgetRepository.budgets.map { it.sumOf { b -> b.amount } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val budgetLeft: StateFlow<Double> =
        combine(budgetLimit, monthlySpend) { limit, spent -> limit - spent }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val budgetProgress: StateFlow<Float> =
        combine(budgetLimit, monthlySpend) { limit, spent ->
            if (limit > 0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    // ── Top spending categories ───────────────────────────────────────────────

    val topCategories: StateFlow<List<HomeSpendingCategory>> =
        combine(expenseRepository.transactions, monthlySpend) { list, total ->
            list.groupBy { it.category }.entries
                .map { (cat, items) ->
                    val amt = items.sumOf { it.amount }
                    HomeSpendingCategory(cat, amt, if (total > 0) (amt / total).toFloat() else 0f)
                }
                .sortedByDescending { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
}