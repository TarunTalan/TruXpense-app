package com.example.truxpense.data.remote.dto.response

data class User(
    val authProvider: String,
    val email: String,
    val id: Int,
    val profilePicture: Any,
    val role: String,
    val username: Any
)