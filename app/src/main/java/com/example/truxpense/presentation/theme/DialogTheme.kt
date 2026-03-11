package com.example.truxpense.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Wraps [content] in the **stock Material3 baseline color scheme** (light or dark),
 * completely ignoring TruXpense's custom brand colors.
 *
 * Use this around every AlertDialog, ModalBottomSheet, DatePickerDialog,
 * and any other system-style overlay so they render with M3 defaults
 * rather than the app's teal/amber palette.
 */
@Composable
fun AppDialogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}

