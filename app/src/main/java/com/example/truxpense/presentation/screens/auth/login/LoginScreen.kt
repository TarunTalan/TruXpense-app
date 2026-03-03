package com.example.truxpense.presentation.screens.auth.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.example.truxpense.presentation.utils.blockTouchesWhen
import com.example.truxpense.presentation.utils.clearFocusOnTap

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onNavigateToOtp: (String, AuthFlowType) -> Unit,
    onNavigateToHome: (String) -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToUsername: (String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val otpLockSeconds by viewModel.otpLockSecondsRemaining.collectAsState()

    // Handle navigation events
    LaunchedEffect(state.navigateToOtp) {
        if (state.navigateToOtp) {
            onNavigateToOtp(state.email, AuthFlowType.LOGIN)
            viewModel.onEvent(LoginEvent.OnNavigationHandled)
        }
    }

    LaunchedEffect(state.navigateToHome, state.authToken) {
        if (state.navigateToHome && state.authToken != null) {
            onNavigateToHome(state.authToken!!)
            viewModel.onEvent(LoginEvent.OnNavigationHandled)
        }
    }

    LaunchedEffect(state.navigateToUsername, state.authToken) {
        if (state.navigateToUsername && state.authToken != null) {
            onNavigateToUsername(state.authToken!!)
            viewModel.onEvent(LoginEvent.OnNavigationHandled)
        }
    }

    Scaffold(
        topBar = {
            LoginTopBar(
                onBack = onBack,
                enabled = !state.isLoading
            )
        },
        bottomBar = {
            LoginBottomBar(
                onLogin = { viewModel.onEvent(LoginEvent.LoginWithEmail) },
                onNavigateToSignup = onNavigateToSignup,
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
                .blockTouchesWhen(state.isLoading)
        ) {
            LoginContent(
                state = state,
                otpLockSeconds = otpLockSeconds,
                onEvent = viewModel::onEvent
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
private fun LoginTopBar(
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
private fun LoginBottomBar(
    onLogin: () -> Unit,
    onNavigateToSignup: () -> Unit,
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
            onClick = onLogin,
            text = "Verify Email",
            enabled = enabled,
            isLoading = isLoading
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "New to TruXpense?",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            Text(
                text = " Sign up",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(enabled = !isLoading) { onNavigateToSignup() }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun LoginContent(
    state: LoginUiState,
    otpLockSeconds: Int,
    onEvent: (LoginEvent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LoginHeader()
        Spacer(modifier = Modifier.height(26.dp))

        // Email Input with integrated error display
        val lockMsg =
            if (otpLockSeconds > 0) "Locked: Too many attempts. Try again in ${formatLockTime(otpLockSeconds)}" else null
        AuthTextField(
            bgColor = MaterialTheme.colorScheme.background,
            label = "Email Address",
            placeholder = "Example@xyz.com",
            bottomLabel = "We'll send a verification code to this email",
            error = state.error ?: lockMsg,
            value = state.email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            onValueChange = { onEvent(LoginEvent.EmailChanged(it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // T&C checkbox removed from Login screen
    }
}

@Composable
private fun LoginHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Login",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = "Welcome back!",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

// Terms & Conditions UI removed from LoginScreen.kt

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
fun LoginScreenPreview() {
    LoginScreen(
        onBack = {},
        onNavigateToHome = {},
        onNavigateToSignup = {},
        onNavigateToUsername = {},
        onNavigateToOtp = { _, _ -> }
    )
}