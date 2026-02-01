package com.example.truxpense.data.remote.api

// Onboarding API definitions

import com.example.truxpense.data.remote.dto.request.UsernameRequest
import com.example.truxpense.data.remote.dto.response.UsernameResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PUT

/**
 * API surface for authenticated user operations.
 * Uses the full API Retrofit (with AuthInterceptor) so requests include Authorization header.
 */
interface OnboardingApi {
    @PUT("api/auth/username")
    suspend fun username(@Body request: UsernameRequest): Response<UsernameResponse>
}
