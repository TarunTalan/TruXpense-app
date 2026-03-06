package com.example.truxpense.data.repository.savings

import com.example.truxpense.data.local.dao.SavingsDao
import com.example.truxpense.data.local.entity.SavingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ── Domain model ──────────────────────────────────────────────────────────────

data class SavingsEntry(
    val id: String = UUID.randomUUID().toString(),
    val goalName: String,
    val amount: Double,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

// ── Repository ────────────────────────────────────────────────────────────────

@Singleton
class SavingsRepository @Inject constructor(
    private val dao: SavingsDao,
) {
    /** All savings entries, newest first. */
    val allSavings: Flow<List<SavingsEntry>> = dao.getAll().map { list ->
        list.map { it.toDomain() }
    }

    /** Live total savings between two epoch-ms timestamps. */
    fun totalSavingsBetween(startMs: Long, endMs: Long): Flow<Double> =
        dao.getTotalSavingsBetween(startMs, endMs)

    suspend fun addSavings(entry: SavingsEntry) {
        dao.insert(entry.toEntity())
    }

    suspend fun updateSavings(entry: SavingsEntry) {
        dao.update(
            id = entry.id,
            goalName = entry.goalName,
            amount = entry.amount,
            notes = entry.notes,
            timestamp = entry.timestamp,
        )
    }

    suspend fun getById(id: String): SavingsEntry? = dao.getById(id)?.toDomain()

    suspend fun deleteSavings(id: String) {
        dao.deleteById(id)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun SavingsEntry.toEntity() = SavingsEntity(
        id = id,
        goalName = goalName,
        amount = amount,
        notes = notes,
        timestamp = timestamp,
    )

    private fun SavingsEntity.toDomain() = SavingsEntry(
        id = id,
        goalName = goalName,
        amount = amount,
        notes = notes,
        timestamp = timestamp,
    )
}

