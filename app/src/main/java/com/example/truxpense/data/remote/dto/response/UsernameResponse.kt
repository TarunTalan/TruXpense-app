package com.example.truxpense.data.remote.dto.response

data class UsernameResponse(
    val authProvider: String,
    val createdAt: String,
    val email: String,
    val enabled: Boolean,
    val id: Int,
    val phoneNumber: String?,
    val profilePicture: String?,
    val providerId: String?,
    val role: String,
    val updatedAt: String,
    val username: String
)