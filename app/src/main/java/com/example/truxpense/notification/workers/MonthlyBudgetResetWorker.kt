package com.example.truxpense.notification.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.truxpense.notification.helper.NotificationHelper
import com.example.truxpense.notification.datastore.NotificationPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit


@HiltWorker
class MonthlyBudgetResetWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationHelper: NotificationHelper,
    private val prefs: NotificationPreferences,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "monthly_budget_reset_check"

        // Key stored in DataStore (via NotificationPreferences) to track
        // which month we last sent the reset notification for.
        // Format: "YYYY-MM"
        const val LAST_RESET_MONTH_KEY = "last_reset_month"

        fun buildRequest(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<MonthlyBudgetResetWorker>(24, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .addTag(WORK_NAME)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
                .build()

        /** Full month name e.g. "March 2026" */
        fun currentMonthName(): String {
            val cal   = Calendar.getInstance()
            val names = listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            return "${names[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
        }

        /** "2026-03" */
        fun currentYearMonth(): String {
            val cal = Calendar.getInstance()
            return "%d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        }
    }

    override suspend fun doWork(): Result {
        val settings = prefs.getSnapshot()
        if (!settings.monthlyResetEnabled) return Result.success()

        val cal = Calendar.getInstance()
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)

        // Only act on day 1 of the month
        if (dayOfMonth != 1) return Result.success()

        val yearMonth = currentYearMonth()

        // Guard: don't re-send if we already sent the notification for this month.
        // We repurpose the notifiedBudgets set with a sentinel key "MONTHLY_RESET:YYYY-MM"
        val sentinel = "MONTHLY_RESET:$yearMonth"
        if (sentinel in settings.notifiedBudgets) return Result.success()

        // 1. Post notification
        notificationHelper.showMonthlyResetReminder(currentMonthName())

        // 2. Clear per-category dedup set so budget alerts fire for the new month
        prefs.clearNotifiedBudgets()

        // 3. Re-record the sentinel for this month (clearNotifiedBudgets just wiped it)
        prefs.markBudgetNotified("MONTHLY_RESET", yearMonth)

        return Result.success()
    }
}