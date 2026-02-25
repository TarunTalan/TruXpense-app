package com.example.truxpense.presentation.screens.dashboard.transaction


enum class TransactionPeriod(val label: String) {
    WEEK("week"), MONTH("Month"), YEAR("Year"),
}

enum class TransactionFilter(val label: String) {
    CATEGORY("Category"), ACCOUNT("Account"),
}

// ─── UI Models ────────────────────────────────────────────────────────────────

data class TransactionItem(
    val id: String,
    val merchant: String,
    val category: String,
    val timeLabel: String,       // e.g. "Yesterday", "2 days ago"
    val amount: Double,          // negative = expense, positive = income
    val paymentMethod: String,   // e.g. "UPI", "Card", "Cash"
)

/**
 * Full transaction model used by [TransactionDetailScreen].
 * Carries every field shown in the detail view.
 */
data class TransactionDetail(
    val id: String,
    val merchant: String,
    val category: String,
    val account: String,         // e.g. "HDFC Bank", "Cash"
    val date: String,            // e.g. "12 Feb 2026"
    val time: String,            // e.g. "8:45 PM"
    val amount: Double,          // negative = expense, positive = income
    val type: String,            // "Expense" | "Income"
    val source: String,          // "Detected from SMS" | "Added manually"
    val paymentMethod: String,   // e.g. "UPI", "Card"
    val notes: String,
)

data class TransactionDayGroup(
    val dayLabel: String,        // e.g. "Feb 1"
    val items: List<TransactionItem>,
)

data class TransactionMonthGroup(
    val monthLabel: String,      // e.g. "Feb2026"
    val totalSpent: Double,
    val days: List<TransactionDayGroup>,
)

// ─── Sample data ─────────────────────────────────────────────────────────────

private fun dayGroup(day: String) = TransactionDayGroup(
    dayLabel = day,
    items = listOf(
        TransactionItem("$day-1", "Zomato", "Food", "Yesterday", -500.0, "UPI"),
        TransactionItem("$day-2", "Uber", "Transport", "Yesterday", -450.0, "UPI"),
        TransactionItem("$day-3", "Zepto", "Groceries", "Yesterday", -350.0, "UPI"),
    ),
)

val sampleMonthGroups = listOf(
    TransactionMonthGroup(
        monthLabel = "Feb2026",
        totalSpent = 25_000.0,
        days = listOf(
            dayGroup("Feb 1"),
            dayGroup("Feb2"),
            dayGroup("Feb3"),
            dayGroup("Feb4"),
        ),
    ),
)