package com.example.truxpense.presentation.screens.dashboard.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.screens.dashboard.components.SmsPermissionBanner

@Composable
fun EmptyHomeContent(
    onAddExpense: (() -> Unit)? = null,
    hasSmsPermission: Boolean,
    onSmsGranted: (() -> Unit)? = null
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, topBar = {
            ScreenTopBar(
                headerTitle = "TruXpense", showBack = false, showProfileIcons = true
            )
        }) { innerPadding ->

        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
        ) {
            // SMS banner
            if (!hasSmsPermission) {
                SmsPermissionBanner(
                    modifier = Modifier.fillMaxWidth(), onGranted = { onSmsGranted?.invoke() })
                Spacer(Modifier.height(8.dp))
            }

            // Centered body
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).weight(1f),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.illustration_home),
                    contentDescription = "Home illustration",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.45f)
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Start tracking your spending",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "We'll automatically track expenses from SMS, or you can add them manually.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Bottom actions
            Button(
                onClick = { onAddExpense?.invoke() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Add your first expense", color = MaterialTheme.colorScheme.background)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EmptyHomeTabScreenPreview() {
    MaterialTheme {
        EmptyHomeContent(
            onAddExpense = {}, hasSmsPermission = false, onSmsGranted = {}
        )
    }
}