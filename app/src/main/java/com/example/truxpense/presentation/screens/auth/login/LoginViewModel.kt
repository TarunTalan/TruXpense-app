package com.example.truxpense.presentation.screens.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.prefs.AuthPreferences
import com.example.truxpense.data.repository.AuthRepository
import com.example.truxpense.presentation.utils.InputValidators
import com.example.truxpense.presentation.utils.ResponseHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefs: AuthPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _otpLockSecondsRemaining = MutableStateFlow(0)
    val otpLockSecondsRemaining: StateFlow<Int> = _otpLockSecondsRemaining.asStateFlow()

    private var lockWatcherJob: Job? = null

    // Watches lockout for a specific email
    private fun startEmailLockWatcher(email: String) {
        lockWatcherJob?.cancel()
        if (email.isBlank()) return

        lockWatcherJob = viewModelScope.launch {
            prefs.otpLockUntilFor(email).collect { lockUntil ->
                updateLockCountdown(lockUntil, email)
            }
        }
    }

    // Public helper so UI can start watching per-email lock when needed
    fun watchLockFor(email: String) {
        startEmailLockWatcher(email)
    }

    // Updates the countdown timer for lockout
    private suspend fun updateLockCountdown(lockUntil: Long, email: String? = null) {
        val now = System.currentTimeMillis()
        val remaining = max(0L, lockUntil - now)

        if (remaining > 0L) {
            var seconds = (remaining / 1000L).toInt()
            _otpLockSecondsRemaining.value = seconds

            // Countdown loop
            while (seconds > 0) {
                delay(1000)
                seconds--
                _otpLockSecondsRemaining.value = seconds
            }

            // Lock expired - clear it
            clearLockout(email)
        } else {
            _otpLockSecondsRemaining.value = 0
        }
    }

    // Clears the lockout for an email
    private suspend fun clearLockout(email: String?) {
        try {
            if (email != null) {
                withContext(Dispatchers.IO) {
                    prefs.clearOtpLockFor(email)
                    prefs.resetOtpFailCountFor(email)
                }
            }
            _otpLockSecondsRemaining.value = 0
        } catch (_: Exception) {
            // Log error
        }
    }

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> handleEmailChanged(event.email)
            is LoginEvent.AgreeTncChanged -> handleTncChanged(event.agreed)
            is LoginEvent.ShowTncDialog -> handleShowTncDialog(event.show)
            LoginEvent.LoginWithEmail -> handleLoginWithEmail()
            LoginEvent.ClearError -> handleClearError()
            LoginEvent.OnNavigationHandled -> handleNavigationHandled()
            LoginEvent.LoginWithGoogle -> { /* Handled by IntroViewModel */ }
        }
    }

    private fun handleEmailChanged(email: String) {
        val sanitizedEmail = InputValidators.filterEmailInput(email)
        val isValid = InputValidators.isValidEmail(sanitizedEmail)

        // Start watching lockout for this email
        startEmailLockWatcher(sanitizedEmail)

        _state.value = _state.value.copy(
            email = sanitizedEmail,
            isEmailValid = isValid,
            canLogin = isValid && _state.value.agreeTnc,
            error = null
        )
    }

    private fun handleTncChanged(agreed: Boolean) {
        _state.value = _state.value.copy(
            agreeTnc = agreed,
            canLogin = agreed && _state.value.isEmailValid,
            error = if (agreed) null else _state.value.error
        )
    }

    private fun handleShowTncDialog(show: Boolean) {
        _state.value = _state.value.copy(showTncDialog = show)
    }

    private fun handleLoginWithEmail() {
        val email = _state.value.email

        // Check if locked
        if (_otpLockSecondsRemaining.value > 0) {
            _state.value = _state.value.copy(
                error = "Too many attempts. Try again in ${formatLockTime(_otpLockSecondsRemaining.value)}"
            )
            return
        }

        // Validate email
        if (!InputValidators.isValidEmail(email)) {
            _state.value = _state.value.copy(
                isEmailValid = false,
                canLogin = false,
                error = "Please enter a valid email address"
            )
            return
        }

        // Check T&C acceptance
        if (!_state.value.agreeTnc) {
            _state.value = _state.value.copy(
                canLogin = false,
                error = "Please accept the Terms and Conditions to continue"
            )
            return
        }

        sendLoginOtp()
    }

    private fun handleClearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun handleNavigationHandled() {
        _state.value = _state.value.copy(
            navigateToOtp = false,
            navigateToHome = false,
            navigateToUsername = false
        )
    }

    private fun sendLoginOtp() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = authRepository.sendLoginOtp(_state.value.email)

            _state.value = _state.value.copy(isLoading = false)

            if (result.isSuccess) {
                _state.value = _state.value.copy(navigateToOtp = true)
            } else {
                val errorMessage = ResponseHandler.getMessageFromResult(
                    result,
                    "Failed to send OTP"
                )
                _state.value = _state.value.copy(error = errorMessage)
            }
        }
    }

    private fun formatLockTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return if (minutes > 0) {
            "$minutes:${secs.toString().padStart(2, '0')}"
        } else {
            "$secs seconds"
        }
    }

    override fun onCleared() {
        super.onCleared()
        lockWatcherJob?.cancel()
    }
}