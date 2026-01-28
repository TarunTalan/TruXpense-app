package com.example.truxpense.presentation.screens.auth.otp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.presentation.screens.auth.components.AuthButton
import com.example.truxpense.presentation.utils.clearFocusOnTap

@Composable
fun OtpScreen(
    onBack: (() -> Unit)? = null,
    onVerified: (() -> Unit)? = null,
    onResend: (() -> Unit)? = null,
    otpLength: Int = 6,
    resendSeconds: Int = 30,
    viewModel: OtpViewModel = hiltViewModel()
) {
    val otpDigits by viewModel.digits.collectAsState()
    val canResend by viewModel.canResend.collectAsState()
    val resendRemaining by viewModel.resendSecondsRemaining.collectAsState()

    val focusRequesters = remember { List(otpLength) { FocusRequester() } }
    val keyboardController = LocalSoftwareKeyboardController.current

    val enabledVerify = otpDigits.all { it.isNotEmpty() }

    // Auto-focus first box on screen load
    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
        // start initial resend countdown
        viewModel.startResendTimer(resendSeconds)
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp, start = 5.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(onClick = { onBack?.invoke() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuthButton(
                    onClick = { onVerified?.invoke() },
                    text = "Verify OTP",
                    enabled = enabledVerify
                )
                Spacer(modifier = Modifier.height(8.dp))

                ResendButton(
                    onClick = {
                        onResend?.invoke()
                        viewModel.startResendTimer(resendSeconds)
                    },
                    text = if (canResend) "Resend OTP" else "Resend in ${resendRemaining}s",
                    enabled = canResend,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Verify OTP",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Enter the 6-digit code sent to your email",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // OTP boxes
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                for (i in 0 until otpLength) {
                    val value = otpDigits[i]
                    val isFocused = remember { mutableStateOf(false) }

                    OtpDigitBox(
                        value = value,
                        index = i,
                        otpLength = otpLength,
                        isFocused = isFocused.value,
                        onValueChange = { newValue ->
                            when {
                                // Handle paste (multiple characters)
                                newValue.length > 1 -> {
                                    val digitsOnly = newValue.filter { it.isDigit() }
                                    if (digitsOnly.isNotEmpty()) {
                                        // Fill remaining boxes with pasted digits
                                        digitsOnly.take(otpLength - i).forEachIndexed { index, char ->
                                            viewModel.updateDigit(i + index, char.toString())
                                        }
                                        // Focus last filled box or last box
                                        val lastIndex = minOf(i + digitsOnly.length - 1, otpLength - 1)
                                        if (lastIndex < otpLength - 1) {
                                            focusRequesters[lastIndex + 1].requestFocus()
                                        } else {
                                            keyboardController?.hide()
                                        }
                                    }
                                }
                                // Handle single digit input
                                newValue.length == 1 && newValue.all { it.isDigit() } -> {
                                    // Overwrite existing value
                                    viewModel.updateDigit(i, newValue)
                                    // Move to next box
                                    if (i < otpLength - 1) {
                                        focusRequesters[i + 1].requestFocus()
                                    } else {
                                        keyboardController?.hide()
                                    }
                                }
                                // Handle deletion
                                newValue.isEmpty() -> {
                                    viewModel.updateDigit(i, "")
                                }
                            }
                        },
                        onBackspace = {
                            if (value.isEmpty() && i > 0) {
                                // Current box is empty, move back and clear previous
                                focusRequesters[i - 1].requestFocus()
                                viewModel.updateDigit(i - 1, "")
                            } else {
                                // Current box has value, just clear it
                                viewModel.updateDigit(i, "")
                            }
                        },
                        onFocusChanged = { focused ->
                            isFocused.value = focused
                        },
                        focusRequester = focusRequesters[i],
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun OtpDigitBox(
    value: String,
    index: Int,
    otpLength: Int,
    isFocused: Boolean,
    onValueChange: (String) -> Unit,
    onBackspace: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isFocused -> MaterialTheme.colorScheme.primary
        value.isNotEmpty() -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }

    // Use TextFieldValue to control cursor position
    var textFieldValue by remember(value) {
        mutableStateOf(TextFieldValue(
            text = value,
            selection = TextRange(value.length)
        ))
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newTextFieldValue ->
                val newText = newTextFieldValue.text

                // Only process if the text has actually changed
                if (newText != value) {
                    when {
                        // Paste scenario: multiple digits
                        newText.length > 1 -> {
                            onValueChange(newText)
                        }
                        // Single digit: take only the last character (overwrite)
                        newText.length == 1 && newText.all { it.isDigit() } -> {
                            val lastChar = newText.last().toString()
                            textFieldValue = TextFieldValue(
                                text = lastChar,
                                selection = TextRange(1)
                            )
                            onValueChange(lastChar)
                        }
                        // Empty: deletion - DON'T call onValueChange here, let onBackspace handle it
                        newText.isEmpty() -> {
                            // Just update the local text field, onBackspace will handle the logic
                            textFieldValue = TextFieldValue(
                                text = "",
                                selection = TextRange(0)
                            )
                        }
                        // If somehow more than one digit but not paste, take last digit
                        else -> {
                            val digitsOnly = newText.filter { it.isDigit() }
                            if (digitsOnly.isNotEmpty()) {
                                val lastChar = digitsOnly.last().toString()
                                textFieldValue = TextFieldValue(
                                    text = lastChar,
                                    selection = TextRange(1)
                                )
                                onValueChange(lastChar)
                            }
                        }
                    }
                }
            },
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = if (index == otpLength - 1) ImeAction.Done else ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    onFocusChanged(focusState.isFocused)
                    // When focused, select all text for easy overwrite
                    if (focusState.isFocused && value.isNotEmpty()) {
                        textFieldValue = textFieldValue.copy(
                            selection = TextRange(0, value.length)
                        )
                    }
                }
                .onPreviewKeyEvent { keyEvent ->
                    // Handle backspace/delete key on key down
                    if (keyEvent.type == KeyEventType.KeyDown &&
                        (keyEvent.key == Key.Backspace || keyEvent.key == Key.Delete)
                    ) {
                        onBackspace()
                        true // Consume the event
                    } else {
                        false
                    }
                },
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "•",
                            fontSize = 32.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun ResendButton(
    onClick: () -> Unit,
    content: @Composable (() -> Unit)? = null,
    text: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val borderColor = if (enabled) {
        MaterialTheme.colorScheme.outline
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onTertiary,
            disabledContainerColor = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.secondary,
            disabledContentColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .height(46.dp)
            .clip(shape = MaterialTheme.shapes.medium)
            .border(1.dp, borderColor, shape = MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
    ) {
        if (text != null) {
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                }
            )
        } else {
            content?.invoke()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OtpScreenPreview() {
    OtpScreen()
}