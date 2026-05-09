package com.example.truxpense.sms

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.truxpense.data.repository.sms.PendingTransactionRepository
import com.example.truxpense.data.sms.parser.SmsParserEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * One-shot WorkManager worker that reads the device SMS inbox and bulk-parses all
 * bank messages not yet seen by TruXpense (Re-scan History — S-05).
 *
 * ## Usage
 * ```kotlin
 * val request = HistoryRescanWorker.buildRequest()
 * WorkManager.getInstance(context).enqueueUniqueWork(
 *     HistoryRescanWorker.UNIQUE_WORK_NAME,
 *     ExistingWorkPolicy.KEEP,
 *     request
 * )
 * ```
 *
 * ## Progress keys
 * - [PROGRESS_PCT]     — 0–100 Int
 * - [PROGRESS_SCANNED] — messages processed so far
 * - [PROGRESS_TOTAL]   — total bank messages found
 *
 * ## Output keys (on SUCCESS)
 * - [OUTPUT_FOUND]     — number of new transactions inserted
 */
@HiltWorker
class HistoryRescanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val smsParser: SmsParserEngine,
    private val repository: PendingTransactionRepository
) : CoroutineWorker(appContext, params) {

    companion object {
        const val UNIQUE_WORK_NAME = "history_rescan"
        const val PROGRESS_PCT     = "progress_pct"
        const val PROGRESS_SCANNED = "progress_scanned"
        const val PROGRESS_TOTAL   = "progress_total"
        const val OUTPUT_FOUND     = "found_count"

        private const val TAG          = "HistoryRescanWorker"
        private const val CHUNK_SIZE   = 50
        private const val MAX_SMS_SCAN = 2_000   // safety cap — don't read entire inbox forever

        fun buildRequest(): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<HistoryRescanWorker>().build()
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting SMS inbox rescan")

        val bankMessages = readInboxSms()
        val total = bankMessages.size
        Log.d(TAG, "Found $total candidate bank SMS messages")

        if (total == 0) {
            return Result.success(workDataOf(OUTPUT_FOUND to 0))
        }

        var scanned = 0
        var found   = 0

        // Process in chunks to allow progress reporting and respect cancellation
        for (chunk in bankMessages.chunked(CHUNK_SIZE)) {
            if (isStopped) {
                Log.d(TAG, "Worker stopped — exiting early after $scanned/$total")
                break
            }

            val parsed = chunk.mapNotNull { (sender, body) ->
                smsParser.parse(sender, body)
            }

            if (parsed.isNotEmpty()) {
                repository.saveAllPending(parsed)
                found += parsed.size
            }

            scanned += chunk.size

            val pct = ((scanned.toFloat() / total) * 100).toInt().coerceIn(0, 100)
            setProgress(workDataOf(
                PROGRESS_PCT     to pct,
                PROGRESS_SCANNED to scanned,
                PROGRESS_TOTAL   to total
            ))
        }

        Log.d(TAG, "Rescan complete — scanned=$scanned, new transactions found=$found")
        return Result.success(workDataOf(OUTPUT_FOUND to found))
    }

    // ─────────────────────────────────────────────────────────────────────

    /**
     * Reads the SMS inbox via [ContentResolver] and returns (sender, body) pairs
     * that pass the fast bank pre-filter.
     *
     * Sorted newest-first so recent transactions appear at the top of S-02.
     * Capped at [MAX_SMS_SCAN] to avoid scanning an inbox with thousands of messages.
     */
    private fun readInboxSms(): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        val uri     = Uri.parse("content://sms/inbox")

        val cursor: Cursor? = applicationContext.contentResolver.query(
            uri,
            arrayOf("address", "body"),
            null, null,
            "date DESC"
        )

        cursor?.use {
            val addressCol = it.getColumnIndexOrThrow("address")
            val bodyCol    = it.getColumnIndexOrThrow("body")
            var count      = 0

            while (it.moveToNext() && count < MAX_SMS_SCAN) {
                val sender = it.getString(addressCol) ?: continue
                val body   = it.getString(bodyCol)   ?: continue
                count++
                if (smsParser.isBankSms(sender, body)) {
                    results.add(sender to body)
                }
            }
        }

        return results
    }
}