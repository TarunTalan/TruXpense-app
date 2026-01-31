package com.example.truxpense.data.repository

import com.example.truxpense.data.prefs.AuthPreferences
import com.example.truxpense.data.remote.api.AuthApi
import com.example.truxpense.data.remote.api.TokenResponse
import com.example.truxpense.data.remote.dto.request.VerifyLoginOtpRequest
import com.example.truxpense.data.remote.dto.request.LoginOtpRequest
import com.example.truxpense.data.remote.dto.request.SignupOtpRequest
import com.example.truxpense.data.remote.dto.response.LoginOtpResponse
import com.example.truxpense.data.remote.dto.response.SignupOtpResponse
import com.example.truxpense.data.remote.dto.response.VerifyLoginOtpResponse
import java.lang.reflect.Field
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val prefs: AuthPreferences
) {
    // Google OAuth: send ID token to backend and return TokenResponse via Result
    // Backend now returns VerifyLoginOtpResponse for OAuth/login verification which includes user info.
    // Return the raw VerifyLoginOtpResponse so callers can inspect user info immediately.
    suspend fun sendIdTokenToServer(idToken: String): Result<VerifyLoginOtpResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val resp = api.loginWithGoogle(mapOf("idToken" to idToken))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body != null) {
                        // persist tokens (fields are non-nullable in VerifyLoginOtpResponse)
                        try {
                            prefs.saveTokens(body.accessToken, body.refreshToken, body.expiresIn.toLong())
                        } catch (_: Exception) {}
                        // persist username if backend returned it (handle multiple possible DTO shapes)
                        try {
                            val usernameRaw: String? = getUsernameFromResponse(body)
                            val uname = usernameRaw?.takeIf { it.isNotBlank() }
                            if (uname != null) prefs.saveUsername(uname)
                        } catch (_: Exception) {}

                        Result.success(body)
                    } else {
                        Result.failure(Exception("Empty body"))
                    }
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
            val resp = api.sendSignupOtp(SignupOtpRequest(email))
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
            val resp = api.verifySignupOtp(com.example.truxpense.data.remote.api.VerifySignupRequest(email, otp))
            if (resp.isSuccessful) {
                val body = resp.body()
                if (body != null) {
                    // body is VerifyLoginOtpResponse (non-nullable fields)
                    // persist tokens
                    try {
                        prefs.saveTokens(body.accessToken, body.refreshToken, body.expiresIn.toLong())
                    } catch (_: Exception) {}
                    // persist username if present (support multiple DTOs)
                    try {
                        val usernameRaw: String? = getUsernameFromResponse(body)
                        val uname = usernameRaw?.takeIf { it.isNotBlank() }
                        if (uname != null) prefs.saveUsername(uname)
                    } catch (_: Exception) {}

                    val token = TokenResponse(body.accessToken, body.refreshToken, body.expiresIn.toLong())
                    Result.success(token)
                } else {
                    Result.failure(Exception("Empty body"))
                }
            } else {
                Result.failure(Exception("Server returned ${resp.code()} - ${resp.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendLoginOtp(email: String): Result<LoginOtpResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = api.sendLoginOtp(LoginOtpRequest(email))
            if (resp.isSuccessful) {
                resp.body()?.let { Result.success(it) } ?: Result.failure(Exception("Empty body"))
            } else {
                Result.failure(Exception("Server returned ${resp.code()} - ${resp.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // verifyLoginOtp now returns VerifyLoginOtpResponse from backend which includes user info
    // We convert it to TokenResponse for existing callers and persist username if present.
    suspend fun verifyLoginOtp(email: String, otp: String): Result<TokenResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = api.verifyLoginOtp(VerifyLoginOtpRequest(email, otp))
            if (resp.isSuccessful) {
                val body = resp.body()
                if (body != null) {
                    val token = TokenResponse(body.accessToken, body.refreshToken, body.expiresIn.toLong())
                    token.accessToken?.let { at -> token.refreshToken?.let { rt -> prefs.saveTokens(at, rt, token.expiresIn ?: 0L) } }
                    try {
                        val usernameRaw: String? = getUsernameFromResponse(body)
                        val uname = usernameRaw?.takeIf { it.isNotBlank() }
                        if (uname != null) prefs.saveUsername(uname)
                    } catch (_: Exception) {}
                    Result.success(token)
                } else {
                    Result.failure(Exception("Empty body"))
                }
            } else {
                Result.failure(Exception("Server returned ${resp.code()} - ${resp.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // resendSignupOtp removed because it's not referenced anywhere in the app.

    // Helper function to extract username from response using reflection
    private fun getUsernameFromResponse(body: VerifyLoginOtpResponse): String? {
        // Attempt to get the "username" field via reflection
        return try {
            val userField: Field = body.javaClass.getDeclaredField("user")
            userField.isAccessible = true
            val user = userField.get(body)

            // Check if the user object has a "username" field
            val usernameField: Field = user?.javaClass?.getDeclaredField("username") ?: return null
            usernameField.isAccessible = true
            usernameField.get(user) as? String
        } catch (_: Exception) {
            null
        }
    }
}