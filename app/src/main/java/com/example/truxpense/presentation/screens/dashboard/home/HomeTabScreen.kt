package com.example.truxpense.presentation.screens.dashboard.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.presentation.screens.dashboard.budget.BudgetViewModel
import com.example.truxpense.presentation.screens.dashboard.components.*
import com.example.truxpense.presentation.screens.dashboard.theme.DashboardDimens
import com.example.truxpense.presentation.screens.onboarding.currency.CurrencyViewModel
import com.example.truxpense.util.currencyFormat
import com.example.truxpense.util.formatAmountParts
import com.example.truxpense.util.toCurrency
import com.example.truxpense.presentation.screens.dashboard.notifications.NotificationViewModel


@Composable
fun HomeTabScreen(
    vm: HomeViewModel,
    onAddExpense: (() -> Unit)? = null,
    onNavigateToBudget: (() -> Unit)? = null,
    onViewAll: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null,
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
            hasSmsPermission = hasSmsPermission,
        )
        return
    }

    HomeTabContent(
        monthlySpend = monthlySpend,
        hasSmsPermission = hasSmsPermission,
        onAddExpense = onAddExpense,
        onSmsGranted = { vm.onSmsPermissionResult(true); vm.refreshSmsPermission() },
        currencyCode = currencyCode,
        vm = vm,
        onNavigateToBudget = onNavigateToBudget,
        onViewAll = onViewAll,
        onNotificationsClick = onNotificationsClick,
    )
}


@Composable
fun HomeTabContent(
    monthlySpend: Double,
    hasSmsPermission: Boolean,
    onAddExpense: (() -> Unit)? = null,
    onSmsGranted: (() -> Unit)? = null,
    currencyCode: String = "INR",
    vm: HomeViewModel = hiltViewModel(),
    onNavigateToBudget: (() -> Unit)? = null,
    onViewAll: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null,
) {
    val fmt = remember(currencyCode) { currencyFormat(currencyCode) }
    val notificationVm: NotificationViewModel = hiltViewModel()
    val unreadCount by notificationVm.unreadCount.collectAsState()
    val topCategories by vm.topCategories.collectAsState(initial = emptyList())
    val recentTx by vm.recentTransactions.collectAsState(initial = emptyList<HomeTransactionItem>())
    // Budget VM for real budget data
    val budgetVm: BudgetViewModel = hiltViewModel()
    val budgetItems by budgetVm.categoryDisplayItems.collectAsState(initial = emptyList())
    val hasBudgets by budgetVm.hasBudgets.collectAsState(initial = false)
    val totalBudget by budgetVm.totalBudget.collectAsState()
    val totalSpentInBudgets by budgetVm.totalSpent.collectAsState()
    // compute overall progress as Float only when budgets exist
    val overallProgress: Float = if (hasBudgets) {
        if (totalBudget > 0) (totalSpentInBudgets.toFloat() / totalBudget.toFloat()).coerceIn(0f, 1f) else 0f
    } else 0f

    // One-shot progress bar fill — fires once when this screen is navigated to
    var progTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { progTriggered = true }
    val progMultiplier by animateFloatAsState(
        targetValue = if (progTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "home_progress",
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                headerTitle = "TruXpense",
                showBack = false,
                showProfileIcons = true,
                onNotificationsClick = { onNotificationsClick?.invoke() },
                unreadCount = unreadCount
            )
        },
        floatingActionButton = {
            AddFab(onClick = { onAddExpense?.invoke() })
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(
                start = DashboardDimens.screenPaddingH,
                end = DashboardDimens.screenPaddingH,
                top = DashboardDimens.spaceLg,
                bottom = DashboardDimens.spaceLg,
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
            if (hasBudgets && budgetItems.isNotEmpty()) {
                // overall budget progress computed from BudgetViewModel totals
                item {
                    SectionCard(
                        title = "Budget",
                        trailingContent = {
                            // Use `secondary` as background color in dark mode, keep primaryContainer in light mode.
                            val detailContainer =
                                if (isSystemInDarkTheme()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primaryContainer
                            val detailContent =
                                if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimaryContainer
                            FilledTonalButton(
                                onClick = { onNavigateToBudget?.invoke() },
                                contentPadding = PaddingValues(
                                    start = DashboardDimens.spaceLg, end = DashboardDimens.spaceLg
                                ),
                                modifier = Modifier.height(DashboardDimens.buttonHeightSm),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = detailContainer,
                                    contentColor = detailContent,
                                )
                            ) {
                                Text("Details", style = MaterialTheme.typography.labelMedium)
                            }
                        },
                    ) {
                        Text(
                            text = "${(totalBudget.toDouble() - totalSpentInBudgets.toDouble()).toCurrency(fmt)} of ${
                                totalBudget.toDouble().toCurrency(fmt)
                            }",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(DashboardDimens.spaceMdL))
                        BudgetProgressBar(progress = overallProgress, progressMultiplier = progMultiplier)
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
                    progressMultiplier = progMultiplier,
                )
            }

            // Recent transactions
            item {
                RecentTransactionsCard(
                    transactions = recentTx, onViewAll = { onViewAll?.invoke() })
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
        elevation = CardDefaults.cardElevation(DashboardDimens.cardElevation),
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
    onViewAll: (() -> Unit)? = null,
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
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "View all",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onViewAll?.invoke() })
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.background)
            Spacer(Modifier.height(DashboardDimens.spaceLg))
            // Column labels (increase vertical padding to spaceXl)
            Row(
                modifier = Modifier.fillMaxWidth().padding(
                    start = DashboardDimens.screenPaddingH,
                    end = DashboardDimens.screenPaddingH,
                    top = DashboardDimens.spaceMd,
                    bottom = DashboardDimens.spaceMd,
                ),
                horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
            ) {
                TxColumnLabel("Transaction", Modifier.weight(1f), TextAlign.Start)
                TxColumnLabel("Category", Modifier.weight(1f), TextAlign.Center)
                TxColumnLabel("Amount", Modifier.weight(1f), TextAlign.End)
            }


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
        style = MaterialTheme.typography.bodySmall,
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
            vertical = DashboardDimens.spaceXl,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
    ) {
        Text(
            text = tx.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
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
                Text(
                    prefix,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.width(DashboardDimens.spaceXxs))
            }
            Text(
                text = numeric,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (suffix.isNotEmpty()) {
                Spacer(Modifier.width(DashboardDimens.spaceXs))
                Text(
                    suffix,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

// Preview

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeTabScreenPreview() {
    val sampleTopCategories = listOf(
        HomeSpendingCategory("Food", 4250.0, 0.42f),
        HomeSpendingCategory("Shopping", 2250.0, 0.22f),
        HomeSpendingCategory("Transport", 450.0, 0.045f),
    )
    val sampleRecent = listOf(
        HomeTransactionItem(id = "tx1", title = "Zomato", category = "Food", amount = 500.0, currencyCode = "INR"),
        HomeTransactionItem(id = "tx2", title = "Uber", category = "Transport", amount = 350.0, currencyCode = "INR"),
        HomeTransactionItem(
            id = "tx3", title = "BigBasket", category = "Groceries", amount = 1200.0, currencyCode = "INR"
        ),
    )

    val fmt = remember { currencyFormat("INR") }

    MaterialTheme {
        Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
            ScreenTopBar(
                headerTitle = "TruXpense",
                showBack = false,
                showProfileIcons = true,
                onNotificationsClick = { /* preview stub */ }
            )
        }, floatingActionButton = {
             AddFab(onClick = {})
         }) { inner ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(
                    start = DashboardDimens.screenPaddingH,
                    end = DashboardDimens.screenPaddingH,
                    top = DashboardDimens.spaceLg,
                    bottom = DashboardDimens.spaceLg,
                ),
                verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg),
            ) {
                item {
                    SectionCard(title = "Spend this month") {
                        Text(
                            text = 12345.0.toCurrency(fmt),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                item {
                    SectionCard(
                        title = "Budget",
                        trailingContent = {
                            // Preview: reflect same dark-mode behavior for visuals
                            val previewDetailContainer =
                                if (isSystemInDarkTheme()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primaryContainer
                            val previewDetailContent =
                                if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimaryContainer
                            Button(
                                onClick = { /*TODO*/ },
                                modifier = Modifier.height(DashboardDimens.buttonHeightSm).clip(RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(
                                    start = DashboardDimens.spaceLg, end = DashboardDimens.spaceLg
                                ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = previewDetailContainer,
                                    contentColor = previewDetailContent,
                                )
                            ) {
                                Text("", style = MaterialTheme.typography.labelMedium)
                            }
                        },
                    ) {
                        Text(
                            text = "8,000.00 of 10,000.00",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(DashboardDimens.spaceMdL))
                        BudgetProgressBar(progress = 0.8f)
                    }
                }

                item {
                    InsightCard(
                        message = "You spent more on food this week than usual",
                        actionText = "Consider setting a weekly limit",
                        onAction = { /* navigate */ },
                    )
                }

                item {
                    SectionHeader(text = "Highest spending categories this month")
                }

                items(sampleTopCategories, key = { it.name }) { category ->
                    SpendingCategoryCard(
                        category = category,
                        fmt = fmt,
                        errorColor = MaterialTheme.colorScheme.error,
                    )
                }

                item {
                    RecentTransactionsCard(transactions = sampleRecent)
                }

                item { Spacer(Modifier.height(DashboardDimens.fabClearance)) }
            }
        }
    }
}

@Preview(showBackground = true, name = "RecentTransactionsCardPreview")
@Composable
fun RecentTransactionsCardPreview() {
    val sampleRecent = listOf(
        HomeTransactionItem(id = "tx1", title = "Zomato", category = "Food", amount = 500.0, currencyCode = "INR"),
        HomeTransactionItem(id = "tx2", title = "Uber", category = "Transport", amount = 350.0, currencyCode = "INR"),
        HomeTransactionItem(
            id = "tx3", title = "BigBasket", category = "Groceries", amount = 1200.0, currencyCode = "INR"
        ),
    )

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            RecentTransactionsCard(transactions = sampleRecent)
        }
    }
}