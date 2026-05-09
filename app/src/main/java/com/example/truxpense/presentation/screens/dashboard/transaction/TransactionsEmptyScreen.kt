package com.example.truxpense.presentation.screens.dashboard.transaction

import androidx.compose.foundation.layout.*
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
fun TransactionsTopBar() {
    ScreenTopBar(headerTitle = "Transactions", showBack = false)
}

@Composable
fun TransactionsEmptyContent(
    modifier: Modifier = Modifier,
    onAddTransaction: () -> Unit = {}
) {
    EmptyScreenContent(
        modifier = modifier.fillMaxSize(),
        illustrationRes = R.drawable.transaction_illuustraton,
        title = "No transactions yet",
        subtitle = "Add your first transactions to see insights and summaries.",
        ctaText = "Add your first transaction",
        onCta = onAddTransaction,
    )
}

@Composable
fun TransactionsEmptyScreen(
    modifier: Modifier = Modifier,
    onAddTransaction: () -> Unit = {}
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, topBar = { TransactionsTopBar() }) { innerPadding ->

        TransactionsEmptyContent(
            modifier = modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            onAddTransaction = onAddTransaction,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TransactionsEmptyScreenPreview() {
    MaterialTheme {
        TransactionsEmptyScreen()
    }
}
