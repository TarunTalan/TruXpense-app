package com.example.truxpense.presentation.screens.auth.otp

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.auth.AuthFlowType
import com.example.truxpense.presentation.screens.auth.AuthFlowViewModel
import com.example.truxpense.presentation.screens.auth.components.AuthButton
import com.example.truxpense.presentation.utils.clearFocusOnTap
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    onBack: (() -> Unit)? = null,
    onVerified: (() -> Unit)? = null,
    onResend: (() -> Unit)? = null,
    otpLength: Int = 6,
    resendSeconds: Int = 30,
    authEmailParam: String? = null,
    flowParam: AuthFlowType? = null,
    viewModel: OtpViewModel = hiltViewModel()
) {
    val otpDigits by viewModel.digits.collectAsState()
    val canResend by viewModel.canResend.collectAsState()
    val resendRemaining by viewModel.resendSecondsRemaining.collectAsState()

    val authFlowVm: AuthFlowViewModel = hiltViewModel()
    val currentFlowFromVm by authFlowVm.flow.collectAsState()
    val authEmailFromVm by authFlowVm.email.collectAsState()

    // prefer explicit params passed from NavHost to avoid race conditions
    val currentFlow: AuthFlowType? = flowParam ?: currentFlowFromVm
    val authEmail: String = authEmailParam ?: authEmailFromVm

    // Use the ViewModel's error StateFlow so API errors are displayed inline
    val vmError by viewModel.error.collectAsState()

    // start initial resend countdown
    LaunchedEffect(Unit) {
        viewModel.startResendTimer(resendSeconds)
    }

    // Ensure we resume any persisted per-email lock countdown when this screen is shown
    LaunchedEffect(authEmail) {
        val emailNorm = authEmail.trim().lowercase()
        if (emailNorm.isNotEmpty()) {
            viewModel.watchLockFor(emailNorm)
        }
    }

    // Also restart watching when the screen lifecycle resumes (covers background/resume)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, authEmail) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val emailNorm = authEmail.trim().lowercase()
                if (emailNorm.isNotEmpty()) viewModel.watchLockFor(emailNorm)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Delegate UI to a UI-only composable so we can preview safely
    OtpContent(
        otpDigits = otpDigits,
        canResend = canResend,
        resendRemaining = resendRemaining,
        currentFlow = currentFlow,
        authEmail = authEmail,
        otpLength = otpLength,
        resendSeconds = resendSeconds,
        onBack = onBack,
        onDigitChange = { index, value -> viewModel.updateDigit(index, value) },
        onBackspace = { index -> viewModel.updateDigit(index, "") },
        onVerify = { otp ->
            // Call verify with explicit flowType so the correct backend endpoint is used
            viewModel.verifyOtp(
                email = authEmail,
                otp = otp,
                flowType = currentFlow,
                onSuccess = {
                    onVerified?.invoke()
                }
            )
        },
        onResend = {
            // Use viewModel.resendOtp with explicit flow type to call the right endpoint
            viewModel.resendOtp(
                email = authEmail,
                flowType = currentFlow,
                onSuccess = {
                    // restart timer and notify parent
                    viewModel.startResendTimer(resendSeconds)
                    onResend?.invoke()
                }
            )
        },
        // show vm error (set by viewModel when API fails)
        errorMessage = vmError
    )
}

@Composable
fun OtpContent(
    otpDigits: List<String>,
    canResend: Boolean,
    resendRemaining: Int,
    currentFlow: AuthFlowType?,
    authEmail: String,
    resendSeconds: Int,
    otpLength: Int,
    onBack: (() -> Unit)? = null,
    onDigitChange: (Int, String) -> Unit = { _, _ -> },
    onBackspace: (Int) -> Unit = {},
    onVerify: (String) -> Unit = {},
    onResend: () -> Unit = {},
    errorMessage: String? = null
) {
    val vm: OtpViewModel = hiltViewModel()
    val isLoading by vm.isLoading.collectAsState()
    val otpLockSeconds by vm.otpLockSecondsRemaining.collectAsState()

    val focusRequesters = remember { List(otpLength) { FocusRequester() } }
    val keyboardController = LocalSoftwareKeyboardController.current

    val enabledVerify = otpDigits.all { it.isNotEmpty() } && !isLoading
    val isOtpLocked = otpLockSeconds > 0
    val effectiveEnabledVerify = enabledVerify && !isOtpLocked

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val backTint = if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onBackground
                IconButton(onClick = { onBack?.invoke() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.back_icon),
                        contentDescription = null,
                        tint = backTint
                    )
                }
            }
        },
        // bottomBar with loading-aware button
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuthButton(
                    onClick = {
                        // Build OTP string
                        val otp = otpDigits.joinToString(separator = "")
                        // Call ViewModel verify which will hit the backend and return parsed messages
                        if (!isOtpLocked) onVerify(otp)
                    },
                    text = "Verify OTP",
                    enabled = effectiveEnabledVerify,
                    isLoading = isLoading
                )
                Spacer(modifier = Modifier.height(8.dp))

                ResendButton(
                    onClick = {
                        // Use ViewModel (vm) to resend and restart timer if not locked
                        if (!isOtpLocked) {
                            vm.resendOtp(
                                email = authEmail,
                                flowType = currentFlow,
                                onSuccess = {
                                    // restart timer and notify parent
                                    vm.startResendTimer(resendSeconds)
                                    onResend()
                                }
                            )
                        }
                    },
                    text = when {
                        isOtpLocked -> "Try again in ${formatSeconds(otpLockSeconds)}"
                        canResend -> "Resend OTP"
                        else -> "Resend in ${resendRemaining}s"
                    },
                    enabled = (canResend && !isLoading) && !isOtpLocked,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // ensure OTP screen has an opaque background so underlying screens/buttons
                // cannot be interacted with while OTP is active
                .background(MaterialTheme.colorScheme.background)
                .clearFocusOnTap(),
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

            Spacer(modifier = Modifier.height(26.dp))

            // OTP boxes
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val hasError = !errorMessage.isNullOrEmpty()
                for (i in 0 until otpLength) {
                    val value = otpDigits[i]
                    val isFocused = remember { mutableStateOf(false) }

                    OtpDigitBox(
                        value = value,
                        index = i,
                        otpLength = otpLength,
                        isFocused = isFocused.value,
                        hasError = hasError,
                        onValueChange = { newValue ->
                            when {
                                // Handle paste (multiple characters)
                                newValue.length > 1 -> {
                                    val digitsOnly = newValue.filter { it.isDigit() }
                                    if (digitsOnly.isNotEmpty()) {
                                        // Fill remaining boxes with pasted digits
                                        digitsOnly.take(otpLength - i).forEachIndexed { index, char ->
                                            onDigitChange(i + index, char.toString())
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
                                    onDigitChange(i, newValue)
                                    // Move to next box
                                    if (i < otpLength - 1) {
                                        focusRequesters[i + 1].requestFocus()
                                    } else {
                                        keyboardController?.hide()
                                    }
                                }
                                // Handle deletion
                                newValue.isEmpty() -> {
                                    onDigitChange(i, "")
                                }
                            }
                        },
                        onBackspace = {
                            if (value.isEmpty() && i > 0) {
                                // Current box is empty, move back and clear previous
                                focusRequesters[i - 1].requestFocus()
                                onBackspace(i - 1)
                            } else {
                                // Current box has value, just clear it
                                onBackspace(i)
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

            // Inline error message instead of AlertDialog. Suppress inline error while
            // the OTP lock countdown is active to avoid duplicate messages (lock text
            // is shown separately when `isOtpLocked` is true).
            if (!errorMessage.isNullOrEmpty() && !isOtpLocked) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            // When OTP is locked, show a small countdown message below the error area
            if (isOtpLocked) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Too many invalid attempts. Try again in ${formatSeconds(otpLockSeconds)}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // While verifying OTP, block pointer events on the entire screen so underlying UI
        // (or concurrent click events) can't activate OAuth buttons or other controls.
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .pointerInput(Unit) { /* no-op to restart when shown */ }
                    .pointerInteropFilter { true },
            )
        }
    }
}

// simple helper to format seconds as mm:ss
private fun formatSeconds(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", m, s)
}

@Composable
fun OtpDigitBox(
    value: String,
    index: Int,
    otpLength: Int,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    hasError: Boolean = false,
    onValueChange: (String) -> Unit,
    onBackspace: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    focusRequester: FocusRequester,
) {
    val borderColor = when {
        hasError -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.primary
        value.isNotEmpty() -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }

    // Use TextFieldValue to control cursor position
    var textFieldValue by remember(value) {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        )
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
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
                            text = "",
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable (() -> Unit)? = null,
    text: String? = null,
    enabled: Boolean = true,
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
            .fillMaxWidth()
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

@Preview(showBackground = true, name = "OTP - Light")
@Composable
fun OtpContentPreviewLight() {
    OtpContent(
        otpDigits = List(6) { "" },
        canResend = false,
        resendRemaining = 28,
        currentFlow = AuthFlowType.SIGNUP,
        authEmail = "demo@example.com",
        resendSeconds = 30,
        otpLength = 6,
        onBack = {},
        onDigitChange = { _, _ -> },
        onBackspace = {},
        onVerify = { /* no-op */ },
        onResend = {},
        errorMessage = null
    )
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "OTP - Dark")
@Composable
fun OtpContentPreviewDark() {
    OtpContent(
        otpDigits = List(6) { "" },
        canResend = true,
        resendRemaining = 0,
        currentFlow = AuthFlowType.LOGIN,
        authEmail = "demo@example.com",
        resendSeconds = 30,
        otpLength = 6,
        onBack = {},
        onDigitChange = { _, _ -> },
        onBackspace = {},
        onVerify = { /* no-op */ },
        onResend = {},
        errorMessage = "Invalid code, please try again"
    )
}
