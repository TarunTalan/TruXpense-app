package com.example.truxpense.presentation.screens.auth.otp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.prefs.AuthPreferences
import com.example.truxpense.data.otp.OtpLockManager
import com.example.truxpense.data.remote.api.TokenResponse
import com.example.truxpense.data.repository.AuthRepository
import com.example.truxpense.presentation.navigation.AuthFlowType
import com.example.truxpense.presentation.utils.ResponseHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefs: AuthPreferences,
    private val lockManager: OtpLockManager
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

    private val _otpLockSecondsRemaining = MutableStateFlow(0)
    val otpLockSecondsRemaining: StateFlow<Int> = _otpLockSecondsRemaining

    private var resendJob: Job? = null
    private var lockCountdownJob: Job? = null
    // Track last emitted onError message to avoid duplicate UI messages
    private var lastEmittedOnError: String? = null
    // Prevent concurrent verify/resend requests that can emit multiple errors
    private val verifying = AtomicBoolean(false)

    companion object {
        private const val RESEND_COOLDOWN_SECONDS = 30
    }

    fun updateDigit(index: Int, value: String) {
        if (index !in 0..5) return

        val updatedDigits = _digits.value.toMutableList()
        updatedDigits[index] = value
        _digits.value = updatedDigits

        // Clear error when user types
        _error.value = null
    }

    /**
     * Starts the resend cooldown timer
     */
    fun startResendTimer(seconds: Int = RESEND_COOLDOWN_SECONDS) {
        resendJob?.cancel()
        _canResend.value = false
        _resendSecondsRemaining.value = seconds

        resendJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _resendSecondsRemaining.value = remaining
            }
            _canResend.value = true
            _resendSecondsRemaining.value = 0
        }
    }

    /**
     * Starts countdown timer for lockout period (reads remaining seconds from lockManager)
     */
    private fun startLockCountdownFromManager(email: String) {
        lockCountdownJob?.cancel()

        lockCountdownJob = viewModelScope.launch {
            while (true) {
                val seconds = lockManager.getRemainingLockSeconds(email)
                _otpLockSecondsRemaining.value = seconds
                if (seconds <= 0) {
                    // ensure manager cleared state
                    lockManager.clearLock(email)
                    break
                }
                delay(1000)
            }
        }
    }

    /**
     * Verifies OTP with the server
     */
    fun verifyOtp(
        email: String,
        otp: String,
        flowType: AuthFlowType?,
        onSuccess: (TokenResponse) -> Unit
    ) {
        val normEmail = email.trim().lowercase()
        viewModelScope.launch {
            // Prevent concurrent verification attempts
            if (!verifying.compareAndSet(false, true)) return@launch

            // Check if locked via manager
            if (lockManager.isLocked(normEmail)) {
                val remaining = lockManager.getRemainingLockSeconds(normEmail)
                _otpLockSecondsRemaining.value = remaining
                val lockMsg = formatLockoutError(remaining)
                _error.value = null
                emitLock(lockMsg)
                verifying.set(false)
                // start countdown UI
                startLockCountdownFromManager(normEmail)
                return@launch
            }

            _isLoading.value = true
            _error.value = null

            try {
                val result = when (flowType) {
                    AuthFlowType.SIGNUP -> authRepository.verifySignupOtp(normEmail, otp)
                    AuthFlowType.LOGIN -> authRepository.verifyLoginOtp(normEmail, otp)
                    null -> {
                        _isLoading.value = false
                        val errorMsg = "Unknown authentication flow"
                        emitError(errorMsg)
                        return@launch
                    }
                }

                if (result.isSuccess) {
                    val token = result.getOrNull()!!

                    // Wait for token persistence
                    waitForTokenPersistence(token)

                    // Reset failure count on success
                    lockManager.resetFailures(normEmail)

                    _isLoading.value = false
                    onSuccess(token)
                } else {
                    _isLoading.value = false

                    // Register failure in manager (handles sliding window & locking)
                    val locked = lockManager.registerFailure(normEmail)

                    if (locked) {
                        val remaining = lockManager.getRemainingLockSeconds(normEmail)
                        val lockMsg = formatLockoutError(remaining)
                        _error.value = null
                        emitLock(lockMsg)
                        startLockCountdownFromManager(normEmail)
                    } else {
                        // No lock; show the server-provided message (if any)
                        val errorMessage = ResponseHandler.getMessageFromResult(
                            result,
                            "Invalid OTP. Please try again."
                        )
                        emitError(errorMessage)
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
                val errorMessage = ResponseHandler.parseThrowable(e)
                emitError(errorMessage)
            } finally {
                verifying.set(false)
            }
        }
    }

    /**
     * Resends OTP to the user's email
     */
    fun resendOtp(
        email: String,
        flowType: AuthFlowType?,
        onSuccess: () -> Unit
    ) {
        val normEmail = email.trim().lowercase()
        viewModelScope.launch {
            // Check if locked
            if (lockManager.isLocked(normEmail)) {
                val remaining = lockManager.getRemainingLockSeconds(normEmail)
                val lockMsg = formatLockoutError(remaining)
                _error.value = null
                emitLock(lockMsg)
                startLockCountdownFromManager(normEmail)
                return@launch
            }

            _isLoading.value = true
            _error.value = null

            try {
                val result = when (flowType) {
                    AuthFlowType.SIGNUP -> authRepository.sendSignupOtp(normEmail)
                    AuthFlowType.LOGIN -> authRepository.sendLoginOtp(normEmail)
                    null -> {
                        _isLoading.value = false
                        val errorMsg = "Unknown authentication flow"
                        emitError(errorMsg)
                        return@launch
                    }
                }

                if (result.isSuccess) {
                    // Successful resend - reset failure count
                    lockManager.resetFailures(normEmail)

                    _isLoading.value = false
                    startResendTimer()
                    onSuccess()
                } else {
                    _isLoading.value = false

                    // Register failure in manager (handles sliding window & locking)
                    val locked = lockManager.registerFailure(normEmail)

                    if (locked) {
                        val remaining = lockManager.getRemainingLockSeconds(normEmail)
                        val lockMsg = formatLockoutError(remaining)
                        _error.value = null
                        emitLock(lockMsg)
                        startLockCountdownFromManager(normEmail)
                    } else {
                        // No lock; show server-provided message (if any)
                        val errorMessage = ResponseHandler.getMessageFromResult(
                            result,
                            "Failed to resend OTP"
                        )
                        emitError(errorMessage)
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
                val errorMessage = ResponseHandler.parseThrowable(e)
                emitError(errorMessage)
            }
        }
    }

    /**
     * Waits for token to be persisted to DataStore
     */
    private suspend fun waitForTokenPersistence(token: TokenResponse) {
        try {
            if (!token.accessToken.isNullOrBlank()) {
                prefs.accessToken.first { it == token.accessToken }
            } else {
                prefs.accessToken.first { !it.isNullOrBlank() }
            }
        } catch (_: Exception) {
            // Best effort - proceed even if wait fails
        }
    }

    /**
     * Formats lockout error message with remaining time
     */
    private fun formatLockoutError(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        val timeString = if (minutes > 0) {
            "$minutes:${secs.toString().padStart(2, '0')}"
        } else {
            "$secs seconds"
        }
        return "Too many invalid attempts. Try again in $timeString"
    }


    // Public helper: start watching/checking for a lock for the given email.
    // This is called by UI when the OTP screen is shown so persisted locks are resumed.
    fun watchLockFor(email: String) {
        val normEmail = email.trim().lowercase()
        if (normEmail.isBlank()) return
        viewModelScope.launch {
            // Use lockManager to check & start countdown
            if (lockManager.isLocked(normEmail)) {
                val sec = lockManager.getRemainingLockSeconds(normEmail)
                _otpLockSecondsRemaining.value = sec
                startLockCountdownFromManager(normEmail)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        resendJob?.cancel()
        lockCountdownJob?.cancel()
    }

    // Helper to set error state and emit onError in a deduped manner
    private fun emitError(message: String) {
        // Only update the internal error state and log. Do NOT call external onError to avoid
        // duplicate UI displays (some callers may also show toasts/snackbars in onError).
        _error.value = message
        Log.d("OtpViewModel", "emitError: message='$message' lastEmitted='${lastEmittedOnError}'")
        if (lastEmittedOnError != message) {
            lastEmittedOnError = message
            Log.d("OtpViewModel", "emitError: recorded lastEmitted='$message' (no onError call)")
        } else {
            Log.d("OtpViewModel", "emitError: suppressed duplicate message='$message'")
        }
    }

    // Emit a lock-related message internally but do NOT call onError: the UI already
    // displays the lock countdown text based on `otpLockSeconds`. Avoid invoking
    // onError to prevent duplicate messages (toast/snackbar + inline).
    private fun emitLock(message: String) {
        if (lastEmittedOnError != message) {
            lastEmittedOnError = message
            // Intentionally do NOT call onError(message) to avoid duplicate displays.
        }
    }
}
