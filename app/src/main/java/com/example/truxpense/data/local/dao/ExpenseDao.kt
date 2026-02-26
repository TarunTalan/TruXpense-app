package com.example.truxpense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.truxpense.data.local.entity.ExpenseEntity

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY timestamp DESC")
    fun getExpensesByCategory(category: String): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalSpent(): Flow<Double?>

    @Query(
        """UPDATE expenses
           SET amount = :amount,
               category = :category,
               paymentMethod = :paymentMethod,
               merchant = :merchant,
               timestamp = :timestamp
           WHERE id = :id"""
    )
    suspend fun updateExpense(
        id: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        merchant: String,
        timestamp: Long,
    )

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: String)

    @Query("DELETE FROM expenses")
    suspend fun clearAll()
}