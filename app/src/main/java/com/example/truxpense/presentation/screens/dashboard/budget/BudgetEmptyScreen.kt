package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.screens.dashboard.components.EmptyScreenContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetTopBar() {
    ScreenTopBar(headerTitle = "Budgets", showBack = false)
}

@Composable
fun BudgetsEmptyScreen(
    modifier: Modifier = Modifier, onAddBudget: () -> Unit = {}
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, topBar = { BudgetTopBar() }) { innerPadding ->

        EmptyScreenContent(
            modifier = modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            illustrationRes = R.drawable.budget_illustration,
            title = "Create budgets to stay in control",
            subtitle = "Set monthly limits for categories like Food, Travel, or Bills to track and manage your spending.",
            ctaText = "Add your first budget",
            onCta = onAddBudget,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BudgetsEmptyScreenPreview() {
    MaterialTheme {
        BudgetsEmptyScreen()
    }
}