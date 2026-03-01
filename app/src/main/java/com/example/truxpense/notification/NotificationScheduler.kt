package com.example.truxpense.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and cancels all background workers for push notifications.
 *
 * Bug fixed: previously used stub workers from the `.workers` sub-package
 * ([DailyReminderWorker], [BudgetAlertWorker], [MonthlySummaryWorker]) which
 * did nothing in doWork().  Now uses the real implemented workers:
 *
 *   Stub (was)                     →  Real (now)
 *   DailyReminderWorker            →  DailyExpenseReminderWorker
 *   BudgetAlertWorker              →  BudgetThresholdWorker
 *   MonthlySummaryWorker           →  MonthlyBudgetResetWorker
 *
 * Daily reminder scheduling: also fixed from PeriodicWork to a chain of
 * [OneTimeWorkRequest]s as required by [DailyExpenseReminderWorker], which
 * self-reschedules to maintain exact per-minute accuracy.
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    // ── Public API ─────────────────────────────────────────────────────────────

    /** Applies the full [NotificationSettings] snapshot — idempotent, safe to call on app start. */
    fun applySettings(settings: NotificationSettings) {
        if (settings.dailyReminderEnabled) scheduleDailyReminder(
            settings.dailyReminderHour,
            settings.dailyReminderMinute
        )
        else cancelDailyReminder()

        if (settings.budgetThresholdEnabled) scheduleBudgetThresholdCheck()
        else cancelBudgetThresholdCheck()

        if (settings.monthlyResetEnabled) scheduleMonthlyResetCheck()
        else cancelMonthlyResetCheck()
    }

    fun scheduleAll(
        reminderHour: Int = NotificationConstants.DEFAULT_REMINDER_HOUR,
        reminderMinute: Int = NotificationConstants.DEFAULT_REMINDER_MINUTE,
    ) {
        scheduleDailyReminder(reminderHour, reminderMinute)
        scheduleBudgetThresholdCheck()
        scheduleMonthlyResetCheck()
    }

    fun cancelAll() {
        cancelDailyReminder()
        cancelBudgetThresholdCheck()
        cancelMonthlyResetCheck()
    }

    // ── Daily reminder ─────────────────────────────────────────────────────────
    // Uses DailyExpenseReminderWorker (OneTimeWork self-rescheduling chain).
    // The real worker computes the delay to the user's exact preferred time.

    fun scheduleDailyReminder(hour: Int, minute: Int) {
        val delayMs = delayUntil(hour, minute)
        workManager.enqueueUniqueWork(
            NotificationConstants.WORK_DAILY_REMINDER,
            ExistingWorkPolicy.REPLACE,
            DailyExpenseReminderWorker.buildRequest(delayMs),
        )
    }

    fun cancelDailyReminder() {
        workManager.cancelUniqueWork(NotificationConstants.WORK_DAILY_REMINDER)
    }

    fun rescheduleDailyReminder(hour: Int, minute: Int) = scheduleDailyReminder(hour, minute)

    // ── Budget threshold check ─────────────────────────────────────────────────
    // Uses BudgetThresholdWorker (PeriodicWork every 6 h).

    fun scheduleBudgetThresholdCheck() {
        workManager.enqueueUniquePeriodicWork(
            NotificationConstants.WORK_BUDGET_CHECKER,
            ExistingPeriodicWorkPolicy.KEEP,
            BudgetThresholdWorker.buildRequest(),
        )
    }

    fun cancelBudgetThresholdCheck() {
        workManager.cancelUniqueWork(NotificationConstants.WORK_BUDGET_CHECKER)
    }

    // ── Monthly budget-reset check ─────────────────────────────────────────────
    // Uses MonthlyBudgetResetWorker (PeriodicWork every 24 h, fires notification on day 1).

    fun scheduleMonthlyResetCheck() {
        workManager.enqueueUniquePeriodicWork(
            NotificationConstants.WORK_MONTHLY_SUMMARY,
            ExistingPeriodicWorkPolicy.KEEP,
            MonthlyBudgetResetWorker.buildRequest(),
        )
    }

    fun cancelMonthlyResetCheck() {
        workManager.cancelUniqueWork(NotificationConstants.WORK_MONTHLY_SUMMARY)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Returns the millisecond delay from now until the next [hour]:[minute]. */
    private fun delayUntil(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }
}