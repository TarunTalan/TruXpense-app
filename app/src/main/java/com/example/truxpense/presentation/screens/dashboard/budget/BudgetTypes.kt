package com.example.truxpense.presentation.screens.dashboard.budget

// Small shared types used in Budget detail UI

enum class PeriodTab { WEEK, MONTH }

data class SpendPoint(val dayLabel: String, val amount: Double)

data class BudgetTransaction(
    val id: String,
    val amount: Double,
    val type: String,
    val addedFrom: String,
    val merchant: String,
    val category: String,
    val account: String,
    val date: String,
    val time: String,
)

