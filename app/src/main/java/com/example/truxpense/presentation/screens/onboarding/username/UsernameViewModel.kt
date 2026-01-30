package com.example.truxpense.presentation.screens.onboarding.username

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.truxpense.data.prefs.AuthPreferences
import com.example.truxpense.presentation.utils.InputValidators
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class UsernameViewModel @Inject constructor(private val prefs: AuthPreferences): ViewModel() {
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun onUsernameChanged(new: String) {
        // Filter username to letters only and cap length
        _username.value = InputValidators.filterUsernameInput(new)
        // clear any previous error when user edits
        _error.value = null
    }

    // Save username and mark onboarding complete
    fun saveAndComplete(onComplete: (() -> Unit)? = null) {
        val name = _username.value
        CoroutineScope(Dispatchers.IO).launch {
            _isSaving.value = true
            try {
                prefs.saveUsername(name)
                // mark onboarding complete and clear signupStarted so app restarts in the correct state
                prefs.setOnboardingComplete(true)
                prefs.setSignupStarted(false)
                onComplete?.invoke()
            } catch (_: Throwable) {
                // expose a friendly error to UI
                _error.value = "Failed to save username. Please try again."
            } finally {
                _isSaving.value = false
            }
        }
    }
}
