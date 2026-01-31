package com.example.truxpense.data.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Handles 401 responses by attempting to refresh the access token.
 * Uses the injected TokenRefresher (which is properly synchronized) and emits logout events
 * via AuthSessionManager when refresh fails.
 */
class AuthAuthenticator(
    private val tokenRefresher: TokenRefresher,
    private val sessionManager: AuthSessionManager
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite retry loops
        if (responseCount(response) >= 2) {
            sessionManager.handleRefreshFailure()
            return null
        }

        val failedRequest = response.request
        val failedPath = failedRequest.url.encodedPath

        // Don't authenticate public endpoints
        if (failedRequest.header("No-Auth") != null || failedPath.startsWith("/api/auth/")) {
            return null
        }

        // Attempt to refresh the token via the injected TokenRefresher
        val body = try {
            runBlocking { tokenRefresher.refresh() }
        } catch (_: Exception) {
            null
        }

        if (body != null) {
            val newAccess = body.accessToken
            return response.request.newBuilder()
                .header("Authorization", "Bearer $newAccess")
                .build()
        }

        // Refresh failed -> clear session and notify listeners
        sessionManager.handleRefreshFailure()
        return null
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var current = response.priorResponse
        while (current != null) {
            count++
            current = current.priorResponse
        }
        return count
    }
}