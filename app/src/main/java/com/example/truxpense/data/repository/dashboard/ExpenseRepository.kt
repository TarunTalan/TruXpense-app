package com.example.truxpense.data.repository.dashboard

import com.example.truxpense.data.local.dao.ExpenseDao
import com.example.truxpense.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

// ── Domain model ─────────────────────────────────────────────────────────────

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val category: String,
    val paymentMethod: String,
    val merchant: String,
    val timestamp: Long = System.currentTimeMillis()
)

// ── Repository ────────────────────────────────────────────────────────────────

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
) {
    /** Cold Flow from Room — emits fresh list on every INSERT/DELETE. */
    val transactions: Flow<List<Transaction>> = expenseDao.getAllExpenses().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun addExpense(transaction: Transaction) {
        expenseDao.insertExpense(transaction.toEntity())
    }

    suspend fun updateExpense(transaction: Transaction) {
        expenseDao.updateExpense(
            id = transaction.id,
            amount = transaction.amount,
            category = transaction.category,
            paymentMethod = transaction.paymentMethod,
            merchant = transaction.merchant,
            timestamp = transaction.timestamp,
        )
    }

    suspend fun deleteExpense(id: String) {
        expenseDao.deleteExpense(id)
    }

    suspend fun clearAll() {
        expenseDao.clearAll()
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun Transaction.toEntity() = ExpenseEntity(
        id = id,
        amount = amount,
        category = category,
        paymentMethod = paymentMethod,
        merchant = merchant,
        timestamp = timestamp,
    )

    private fun ExpenseEntity.toDomain() = Transaction(
        id = id,
        amount = amount,
        category = category,
        paymentMethod = paymentMethod,
        merchant = merchant,
        timestamp = timestamp,
    )
}

// ── Legacy singleton shim ─────────────────────────────────────────────────────
// Kept only as a compile-time safety net; real DI uses @Inject.
// Remove once all call-sites use Hilt injection.
object RepositoryProvider {
    // This will throw at runtime if someone still calls it — intentional.
    val expenseRepository: ExpenseRepository
        get() = error("Use Hilt @Inject instead of RepositoryProvider")
}