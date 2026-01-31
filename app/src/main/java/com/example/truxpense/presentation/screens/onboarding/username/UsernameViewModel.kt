package com.example.truxpense.presentation.screens.onboarding.username

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.truxpense.data.prefs.AuthPreferences
import com.example.truxpense.presentation.utils.InputValidators
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.truxpense.data.repository.OnboardingRepository
import kotlinx.coroutines.withContext

@HiltViewModel
class UsernameViewModel @Inject constructor(private val prefs: AuthPreferences, private val repo: OnboardingRepository): ViewModel() {
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
        // Use viewModelScope so lifecycle is respected and switch contexts as needed
        viewModelScope.launch {
            // update UI state on main
            _isSaving.value = true
            try {
                // perform network + IO on background thread
                val res = withContext(Dispatchers.IO) { repo.setUsername(name) }

                // Handle result on main thread (UI-safe). Persisting onboarding flags uses IO.
                res.fold(onSuccess = {
                    try {
                        // persist onboarding flags off the main thread
                        withContext(Dispatchers.IO) {
                            prefs.setOnboardingComplete(true)
                            prefs.setSignupStarted(false)
                        }
                        // Ensure navigation/UI callbacks run on the main thread
                        withContext(Dispatchers.Main) {
                            onComplete?.invoke()
                        }
                    } catch (e: Throwable) {
                        _error.value = e.message ?: "Failed to save username. Please try again."
                    }
                }, onFailure = { err ->
                    _error.value = err.message ?: "Failed to save username. Please try again."
                })
            } catch (t: Throwable) {
                // expose a friendly error to UI
                _error.value = t.message ?: "Failed to save username. Please try again."
            } finally {
                _isSaving.value = false
            }
        }
    }
}
