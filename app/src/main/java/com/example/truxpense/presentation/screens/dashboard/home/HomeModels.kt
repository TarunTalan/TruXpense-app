package com.example.truxpense.presentation.screens.dashboard.home

import java.io.Serializable

// ── Transaction item shown in the Recent Transactions table ───────────────────

data class HomeTransactionItem(
    val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val currencyCode: String = "INR",
    val isExpense: Boolean = true,
) : Serializable

// ── Category spending summary (used for top-spending breakdown) ───────────────

data class HomeSpendingCategory(
    val name: String,
    val amount: Double,
    val progress: Float,
)

// ── Month-over-month spend change ─────────────────────────────────────────────

/**
 * @param percentChange  Positive = spending increased, negative = decreased.
 *                       Null when there is no previous-month data to compare.
 * @param prevMonthLabel Short label for the previous month, e.g. "Feb".
 */
data class MonthlyChangeData(
    val percentChange: Double?,
    val prevMonthLabel: String,
)

// ── Dynamic spending insight shown on the home screen ────────────────────────

/**
 * InsightTarget tells the UI what action to take when the insight CTA is tapped.
 */
sealed interface InsightTarget {
    object AnalyticsRoot : InsightTarget
    data class AnalyticsCategory(val category: String) : InsightTarget
    data class AnalyticsMerchant(val merchant: String) : InsightTarget
    data class TransactionDetail(val txId: String) : InsightTarget
    data class BudgetDetailByCategory(val category: String) : InsightTarget
}

data class SpendingInsight(
    /** Short one-liner shown as the card body. */
    val message: String,
    /** CTA text on the row beneath the message. */
    val actionText: String = "View analytics",
    /** Optional target describing what to open on CTA tap. */
    val target: InsightTarget = InsightTarget.AnalyticsRoot,
)
