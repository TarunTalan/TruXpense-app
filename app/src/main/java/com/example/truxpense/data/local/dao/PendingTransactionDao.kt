package com.example.truxpense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.truxpense.data.sms.model.TxnState
import com.example.truxpense.data.local.entity.PendingTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PendingTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<PendingTransactionEntity>)

    @Update
    suspend fun update(entity: PendingTransactionEntity)

    @Query("UPDATE pending_transactions SET state = :state WHERE id = :id")
    suspend fun updateState(id: String, state: String)

    @Query("UPDATE pending_transactions SET category = :category, state = :state WHERE id = :id")
    suspend fun confirmWithCategory(id: String, category: String, state: String = TxnState.CONFIRMED.name)

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM pending_transactions WHERE state = :state")
    suspend fun deleteByState(state: String = TxnState.REJECTED.name)

    @Query("SELECT * FROM pending_transactions WHERE state = 'PENDING' ORDER BY timestamp DESC")
    fun observePending(): Flow<List<PendingTransactionEntity>>

    @Query("SELECT COUNT(*) FROM pending_transactions WHERE state = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM pending_transactions WHERE state = 'CONFIRMED' ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getConfirmedForSync(limit: Int = 50): List<PendingTransactionEntity>

    @Query("SELECT COUNT(*) FROM pending_transactions WHERE state = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM pending_transactions WHERE id = :id LIMIT 1)")
    suspend fun exists(id: String): Boolean

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM pending_transactions
            WHERE ABS(amount - :amount) < 0.01
              AND ABS(timestamp - :timestamp) <= 30000
              AND txn_type = :txnType
            LIMIT 1
        )
    """)
    suspend fun isDuplicate(amount: Double, timestamp: Long, txnType: String): Boolean
}

