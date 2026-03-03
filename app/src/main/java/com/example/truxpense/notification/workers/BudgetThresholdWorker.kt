package com.example.truxpense.notification.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.truxpense.data.repository.budget.BudgetRepository
import com.example.truxpense.data.repository.expense.ExpenseRepository
import com.example.truxpense.notification.helper.NotificationHelper
import com.example.truxpense.notification.datastore.NotificationPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

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
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
                .build()

        /** One-shot request used for immediate checks (e.g. after adding an expense). */
        fun buildOneTimeRequest(): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<BudgetThresholdWorker>()
                .addTag(WORK_NAME)
                .build()
    }

    override suspend fun doWork(): Result {
        val settings = prefs.getSnapshot()          // fixed: now uses .first() properly
        if (!settings.budgetThresholdEnabled) return Result.success()

        val threshold    = settings.thresholdPercent / 100.0   // e.g. 0.90
        val yearMonth    = currentYearMonth()
        val alreadySent  = settings.notifiedBudgets

        val budgets      = budgetRepository.budgets.first()
        val transactions = expenseRepository.transactions.first()

        val currentMonthStart = monthStartMs()
        val spentByCategory = transactions
            .filter { it.timestamp >= currentMonthStart }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }

        budgets.forEachIndexed { index, budget ->
            val spent = spentByCategory[budget.category] ?: 0.0
            if (budget.amount <= 0) return@forEachIndexed

            val ratio = spent / budget.amount

            // ── Budget exceeded (100 %+) ──────────────────────────────────────
            if (ratio >= 1.0) {
                val exceededKey = "${budget.category}:exceeded:$yearMonth"
                if (exceededKey !in alreadySent) {
                    notificationHelper.notifyBudgetExceeded(
                        category = budget.category,
                        spent    = spent,
                        limit    = budget.amount,
                    )
                    prefs.markBudgetNotified(budget.category + ":exceeded", yearMonth)
                }
                return@forEachIndexed  // already exceeded — skip warning check
            }

            // ── Budget warning (>= threshold, e.g. 90 %) ─────────────────────
            if (ratio >= threshold) {
                val warningKey = "${budget.category}:warning:$yearMonth"
                if (warningKey !in alreadySent) {
                    val remaining = (budget.amount - spent).coerceAtLeast(0.0)
                    notificationHelper.showBudgetThresholdAlert(
                        categoryName  = budget.category,
                        percentUsed   = (ratio * 100).toInt(),
                        remaining     = formatINR(remaining),
                        categoryIndex = index,
                    )
                    prefs.markBudgetNotified(budget.category + ":warning", yearMonth)
                }
            }
        }

        return Result.success()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun currentYearMonth(): String {
        val cal = Calendar.getInstance()
        return "%d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    private fun monthStartMs(): Long =
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun formatINR(amount: Double): String = runCatching {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
    }.getOrDefault("₹${"%,.0f".format(amount)}")
}