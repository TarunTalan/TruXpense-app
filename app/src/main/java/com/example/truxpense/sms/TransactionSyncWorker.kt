package com.example.truxpense.sms

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.example.truxpense.data.repository.sms.PendingTransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager worker that uploads CONFIRMED transactions to the Spring Boot backend.
 *
 * Runs every 6 hours when a network connection is available.
 * Uses exponential back-off on failure.
 *
 * ## Usage (schedule once from Application or ViewModel)
 * ```kotlin
 * val request = TransactionSyncWorker.buildPeriodicRequest()
 * WorkManager.getInstance(context).enqueueUniquePeriodicWork(
 *     TransactionSyncWorker.UNIQUE_WORK_NAME,
 *     ExistingPeriodicWorkPolicy.KEEP,
 *     request
 * )
 * ```
 */
@HiltWorker
class TransactionSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: PendingTransactionRepository
    // TODO: inject your ApiService / SmsApiService here when the backend layer is wired up
    // private val apiService: SmsApiService
) : CoroutineWorker(appContext, params) {

    companion object {
        const val UNIQUE_WORK_NAME = "transaction_sync"
        private const val TAG      = "TransactionSyncWorker"

        fun buildPeriodicRequest(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<TransactionSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
    }

    override suspend fun doWork(): Result {
        val batch = repository.getConfirmedForSync(limit = 50)

        if (batch.isEmpty()) {
            Log.d(TAG, "Nothing to sync.")
            return Result.success()
        }

        Log.d(TAG, "Syncing ${batch.size} confirmed transactions to backend")

        return try {
            // TODO: replace with real API call when SmsApiService is available
            // val response = apiService.syncTransactions(batch.map { it.toApiDto() })
            // if (!response.isSuccessful) return Result.retry()

            // After successful upload, mark rows so they are not re-synced
            // For now we just log — remove this block and uncomment above when API is ready
            Log.d(TAG, "Sync stubbed — ${batch.size} transactions ready for upload")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed — will retry", e)
            Result.retry()
        }
    }
}