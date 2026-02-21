package com.example.truxpense.presentation.screens.dashboard.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.SpendingCategoryCard
import com.example.truxpense.presentation.screens.onboarding.currency.CurrencyViewModel
import com.example.truxpense.util.currencyFormat
import com.example.truxpense.util.formatAmountParts
import com.example.truxpense.util.progressColor
import com.example.truxpense.util.toCurrency


@Composable
fun HomeTabScreen(
    username: String?,
    vm: HomeViewModel,
    onLogout: (() -> Unit)?,
    onAddExpense: (() -> Unit)? = null
) {
    val hasSmsPermission by vm.hasSmsPermission.collectAsState()
    LaunchedEffect(Unit) { vm.refreshSmsPermission() }

    val expenseCount by vm.expenseCount.collectAsState()
    val monthlySpend by vm.monthlySpend.collectAsState()
    val budgetLimit by vm.budgetLimit.collectAsState()
    val budgetLeft by vm.budgetLeft.collectAsState()

    val currencyViewModel: CurrencyViewModel = hiltViewModel()
    val currencyCode by remember {
        derivedStateOf { currencyViewModel.selectedCurrency.value?.code ?: "INR" }
    }

    if (expenseCount == 0) {
        EmptyHomeContent(
            onAddExpense = onAddExpense,
            onLogout = onLogout,
            hasSmsPermission = hasSmsPermission,
            username = username
        )
        return
    }

    HomeTabScreenContent(
        expenseCount = expenseCount,
        monthlySpend = monthlySpend,
        budgetLimit = budgetLimit,
        budgetLeft = budgetLeft,
        hasSmsPermission = hasSmsPermission,
        onAddExpense = onAddExpense,
        onLogout = onLogout,
        onSmsGranted = { vm.onSmsPermissionResult(true); vm.refreshSmsPermission() },
        username = username,
        currencyCode = currencyCode
    )
}

// ─── Main Content ─────────────────────────────────────────────────────────────

@Suppress("UNUSED_PARAMETER")
@Composable
fun HomeTabScreenContent(
    expenseCount: Int,
    monthlySpend: Double,
    budgetLimit: Double,
    budgetLeft: Double,
    hasSmsPermission: Boolean,
    onAddExpense: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onSmsGranted: (() -> Unit)? = null,
    username: String?,
    currencyCode: String? = "INR"
) {
    val fmt = remember(currencyCode) { currencyFormat(currencyCode) }
    val budgetUsed = (budgetLimit - budgetLeft).coerceAtLeast(0.0)
    val progress = if (budgetLimit > 0) (budgetUsed / budgetLimit).toFloat().coerceIn(0f, 1f) else 0f

    // Observe sample data from the HomeViewModel
    val vm: HomeViewModel = hiltViewModel()
    val topCategories by vm.topCategories.collectAsState()
    val recentTx by vm.recentTransactions.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { DashboardTopBar(username = username) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddExpense?.invoke() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add expense")
            }
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── SMS permission banner ─────────────────────────────────────
            if (!hasSmsPermission) {
                item {
                    SmsPermissionBanner(
                        modifier = Modifier.fillMaxWidth(),
                        onGranted = { onSmsGranted?.invoke() }
                    )
                }
            }

            // ── Monthly spend ─────────────────────────────────────────────
            item {
                SectionCard(title = "Spend this month") {
                    Text(
                        text = monthlySpend.toCurrency(fmt),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── Budget summary ────────────────────────────────────────────
            item {
                SectionCard(
                    title = "Budget",
                    trailingContent = {
                        FilledTonalButton(
                            onClick = {},
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Details", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                ) {
                    Text(
                        text = "${budgetLeft.toCurrency(fmt)} of ${budgetLimit.toCurrency(fmt)}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    BudgetProgressBar(progress = progress)
                }
            }

            // ── Insight nudge ─────────────────────────────────────────────
            item {
                InsightCard(
                    message = "You spent more on food this week than usual",
                    actionText = "Consider setting a weekly limit",
                    onAction = { /* navigate */ }
                )
            }

            // ── Top spending categories ───────────────────────────────────
            item {
                SectionHeader(text = "Highest spending categories this month")
            }

            items(topCategories, key = { it.name }) { category ->
                SpendingCategoryCard(
                    category = category,
                    fmt = fmt,
                    errorColor = MaterialTheme.colorScheme.error
                )
            }

            // ── Recent transactions ───────────────────────────────────────
            item {
                RecentTransactionsCard(transactions = recentTx)
            }

            item { Spacer(Modifier.height(72.dp)) } // FAB clearance
        }
    }
}


/** Consistent card shell with optional title row and trailing slot. */
@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    title: String,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                trailingContent?.invoke()
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

/** Animated linear progress bar with % label. */
@Composable
fun BudgetProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val color = progressColor(progress, MaterialTheme.colorScheme.error)
    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = color,
            trackColor = Color(0xFFD9DEE3),
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${(progress * 100).toInt()}% used",
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}


/** Insight nudge card. */
@Composable
private fun InsightCard(
    message: String,
    actionText: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

// Recent Transactions

@Composable
fun RecentTransactionsCard(
    modifier: Modifier = Modifier,
    transactions: List<HomeTransactionItem>
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {

            // Header row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent transactions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "View all",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Column labels
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TxColumnLabel("Transaction", Modifier.weight(1f), TextAlign.Start)
                TxColumnLabel("Category", Modifier.weight(1f), TextAlign.Center)
                TxColumnLabel("Amount", Modifier.weight(1f), TextAlign.End)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val display = transactions.take(3)
                display.forEachIndexed { index, tx ->
                    TransactionRow(tx = tx)
                    if (index < display.lastIndex)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun TxColumnLabel(text: String, modifier: Modifier, align: TextAlign) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align
    )
}

@Composable
private fun TransactionRow(tx: HomeTransactionItem) {
    val (prefix, numeric, suffix) = formatAmountParts(tx.amount, tx.currencyCode)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = tx.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = tx.category,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (prefix.isNotEmpty()) {
                Text(prefix, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(2.dp))
            }
            Text(
                text = numeric,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (suffix.isNotEmpty()) {
                Spacer(Modifier.width(4.dp))
                Text(suffix, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}


@Composable
fun EmptyHomeContent(
    onAddExpense: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    hasSmsPermission: Boolean,
    onSmsGranted: (() -> Unit)? = null,
    username: String? = null
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { DashboardTopBar(username = username) }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            // ── SMS Banner ────────────────────────────────────────────────
            if (!hasSmsPermission) {
                SmsPermissionBanner(
                    modifier = Modifier.fillMaxWidth(),
                    onGranted = { onSmsGranted?.invoke() }
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── Centered body ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.illustration_home),
                    contentDescription = "Home illustration",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.45f)
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Start tracking your spending",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "We'll automatically track expenses from SMS, or you can add them manually.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // ── Bottom actions ────────────────────────────────────────────
            Button(
                onClick = { onAddExpense?.invoke() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("+ Add your first expense")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeTabScreenPreview() {
    MaterialTheme {
        HomeTabScreenContent(
            expenseCount = 3,
            monthlySpend = 1234.56,
            budgetLimit = 10000.0,
            budgetLeft = 5123.45,
            hasSmsPermission = false,
            onAddExpense = {},
            onLogout = {},
            onSmsGranted = {},
            username = "Tarun",
            currencyCode = "INR"
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EmptyHomeTabScreenPreview() {
    MaterialTheme {
        EmptyHomeContent(
            onAddExpense = {},
            onLogout = {},
            hasSmsPermission = false,
            onSmsGranted = {},
            username = "Tarun"
        )
    }
}