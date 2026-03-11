package com.example.truxpense.data.repository.savings

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ──────────────────────────────────────────────────────────────────────────────
// Entities
// ──────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,           // emoji char e.g. "📱"
    val colorHex: String,       // e.g. "#9B59F5"
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val targetDateEpoch: Long,  // LocalDate.toEpochDay()
    val autoContribute: Boolean = false,
    val autoContributeAmount: Double = 500.0,
    val autoContributeFrequency: ContributeFrequency = ContributeFrequency.DAILY,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
)

enum class ContributeFrequency { DAILY, WEEKLY, MONTHLY }

@Entity(
    tableName = "savings_contributions",
    foreignKeys = [ForeignKey(
        entity = SavingsGoal::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("goalId")]
)
data class SavingsContribution(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val amount: Double,
    val label: String,          // "Auto-save" | "Manual add"
    val timestampMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "savings_entries")
data class SavingsEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val amount: Double,
    val timestampMs: Long = System.currentTimeMillis(),
)

// ──────────────────────────────────────────────────────────────────────────────
// DAO
// ──────────────────────────────────────────────────────────────────────────────

@Dao
interface SavingsDao {

    // Goals
    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    fun observeGoals(): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    fun observeGoal(id: Long): Flow<SavingsGoal?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: SavingsGoal): Long

    @Delete
    suspend fun deleteGoal(goal: SavingsGoal)

    @Query("UPDATE savings_goals SET savedAmount = savedAmount + :delta WHERE id = :id")
    suspend fun addToSaved(id: Long, delta: Double)

    @Query("UPDATE savings_goals SET savedAmount = :amount WHERE id = :id")
    suspend fun setSaved(id: Long, amount: Double)

    @Query("UPDATE savings_goals SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long)

    // Contributions
    @Query("SELECT * FROM savings_contributions WHERE goalId = :goalId ORDER BY timestampMs DESC LIMIT 20")
    fun observeContributions(goalId: Long): Flow<List<SavingsContribution>>

    @Insert
    suspend fun insertContribution(c: SavingsContribution)

    // Savings entries (total savings pool)
    @Query("SELECT * FROM savings_entries ORDER BY timestampMs DESC LIMIT 10")
    fun observeEntries(): Flow<List<SavingsEntry>>

    @Insert
    suspend fun insertEntry(e: SavingsEntry)

    @Query("SELECT COALESCE(SUM(amount),0) FROM savings_entries")
    fun observeTotalSavings(): Flow<Double>
}