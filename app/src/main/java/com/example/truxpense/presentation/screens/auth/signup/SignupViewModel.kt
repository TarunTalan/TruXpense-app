package com.example.truxpense.presentation.screens.auth.signup

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.prefs.AuthPreferences
import com.example.truxpense.data.repository.AuthRepository
import com.example.truxpense.presentation.utils.InputValidators
import com.example.truxpense.presentation.utils.InputValidators.filterEmailInput
import com.example.truxpense.presentation.utils.ResponseHandler
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefs: AuthPreferences
) : ViewModel() {

    companion object {
        private const val TAG = "SignUpViewModel"
    }

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    fun onEvent(event: SignupEvent) {
        when (event) {
            is SignupEvent.EmailChanged -> {
                val email = filterEmailInput(event.email)
                val isValid = InputValidators.isValidEmail(email)
                // Reset error when user edits the email
                _state.value = _state.value.copy(
                    email = email,
                    isEmailValid = isValid,
                    canSignUp = isValid && _state.value.agreeTnc,
                    error = null
                )
            }

            is SignupEvent.AgreeTncChanged -> {
                _state.value = _state.value.copy(
                    agreeTnc = event.agreed,
                    canSignUp = event.agreed && _state.value.isEmailValid,
                    // clear TnC-related error when user accepts
                    error = if (event.agreed) null else _state.value.error
                )
            }

            is SignupEvent.ShowTncDialog -> {
                _state.value = _state.value.copy(showTncDialog = event.show)
            }

            SignupEvent.SignUpWithEmail -> {
                // Validate email before sending OTP. If invalid, show inline error
                val isValidNow = InputValidators.isValidEmail(_state.value.email)
                if (!isValidNow) {
                    _state.value = _state.value.copy(
                        isEmailValid = false,
                        canSignUp = false,
                        error = "Please enter a valid email address"
                    )
                } else if (!_state.value.agreeTnc) {
                    _state.value = _state.value.copy(
                        canSignUp = false,
                        error = "Please accept the Terms and Conditions to continue"
                    )
                } else {
                    _state.value = _state.value.copy(error = null)
                    sendSignupOtp()
                }
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
                    // Persist that signup reached username step so app restart resumes here
                    viewModelScope.launch {
                        prefs.setSignupStarted(true)
                    }
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

}