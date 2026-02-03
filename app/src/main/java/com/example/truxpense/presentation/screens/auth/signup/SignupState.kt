package com.example.truxpense.presentation.screens.auth.signup

data class SignUpUiState(
    val email: String = "",
    val agreeTnc: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showTncDialog: Boolean = false,

    // Navigation events
    val navigateToOtp: Boolean = false,
    val navigateToUsername: Boolean = false,
    val authToken: String? = null,

    // Validation
    val isEmailValid: Boolean = false,
    val canSignUp: Boolean = false
)