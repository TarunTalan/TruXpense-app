package com.example.truxpense.notification

import android.content.Context
import androidx.work.*
import com.example.truxpense.notification.workers.BudgetAlertWorker
import com.example.truxpense.notification.workers.DailyReminderWorker
import com.example.truxpense.notification.workers.MonthlySummaryWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val workManager get() = WorkManager.getInstance(context)

    private val defaultConstraints = Constraints.Builder()
        .setRequiresBatteryNotLow(false)
        .build()

    // ── Public API ─────────────────────────────────────────────────────────────

    fun scheduleAll(
        reminderHour: Int = NotificationConstants.DEFAULT_REMINDER_HOUR,
        reminderMinute: Int = NotificationConstants.DEFAULT_REMINDER_MINUTE,
    ) {
        scheduleDailyReminder(reminderHour, reminderMinute)
        scheduleBudgetChecker()
        scheduleMonthlySummary()
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(NotificationConstants.WORK_DAILY_REMINDER)
        workManager.cancelUniqueWork(NotificationConstants.WORK_BUDGET_CHECKER)
        workManager.cancelUniqueWork(NotificationConstants.WORK_MONTHLY_SUMMARY)
    }

    fun rescheduleDailyReminder(hour: Int, minute: Int) {
        workManager.cancelUniqueWork(NotificationConstants.WORK_DAILY_REMINDER)
        scheduleDailyReminder(hour, minute)
    }

    // Expose public APIs required by ViewModel
    fun scheduleDailyReminder(hour: Int, minute: Int) { scheduleDailyReminderInternal(hour, minute) }
    fun cancelDailyReminder() { cancelDailyReminderInternal() }

    fun scheduleBudgetThresholdCheck() { scheduleBudgetChecker() }
    fun cancelBudgetThresholdCheck() { cancelBudgetChecker() }

    fun scheduleMonthlyResetCheck() { scheduleMonthlySummary() }
    fun cancelMonthlyResetCheck() { cancelMonthlyNotifications() }

    // Apply persisted settings: idempotent
    fun applySettings(settings: NotificationSettings) {
        if (settings.dailyReminderEnabled) {
            scheduleDailyReminder(settings.dailyReminderHour, settings.dailyReminderMinute)
        } else {
            cancelDailyReminder()
        }

        if (settings.budgetThresholdEnabled) {
            scheduleBudgetThresholdCheck()
        } else {
            cancelBudgetThresholdCheck()
        }

        if (settings.monthlyResetEnabled) {
            scheduleMonthlyResetCheck()
        } else {
            cancelMonthlyResetCheck()
        }
    }

    // ── Private ────────────────────────────────────────────────────────────────

    private fun scheduleDailyReminderInternal(hour: Int, minute: Int) {
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateInitialDelay(hour, minute), TimeUnit.MILLISECONDS)
            .setConstraints(defaultConstraints)
            .addTag(NotificationConstants.WORK_DAILY_REMINDER)
            .build()
        workManager.enqueueUniquePeriodicWork(
            NotificationConstants.WORK_DAILY_REMINDER,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun cancelDailyReminderInternal() { workManager.cancelUniqueWork(NotificationConstants.WORK_DAILY_REMINDER) }

    private fun scheduleBudgetChecker() {
        val request = PeriodicWorkRequestBuilder<BudgetAlertWorker>(6, TimeUnit.HOURS)
            .setConstraints(defaultConstraints)
            .addTag(NotificationConstants.WORK_BUDGET_CHECKER)
            .build()
        workManager.enqueueUniquePeriodicWork(
            NotificationConstants.WORK_BUDGET_CHECKER,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun scheduleMonthlySummary() {
        val request = PeriodicWorkRequestBuilder<MonthlySummaryWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateInitialDelay(hour = 1, minute = 5), TimeUnit.MILLISECONDS)
            .setConstraints(defaultConstraints)
            .addTag(NotificationConstants.WORK_MONTHLY_SUMMARY)
            .build()
        workManager.enqueueUniquePeriodicWork(
            NotificationConstants.WORK_MONTHLY_SUMMARY,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun cancelBudgetChecker() {
        workManager.cancelUniqueWork(NotificationConstants.WORK_BUDGET_CHECKER)
    }

    private fun cancelMonthlyNotifications() { workManager.cancelUniqueWork(NotificationConstants.WORK_MONTHLY_SUMMARY) }

    /**
     * Returns ms until the next occurrence of [hour]:[minute].
     * If that time has already passed today, targets tomorrow.
     */
    private fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }
}