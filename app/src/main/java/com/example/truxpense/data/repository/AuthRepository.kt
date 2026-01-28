package com.example.truxpense.data.repository

import com.example.truxpense.data.remote.AuthApi
import com.example.truxpense.data.remote.LoginRequest
import com.example.truxpense.data.remote.RefreshRequest
import com.example.truxpense.data.prefs.AuthPreferences
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val prefs: AuthPreferences
) {
    suspend fun login(username: String, password: String): Boolean {
        val response = api.login(LoginRequest(username, password))
        if (response.isSuccessful) {
            response.body()?.let {
                prefs.saveTokens(it.accessToken, it.refreshToken, it.expiresIn)
                return true
            }
        }
        return false
    }

    suspend fun refresh(): Boolean {
        val refreshToken = prefs.refreshToken.first() ?: return false
        val response = api.refresh(RefreshRequest(refreshToken))
        if (response.isSuccessful) {
            response.body()?.let {
                prefs.saveTokens(it.accessToken, it.refreshToken, it.expiresIn)
                return true
            }
        }
        return false
    }

    suspend fun logout() {
        prefs.clear()
    }

    suspend fun getAccessToken(): String? = prefs.accessToken.first()
}
