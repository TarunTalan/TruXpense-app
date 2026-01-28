package com.example.truxpense.presentation.screens.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AuthButton(
    onClick: () -> Unit,
    content: @Composable (() -> Unit)? = null,
    text: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.background,
            disabledContentColor = MaterialTheme.colorScheme.background.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .height(48.dp)
            .padding(vertical = 0.dp)
            .clip(shape = MaterialTheme.shapes.medium)
            .background(
                if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            ),
        shape = MaterialTheme.shapes.medium,
    ) {
        if (text != null) {
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
        onClick = { }, content = {},
        text = "Get Started",
        enabled = true,
        modifier = Modifier.fillMaxWidth(),
    )
}