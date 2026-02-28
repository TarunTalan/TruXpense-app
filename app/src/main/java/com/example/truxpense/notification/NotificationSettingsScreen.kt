package com.example.truxpense.notification

import android.os.Build
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.screens.dashboard.theme.DashboardDimens

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    vm: NotificationSettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val settings by vm.settings.collectAsState()
    val context  = LocalContext.current

    // Android 13+ POST_NOTIFICATIONS runtime permission
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDenied = !granted
    }

    // Ask for POST_NOTIFICATIONS when screen opens (Android 13+)
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
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

            // ── Permission banner ─────────────────────────────────────────────
            if (permissionDenied) {
                item { PermissionBanner() }
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
private fun PermissionBanner() {
    Surface(
        shape  = RoundedCornerShape(DashboardDimens.cornerCard),
        color  = MaterialTheme.colorScheme.errorContainer,
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
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp),
            )
            Column {
                Text(
                    text  = "Notification permission required",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text  = "Grant the permission in Settings → Apps → Truxpense → Notifications.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                )
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
            dailyReminderEnabled   = true,
            dailyReminderHour      = 21,
            dailyReminderMinute    = 0,
            budgetThresholdEnabled = true,
            thresholdPercent       = 90,
            monthlyResetEnabled    = true,
            fcmToken               = "eXaMpLeToKeN123…",
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