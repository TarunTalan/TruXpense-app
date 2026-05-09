package com.example.truxpense.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.truxpense.presentation.screens.auth.components.AuthTextField

@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Enter amount",
    leadingSymbol: String = "₹",
    leadingIcon: (@Composable (() -> Unit))? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
    bgColor: Color = MaterialTheme.colorScheme.background,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    focusedBorderColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = MaterialTheme.shapes.medium,
    contentPadding: Int = 0,
    error: String? = null
) {
    // Use focus change like AuthTextField so border behavior matches exactly
    var isFocused by remember { mutableStateOf(false) }

    // Choose border color: error has priority, otherwise primary when focused, else provided borderColor
    val effectiveBorderColor = when {
        !error.isNullOrEmpty() -> MaterialTheme.colorScheme.error
        isFocused -> focusedBorderColor
        else -> borderColor
    }

    // Border width: make error and focused states more prominent (2.dp), otherwise 1.dp
    val borderWidth = if (!error.isNullOrEmpty() || isFocused) 2.dp else 1.dp

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(bgColor)
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = borderWidth,
                color = effectiveBorderColor,
                shape = shape
            ),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = contentPadding.dp)
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                } else {
                    Text(
                        text = leadingSymbol,
                        color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun NumberFieldPreviewLight() {
    Column(modifier = Modifier.padding(16.dp)) {
        NumberField(value = "", onValueChange = {})
        Spacer(Modifier.height(12.dp))
        NumberField(value = "1234", onValueChange = {}, leadingIcon = { Text(text = "₹") })
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, name = "NumberField - Dark")
@Composable
fun NumberFieldPreviewDark() {
    Column(modifier = Modifier.padding(16.dp)) {
        NumberField(value = "", onValueChange = {})
        Spacer(Modifier.height(12.dp))
        NumberField(value = "123456", onValueChange = {}, leadingIcon = { Text(text = "₹") })
    }
}

@Preview(showBackground = true, widthDp = 720, name = "Field Comparison")
@Composable
fun FieldComparisonPreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "AuthTextField", modifier = Modifier.padding(bottom = 4.dp))
        AuthTextField(
            bgColor = MaterialTheme.colorScheme.background,
            label = null,
            placeholder = "placeholder",
            error = null,
            bottomLabel = null,
            value = "",
            onValueChange = {},
            contentPadding = 12,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Text(text = "NumberField", modifier = Modifier.padding(bottom = 4.dp))
        NumberField(
            value = "",
            onValueChange = {},
            placeholder = "0",
            leadingIcon = { Text(text = "₹") },
            bgColor = MaterialTheme.colorScheme.background,
            contentPadding = 12,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
