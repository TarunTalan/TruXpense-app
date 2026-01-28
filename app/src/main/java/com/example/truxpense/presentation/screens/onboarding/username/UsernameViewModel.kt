package com.example.truxpense.presentation.screens.onboarding.username

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class UsernameViewModel @Inject constructor(): ViewModel() {
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    fun onUsernameChanged(new: String) {
        _username.value = new
    }
}
