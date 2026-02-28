package com.example.truxpense.di

import com.example.truxpense.data.budget.BudgetRepository as LegacyBudgetRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LegacyBudgetModule {

    @Provides
    @Singleton
    fun provideLegacyBudgetRepository(): LegacyBudgetRepository = LegacyBudgetRepository
}

