package com.example.truxpense.data.remote.dto.response

data class VerifyLoginOtpResponse(
    val accessToken: String,
    val expiresIn: Int,
    val refreshToken: String,
    val tokenType: String,
    val user: User
)