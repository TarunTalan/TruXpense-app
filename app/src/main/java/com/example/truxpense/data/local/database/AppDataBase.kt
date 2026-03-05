package com.example.truxpense.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.truxpense.data.local.dao.BudgetDao
import com.example.truxpense.data.local.dao.ExpenseDao
import com.example.truxpense.data.local.dao.IncomeDao
import com.example.truxpense.data.local.entity.BudgetEntity
import com.example.truxpense.data.local.entity.ExpenseEntity
import com.example.truxpense.data.local.entity.IncomeEntity

@Database(
    entities = [ExpenseEntity::class, BudgetEntity::class, IncomeEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun incomeDao(): IncomeDao
}