package com.example.truxpense.presentation.screens.onboarding.username

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.truxpense.data.prefs.AuthPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class UsernameViewModel @Inject constructor(private val prefs: AuthPreferences): ViewModel() {
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    fun onUsernameChanged(new: String) {
        _username.value = new
    }

    // Save username and mark onboarding complete
    fun saveAndComplete(onComplete: (() -> Unit)? = null) {
        val name = _username.value
        CoroutineScope(Dispatchers.IO).launch {
            prefs.saveUsername(name)
            prefs.setOnboardingComplete(true)
            onComplete?.invoke()
        }
    }
}
