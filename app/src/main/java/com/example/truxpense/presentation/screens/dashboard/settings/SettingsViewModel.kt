package com.example.truxpense.presentation.screens.dashboard.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.prefs.AuthPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AuthPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val username: Flow<String?> = prefs.username
    val phone: Flow<String?> = prefs.phone

    private val _smsEnabled = MutableStateFlow(
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    )
    val smsEnabled: StateFlow<Boolean> = _smsEnabled

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled

    fun setSmsEnabled(enabled: Boolean) {
        _smsEnabled.value = enabled
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun savePhone(number: String) {
        viewModelScope.launch { prefs.savePhone(number) }
    }

    fun logout(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            prefs.clear()
            onComplete?.invoke()
        }
    }
}
