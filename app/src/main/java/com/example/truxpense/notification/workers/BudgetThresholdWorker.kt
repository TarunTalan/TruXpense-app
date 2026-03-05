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
        /** When true the immediate one-shot check skips the monthly-dedup guard
         *  so the notification always fires if the threshold is met right now. */
        const val KEY_FORCE = "force_check"

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

        /** One-shot request used for immediate checks (e.g. after adding an expense).
         *  Passes FORCE=true so the dedup guard is bypassed for this run. */
        fun buildOneTimeRequest(): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<BudgetThresholdWorker>()
                .setInputData(workDataOf(KEY_FORCE to true))
                .addTag(WORK_NAME)
                .build()
    }

    override suspend fun doWork(): Result {
        val settings  = prefs.getSnapshot()
        val forceCheck = inputData.getBoolean(KEY_FORCE, false)

        // ── Budget threshold alerts ───────────────────────────────────────────
        if (settings.budgetThresholdEnabled) {
            val useCustomLimit = settings.budgetAlertCustomLimitEnabled
            val customLimitINR = settings.budgetAlertCustomLimit.toDouble()
            val thresholdRatio = settings.thresholdPercent / 100.0
            val yearMonth      = currentYearMonth()
            val alreadySent    = settings.notifiedBudgets

            val budgets      = budgetRepository.budgets.first()
            val transactions = expenseRepository.transactions.first()

            val currentMonthStart = monthStartMs()
            val spentByCategory = transactions
                .filter { it.timestamp >= currentMonthStart }
                .groupBy { it.category }
                .mapValues { (_, txs) -> txs.sumOf { it.amount } }

            budgets.forEachIndexed { index, budget ->
                val spent     = spentByCategory[budget.category] ?: 0.0
                val remaining = (budget.amount - spent).coerceAtLeast(0.0)
                if (budget.amount <= 0) return@forEachIndexed

                val ratio = spent / budget.amount

                // ── Budget exceeded (100%+) ───────────────────────────────────
                if (ratio >= 1.0) {
                    val exceededKey = "${budget.category}:exceeded:$yearMonth"
                    // On force-check: fire even if already sent once this month
                    if (forceCheck || exceededKey !in alreadySent) {
                        notificationHelper.notifyBudgetExceeded(
                            category = budget.category,
                            spent    = spent,
                            limit    = budget.amount,
                        )
                        if (!forceCheck) {
                            prefs.markBudgetNotified(budget.category + ":exceeded", yearMonth)
                        }
                    }
                    return@forEachIndexed
                }

                // ── Warning threshold ─────────────────────────────────────────
                val shouldWarn = if (useCustomLimit) {
                    remaining <= customLimitINR
                } else {
                    ratio >= thresholdRatio
                }

                if (shouldWarn) {
                    val warningKey = "${budget.category}:warning:$yearMonth"
                    if (forceCheck || warningKey !in alreadySent) {
                        notificationHelper.showBudgetThresholdAlert(
                            categoryName  = budget.category,
                            percentUsed   = (ratio * 100).toInt(),
                            remaining     = formatINR(remaining),
                            categoryIndex = index,
                        )
                        if (!forceCheck) {
                            prefs.markBudgetNotified(budget.category + ":warning", yearMonth)
                        }
                    }
                }
            }
        }

        // ── Unusual spending detection ────────────────────────────────────────
        if (settings.unusualSpendingEnabled) {
            checkUnusualSpending()
        }

        return Result.success()
    }

    /**
     * Compares this week's daily average spend to the previous 3-week average.
     * Fires a notification if today's spend is > 2× the historical average and
     * at least ₹200 more (to avoid noise on tiny amounts).
     */
    private suspend fun checkUnusualSpending() {
        val transactions = expenseRepository.transactions.first()
        val now          = Calendar.getInstance()

        // Today's total spend
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todaySpend = transactions
            .filter { it.timestamp >= todayStart }
            .sumOf { it.amount }

        // Previous 21-day average daily spend (baseline)
        val baselineEnd   = todayStart - 1L
        val baselineStart = baselineEnd - (21L * 24 * 60 * 60 * 1000)
        val baselineSpend = transactions
            .filter { it.timestamp in baselineStart..baselineEnd }
            .sumOf { it.amount }
        val baselineDailyAvg = baselineSpend / 21.0

        // Only notify if there is meaningful baseline data and today is a spike
        if (baselineDailyAvg < 50.0) return // not enough history
        if (todaySpend >= baselineDailyAvg * 2.0 && todaySpend - baselineDailyAvg >= 200.0) {
            val yearMonthDay = "%d-%02d-%02d".format(
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH),
            )
            val alreadySent = prefs.getSnapshot().notifiedBudgets
            val unusualKey  = "unusual:$yearMonthDay"
            if (unusualKey !in alreadySent) {
                notificationHelper.notifyUnusualSpending(
                    todaySpend         = todaySpend,
                    baselineDailyAvg   = baselineDailyAvg,
                )
                prefs.markBudgetNotified("unusual", yearMonthDay)
            }
        }
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
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).format(amount)
    }.getOrDefault("₹${"%,.0f".format(amount)}")
}
