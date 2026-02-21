package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.compose.ui.graphics.Color

// UI model used by Budget screens
data class BudgetCategory(
    val id: Int,
    val name: String,
    val spent: Int,
    val total: Int,
    val barColor: Color
)

fun budgetColorForCategory(category: String): Color {
    return when (category.trim().lowercase()) {
        "food" -> Color(0xFFEF4444)
        "transport" -> Color(0xFF14B8A6)
        "shopping" -> Color(0xFFF59E0B)
        "bills" -> Color(0xFFEF4444)
        "entertainment" -> Color(0xFF8B5CF6)
        "health" -> Color(0xFF10B981)
        "education" -> Color(0xFFEF4444)
        "groceries" -> Color(0xFF3B82F6)
        "dining out" -> Color(0xFFF97316)
        else -> Color(0xFF6B7280)
    }
}
