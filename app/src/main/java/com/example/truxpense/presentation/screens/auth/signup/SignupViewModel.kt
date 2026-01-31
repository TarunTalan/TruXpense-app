package com.example.truxpense.presentation.screens.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.AuthRepository
import com.example.truxpense.presentation.utils.InputValidators
import com.example.truxpense.presentation.utils.InputValidators.filterEmailInput
import com.example.truxpense.presentation.utils.ResponseHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

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
                // Google sign-in handled centrally by IntroViewModel; ignore here
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