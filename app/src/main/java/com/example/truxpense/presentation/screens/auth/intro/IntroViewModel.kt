package com.example.truxpense.presentation.screens.auth.intro

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class IntroViewModel @Inject constructor(): ViewModel() {
    private val _hasSeenIntro = MutableStateFlow(false)
    val hasSeenIntro: StateFlow<Boolean> = _hasSeenIntro

    fun markSeen() { _hasSeenIntro.value = true }
}
