package com.example.truxpense.notification.helper

import android.annotation.SuppressLint
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
 * Centralised helper that constructs and posts every local notification.
 *
 * Permission guard: [canNotify] checks POST_NOTIFICATIONS (Android 13+) and
 * the app-level notification toggle before any [NotificationManagerCompat.notify]
 * call, so [SuppressLint] below is safe — the permission is validated at runtime.
 *
 * Tap navigation: every notification carries [NotificationConstants.EXTRA_DESTINATION]
 * in its PendingIntent so [MainActivity] can deep-link the user to the right screen.
 */
@SuppressLint("MissingPermission")
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val TAG = "NotificationHelper"

    private val pendingFlags
        get() = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

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
        val n = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_DAILY_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Log today's expenses")
            .setContentText("Keep your budget on track — it only takes a moment.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("You haven't logged any expenses yet today. Take a few seconds to record what you've spent and stay on top of your budget.")
            )
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
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
        val n = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_BUDGET_ALERT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$category budget at $pct%")
            .setContentText("${fmt(remaining)} remaining out of ${fmt(limit)}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("You've used ${fmt(spent)} of your ${fmt(limit)} $category budget ($pct%). Only ${fmt(remaining)} remains — review your spending to stay within your limit.")
            )
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0L, 150L, 100L, 300L))
            .build()
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
        val n = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_BUDGET_ALERT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$category budget exceeded")
            .setContentText("Over by ${fmt(overspend)}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("You've spent ${fmt(spent)} against a ${fmt(limit)} budget for $category — ${fmt(overspend)} over the limit. Tap to review your transactions and adjust your budget if needed.")
            )
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0L, 200L, 100L, 200L, 100L, 400L))
            .build()
        NotificationManagerCompat.from(context).notify(NotificationConstants.NOTIF_BUDGET_100 + category.hashCode(), n)
    }

    // ── Monthly summary ────────────────────────────────────────────────────────

    fun notifyMonthlySummary(
        monthLabel: String,
        totalSpent: Double,
        totalBudget: Double,
        topCategory: String?,
    ) {
        if (!canNotify()) return
        val utilLine = if (totalBudget > 0)
            "That's ${((totalSpent / totalBudget) * 100).toInt()}% of your ${fmt(totalBudget)} total budget."
        else ""
        val topLine = if (topCategory != null) "Your highest spending category was $topCategory." else ""
        val pi = pendingIntent(NotificationConstants.NOTIF_MONTHLY_SUMMARY, NotificationConstants.DEST_ANALYTICS)
        val n = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_MONTHLY_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$monthLabel spending summary")
            .setContentText("Total spent: ${fmt(totalSpent)}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("You spent ${fmt(totalSpent)} in $monthLabel. $utilLine $topLine Tap to view your full monthly breakdown.")
            )
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NotificationConstants.NOTIF_MONTHLY_SUMMARY, n)
    }

    // ── Budget reset reminder ──────────────────────────────────────────────────

    fun notifyBudgetResetReminder(monthLabel: String) {
        if (!canNotify()) return
        val pi = pendingIntent(NotificationConstants.NOTIF_RESET_REMINDER, NotificationConstants.DEST_BUDGET_TAB)
        val n = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_MONTHLY_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New month — review your budgets")
            .setContentText("$monthLabel has started. Update your spending limits.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$monthLabel is here. Take a moment to review your budget limits, carry over any adjustments, or set up new categories for the month ahead.")
            )
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
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
            .setContentTitle("$categoryName budget at $percentUsed%")
            .setContentText("$remaining remaining")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("You've used $percentUsed% of your $categoryName budget. Only $remaining left — consider reviewing your spending to stay within your limit.")
            )
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context)
            .notify(NotificationConstants.NOTIF_BUDGET_90 + categoryName.hashCode(), n)
    }

    fun showBudgetExceeded(categoryName: String, spent: Double, limit: Double, categoryIndex: Int) =
        notifyBudgetExceeded(categoryName, spent, limit)

    fun showMonthlyResetReminder(monthLabel: String) = notifyBudgetResetReminder(monthLabel)

    // ── Unusual spending ───────────────────────────────────────────────────────

    fun notifyUnusualSpending(todaySpend: Double, baselineDailyAvg: Double) {
        if (!canNotify()) return
        val pi = pendingIntent(NotificationConstants.NOTIF_UNUSUAL_SPENDING, NotificationConstants.DEST_ANALYTICS)
        val multiplier = "%.1f".format(todaySpend / baselineDailyAvg)
        val n = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_BUDGET_ALERT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Unusual spending detected")
            .setContentText("Today's spend (${fmt(todaySpend)}) is ${multiplier}× your daily average")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "You've spent ${fmt(todaySpend)} today — ${multiplier}× your usual daily average of ${fmt(baselineDailyAvg)}. " +
                        "Tap to review your transactions and make sure everything looks right."
                    )
            )
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0L, 150L, 100L, 150L))
            .build()
        NotificationManagerCompat.from(context).notify(NotificationConstants.NOTIF_UNUSUAL_SPENDING, n)
    }

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