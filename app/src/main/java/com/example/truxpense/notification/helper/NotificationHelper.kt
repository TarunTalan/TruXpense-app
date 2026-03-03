package com.example.truxpense.notification.helper

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.truxpense.MainActivity
import com.example.truxpense.R
import com.example.truxpense.notification.channels.NotificationConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Centralised helper that constructs and posts every push notification.
 *
 * Bug fixed: previously used [NotificationConstants.CHANNEL_*] which had different
 * string values from [NotificationChannels.*], so the OS silently discarded every
 * notification because the channel didn't exist.  All builders now reference
 * [NotificationChannels] directly through the fixed [NotificationConstants] aliases.
 *
 * Tap navigation: every notification carries [NotificationConstants.EXTRA_DESTINATION]
 * (and optionally [NotificationConstants.EXTRA_CATEGORY]) in its PendingIntent.
 * [MainActivity.handleNotificationIntent] reads these and forwards them to
 * [NotificationDeepLinkManager], which in turn drives [DashboardScreen] navigation.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val TAG = "NotificationHelper"

    private val pendingFlags
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

    /**
     * Returns true if the app is allowed to post notifications.
     * On Android 13+ (API 33) this requires POST_NOTIFICATIONS runtime permission.
     * Logs a clear warning when blocked so it shows up in Logcat.
     */
    private fun canNotify(): Boolean {
        // Check OS-level notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted — notification blocked. " +
                        "Grant the permission in app Settings or via the runtime dialog.")
                return false
            }
        }
        // Check that notifications aren't disabled at the app or channel level
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            Log.w(TAG, "Notifications disabled for this app in system settings.")
            return false
        }
        return true
    }

    // ── Daily reminder ─────────────────────────────────────────────────────────

    fun notifyDailyReminder() {
        if (!canNotify()) return
        val pi = pendingIntent(NotificationConstants.NOTIF_DAILY_REMINDER, NotificationConstants.DEST_ADD_EXPENSE)
        val n = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_DAILY_REMINDER)   // ← fixed
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("Don't forget your expenses! \uD83D\uDCB8")
            .setContentText("Tap to quickly log what you spent today.").setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "It only takes a few seconds. Keep your budget on track by logging every expense!"
                )
            ).setContentIntent(pi).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
        NotificationManagerCompat.from(context).notify(NotificationConstants.NOTIF_DAILY_REMINDER, n)
    }

    // ── Budget 90 % warning ────────────────────────────────────────────────────

    fun notifyBudget90Percent(category: String, spent: Double, limit: Double) {
        if (!canNotify()) return
        val remaining = limit - spent
        val pct = ((spent / limit) * 100).toInt()
        val pi = pendingIntent(
            requestCode = NotificationConstants.NOTIF_BUDGET_90 + category.hashCode(),
            destination = NotificationConstants.DEST_BUDGET_DETAIL,
            category = category,
        )
        val n = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_BUDGET_ALERT)     // ← fixed
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("\u26A0\uFE0F $category budget almost full ($pct%)")
            .setContentText("${fmt(remaining)} remaining of ${fmt(limit)}").setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "You've used ${fmt(spent)} out of your ${fmt(limit)} $category budget. " + "Only ${fmt(remaining)} left — consider slowing down spending!"
                )
            ).setContentIntent(pi).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0L, 150L, 100L, 300L)).build()
        NotificationManagerCompat.from(context).notify(NotificationConstants.NOTIF_BUDGET_90 + category.hashCode(), n)
    }

    // ── Budget exceeded (100 %) ────────────────────────────────────────────────

    fun notifyBudgetExceeded(category: String, spent: Double, limit: Double) {
        if (!canNotify()) return
        val overspend = spent - limit
        val pi = pendingIntent(
            requestCode = NotificationConstants.NOTIF_BUDGET_100 + category.hashCode(),
            destination = NotificationConstants.DEST_BUDGET_DETAIL,
            category = category,
        )
        val n = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_BUDGET_ALERT)     // ← fixed
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("\uD83D\uDEA8 $category budget exceeded!")
            .setContentText("Over by ${fmt(overspend)}").setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "You've spent ${fmt(spent)}, which is ${fmt(overspend)} over your " + "${fmt(limit)} $category budget. Tap to review and adjust."
                )
            ).setContentIntent(pi).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0L, 200L, 100L, 200L, 100L, 400L)).build()
        NotificationManagerCompat.from(context).notify(NotificationConstants.NOTIF_BUDGET_100 + category.hashCode(), n)
    }

    // ── Monthly summary ────────────────────────────────────────────────────────
    // Destination fixed: was DEST_DASHBOARD, now goes to DEST_ANALYTICS so the
    // user lands directly on the analytics breakdown.

    fun notifyMonthlySummary(
        monthLabel: String,
        totalSpent: Double,
        totalBudget: Double,
        topCategory: String?,
    ) {
        if (!canNotify()) return
        val utilLine =
            if (totalBudget > 0) "That's ${((totalSpent / totalBudget) * 100).toInt()}% of your ${fmt(totalBudget)} budget."
            else ""
        val topLine = if (topCategory != null) "Top category: $topCategory." else ""
        val pi = pendingIntent(NotificationConstants.NOTIF_MONTHLY_SUMMARY, NotificationConstants.DEST_ANALYTICS)
        val n = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_MONTHLY_SUMMARY)  // ← fixed
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("\uD83D\uDCCA $monthLabel recap")
            .setContentText("You spent ${fmt(totalSpent)} this month.").setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "You spent ${fmt(totalSpent)} in $monthLabel. $utilLine $topLine Tap to see the full breakdown."
                )
            ).setContentIntent(pi).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
        NotificationManagerCompat.from(context).notify(NotificationConstants.NOTIF_MONTHLY_SUMMARY, n)
    }

    // ── Budget reset reminder ──────────────────────────────────────────────────
    // Destination fixed: was DEST_DASHBOARD, now goes to DEST_BUDGET_TAB.

    fun notifyBudgetResetReminder(monthLabel: String) {
        if (!canNotify()) return
        val pi = pendingIntent(NotificationConstants.NOTIF_RESET_REMINDER, NotificationConstants.DEST_BUDGET_TAB)
        val n = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_MONTHLY_SUMMARY)  // ← fixed
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("\uD83D\uDDD3\uFE0F New month, fresh budgets!")
            .setContentText("Review and reset your budgets for $monthLabel.").setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "A new month has started! Head over to your budgets to review your limits, " + "reset spending, or set up new categories for $monthLabel."
                )
            ).setContentIntent(pi).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
        NotificationManagerCompat.from(context).notify(NotificationConstants.NOTIF_RESET_REMINDER, n)
    }

    // ── Backwards-compatible wrappers used by workers ─────────────────────────

    fun showDailyReminder() = notifyDailyReminder()

    fun showBudgetThresholdAlert(
        categoryName: String,
        percentUsed: Int,
        remaining: String,
        categoryIndex: Int,
    ) {
        if (!canNotify()) return
        val pi = pendingIntent(
            requestCode = NotificationConstants.NOTIF_BUDGET_90 + categoryName.hashCode(),
            destination = NotificationConstants.DEST_BUDGET_DETAIL,
            category = categoryName,
        )
        val n = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_BUDGET_ALERT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("\u26A0\uFE0F $categoryName budget almost full ($percentUsed%)")
            .setContentText("$remaining remaining").setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "You've used $percentUsed% of your $categoryName budget. Only $remaining left — consider slowing down spending!"
                )
            ).setContentIntent(pi).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH).build()
        NotificationManagerCompat.from(context)
            .notify(NotificationConstants.NOTIF_BUDGET_90 + categoryName.hashCode(), n)
    }

    fun showBudgetExceeded(categoryName: String, spent: Double, limit: Double, categoryIndex: Int) =
        notifyBudgetExceeded(categoryName, spent, limit)

    fun showMonthlyResetReminder(monthLabel: String) = notifyBudgetResetReminder(monthLabel)

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun pendingIntent(
        requestCode: Int,
        destination: String,
        category: String? = null,
    ): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationConstants.EXTRA_DESTINATION, destination)
            if (category != null) putExtra(NotificationConstants.EXTRA_CATEGORY, category)
        },
        pendingFlags,
    )

    private fun fmt(amount: Double): String {
        val a = abs(amount)
        return when {
            a >= 1_00_000.0 -> "₹${"%.1f".format(a / 1_00_000.0)}L"
            a >= 1_000.0 -> "₹${"%.1f".format(a / 1_000.0)}K"
            else -> "₹${"%,.0f".format(a)}"
        }
    }
}