package com.example.truxpense.presentation.screens.dashboard.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.theme.DashboardDimens

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
    onEnableSms: () -> Unit = {},
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
            // SMS status banner — only shown when permission is NOT granted
            item {
                if (!smsPermissionGranted) {
                    SmsBanner(
                        modifier = Modifier.fillMaxWidth(),
                        onEnable = onEnableSms,
                    )
                }
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
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Add Bank Account")
                }
            }
        }
    }
}

@Composable
private fun SmsBanner(
    modifier: Modifier = Modifier,
    onEnable: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val startGrd = colorScheme.surfaceContainerLowest
    val endGrd = colorScheme.surfaceContainerHighest
    val smsGradient = remember(startGrd, endGrd) {
        Brush.horizontalGradient(0.0f to startGrd, 1f to endGrd)
    }
    val isDark = isSystemInDarkTheme()
    val borderColor = Color(0xFFF4A62A)
    val txtColor = if (isDark) colorScheme.surfaceContainerHighest else Color.White

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(DashboardDimens.cornerCard))
            .background(smsGradient)
            .border(
                color = borderColor,
                width = 1.dp,
                shape = RoundedCornerShape(DashboardDimens.cornerCard),
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.sms_icon),
                contentDescription = null,
                tint = colorScheme.errorContainer,
                modifier = Modifier.size(DashboardDimens.iconMd),
            )
            Text(
                text = "Enable SMS access",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.errorContainer,
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onEnable,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.errorContainer,
                    contentColor = txtColor,
                ),
                modifier = Modifier.height(30.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text(
                    "Enable",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Text(
            text = "Automatically track expenses from bank sms.",
            fontSize = 11.sp,
            color = colorScheme.errorContainer,
            lineHeight = 15.sp,
            maxLines = 1,
        )
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

@Preview(showBackground = true, showSystemUi = true, name = "With accounts + SMS granted")
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

@Preview(showBackground = true, showSystemUi = true, name = "Empty + SMS not granted")
@Composable
fun LinkedAccountsScreenSmsPreview() {
    MaterialTheme {
        LinkedAccountsScreen(accounts = emptyList(), smsPermissionGranted = false)
    }
}

