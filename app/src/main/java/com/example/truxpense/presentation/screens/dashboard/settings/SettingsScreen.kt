package com.example.truxpense.presentation.screens.dashboard.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import android.graphics.Color as AndroidColor

// ─── Model ────────────────────────────────────────────────────────────────────

data class SettingsMenuItem(
    val iconRes: Int,
    val title: String,
    val subtitle: String? = null,
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    username: String = "Tamanna",
    phone: String = "+91 98XXXXXXXX",
    onPersonalInfo: () -> Unit = {},
    onLinkedAccounts: () -> Unit = {},
    onSecurity: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onHelp: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    onTerms: () -> Unit = {},
    onAbout: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
) {
    // Account section rows
    val accountItems = listOf(
        SettingsMenuItem(R.drawable.linked_accounts, "Linked accounts", "Bank / SMS source status"),
        SettingsMenuItem(R.drawable.security_, "Security", "Password and login method"),
    )
    val accountActions = listOf(onLinkedAccounts, onSecurity)

    // Preferences section rows
    val prefItems = listOf(
        SettingsMenuItem(R.drawable.notifications, "Notifications & Reminders", "Alerts, reports, quiet hours"),
    )
    val prefActions = listOf(onNotifications)

    // Support section rows
    val supportItems = listOf(
        SettingsMenuItem(R.drawable.help, "Help & Support"),
        SettingsMenuItem(R.drawable.privacy_policy, "Privacy policy"),
        SettingsMenuItem(R.drawable.description, "Terms of service"),
        SettingsMenuItem(R.drawable.folder_icon, "About TruXpense"),
    )
    val supportActions = listOf(onHelp, onPrivacyPolicy, onTerms, onAbout)

    // Dialog state for logout confirmation
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(headerTitle = "Settings", showBack = false) }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
        ) {

            // ── User header card ─────────────────────────────────────────
            item {
                UserHeader(
                    username = username,
                    phone = phone,
                    modifier = Modifier.padding(vertical = 16.dp),
                    onEditProfile = onPersonalInfo
                )
            }

            // ── Account ──────────────────────────────────────────────────
            item {
                MenuSection(label = "Account") {
                    accountItems.forEachIndexed { i, item ->
                        MenuRow(
                            iconRes = item.iconRes,
                            title = item.title,
                            subtitle = item.subtitle,
                            onClick = accountActions[i]
                        )
                        if (i < accountItems.lastIndex) SectionDivider()
                    }
                    SectionDivider()
                    // Destructive: Delete account
                    MenuRow(
                        iconRes = R.drawable.delete,
                        title = "Delete account",
                        subtitle = "Permanently remove your account and data",
                        onClick = onDeleteAccount,
                        titleColor = MaterialTheme.colorScheme.error,
                        iconTint = MaterialTheme.colorScheme.error,
                        subtitleColor = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Preferences ───────────────────────────────────────────────
            item {
                MenuSection(label = "Preferences") {
                    prefItems.forEachIndexed { i, item ->
                        MenuRow(
                            iconRes = item.iconRes,
                            title = item.title,
                            subtitle = item.subtitle,
                            onClick = prefActions[i]
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Support ───────────────────────────────────────────────────
            item {
                MenuSection(label = "Support") {
                    supportItems.forEachIndexed { i, item ->
                        MenuRow(
                            iconRes = item.iconRes,
                            title = item.title,
                            subtitle = item.subtitle,
                            onClick = supportActions[i]
                        )
                        if (i < supportItems.lastIndex) SectionDivider()
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
                    modifier = Modifier.clickable(onClick = { showLogoutDialog = true })
                )
            }
        }

        // Confirmation dialog shown when user taps Log out
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text(text = "Log out") },
                text = { Text(text = "Are you sure you want to log out?") },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }) {
                        Text(text = "Log out", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text(text = "Cancel")
                    }
                })
        }
    }
}

// ─── Private composables ─────────────────────────────────────────────────────

@Composable
private fun UserHeader(
    username: String,
    phone: String,
    modifier: Modifier = Modifier,
    onEditProfile: () -> Unit = {},
) {
    val avatarBgColor = remember(username) {
        val hue = (username.hashCode() and 0xFFFF) % 360f
        Color(AndroidColor.HSVToColor(floatArrayOf(hue, 0.55f, 0.85f)))
    }
    val avatarContentColor = if (avatarBgColor.luminance() < 0.5f) Color.White else Color.Black

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(72.dp).background(color = avatarBgColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val initial = username.trim().let {
                    if (it.isNotEmpty()) it[0].uppercaseChar().toString() else "?"
                }
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = avatarContentColor
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.height(4.dp))
            Text(text = phone.takeIf { it.isNotBlank() } ?: "+91 98XXXXXXXX",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth().height(48.dp).clip(MaterialTheme.shapes.medium)
                    .border((1.5).dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text = "Edit profile")
            }
        }
    }
}

@Composable
private fun MenuSection(
    label: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(0.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp), content = content)
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    )
}

@Composable
private fun MenuRow(
    iconRes: Int,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    titleColor: Color? = null,
    subtitleColor: Color? = null,
    iconTint: Color? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = iconTint ?: MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = titleColor ?: MaterialTheme.colorScheme.tertiary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor ?: MaterialTheme.colorScheme.secondary
                )
            }
        }
        // Chevron — only on non-destructive rows
        if (titleColor == null) {
            Icon(
                painter = painterResource(R.drawable.right_arrow),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Exported for use from SecurityScreen / NotificationsScreen
@Composable
fun Toggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.scale(0.75f)) {
        Switch(
            checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    MaterialTheme { SettingsScreen() }
}