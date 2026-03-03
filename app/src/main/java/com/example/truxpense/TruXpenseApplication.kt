package com.example.truxpense

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.example.truxpense.notification.channels.NotificationChannels
import com.example.truxpense.notification.datastore.NotificationPreferences
import com.example.truxpense.notification.scheduler.NotificationScheduler
import com.example.truxpense.service.TransactionNotifier
import com.example.truxpense.sms.TransactionSyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class TruXpenseApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var scheduler: NotificationScheduler

    @Inject
    lateinit var prefs: NotificationPreferences

    /**
     * Injecting [TransactionNotifier] here ensures the SMS notification channel
     * ("sms_parse_channel") is created at startup — before the first bank SMS arrives.
     * Channel creation is a no-op if it already exists, so this is always safe.
     */
    @Inject
    lateinit var transactionNotifier: TransactionNotifier

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()

        // Register all notification channels on every launch (no-op if already registered)
        NotificationChannels.registerAll(this)
        // SMS expense channel is additionally bootstrapped via TransactionNotifier injection above

        // Restore WorkManager workers from the user's persisted notification settings
        appScope.launch {
            val settings = prefs.settings.first()
            scheduler.applySettings(settings)
        }

        // Schedule the periodic transaction sync worker.
        // KEEP policy means if it's already enqueued (e.g. after a reboot), we don't
        // reset its next-run timer — respects battery optimisation.
        scheduleSmsWorkers()
    }

    // ─────────────────────────────────────────────────────────────────────

    private fun scheduleSmsWorkers() {
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            TransactionSyncWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            TransactionSyncWorker.buildPeriodicRequest()
        )
    }
}