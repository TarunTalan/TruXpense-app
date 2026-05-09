package com.example.truxpense.data.remote.api

// username endpoint moved to UserApi to ensure requests use authenticated client
import com.example.truxpense.data.remote.dto.request.LoginOtpRequest
import com.example.truxpense.data.remote.dto.request.SignupOtpRequest
import com.example.truxpense.data.remote.dto.request.VerifyLoginOtpRequest
import com.example.truxpense.data.remote.dto.response.LoginOtpResponse
import com.example.truxpense.data.remote.dto.response.SignupOtpResponse
import com.example.truxpense.data.remote.dto.response.VerifyLoginOtpResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/google")
    suspend fun loginWithGoogle(@Body body: Map<String, String>): Response<VerifyLoginOtpResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<VerifyLoginOtpResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<TokenResponse>

    // OTP-based signup/login endpoints
    @POST("api/auth/signup/send-otp")
    suspend fun sendSignupOtp(@Body request: SignupOtpRequest): Response<SignupOtpResponse>

    @POST("api/auth/signup/verify")
    suspend fun verifySignupOtp(@Body request: VerifySignupRequest): Response<VerifyLoginOtpResponse>

    @POST("api/auth/login/send-otp")
    suspend fun sendLoginOtp(@Body request: LoginOtpRequest): Response<LoginOtpResponse>

    @POST("api/auth/login/verify")
    suspend fun verifyLoginOtp(@Body request: VerifyLoginOtpRequest): Response<VerifyLoginOtpResponse>

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

data class VerifySignupRequest(
    val email: String,
    val otpCode: String
)
