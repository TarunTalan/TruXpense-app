package com.example.truxpense.presentation.screens.dashboard.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.utils.clearFocusOnTap

@Composable
fun SecurityScreen(
    biometricsAvailable: Boolean = true,
    biometricsEnabled: Boolean = false,
    appLockEnabled: Boolean = false,
    onBack: () -> Unit = {},
    onBiometricsToggle: (Boolean) -> Unit = {},
    onAppLockToggle: (Boolean) -> Unit = {},
    onChangePassword: (old: String, new: String) -> Unit = { _, _ -> },
) {
    var bioEnabled  by remember { mutableStateOf(biometricsEnabled) }
    var lockEnabled by remember { mutableStateOf(appLockEnabled) }

    // Change password sheet state
    var showPasswordSheet by remember { mutableStateOf(false) }

    if (showPasswordSheet) {
        ChangePasswordSheet(
            onDismiss = { showPasswordSheet = false },
            onConfirm = { old, new ->
                onChangePassword(old, new)
                showPasswordSheet = false
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(headerTitle = "Security", showBack = true, onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
                .clearFocusOnTap(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Login & Password section ────────────────────────────────────
            SettingsSectionCard(label = "Login & Password") {
                ClickableSettingRow(
                    iconRes = R.drawable.security,
                    title = "Change Password",
                    subtitle = "Update your account password",
                    onClick = { showPasswordSheet = true }
                )
            }

            // ── App Lock section ────────────────────────────────────────────
            SettingsSectionCard(label = "App Lock") {
                if (biometricsAvailable) {
                    ToggleSettingRow(
                        iconRes = R.drawable.fingerprint,
                        title = "Biometric Unlock",
                        subtitle = "Use fingerprint or face to unlock TruXpense",
                        checked = bioEnabled,
                        onCheckedChange = {
                            bioEnabled = it
                            onBiometricsToggle(it)
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
                ToggleSettingRow(
                    iconRes = R.drawable.app_lock,
                    title = "App Lock",
                    subtitle = "Require authentication when opening the app",
                    checked = lockEnabled,
                    onCheckedChange = {
                        lockEnabled = it
                        onAppLockToggle(it)
                    }
                )
            }

            // ── Info note ───────────────────────────────────────────────────
            Text(
                text = "Enabling App Lock adds an extra layer of protection so only you can open TruXpense.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

// ── Change-password bottom sheet ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordSheet(
    onDismiss: () -> Unit,
    onConfirm: (old: String, new: String) -> Unit,
) {
    var oldPass  by remember { mutableStateOf("") }
    var newPass  by remember { mutableStateOf("") }
    var confPass by remember { mutableStateOf("") }
    var oldVisible  by remember { mutableStateOf(false) }
    var newVisible  by remember { mutableStateOf(false) }
    var confVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Change Password",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            PasswordField(
                value = oldPass,
                onValueChange = { oldPass = it; error = null },
                label = "Current Password",
                visible = oldVisible,
                onToggleVisible = { oldVisible = !oldVisible }
            )
            PasswordField(
                value = newPass,
                onValueChange = { newPass = it; error = null },
                label = "New Password",
                visible = newVisible,
                onToggleVisible = { newVisible = !newVisible }
            )
            PasswordField(
                value = confPass,
                onValueChange = { confPass = it; error = null },
                label = "Confirm New Password",
                visible = confVisible,
                onToggleVisible = { confVisible = !confVisible }
            )

            if (error != null) {
                Text(
                    text = error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    when {
                        oldPass.isBlank()       -> error = "Enter your current password"
                        newPass.length < 8      -> error = "New password must be at least 8 characters"
                        newPass != confPass     -> error = "Passwords do not match"
                        else                    -> onConfirm(oldPass, newPass)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Update Password")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    painter = painterResource(if (visible) R.drawable.security else R.drawable.security),
                    contentDescription = if (visible) "Hide" else "Show",
                    modifier = Modifier.size(DashboardDimens.iconMd)
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    )
}

// ── Shared row composables ────────────────────────────────────────────────────

@Composable
internal fun SettingsSectionCard(
    label: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp), content = content)
        }
    }
}

@Composable
internal fun ClickableSettingRow(
    iconRes: Int,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(Modifier.apply { }) // ripple via clickable below
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(DashboardDimens.iconMd)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
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
        Icon(
            painter = painterResource(R.drawable.right_arrow),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(DashboardDimens.iconMd)
        )
    }

}

@Composable
internal fun ToggleSettingRow(
    iconRes: Int,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(DashboardDimens.iconMd)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
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
        Toggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// Preview for SecurityScreen
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SecurityScreenPreview() {
    MaterialTheme {
        SecurityScreen(biometricsAvailable = true, biometricsEnabled = true, appLockEnabled = false)
    }
}
