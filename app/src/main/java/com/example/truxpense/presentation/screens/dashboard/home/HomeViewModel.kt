package com.example.truxpense.presentation.screens.dashboard.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.R
import com.example.truxpense.data.budget.BudgetRepository
import com.example.truxpense.data.prefs.AuthPreferences
import com.example.truxpense.data.repository.dashboard.RepositoryProvider
import com.example.truxpense.data.repository.dashboard.Transaction
import com.google.android.gms.auth.api.identity.Identity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prefs: AuthPreferences,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // Auth / user

    val username: Flow<String?> = prefs.username

    // SMS permission

    private val _hasSmsPermission = MutableStateFlow(
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    )
    val hasSmsPermission: StateFlow<Boolean> = _hasSmsPermission.asStateFlow()

    private val _requestSmsPermission = MutableSharedFlow<Unit>(replay = 0)
    val requestSmsPermission = _requestSmsPermission

    fun refreshSmsPermission() {
        _hasSmsPermission.value = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onSmsPermissionResult(granted: Boolean) {
        _hasSmsPermission.value = granted
    }

    fun emitRequestSmsPermission() {
        viewModelScope.launch { _requestSmsPermission.emit(Unit) }
    }

    // Single repository source
    private val repository = RepositoryProvider.expenseRepository

    // Recent transactions (top 4) derived from central repository
    val recentTransactions: StateFlow<List<HomeTransactionItem>> = repository.transactions.map { list ->
        list.sortedByDescending { it.timestamp }.take(4).map { t ->
            HomeTransactionItem(
                id = t.id, title = t.merchant, category = t.category, amount = t.amount, currencyCode = "INR"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total spent (all time / period can be refined later)
    val monthlySpend: StateFlow<Double> = repository.transactions.map { list -> list.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Expense count
    val expenseCount: StateFlow<Int> =
        repository.transactions.map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Budget derived from BudgetRepository
    val budgetLimit: StateFlow<Double> = BudgetRepository.budgets.map { list -> list.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Budget left = limit - totalSpent (clamped, could be negative to indicate over-spend)
    val budgetLeft: StateFlow<Double> = combine(budgetLimit, monthlySpend) { limit, spent ->
        (limit - spent)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Budget progress derived as used / limit
    val budgetProgress: StateFlow<Float> = combine(budgetLimit, budgetLeft) { limit, left ->
        val used = (limit - left)
        if (limit > 0) (used / limit).toFloat().coerceIn(0f, 1f) else 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // Top categories computed from repository transactions
    val topCategories: StateFlow<List<HomeSpendingCategory>> =
        combine(repository.transactions, monthlySpend) { list, total ->
            val grouped = list.groupBy { it.category }
            grouped.entries.map { (cat, items) ->
                val amt = items.sumOf { it.amount }
                val progress = if (total > 0) (amt / total).toFloat() else 0f
                HomeSpendingCategory(cat, amt, progress)
            }.sortedByDescending { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Add a transaction — delegate to repository so all consumers see it
    fun addTransaction(tx: HomeTransactionItem) {
        val t = Transaction(
            id = tx.id,
            amount = tx.amount,
            category = tx.category,
            paymentMethod = "",
            merchant = tx.title,
            timestamp = System.currentTimeMillis(),
        )
        repository.addExpense(t)
    }

    // Logout

    fun logout(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                prefs.clear()
                try {
                    val clientId = try {
                        context.getString(R.string.default_web_client_id)
                    } catch (_: Exception) {
                        null
                    }
                    if (!clientId.isNullOrBlank() && !clientId.startsWith("REPLACE_WITH")) {
                        Identity.getSignInClient(context).signOut()
                    }
                } catch (_: Throwable) { /* ignore */
                }
            } finally {
                onComplete?.invoke()
            }
        }
    }
}