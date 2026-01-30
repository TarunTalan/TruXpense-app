package com.example.truxpense.presentation.screens.onboarding.username

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.auth.components.AuthButton
import com.example.truxpense.presentation.screens.auth.components.AuthTextField
import com.example.truxpense.presentation.utils.clearFocusOnTap

@Composable
fun UsernameScreen(
    onBack: (() -> Unit)? = null,
    onComplete: (() -> Unit)? = null,
    viewModel: UsernameViewModel = hiltViewModel()
) {
    // Read username from ViewModel
    val username by viewModel.username.collectAsState(initial = "")
    val usernameError by viewModel.error.collectAsState(initial = null)
    val isSaving by viewModel.isSaving.collectAsState(initial = false)
    val enabled = username.isNotBlank() && !isSaving

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(onClick = { onBack?.invoke() }) {
                    Icon(painter = painterResource(id = R.drawable.back_icon), contentDescription = "Back")
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuthButton(
                    onClick = { if (username.isNotBlank()) viewModel.saveAndComplete { onComplete?.invoke() } },
                    text = "Continue",
                    enabled = enabled,
                    isLoading = isSaving
                )
            }
        }
    ) { innerPadding ->
        // Main content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clearFocusOnTap()
        ) {
            UsernameContent(
                username = username,
                onUsernameChange = { viewModel.onUsernameChanged(it) },
                onBack = { onBack?.invoke() },
                onComplete = { if (enabled) viewModel.saveAndComplete { onComplete?.invoke() } },
                enabled = enabled,
                showActions = false,
                error = usernameError
            )
        }
    }
}

@Composable
fun UsernameContent(
    username: String,
    onUsernameChange: (String) -> Unit,
    onBack: () -> Unit = {},
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
        if (showActions) {
            // Small top back action for preview-only
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.back_icon),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.clickable { onBack() }.padding(vertical = 20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "What should we call you?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "This helps us personalize your experience",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(20.dp))

        AuthTextField(
            bgColor = MaterialTheme.colorScheme.background,
            label = "Username",
            placeholder = "e.g. john_doe",
            bottomLabel = "You can change this later",
            value = username,
            keyboardOptions = KeyboardOptions.Default,
            onValueChange = onUsernameChange,
            contentPadding = 16,
            modifier = Modifier.fillMaxWidth(),
            error = error,
            enabled = true,
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
                    onBack = {},
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
                    onBack = {},
                    onComplete = {},
                    enabled = true,
                    showActions = true
                )
            }
        }
    }
}