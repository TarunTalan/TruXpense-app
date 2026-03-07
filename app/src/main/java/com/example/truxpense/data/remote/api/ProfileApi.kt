package com.example.truxpense.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT

/**
 * Authenticated profile-management endpoints.
 * Uses ApiRetrofit (with AuthInterceptor) so every request carries the Authorization header.
 */
interface ProfileApi {

    /**
     * Send OTP to the new email address the user wants to switch to.
     * POST /api/auth/signup/send-otp  { "email": "new@example.com" }
     */
    @POST("api/auth/signup/send-otp")
    suspend fun sendEmailChangeOtp(@Body request: EmailOtpRequest): Response<EmailOtpResponse>

    /**
     * Update the authenticated user's email after OTP is verified externally.
     * PUT /api/auth/email  { "email": "new@example.com" }
     */
    @PUT("api/auth/email")
    suspend fun updateEmail(@Body request: UpdateEmailRequest): Response<UpdateEmailResponse>

    /**
     * Update the authenticated user's display name.
     * PUT /api/auth/username  { "username": "Tarun Talan" }
     */
    @PUT("api/auth/username")
    suspend fun updateUsername(@Body request: UpdateUsernameRequest): Response<UpdateUsernameResponse>
}

// ── DTOs ──────────────────────────────────────────────────────────────────────

data class EmailOtpRequest(
    val email: String,
)

data class EmailOtpResponse(
    val message: String,
)

/** PUT /api/auth/email — backend identifies user from JWT, verifies OTP, then updates email */
data class UpdateEmailRequest(
    val email: String,
    val otpCode: String,
)

data class UpdateEmailResponse(
    val message: String,
    val email: String? = null,
)

/** PUT /api/auth/username */
data class UpdateUsernameRequest(
    val username: String,
)

data class UpdateUsernameResponse(
    val message: String? = null,
    val username: String? = null,
)
