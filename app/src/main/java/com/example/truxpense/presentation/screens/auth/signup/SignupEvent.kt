package com.example.truxpense.presentation.screens.auth.signup

sealed class SignupEvent {
    data class EmailChanged(val email: String) : SignupEvent()
    data class AgreeTncChanged(val agreed: Boolean) : SignupEvent()
    data class ShowTncDialog(val show: Boolean) : SignupEvent()
    object SignUpWithEmail : SignupEvent()
    object SignUpWithGoogle : SignupEvent()
    object ClearError : SignupEvent()
    object OnNavigationHandled : SignupEvent()
}