package com.example.truxpense.presentation.screens.onboarding.username

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.presentation.screens.auth.components.AuthButton
import com.example.truxpense.presentation.screens.auth.components.AuthTextField
import com.example.truxpense.presentation.utils.blockTouchesWhen
import com.example.truxpense.presentation.utils.clearFocusOnTap

@SuppressLint("ContextCastToActivity")
@Composable
fun UsernameScreen(
    onComplete: (() -> Unit)? = null,
    viewModel: UsernameViewModel = hiltViewModel()
) {
    // Read username from ViewModel
    val username by viewModel.username.collectAsState(initial = "")
    val usernameError by viewModel.error.collectAsState(initial = null)
    val isSaving by viewModel.isSaving.collectAsState(initial = false)
    val enabled = username.isNotBlank() && !isSaving

    // When on the username screen, pressing system back should exit the app.
    val activity = LocalContext.current as? Activity
    BackHandler(enabled = true) {
        activity?.finish()
    }

    Scaffold(
        bottomBar = {
            UsernameBottomBar(
                onComplete = { if (username.isNotBlank()) viewModel.saveAndComplete { onComplete?.invoke() } },
                enabled = enabled,
                isLoading = isSaving
            )
        }
    ) { innerPadding ->
        // Main content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clearFocusOnTap()
                .blockTouchesWhen(isSaving) // disable taps during saving
        ) {
            UsernameContent(
                username = username,
                onUsernameChange = { viewModel.onUsernameChanged(it) },
                onComplete = { if (enabled) viewModel.saveAndComplete { onComplete?.invoke() } },
                enabled = enabled,
                showActions = false,
                error = usernameError
            )
        }
    }
}


@Composable
private fun UsernameBottomBar(
    onComplete: () -> Unit,
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
            onClick = onComplete,
            text = "Continue",
            enabled = enabled,
            isLoading = isLoading
        )
    }
}

@Composable
fun UsernameContent(
    username: String,
    onUsernameChange: (String) -> Unit,
    onComplete: () -> Unit = {},
    enabled: Boolean = false,
    showActions: Boolean = false,
    error: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clearFocusOnTap(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = "What should we call you?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "This helps us personalize your experience",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        AuthTextField(
            bgColor = MaterialTheme.colorScheme.background,
            label = "Full Name",
            placeholder = "e.g. john_doe",
            bottomLabel = "You can change this later",
            value = username,
            keyboardOptions = KeyboardOptions.Default,
            onValueChange = onUsernameChange,
            contentPadding = 16,
            modifier = Modifier.fillMaxWidth(),
            error = error,
        )

        if (showActions) {
            Spacer(modifier = Modifier.weight(1f))
            AuthButton(
                onClick = onComplete,
                text = "Continue",
                enabled = enabled
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, name = "Username - Light")
@Composable
fun UsernameContentPreviewLight() {
    MaterialTheme {
        Surface {
            Column(modifier = Modifier.fillMaxSize()) {
                UsernameContent(
                    username = "",
                    onUsernameChange = {},
                    onComplete = {},
                    enabled = false,
                    showActions = true
                )
            }
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Username - Dark")
@Composable
fun UsernameContentPreviewDark() {
    MaterialTheme {
        Surface {
            Column(modifier = Modifier.fillMaxSize()) {
                UsernameContent(
                    username = "john_doe",
                    onUsernameChange = {},
                    onComplete = {},
                    enabled = true,
                    showActions = true
                )
            }
        }
    }
}