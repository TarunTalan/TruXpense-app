package com.example.truxpense.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Central registry for every notification channel used by Truxpense.
 *
 * Android O+ requires channels. Call [registerAll] once at app startup
 * (e.g. inside Application.onCreate). It is safe to call multiple times —
 * existing channels are not modified.
 */
object NotificationChannels {

    // ── IDs ───────────────────────────────────────────────────────────────────

    /** Daily "don't forget to log your expenses" reminder. */
    const val DAILY_REMINDER = "truxpense_daily_reminder"

    /** Alert fired when a budget category hits the configured threshold (e.g. 90 %). */
    const val BUDGET_ALERT = "truxpense_budget_alert"

    /** End-of-month prompt to review and reset budget limits. */
    const val MONTHLY_RESET = "truxpense_monthly_reset"

    /** Catch-all channel for general FCM push messages from the server. */
    const val GENERAL_PUSH = "truxpense_general"

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Creates all app channels in the system. Safe to call on every launch.
     * No-op below Android O.
     */
    fun registerAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?: return

        val channels = listOf(
            NotificationChannel(
                DAILY_REMINDER,
                "Daily Expense Reminder",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Reminds you to log your daily expenses"
                enableLights(true)
                enableVibration(true)
            },
            NotificationChannel(
                BUDGET_ALERT,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alerts when a budget category is nearly exhausted"
                enableLights(true)
                enableVibration(true)
            },
            NotificationChannel(
                MONTHLY_RESET,
                "Monthly Budget Review",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Monthly reminders to review and reset your budgets"
                enableLights(false)
                enableVibration(false)
            },
            NotificationChannel(
                GENERAL_PUSH,
                "App Updates & Announcements",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "General notifications and feature announcements"
            },
        )

        channels.forEach { manager.createNotificationChannel(it) }
    }
}