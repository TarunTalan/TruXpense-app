package com.example.truxpense.presentation.screens.dashboard.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar

// ─── Model ────────────────────────────────────────────────────────────────────

data class NotificationPrefs(
    // Spending Alerts
    val budgetAlerts: Boolean = true,
    val spendingInsights: Boolean = true,
    val unusualSpending: Boolean = true,
    // Daily Expense Reminder
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderHour: Int = 21,   // 9 PM default
    val dailyReminderMinute: Int = 0,
    // Reminders
    val billReminders: Boolean = true,
    val weeklyReport: Boolean = false,
    val monthlyReport: Boolean = true,
    val subscriptionAlert: Boolean = true,
    // Quiet Hours
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22,
    val quietHoursStartMinute: Int = 0,
    val quietHoursEndHour: Int = 7,
    val quietHoursEndMinute: Int = 0,
)

/** Formats hour + minute into a readable 12-hour string — e.g. "9:00 PM" */
private fun formatTime(hour: Int, minute: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "%d:%02d %s".format(h, minute, period)
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    prefs: NotificationPrefs = NotificationPrefs(),
    onBack: () -> Unit = {},
    onPrefsChanged: (NotificationPrefs) -> Unit = {},
) {
    var state by remember { mutableStateOf(prefs) }

    fun update(new: NotificationPrefs) {
        state = new
        onPrefsChanged(new)
    }

    // Tracks which time field the picker is editing: null | "reminder" | "quiet_start" | "quiet_end"
    var activeTimePicker by remember { mutableStateOf<String?>(null) }

    // Recreate TimePickerState whenever a different field opens
    val timePickerState = remember(activeTimePicker) {
        when (activeTimePicker) {
            "reminder" -> TimePickerState(
                initialHour = state.dailyReminderHour, initialMinute = state.dailyReminderMinute, is24Hour = false
            )

            "quiet_start" -> TimePickerState(
                initialHour = state.quietHoursStartHour, initialMinute = state.quietHoursStartMinute, is24Hour = false
            )

            "quiet_end" -> TimePickerState(
                initialHour = state.quietHoursEndHour, initialMinute = state.quietHoursEndMinute, is24Hour = false
            )

            else -> null
        }
    }

    // Time picker dialog
    if (activeTimePicker != null && timePickerState != null) {
        val dialogTitle = when (activeTimePicker) {
            "reminder" -> "Daily Reminder Time"
            "quiet_start" -> "Quiet Hours Start"
            else -> "Quiet Hours End"
        }
        TimePickerDialog(
            title = dialogTitle,
            state = timePickerState,
            onDismiss = { activeTimePicker = null },
            onConfirm = {
                when (activeTimePicker) {
                    "reminder" -> update(
                        state.copy(
                            dailyReminderHour = timePickerState.hour,
                            dailyReminderMinute = timePickerState.minute,
                        )
                    )

                    "quiet_start" -> update(
                        state.copy(
                            quietHoursStartHour = timePickerState.hour,
                            quietHoursStartMinute = timePickerState.minute,
                        )
                    )

                    "quiet_end" -> update(
                        state.copy(
                            quietHoursEndHour = timePickerState.hour,
                            quietHoursEndMinute = timePickerState.minute,
                        )
                    )
                }
                activeTimePicker = null
            })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, topBar = {
            ScreenTopBar(
                headerTitle = "Notifications & Reminders", showBack = true, onBack = onBack
            )
        }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Spending Alerts ────────────────────────────────────────────
            SettingsSectionCard(label = "Spending Alerts") {
                ToggleSettingRow(
                    iconRes = R.drawable.alert,
                    title = "Budget Alerts",
                    subtitle = "Notify when you're close to a budget limit",
                    checked = state.budgetAlerts,
                    onCheckedChange = { update(state.copy(budgetAlerts = it)) })

                SectionDivider()
                ToggleSettingRow(
                    iconRes = R.drawable.splash_screen_icon,
                    title = "Spending Insights",
                    subtitle = "Weekly summaries and spending patterns",
                    checked = state.spendingInsights,
                    onCheckedChange = { update(state.copy(spendingInsights = it)) })
                SectionDivider()
                ToggleSettingRow(
                    iconRes = R.drawable.money,
                    title = "Unusual Spending",
                    subtitle = "Alert when a transaction looks out of the ordinary",
                    checked = state.unusualSpending,
                    onCheckedChange = { update(state.copy(unusualSpending = it)) })
            }

            // ── Daily Expense Reminder ─────────────────────────────────────
            SettingsSectionCard(label = "Daily Expense Reminder") {
                ToggleSettingRow(
                    iconRes = R.drawable.alarm,
                    title = "Daily Reminder",
                    subtitle = "Get nudged to log your expenses every day",
                    checked = state.dailyReminderEnabled,
                    onCheckedChange = { update(state.copy(dailyReminderEnabled = it)) })
                AnimatedVisibility(
                    visible = state.dailyReminderEnabled,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column {
                        SectionDivider()
                        ReminderTimeRow(
                            hour = state.dailyReminderHour,
                            minute = state.dailyReminderMinute,
                            onEditTime = { activeTimePicker = "reminder" })
                    }
                }
            }

            // ── Reminders ──────────────────────────────────────────────────
            SettingsSectionCard(label = "Reminders") {
                ToggleSettingRow(
                    iconRes = R.drawable.bill,
                    title = "Bill Reminders",
                    subtitle = "Credit card dues, rent, subscriptions",
                    checked = state.billReminders,
                    onCheckedChange = { update(state.copy(billReminders = it)) })
                SectionDivider()
                ToggleSettingRow(
                    iconRes = R.drawable.report,
                    title = "Weekly Report",
                    subtitle = "Receive a spending summary every Sunday",
                    checked = state.weeklyReport,
                    onCheckedChange = { update(state.copy(weeklyReport = it)) })
                SectionDivider()
                ToggleSettingRow(
                    iconRes = R.drawable.report_ic,
                    title = "Monthly Report",
                    subtitle = "Full month-end spending breakdown",
                    checked = state.monthlyReport,
                    onCheckedChange = { update(state.copy(monthlyReport = it)) })
                SectionDivider()
                ToggleSettingRow(
                    iconRes = R.drawable.notifications,
                    title = "Subscription Alerts",
                    subtitle = "Remind about recurring charges you may have forgotten",
                    checked = state.subscriptionAlert,
                    onCheckedChange = { update(state.copy(subscriptionAlert = it)) })
            }

            // ── Quiet Hours ────────────────────────────────────────────────
            SettingsSectionCard(label = "Quiet Hours") {
                ToggleSettingRow(
                    iconRes = R.drawable.mute_notification,
                    title = "Enable Quiet Hours",
                    subtitle = "Suppress all notifications during selected times",
                    checked = state.quietHoursEnabled,
                    onCheckedChange = { update(state.copy(quietHoursEnabled = it)) })
                AnimatedVisibility(
                    visible = state.quietHoursEnabled,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column {
                        SectionDivider()
                        QuietHoursTimeRow(
                            label = "Start",
                            hour = state.quietHoursStartHour,
                            minute = state.quietHoursStartMinute,
                            onEditTime = { activeTimePicker = "quiet_start" })
                        SectionDivider()
                        QuietHoursTimeRow(
                            label = "End",
                            hour = state.quietHoursEndHour,
                            minute = state.quietHoursEndMinute,
                            onEditTime = { activeTimePicker = "quiet_end" })
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

// ─── Daily reminder time row ──────────────────────────────────────────────────

@Composable
private fun ReminderTimeRow(
    hour: Int,
    minute: Int,
    onEditTime: () -> Unit,
) {
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
            Text(
                text = "Reminder time",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = "Tap the time to change it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        // Tappable time chip
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

// ─── Quiet hours time row ─────────────────────────────────────────────────────

@Composable
private fun QuietHoursTimeRow(
    label: String,
    hour: Int,
    minute: Int,
    onEditTime: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            // indent to align with other indented rows under the toggle
            .padding(start = 50.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "$label time",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary
        )
        Surface(
            onClick = onEditTime,
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 0.dp,
        ) {
            Text(
                text = formatTime(hour, minute),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
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
    // Lets the user toggle between clock dial and keyboard input
    var useInput by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss, confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }, dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.secondary)
            }
        }, title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                // Toggle dial ↔ keyboard
                TextButton(onClick = { useInput = !useInput }) {
                    Text(
                        text = if (useInput) "Use dial" else "Use keyboard",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }, text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (useInput) {
                    TimeInput(state = state)
                } else {
                    TimePicker(state = state)
                }
            }
        }, modifier = Modifier.wrapContentSize()
    )
}

// ─── Section divider (private to this file) ───────────────────────────────────

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun NotificationsScreenPreview() {
    val sample = NotificationPrefs(
        budgetAlerts = true,
        spendingInsights = true,
        unusualSpending = false,
        dailyReminderEnabled = true,
        dailyReminderHour = 21,
        dailyReminderMinute = 0,
        billReminders = true,
        weeklyReport = false,
        monthlyReport = true,
        subscriptionAlert = true,
        quietHoursEnabled = true,
        quietHoursStartHour = 22,
        quietHoursStartMinute = 0,
        quietHoursEndHour = 7,
        quietHoursEndMinute = 0,
    )
    MaterialTheme {
        NotificationsScreen(prefs = sample, onPrefsChanged = {})
    }
}
