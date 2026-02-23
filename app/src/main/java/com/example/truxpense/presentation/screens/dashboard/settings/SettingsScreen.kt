package com.example.truxpense.presentation.screens.dashboard.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar


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
        topBar = { ScreenTopBar(title = "Settings", showBack = false) }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {

            // User header
            item {
                UserHeader(
                    username = username,
                    phone = phone,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(20.dp))
            }

            // Account section
            item {
                Spacer(Modifier.height(8.dp))
                MenuSection {
                    SectionLabel("Account")
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
                            onClick = clickAction
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Permissions section
            item {
                MenuSection {
                    SectionLabel("Permissions")
                    // SMS access row — tap to toggle. Includes a subtitle explaining purpose.
                    val smsInteraction = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = smsInteraction, indication = null) {
                                val new = !smsAccess
                                smsAccess = new
                                onSmsToggle(new)
                            }
                            .padding(horizontal = 16.dp), // row controls horizontal inset now
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.sms),
                            contentDescription = "SMS access",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SMS access",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )

                        }

                        // Narrower fixed width container so toggles align vertically across rows
                        Box(modifier = Modifier.width(56.dp), contentAlignment = Alignment.CenterEnd) {
                            Toggle(
                                checked = smsAccess,
                                onCheckedChange = { checked ->
                                    smsAccess = checked
                                    onSmsToggle(checked)
                                }
                            )
                        }
                    }

                    // Notifications row with a slightly smaller switch and consistent height
                    val notifInteraction = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = notifInteraction, indication = null) {
                                val new = !notifications
                                notifications = new
                                onNotificationsToggle(new)
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.notifications),
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f),
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(Modifier.width(12.dp))

                        Text(
                            text = "Notifications",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )

                        Box(modifier = Modifier.width(72.dp), contentAlignment = Alignment.CenterEnd) {
                            Toggle(
                                checked = notifications,
                                onCheckedChange = { checked ->
                                    notifications = checked
                                    onNotificationsToggle(checked)
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Support section
            item {
                MenuSection {
                    SectionLabel("Support")
                    supportItems.forEachIndexed { index, item ->
                        MenuRow(
                            iconRes = item.iconRes,
                            title = item.title,
                            subtitle = null,
                            onClick = supportActions[index]
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Log out
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

// Reusable components

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
            val phoneText = phone.takeIf { it.isNotBlank() } ?: "+91 98XXXXXXXX"
            Text(
                text = phoneText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
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
    // Accept a modifier so callers can control horizontal inset; default keeps close spacing
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(top = 12.dp, start = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun MenuSection(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
private fun MenuRow(
    iconRes: Int,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
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
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
//                    fontWeight = FontWeight.SemiBold,
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
    }
}

@Composable
fun Toggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Scale down the Switch to make it smaller and visually consistent
    Box(modifier = modifier.scale(0.75f)) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        )
    }
}

// Preview
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        SettingsScreen()
    }
}