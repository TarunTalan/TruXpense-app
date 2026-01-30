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

    private var resendJob: Job? = null

    fun updateDigit(index: Int, value: String) {
        val copy = _digits.value.toMutableList()
        copy[index] = value
        _digits.value = copy
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

    // Verify OTP against server. isSignup toggles which endpoint to call.
    fun verifyOtp(
        email: String,
        otp: String,
        isSignup: Boolean,
        onSuccess: (TokenResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = if (isSignup) {
                    authRepository.verifySignupOtp(email, otp)
                } else {
                    authRepository.verifyLoginOtp(email, otp)
                }

                if (result.isSuccess) {
                    val token = result.getOrNull()!!
                    onSuccess(token)
                } else {
                    val message = ResponseHandler.getMessageFromResult(result, "Failed to verify OTP")
                    onError(message)
                }
            } catch (t: Throwable) {
                val message = ResponseHandler.parseThrowable(t)
                onError(message)
            }
        }
    }

    // Resend OTP via backend for signup or login
    fun resendOtp(
        email: String,
        isSignup: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = if (isSignup) {
                    authRepository.sendSignupOtp(email)
                } else {
                    authRepository.sendLoginOtp(email)
                }

                if (result.isSuccess) {
                    onSuccess()
                } else {
                    val message = ResponseHandler.getMessageFromResult(result, "Failed to resend OTP")
                    onError(message)
                }
            } catch (t: Throwable) {
                val message = ResponseHandler.parseThrowable(t)
                onError(message)
            }
        }
    }
}
