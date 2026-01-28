package com.example.truxpense.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(val username: String, val password: String)
data class TokenResponse(val accessToken: String, val refreshToken: String, val expiresIn: Long)
data class RefreshRequest(val refreshToken: String)

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<TokenResponse>
}
