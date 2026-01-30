package com.example.truxpense.data.remote.api

import com.example.truxpense.data.remote.dto.request.VerifyLoginOtpRequest
import com.example.truxpense.data.remote.dto.response.LoginOtpResponse
import com.example.truxpense.data.remote.dto.response.ResendLoginOtpRequest
import com.example.truxpense.data.remote.dto.response.ResendSignupOtpRequest
import com.example.truxpense.data.remote.dto.response.SignupOtpResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/google")
    suspend fun loginWithGoogle(@Body body: Map<String, String>): Response<TokenResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<TokenResponse>

    // OTP-based signup/login endpoints
    @POST("api/auth/signup/send-otp")
    suspend fun sendSignupOtp(@Body request: Map<String, String>): Response<SignupOtpResponse>

    @POST("api/auth/signup/verify-otp")
    suspend fun verifySignupOtp(@Body request: VerifySignupRequest): Response<TokenResponse>

    @POST("api/auth/login/send-otp")
    suspend fun sendLoginOtp(@Body request: Map<String, String>): Response<LoginOtpResponse>

    @POST("api/auth/login/verify-otp")
    suspend fun verifyLoginOtp(@Body request: VerifyLoginOtpRequest): Response<TokenResponse>

    @POST("api/auth/signup/resend-otp")
    suspend fun resendSignupOtp(@Body request: Map<String, String>): Response<ResendSignupOtpRequest>

    @POST("api/auth/login/resend-otp")
    suspend fun resendLoginOtp(@Body request: Map<String, String>): Response<ResendLoginOtpRequest>
}

// Unified token response used across endpoints
data class TokenResponse(
    val accessToken: String?,
    val refreshToken: String?,
    val expiresIn: Long? = null
)

// Back-end DTOs for login/refresh
data class LoginRequest(
    val username: String,
    val password: String
)

data class RefreshRequest(
    val refreshToken: String
)

// Add thin request/response wrappers used by API (re-use existing DTOs where available)
// Note: some DTOs are already present in the project under data.remote.dto.request/response

// Local types to avoid import cycles
data class VerifySignupRequest(
    val email: String,
    val otpCode: String
)
