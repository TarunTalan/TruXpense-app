package com.example.truxpense.di

import android.content.Context
import com.example.truxpense.data.auth.AuthSessionManager
import com.example.truxpense.data.prefs.AuthPreferences
import com.example.truxpense.data.remote.api.AuthApi
import com.example.truxpense.data.remote.api.OnboardingApi
import com.example.truxpense.data.repository.AuthRepository
import com.example.truxpense.data.repository.GoogleSignInRepository
import com.example.truxpense.data.repository.OnboardingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module providing auth-specific dependencies.
 *
 * Note: Network-related auth components (TokenManager, TokenRefresher, etc.)
 * are now in NetworkModule to maintain proper dependency hierarchy.
 */
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