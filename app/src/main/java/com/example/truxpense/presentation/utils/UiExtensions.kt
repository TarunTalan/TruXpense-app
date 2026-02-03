package com.example.truxpense.presentation.utils

// UI extensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter

// Block all pointer events while enabled
fun Modifier.blockTouchesWhen(enabled: Boolean): Modifier = if (!enabled) this else this
    .pointerInput(enabled) { /* no-op - keep pointerInput to restart when enabled changes */ }
    .pointerInteropFilter { true }
