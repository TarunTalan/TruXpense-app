package com.example.truxpense.presentation.screens.auth.login

sealed class LoginEvent {
    data class EmailChanged(val email: String) : LoginEvent()
    object LoginWithEmail : LoginEvent()
    object LoginWithGoogle : LoginEvent()
    object ClearError : LoginEvent()
    object OnNavigationHandled : LoginEvent()
}