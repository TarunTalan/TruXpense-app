package com.example.truxpense.data.repository

import com.example.truxpense.data.prefs.AuthPreferences
import com.example.truxpense.data.remote.api.AuthApi
import com.example.truxpense.data.remote.api.LoginRequest
import com.example.truxpense.data.remote.api.RefreshRequest
import com.example.truxpense.data.remote.api.TokenResponse
import com.example.truxpense.data.remote.api.VerifySignupRequest
import com.example.truxpense.data.remote.dto.request.VerifyLoginOtpRequest
import com.example.truxpense.data.remote.dto.response.LoginOtpResponse
import com.example.truxpense.data.remote.dto.response.ResendLoginOtpRequest
import com.example.truxpense.data.remote.dto.response.ResendSignupOtpRequest
import com.example.truxpense.data.remote.dto.response.SignupOtpResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val prefs: AuthPreferences
) {
    suspend fun login(username: String, password: String): Boolean {
        val response = api.login(LoginRequest(username, password))
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
        val response = api.refresh(RefreshRequest(refreshToken))
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

    // OTP flows
    suspend fun sendSignupOtp(email: String): Result<SignupOtpResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = api.sendSignupOtp(mapOf("email" to email))
            if (resp.isSuccessful) {
                resp.body()?.let { Result.success(it) } ?: Result.failure(Exception("Empty body"))
            } else {
                Result.failure(Exception("Server returned ${resp.code()} - ${resp.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifySignupOtp(email: String, otp: String): Result<TokenResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = api.verifySignupOtp(VerifySignupRequest(email, otp))
            if (resp.isSuccessful) {
                resp.body()?.let { body ->
                    body.accessToken?.let { at -> body.refreshToken?.let { rt -> prefs.saveTokens(at, rt, body.expiresIn ?: 0L) } }
                    Result.success(body)
                } ?: Result.failure(Exception("Empty body"))
            } else {
                Result.failure(Exception("Server returned ${resp.code()} - ${resp.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendLoginOtp(email: String): Result<LoginOtpResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = api.sendLoginOtp(mapOf("email" to email))
            if (resp.isSuccessful) {
                resp.body()?.let { Result.success(it) } ?: Result.failure(Exception("Empty body"))
            } else {
                Result.failure(Exception("Server returned ${resp.code()} - ${resp.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyLoginOtp(email: String, otp: String): Result<TokenResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = api.verifyLoginOtp(VerifyLoginOtpRequest(email, otp))
            if (resp.isSuccessful) {
                resp.body()?.let { body ->
                    body.accessToken?.let { at -> body.refreshToken?.let { rt -> prefs.saveTokens(at, rt, body.expiresIn ?: 0L) } }
                    Result.success(body)
                } ?: Result.failure(Exception("Empty body"))
            } else {
                Result.failure(Exception("Server returned ${resp.code()} - ${resp.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendSignupOtp(email: String): Result<ResendSignupOtpRequest> = withContext(Dispatchers.IO) {
        try {
            val resp = api.resendSignupOtp(mapOf("email" to email))
            if (resp.isSuccessful) {
                resp.body()?.let { Result.success(it) } ?: Result.failure(Exception("Empty body"))
            } else {
                Result.failure(Exception("Server returned ${resp.code()} - ${resp.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendLoginOtp(email: String): Result<ResendLoginOtpRequest> = withContext(Dispatchers.IO) {
        try {
            val resp = api.resendLoginOtp(mapOf("email" to email))
            if (resp.isSuccessful) {
                resp.body()?.let { Result.success(it) } ?: Result.failure(Exception("Empty body"))
            } else {
                Result.failure(Exception("Server returned ${resp.code()} - ${resp.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}