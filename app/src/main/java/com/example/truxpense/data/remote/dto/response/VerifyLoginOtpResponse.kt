package com.example.truxpense.data.remote.dto.response

data class VerifyLoginOtpResponse(
    val accessToken: String,
    val expiresIn: Int,
    val newUser: Boolean,
    val refreshToken: String,
    val tokenType: String,
    val user: LoggedInUser
)