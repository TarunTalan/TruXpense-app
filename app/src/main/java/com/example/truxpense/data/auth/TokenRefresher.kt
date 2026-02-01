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

// Manages token refresh with proper synchronization
@Singleton
class TokenRefresher @Inject constructor(
    private val authApi: AuthApi,
    private val prefs: AuthPreferences
) {
    private val mutex = Mutex()

    // Refreshes the access token; only one refresh runs at a time
    suspend fun refresh(): TokenResponse? = mutex.withLock {
        val refreshToken = prefs.refreshToken.first()
        if (refreshToken.isNullOrBlank()) return null

        try {
            val response = authApi.refresh(RefreshRequest(refreshToken))

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    prefs.saveTokens(
                        body.accessToken.orEmpty(),
                        body.refreshToken ?: refreshToken,
                        body.expiresIn ?: 0L
                    )
                    return body
                }
            }

            null
        } catch (_: Exception) {
            null
        }
    }
}