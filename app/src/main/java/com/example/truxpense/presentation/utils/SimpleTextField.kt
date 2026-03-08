package com.example.truxpense.presentation.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A lightweight reusable text field that follows the project's AuthTextField visual style
 * but intentionally has no trailing icon. Fixed height = 48.dp.
 *
 * Designed for simple single-line inputs used across the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    bgColor: Color = MaterialTheme.colorScheme.surface,
    placeholder: String? = null,
    label: String? = null,
    error: String? = null,
    bottomLabel: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null,
    singleLine: Boolean = true,
    contentPadding: Int = 0,
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Column(modifier = modifier) {
        if (label != null && label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(4.dp))
        }

        val borderColor = when {
            !error.isNullOrEmpty() -> MaterialTheme.colorScheme.error
            isFocused -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
        }

        val borderWidth = if (!error.isNullOrEmpty() || isFocused) 2.dp else 1.dp
        val colors = OutlinedTextFieldDefaults.colors()
        val shape = MaterialTheme.shapes.medium

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.padding(horizontal = contentPadding.dp).fillMaxWidth().height(48.dp).clip(shape)
                .background(bgColor).onFocusChanged { isFocused = it.isFocused }
                .border(width = borderWidth, color = borderColor, shape = shape),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onBackground
            ),
            keyboardOptions = keyboardOptions.copy(imeAction = imeAction),
            keyboardActions = KeyboardActions(onDone = { onImeAction?.invoke() }),
            singleLine = singleLine,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = singleLine,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    placeholder = { if (placeholder != null) Text(placeholder) },
                    contentPadding = OutlinedTextFieldDefaults.contentPadding(
                        top = 0.dp,
                        bottom = 0.dp,
                        start = 12.dp,
                        end = 12.dp,
                    ),
                    colors = colors,
                    container = {},
                )
            },
        )

        val bottomText = error ?: bottomLabel
        if (!bottomText.isNullOrEmpty()) {
            Text(
                text = bottomText,
                color = if (!error.isNullOrEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp, start = contentPadding.dp, end = contentPadding.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SimpleTextFieldPreview() {
    SimpleTextField(
        value = "",
        onValueChange = {},
        label = "",
        placeholder = "Enter custom category",
        modifier = Modifier.padding(16.dp)
    )
}