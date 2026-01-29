package com.example.truxpense.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/google")
    suspend fun loginWithGoogle(@Body body: Map<String, String>): Response<TokenResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<TokenResponse>
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
