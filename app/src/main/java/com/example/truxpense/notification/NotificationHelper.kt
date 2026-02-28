package com.example.truxpense.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.truxpense.R
import com.example.truxpense.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralised helper that constructs and posts every push notification in Truxpense.
 *
 * All notification content — titles, bodies, channel routing, PendingIntents — lives here.
 * Workers and other callers just call the typed notify*() methods.
 *
 * Permission note: Android 13+ (API 33) requires POST_NOTIFICATIONS.
 * The permission is requested at runtime by [NotificationScheduler] / the settings UI
 * before any notification is scheduled; callers here assume permission is granted.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val pendingIntentFlags
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

    // ── Daily reminder ─────────────────────────────────────────────────────────

    fun notifyDailyReminder() {
        val pending = PendingIntent.getActivity(
            context, NotificationConstants.NOTIF_DAILY_REMINDER,
            mainActivityIntent(NotificationConstants.DEST_ADD_EXPENSE),
            pendingIntentFlags,
        )
        val notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_DAILY_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Don't forget your expenses! \uD83D\uDCB8")
            .setContentText("Tap to quickly log what you spent today.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("It only takes a few seconds. Keep your budget on track by logging every expense!")
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NotificationConstants.NOTIF_DAILY_REMINDER, notification)
    }

    // ── Budget 90 % warning ────────────────────────────────────────────────────

    fun notifyBudget90Percent(category: String, spent: Double, limit: Double) {
        val remaining = limit - spent
        val pct = ((spent / limit) * 100).toInt()
        val pending = PendingIntent.getActivity(
            context,
            NotificationConstants.NOTIF_BUDGET_90 + category.hashCode(),
            mainActivityIntent(NotificationConstants.DEST_BUDGET_DETAIL, category),
            pendingIntentFlags,
        )
        val notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_BUDGET_ALERT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("\u26A0\uFE0F $category budget almost full ($pct%)")
            .setContentText("${formatINR(remaining)} remaining of ${formatINR(limit)}")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "You've used ${formatINR(spent)} out of your ${formatINR(limit)} $category budget. " +
                    "Only ${formatINR(remaining)} left \u2014 consider slowing down spending!"
                )
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0L, 150L, 100L, 300L))
            .build()
        NotificationManagerCompat.from(context)
            .notify(NotificationConstants.NOTIF_BUDGET_90 + category.hashCode(), notification)
    }

    // ── Budget exceeded (100 %) ────────────────────────────────────────────────

    fun notifyBudgetExceeded(category: String, spent: Double, limit: Double) {
        val overspend = spent - limit
        val pending = PendingIntent.getActivity(
            context,
            NotificationConstants.NOTIF_BUDGET_100 + category.hashCode(),
            mainActivityIntent(NotificationConstants.DEST_BUDGET_DETAIL, category),
            pendingIntentFlags,
        )
        val notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_BUDGET_ALERT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("\uD83D\uDEA8 $category budget exceeded!")
            .setContentText("Over by ${formatINR(overspend)}")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "You've spent ${formatINR(spent)}, which is ${formatINR(overspend)} over your " +
                    "${formatINR(limit)} $category budget. Tap to review and adjust."
                )
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0L, 200L, 100L, 200L, 100L, 400L))
            .build()
        NotificationManagerCompat.from(context)
            .notify(NotificationConstants.NOTIF_BUDGET_100 + category.hashCode(), notification)
    }

    // ── Monthly summary ────────────────────────────────────────────────────────

    fun notifyMonthlySummary(
        monthLabel: String,
        totalSpent: Double,
        totalBudget: Double,
        topCategory: String?,
    ) {
        val utilLine = if (totalBudget > 0) {
            val pct = ((totalSpent / totalBudget) * 100).toInt()
            "That's $pct% of your ${formatINR(totalBudget)} budget."
        } else ""
        val topLine = if (topCategory != null) "Top category: $topCategory." else ""
        val pending = PendingIntent.getActivity(
            context, NotificationConstants.NOTIF_MONTHLY_SUMMARY,
            mainActivityIntent(NotificationConstants.DEST_DASHBOARD),
            pendingIntentFlags,
        )
        val notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_MONTHLY_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("\uD83D\uDCCA $monthLabel recap")
            .setContentText("You spent ${formatINR(totalSpent)} this month.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "You spent ${formatINR(totalSpent)} in $monthLabel. $utilLine $topLine " +
                    "Tap to see the full breakdown."
                )
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NotificationConstants.NOTIF_MONTHLY_SUMMARY, notification)
    }

    // ── Budget reset reminder ──────────────────────────────────────────────────

    fun notifyBudgetResetReminder(monthLabel: String) {
        val pending = PendingIntent.getActivity(
            context, NotificationConstants.NOTIF_RESET_REMINDER,
            mainActivityIntent(NotificationConstants.DEST_DASHBOARD),
            pendingIntentFlags,
        )
        val notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_MONTHLY_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("\uD83D\uDDD3\uFE0F New month, fresh budgets!")
            .setContentText("Review and reset your budgets for $monthLabel.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "A new month has started! Head over to your budgets to review your limits, " +
                    "reset spending, or set up new categories for $monthLabel."
                )
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NotificationConstants.NOTIF_RESET_REMINDER, notification)
    }

    // ── Backwards-compatible wrappers used by older callsites in workers ──────

    fun showDailyReminder() { notifyDailyReminder() }

    fun showBudgetThresholdAlert(categoryName: String, percentUsed: Int, remaining: String, categoryIndex: Int) {
        // Build a notification similar to notifyBudget90Percent but using percent & remaining string
        val pending = PendingIntent.getActivity(
            context,
            NotificationConstants.NOTIF_BUDGET_90 + categoryName.hashCode(),
            mainActivityIntent(NotificationConstants.DEST_BUDGET_DETAIL, categoryName),
            pendingIntentFlags,
        )
        val notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_BUDGET_ALERT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("\u26A0\uFE0F $categoryName budget almost full ($percentUsed%)")
            .setContentText("$remaining remaining")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "You've used $percentUsed% of your $categoryName budget. Only $remaining left — consider slowing down spending!"
                )
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context)
            .notify(NotificationConstants.NOTIF_BUDGET_90 + categoryName.hashCode(), notification)
    }

    fun showBudgetExceeded(categoryName: String, spent: Double, limit: Double, categoryIndex: Int) {
        notifyBudgetExceeded(categoryName, spent, limit)
    }

    fun showMonthlyResetReminder(monthLabel: String) { notifyBudgetResetReminder(monthLabel) }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun mainActivityIntent(destination: String, category: String? = null): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationConstants.EXTRA_DESTINATION, destination)
            if (category != null) putExtra(NotificationConstants.EXTRA_CATEGORY, category)
        }

    private fun formatINR(amount: Double): String {
        val abs = kotlin.math.abs(amount)
        return when {
            abs >= 1_00_000.0 -> "₹${"%.1f".format(abs / 1_00_000.0)}L"
            abs >= 1_000.0    -> "₹${"%.1f".format(abs / 1_000.0)}K"
            else              -> "₹${"%,.0f".format(abs)}"
        }
    }
}