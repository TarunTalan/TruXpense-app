package com.example.truxpense.data.repository.report

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// ── Report type ───────────────────────────────────────────────────────────────

enum class ReportType { EXPENSE, INCOME, ALL }

// ── Room entity ───────────────────────────────────────────────────────────────

/**
 * A saved report configuration.
 *
 * [categories] stores a pipe-separated list of category names, e.g. "Food|Travel|Bills".
 * An empty string means "all categories".
 */
@Entity(tableName = "reports")
data class Report(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val fromDate: Long,
    val toDate: Long,
    val reportType: String = ReportType.EXPENSE.name,   // serialised as String for Room
    val categories: String = "",                         // "" → all; "Food|Travel" → subset
) {
    // ── Convenience accessors ─────────────────────────────────────────────────

    fun parsedType(): ReportType =
        runCatching { ReportType.valueOf(reportType) }.getOrDefault(ReportType.EXPENSE)

    fun parsedCategories(): List<String> =
        if (categories.isBlank()) emptyList()
        else categories.split("|").map { it.trim() }.filter { it.isNotEmpty() }
}

// ── UI / presentation models ──────────────────────────────────────────────────

/** Row shown in a report list (e.g. "All Reports" screen). */
data class ReportSummaryItem(
    val id: String,
    val title: String,
    val dateRange: String,          // e.g. "1 Jan – 31 Jan 2026"
    val reportType: ReportType,
    val totalFormatted: String,     // e.g. "₹12,400"
    val createdAt: Long,
)

/** Category-level spend/income breakdown inside a report. */
data class ReportCategoryRow(
    val name: String,
    val amount: Double,
    val share: Float,               // 0f–1f fraction of total
    val color: androidx.compose.ui.graphics.Color,
)

/** A single data point on the report's trend chart. */
data class ReportTrendPoint(
    val label: String,              // "Mon", "W1", "Jan", etc.
    val amount: Double,
    val tooltipDate: String = label,
)

/** A transaction row inside a report. */
data class ReportTransactionRow(
    val id: String,
    val merchant: String,
    val category: String,
    val amount: Double,
    val isExpense: Boolean,
    val dateLabel: String,          // e.g. "14 Feb 2026"
    val timeLabel: String,          // e.g. "3:42 PM"
    val paymentMethod: String,
)