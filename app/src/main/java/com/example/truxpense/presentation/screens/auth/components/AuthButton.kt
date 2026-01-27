package com.example.truxpense.presentation.screens.auth.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AuthButton(
    onClick: () -> Unit,
    content: @Composable (() -> Unit)? = null,
    bgColor: Color,
    disabledBgColor: Color = Color(255, 255, 255, 1),
    textColor: Color,
    text: String? = null,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            disabledContainerColor = disabledBgColor,
            contentColor = textColor
        ),
        modifier = Modifier.fillMaxWidth().height(40.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        if (text != null) {
            Text(text = text, color = textColor)
        } else {
            content?.invoke()
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = Devices.PIXEL_4)
@Composable
fun AuthButtonPreview() {
    AuthButton(
        onClick = { }, content = {},
        bgColor = Color(47, 164, 169, 1),
        textColor = Color.Black,
        text = "Get Started",
    )
}