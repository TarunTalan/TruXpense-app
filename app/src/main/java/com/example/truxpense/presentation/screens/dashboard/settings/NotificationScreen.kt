package com.example.truxpense.presentation.screens.dashboard.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.truxpense.R
import com.example.truxpense.notification.datastore.NotificationSettings
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar

// ─── Model (kept for SettingsViewModel backward-compat) ──────────────────────

data class NotificationPrefs(
    val budgetAlerts: Boolean = true,
    val spendingInsights: Boolean = true,
    val unusualSpending: Boolean = true,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderHour: Int = 21,
    val dailyReminderMinute: Int = 0,
    val billReminders: Boolean = true,
    val weeklyReport: Boolean = false,
    val monthlyReport: Boolean = true,
    val subscriptionAlert: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22,
    val quietHoursStartMinute: Int = 0,
    val quietHoursEndHour: Int = 7,
    val quietHoursEndMinute: Int = 0,
)

private fun formatTime(hour: Int, minute: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val h = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }
    return "%d:%02d %s".format(h, minute, period)
}

// ─── Screen (wired to NotificationSettingsViewModel) ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit = {},
    // legacy params kept so HomeScreen.kt call-site still compiles
    prefs: NotificationPrefs = NotificationPrefs(),
    onPrefsChanged: (NotificationPrefs) -> Unit = {},
    vm: NotificationSettingsViewModel = hiltViewModel(),
) {
    val settings by vm.settings.collectAsStateWithLifecycle()

    // Time picker state
    var activeTimePicker by remember { mutableStateOf<String?>(null) }
    val timePickerState = remember(activeTimePicker, settings) {
        when (activeTimePicker) {
            "reminder"    -> TimePickerState(settings.dailyReminderHour, settings.dailyReminderMinute, false)
            else          -> null
        }
    }

    if (activeTimePicker != null && timePickerState != null) {
        TimePickerDialog(
            title = "Daily Reminder Time",
            state = timePickerState,
            onDismiss = { activeTimePicker = null },
            onConfirm = {
                vm.setDailyReminderTime(timePickerState.hour, timePickerState.minute)
                activeTimePicker = null
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(headerTitle = "Notifications & Reminders", showBack = true, onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Spending Alerts ────────────────────────────────────────────
            SettingsSectionCard(label = "Spending Alerts") {

                // Budget Alerts toggle
                ToggleSettingRow(
                    iconRes = R.drawable.alert,
                    title = "Budget Alerts",
                    subtitle = "Notify when you're close to a budget limit",
                    checked = settings.budgetThresholdEnabled,
                    onCheckedChange = { vm.setBudgetThresholdEnabled(it) }
                )

                // Budget threshold sub-options (shown when Budget Alerts enabled)
                AnimatedVisibility(
                    visible = settings.budgetThresholdEnabled,
                    enter = expandVertically(), exit = shrinkVertically()
                ) {
                    Column {
                        SectionDivider()
                        BudgetAlertThresholdRow(settings = settings, vm = vm)
                    }
                }

                SectionDivider()

                // Spending Insights
                ToggleSettingRow(
                    iconRes = R.drawable.splash_screen_icon,
                    title = "Spending Insights",
                    subtitle = "Weekly summaries and spending patterns",
                    checked = settings.spendingInsightsEnabled,
                    onCheckedChange = { vm.setSpendingInsightsEnabled(it) }
                )

                SectionDivider()

                // Unusual Spending
                ToggleSettingRow(
                    iconRes = R.drawable.money,
                    title = "Unusual Spending",
                    subtitle = "Alert when a transaction looks out of the ordinary",
                    checked = settings.unusualSpendingEnabled,
                    onCheckedChange = { vm.setUnusualSpendingEnabled(it) }
                )
            }

            // ── Daily Expense Reminder ─────────────────────────────────────
            SettingsSectionCard(label = "Daily Expense Reminder") {
                ToggleSettingRow(
                    iconRes = R.drawable.alarm,
                    title = "Daily Reminder",
                    subtitle = "Get nudged to log your expenses every day",
                    checked = settings.dailyReminderEnabled,
                    onCheckedChange = { vm.setDailyReminderEnabled(it) }
                )
                AnimatedVisibility(
                    visible = settings.dailyReminderEnabled,
                    enter = expandVertically(), exit = shrinkVertically()
                ) {
                    Column {
                        SectionDivider()
                        ReminderTimeRow(
                            hour = settings.dailyReminderHour,
                            minute = settings.dailyReminderMinute,
                            onEditTime = { activeTimePicker = "reminder" }
                        )
                    }
                }
            }

            Text(
                text = "Notification preferences are saved automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

// ─── Budget Alert threshold sub-row ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetAlertThresholdRow(
    settings: NotificationSettings,
    vm: NotificationSettingsViewModel,
) {
    val focusManager = LocalFocusManager.current
    val useCustom = settings.budgetAlertCustomLimitEnabled

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        // ── Section label ─────────────────────────────────────────────────
        Text(
            text = "Alert trigger",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ── Segmented mode picker ─────────────────────────────────────────
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !useCustom,
                onClick = { vm.setBudgetAlertCustomLimitEnabled(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                label = { Text("% of budget", style = MaterialTheme.typography.labelMedium) },
            )
            SegmentedButton(
                selected = useCustom,
                onClick = { vm.setBudgetAlertCustomLimitEnabled(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = { Text("Fixed amount (₹)", style = MaterialTheme.typography.labelMedium) },
            )
        }

        // ── Mode-specific control ─────────────────────────────────────────
        if (!useCustom) {
            // Percentage slider
            val sliderValue = settings.thresholdPercent.toFloat()
            var sliderDisplay by remember(settings.thresholdPercent) {
                mutableIntStateOf(settings.thresholdPercent)
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Alert when ${sliderDisplay}% of budget is used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 0.dp,
                    ) {
                        Text(
                            text = "$sliderDisplay%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                Slider(
                    value = sliderValue,
                    onValueChange = { sliderDisplay = it.toInt() },
                    onValueChangeFinished = { vm.setThresholdPercent(sliderDisplay) },
                    valueRange = 50f..100f,
                    steps = 9,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("50%", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("100%", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

        } else {
            // Fixed amount input
            var customText by remember(settings.budgetAlertCustomLimit) {
                mutableStateOf(settings.budgetAlertCustomLimit.toString())
            }
            val isError = customText.isNotBlank() && (customText.toIntOrNull() ?: 0) < 1

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = customText,
                    onValueChange = { v ->
                        if (v.length <= 7 && v.all { it.isDigit() }) customText = v
                    },
                    label = { Text("Remaining budget threshold") },
                    leadingIcon = {
                        Text(
                            "₹",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    },
                    placeholder = { Text("e.g. 1000") },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Please enter a valid amount greater than ₹0",
                                color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        val amount = customText.toIntOrNull()
                        if (amount != null && amount >= 1) vm.setBudgetAlertCustomLimit(amount)
                    }),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                    ),
                )

                val displayAmt = customText.toIntOrNull() ?: settings.budgetAlertCustomLimit
                val fmtAmt = if (displayAmt >= 1000)
                    "₹${"%.1f".format(displayAmt / 1000.0)}K"
                else "₹$displayAmt"

                if (!isError && customText.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.alert),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "Alert fires when remaining budget drops below $fmtAmt",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ─── Daily reminder time row ──────────────────────────────────────────────────

@Composable
private fun ReminderTimeRow(hour: Int, minute: Int, onEditTime: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.time_is_money),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Reminder time", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
            Text("Tap the time to change it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        Surface(
            onClick = onEditTime,
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 0.dp,
        ) {
            Text(
                text = formatTime(hour, minute),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

// ─── Material3 Time Picker dialog ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    state: TimePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var useInput by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.secondary) }
        },
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
                TextButton(onClick = { useInput = !useInput }) {
                    Text(if (useInput) "Use dial" else "Use keyboard", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (useInput) TimeInput(state = state) else TimePicker(state = state)
            }
        },
        modifier = Modifier.wrapContentSize()
    )
}

// ─── Section divider ─────────────────────────────────────────────────────────

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

// ─── Preview ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun NotificationsScreenPreview() {
    MaterialTheme {
        // Preview shows the static layout without VM
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { ScreenTopBar(headerTitle = "Notifications & Reminders", showBack = true) }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsSectionCard(label = "Spending Alerts") {
                    ToggleSettingRow(iconRes = R.drawable.alert, title = "Budget Alerts", subtitle = "Notify when close to budget limit", checked = true, onCheckedChange = {})
                    SectionDivider()
                    ToggleSettingRow(iconRes = R.drawable.splash_screen_icon, title = "Spending Insights", subtitle = "Weekly summaries and spending patterns", checked = true, onCheckedChange = {})
                    SectionDivider()
                    ToggleSettingRow(iconRes = R.drawable.money, title = "Unusual Spending", subtitle = "Alert when a transaction looks unusual", checked = false, onCheckedChange = {})
                }
                SettingsSectionCard(label = "Daily Expense Reminder") {
                    ToggleSettingRow(iconRes = R.drawable.alarm, title = "Daily Reminder", subtitle = "Get nudged to log expenses daily", checked = true, onCheckedChange = {})
                    SectionDivider()
                    ReminderTimeRow(hour = 21, minute = 0, onEditTime = {})
                }
            }
        }
    }
}
