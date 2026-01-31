package com.example.truxpense.data.auth

import com.example.truxpense.data.prefs.AuthPreferences
import com.example.truxpense.data.remote.api.AuthApi
import com.example.truxpense.data.remote.api.RefreshRequest
import com.example.truxpense.data.remote.api.TokenResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages token refresh with proper synchronization.
 * Uses injected AuthApi instead of creating new Retrofit instances.
 */
@Singleton
class TokenRefresher @Inject constructor(
    private val authApi: AuthApi,
    private val prefs: AuthPreferences
) {
    private val mutex = Mutex()

    /**
     * Refreshes the access token. Only one refresh runs at a time.
     * Other callers wait for the result.
     */
    suspend fun refresh(): TokenResponse? = mutex.withLock {
        val refreshToken = prefs.refreshToken.first()
        if (refreshToken.isNullOrBlank()) return null

        try {
            val response = authApi.refresh(RefreshRequest(refreshToken))

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // Persist tokens using AuthPreferences.saveTokens(access, refresh, expiresIn)
                    prefs.saveTokens(
                        body.accessToken.orEmpty(),
                        body.refreshToken ?: refreshToken,
                        body.expiresIn ?: 0L
                    )
                    return body
                }
            }

            // Server returned error or empty body
            null
        } catch (_: Exception) {
            // Network error or other exception
            null
        }
    }
}