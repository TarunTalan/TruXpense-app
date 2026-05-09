package com.example.truxpense.presentation.screens.dashboard.transaction


enum class TransactionPeriod(val label: String) {
    WEEK("week"), MONTH("Month"), YEAR("Year"),
}

enum class TransactionFilter(val label: String) {
    CATEGORY("Category"), ACCOUNT("Account"),
}

/** Distinguishes expense vs income entries in the unified feed. */
enum class EntryType { EXPENSE, INCOME }

// ─── UI Models ────────────────────────────────────────────────────────────────

data class TransactionItem(
    val id: String,
    val merchant: String,        // merchant name OR income source
    val category: String,        // expense category OR income source label
    val timeLabel: String,       // e.g. "Yesterday", "Mar 6"
    val amount: Double,          // always positive; sign shown by entryType
    val paymentMethod: String,   // e.g. "UPI", "Card", "" for income
    val entryType: EntryType = EntryType.EXPENSE,
    val timestamp: Long = 0L,
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
    val notes: String,
)

data class TransactionDayGroup(
    val dayLabel: String,        // e.g. "Feb 1"
    val items: List<TransactionItem>,
)

data class TransactionMonthGroup(
    val monthLabel: String,      // e.g. "Feb 2026"
    val totalExpense: Double,
    val totalIncome: Double,
    val days: List<TransactionDayGroup>,
) {
    val net: Double get() = totalIncome - totalExpense
}

// ─── Sample data ─────────────────────────────────────────────────────────────

private fun dayGroup(day: String) = TransactionDayGroup(
    dayLabel = day,
    items = listOf(
        TransactionItem("$day-1", "Zomato", "Food", day, 500.0, "UPI", EntryType.EXPENSE),
        TransactionItem("$day-2", "Uber", "Transport", day, 450.0, "UPI", EntryType.EXPENSE),
        TransactionItem("$day-3", "Salary", "Salary", day, 50000.0, "", EntryType.INCOME),
    ),
)

val sampleMonthGroups = listOf(
    TransactionMonthGroup(
        monthLabel = "Feb 2026",
        totalExpense = 25_000.0,
        totalIncome = 50_000.0,
        days = listOf(
            dayGroup("Feb 1"),
            dayGroup("Feb 2"),
            dayGroup("Feb 3"),
            dayGroup("Feb 4"),
        ),
    ),
)