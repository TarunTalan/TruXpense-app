package com.example.truxpense.di

import android.content.Context
import androidx.room.Room
import com.example.truxpense.data.local.database.AppDatabase
import com.example.truxpense.data.local.dao.BudgetDao
import com.example.truxpense.data.local.dao.ExpenseDao
import com.example.truxpense.data.local.dao.IncomeDao
import com.example.truxpense.data.repository.savings.SavingsDao
import com.example.truxpense.data.repository.expense.ExpenseRepository
import com.example.truxpense.data.repository.income.IncomeRepository
import com.example.truxpense.data.repository.savings.SavingsRepository
import com.example.truxpense.data.repository.report.ReportDao
import com.example.truxpense.data.repository.report.ReportRepository
import com.example.truxpense.data.repository.report.ReportRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "truxpense_db")
            .addMigrations(
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
            )
            .build()

    @Provides @Singleton
    fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()

    @Provides @Singleton
    fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()

    @Provides @Singleton
    fun provideIncomeDao(db: AppDatabase): IncomeDao = db.incomeDao()

    @Provides @Singleton
    fun provideSavingsDao(db: AppDatabase): SavingsDao = db.savingsDao()

    @Provides @Singleton
    fun provideReportDao(db: AppDatabase): ReportDao = db.reportDao()

    @Provides @Singleton
    fun provideExpenseRepository(dao: ExpenseDao): ExpenseRepository = ExpenseRepository(dao)

    @Provides @Singleton
    fun provideIncomeRepository(dao: IncomeDao): IncomeRepository = IncomeRepository(dao)

    @Provides @Singleton
    fun provideSavingsRepository(dao: SavingsDao): SavingsRepository = SavingsRepository(dao)

    @Provides @Singleton
    fun provideReportRepository(dao: ReportDao): ReportRepository = ReportRepositoryImpl(dao)
}