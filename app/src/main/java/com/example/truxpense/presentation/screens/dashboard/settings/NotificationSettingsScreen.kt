package com.example.truxpense.presentation.screens.dashboard.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.notification.datastore.NotificationSettings
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.theme.DashboardDimens

// ── Permission state ──────────────────────────────────────────────────────────


private sealed interface NotifPermissionState {
    object Granted          : NotifPermissionState
    object NeedsRuntimeAsk  : NotifPermissionState   // API 33+ only
    object DisabledInSystem : NotifPermissionState   // all API levels
}

private fun resolvePermissionState(context: android.content.Context): NotifPermissionState {
    val manager = NotificationManagerCompat.from(context)
    // First check the coarse "are notifications enabled for this app" flag — works on all APIs
    if (!manager.areNotificationsEnabled()) return NotifPermissionState.DisabledInSystem
    // On Android 13+ also check the runtime permission
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) return NotifPermissionState.NeedsRuntimeAsk
    }
    return NotifPermissionState.Granted
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    vm: NotificationSettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val settings by vm.settings.collectAsState()
    val context  = LocalContext.current

    // Resolve permission state and re-check every time the screen is (re-)composed
    // so it updates correctly after the user returns from system settings.
    var permState by remember { mutableStateOf(resolvePermissionState(context)) }

    // Android 13+ runtime permission launcher
    val runtimePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Re-resolve after the dialog result — covers grant, deny, and "don't ask again"
        permState = resolvePermissionState(context)
        if (!granted) {
            // If denied, re-check whether we should now show "open system settings" banner
            permState = if (!NotificationManagerCompat.from(context).areNotificationsEnabled())
                NotifPermissionState.DisabledInSystem
            else
                NotifPermissionState.NeedsRuntimeAsk
        }
    }

    // Launcher that opens the system app-notification settings page (all API levels)
    val systemSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Re-resolve when user comes back from system settings
        permState = resolvePermissionState(context)
    }

    // On first composition: if permission is missing, proactively request it
    LaunchedEffect(Unit) {
        when (permState) {
            is NotifPermissionState.NeedsRuntimeAsk -> {
                // Android 13+: show the OS runtime dialog automatically
                runtimePermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            is NotifPermissionState.DisabledInSystem -> {
                // Already disabled — banner will guide user, don't auto-open settings
            }
            is NotifPermissionState.Granted -> { /* nothing to do */ }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                headerTitle = "Notifications",
                showBack    = true,
                onBack      = onBack,
            )
        },
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = DashboardDimens.screenPaddingH,
                vertical   = DashboardDimens.spaceLg,
            ),
            verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg),
        ) {

            // ── Permission banner (shown for all non-Granted states) ───────────
            if (permState != NotifPermissionState.Granted) {
                item {
                    PermissionBanner(
                        state = permState,
                        onRequestPermission = {
                            runtimePermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        },
                        onOpenSystemSettings = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            systemSettingsLauncher.launch(intent)
                        },
                    )
                }
            }

            // ── Daily reminder ────────────────────────────────────────────────
            item {
                DailyReminderCard(
                    enabled = settings.dailyReminderEnabled,
                    hour    = settings.dailyReminderHour,
                    minute  = settings.dailyReminderMinute,
                    onToggle    = vm::setDailyReminderEnabled,
                    onTimeChange = vm::setDailyReminderTime,
                )
            }

            // ── Budget threshold ──────────────────────────────────────────────
            item {
                BudgetAlertCard(
                    enabled   = settings.budgetThresholdEnabled,
                    threshold = settings.thresholdPercent,
                    onToggle  = vm::setBudgetThresholdEnabled,
                    onThresholdChange = vm::setThresholdPercent,
                )
            }

            // ── Monthly reset ─────────────────────────────────────────────────
            item {
                MonthlyResetCard(
                    enabled  = settings.monthlyResetEnabled,
                    onToggle = vm::setMonthlyResetEnabled,
                )
            }

            // ── FCM debug token ───────────────────────────────────────────────
            if (settings.fcmToken.isNotBlank()) {
                item { FcmTokenCard(token = settings.fcmToken) }
            }

            item { Spacer(Modifier.height(DashboardDimens.spaceXxl)) }
        }
    }
}

// ── Permission banner ─────────────────────────────────────────────────────────

@Composable
private fun PermissionBanner(
    state: NotifPermissionState,
    onRequestPermission: () -> Unit,
    onOpenSystemSettings: () -> Unit,
) {
    val (message, buttonLabel, onClick) = when (state) {
        is NotifPermissionState.NeedsRuntimeAsk -> Triple(
            "Allow TruXpense to send budget alerts and reminders.",
            "Allow notifications",
            onRequestPermission,
        )
        is NotifPermissionState.DisabledInSystem -> Triple(
            "Notifications are turned off for this app. " +
                    "Enable them in system settings to receive budget alerts.",
            "Open settings",
            onOpenSystemSettings,
        )
        is NotifPermissionState.Granted -> Triple("", "", {})
    }

    Surface(
        shape    = RoundedCornerShape(DashboardDimens.cornerCard),
        color    = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(DashboardDimens.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "Notifications off",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(DashboardDimens.spaceMd))
                OutlinedButton(
                    onClick = onClick,
                    colors  = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    border  = ButtonDefaults.outlinedButtonBorder(enabled = true),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                ) {
                    Text(buttonLabel, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ── Daily reminder card ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyReminderCard(
    enabled: Boolean,
    hour: Int,
    minute: Int,
    onToggle: (Boolean) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour   = hour,
        initialMinute = minute,
        is24Hour      = false,
    )

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss  = { showTimePicker = false },
            onConfirm  = {
                onTimeChange(timePickerState.hour, timePickerState.minute)
                showTimePicker = false
            },
        ) {
            TimePicker(state = timePickerState)
        }
    }

    NotificationCard(
        icon    = "⏰",
        title   = "Daily Expense Reminder",
        subtitle = "Reminds you to log expenses every day",
        enabled = enabled,
        onToggle = onToggle,
    ) {
        AnimatedVisibility(
            visible = enabled,
            enter   = expandVertically(),
            exit    = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = DashboardDimens.spaceMd),
                verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceXs),
            ) {
                Text(
                    text  = "Reminder time",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DashboardDimens.cornerToggleInner))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { showTimePicker = true }
                        .padding(
                            horizontal = DashboardDimens.spaceMdL,
                            vertical   = DashboardDimens.spaceMd,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text  = formatTime(hour, minute),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text  = "Change",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

// ── Budget alert card ─────────────────────────────────────────────────────────

@Composable
private fun BudgetAlertCard(
    enabled: Boolean,
    threshold: Int,
    onToggle: (Boolean) -> Unit,
    onThresholdChange: (Int) -> Unit,
) {
    // Chips: 75 / 80 / 90 / 100 %
    val thresholdOptions = listOf(75, 80, 90, 100)

    NotificationCard(
        icon     = "⚠️",
        title    = "Budget Alerts",
        subtitle = "Alert when a budget category is nearly exhausted",
        enabled  = enabled,
        onToggle = onToggle,
    ) {
        AnimatedVisibility(
            visible = enabled,
            enter   = expandVertically(),
            exit    = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = DashboardDimens.spaceMd),
                verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceXs),
            ) {
                Text(
                    text  = "Alert threshold",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceSm),
                ) {
                    thresholdOptions.forEach { pct ->
                        val selected = pct == threshold
                        FilterChip(
                            selected  = selected,
                            onClick   = { onThresholdChange(pct) },
                            label = {
                                Text(
                                    text  = "$pct%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor     = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
                Text(
                    text  = "You'll be notified when spending hits $threshold% of a budget limit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Monthly reset card ────────────────────────────────────────────────────────

@Composable
private fun MonthlyResetCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    NotificationCard(
        icon     = "📅",
        title    = "Monthly Budget Review",
        subtitle = "Prompt to review and adjust budgets at the start of each month",
        enabled  = enabled,
        onToggle = onToggle,
    )
}

// ── FCM token debug card ──────────────────────────────────────────────────────

@Composable
private fun FcmTokenCard(token: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPadding)) {
            Text(
                text  = "Device Token (debug)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = DashboardDimens.spaceXs),
            )
            Text(
                text  = token,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
            )
        }
    }
}

// ── Reusable notification card shell ─────────────────────────────────────────

@Composable
private fun NotificationCard(
    icon: String,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    expandedContent: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPadding)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // ── Icon + text ───────────────────────────────────────────────
                Row(
                    modifier          = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = icon, fontSize = 22.sp)
                    Column {
                        Text(
                            text  = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text  = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // ── Toggle ────────────────────────────────────────────────────
                Switch(
                    checked         = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor  = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor  = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
            }

            // Expanded options (only some cards have these)
            expandedContent?.invoke()
        }
    }
}

// ── Time picker dialog wrapper ────────────────────────────────────────────────

@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Set reminder time", style = MaterialTheme.typography.titleMedium) },
        text  = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatTime(hour: Int, minute: Int): String {
    val ampm   = if (hour < 12) "AM" else "PM"
    val h      = if (hour % 12 == 0) 12 else hour % 12
    return "$h:${minute.toString().padStart(2, '0')} $ampm"
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun NotificationSettingsScreenPreview() {
    MaterialTheme {
        // Stateless preview without VM
        val settings = NotificationSettings(
            dailyReminderEnabled = true,
            dailyReminderHour = 21,
            dailyReminderMinute = 0,
            budgetThresholdEnabled = true,
            thresholdPercent = 90,
            monthlyResetEnabled = true,
            fcmToken = "eXaMpLeToKeN123…",
        )

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { ScreenTopBar(headerTitle = "Notifications", showBack = true, onBack = {}) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    horizontal = DashboardDimens.screenPaddingH,
                    vertical   = DashboardDimens.spaceLg,
                ),
                verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg),
            ) {
                item {
                    DailyReminderCard(
                        enabled      = settings.dailyReminderEnabled,
                        hour         = settings.dailyReminderHour,
                        minute       = settings.dailyReminderMinute,
                        onToggle     = {},
                        onTimeChange = { _, _ -> },
                    )
                }
                item {
                    BudgetAlertCard(
                        enabled           = settings.budgetThresholdEnabled,
                        threshold         = settings.thresholdPercent,
                        onToggle          = {},
                        onThresholdChange = {},
                    )
                }
                item {
                    MonthlyResetCard(enabled = settings.monthlyResetEnabled, onToggle = {})
                }
                item { FcmTokenCard(token = settings.fcmToken) }
            }
        }
    }
}