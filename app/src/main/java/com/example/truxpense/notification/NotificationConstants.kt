package com.example.truxpense.notification

object NotificationConstants {
    const val DEFAULT_REMINDER_HOUR = 9
    const val DEFAULT_REMINDER_MINUTE = 0

    const val WORK_DAILY_REMINDER = "truxpense_work_daily_reminder"
    const val WORK_BUDGET_CHECKER = "truxpense_work_budget_checker"
    const val WORK_MONTHLY_SUMMARY = "truxpense_work_monthly_summary"

    // Notification channel IDs
    const val CHANNEL_DAILY_REMINDER = "truxpense_channel_daily_reminder"
    const val CHANNEL_BUDGET_ALERT = "truxpense_channel_budget_alert"
    const val CHANNEL_MONTHLY_SUMMARY = "truxpense_channel_monthly_summary"

    // Notification ids (small ints used with NotificationManager)
    const val NOTIF_DAILY_REMINDER = 1001
    const val NOTIF_BUDGET_90 = 1100
    const val NOTIF_BUDGET_100 = 1200
    const val NOTIF_MONTHLY_SUMMARY = 1300
    const val NOTIF_RESET_REMINDER = 1400

    // Destinations used by PendingIntent extras
    const val DEST_ADD_EXPENSE = "dest_add_expense"
    const val DEST_BUDGET_DETAIL = "dest_budget_detail"
    const val DEST_DASHBOARD = "dest_dashboard"

    // Extra keys
    const val EXTRA_DESTINATION = "extra_destination"
    const val EXTRA_CATEGORY = "extra_category"
}
