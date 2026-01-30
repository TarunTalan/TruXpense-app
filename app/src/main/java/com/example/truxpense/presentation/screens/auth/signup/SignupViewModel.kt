package com.example.truxpense.presentation.screens.auth.signup

import android.content.Intent
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.AuthRepository
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import com.example.truxpense.presentation.utils.ResponseHandler

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SignUpViewModel"
    }

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    fun onEvent(event: SignupEvent) {
        when (event) {
            is SignupEvent.EmailChanged -> {
                val email = event.email
                val isValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
                _state.value = _state.value.copy(
                    email = email,
                    isEmailValid = isValid,
                    canSignUp = isValid && _state.value.agreeTnc
                )
            }

            is SignupEvent.AgreeTncChanged -> {
                _state.value = _state.value.copy(
                    agreeTnc = event.agreed,
                    canSignUp = event.agreed && _state.value.isEmailValid
                )
            }

            is SignupEvent.ShowTncDialog -> {
                _state.value = _state.value.copy(showTncDialog = event.show)
            }

            SignupEvent.SignUpWithEmail -> {
                sendSignupOtp()
            }

            SignupEvent.SignUpWithGoogle -> {
                // Google sign-in is handled separately via handleGoogleSignInResult
                // This event is just for logging/tracking if needed
                Log.d(TAG, "Google sign-up initiated")
            }

            SignupEvent.ClearError -> {
                _state.value = _state.value.copy(error = null)
            }

            SignupEvent.OnNavigationHandled -> {
                _state.value = _state.value.copy(
                    navigateToOtp = false,
                    navigateToUsername = false
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
                        navigateToUsername = true,
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

    private fun sendSignupOtp() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val emailVal = _state.value.email
            val result = authRepository.sendSignupOtp(emailVal)
            _state.value = _state.value.copy(isLoading = false)

            if (result.isSuccess) {
                _state.value = _state.value.copy(navigateToOtp = true)
            } else {
                val msg = ResponseHandler.getMessageFromResult(result, "Failed to send OTP")
                _state.value = _state.value.copy(error = msg)
            }
        }
    }

    fun verifySignupOtp(otp: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val emailVal = _state.value.email
            val result = authRepository.verifySignupOtp(emailVal, otp)
            _state.value = _state.value.copy(isLoading = false)

            if (result.isSuccess) {
                val tokenResp = result.getOrNull()!!
                _state.value = _state.value.copy(
                    navigateToUsername = true,
                    authToken = tokenResp.accessToken
                )
            } else {
                val msg = ResponseHandler.getMessageFromResult(result, "Failed to verify OTP")
                _state.value = _state.value.copy(error = msg)
            }
        }
    }
}