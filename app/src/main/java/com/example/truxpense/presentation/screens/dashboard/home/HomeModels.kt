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