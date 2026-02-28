package com.example.truxpense.presentation.screens.dashboard.analytics

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.EmptyScreenContent
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar

@Composable
fun AnalyticsTopBar() {
    ScreenTopBar(headerTitle = "Analytics", showBack = false)
}

@Composable
fun AnalyticsEmptyScreen(
    modifier: Modifier = Modifier, onAddExpense: () -> Unit = {}
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, topBar = { AnalyticsTopBar() }) { innerPadding ->

        EmptyScreenContent(
            modifier = modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            illustrationRes = R.drawable.analytic_illustration,
            title = "Not enough data yet",
            subtitle = "We'll show you spending insights once we have more transactions. This usually happens after a few days of use.",
            ctaText = "Add transaction",
            onCta = onAddExpense,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AnalyticsEmptyScreenPreview() {
    MaterialTheme {
        AnalyticsEmptyScreen()
    }
}