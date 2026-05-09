package com.example.truxpense.presentation.screens.auth.login

data class LoginUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,

    // Navigation events
    val navigateToOtp: Boolean = false,
    val navigateToHome: Boolean = false,
    val navigateToUsername: Boolean = false,
    val authToken: String? = null,

    // Validation
    val isEmailValid: Boolean = false
)