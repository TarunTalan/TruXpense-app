package com.example.truxpense.presentation.screens.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.truxpense.R

@Composable
fun AuthTextField(
    modifier: Modifier = Modifier,
    bgColor: Color,
    label: String? = null,
    placeholder: String? = null,
    error: String? = null,
    bottomLabel: String? = null,
    value: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
    onValueChange: (String) -> Unit,
    contentPadding: Int = 0,
    trailing: (@Composable () -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(2.dp))
        }

        // Choose border color: error has priority, otherwise primary when focused, else outline
        val borderColor = when {
            !error.isNullOrEmpty() -> MaterialTheme.colorScheme.error
            isFocused -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
        }

        // Border width: make error and focused states more prominent (2.dp), otherwise 1.dp
        val borderWidth = if (!error.isNullOrEmpty() || isFocused) 2.dp else 1.dp

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(bgColor)
                .clip(shape = MaterialTheme.shapes.medium)
                .onFocusChanged { isFocused = it.isFocused }
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = MaterialTheme.shapes.medium
                ),
            keyboardOptions = keyboardOptions,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = contentPadding.dp)
                ) {
                    if (keyboardOptions.keyboardType == KeyboardType.Email) {
                        Icon(
                            painter = painterResource(id = R.drawable.email_icon),
                            contentDescription = "Email",
                            tint = Color.Unspecified,
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                            )
                        }
                        innerTextField()
                    }

                    if (trailing != null) {
                        Box(modifier = Modifier.padding(start = 8.dp)) {
                            trailing()
                        }
                    }
                }
            }
        )

        // Show either the error (preferentially) or the regular bottom label.
        val bottomText = error ?: bottomLabel
        if (!bottomText.isNullOrEmpty()) {
            Text(
                text = bottomText,
                color = if (!error.isNullOrEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuthTextFieldPreview() {
    AuthTextField(
        bgColor = MaterialTheme.colorScheme.background,
        label = "Email Address",
        placeholder = "placeholder",
        error = "",
        bottomLabel = "bottomLabel",
        value = "",
        onValueChange = {},
        modifier = Modifier,
    )
}