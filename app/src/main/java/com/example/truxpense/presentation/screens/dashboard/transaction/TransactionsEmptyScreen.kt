package com.example.truxpense.presentation.screens.dashboard.transaction

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.screens.dashboard.components.SmsPermissionBanner

@Composable
fun TransactionsTopBar() {
    ScreenTopBar(headerTitle = "Transactions", showBack = false)
}

@Composable
fun TransactionsEmptyScreen(
    modifier: Modifier = Modifier,
    onAddTransaction: () -> Unit = {},
    hasSmsPermission: Boolean = true,
    onSmsGranted: (() -> Unit)? = null
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, topBar = { TransactionsTopBar() }) { innerPadding ->

        Column(
            modifier = modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
        ) {

            if (!hasSmsPermission) {
                SmsPermissionBanner(modifier = Modifier.fillMaxWidth(), onGranted = { onSmsGranted?.invoke() })
                Spacer(Modifier.height(8.dp))
            }

            // Centered body
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).weight(1f),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.transaction_illuustraton),
                    contentDescription = "Transactions illustration",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.45f)
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "No transactions yet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Add your first transactions to see insights and summaries.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Bottom actions
            Button(
                onClick = { onAddTransaction.invoke() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Add Transaction", color = MaterialTheme.colorScheme.background)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TransactionsEmptyScreenPreview() {
    MaterialTheme {
        TransactionsEmptyScreen(hasSmsPermission = false)
    }
}
