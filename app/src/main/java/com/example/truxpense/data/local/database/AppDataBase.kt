package com.example.truxpense.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.truxpense.data.local.dao.BudgetDao
import com.example.truxpense.data.local.dao.ExpenseDao
import com.example.truxpense.data.local.dao.IncomeDao
import com.example.truxpense.data.local.entity.BudgetEntity
import com.example.truxpense.data.local.entity.ExpenseEntity
import com.example.truxpense.data.local.entity.IncomeEntity
import com.example.truxpense.data.local.entity.SavingsEntity
import com.example.truxpense.data.repository.report.Report
import com.example.truxpense.data.repository.report.ReportDao
import com.example.truxpense.data.repository.savings.SavingsContribution
import com.example.truxpense.data.repository.savings.SavingsDao
import com.example.truxpense.data.repository.savings.SavingsEntry
import com.example.truxpense.data.repository.savings.SavingsGoal
import com.example.truxpense.data.repository.vault.VaultEntry
import com.example.truxpense.data.repository.vault.VaultEntryDao

@Database(
    entities = [
        ExpenseEntity::class,
        BudgetEntity::class,
        IncomeEntity::class,
        SavingsEntity::class,
        SavingsGoal::class,
        SavingsContribution::class,
        SavingsEntry::class,
        Report::class,
        VaultEntry::class,
    ],
    version = 10,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun incomeDao(): IncomeDao
    abstract fun savingsDao(): SavingsDao
    abstract fun reportDao(): ReportDao
    abstract fun vaultEntryDao(): VaultEntryDao

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

        /** Adds paymentMethod column to income table introduced in version 6. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `income` ADD COLUMN `paymentMethod` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        /** Adds savings_goals, savings_contributions, savings_entries tables (version 7). */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `savings_goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `icon` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `targetAmount` REAL NOT NULL,
                        `savedAmount` REAL NOT NULL DEFAULT 0.0,
                        `targetDateEpoch` INTEGER NOT NULL,
                        `autoContribute` INTEGER NOT NULL DEFAULT 0,
                        `autoContributeAmount` REAL NOT NULL DEFAULT 500.0,
                        `autoContributeFrequency` TEXT NOT NULL DEFAULT 'DAILY',
                        `createdAt` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `savings_contributions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `goalId` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        `label` TEXT NOT NULL,
                        `timestampMs` INTEGER NOT NULL,
                        FOREIGN KEY(`goalId`) REFERENCES `savings_goals`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_contributions_goalId` ON `savings_contributions`(`goalId`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `savings_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `label` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `timestampMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /** Adds the `reports` table introduced in version 8. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reports` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `fromDate` INTEGER NOT NULL,
                        `toDate` INTEGER NOT NULL,
                        `reportType` TEXT NOT NULL DEFAULT 'EXPENSE',
                        `categories` TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
            }
        }

        /** Adds the `vault_entries` table introduced in version 9. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `vault_entries` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `reportId` TEXT NOT NULL DEFAULT '',
                        `title` TEXT NOT NULL,
                        `format` TEXT NOT NULL,
                        `reportType` TEXT NOT NULL DEFAULT '',
                        `dateRangeLabel` TEXT NOT NULL DEFAULT '',
                        `localFilePath` TEXT NOT NULL DEFAULT '',
                        `cloudUrl` TEXT NOT NULL DEFAULT '',
                        `storagePath` TEXT NOT NULL DEFAULT '',
                        `fileSizeBytes` INTEGER NOT NULL DEFAULT 0,
                        `tags` TEXT NOT NULL DEFAULT '',
                        `syncStatus` TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
                        `storageOption` TEXT NOT NULL DEFAULT 'BOTH',
                        `savedAt` INTEGER NOT NULL,
                        `uploadedAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        /** Adds `celebrationShown` column to savings_goals (version 10). */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `savings_goals` ADD COLUMN `celebrationShown` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}