package com.example.truxpense.presentation.screens.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator

@Composable
fun AuthButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit)? = null,
    text: String? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingDelayMs: Long = 300L,
) {
    // Only show the spinner if loading has lasted longer than loadingDelayMs
    var showSpinner by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) {
        if (isLoading) {
            showSpinner = false
            try {
                kotlinx.coroutines.delay(loadingDelayMs)
                showSpinner = true
            } catch (_: Exception) {
                showSpinner = true
            }
        } else {
            showSpinner = false
        }
    }

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.background,
            disabledContentColor = MaterialTheme.colorScheme.background.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(vertical = 0.dp)
            .clip(shape = MaterialTheme.shapes.medium)
            .background(
                if (enabled && !isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            ),
        shape = MaterialTheme.shapes.medium,
    ) {
        val displaySpinner = isLoading && showSpinner
        if (displaySpinner) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.background,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
        } else if (text != null) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.background,
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            content?.invoke()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuthButtonPreview() {
    AuthButton(
        onClick = { },
        modifier = Modifier,
        content = {},
        text = "Get Started",
        enabled = true,
    )
}