package com.example.truxpense.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.truxpense.data.repository.dashboard.BudgetRepository
import com.example.truxpense.data.repository.dashboard.ExpenseRepository
import com.example.truxpense.notification.NotificationHelper
import com.example.truxpense.notification.NotificationPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Runs every 6 hours and checks whether any budget category has met or exceeded
 * the user's configured alert threshold (default 90 %).
 *
 * Deduplication
 * ─────────────
 * Once a notification is sent for a category in a given month, the category is
 * recorded in [NotificationPreferences.notifiedBudgets] ("Category:YYYY-MM").
 * The worker skips already-notified entries, so the user sees at most one alert
 * per category per calendar month.
 *
 * The dedup set is cleared by [MonthlyBudgetResetWorker] at the start of each
 * new month, allowing alerts to fire again.
 */
@HiltWorker
class BudgetThresholdWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val notificationHelper: NotificationHelper,
    private val prefs: NotificationPreferences,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "budget_threshold_check"

        fun buildRequest(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<BudgetThresholdWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .addTag(WORK_NAME)
                // Back-off: retry after 15 min if the worker fails (e.g. DB not ready)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
                .build()
    }

    override suspend fun doWork(): Result {
        val settings = prefs.getSnapshot()
        if (!settings.budgetThresholdEnabled) return Result.success()

        val threshold   = settings.thresholdPercent / 100.0
        val yearMonth   = currentYearMonth()
        val alreadySent = settings.notifiedBudgets  // e.g. {"Food:2026-03"}

        // One-shot reads from Room (Flow.first())
        val budgets      = budgetRepository.budgets.first()
        val transactions = expenseRepository.transactions.first()

        // Group total spending per category for the current month
        val currentMonthStart = monthStartMs()
        val spentByCategory = transactions
            .filter { it.timestamp >= currentMonthStart }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }

        budgets.forEachIndexed { index, budget ->
            val spent = spentByCategory[budget.category] ?: 0.0
            if (budget.amount <= 0) return@forEachIndexed

            val ratio = spent / budget.amount
            val key   = "${budget.category}:$yearMonth"

            // Skip if already notified this month OR below threshold
            if (ratio < threshold || key in alreadySent) return@forEachIndexed

            val remaining = (budget.amount - spent).coerceAtLeast(0.0)
            notificationHelper.showBudgetThresholdAlert(
                categoryName   = budget.category,
                percentUsed    = (ratio * 100).toInt(),
                remaining      = formatINR(remaining),
                categoryIndex  = index,
            )
            prefs.markBudgetNotified(budget.category, yearMonth)
        }

        return Result.success()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** "2026-03" */
    private fun currentYearMonth(): String {
        val cal = Calendar.getInstance()
        return "%d-%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
        )
    }

    /** Unix ms of the first millisecond of the current calendar month. */
    private fun monthStartMs(): Long =
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun formatINR(amount: Double): String = runCatching {
        val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        fmt.format(amount)
    }.getOrDefault("₹${"%,.0f".format(amount)}")
}