package com.example.truxpense.presentation.screens.dashboard.home

import java.io.Serializable

// Simple UI models used by Home screen
data class HomeTransactionItem(
    val id: String, val title: String, val category: String, val amount: Double, val currencyCode: String = "INR"
) : Serializable

data class HomeSpendingCategory(
    val name: String,
    val amount: Double,
    val progress: Float,
)
