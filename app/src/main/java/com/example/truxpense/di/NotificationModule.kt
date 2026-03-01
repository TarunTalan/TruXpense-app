package com.example.truxpense.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Top-level extension — creates a single DataStore file named "notification_prefs.preferences_pb"
private val Context.notificationDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "notification_prefs")

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {


    @Provides
    @Singleton
    fun provideNotificationDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.notificationDataStore
}