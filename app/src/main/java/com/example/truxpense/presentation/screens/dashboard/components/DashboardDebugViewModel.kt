package com.example.truxpense.presentation.screens.dashboard.components

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DashboardDebugViewModel @Inject constructor() : ViewModel() {
    // Debug percent to display (e.g., 20 means "20% used"). Set to null to disable override.
    private val _debugPercent = MutableStateFlow<Int?>(20)
    val debugPercent: StateFlow<Int?> = _debugPercent

    // Debug progress value used for the progress bar (0.0 - 1.0). Set to null to disable override.
    private val _debugProgressValue = MutableStateFlow<Float?>(0.2f)
    val debugProgressValue: StateFlow<Float?> = _debugProgressValue

    // Optionally mutate the debug values from code/tests if needed (use StateFlow directly)
}
