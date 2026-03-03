package com.example.truxpense.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.truxpense.MainActivity
import com.example.truxpense.R
import com.example.truxpense.data.sms.model.ParsedTransaction
import com.example.truxpense.data.sms.model.TxnType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID    = "sms_parse_channel"
        const val CHANNEL_NAME  = "Expense Alerts"
        const val CHANNEL_DESC  = "Notifications for new expenses detected from bank SMS"

        const val ACTION_CONFIRM = "com.example.truxpense.ACTION_CONFIRM_TXN"
        const val ACTION_REJECT  = "com.example.truxpense.ACTION_REJECT_TXN"
        const val EXTRA_TXN_ID   = "txn_id"

        private const val SUMMARY_NOTIFICATION_ID = 1001
        private const val SUMMARY_GROUP_KEY       = "com.example.truxpense.SMS_TXN_GROUP"
    }

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    init {
        createChannelIfNeeded()
    }

    @SuppressLint("MissingPermission")
    fun showReviewNotification(transaction: ParsedTransaction) {
        val notifId = transaction.id.hashCode()

        val title = buildTitle(transaction)
        val body  = buildBody(transaction)

        val openAppIntent = buildOpenAppPendingIntent(notifId)
        val confirmIntent = buildActionPendingIntent(ACTION_CONFIRM, transaction.id, notifId)
        val rejectIntent  = buildActionPendingIntent(ACTION_REJECT,  transaction.id, notifId)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openAppIntent)
            .addAction(0, "✓ Confirm", confirmIntent)
            .addAction(0, "✏️ Edit",   openAppIntent)
            .addAction(0, "✕ Ignore",  rejectIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setGroup(SUMMARY_GROUP_KEY)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notifId, notification)
        updateSummaryNotification()
    }

    @SuppressLint("MissingPermission")
    fun updateSummaryNotification(pendingCount: Int = 0) {
        if (pendingCount <= 1) {
            notificationManager.cancel(SUMMARY_NOTIFICATION_ID)
            return
        }
        val summary = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$pendingCount new expenses detected")
            .setContentText("Tap to review your pending transactions")
            .setContentIntent(buildOpenAppPendingIntent(SUMMARY_NOTIFICATION_ID))
            .setGroup(SUMMARY_GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(SUMMARY_NOTIFICATION_ID, summary)
    }

    fun cancelForTransaction(txnId: String) {
        notificationManager.cancel(txnId.hashCode())
    }

    private fun buildTitle(tx: ParsedTransaction): String {
        val sign   = if (tx.type == TxnType.CREDIT) "+" else "-"
        val amount = "₹%.2f".format(tx.amount)
        return "${tx.type.label}: $sign$amount${if (tx.merchant != null) " · ${tx.merchant}" else ""}"
    }

    private fun buildBody(tx: ParsedTransaction): String {
        val confPct  = (tx.confidence * 100).toInt()
        val category = tx.category.displayName
        val account  = if (tx.accountLast4 != null) " · ${tx.bank} **${tx.accountLast4}" else " · ${tx.bank}"
        return "$category · $confPct% confidence$account"
    }

    private fun buildOpenAppPendingIntent(requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "pending_transactions")
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildActionPendingIntent(action: String, txnId: String, requestCode: Int): PendingIntent {
        val intent = Intent(action).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_TXN_ID, txnId)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val existing = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESC
            enableVibration(true)
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}

