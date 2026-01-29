package com.example.truxpense.presentation.screens.auth.signup

import android.content.Intent
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.remote.api.TokenResponse
import com.example.truxpense.data.repository.AuthRepository
import kotlinx.coroutines.launch

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _agreeTnc = MutableStateFlow(false)
    val agreeTnc: StateFlow<Boolean> = _agreeTnc

    fun onEmailChanged(new: String) { _email.value = new }
    fun onAgreeChanged(value: Boolean) { _agreeTnc.value = value }

    fun onAgreeTncChanged(value: Boolean) = onAgreeChanged(value)

    fun handleGoogleSignInResult(intent: Intent?, onSuccess: (TokenResponse) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(intent)
                val account = task.getResult(Exception::class.java)
                val idToken = account?.idToken
                if (!idToken.isNullOrEmpty()) {
                    val result = authRepository.sendIdTokenToServer(idToken)
                    if (result.isSuccess) {
                        onSuccess(result.getOrNull()!!)
                    } else {
                        onError(result.exceptionOrNull()?.localizedMessage ?: "Server error")
                    }
                } else {
                    onError("ID token is null — check default_web_client_id and SHA-1 registration")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Sign-in failed")
            }
        }
    }
}
