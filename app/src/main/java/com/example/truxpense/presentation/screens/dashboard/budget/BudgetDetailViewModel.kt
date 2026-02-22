package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetDetailViewModel @Inject constructor() : ViewModel() {
    private val _budgetName = MutableStateFlow("Food Budget")
    val budgetName: StateFlow<String> = _budgetName.asStateFlow()

    private val _monthlyLimit = MutableStateFlow(5000.0)
    val monthlyLimit: StateFlow<Double> = _monthlyLimit.asStateFlow()

    private val _spent = MutableStateFlow(4200.0)
    val spent: StateFlow<Double> = _spent.asStateFlow()

    private val _transactions: MutableStateFlow<List<BudgetTransaction>> = MutableStateFlow(sampleTransactions)
    val transactions: StateFlow<List<BudgetTransaction>> = _transactions.asStateFlow()

    private val _spendPoints: MutableStateFlow<List<SpendPoint>> = MutableStateFlow(sampleSpendPoints)
    val spendPoints: StateFlow<List<SpendPoint>> = _spendPoints.asStateFlow()

    fun loadBudget(name: String, limit: Double, spentAmount: Double) {
        viewModelScope.launch {
            _budgetName.value = name
            _monthlyLimit.value = limit
            _spent.value = spentAmount
            // in real implementation, load transactions and spendPoints from repository
        }
    }
}

// Models used by the detail screen (kept with the ViewModel)
data class BudgetTransaction(
    val id: String,
    val amount: Double,
    val type: String,          // "Expense" | "Income"
    val addedFrom: String,
    val merchant: String,
    val category: String,
    val account: String,
    val date: String,
    val time: String,
)

data class SpendPoint(val dayLabel: String, val amount: Double)

enum class PeriodTab { WEEK, MONTH }

// Sample data moved here so ViewModel owns it
val sampleTransactions = listOf(
    BudgetTransaction(
        id = "1",
        amount = 1200.0,
        type = "Expense",
        addedFrom = "ADDED FROM SMS",
        merchant = "Swiggy",
        category = "Food",
        account = "HDFC Bank",
        date = "12 Feb 2026",
        time = "8:45 PM"
    ), BudgetTransaction(
        id = "2",
        amount = 450.0,
        type = "Expense",
        addedFrom = "ADDED FROM SMS",
        merchant = "Zomato",
        category = "Food",
        account = "SBI Bank",
        date = "10 Feb 2026",
        time = "1:15 PM"
    )
)

val sampleSpendPoints = listOf(
    SpendPoint("Mon", 300.0),
    SpendPoint("Tue", 1200.0),
    SpendPoint("Wed", 600.0),
    SpendPoint("Thu", 900.0),
    SpendPoint("Fri", 500.0),
    SpendPoint("Sat", 800.0),
    SpendPoint("Sun", 400.0),
)
