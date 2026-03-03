package com.example.truxpense.di

// Auth bindings

import android.content.Context
import com.example.truxpense.data.session.AuthSessionManager
import com.example.truxpense.data.local.datastore.AuthPreferences
import com.example.truxpense.data.remote.api.AuthApi
import com.example.truxpense.data.remote.api.OnboardingApi
import com.example.truxpense.data.repository.auth.AuthRepository
import com.example.truxpense.data.repository.auth.GoogleSignInRepository
import com.example.truxpense.data.repository.onboarding.OnboardingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    /**
     * Manages session logout events.
     * UI components collect logoutEvents to navigate to login.
     */
    @Provides
    @Singleton
    fun provideAuthSessionManager(prefs: AuthPreferences): AuthSessionManager {
        return AuthSessionManager(prefs)
    }

    /**
     * Google Sign-In repository for handling Google OAuth flow
     */
    @Provides
    @Singleton
    fun provideGoogleSignInRepository(
        @ApplicationContext context: Context
    ): GoogleSignInRepository {
        return GoogleSignInRepository(context)
    }

    /**
     * Main authentication repository.
     * Handles login, signup, OTP verification, etc.
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        api: AuthApi,
        prefs: AuthPreferences
    ): AuthRepository {
        return AuthRepository(api, prefs)
    }

    /**
     * Onboarding repository for handling user onboarding process
     */
    @Provides
    @Singleton
    fun provideOnboardingRepository(
        onboardingApi: OnboardingApi,
        prefs: AuthPreferences
    ): OnboardingRepository {
        return OnboardingRepository(onboardingApi, prefs)
    }
}