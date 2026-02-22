package com.example.truxpense.presentation.screens.dashboard.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.R
import com.example.truxpense.data.prefs.AuthPreferences
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

    // Expense / spend data

    private val _expenseCount = MutableStateFlow(0)
    val expenseCount: StateFlow<Int> = _expenseCount.asStateFlow()

    private val _monthlySpend = MutableStateFlow(0.0)
    val monthlySpend: StateFlow<Double> = _monthlySpend.asStateFlow()

    private val _budgetLimit = MutableStateFlow(0.0)
    val budgetLimit: StateFlow<Double> = _budgetLimit.asStateFlow()

    private val _budgetLeft = MutableStateFlow(0.0)
    val budgetLeft: StateFlow<Double> = _budgetLeft.asStateFlow()

    // Derived values (budget consumption)

    /**
     * Amount consumed = limit − left, clamped to ≥ 0.
     * Replaces `val budgetUsed = (budgetLimit - budgetLeft).coerceAtLeast(0.0)` in the screen.
     */
    val budgetUsed: StateFlow<Double> =
        combine(_budgetLimit, _budgetLeft) { limit, left -> (limit - left).coerceAtLeast(0.0) }.stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                0.0
            )


    val budgetProgress: StateFlow<Float> = combine(_budgetLimit, budgetUsed) { limit, used ->
        if (limit > 0) (used / limit).toFloat().coerceIn(0f, 1f) else 0f
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0f)

    // Setters (called from repository layer or tests)

    fun setExpenseCount(count: Int) {
        _expenseCount.value = count
    }

    fun setBudgetLimit(limit: Double) {
        _budgetLimit.value = limit
    }

    fun setBudgetLeft(left: Double) {
        _budgetLeft.value = left
    }

    fun setBudget(limit: Double, left: Double) {
        _budgetLimit.value = limit
        _budgetLeft.value = left
    }

    // Add a transaction (called from AddExpense flow)
    fun addTransaction(tx: HomeTransactionItem) {
        _expenseCount.value += 1
        _monthlySpend.value += tx.amount
        _recentTx.value = _recentTx.value.toMutableList().apply { add(0, tx) }
    }

    // UI lists

    private val _topCategories = MutableStateFlow(
        listOf(
            HomeSpendingCategory("Food", 2_400.0, 0.72f),
            HomeSpendingCategory("Clothes", 4_500.0, 0.45f),
        )
    )
    val topCategories: StateFlow<List<HomeSpendingCategory>> = _topCategories.asStateFlow()

    private val _recentTx = MutableStateFlow<List<HomeTransactionItem>>(emptyList())
    val recentTransactions: StateFlow<List<HomeTransactionItem>> = _recentTx.asStateFlow()

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