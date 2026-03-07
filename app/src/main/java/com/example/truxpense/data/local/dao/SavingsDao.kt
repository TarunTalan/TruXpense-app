package com.example.truxpense.data.local.dao

import androidx.room.*
import com.example.truxpense.data.local.entity.SavingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(savings: SavingsEntity)

    @Query("UPDATE savings SET goalName=:goalName, amount=:amount, notes=:notes, timestamp=:timestamp WHERE id=:id")
    suspend fun update(id: String, goalName: String, amount: Double, notes: String, timestamp: Long)

    @Query("SELECT * FROM savings ORDER BY timestamp DESC")
    fun getAll(): Flow<List<SavingsEntity>>

    @Query("SELECT * FROM savings WHERE id = :id")
    suspend fun getById(id: String): SavingsEntity?

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM savings WHERE timestamp >= :startMs AND timestamp <= :endMs")
    fun getTotalSavingsBetween(startMs: Long, endMs: Long): Flow<Double>

    @Query("DELETE FROM savings WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM savings")
    suspend fun clearAll()
}

