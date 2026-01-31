package com.example.truxpense.data.remote.dto.response

data class LoggedInUser(
    val authProvider: String,
     val country: String?,
    val countryAutoSet: Boolean,
    val createdAt: String,
    val currency: String?,
    val currencyAutoSet: Boolean,
    val email: String,
    val enabled: Boolean,
    val id: Int,
    val phoneNumber: String?,
    val profilePicture: String?,
    val providerId: String?,
    val role: String,
    val updatedAt: String,
    val username: String?
)