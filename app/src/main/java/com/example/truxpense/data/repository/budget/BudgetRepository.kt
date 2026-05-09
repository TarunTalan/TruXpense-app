package com.example.truxpense.data.repository.budget

import com.example.truxpense.data.local.dao.BudgetDao
import com.example.truxpense.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ── Domain model ─────────────────────────────────────────────────────────────

data class Budget(
    val id: String = UUID.randomUUID().toString(),
    val category: String,
    val amount: Double,
    val createdAt: Long = System.currentTimeMillis(),
)

// ── Repository ────────────────────────────────────────────────────────────────

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
) {
    /** Cold Flow from Room — emits fresh list on every INSERT/DELETE. */
    val budgets: Flow<List<Budget>> = budgetDao.getAllBudgets().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun addBudget(budget: Budget) {
        budgetDao.insertBudget(budget.toEntity())
    }

    suspend fun getBudgetByCategory(category: String): Budget? {
        return budgetDao.getBudgetByCategory(category)?.toDomain()
    }

    suspend fun deleteBudget(id: String) {
        budgetDao.deleteBudget(id)
    }

    suspend fun deleteBudgetByCategory(category: String) {
        budgetDao.deleteBudgetByCategory(category)
    }

    suspend fun clear() {
        budgetDao.clearAll()
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun Budget.toEntity() = BudgetEntity(
        id = id,
        category = category,
        amount = amount,
        createdAt = createdAt,
    )

    private fun BudgetEntity.toDomain() = Budget(
        id = id,
        category = category,
        amount = amount,
        createdAt = createdAt,
    )
}