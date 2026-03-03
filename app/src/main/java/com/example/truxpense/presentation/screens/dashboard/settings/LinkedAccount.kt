package com.example.truxpense.presentation.screens.dashboard.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import androidx.compose.ui.tooling.preview.Preview

data class LinkedAccount(
    val bankName: String,
    val accountMasked: String,
    val isActive: Boolean,
)

@Composable
fun LinkedAccountsScreen(
    accounts: List<LinkedAccount> = emptyList(),
    smsPermissionGranted: Boolean = true,
    onBack: () -> Unit = {},
    onAddAccount: () -> Unit = {},
    onRemoveAccount: (LinkedAccount) -> Unit = {},
) {
    var accountList  by remember { mutableStateOf(accounts) }
    var removeTarget by remember { mutableStateOf<LinkedAccount?>(null) }

    if (removeTarget != null) {
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text("Remove Account") },
            text = {
                Text("Remove ${removeTarget!!.bankName} (${removeTarget!!.accountMasked}) from TruXpense?")
            },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveAccount(removeTarget!!)
                    accountList = accountList - removeTarget!!
                    removeTarget = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { removeTarget = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(headerTitle = "Linked Accounts", showBack = true, onBack = onBack)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // SMS status banner
            item {
                SmsStatusBanner(granted = smsPermissionGranted)
            }

            item {
                Text(
                    text = "Your Banks",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            if (accountList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.linked_accounts),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No linked accounts yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            } else {
                items(accountList, key = { it.accountMasked }) { account ->
                    LinkedAccountCard(
                        account = account,
                        onRemove = { removeTarget = account }
                    )
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onAddAccount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("+ Add Bank Account")
                }
            }
        }
    }
}

@Composable
private fun SmsStatusBanner(granted: Boolean) {
    val containerColor = if (granted) MaterialTheme.colorScheme.primaryContainer
                         else MaterialTheme.colorScheme.errorContainer
    val contentColor   = if (granted) MaterialTheme.colorScheme.onPrimaryContainer
                         else MaterialTheme.colorScheme.onErrorContainer
    val iconColor      = if (granted) MaterialTheme.colorScheme.primary
                         else MaterialTheme.colorScheme.error

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.sms),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (granted) "SMS Access Active" else "SMS Access Required",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                Text(
                    text = if (granted)
                        "TruXpense is reading your bank SMS to auto-log transactions."
                    else
                        "Grant SMS permission so TruXpense can auto-detect transactions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun LinkedAccountCard(
    account: LinkedAccount,
    onRemove: () -> Unit,
) {
    val statusColor = if (account.isActive) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.error

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.linked_accounts),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.bankName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = account.accountMasked,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = statusColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = if (account.isActive) "Active" else "Inactive",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            TextButton(
                onClick = onRemove,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Remove", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LinkedAccountsScreenPreview() {
    val samples = listOf(
        LinkedAccount(bankName = "State Bank of India", accountMasked = "XXXX-1234", isActive = true),
        LinkedAccount(bankName = "Axis Bank", accountMasked = "XXXX-5678", isActive = false),
    )
    MaterialTheme {
        LinkedAccountsScreen(accounts = samples, smsPermissionGranted = true)
    }
}
