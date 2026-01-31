package com.example.truxpense.presentation.screens.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.AuthRepository
import com.example.truxpense.presentation.utils.InputValidators
import com.example.truxpense.presentation.utils.ResponseHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> {
                // sanitize input: remove spaces, emoji and disallowed characters and cap length
                val email = InputValidators.filterEmailInput(event.email)
                val isValid = InputValidators.isValidEmail(email)
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
                val isValidNow = InputValidators.isValidEmail(_state.value.email)
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
                // Google sign-in now handled by IntroViewModel; ignore here
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

}