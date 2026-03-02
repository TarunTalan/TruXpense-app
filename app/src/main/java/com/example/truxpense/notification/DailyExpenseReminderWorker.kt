package com.example.truxpense.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.truxpense.notification.NotificationHelper
import com.example.truxpense.notification.NotificationPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that fires the daily "log your expenses" reminder.
 *
 * Scheduling strategy
 * ───────────────────
 * Rather than a fixed PeriodicWorkRequest, [NotificationScheduler] enqueues a
 * new [OneTimeWorkRequest] each time this worker runs — computing the *exact*
 * delay to the next occurrence of the user's preferred time.
 *
 * Why one-shot instead of periodic?
 * - PeriodicWork has a minimum flex window of 5 minutes, meaning the OS may
 *   fire it up to 5 minutes early/late.
 * - OneTimeWork with recomputed delay achieves ±1 minute accuracy while still
 *   surviving Doze, App Standby, and reboots (WorkManager persists to Room).
 */
@HiltWorker
class DailyExpenseReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationHelper: NotificationHelper,
    private val prefs: NotificationPreferences,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "daily_expense_reminder"

        /**
         * Computes the millisecond delay from now until the next [hour]:[minute].
         * If that time has already passed today, rolls over to tomorrow.
         */
        fun delayUntilNextOccurrence(hour: Int, minute: Int): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // If already past today's slot, schedule for tomorrow
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }

        fun buildRequest(delayMs: Long): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<DailyExpenseReminderWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false) // must fire even on low battery
                        .build()
                )
                .addTag(WORK_NAME)
                .build()
    }

    override suspend fun doWork(): Result {
        val settings = prefs.getSnapshot()

        // Re-check the toggle in case it was disabled since the work was enqueued
        if (!settings.dailyReminderEnabled) return Result.success()

        // Fire the notification
        notificationHelper.showDailyReminder()

        // Self-reschedule: enqueue the next day's occurrence
        val delay = delayUntilNextOccurrence(settings.dailyReminderHour, settings.dailyReminderMinute)
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                buildRequest(delay),
            )

        return Result.success()
    }
}