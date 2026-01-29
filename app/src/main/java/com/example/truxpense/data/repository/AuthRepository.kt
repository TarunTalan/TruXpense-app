package com.example.truxpense.data.repository

import com.example.truxpense.data.prefs.AuthPreferences
import com.example.truxpense.data.remote.api.AuthApi
import com.example.truxpense.data.remote.api.TokenResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val prefs: AuthPreferences
) {
    suspend fun login(username: String, password: String): Boolean {
        val response = api.login(com.example.truxpense.data.remote.api.LoginRequest(username, password))
        if (response.isSuccessful) {
            response.body()?.let {
                prefs.saveTokens(it.accessToken ?: "", it.refreshToken ?: "", it.expiresIn ?: 0L)
                return true
            }
        }
        return false
    }

    suspend fun refresh(): Boolean {
        val refreshToken = prefs.refreshToken.first() ?: return false
        val response = api.refresh(com.example.truxpense.data.remote.api.RefreshRequest(refreshToken))
        if (response.isSuccessful) {
            response.body()?.let {
                prefs.saveTokens(it.accessToken ?: "", it.refreshToken ?: "", it.expiresIn ?: 0L)
                return true
            }
        }
        return false
    }

    suspend fun logout() {
        prefs.clear()
    }

    suspend fun getAccessToken(): String? = prefs.accessToken.first()

    // Google OAuth: send ID token to backend and return TokenResponse via Result
    suspend fun sendIdTokenToServer(idToken: String): Result<TokenResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val resp = api.loginWithGoogle(mapOf("idToken" to idToken))
                if (resp.isSuccessful) {
                    val body = resp.body() ?: TokenResponse(null, null)
                    // Persist tokens if provided
                    body.accessToken?.let { at -> body.refreshToken?.let { rt -> prefs.saveTokens(at, rt, body.expiresIn ?: 0L) } }
                    Result.success(body)
                } else {
                    Result.failure(Exception("Server error: ${resp.code()} - ${resp.errorBody()?.string()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}