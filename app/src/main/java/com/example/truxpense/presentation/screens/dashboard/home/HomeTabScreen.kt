package com.example.truxpense.presentation.screens.dashboard.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.presentation.screens.dashboard.components.DashboardTopBar
import com.example.truxpense.presentation.screens.dashboard.components.SmsPermissionBanner
import com.example.truxpense.presentation.screens.dashboard.components.SpendingCategoryCard
import com.example.truxpense.presentation.screens.dashboard.theme.DashboardDimens
import com.example.truxpense.presentation.screens.onboarding.currency.CurrencyViewModel
import com.example.truxpense.util.currencyFormat
import com.example.truxpense.util.formatAmountParts
import com.example.truxpense.util.progressColor
import com.example.truxpense.util.toCurrency
import com.example.truxpense.presentation.screens.dashboard.budget.BudgetViewModel


@Composable
fun HomeTabScreen(
    username: String?,
    vm: HomeViewModel,
    onLogout: (() -> Unit)?,
    onAddExpense: (() -> Unit)? = null,
    onNavigateToBudget: (() -> Unit)? = null,
) {
    // Keep empty/content decision based on expenseCount (VM-driven)
    val hasSmsPermission by vm.hasSmsPermission.collectAsState()
    LaunchedEffect(Unit) { vm.refreshSmsPermission() }

    val expenseCount by vm.expenseCount.collectAsState()
    val monthlySpend by vm.monthlySpend.collectAsState()
    // val budgetLimit by vm.budgetLimit.collectAsState()
    // val budgetLeft by vm.budgetLeft.collectAsState()
    // val budgetProgress by vm.budgetProgress.collectAsState()   // ← from VM, was computed here

    val currencyVm: CurrencyViewModel = hiltViewModel()
    val currencyCode by remember {
        derivedStateOf { currencyVm.selectedCurrency.value?.code ?: "INR" }
    }

    if (expenseCount == 0) {
        EmptyHomeContent(
            onAddExpense = onAddExpense,
            onLogout = onLogout,
            hasSmsPermission = hasSmsPermission,
            username = username,
        )
        return
    }

    HomeTabContent(
        monthlySpend = monthlySpend,
        hasSmsPermission = hasSmsPermission,
        onAddExpense = onAddExpense,
        onSmsGranted = { vm.onSmsPermissionResult(true); vm.refreshSmsPermission() },
        username = username,
        currencyCode = currencyCode,
        vm = vm,
        onNavigateToBudget = onNavigateToBudget,
    )
}


@Composable
fun HomeTabContent(
    monthlySpend: Double,
    hasSmsPermission: Boolean,
    onAddExpense: (() -> Unit)? = null,
    onSmsGranted: (() -> Unit)? = null,
    username: String?,
    currencyCode: String = "INR",
    vm: HomeViewModel = hiltViewModel(),
    onNavigateToBudget: (() -> Unit)? = null,
) {
    val fmt = remember(currencyCode) { currencyFormat(currencyCode) }
    val topCategories by vm.topCategories.collectAsState(initial = emptyList())
    val recentTx by vm.recentTransactions.collectAsState(initial = emptyList<HomeTransactionItem>())
    // Budget VM for real budget data
    val budgetVm: BudgetViewModel = hiltViewModel()
    val budgetItems by budgetVm.categoryDisplayItems.collectAsState(initial = emptyList())
    val totalBudget by budgetVm.totalBudget.collectAsState()
    val totalSpentInBudgets by budgetVm.totalSpent.collectAsState()
    // compute overall progress as Float (avoid using remember inside LazyColumn DSL)
    val overallProgress: Float = if (totalBudget > 0) (totalSpentInBudgets.toFloat() / totalBudget.toFloat()).coerceIn(0f, 1f) else 0f

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
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = DashboardDimens.screenPaddingH,
                vertical = DashboardDimens.spaceLg,
            ),
            verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg),
        ) {

            // SMS permission banner
            if (!hasSmsPermission) {
                item {
                    SmsPermissionBanner(
                        modifier = Modifier.fillMaxWidth(),
                        onGranted = { onSmsGranted?.invoke() },
                    )
                }
            }

            // Monthly spend
            item {
                SectionCard(title = "Spend this month") {
                    Text(
                        text = monthlySpend.toCurrency(fmt),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Budget summary — show only when user has created budgets
            if (budgetItems.isNotEmpty()) {
                // overall budget progress computed from BudgetViewModel totals
                item {
                    SectionCard(
                        title = "Budget",
                        trailingContent = {
                            FilledTonalButton(
                                onClick = { onNavigateToBudget?.invoke() },
                                modifier = Modifier.height(DashboardDimens.buttonHeightSm),
                                contentPadding = PaddingValues(horizontal = DashboardDimens.spaceLg),
                            ) {
                                Text("Details", style = MaterialTheme.typography.labelMedium)
                            }
                        },
                    ) {
                        Text(
                            text = "${(totalBudget.toDouble() - totalSpentInBudgets.toDouble()).toCurrency(fmt)} of ${totalBudget.toDouble().toCurrency(fmt)}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(DashboardDimens.spaceMdL))
                        BudgetProgressBar(progress = overallProgress)
                    }
                }
            }

            // Insight nudge
            item {
                InsightCard(
                    message = "You spent more on food this week than usual",
                    actionText = "Consider setting a weekly limit",
                    onAction = { /* navigate */ },
                )
            }

            // Top spending categories
            item {
                SectionHeader(text = "Highest spending categories this month")
            }

            items(topCategories, key = { it.name }) { category ->
                SpendingCategoryCard(
                    category = category,
                    fmt = fmt,
                    errorColor = MaterialTheme.colorScheme.error,
                )
            }

            // Recent transactions
            item {
                RecentTransactionsCard(transactions = recentTx)
            }

            // FAB clearance — last item never hidden behind the FAB
            item { Spacer(Modifier.height(DashboardDimens.fabClearance)) }
        }
    }
}

// Section card

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    title: String,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                trailingContent?.invoke()
            }
            Spacer(Modifier.height(DashboardDimens.spaceMd))
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
        modifier = Modifier.padding(vertical = DashboardDimens.spaceXs),
    )
}

// Budget progress bar

@Composable
fun BudgetProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val color = progressColor(progress, MaterialTheme.colorScheme.error)
    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(DashboardDimens.progressBarHeight2),
            color = color,
            trackColor = Color(0xFFD9DEE3),
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )
        Spacer(Modifier.height(DashboardDimens.spaceXs))
        Text(
            text = "${(progress * 100).toInt()}% used",
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}

// Insight nudge card

@Composable
private fun InsightCard(
    message: String,
    actionText: String,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPadding)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(DashboardDimens.spaceSm))
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}

// Recent transactions card

@Composable
fun RecentTransactionsCard(
    modifier: Modifier = Modifier,
    transactions: List<HomeTransactionItem>,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = DashboardDimens.spaceXs)) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(
                        horizontal = DashboardDimens.screenPaddingH,
                        vertical = DashboardDimens.spaceLg,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent transactions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "View all",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Column labels
            Row(
                modifier = Modifier.fillMaxWidth().padding(
                        horizontal = DashboardDimens.screenPaddingH,
                        vertical = DashboardDimens.spaceMd,
                    ),
                horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
            ) {
                TxColumnLabel("Transaction", Modifier.weight(1f), TextAlign.Start)
                TxColumnLabel("Category", Modifier.weight(1f), TextAlign.Center)
                TxColumnLabel("Amount", Modifier.weight(1f), TextAlign.End)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(DashboardDimens.screenPaddingH),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No recent transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val display = transactions.take(3)
                display.forEachIndexed { index, tx ->
                    TransactionRow(tx = tx)
                    if (index < display.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(DashboardDimens.spaceXs))
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
        textAlign = align,
    )
}

@Composable
private fun TransactionRow(tx: HomeTransactionItem) {
    val (prefix, numeric, suffix) = formatAmountParts(tx.amount, tx.currencyCode)
    Row(
        modifier = Modifier.fillMaxWidth().padding(
                horizontal = DashboardDimens.screenPaddingH,
                vertical = DashboardDimens.spaceLg,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
    ) {
        Text(
            text = tx.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = tx.category,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (prefix.isNotEmpty()) {
                Text(prefix, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(DashboardDimens.spaceXxs))
            }
            Text(
                text = numeric,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (suffix.isNotEmpty()) {
                Spacer(Modifier.width(DashboardDimens.spaceXs))
                Text(suffix, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// Preview

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeTabScreenPreview() {
    MaterialTheme {
        HomeTabContent(
            monthlySpend = 1_234.56,
            hasSmsPermission = false,
            onAddExpense = {},
            onSmsGranted = {},
            username = "Tarun",
            currencyCode = "INR",
        )
    }
}