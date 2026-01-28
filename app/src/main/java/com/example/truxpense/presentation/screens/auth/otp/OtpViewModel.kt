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

@HiltViewModel
class OtpViewModel @Inject constructor(): ViewModel() {
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
}
