package com.example.truxpense.presentation.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import com.example.truxpense.data.prefs.AuthPreferences

@HiltViewModel
class SplashViewModel @Inject constructor(private val prefs: AuthPreferences): ViewModel() {
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    // Expose onboarding completion from persisted preferences so navigation can decide next screen
    // AuthPreferences.onboardingComplete is a Flow<Boolean> (DataStore), so expose as Flow here.
    val onboardingComplete: Flow<Boolean> = prefs.onboardingComplete

    // Expose persisted auth state so startup can route correctly
    val accessToken: Flow<String?> = prefs.accessToken
    val username: Flow<String?> = prefs.username
    val signupStarted: Flow<Boolean> = prefs.signupStarted
    val onboardingStep: Flow<String?> = prefs.onboardingStep

    fun markReady() {
        _isReady.value = true
    }

    fun saveOnboardingStep(step: String) {
        viewModelScope.launch {
            prefs.saveOnboardingStep(step)
        }
    }

    fun markOnboardingComplete() {
        viewModelScope.launch {
            prefs.setOnboardingComplete(true)
            prefs.saveOnboardingStep("completed")
        }
    }
}
