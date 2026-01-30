package com.example.truxpense.presentation.screens.auth.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.truxpense.data.repository.AuthRepository
import com.example.truxpense.presentation.utils.ResponseHandler
import com.example.truxpense.data.remote.api.TokenResponse
import com.example.truxpense.presentation.navigation.AuthFlowType

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _digits = MutableStateFlow(List(6) { "" })
    val digits: StateFlow<List<String>> = _digits

    private val _canResend = MutableStateFlow(false)
    val canResend: StateFlow<Boolean> = _canResend

    private val _resendSecondsRemaining = MutableStateFlow(0)
    val resendSecondsRemaining: StateFlow<Int> = _resendSecondsRemaining

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var resendJob: Job? = null

    fun updateDigit(index: Int, value: String) {
        val copy = _digits.value.toMutableList()
        copy[index] = value
        _digits.value = copy
        // clear any previous error when user enters input
        _error.value = null
    }

    fun setCanResend(value: Boolean) { _canResend.value = value }

    fun startResendTimer(seconds: Int = 30) {
        // cancel any existing timer
        resendJob?.cancel()
        _canResend.value = false
        _resendSecondsRemaining.value = seconds
        resendJob = viewModelScope.launch {
            var t = seconds
            while (t > 0) {
                delay(1000)
                t--
                _resendSecondsRemaining.value = t
            }
            _canResend.value = true
        }
    }

    // Verify OTP against server. `flowType` determines which endpoint to call.
    fun verifyOtp(
        email: String,
        otp: String,
        flowType: AuthFlowType?,
        onSuccess: (TokenResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = when (flowType) {
                    AuthFlowType.SIGNUP -> authRepository.verifySignupOtp(email, otp)
                    AuthFlowType.LOGIN -> authRepository.verifyLoginOtp(email, otp)
                    else -> {
                        // Unknown flow: return a failure so UI shows a proper message
                        _isLoading.value = false
                        val msg = "Unknown authentication flow"
                        _error.value = msg
                        onError(msg)
                        return@launch
                    }
                }

                if (result.isSuccess) {
                    val token = result.getOrNull()!!
                    _isLoading.value = false
                    onSuccess(token)
                } else {
                    _isLoading.value = false
                    val message = ResponseHandler.getMessageFromResult(result, "Failed to verify OTP")
                    // set internal error state so UI can reflect it in fields
                    _error.value = message
                    onError(message)
                }
            } catch (t: Throwable) {
                _isLoading.value = false
                val message = ResponseHandler.parseThrowable(t)
                _error.value = message
                onError(message)
            }
        }
    }

    // Resend OTP via backend for signup or login
    fun resendOtp(
        email: String,
        flowType: AuthFlowType?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = when (flowType) {
                    AuthFlowType.SIGNUP -> authRepository.sendSignupOtp(email)
                    AuthFlowType.LOGIN -> authRepository.sendLoginOtp(email)
                    else -> {
                        _isLoading.value = false
                        val msg = "Unknown authentication flow"
                        _error.value = msg
                        onError(msg)
                        return@launch
                    }
                }

                if (result.isSuccess) {
                    _isLoading.value = false
                    onSuccess()
                } else {
                    _isLoading.value = false
                    val message = ResponseHandler.getMessageFromResult(result, "Failed to resend OTP")
                    _error.value = message
                    onError(message)
                }
            } catch (t: Throwable) {
                _isLoading.value = false
                val message = ResponseHandler.parseThrowable(t)
                _error.value = message
                onError(message)
            }
        }
    }

    fun clearError() { _error.value = null }
}
