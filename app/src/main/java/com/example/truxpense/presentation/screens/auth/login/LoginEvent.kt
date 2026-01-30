package com.example.truxpense.presentation.screens.auth.login

sealed class LoginEvent {
    data class EmailChanged(val email: String) : LoginEvent()
    data class AgreeTncChanged(val agreed: Boolean) : LoginEvent()
    data class ShowTncDialog(val show: Boolean) : LoginEvent()
    object LoginWithEmail : LoginEvent()
    object LoginWithGoogle : LoginEvent()
    object ClearError : LoginEvent()
    object OnNavigationHandled : LoginEvent()
}