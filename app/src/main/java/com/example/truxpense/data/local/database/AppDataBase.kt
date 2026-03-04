package com.example.truxpense.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.truxpense.data.local.dao.ExpenseDao
import com.example.truxpense.data.local.dao.BudgetDao

@Database(
    entities = [com.example.truxpense.data.local.entity.ExpenseEntity::class, com.example.truxpense.data.local.entity.BudgetEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
}