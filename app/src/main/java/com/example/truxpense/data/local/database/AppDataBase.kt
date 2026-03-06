package com.example.truxpense.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.truxpense.data.local.dao.BudgetDao
import com.example.truxpense.data.local.dao.ExpenseDao
import com.example.truxpense.data.local.dao.IncomeDao
import com.example.truxpense.data.local.dao.SavingsDao
import com.example.truxpense.data.local.entity.BudgetEntity
import com.example.truxpense.data.local.entity.ExpenseEntity
import com.example.truxpense.data.local.entity.IncomeEntity
import com.example.truxpense.data.local.entity.SavingsEntity

@Database(
    entities = [ExpenseEntity::class, BudgetEntity::class, IncomeEntity::class, SavingsEntity::class],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun incomeDao(): IncomeDao
    abstract fun savingsDao(): SavingsDao

    companion object {
        /** Adds the `savings` table introduced in version 5. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `savings` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `goalName` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `notes` TEXT NOT NULL DEFAULT '',
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}