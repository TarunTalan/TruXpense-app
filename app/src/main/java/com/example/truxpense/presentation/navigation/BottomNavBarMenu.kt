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
        Screen.Dashboard.Home.Root, "Home", R.drawable.home_icon, R.drawable.home_icon_selected
    )

    object Transactions : BottomNavBarMenu(
        Screen.Dashboard.Transactions.Root, "Transaction", R.drawable.transaction_icon, R.drawable.transaction_selected_icon
    )

    object Budget : BottomNavBarMenu(
        Screen.Dashboard.Budget.Root, "Budget", R.drawable.budget_icon, R.drawable.budget_selected
    )

    object Analytics : BottomNavBarMenu(
        Screen.Dashboard.Analytics.Root, "Analytic", R.drawable.analytics_icon, R.drawable.analytic_selected
    )

    object Settings : BottomNavBarMenu(
        Screen.Dashboard.Settings.Root, "Settings", R.drawable.setting_icon, R.drawable.setting_selected
    )

    companion object {
        val all = listOf(Home, Transactions, Budget, Analytics, Settings)
    }
}
