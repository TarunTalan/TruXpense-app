package com.example.truxpense.data.budget

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class Budget(
    val id: String = UUID.randomUUID().toString(),
    val category: String,
    val amount: Double
)

object BudgetRepository {
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets = _budgets.asStateFlow()

    fun addBudget(budget: Budget) {
        _budgets.value = _budgets.value + budget
    }

    fun clear() {
        _budgets.value = emptyList()
    }
}

