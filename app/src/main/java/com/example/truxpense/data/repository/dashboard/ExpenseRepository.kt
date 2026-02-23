package com.example.truxpense.data.repository.dashboard


import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val category: String,
    val paymentMethod: String,
    val merchant: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ExpenseRepository {

    private val _transactions =
        MutableStateFlow<List<Transaction>>(emptyList())

    val transactions: StateFlow<List<Transaction>> = _transactions

    fun addExpense(transaction: Transaction) {
        // Prevent inserting the same transaction twice (id collision / double save)
        val exists = _transactions.value.any { it.id == transaction.id }
        if (exists) return
        _transactions.update { it + transaction }
    }

}
object RepositoryProvider {
    val expenseRepository = ExpenseRepository()
}