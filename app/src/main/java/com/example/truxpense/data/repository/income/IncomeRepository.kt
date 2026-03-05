package com.example.truxpense.data.repository.income

import com.example.truxpense.data.local.dao.IncomeDao
import com.example.truxpense.data.local.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ── Domain model ──────────────────────────────────────────────────────────────

data class Income(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val source: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

// ── Repository ────────────────────────────────────────────────────────────────

@Singleton
class IncomeRepository @Inject constructor(
    private val incomeDao: IncomeDao,
) {
    /** All income entries, newest first. */
    val allIncome: Flow<List<Income>> = incomeDao.getAllIncome().map { list ->
        list.map { it.toDomain() }
    }

    /** Live total income between two epoch-ms timestamps. */
    fun totalIncomeBetween(startMs: Long, endMs: Long): Flow<Double> =
        incomeDao.getTotalIncomeBetween(startMs, endMs)

    suspend fun addIncome(income: Income) {
        incomeDao.insertIncome(income.toEntity())
    }

    suspend fun deleteIncome(id: String) {
        incomeDao.deleteIncome(id)
    }

    suspend fun clearAll() {
        incomeDao.clearAll()
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun Income.toEntity() = IncomeEntity(
        id = id,
        amount = amount,
        source = source,
        notes = notes,
        timestamp = timestamp,
    )

    private fun IncomeEntity.toDomain() = Income(
        id = id,
        amount = amount,
        source = source,
        notes = notes,
        timestamp = timestamp,
    )
}

