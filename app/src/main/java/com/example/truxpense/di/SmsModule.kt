package com.example.truxpense.di

import android.content.Context
import com.example.truxpense.data.local.database.SmsDatabase
import com.example.truxpense.data.local.dao.PendingTransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SmsModule {

    @Provides
    @Singleton
    fun provideSmsDatabase(@ApplicationContext context: Context): SmsDatabase =
        SmsDatabase.getInstance(context)

    @Provides
    @Singleton
    fun providePendingTransactionDao(db: SmsDatabase): PendingTransactionDao =
        db.pendingTransactionDao()
}

