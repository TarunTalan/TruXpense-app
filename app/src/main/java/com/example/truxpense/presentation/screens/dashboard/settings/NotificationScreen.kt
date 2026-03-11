package com.example.truxpense.presentation.screens.dashboard.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.truxpense.R
import com.example.truxpense.notification.datastore.NotificationSettings
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.theme.AppDialogTheme

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
    val h = when {
        hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour
    }
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
            "reminder" -> TimePickerState(settings.dailyReminderHour, settings.dailyReminderMinute, false)
            else -> null
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
            })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                headerTitle = "Notifications & Reminders",
                showBack = true,
                onBack = onBack
            )
        }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Spending Alerts ────────────────────────────────────────────
            SettingsSectionCard(label = "Spending Alerts") {

                // Budget Alerts toggle
                ToggleSettingRow(
                    iconRes = R.drawable.alert,
                    title = "Budget Alerts",
                    subtitle = "Notify when you're close to a budget limit",
                    checked = settings.budgetThresholdEnabled,
                    onCheckedChange = { vm.setBudgetThresholdEnabled(it) })

                // Budget threshold sub-options (shown when Budget Alerts enabled)
                AnimatedVisibility(
                    visible = settings.budgetThresholdEnabled, enter = expandVertically(), exit = shrinkVertically()
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
                    onCheckedChange = { vm.setSpendingInsightsEnabled(it) })

                SectionDivider()

                // Unusual Spending
                ToggleSettingRow(
                    iconRes = R.drawable.money,
                    title = "Unusual Spending",
                    subtitle = "Alert when a transaction looks out of the ordinary",
                    checked = settings.unusualSpendingEnabled,
                    onCheckedChange = { vm.setUnusualSpendingEnabled(it) })
            }

            // ── Daily Expense Reminder ─────────────────────────────────────
            SettingsSectionCard(label = "Daily Expense Reminder") {
                ToggleSettingRow(
                    iconRes = R.drawable.alarm,
                    title = "Daily Reminder",
                    subtitle = "Get nudged to log your expenses every day",
                    checked = settings.dailyReminderEnabled,
                    onCheckedChange = { vm.setDailyReminderEnabled(it) })
                AnimatedVisibility(
                    visible = settings.dailyReminderEnabled, enter = expandVertically(), exit = shrinkVertically()
                ) {
                    Column {
                        SectionDivider()
                        ReminderTimeRow(
                            hour = settings.dailyReminderHour,
                            minute = settings.dailyReminderMinute,
                            onEditTime = { activeTimePicker = "reminder" })
                    }
                }
            }
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
    val colorScheme = MaterialTheme.colorScheme
    val useCustom = settings.budgetAlertCustomLimitEnabled

    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {

        // ── Mode toggle — compact chip pair ───────────────────────────────
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !useCustom,
                onClick = { vm.setBudgetAlertCustomLimitEnabled(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                label = {
                    Text(
                        "% of budget",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (!useCustom) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
            )
            SegmentedButton(
                selected = useCustom,
                onClick = { vm.setBudgetAlertCustomLimitEnabled(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = {
                    Text(
                        "Fixed amount (₹)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (useCustom) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
            )
        }

        // ── Mode-specific control ─────────────────────────────────────────
        AnimatedContent(
            targetState = useCustom,
            transitionSpec = {
                fadeIn(tween(200)).togetherWith(fadeOut(tween(150)))
            },
            label = "thresholdMode",
        ) { isCustom ->
            if (!isCustom) {

                // ── Percentage slider ─────────────────────────────────────
                var sliderDisplay by remember(settings.thresholdPercent) {
                    mutableIntStateOf(settings.thresholdPercent)
                }

                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {

                    // Centred value — clean, no extra text
                    Text(
                        text = "$sliderDisplay%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )

                    Slider(
                        value = sliderDisplay.toFloat(),
                        onValueChange = { sliderDisplay = it.toInt() },
                        onValueChangeFinished = { vm.setThresholdPercent(sliderDisplay) },
                        valueRange = 50f..100f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Min / max range labels
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "50%",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        Text(
                            text = "100%",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }

            } else {

                // ── Fixed amount input ────────────────────────────────────
                var customText by remember(settings.budgetAlertCustomLimit) {
                    mutableStateOf(settings.budgetAlertCustomLimit.toString())
                }
                val isError = customText.isNotBlank() && (customText.toIntOrNull() ?: 0) < 1
                val displayAmt = customText.toIntOrNull() ?: settings.budgetAlertCustomLimit
                val fmtAmt = if (displayAmt >= 1000) "₹${"%.1f".format(displayAmt / 1000.0)}K"
                else "₹$displayAmt"

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    // Styled amount input — matches surfaceContainer card aesthetic
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.surfaceContainer,
                        border = if (isError) BorderStroke(1.dp, colorScheme.error)
                        else BorderStroke(
                            1.dp, colorScheme.outline.copy(alpha = 0.3f)
                        ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // ₹ prefix badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colorScheme.primary.copy(alpha = 0.1f),
                            ) {
                                Text(
                                    text = "₹",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                            BasicTextField(
                                value = customText,
                                onValueChange = { v ->
                                    if (v.length <= 7 && v.all { it.isDigit() }) customText = v
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    color = colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done,
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    focusManager.clearFocus()
                                    val amount = customText.toIntOrNull()
                                    if (amount != null && amount >= 1) vm.setBudgetAlertCustomLimit(amount)
                                }),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    Box {
                                        if (customText.isEmpty()) {
                                            Text(
                                                text = "e.g. 1000",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            )
                                        }
                                        inner()
                                    }
                                },
                            )
                        }
                    }

                    // Inline error
                    AnimatedVisibility(
                        visible = isError,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 4.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.alert),
                                contentDescription = null,
                                tint = colorScheme.error,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = "Enter a valid amount greater than ₹0",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.error,
                            )
                        }
                    }

                    // Preview pill (only when valid input)
                    AnimatedVisibility(
                        visible = !isError && customText.isNotBlank(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        AlertPreviewPill(
                            text = "You'll be alerted when remaining budget drops below $fmtAmt",
                        )
                    }
                }
            }
        }
    }
}

// ── Alert preview pill ────────────────────────────────────────────────────────

@Composable
private fun AlertPreviewPill(text: String) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = colorScheme.primary.copy(alpha = 0.07f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.alert),
                contentDescription = null,
                tint = colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.primary.copy(alpha = 0.8f),
                lineHeight = 18.sp,
            )
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
            painter = painterResource(R.drawable.time_),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Reminder time", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                "Tap the time to change it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
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
    AppDialogTheme {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onConfirm) { Text("OK", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { useInput = !useInput }) {
                        Text(if (useInput) "Use dial" else "Use keyboard", style = MaterialTheme.typography.labelMedium)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (useInput) TimeInput(state = state) else TimePicker(state = state)
                }
            },
            modifier = Modifier.wrapContentSize(),
        )
    }
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
            topBar = { ScreenTopBar(headerTitle = "Notifications & Reminders", showBack = true) }) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsSectionCard(label = "Spending Alerts") {
                    ToggleSettingRow(
                        iconRes = R.drawable.alert,
                        title = "Budget Alerts",
                        subtitle = "Notify when close to budget limit",
                        checked = true,
                        onCheckedChange = {})
                    SectionDivider()
                    ToggleSettingRow(
                        iconRes = R.drawable.splash_screen_icon,
                        title = "Spending Insights",
                        subtitle = "Weekly summaries and spending patterns",
                        checked = true,
                        onCheckedChange = {})
                    SectionDivider()
                    ToggleSettingRow(
                        iconRes = R.drawable.money,
                        title = "Unusual Spending",
                        subtitle = "Alert when a transaction looks unusual",
                        checked = false,
                        onCheckedChange = {})
                }
                SettingsSectionCard(label = "Daily Expense Reminder") {
                    ToggleSettingRow(
                        iconRes = R.drawable.alarm,
                        title = "Daily Reminder",
                        subtitle = "Get nudged to log expenses daily",
                        checked = true,
                        onCheckedChange = {})
                    SectionDivider()
                    ReminderTimeRow(hour = 21, minute = 0, onEditTime = {})
                }
            }
        }
    }
}