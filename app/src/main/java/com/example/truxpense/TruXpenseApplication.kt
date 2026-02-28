package com.example.truxpense

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.truxpense.notification.NotificationChannels
import com.example.truxpense.notification.NotificationPreferences
import com.example.truxpense.notification.NotificationScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// Application class
@HiltAndroidApp
class TruXpenseApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var scheduler: NotificationScheduler

    @Inject
    lateinit var prefs: NotificationPreferences

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Register notification channels (safe to call every launch)
        NotificationChannels.registerAll(this)

        // Restore scheduled workers from persisted settings
        appScope.launch {
            val settings = prefs.settings.first()
            scheduler.applySettings(settings)
        }
    }
}
