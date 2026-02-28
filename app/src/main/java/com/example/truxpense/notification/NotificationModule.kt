package com.example.truxpense.notification

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module wiring WorkManager and the notification graph.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * IMPORTANT — Application class setup required
 * ──────────────────────────────────────────────────────────────────────────────
 * Because we inject dependencies into workers via [HiltWorkerFactory], the app
 * must **not** call WorkManager.initialize() in its manifest (remove the default
 * initializer) and must instead implement [androidx.work.Configuration.Provider].
 *
 * Add the following to your Application class (already annotated with @HiltAndroidApp):
 *
 * ```kotlin
 * @HiltAndroidApp
 * class TruxpenseApp : Application(), Configuration.Provider {
 *
 *     @Inject lateinit var workerFactory: HiltWorkerFactory
 *
 *     override val workManagerConfiguration: Configuration
 *         get() = Configuration.Builder()
 *             .setWorkerFactory(workerFactory)
 *             .build()
 *
 *     override fun onCreate() {
 *         super.onCreate()
 *         NotificationChannels.registerAll(this)
 *
 *         // Restore all scheduled workers on app start
 *         val scheduler: NotificationScheduler by inject()  // or use Hilt EntryPoint
 *         val prefs: NotificationPreferences by inject()
 *         lifecycleScope.launch {
 *             prefs.settings.first().let { scheduler.applySettings(it) }
 *         }
 *     }
 * }
 * ```
 *
 * Also add to AndroidManifest.xml inside <application>:
 * ```xml
 * <provider
 *     android:name="androidx.startup.InitializationProvider"
 *     android:authorities="${applicationId}.androidx-startup"
 *     android:exported="false"
 *     tools:node="merge">
 *   <!-- Remove the default WorkManager initializer so Hilt can take over -->
 *   <meta-data
 *       android:name="androidx.work.WorkManagerInitializer"
 *       android:value="androidx.startup"
 *       tools:node="remove" />
 * </provider>
 * ```
 */
@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}