package com.example.truxpense.data.remote.dto.request

data class VerifyLoginOtpRequest(
    val email: String,
    val otpCode: String
)