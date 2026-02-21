package com.example.truxpense.presentation.screens.dashboard.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.scale
import com.example.truxpense.R

// ─── Models ──────────────────────────────────────────────────────────────────

data class SettingsMenuItem(
    val iconRes: Int,
    val title: String,
    val subtitle: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    username: String = "Tamanna",
    phone: String = "+91 98XXXXXXXX",
    smsEnabled: Boolean = true,
    notificationsEnabled: Boolean = true,
    onPersonalInfo: () -> Unit = {},
    onLinkedAccounts: () -> Unit = {},
    onSecurity: () -> Unit = {},
    onSmsToggle: (Boolean) -> Unit = {},
    onNotificationsToggle: (Boolean) -> Unit = {},
    onHelp: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    onTerms: () -> Unit = {},
    onAbout: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    var smsAccess by remember { mutableStateOf(smsEnabled) }
    var notifications by remember { mutableStateOf(notificationsEnabled) }

    val accountItems = listOf(
        SettingsMenuItem(R.drawable.account, "Personal information", "Edit name, email, phone"),
        SettingsMenuItem(R.drawable.linked_accounts, "Linked accounts", "Bank/ SMS source status"),
        SettingsMenuItem(R.drawable.security, "Security", "Login method, OTP info"),
        SettingsMenuItem(R.drawable.personal_info, "Personal information", "Edit name, email, phone"),
    )

    val supportItems = listOf(
        SettingsMenuItem(R.drawable.help, "Help & Support"),
        SettingsMenuItem(R.drawable.privacy_policy, "Privacy policy"),
        SettingsMenuItem(R.drawable.description, "Terms of service"),
        SettingsMenuItem(R.drawable.folder_icon, "About TruXpense"),
    )

    val supportActions = listOf(onHelp, onPrivacyPolicy, onTerms, onAbout)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Use a simple TopAppBar with only title (no icons)
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {

            // ── User header ───────────────────────────────────────────────
            item {
                UserHeader(
                    username = username,
                    phone = phone,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(20.dp))
            }

            // ── Account section ───────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SectionLabel("Account")
                MenuSection {
                    accountItems.forEachIndexed { index, item ->
                        val clickAction = when (index) {
                            0 -> onPersonalInfo
                            1 -> onLinkedAccounts
                            2 -> onSecurity
                            else -> onPersonalInfo
                        }
                        MenuRow(
                            iconRes = item.iconRes,
                            title = item.title,
                            subtitle = item.subtitle,
                            onClick = clickAction,
                            showDivider = index < accountItems.lastIndex
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Permissions section ───────────────────────────────────────
            item {
                SectionLabel("Permissions")
                MenuSection {
                    // SMS access row — tap to toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { smsAccess = !smsAccess; onSmsToggle(!smsAccess) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.sms),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = "SMS access",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Enabled",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (smsAccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "/ ",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Disabled",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (!smsAccess) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 50.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Notifications row with a slightly smaller switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.notifications),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = "Notifications",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        // Scale down the Switch to make it smaller and visually consistent
                        Box(modifier = Modifier.scale(0.75f)) {
                            Switch(
                                checked = notifications,
                                onCheckedChange = {
                                    notifications = it
                                    onNotificationsToggle(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                )
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Support section ───────────────────────────────────────────
            item {
                SectionLabel("Support")
                MenuSection {
                    supportItems.forEachIndexed { index, item ->
                        MenuRow(
                            iconRes = item.iconRes,
                            title = item.title,
                            subtitle = null,
                            onClick = supportActions[index],
                            showDivider = index < supportItems.lastIndex
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Log out ───────────────────────────────────────────────────
            item {
                Text(
                    text = "Log out",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable(onClick = onLogout)
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ─── Reusable Components ──────────────────────────────────────────────────────

@Composable
private fun UserHeader(
    username: String,
    phone: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = phone,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Avatar circle with initial
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Safely derive the initial from username. Protect against empty/blank username
            val initial = username.trim().let { if (it.isNotEmpty()) it[0].uppercaseChar().toString() else "?" }

            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun MenuSection(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        Column(content = content)
    }
}

@Composable
private fun MenuRow(
    iconRes: Int,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary

                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 50.dp),
                color = MaterialTheme.colorScheme.outline.copy(0.5f)
            )
        }
    }
}

// ─── Preview ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        SettingsScreen()
    }
}