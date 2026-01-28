package com.example.truxpense.presentation.screens.onboarding.username

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.presentation.screens.auth.components.AuthButton
import com.example.truxpense.presentation.screens.auth.components.AuthTextField
import com.example.truxpense.presentation.utils.clearFocusOnTap

@Composable
fun UsernameScreen(
    onBack: (() -> Unit)? = null,
    onNext: ((String) -> Unit)? = null,
    viewModel: UsernameViewModel = hiltViewModel()
) {
    val username by viewModel.username.collectAsState()
    val enabled = username.isNotBlank()

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp, start = 5.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(onClick = { onBack?.invoke() }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                AuthButton(onClick = { if (enabled) onNext?.invoke(username) }, text = "Continue", enabled = enabled)
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
                text = "What should we call you?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "This help us personalized your experience",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(20.dp))

            AuthTextField(
                value = username,
                onValueChange = { viewModel.onUsernameChanged(it) },
                label = "Username",
                placeholder = "e.g. john_doe",
                bottomLabel = "You can change this later",
                bgColor = MaterialTheme.colorScheme.background,
                keyboardOptions = KeyboardOptions.Default,
                contentPadding = 16
            )
        }
    }
}

@Preview
@Composable
fun UsernameScreenPreview() {
    UsernameScreen()
}