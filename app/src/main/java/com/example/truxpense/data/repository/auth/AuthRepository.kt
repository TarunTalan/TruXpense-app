package com.example.truxpense.data.repository.auth

// Auth repository

import com.example.truxpense.data.local.datastore.AuthPreferences
import com.example.truxpense.data.remote.api.AuthApi
import com.example.truxpense.data.remote.api.TokenResponse
import com.example.truxpense.data.remote.dto.request.VerifyLoginOtpRequest
import com.example.truxpense.data.remote.dto.request.LoginOtpRequest
import com.example.truxpense.data.remote.dto.request.SignupOtpRequest
import com.example.truxpense.data.remote.dto.response.LoginOtpResponse
import com.example.truxpense.data.remote.dto.response.SignupOtpResponse
import com.example.truxpense.data.remote.dto.response.VerifyLoginOtpResponse
import com.example.truxpense.presentation.utils.ResponseHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val prefs: AuthPreferences
) {

    /**
     * Google OAuth: send ID token to backend and return VerifyLoginOtpResponse
     */
    suspend fun sendIdTokenToServer(idToken: String): Result<VerifyLoginOtpResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.loginWithGoogle(mapOf("idToken" to idToken))

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Persist tokens
                        saveTokensAndUsername(body)
                        Result.success(body)
                    } else {
                        Result.failure(Exception("Empty response from server"))
                    }
                } else {
                    val errorMessage = ResponseHandler.parseHttpResponse(
                        response.code(),
                        response.errorBody()?.string()
                    )
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                val errorMessage = ResponseHandler.parseThrowable(e)
                Result.failure(Exception(errorMessage))
            }
        }
    }

    /**
     * Send OTP for signup
     */
    suspend fun sendSignupOtp(email: String): Result<SignupOtpResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.sendSignupOtp(SignupOtpRequest(email))

                if (response.isSuccessful) {
                    response.body()?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("Empty response from server"))
                } else {
                    val errorMessage = ResponseHandler.parseHttpResponse(
                        response.code(),
                        response.errorBody()?.string()
                    )
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                val errorMessage = ResponseHandler.parseThrowable(e)
                Result.failure(Exception(errorMessage))
            }
        }
    }

    /**
     * Verify signup OTP
     */
    suspend fun verifySignupOtp(email: String, otp: String): Result<TokenResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.verifySignupOtp(
                    com.example.truxpense.data.remote.api.VerifySignupRequest(email, otp)
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Persist tokens and username
                        saveTokensAndUsername(body)

                        val token = TokenResponse(
                            body.accessToken,
                            body.refreshToken,
                            body.expiresIn.toLong()
                        )
                        Result.success(token)
                    } else {
                        Result.failure(Exception("Empty response from server"))
                    }
                } else {
                    val errorMessage = ResponseHandler.parseHttpResponse(
                        response.code(),
                        response.errorBody()?.string()
                    )
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                val errorMessage = ResponseHandler.parseThrowable(e)
                Result.failure(Exception(errorMessage))
            }
        }
    }

    /**
     * Send OTP for login
     */
    suspend fun sendLoginOtp(email: String): Result<LoginOtpResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.sendLoginOtp(LoginOtpRequest(email))

                if (response.isSuccessful) {
                    response.body()?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("Empty response from server"))
                } else {
                    val errorMessage = ResponseHandler.parseHttpResponse(
                        response.code(),
                        response.errorBody()?.string()
                    )
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                val errorMessage = ResponseHandler.parseThrowable(e)
                Result.failure(Exception(errorMessage))
            }
        }
    }

    /**
     * Verify login OTP
     */
    suspend fun verifyLoginOtp(email: String, otp: String): Result<TokenResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.verifyLoginOtp(VerifyLoginOtpRequest(email, otp))

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Persist tokens and username
                        val token = TokenResponse(
                            body.accessToken,
                            body.refreshToken,
                            body.expiresIn.toLong()
                        )

                        token.accessToken?.let { at ->
                            token.refreshToken?.let { rt ->
                                prefs.saveTokens(at, rt, token.expiresIn ?: 0L)
                            }
                        }

                        saveUsername(body)
                        Result.success(token)
                    } else {
                        Result.failure(Exception("Empty response from server"))
                    }
                } else {
                    val errorMessage = ResponseHandler.parseHttpResponse(
                        response.code(),
                        response.errorBody()?.string()
                    )
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                val errorMessage = ResponseHandler.parseThrowable(e)
                Result.failure(Exception(errorMessage))
            }
        }
    }

    /**
     * Helper to save tokens and username
     */
    private suspend fun saveTokensAndUsername(body: VerifyLoginOtpResponse) {
        try {
            prefs.saveTokens(
                body.accessToken,
                body.refreshToken,
                body.expiresIn.toLong()
            )
        } catch (_: Exception) {
            // Log error but don't fail the operation
        }

        saveUsername(body)
    }

    /**
     * Helper to extract and save username from response
     */
    private suspend fun saveUsername(body: VerifyLoginOtpResponse) {
        try {
            val user = body.user

            // Username
            user.username?.takeIf { it.isNotBlank() }?.let { prefs.saveUsername(it) }

            // Email — persisted here so PersonalInfo screen shows it immediately after login
            user.email.takeIf { it.isNotBlank() }?.let { prefs.saveEmail(it) }

            // Phone
            user.phoneNumber?.takeIf { it.isNotBlank() }?.let { prefs.savePhone(it) }

        } catch (_: Exception) {
            // Log error but don't fail the operation
        }
    }
}

