package com.example.truxpense.data.auth

import com.example.truxpense.data.prefs.AuthPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache of the access token using StateFlow for proper initialization.
 * Ensures token is available immediately when needed.
 */
@Singleton
class TokenManager @Inject constructor(
    prefs: AuthPreferences,
    appScope: CoroutineScope
) {
    // StateFlow ensures we always have a value (even if null) immediately
    private val tokenFlow: StateFlow<String?> = prefs.accessToken
        .stateIn(
            scope = appScope,
            started = SharingStarted.Eagerly, // Start immediately
            initialValue = null
        )

    // no-op init

    fun getToken(): String? = tokenFlow.value
}