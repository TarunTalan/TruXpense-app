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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ViewModel for home screen managing logout and SMS permission state
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prefs: AuthPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Expose stored username Flow from preferences so UI can collect and display it
    val username: Flow<String?> = prefs.username

    // SMS permission state (true when READ_SMS is granted)
    private val _hasSmsPermission = MutableStateFlow(
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    )
    val hasSmsPermission: StateFlow<Boolean> = _hasSmsPermission

    // Simple expense count state (0 means no expenses yet). UI can observe to show empty state.
    private val _expenseCount = MutableStateFlow(0)
    val expenseCount: StateFlow<Int> = _expenseCount

    // Monthly spend amount in major currency units (e.g., rupees/dollars). Default 0.0
    private val _monthlySpend = MutableStateFlow(0.0)
    val monthlySpend: StateFlow<Double> = _monthlySpend

    // Helper to set monthly spend (call after calculating from transactions)
    fun setMonthlySpend(amount: Double) {
        _monthlySpend.value = amount
    }

    // Budget state: total budget limit and amount left
    private val _budgetLimit = MutableStateFlow(0.0)
    val budgetLimit: StateFlow<Double> = _budgetLimit

    private val _budgetLeft = MutableStateFlow(0.0)
    val budgetLeft: StateFlow<Double> = _budgetLeft

    // Helpers to set budget values (call after calculating budget usage)
    fun setBudgetLimit(limit: Double) { _budgetLimit.value = limit }
    fun setBudgetLeft(left: Double) { _budgetLeft.value = left }
    fun setBudget(limit: Double, left: Double) {
        _budgetLimit.value = limit
        _budgetLeft.value = left
    }

    // Helper to set expense count (call this from repo or after loading transactions)
    fun setExpenseCount(count: Int) {
        _expenseCount.value = count
    }

    // One-shot events to request permission from UI (not strictly required but available)
    private val _requestSmsPermission = MutableSharedFlow<Unit>(replay = 0)
    val requestSmsPermission = _requestSmsPermission // consumers can collect to trigger launcher

    fun refreshSmsPermission() {
        _hasSmsPermission.value = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    }

    fun onSmsPermissionResult(granted: Boolean) {
        _hasSmsPermission.value = granted
    }

    fun emitRequestSmsPermission() {
        viewModelScope.launch {
            _requestSmsPermission.emit(Unit)
        }
    }

    // Sample UI lists (replace with real data later) — expose as StateFlows
    private val _topCategories = MutableStateFlow(listOf(
        HomeSpendingCategory("Food", 2400.0, 0.72f),
        HomeSpendingCategory("Clothes", 4500.0, 0.45f)
    ))
    val topCategories: StateFlow<List<HomeSpendingCategory>> = _topCategories

    private val _recentTx = MutableStateFlow(listOf(
        HomeTransactionItem("1", "Grocery at BigMart", "Food", 540.0, "INR"),
        HomeTransactionItem("2", "Coffee", "Food", 120.0, "INR"),
        HomeTransactionItem("3", "Electricity Bill", "Bills", 2400.0, "INR")
    ))
    val recentTransactions: StateFlow<List<HomeTransactionItem>> = _recentTx

    fun logout(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                // clear all saved preferences (tokens, username, onboarding flags)
                prefs.clear()

                // Attempt to sign out from Google Identity (One Tap) if configured. Ignore failures.
                try {
                    val clientId = try { context.getString(R.string.default_web_client_id) } catch (_: Exception) { null }
                    if (!clientId.isNullOrBlank() && !clientId.startsWith("REPLACE_WITH")) {
                        val oneTapClient = Identity.getSignInClient(context)
                        oneTapClient.signOut().addOnCompleteListener { /* no-op */ }
                    }
                } catch (_: Throwable) {
                    // ignore sign-out errors; logout still proceeds
                }
            } finally {
                onComplete?.invoke()
            }
        }
    }
}
