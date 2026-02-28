package com.example.truxpense.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.truxpense.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TruxpenseFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Minimal implementation: show a simple notification for data/notification messages
        val channelId = "truxpense_default_channel"
        createNotificationChannel(channelId)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "TruXpense"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "You have a new message"

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    override fun onNewToken(token: String) {
        // TODO: send token to app server if needed
        super.onNewToken(token)
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "TruXpense"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance)
            channel.enableLights(true)
            channel.enableVibration(true)
            channel.setSound(null, null)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}

