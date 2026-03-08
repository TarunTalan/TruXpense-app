package com.example.truxpense.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.truxpense.MainActivity
import com.example.truxpense.R
import com.example.truxpense.notification.channels.NotificationChannels
import com.example.truxpense.notification.channels.NotificationConstants
import com.example.truxpense.notification.datastore.NotificationPreferences
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
            smallIconRes = R.drawable.warning,  // warning icon
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
            smallIconRes = R.drawable.warning,  // warning icon
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
            smallIconRes = R.drawable.splash_screen_icon,
        )
    }

    private fun showDailyReminder(title: String, body: String) {
        show(
            notifId = NotificationConstants.NOTIF_DAILY_REMINDER,
            title = title,
            body = body,
            channelId = NotificationChannels.DAILY_REMINDER,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            destination = NotificationConstants.DEST_ADD_EXPENSE,
            smallIconRes = R.drawable.log,
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
            smallIconRes = R.drawable.sync,
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
            smallIconRes = R.drawable.splash_screen_icon,
        )
    }

    // ── Core builder ──────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun show(
        notifId: Int,
        title: String,
        body: String,
        channelId: String,
        priority: Int,
        destination: String,
        category: String? = null,
        vibrate: LongArray? = null,
        smallIconRes: Int? = null,
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
            NotificationCompat.Builder(this, channelId).setSmallIcon(smallIconRes ?: R.drawable.ic_notification).setContentTitle(title)
                .setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body)).setContentIntent(pi)
                .setAutoCancel(true).setPriority(priority)

        // ── Large icon ────────────────────────────────────────────────────────
        // API < 26 : R.mipmap.ic_launcher_round resolves to a real WebP bitmap —
        //            BitmapFactory decodes it, scale to the standard size, circle-clip.
        // API 26+  : The mipmap entry is an adaptive-icon XML; BitmapFactory returns null.
        //            Instead we manually composite background color + foreground vector
        //            onto a Canvas, then circle-clip the result.
        try {
            val size = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)

            val rawBmp: Bitmap? = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                // Bitmap WebP lives in mipmap-{hdpi,xhdpi,xxhdpi,xxxhdpi} — decode directly.
                val decoded = android.graphics.BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round)
                if (decoded != null) Bitmap.createScaledBitmap(decoded, size, size, true) else null
            } else {
                // Adaptive icon path: draw background + foreground onto a canvas.
                val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(android.graphics.Color.parseColor("#1F6F73"))
                val fg = androidx.core.content.ContextCompat.getDrawable(
                    this, R.drawable.ic_launcher_foreground
                )
                if (fg != null) { fg.setBounds(0, 0, size, size); fg.draw(canvas) }
                bmp
            }

            if (rawBmp != null) {
                // Circle-clip so it matches the round-icon style in the notification drawer.
                val circleBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val circleCanvas = Canvas(circleBmp)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                circleCanvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                circleCanvas.drawBitmap(rawBmp, Rect(0, 0, size, size), Rect(0, 0, size, size), paint)
                builder.setLargeIcon(circleBmp)
            }
        } catch (_: Exception) { }

        if (vibrate != null) builder.setVibrate(vibrate)

        NotificationManagerCompat.from(this).notify(notifId, builder.build())
    }
}