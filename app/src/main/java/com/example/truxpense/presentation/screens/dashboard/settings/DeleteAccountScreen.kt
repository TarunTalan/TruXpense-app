package com.example.truxpense.presentation.screens.dashboard.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.auth.components.AuthTextField
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.utils.clearFocusOnTap

private const val CONFIRM_PHRASE = "DELETE MY ACCOUNT"

@Composable
fun DeleteAccountScreen(
    username: String = "",
    isDeleting: Boolean = false,
    onBack: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
) {
    var step by remember { mutableIntStateOf(0) }   // 0 = warning, 1 = type phrase
    var typed by remember { mutableStateOf("") }
    val phraseMatches = typed.trim().equals(CONFIRM_PHRASE, ignoreCase = false)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(headerTitle = "Delete Account", showBack = true, onBack = onBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .clearFocusOnTap(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Warning icon
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = painterResource(R.drawable.account),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = "Delete your account?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            // What will be deleted
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "This will permanently delete:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    val bullets = listOf(
                        "All your transaction history",
                        "Budget settings and categories",
                        "Linked bank account details",
                        "Notification preferences",
                        "Your profile (${username.ifBlank { "this account" }})",
                    )
                    bullets.forEach { item ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(
                                modifier = Modifier.size(6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Surface(
                                    modifier = Modifier.size(6.dp),
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.secondary,
                                ) {}
                            }
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (step == 0) {
                // Step 1 — initial warning
                Button(
                    onClick = { step = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onError,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("I understand, continue", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Keep my account")
                }
            } else {
                // Step 2 — type the confirmation phrase
                Text(
                    text = "Type  \"$CONFIRM_PHRASE\"  to confirm:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                AuthTextField(
                    bgColor = MaterialTheme.colorScheme.background,
                    label = null,
                    placeholder = CONFIRM_PHRASE,
                    value = typed,
                    onValueChange = { typed = it },
                    error = if (typed.isNotBlank() && !phraseMatches) "Phrase does not match" else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = onConfirmDelete,
                    enabled = phraseMatches && !isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        disabledContainerColor = MaterialTheme.colorScheme.onError.copy(0.7f),
                        disabledContentColor = MaterialTheme.colorScheme.error.copy(0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Delete My Account", fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(onClick = onBack) {
                    Text("Cancel", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DeleteAccountScreenPreview() {
    MaterialTheme {
        DeleteAccountScreen(username = "Tushar", isDeleting = false)
    }
}
