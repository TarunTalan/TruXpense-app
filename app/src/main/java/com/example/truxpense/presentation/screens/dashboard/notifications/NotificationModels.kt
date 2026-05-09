package com.example.truxpense.presentation.screens.dashboard.notifications

// Models used by NotificationScreen and repository

sealed class NotificationDestination {
    data class BudgetDetail(val budgetName: String, val monthlyLimit: Double, val spent: Double) : NotificationDestination()
    object WeeklyAnalytics : NotificationDestination()
    data class TransactionDetail(val transactionId: String) : NotificationDestination()
    object AddExpense : NotificationDestination()
    object TransactionList : NotificationDestination()
}

enum class NotificationIconType {
    BUDGET_EXCEEDED,
    BUDGET_WARNING,
    SPENDING_INSIGHT,
    ADD_EXPENSE_PROMPT,
    SYNC_SUCCESS
}

data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val timeLabel: String,
    val timeMs: Long = 0L,
    val isRead: Boolean = false,
    val iconType: NotificationIconType = NotificationIconType.SPENDING_INSIGHT,
    val destination: NotificationDestination = NotificationDestination.AddExpense
)
