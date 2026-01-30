package com.example.truxpense.data.remote.dto.request

data class VerifySingupOtpRequest(
    val email: String,
    val otpCode: String
)