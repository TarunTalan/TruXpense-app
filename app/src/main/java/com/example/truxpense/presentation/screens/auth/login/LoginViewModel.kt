package com.example.truxpense.presentation.screens.auth.login

import android.content.Intent
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.AuthRepository
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import com.example.truxpense.presentation.utils.ResponseHandler

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    // one-shot flow used by the NavHost/Activity to launch external sign-in intent
    private val _signInRequest = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val signInRequest = _signInRequest.asSharedFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> {
                // sanitize input: remove spaces, emoji and disallowed characters and cap length
                val email = com.example.truxpense.presentation.utils.InputValidators.filterEmailInput(event.email)
                val isValid = com.example.truxpense.presentation.utils.InputValidators.isValidEmail(email)
                // Reset error when user edits the email
                _state.value = _state.value.copy(
                    email = email,
                    isEmailValid = isValid,
                    canLogin = isValid && _state.value.agreeTnc,
                    error = null
                )
            }

            is LoginEvent.AgreeTncChanged -> {
                _state.value = _state.value.copy(
                    agreeTnc = event.agreed,
                    canLogin = event.agreed && _state.value.isEmailValid,
                    // clear TnC-related error when user accepts
                    error = if (event.agreed) null else _state.value.error
                )
            }

            is LoginEvent.ShowTncDialog -> {
                _state.value = _state.value.copy(showTncDialog = event.show)
            }

            LoginEvent.LoginWithEmail -> {
                // Validate email before sending OTP. If invalid, show inline error
                val isValidNow = com.example.truxpense.presentation.utils.InputValidators.isValidEmail(_state.value.email)
                if (!isValidNow) {
                    _state.value = _state.value.copy(
                        isEmailValid = false,
                        canLogin = false,
                        error = "Please enter a valid email address"
                    )
                } else if (!_state.value.agreeTnc) {
                    // Show a TnC acceptance error if user hasn't agreed
                    _state.value = _state.value.copy(
                        canLogin = false,
                        error = "Please accept the Terms and Conditions to continue"
                    )
                } else {
                    // clear any previous error and proceed
                    _state.value = _state.value.copy(error = null)
                    sendLoginOtp()
                }
            }

            LoginEvent.LoginWithGoogle -> {
                // Google sign-in is handled separately via handleGoogleSignInResult
                Log.d(TAG, "Google login initiated")
            }

            LoginEvent.ClearError -> {
                _state.value = _state.value.copy(error = null)
            }

            LoginEvent.OnNavigationHandled -> {
                _state.value = _state.value.copy(
                    navigateToOtp = false,
                    navigateToHome = false
                )
            }
        }
    }

    fun handleGoogleSignInResult(intent: Intent?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            Log.d(TAG, "handleGoogleSignInResult: intent=$intent")

            try {
                val task = com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getSignedInAccountFromIntent(intent)

                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Google account obtained: id=${account?.id}, email=${account?.email}")
                val idToken = account?.idToken

                if (idToken.isNullOrEmpty()) {
                    val error = "Failed to get ID token. Please check your Google Sign-In configuration."
                    Log.w(TAG, "ID token empty or null")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error
                    )
                    return@launch
                }

                // Send ID token to backend
                val result = authRepository.sendIdTokenToServer(idToken)

                _state.value = _state.value.copy(isLoading = false)
                Log.d(TAG, "sendIdTokenToServer completed: success=${result.isSuccess}")

                if (result.isSuccess) {
                    val tokenResponse = result.getOrNull()!!
                    _state.value = _state.value.copy(
                        navigateToHome = true,
                        authToken = tokenResponse.accessToken
                    )
                } else {
                    val message = ResponseHandler.getMessageFromResult(result, "Server error occurred")
                    _state.value = _state.value.copy(error = message)
                }

            } catch (e: ApiException) {
                Log.w(TAG, "Google Sign-In ApiException (status=${e.statusCode}): ${e.localizedMessage}")
                val errorMessage = when (e.statusCode) {
                    com.google.android.gms.common.api.CommonStatusCodes.CANCELED -> null // Don't show error for user cancellation
                    com.google.android.gms.common.api.CommonStatusCodes.NETWORK_ERROR -> "Network error. Please check your connection."
                    com.google.android.gms.common.api.CommonStatusCodes.INVALID_ACCOUNT -> "Invalid Google account"
                    else -> "Google Sign-In failed: ${e.localizedMessage}"
                }

                _state.value = _state.value.copy(
                    isLoading = false,
                    error = errorMessage
                )

            } catch (e: Exception) {
                Log.e(TAG, "Unexpected exception during Google sign-in: ${e.localizedMessage}", e)
                val errorMessage = ResponseHandler.parseThrowable(e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }

    private fun sendLoginOtp() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val emailVal = _state.value.email
            val result = authRepository.sendLoginOtp(emailVal)
            _state.value = _state.value.copy(isLoading = false)

            if (result.isSuccess) {
                _state.value = _state.value.copy(navigateToOtp = true)
            } else {
                val msg = ResponseHandler.getMessageFromResult(result, "Failed to send OTP")
                _state.value = _state.value.copy(error = msg)
            }
        }
    }

    fun verifyLoginOtp(otp: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val emailVal = _state.value.email
            val result = authRepository.verifyLoginOtp(emailVal, otp)
            _state.value = _state.value.copy(isLoading = false)

            if (result.isSuccess) {
                val tokenResp = result.getOrNull()!!
                _state.value = _state.value.copy(
                    navigateToHome = true,
                    authToken = tokenResp.accessToken
                )
            } else {
                val msg = ResponseHandler.getMessageFromResult(result, "Failed to verify OTP")
                _state.value = _state.value.copy(error = msg)
            }
        }
    }

    // Clear transient navigation flags (NavHost should call this after handling navigation)
    fun clearTransientFlags() {
        _state.value = _state.value.copy(navigateToOtp = false, navigateToHome = false, authToken = null)
    }

    // Emit a sign-in request (UI calls this to request the NavHost to launch Google Sign-In)
    fun requestGoogleSignIn() {
        _signInRequest.tryEmit(Unit)
    }
}