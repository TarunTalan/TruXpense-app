package com.example.truxpense.data.remote.dto.response

data class RefreshAccessTokenResponse(
    val accessToken: String,
    val expiresIn: Int,
    val refreshToken: String,
    val tokenType: String,
    val user: User
)