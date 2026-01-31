package com.example.truxpense.presentation.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AuthFlowType { SIGNUP, LOGIN }

@HiltViewModel
class AuthFlowViewModel @Inject constructor(): ViewModel() {
    private val _flow = MutableStateFlow<AuthFlowType?>(null)
    val flow: StateFlow<AuthFlowType?> = _flow

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

}
