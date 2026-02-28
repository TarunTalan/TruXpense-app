package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.screens.dashboard.components.SpendingCategoryCard
import com.example.truxpense.presentation.screens.dashboard.components.AddFab
import com.example.truxpense.presentation.screens.dashboard.theme.DashboardDimens
import com.example.truxpense.util.currencyFormat
import com.example.truxpense.util.toCurrency


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetTab(
    vm: BudgetViewModel = hiltViewModel(),
    onNavigateToAddBudget: () -> Unit = {},
    onNavigateToBudgetDetail: (BudgetCategory) -> Unit = {},
    // Named param kept for design-time preview override
    previewBudgets: List<BudgetCategory>? = null,
    currencyCode: String = "INR",
) {
    val displayItems by vm.categoryDisplayItems.collectAsState()
    val totalBudget by vm.totalBudget.collectAsState()
    val totalSpent by vm.totalSpent.collectAsState()
    val currentMonth by vm.currentMonth.collectAsState()
    val canGoBack by vm.canGoBack.collectAsState()
    val canGoForward by vm.canGoForward.collectAsState()

    val budgetsToShow = previewBudgets?.mapIndexed { _, cat ->
        BudgetCategoryDisplay(
            category = cat,
            amountText = "${cat.spent} / ${cat.total}",
            progress = if (cat.total > 0) cat.spent.toFloat() / cat.total else 0f,
        )
    } ?: displayItems

    val fmt = remember(currencyCode) { currencyFormat(currencyCode) }

    // Convert totals to formatted strings for the content
    val totalBudgetStr = totalBudget.toDouble().toCurrency(fmt)
    val totalSpentStr = totalSpent.toDouble().toCurrency(fmt)

    // Show empty screen when there are no budgets or total budget is zero
    if (budgetsToShow.isEmpty() || totalBudget <= 0) {
        BudgetsEmptyScreen(onAddBudget = onNavigateToAddBudget)
    } else {
        BudgetTabContent(
            budgetsToShow = budgetsToShow,
            totalBudget = totalBudgetStr,
            totalSpent = totalSpentStr,
            currentMonth = currentMonth,
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            onPrevious = { vm.previousMonth() },
            onNext = { vm.nextMonth() },
            onNavigateToAddBudget = onNavigateToAddBudget,
            onNavigateToBudgetDetail = onNavigateToBudgetDetail,
        )
    }
}

// Stateless UI extracted for preview and reuse
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetTabContent(
    budgetsToShow: List<BudgetCategoryDisplay>,
    totalBudget: String,
    totalSpent: String,
    currentMonth: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onNavigateToAddBudget: () -> Unit,
    onNavigateToBudgetDetail: (BudgetCategory) -> Unit,
) {
    // One-shot progress bar fill — fires once when this screen is navigated to
    var progTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { progTriggered = true }
    val progMultiplier by animateFloatAsState(
        targetValue = if (progTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "budget_progress",
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(headerTitle = "Budgets", showBack = false) },
        floatingActionButton = {
            AddFab(onClick = onNavigateToAddBudget)
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .padding(horizontal = DashboardDimens.screenPaddingH),
            contentPadding = PaddingValues(bottom = DashboardDimens.spaceXxxl),
            verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg),
        ) {

            // Month navigator
            item {
                MonthNavigatorRow(
                    currentMonth = currentMonth,
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                    onPrevious = onPrevious,
                    onNext = onNext,
                )
            }

            // Total budget summary
            item {
                TotalBudgetCard(
                    totalBudget = totalBudget,
                    totalSpent = totalSpent,
                )
            }

            // Per-category rows
            items(budgetsToShow, key = { it.category.id }) { display ->
                SpendingCategoryCard(
                    name = display.category.name,
                    amountText = display.amountText,
                    progress = display.progress,
                    titleColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToBudgetDetail(display.category) },
                    errorColor = display.category.barColor,
                    progressMultiplier = progMultiplier,
                )
            }
        }
    }
}

// Month navigator row
@Composable
private fun MonthNavigatorRow(
    currentMonth: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = DashboardDimens.spaceSm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, enabled = canGoBack) {
            Icon(
                painter = painterResource(R.drawable.left_arrow),
                contentDescription = "Previous month",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (canGoBack) 1f else 0.38f),
                modifier = Modifier.size(DashboardDimens.iconMd)
            )
        }
        Text(
            text = currentMonth,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,

            )
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(
                painter = painterResource(R.drawable.right_arrow),
                contentDescription = "Next month",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (canGoForward) 1f else 0.38f),
                modifier = Modifier.size(DashboardDimens.iconMd)

            )
        }
    }
}

// Total budget card
@Composable
private fun TotalBudgetCard(
    totalBudget: String,
    totalSpent: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(DashboardDimens.cardElevation),
    ) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPaddingComp)) {
            Text(
                text = "Total budget",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(DashboardDimens.spaceMd))
            Text(
                text = totalBudget,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(DashboardDimens.spaceSm))
            Text(
                text = "$totalSpent spent",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Preview
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BudgetScreenPreview() {
    val sample = listOf(
        BudgetCategory(id = 1, name = "Food", spent = 1_200, total = 3_000, barColor = Color(0xFFEF4444)),
        BudgetCategory(id = 2, name = "Shopping", spent = 500, total = 2_000, barColor = Color(0xFFF59E0B)),
    )
    val sampleDisplay = sample.map {
        BudgetCategoryDisplay(
            category = it,
            amountText = "${it.spent} / ${it.total}",
            progress = if (it.total > 0) it.spent.toFloat() / it.total else 0f,
        )
    }

    val fmt = remember { currencyFormat("INR") }
    MaterialTheme {
        BudgetTabContent(
            budgetsToShow = sampleDisplay,
            totalBudget = sample.sumOf { it.total }.toDouble().toCurrency(fmt),
            totalSpent = sample.sumOf { it.spent }.toDouble().toCurrency(fmt),
            currentMonth = "February 2026",
            canGoBack = true,
            canGoForward = false,
            onPrevious = {},
            onNext = {},
            onNavigateToAddBudget = {},
            onNavigateToBudgetDetail = {}
        )
    }
}