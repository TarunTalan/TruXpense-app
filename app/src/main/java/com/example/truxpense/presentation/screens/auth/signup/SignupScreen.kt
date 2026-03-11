package com.example.truxpense.presentation.screens.auth.signup

import android.graphics.Color.rgb
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.auth.AuthFlowType
import com.example.truxpense.presentation.screens.auth.components.AuthButton
import com.example.truxpense.presentation.screens.auth.components.AuthTextField
import com.example.truxpense.presentation.theme.AppDialogTheme
import com.example.truxpense.presentation.utils.blockTouchesWhen
import com.example.truxpense.presentation.utils.clearFocusOnTap

@Composable
fun SignupScreen(
    onBack: () -> Unit,
    onNavigateToOtp: (String, AuthFlowType) -> Unit,
    onNavigateToUsername: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val otpLockSeconds by viewModel.otpLockSecondsRemaining.collectAsState()

    // Handle navigation events
    LaunchedEffect(state.navigateToOtp) {
        if (state.navigateToOtp) {
            onNavigateToOtp(state.email, AuthFlowType.SIGNUP)
            viewModel.onEvent(SignupEvent.OnNavigationHandled)
        }
    }

    LaunchedEffect(state.navigateToUsername, state.authToken) {
        if (state.navigateToUsername && state.authToken != null) {
            onNavigateToUsername(state.authToken!!)
            viewModel.onEvent(SignupEvent.OnNavigationHandled)
        }
    }

    // If the screen is opened with an already-filled email (cold start), ensure we start watching its lock
    LaunchedEffect(state.email) {
        val emailNorm = state.email.trim().lowercase()
        if (emailNorm.isNotEmpty()) viewModel.watchLockFor(emailNorm)
    }

    Scaffold(
        topBar = {
            SignupTopBar(
                onBack = onBack,
                enabled = !state.isLoading
            )
        },
        bottomBar = {
            SignupBottomBar(
                onSignUp = { viewModel.onEvent(SignupEvent.SignUpWithEmail) },
                onNavigateToLogin = onNavigateToLogin,
                enabled = state.email.isNotBlank() && !state.isLoading && otpLockSeconds == 0,
                isLoading = state.isLoading
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clearFocusOnTap()
                .then(if (state.showTncDialog) Modifier.blur(16.dp) else Modifier)
                .blockTouchesWhen(state.isLoading)
        ) {
            SignupContent(
                state = state,
                otpLockSeconds = otpLockSeconds,
                onEvent = viewModel::onEvent
            )
        }

        if (state.showTncDialog) {
            TncDialog(
                onDismiss = { viewModel.onEvent(SignupEvent.ShowTncDialog(false)) }
            )
        }
    }

    // Ensure lock is watched on resume (covers background -> foreground)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, state.email) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val emailNorm = state.email.trim().lowercase()
                if (emailNorm.isNotEmpty()) viewModel.watchLockFor(emailNorm)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable
private fun SignupTopBar(
    onBack: () -> Unit,
    enabled: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val backTint = if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onBackground
        IconButton(
            onClick = { if (enabled) onBack() },
            modifier = Modifier
        ) {
            Icon(
                painter = painterResource(id = R.drawable.back_icon),
                contentDescription = "Back",
                tint = backTint
            )
        }
    }
}

@Composable
private fun SignupBottomBar(
    onSignUp: () -> Unit,
    onNavigateToLogin: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthButton(
            onClick = onSignUp,
            text = "Sign Up",
            enabled = enabled,
            isLoading = isLoading
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account?",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            Text(
                text = " Login",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(enabled = !isLoading) { onNavigateToLogin() }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun SignupContent(
    state: SignUpUiState,
    otpLockSeconds: Int,
    onEvent: (SignupEvent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SignupHeader()
        Spacer(modifier = Modifier.height(26.dp))

        // Email Input with integrated lockout display
        val lockMsg =
            if (otpLockSeconds > 0) "Too many attempts. Try again in ${formatLockTime(otpLockSeconds)}" else null
        AuthTextField(
            bgColor = MaterialTheme.colorScheme.background,
            label = "Email Address",
            placeholder = "Example@xyz.com",
            bottomLabel = "We'll send a verification code to this email",
            error = state.error ?: lockMsg,
            value = state.email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            onValueChange = { onEvent(SignupEvent.EmailChanged(it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TncCheckbox(
            checked = state.agreeTnc,
            onCheckedChange = { onEvent(SignupEvent.AgreeTncChanged(it)) },
            onShowTnc = { onEvent(SignupEvent.ShowTncDialog(true)) },
            enabled = !state.isLoading && otpLockSeconds == 0
        )
    }
}

@Composable
private fun SignupHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sign Up",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = "Create your account",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TncCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onShowTnc: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier
                .padding(start = 4.dp, end = 12.dp)
                .size(20.dp),
            colors = CheckboxDefaults.colors(
                uncheckedColor = MaterialTheme.colorScheme.outline,
                checkmarkColor = MaterialTheme.colorScheme.background,
                checkedColor = MaterialTheme.colorScheme.primary,
                disabledUncheckedColor = MaterialTheme.colorScheme.outline
            )
        )

        val isDarkTheme = !isSystemInDarkTheme()
        val tncColor = if (isDarkTheme) {
            Color(rgb(59, 120, 194))
        } else {
            Color(rgb(127, 169, 217))
        }

        val annotatedString = buildAnnotatedString {
            append("I agree to the ")
            pushStringAnnotation(tag = "TNC", annotation = "tnc")
            withStyle(
                style = SpanStyle(
                    color = tncColor,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("Terms and Conditions")
            }
            pop()
        }

        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.clickable(enabled = enabled) { onShowTnc() }
        )
    }
}

@Composable
private fun TncDialog(onDismiss: () -> Unit) {
    AppDialogTheme {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Terms and Conditions") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("TruXpense reads only bank transaction messages to help you track your expenses automatically.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "What we access:", fontWeight = FontWeight.SemiBold)
                    Text("• Bank transaction SMS messages")
                    Text("• App usage data for improvements")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "What we don't access:", fontWeight = FontWeight.SemiBold)
                    Text("• Personal messages")
                    Text("• OTPs from other apps")
                    Text("• Contacts or call logs")
                    Text("• Any other unrelated personal data")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "By continuing, you agree to allow TruXpense to access the data listed above for the purpose of automatically categorizing and tracking your expenses.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Close") }
            },
        )
    }
}

private fun formatLockTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes > 0) {
        "$minutes:${secs.toString().padStart(2, '0')}"
    } else {
        "$secs seconds"
    }
}

@Preview(showBackground = true)
@Composable
fun SignupScreenPreview() {
    MaterialTheme {
        SignupContent(
            state = SignUpUiState(
                email = "demo@example.com",
                agreeTnc = true,
                isEmailValid = true,
                canSignUp = true
            ),
            otpLockSeconds = 0,
            onEvent = {}
        )
    }
}