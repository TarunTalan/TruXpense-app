package com.example.truxpense.presentation.screens.dashboard.vault

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.truxpense.data.repository.vault.ReportVaultRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class VaultSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val vaultRepository: ReportVaultRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Upload any LOCAL_ONLY / ERROR entries whose storageOption is BOTH or CLOUD_ONLY
            vaultRepository.uploadPending()
            // Pull latest from Firestore into Room
            vaultRepository.syncFromCloud()
            Result.success()
        } catch (e: Exception) {
            // Retry up to 3 times with exponential backoff
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "VaultSyncWorker"

        /**
         * Schedules a periodic sync — runs every 6 hours,
         * only on WiFi and while charging.
         *
         * Call from your Application.onCreate() or after login.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)   // WiFi only
                .setRequiresCharging(true)
                .build()

            val request = PeriodicWorkRequestBuilder<VaultSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /**
         * Schedules a one-time immediate sync (e.g. on manual retry or app launch).
         * Runs on any network, no charging requirement.
         */
        fun scheduleImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<VaultSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_immediate",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}