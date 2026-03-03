package com.example.truxpense.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Authentication flow types
enum class AuthFlowType {
    SIGNUP,
    LOGIN
}

// ViewModel for auth flow state
@HiltViewModel
class AuthFlowViewModel @Inject constructor() : ViewModel() {

    private val _flow = MutableStateFlow<AuthFlowType?>(null)
    val flow: StateFlow<AuthFlowType?> = _flow.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _lockRemainingSeconds = MutableStateFlow(0)
    val lockRemainingSeconds: StateFlow<Int> = _lockRemainingSeconds.asStateFlow()

    // Set the current authentication flow type
    fun setFlow(flowType: AuthFlowType) {
        _flow.value = flowType
    }

    // Set the email being used for authentication
    fun setEmail(email: String) {
        _email.value = email
    }

    // Set authentication context (flow + email)
    fun setAuthContext(flowType: AuthFlowType, email: String) {
        viewModelScope.launch {
            _flow.value = flowType
            _email.value = email
        }
    }

    // Update lockout status
    fun setLockStatus(isLocked: Boolean, remainingSeconds: Int = 0) {
        viewModelScope.launch {
            _isLocked.value = isLocked
            _lockRemainingSeconds.value = remainingSeconds
        }
    }

    // Clear all authentication context
    fun clearAuthContext() {
        viewModelScope.launch {
            _flow.value = null
            _email.value = ""
            _isLocked.value = false
            _lockRemainingSeconds.value = 0
        }
    }

    // Check if auth context is complete
    fun isAuthContextComplete(): Boolean {
        return _flow.value != null && _email.value.isNotBlank()
    }
}