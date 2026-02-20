package com.example.truxpense.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import android.content.res.Configuration


@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Enter amount",
    leadingSymbol: String = "₹",
    leadingIcon: (@Composable (() -> Unit))? = null,
    textStyle: TextStyle = TextStyle(
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 15.sp
    ),
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    focusedBorderColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(10.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocusedState = interactionSource.collectIsFocusedAsState()
    val isFocused = isFocusedState.value

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) focusedBorderColor else borderColor,
                shape = shape
            )
            .background(backgroundColor, shape)
            .clip(shape)
            .padding(8.dp),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                } else {
                    Text(
                        text = leadingSymbol,
                        color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp
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
