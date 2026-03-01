package com.example.truxpense.notification

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.truxpense.MainActivity
import com.example.truxpense.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * FCM service — previously a stub that showed a generic notification on every
 * message and never saved the token. Now:
 *
 * 1. Routes each FCM message to the correct notification channel based on the
 *    "type" field in the message payload.
 * 2. Attaches a [PendingIntent] carrying [NotificationConstants.EXTRA_DESTINATION]
 *    so tapping the notification navigates to the right screen via
 *    [NotificationDeepLinkManager].
 * 3. Saves new FCM tokens to [NotificationPreferences] (required by the backend
 *    to send targeted pushes to this device).
 *
 * Expected FCM data payload shape (all fields optional — graceful defaults):
 * ```json
 * {
 *   "type":     "budget_warning | budget_exceeded | monthly_summary | daily_reminder | general",
 *   "title":    "optional override title",
 *   "body":     "optional override body",
 *   "category": "Food"   // for budget_* types
 * }
 * ```
 */
@AndroidEntryPoint
class TruxpenseFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var prefs: NotificationPreferences

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── onNewToken ─────────────────────────────────────────────────────────────

    /**
     * Called whenever FCM rotates the registration token for this device.
     * Persists it via [NotificationPreferences] so the UI can display it (debug)
     * and your backend can use it to target push messages.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch { prefs.saveFcmToken(token) }
    }

    // ── onMessageReceived ──────────────────────────────────────────────────────

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val notif = remoteMessage.notification

        // Merge: explicit data fields take precedence over the notification object
        val type = data["type"] ?: "general"
        val title = data["title"] ?: notif?.title ?: "TruXpense"
        val body = data["body"] ?: notif?.body ?: "You have a new update"
        val category = data["category"]                    // null for non-budget types

        when (type) {
            "budget_warning" -> showBudgetWarning(title, body, category)
            "budget_exceeded" -> showBudgetExceeded(title, body, category)
            "monthly_summary" -> showMonthlySummary(title, body)
            "daily_reminder" -> showDailyReminder(title, body)
            "budget_reset" -> showBudgetReset(title, body)
            else -> showGeneral(title, body)
        }
    }

    // ── Typed notification builders ────────────────────────────────────────────

    private fun showBudgetWarning(title: String, body: String, category: String?) {
        val notifId = NotificationConstants.NOTIF_BUDGET_90 + (category?.hashCode() ?: 0)
        show(
            notifId = notifId,
            title = title,
            body = body,
            channelId = NotificationChannels.BUDGET_ALERT,
            priority = NotificationCompat.PRIORITY_HIGH,
            destination = NotificationConstants.DEST_BUDGET_DETAIL,
            category = category,
            vibrate = longArrayOf(0L, 150L, 100L, 300L),
        )
    }

    private fun showBudgetExceeded(title: String, body: String, category: String?) {
        val notifId = NotificationConstants.NOTIF_BUDGET_100 + (category?.hashCode() ?: 0)
        show(
            notifId = notifId,
            title = title,
            body = body,
            channelId = NotificationChannels.BUDGET_ALERT,
            priority = NotificationCompat.PRIORITY_HIGH,
            destination = NotificationConstants.DEST_BUDGET_DETAIL,
            category = category,
            vibrate = longArrayOf(0L, 200L, 100L, 200L, 100L, 400L),
        )
    }

    private fun showMonthlySummary(title: String, body: String) {
        show(
            notifId = NotificationConstants.NOTIF_MONTHLY_SUMMARY,
            title = title,
            body = body,
            channelId = NotificationChannels.MONTHLY_RESET,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            destination = NotificationConstants.DEST_ANALYTICS,          // ← analytics tab
        )
    }

    private fun showDailyReminder(title: String, body: String) {
        show(
            notifId = NotificationConstants.NOTIF_DAILY_REMINDER,
            title = title,
            body = body,
            channelId = NotificationChannels.DAILY_REMINDER,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            destination = NotificationConstants.DEST_ADD_EXPENSE,        // ← add expense
        )
    }

    private fun showBudgetReset(title: String, body: String) {
        show(
            notifId = NotificationConstants.NOTIF_RESET_REMINDER,
            title = title,
            body = body,
            channelId = NotificationChannels.MONTHLY_RESET,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            destination = NotificationConstants.DEST_BUDGET_TAB,         // ← budget tab
        )
    }

    private fun showGeneral(title: String, body: String) {
        show(
            notifId = System.currentTimeMillis().toInt(),
            title = title,
            body = body,
            channelId = NotificationChannels.GENERAL_PUSH,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            destination = NotificationConstants.DEST_DASHBOARD,
        )
    }

    // ── Core builder ──────────────────────────────────────────────────────────

    private fun show(
        notifId: Int,
        title: String,
        body: String,
        channelId: String,
        priority: Int,
        destination: String,
        category: String? = null,
        vibrate: LongArray? = null,
    ) {
        val piFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationConstants.EXTRA_DESTINATION, destination)
            if (category != null) putExtra(NotificationConstants.EXTRA_CATEGORY, category)
        }
        val pi = PendingIntent.getActivity(this, notifId, intent, piFlags)

        val builder =
            NotificationCompat.Builder(this, channelId).setSmallIcon(R.drawable.ic_notification).setContentTitle(title)
                .setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body)).setContentIntent(pi)
                .setAutoCancel(true).setPriority(priority)

        if (vibrate != null) builder.setVibrate(vibrate)

        NotificationManagerCompat.from(this).notify(notifId, builder.build())
    }
}