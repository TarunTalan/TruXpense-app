package com.example.truxpense.notification.channels

object NotificationConstants {
    const val DEFAULT_REMINDER_HOUR = 9
    const val DEFAULT_REMINDER_MINUTE = 0

    // WorkManager unique work names
    const val WORK_DAILY_REMINDER = "truxpense_work_daily_reminder"
    const val WORK_BUDGET_CHECKER = "truxpense_work_budget_checker"
    const val WORK_MONTHLY_SUMMARY = "truxpense_work_monthly_summary"

    // Notification IDs (small ints used with NotificationManager)
    const val NOTIF_DAILY_REMINDER = 1001
    const val NOTIF_BUDGET_90 = 1100  // +category.hashCode() per category
    const val NOTIF_BUDGET_100 = 1200  // +category.hashCode() per category
    const val NOTIF_MONTHLY_SUMMARY = 1300
    const val NOTIF_RESET_REMINDER = 1400

    // ── Channel IDs — MUST match NotificationChannels.* exactly ──────────────
    // Previously these were "truxpense_channel_*" which didn't match the
    // registered channels ("truxpense_*"), causing all notifications to be
    // silently dropped by the OS.
    const val CHANNEL_DAILY_REMINDER = NotificationChannels.DAILY_REMINDER   // "truxpense_daily_reminder"
    const val CHANNEL_BUDGET_ALERT = NotificationChannels.BUDGET_ALERT      // "truxpense_budget_alert"
    const val CHANNEL_MONTHLY_SUMMARY = NotificationChannels.MONTHLY_RESET     // "truxpense_monthly_reset"

    // ── Destination extras carried in PendingIntents ──────────────────────────
    // Parsed by NotificationDeepLinkManager.handle() and turned into navigation actions.
    const val DEST_ADD_EXPENSE = "dest_add_expense"
    const val DEST_BUDGET_TAB = "dest_budget_tab"       // lands on budget list
    const val DEST_BUDGET_DETAIL = "dest_budget_detail"    // same tab, highlights category
    const val DEST_ANALYTICS = "dest_analytics"
    const val DEST_TRANSACTIONS = "dest_transactions"
    const val DEST_DASHBOARD = "dest_dashboard"

    // ── Extra keys ────────────────────────────────────────────────────────────
    const val EXTRA_DESTINATION = "extra_destination"
    const val EXTRA_CATEGORY = "extra_category"
}