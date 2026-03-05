package com.example.truxpense.data.local.dao

import androidx.room.*
import com.example.truxpense.data.local.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity)

    @Query("SELECT * FROM income ORDER BY timestamp DESC")
    fun getAllIncome(): Flow<List<IncomeEntity>>

    /** Sum of all income entries whose timestamp falls in [startMs, endMs]. */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM income WHERE timestamp >= :startMs AND timestamp <= :endMs")
    fun getTotalIncomeBetween(startMs: Long, endMs: Long): Flow<Double>

    @Query("DELETE FROM income WHERE id = :id")
    suspend fun deleteIncome(id: String)

    @Query("DELETE FROM income")
    suspend fun clearAll()
}

