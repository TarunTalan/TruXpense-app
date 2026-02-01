package com.example.truxpense.presentation.screens.auth.intro

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.prefs.AuthPreferences
import com.example.truxpense.data.repository.AuthRepository
import com.example.truxpense.presentation.utils.ResponseHandler
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntroViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefs: AuthPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(IntroUiState())
    val state: StateFlow<IntroUiState> = _state.asStateFlow()

    // Start One Tap sign-in by building a BeginSignInRequest and storing the IntentSender in state
    fun startOneTapSignIn(context: Context) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val webClientId = try { context.getString(com.example.truxpense.R.string.default_web_client_id) } catch (_: Exception) { "" }

                val signInRequestBuilder = BeginSignInRequest.builder()
                    .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                            .setSupported(true)
                            .setServerClientId(webClientId)
                            .setFilterByAuthorizedAccounts(false)
                            .build()
                    )
                    .setAutoSelectEnabled(false)

                val signInRequest = signInRequestBuilder.build()
                val oneTapClient: SignInClient = Identity.getSignInClient(context)

                oneTapClient.beginSignIn(signInRequest)
                    .addOnSuccessListener { result ->
                        // Keep isLoading = true until the IntentSender is actually launched from the UI.
                        _state.value = _state.value.copy(signInIntentSender = result.pendingIntent.intentSender)
                    }
                    .addOnFailureListener { e ->
                        // No saved credentials or OneTap not available; surface error to UI
                        _state.value = _state.value.copy(isLoading = false, error = ResponseHandler.parseThrowable(e))
                    }

            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = ResponseHandler.parseThrowable(e))
            }
        }
    }

    // Clear the pending IntentSender after UI launches it
    fun clearSignInIntentSender() {
        _state.value = _state.value.copy(signInIntentSender = null)
    }

    // Clear any error shown in the UI (used after showing a toast)
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    // Handle result Intent from One Tap; extract idToken and send to backend
    fun handleOneTapResult(context: Context, intent: Intent?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val oneTapClient = Identity.getSignInClient(context)
                val credential: SignInCredential = oneTapClient.getSignInCredentialFromIntent(intent)
                val idToken = credential.googleIdToken

                if (idToken.isNullOrEmpty()) {
                    _state.value = _state.value.copy(isLoading = false, error = "Failed to obtain idToken from Google")
                    return@launch
                }

                val result = authRepository.sendIdTokenToServer(idToken)
                _state.value = _state.value.copy(isLoading = false)

                if (result.isSuccess) {
                    val resp = result.getOrNull()!!
                    // wait for token to be persisted
                    try {
                        if (!resp.accessToken.isNullOrBlank()) {
                            prefs.accessToken.first { it == resp.accessToken }
                        } else {
                            prefs.accessToken.first { !it.isNullOrBlank() }
                        }
                    } catch (_: Exception) {}

                    // Route based on newUser flag returned by backend
                    if (resp.newUser) {
                        // New user -> go to onboarding/username flow. If server already returned a username,
                        // persist it so onboarding can prefill the username field.
                        try {
                            val uname = resp.user?.username
                            if (!uname.isNullOrBlank()) {
                                prefs.saveUsername(uname)
                            }
                        } catch (_: Exception) { }

                        prefs.setSignupStarted(true)
                        _state.value = _state.value.copy(navigateToUsername = true, authToken = resp.accessToken)

                    } else {
                        // Existing user -> navigate directly to home
                        try {
                            val uname = resp.user?.username
                            if (!uname.isNullOrBlank()) {
                                prefs.saveUsername(uname)
                            }
                        } catch (_: Exception) { }

                        _state.value = _state.value.copy(navigateToHome = true, authToken = resp.accessToken)
                    }

                } else {
                    val msg = ResponseHandler.getMessageFromResult(result, "Server error")
                    _state.value = _state.value.copy(error = msg)
                }

            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = ResponseHandler.parseThrowable(e))
            }
        }
    }

}

// Simple UI state for intro/google flow
data class IntroUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToHome: Boolean = false,
    val navigateToUsername: Boolean = false,
    val authToken: String? = null,
    val signInIntentSender: IntentSender? = null
)
