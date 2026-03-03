package com.example.truxpense.presentation.screens.dashboard.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.notification.datastore.NotificationPreferences
import com.example.truxpense.notification.scheduler.NotificationScheduler
import com.example.truxpense.notification.datastore.NotificationSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val prefs: NotificationPreferences,
    private val scheduler: NotificationScheduler,
) : ViewModel() {

    // ── Live settings snapshot ────────────────────────────────────────────────

    val settings: StateFlow<NotificationSettings> = prefs.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationSettings(),
        )

    // ── Daily reminder ────────────────────────────────────────────────────────

    fun setDailyReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setDailyReminderEnabled(enabled)
            val s = settings.value
            if (enabled) scheduler.scheduleDailyReminder(s.dailyReminderHour, s.dailyReminderMinute)
            else         scheduler.cancelDailyReminder()
        }
    }

    fun setDailyReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefs.setDailyReminderTime(hour, minute)
            if (settings.value.dailyReminderEnabled)
                scheduler.scheduleDailyReminder(hour, minute)
        }
    }

    // ── Budget threshold ──────────────────────────────────────────────────────

    fun setBudgetThresholdEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setBudgetThresholdEnabled(enabled)
            if (enabled) scheduler.scheduleBudgetThresholdCheck()
            else         scheduler.cancelBudgetThresholdCheck()
        }
    }

    fun setThresholdPercent(percent: Int) {
        viewModelScope.launch {
            prefs.setThresholdPercent(percent)
            prefs.clearNotifiedBudgets()
        }
    }

    fun setBudgetAlertCustomLimitEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setBudgetAlertCustomLimitEnabled(enabled)
            prefs.clearNotifiedBudgets()
        }
    }

    fun setBudgetAlertCustomLimit(amount: Int) {
        viewModelScope.launch {
            prefs.setBudgetAlertCustomLimit(amount)
            prefs.clearNotifiedBudgets()
        }
    }

    // ── Spending Insights & Unusual Spending ──────────────────────────────────

    fun setSpendingInsightsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setSpendingInsightsEnabled(enabled) }
    }

    fun setUnusualSpendingEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setUnusualSpendingEnabled(enabled) }
    }

    // ── Monthly reset ─────────────────────────────────────────────────────────

    fun setMonthlyResetEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setMonthlyResetEnabled(enabled)
            if (enabled) scheduler.scheduleMonthlyResetCheck()
            else         scheduler.cancelMonthlyResetCheck()
        }
    }
}