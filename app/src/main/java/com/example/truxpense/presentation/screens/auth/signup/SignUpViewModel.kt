package com.example.truxpense.presentation.screens.auth.signup

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SignUpViewModel @Inject constructor(): ViewModel() {
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _agreeTnc = MutableStateFlow(false)
    val agreeTnc: StateFlow<Boolean> = _agreeTnc

    fun onEmailChanged(new: String) { _email.value = new }
    fun onAgreeChanged(value: Boolean) { _agreeTnc.value = value }

    fun onAgreeTncChanged(value: Boolean) = onAgreeChanged(value)
}
