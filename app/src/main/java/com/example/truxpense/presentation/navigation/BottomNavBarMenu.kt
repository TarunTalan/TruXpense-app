package com.example.truxpense.presentation.navigation

import androidx.annotation.DrawableRes
import com.example.truxpense.R

// Dashboard navigation destinations
sealed class BottomNavBarMenu(
    val route: String,
    val label: String,
    @DrawableRes val icon: Int,
    val selectedIcon: Int
) {
    object Home : BottomNavBarMenu(
        "dashboard/home", "Home", R.drawable.home_icon, R.drawable.home_icon_selected
    )

    object Transactions : BottomNavBarMenu(
        "dashboard/transactions", "Transaction", R.drawable.transaction_icon, R.drawable.transaction_selected_icon
    )

    object Budget : BottomNavBarMenu(
        "dashboard/budget", "Budget", R.drawable.budget_icon, R.drawable.budget_selected
    )

    object Analytics : BottomNavBarMenu(
        "dashboard/analytics", "Analytic", R.drawable.analytics_icon, R.drawable.analytic_selected
    )

    object Settings : BottomNavBarMenu(
        "dashboard/settings", "Settings", R.drawable.setting_icon, R.drawable.setting_selected
    )

    companion object {
        val all = listOf(Home, Transactions, Budget, Analytics, Settings)
    }
}
