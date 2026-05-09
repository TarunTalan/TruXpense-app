package com.example.truxpense.presentation.screens.onboarding.username

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.truxpense.data.local.datastore.AuthPreferences
import com.example.truxpense.presentation.utils.InputValidators
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.truxpense.data.repository.onboarding.OnboardingRepository
import kotlinx.coroutines.withContext
import com.example.truxpense.presentation.utils.ResponseHandler
import kotlinx.coroutines.flow.first

@HiltViewModel
class UsernameViewModel @Inject constructor(private val prefs: AuthPreferences, private val repo: OnboardingRepository): ViewModel() {
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        // Load any persisted username (e.g., returned by OAuth) and prefill the field
        viewModelScope.launch {
            try {
                val saved = prefs.username.first()
                if (!saved.isNullOrBlank()) {
                    _username.value = saved
                }
            } catch (_: Exception) {
                // ignore failures to read prefs; user will type username manually
            }
        }
    }

    fun onUsernameChanged(new: String) {
        // Filter username and set immediate validation error (if any)
        val filtered = InputValidators.filterUsernameInput(new)
        _username.value = filtered
        _error.value = null
    }

    // Save username and mark onboarding complete
    fun saveAndComplete(onComplete: (() -> Unit)? = null) {
        val name = _username.value
        // Validate before saving
        val validationError = InputValidators.usernameError(name)
        if (validationError != null) {
            _error.value = validationError
            return
        }
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
                            // Save that user completed username step and is moving to currency
                            prefs.saveOnboardingStep("currency")
                            prefs.setSignupStarted(false)
                            // Persist trimmed username in preferences
                            val trimmed = name.trim()
                            prefs.saveUsername(trimmed)
                            // Update in-memory username to the trimmed value on main after IO
                            _username.value = trimmed
                        }
                        // Ensure navigation/UI callbacks run on the main thread
                        withContext(Dispatchers.Main) {
                            onComplete?.invoke()
                        }
                    } catch (e: Throwable) {
                        _error.value = ResponseHandler.parseThrowable(e)
                    }
                }, onFailure = { _ ->
                    // Use ResponseHandler to normalize repository errors
                    val message = ResponseHandler.getMessageFromResult(res, "Failed to save username. Please try again.")
                    _error.value = message
                })
            } catch (t: Throwable) {
                // expose a friendly error to UI
                _error.value = ResponseHandler.parseThrowable(t)
            } finally {
                _isSaving.value = false
            }
        }
    }
}
