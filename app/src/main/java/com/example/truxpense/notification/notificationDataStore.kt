package com.example.truxpense.notification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// ── DataStore instance — one per app ──────────────────────────────────────────

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_preferences"
)

// ── Settings snapshot ─────────────────────────────────────────────────────────

/**
 * Immutable snapshot of the user's notification preferences.
 * Exposed as a [Flow] from [NotificationPreferences.settings].
 */
data class NotificationSettings(
    /** Whether the daily "log your expenses" reminder is enabled. */
    val dailyReminderEnabled: Boolean = false,
    /** Hour (0–23) at which the daily reminder fires. Default 21 = 9 PM. */
    val dailyReminderHour: Int = 21,
    /** Minute (0–59) for the daily reminder. */
    val dailyReminderMinute: Int = 0,

    /** Whether budget threshold alerts are enabled. */
    val budgetThresholdEnabled: Boolean = true,
    /**
     * Percentage (50–100) at which an alert fires.
     * Default 90 means "fire when 90 % of budget is spent".
     */
    val thresholdPercent: Int = 90,

    /** Whether the end-of-month budget review reminder is enabled. */
    val monthlyResetEnabled: Boolean = true,

    /**
     * FCM registration token for this device.
     * Blank until the first FCM token arrives via [TruxpenseFirebaseMessagingService].
     * Send this to your backend so it can target push notifications at this device.
     */
    val fcmToken: String = "",

    /**
     * Tracks which budget categories have already been notified this month so
     * repeated threshold checks don't spam the user.
     *
     * Format: "CategoryName:YYYY-MM" — e.g. "Food:2026-03"
     */
    val notifiedBudgets: Set<String> = emptySet(),
)

// ── Repository ────────────────────────────────────────────────────────────────

@Singleton
class NotificationPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    // ── Keys ──────────────────────────────────────────────────────────────────

    private object Keys {
        val DAILY_ENABLED         = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_HOUR            = intPreferencesKey("daily_reminder_hour")
        val DAILY_MINUTE          = intPreferencesKey("daily_reminder_minute")
        val THRESHOLD_ENABLED     = booleanPreferencesKey("budget_threshold_enabled")
        val THRESHOLD_PERCENT     = intPreferencesKey("threshold_percent")
        val MONTHLY_RESET_ENABLED = booleanPreferencesKey("monthly_reset_enabled")
        val FCM_TOKEN             = stringPreferencesKey("fcm_token")
        val NOTIFIED_BUDGETS      = stringSetPreferencesKey("notified_budgets")
    }

    // ── Observing ─────────────────────────────────────────────────────────────

    /** Live flow of the full settings snapshot. Emits defaults on first read. */
    val settings: Flow<NotificationSettings> =
        context.notificationDataStore.data
            .catch { e ->
                // Corrupt DataStore → emit defaults rather than crash
                if (e is IOException) emit(emptyPreferences()) else throw e
            }
            .map { prefs ->
                NotificationSettings(
                    dailyReminderEnabled  = prefs[Keys.DAILY_ENABLED]         ?: false,
                    dailyReminderHour     = prefs[Keys.DAILY_HOUR]            ?: 21,
                    dailyReminderMinute   = prefs[Keys.DAILY_MINUTE]          ?: 0,
                    budgetThresholdEnabled= prefs[Keys.THRESHOLD_ENABLED]     ?: true,
                    thresholdPercent      = prefs[Keys.THRESHOLD_PERCENT]     ?: 90,
                    monthlyResetEnabled   = prefs[Keys.MONTHLY_RESET_ENABLED] ?: true,
                    fcmToken              = prefs[Keys.FCM_TOKEN]             ?: "",
                    notifiedBudgets       = prefs[Keys.NOTIFIED_BUDGETS]      ?: emptySet(),
                )
            }

    // ── Writing ───────────────────────────────────────────────────────────────

    suspend fun setDailyReminderEnabled(enabled: Boolean) =
        context.notificationDataStore.edit { it[Keys.DAILY_ENABLED] = enabled }

    suspend fun setDailyReminderTime(hour: Int, minute: Int) =
        context.notificationDataStore.edit {
            it[Keys.DAILY_HOUR]   = hour
            it[Keys.DAILY_MINUTE] = minute
        }

    suspend fun setBudgetThresholdEnabled(enabled: Boolean) =
        context.notificationDataStore.edit { it[Keys.THRESHOLD_ENABLED] = enabled }

    suspend fun setThresholdPercent(percent: Int) =
        context.notificationDataStore.edit { it[Keys.THRESHOLD_PERCENT] = percent.coerceIn(50, 100) }

    suspend fun setMonthlyResetEnabled(enabled: Boolean) =
        context.notificationDataStore.edit { it[Keys.MONTHLY_RESET_ENABLED] = enabled }

    /** Called from [TruxpenseFirebaseMessagingService] when a new token arrives. */
    suspend fun saveFcmToken(token: String) =
        context.notificationDataStore.edit { it[Keys.FCM_TOKEN] = token }

    /**
     * Records that a threshold alert was sent for [category] in the current month
     * so subsequent checks don't re-notify.
     *
     * @param yearMonth  Format "YYYY-MM", e.g. "2026-03"
     */
    suspend fun markBudgetNotified(category: String, yearMonth: String) =
        context.notificationDataStore.edit { prefs ->
            val existing = prefs[Keys.NOTIFIED_BUDGETS] ?: emptySet()
            prefs[Keys.NOTIFIED_BUDGETS] = existing + "$category:$yearMonth"
        }

    /**
     * Called at the start of a new month to clear stale records so alerts
     * fire again for the new month.
     */
    suspend fun clearNotifiedBudgets() =
        context.notificationDataStore.edit { it[Keys.NOTIFIED_BUDGETS] = emptySet() }

    /** One-shot suspend read — use in Workers where Flow collection isn't appropriate. */
    suspend fun getSnapshot(): NotificationSettings {
        var snapshot = NotificationSettings()
        context.notificationDataStore.data.catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }.map { prefs ->
            NotificationSettings(
                dailyReminderEnabled   = prefs[Keys.DAILY_ENABLED]         ?: false,
                dailyReminderHour      = prefs[Keys.DAILY_HOUR]            ?: 21,
                dailyReminderMinute    = prefs[Keys.DAILY_MINUTE]          ?: 0,
                budgetThresholdEnabled = prefs[Keys.THRESHOLD_ENABLED]     ?: true,
                thresholdPercent       = prefs[Keys.THRESHOLD_PERCENT]     ?: 90,
                monthlyResetEnabled    = prefs[Keys.MONTHLY_RESET_ENABLED] ?: true,
                fcmToken               = prefs[Keys.FCM_TOKEN]             ?: "",
                notifiedBudgets        = prefs[Keys.NOTIFIED_BUDGETS]      ?: emptySet(),
            )
        }.collect { snapshot = it; return@collect }
        return snapshot
    }
}