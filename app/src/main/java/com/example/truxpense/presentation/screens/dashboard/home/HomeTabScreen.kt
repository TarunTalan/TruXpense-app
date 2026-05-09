package com.example.truxpense.presentation.screens.dashboard.home

import android.graphics.Color.rgb
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.budget.BudgetCategory
import com.example.truxpense.presentation.screens.dashboard.budget.BudgetCategoryDisplay
import com.example.truxpense.presentation.screens.dashboard.budget.BudgetViewModel
import com.example.truxpense.presentation.screens.dashboard.components.*
import com.example.truxpense.presentation.screens.dashboard.notifications.NotificationViewModel
import com.example.truxpense.presentation.screens.onboarding.currency.CurrencyViewModel
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.theme.TruXpenseTheme
import com.example.truxpense.presentation.utils.currencyFormat
import com.example.truxpense.presentation.utils.formatAbbreviatedAmount
import com.example.truxpense.presentation.utils.progressColor
import com.example.truxpense.presentation.utils.toCurrency
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs


private val BgColor = Color(0xFFF0F2F5)
private val TealColor = Color(0xFF1BAF9D)
private val SubColor = Color(0xFF8490A8)
private val IconBlue = Color(0xFFEBF1FF)
private val IconOrange = Color(0xFFFEF0E4)
private val IconGreen = Color(0xFFE4F7EF)
private val IconPurple = Color(0xFFF0EAFF)

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = GradientCard(modifier = modifier, elevation = 2.dp, content = content)

@Composable
fun HomeTabScreen(
    vm: HomeViewModel,
    onAddExpense: (() -> Unit)? = null,
    onAddIncome: (() -> Unit)? = null,
    onNavigateToBudget: (() -> Unit)? = null,
    onViewAll: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null,
    onPendingReviewClick: (() -> Unit)? = null,
    onNavigateToAnalytics: (() -> Unit)? = null,
    onSavings: (() -> Unit)? = null,
    onNavigateToTransaction: ((String) -> Unit)? = null,
    onNavigateToBudgetDetail: ((String, Double, Double) -> Unit)? = null,
) {
    val hasSmsPermission by vm.hasSmsPermission.collectAsState()
    LaunchedEffect(Unit) { vm.refreshSmsPermission() }

    val expenseCount by vm.expenseCount.collectAsState()
    val isLoaded by vm.isLoaded.collectAsState()
    val monthlySpend by vm.monthlySpend.collectAsState()
    val pendingCount by vm.pendingCount.collectAsState()

    // Shared across both empty and content branches
    val notificationVm: NotificationViewModel = hiltViewModel()
    val unreadCount by notificationVm.unreadCount.collectAsState()
    val username by vm.username.collectAsState(initial = "")

    val currencyVm: CurrencyViewModel = hiltViewModel()
    val currencyCode by remember {
        derivedStateOf { currencyVm.selectedCurrency.value?.code ?: "INR" }
    }

    AnimatedVisibility(
        visible = isLoaded,
        enter = fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)),
    ) {
        if (expenseCount == 0) {
            EmptyHomeContent(
                onAddExpense = onAddExpense,
                hasSmsPermission = hasSmsPermission,
                onSmsGranted = { vm.onSmsPermissionResult(true); vm.refreshSmsPermission() },
                onNotificationsClick = onNotificationsClick,
                onProfileClick = onProfileClick,
                unreadCount = unreadCount,
                username = username ?: "",
            )
        } else {
            HomeTabContent(
                monthlySpend = monthlySpend,
                hasSmsPermission = hasSmsPermission,
                pendingCount = pendingCount,
                unreadCount = unreadCount,
                username = username ?: "",
                onAddExpense = onAddExpense,
                onAddIncome = onAddIncome,
                onSmsGranted = { vm.onSmsPermissionResult(true); vm.refreshSmsPermission() },
                currencyCode = currencyCode,
                vm = vm,
                onNavigateToBudget = onNavigateToBudget,
                onViewAll = onViewAll,
                onNotificationsClick = onNotificationsClick,
                onProfileClick = onProfileClick,
                onPendingReviewClick = onPendingReviewClick,
                onNavigateToAnalytics = onNavigateToAnalytics,
                onSavings = onSavings,
                onNavigateToTransaction = onNavigateToTransaction,
                onNavigateToBudgetDetail = onNavigateToBudgetDetail,
            )
        }
    }
}

@Composable
fun HomeTabContent(
    monthlySpend: Double,
    hasSmsPermission: Boolean,
    pendingCount: Int = 0,
    unreadCount: Int = 0,
    username: String = "",
    onAddExpense: (() -> Unit)? = null,
    onAddIncome: (() -> Unit)? = null,
    onSmsGranted: (() -> Unit)? = null,
    currencyCode: String = "INR",
    vm: HomeViewModel = hiltViewModel(),
    onNavigateToBudget: (() -> Unit)? = null,
    onViewAll: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null,
    onPendingReviewClick: (() -> Unit)? = null,
    onNavigateToAnalytics: (() -> Unit)? = null,
    onSavings: (() -> Unit)? = null,
    onNavigateToTransaction: ((String) -> Unit)? = null,
    onNavigateToBudgetDetail: ((String, Double, Double) -> Unit)? = null,
) {
    val fmt = remember(currencyCode) { currencyFormat(currencyCode) }
    val recentTx by vm.recentTransactions.collectAsState(initial = emptyList())
    val budgetVm: BudgetViewModel = hiltViewModel()
    val budgetDisplayItems by budgetVm.categoryDisplayItems.collectAsState(initial = emptyList())
    val hasBudgets by budgetVm.hasBudgets.collectAsState(initial = false)
    val totalBudgetInt by budgetVm.totalBudget.collectAsState()
    val totalSpentInt by budgetVm.totalSpent.collectAsState()
    val totalBudget = totalBudgetInt.toDouble()
    val totalSpent = totalSpentInt.toDouble()
    val budgetLeft = (totalBudget - totalSpent).coerceAtLeast(0.0)

    // Real income & savings from HomeViewModel
    val monthlyIncome by vm.monthlyIncome.collectAsState()
    val monthlySavings by vm.monthlySavings.collectAsState()

    // Month-over-month spend change (replaces hardcoded "12% vs Feb")
    val monthlyChange by vm.monthOverMonthChange.collectAsState()

    // Per-day spending for the spending-trends chart
    val dailySpendPoints by vm.dailySpendPoints.collectAsState()

    // Dynamic insight derived from real expense data
    val insight by vm.spendingInsight.collectAsState()

    // Animated progress multiplier (fires once on enter)
    var progTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { progTriggered = true }
    val progMul by animateFloatAsState(
        targetValue = if (progTriggered) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "home_progress",
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                headerTitle = "TruXpense",
                showProfileIcons = true,
                unreadCount = unreadCount,
                username = username,
                onNotificationsClick = { onNotificationsClick?.invoke() },
                onProfileClick = { onProfileClick?.invoke() },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

            // ── SMS / Pending banners ─────────────────────────────────────────

            if (!hasSmsPermission) {
                item {
                    SmsBanner(
                        modifier = Modifier.fillMaxWidth(),
                        onEnable = { onSmsGranted?.invoke() },
                    )
                }
            }

            if (hasSmsPermission && pendingCount > 0) {
                item {
                    PendingSmsBanner(
                        count = pendingCount,
                        onClick = { onPendingReviewClick?.invoke() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ── Spend this month ──────────────────────────────────────────────

            item {
                SpendThisMonthCard(
                    monthlySpend = monthlySpend,
                    income = monthlyIncome,
                    savings = monthlySavings,
                    budgetLeft = budgetLeft,
                    monthlyChange = monthlyChange,
                    fmt = fmt,
                )
            }

            // ── Quick Actions ─────────────────────────────────────────────────

            item {
                QuickActionsRow(
                    onAddExpense = { onAddExpense?.invoke() },
                    onAddIncome = { onAddIncome?.invoke() },
                    onSetBudget = { onNavigateToBudget?.invoke() },
                    onSavings = { onSavings?.invoke() },
                )
            }

            // ── Budget overview ───────────────────────────────────────────────

            if (hasBudgets && budgetDisplayItems.isNotEmpty()) {
                item {
                    BudgetOverviewCard(
                        budgetLeft = budgetLeft,
                        budgetDisplayItems = budgetDisplayItems,
                        progMul = progMul,
                        fmt = fmt,
                        onDetails = { onNavigateToBudget?.invoke() },
                    )
                }
            }

            // ── Spending trends ───────────────────────────────────────────────

            item {
                SpendingTrendsCard(dailySpendPoints = dailySpendPoints)
            }

            // ── AI Insight ────────────────────────────────────────────────────

            item {
                val coroutineScope = rememberCoroutineScope()
                InsightCard(
                    message = insight.message,
                    actionText = insight.actionText,
                    onAction = {
                        when (val t = insight.target) {
                            is InsightTarget.AnalyticsRoot -> onNavigateToAnalytics?.invoke()
                            is InsightTarget.AnalyticsCategory -> {
                                // Navigate to analytics; host can decide how to apply category filter
                                onNavigateToAnalytics?.invoke()
                            }
                            is InsightTarget.AnalyticsMerchant -> {
                                onNavigateToAnalytics?.invoke()
                            }
                            is InsightTarget.TransactionDetail -> {
                                onNavigateToTransaction?.invoke(t.txId)
                            }
                            is InsightTarget.BudgetDetailByCategory -> {
                                // Need to resolve budget name, limit and spent via ViewModel (suspend)
                                coroutineScope.launch {
                                    val args = try {
                                        vm.getBudgetDetailArgs(t.category)
                                    } catch (e: Exception) { null }
                                    if (args != null) {
                                        onNavigateToBudgetDetail?.invoke(args.first, args.second, args.third)
                                    } else {
                                        // Fallback to budget list
                                        onNavigateToBudget?.invoke()
                                    }
                                }
                            }
                        }
                    },
                )
            }

            // ── Recent transactions ───────────────────────────────────────────

            item {
                RecentTransactionsCard(
                    transactions = recentTx,
                    onViewAll = { onViewAll?.invoke() },
                )
            }
        }
    }
}

@Composable
private fun SmsBanner(
    modifier: Modifier = Modifier,
    onEnable: () -> Unit,
) {
    val startGrd = MaterialTheme.colorScheme.surfaceContainerLowest
    val endGrd = MaterialTheme.colorScheme.surfaceContainerHighest
    // Gradient: from rgba(255,244,229,1) to rgba(179,116,26,1) with 20% transition
    val smsGradient = remember {
        Brush.horizontalGradient(
            0.0f to startGrd,
            1f to endGrd,
        )
    }
    val borderColor = if (isSystemInDarkTheme()) Color(0xFFF4A62A) else Color(rgb(244, 166, 42))
    val txtColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHighest else Color.White
    Column(
        modifier = modifier.clip(RoundedCornerShape(DashboardDimens.cornerCard)).background(smsGradient).border(
            color = borderColor, width = 1.dp, shape = RoundedCornerShape(DashboardDimens.cornerCard)
        ).padding(horizontal = 14.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.sms_icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(DashboardDimens.iconMd)
            )

            Text(
                text = "Enable SMS access",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.errorContainer,
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onEnable,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = txtColor,
                ),
                // fixed smaller height + comfortable vertical padding so the text doesn't get squashed
                modifier = Modifier.height(30.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }

        Text(
            text = "Automatically track expenses from bank sms.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.errorContainer,
            lineHeight = 15.sp,
            maxLines = 1
        )
    }
}


@Composable
private fun SpendThisMonthCard(
    monthlySpend: Double,
    income: Double,
    savings: Double,
    budgetLeft: Double,
    monthlyChange: MonthlyChangeData,
    fmt: NumberFormat,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Label
            Text(
                text = "Spend this month",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(6.dp))
            // Amount
            Text(
                text = monthlySpend.toCurrency(fmt),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.8).sp,
                lineHeight = 36.sp,
            )
            // Change indicator — real data from previous month
            val pct = monthlyChange.percentChange
            if (pct != null) {
                val isUp = pct >= 0
                val absPct = kotlin.math.abs(pct).toInt()
                val indicatorColor = if (isUp) MaterialTheme.colorScheme.error
                                     else MaterialTheme.colorScheme.tertiary
                val arrowRotation = if (isUp) -90f else 90f   // up arrow = -90, down = 90
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = indicatorColor,
                        modifier = Modifier.size(13.dp).graphicsLayer { rotationZ = arrowRotation },
                    )
                    Text(
                        text = "$absPct% vs ${monthlyChange.prevMonthLabel}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = indicatorColor,
                    )
                }
            }
            // Divider + mini stats
            HorizontalDivider(
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.outline.copy(0.3f),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            ) {
                MiniStat(
                    label = "Income",
                    value = income.toCurrency(fmt),
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(
                    modifier = Modifier.height(45.dp), color = MaterialTheme.colorScheme.outline.copy(0.3f)
                )
                MiniStat(
                    label = "Savings",
                    value = savings.toCurrency(fmt),
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(modifier = Modifier.height(45.dp), color = MaterialTheme.colorScheme.outline.copy(0.3f))
                MiniStat(
                    label = "Left",
                    value = budgetLeft.toCurrency(fmt),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 0.5.sp,
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// QUICK ACTIONS ROW
@Composable
private fun QuickActionsRow(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onSetBudget: () -> Unit,
    onSavings: () -> Unit,
) {
    Column {
        Text(
            text = "Quick actions",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickActionItem(
                label = "Add expense",
                bgColor = IconBlue,
                iconRes = R.drawable.add,
                onClick = onAddExpense,
                modifier = Modifier.weight(1f),
            )
            QuickActionItem(
                label = "Add income",
                bgColor = IconOrange,
                iconRes = R.drawable.add_inocme,
                onClick = onAddIncome,
                modifier = Modifier.weight(1f),
            )
            QuickActionItem(
                label = "Set budget",
                bgColor = IconGreen,
                iconRes = R.drawable.budget,
                onClick = onSetBudget,
                modifier = Modifier.weight(1f),
            )
            QuickActionItem(
                label = "Savings goal",
                bgColor = IconPurple,
                iconRes = R.drawable.savings,
                onClick = onSavings,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    label: String,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
) {
    // Use an explicit MutableInteractionSource and no indication to remove ripple effect
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier.clickable(
            indication = null,
            interactionSource = interactionSource,
            onClick = onClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
            } else {
                // fallback: empty box to preserve layout
                Spacer(modifier = Modifier.size(24.dp))
            }
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}


// BUDGET OVERVIEW CARD
// Budget display item — map from your BudgetViewModel's categoryDisplayItems
data class BudgetDisplayItem(
    val name: String,
    val spent: Double,
    val limit: Double,
) {
    val progress: Float get() = if (limit > 0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f
    val remaining: Double get() = (limit - spent).coerceAtLeast(0.0)
    val isOverspent: Boolean get() = spent > limit
}

@Composable
private fun BudgetOverviewCard(
    budgetLeft: Double,
    budgetDisplayItems: List<BudgetCategoryDisplay>,
    progMul: Float,
    fmt: NumberFormat,
    onDetails: () -> Unit,
) {
    // Map BudgetCategoryDisplay → BudgetDisplayItem for the UI
    val displayItems = remember(budgetDisplayItems) {
        budgetDisplayItems.take(4).map { bcd ->
            BudgetDisplayItem(
                name = bcd.category.name,
                spent = bcd.category.spent.toDouble(),
                limit = bcd.category.total.toDouble(),
            )
        }
    }

    // Compute actual days remaining in the current month once per composition
    val daysLeft = remember {
        val cal = Calendar.getInstance()
        cal.getActualMaximum(Calendar.DAY_OF_MONTH) - cal.get(Calendar.DAY_OF_MONTH) + 1
    }
    val safePerDay = remember(budgetLeft, daysLeft) {
        if (daysLeft > 0) budgetLeft / daysLeft else 0.0
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Column(modifier = Modifier.padding(16.dp)) {
                // Remaining amount
                Text(
                    text = "Budget Remaining",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = budgetLeft.toCurrency(fmt),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp,
                    )
                    Button(
                        onClick = onDetails,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealColor,
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(30.dp),
                    ) {
                        Text(
                            "Details",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.background,
                        )
                    }
                }
                Text(
                    text = buildAnnotatedString {
                        append("$daysLeft more day${if (daysLeft == 1) "" else "s"} · ")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.error)) {
                            append("${safePerDay.toCurrency(fmt)}/day safe to spend")
                        }
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
                )

                Spacer(Modifier.height(14.dp))

                // Per-category budget items
                displayItems.forEachIndexed { index, item ->
                    BudgetCategoryItem(item = item, progMul = progMul, fmt = fmt)
                    if (index < displayItems.lastIndex) {
                        Spacer(Modifier.height(13.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetCategoryItem(
    item: BudgetDisplayItem,
    progMul: Float,
    fmt: NumberFormat,
) {
    val barColor = progressColor(item.progress)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = item.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row {
                Text(
                    text = item.spent.toCurrency(fmt),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = "/${item.limit.toCurrency(fmt)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        BudgetProgressBar(
            progress = item.progress,
            progressMultiplier = progMul,
            showLabel = false,
            height = 6.dp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${item.remaining.toCurrency(fmt)} left this month",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = barColor,
        )
    }
}

// SPENDING TRENDS CARD  — thin wrapper around the shared SpendingTrendChart
@Composable
private fun SpendingTrendsCard(dailySpendPoints: FloatArray = FloatArray(0)) {
    val cal = remember { Calendar.getInstance() }
    val monthName = remember { cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: "" }
    val year = remember { cal.get(Calendar.YEAR) }
    val today = remember { cal.get(Calendar.DAY_OF_MONTH) }

    val points = remember<List<SpendPoint>>(dailySpendPoints, monthName, year, today) {
        dailySpendPoints.toSpendPoints(monthName = monthName, year = year, todayDay = today)
    }

    SpendingTrendCard(
        points = points,
        pillLabel = "$monthName $year",
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// INSIGHT CARD
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun InsightCard(
    message: String,
    actionText: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(DashboardDimens.cornerCard))
            .background(MaterialTheme.colorScheme.surfaceContainer).clickable(onClick = onAction).border(
                color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                width = 1.dp,
                shape = RoundedCornerShape(DashboardDimens.cornerCard)
            ).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painterResource(R.drawable.bulb), contentDescription = null, tint = Color.Unspecified
        )
        Column {
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.8f),
                lineHeight = 19.sp,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(top = 5.dp),
            ) {
                Text(
                    text = actionText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TealColor,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}


// RECENT TRANSACTIONS CARD
@Composable
fun RecentTransactionsCard(
    modifier: Modifier = Modifier,
    transactions: List<HomeTransactionItem>,
    onViewAll: (() -> Unit)? = null,
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent transactions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "View all",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.clickable { onViewAll?.invoke() },
                )
            }

            // Column headers
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TxHeader("Merchant", Modifier.weight(1.1f), TextAlign.Start)
                TxHeader("Category", Modifier.weight(1f), TextAlign.Center)
                TxHeader("Amount", Modifier.weight(0.9f), TextAlign.End)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.3f))

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No recent transactions",
                        fontSize = 13.sp,
                        color = SubColor,
                    )
                }
            } else {
                transactions.take(3).forEachIndexed { idx, tx ->
                    TxRow(tx = tx)
                    if (idx < transactions.take(3).lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.2f))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun TxHeader(text: String, modifier: Modifier, align: TextAlign) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.secondary.copy(0.9f),
        textAlign = align,
    )
}

@Composable
private fun TxRow(tx: HomeTransactionItem) {
    val isCredit = !tx.isExpense          // income = credit = green; expense = debit = normal
    val absAmount = abs(tx.amount)
    val sign = if (isCredit) "+" else "-"

    // Currency symbol
    val symbol = remember(tx.currencyCode) {
        runCatching {
            Currency.getInstance(tx.currencyCode).getSymbol(Locale.getDefault())
        }.getOrDefault("₹")
    }

    // Abbreviated display: "₹1.23L", "₹2.50Cr", "$1.23M" etc.
    val abbreviated = formatAbbreviatedAmount(absAmount, tx.currencyCode)
    val amountText = "$sign$symbol$abbreviated"

    Row(
        modifier = Modifier.fillMaxWidth().clickable { }.padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = tx.title,
            modifier = Modifier.weight(1.1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = tx.category,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary.copy(0.9f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // Amount — credit (income) tinted tertiary/green, expense uses onBackground
        Text(
            text = amountText,
            modifier = Modifier.weight(0.9f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isCredit) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}


// PENDING SMS BANNER  (reused from original, kept intact)
@Composable
fun PendingSmsBanner(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center,
            ) { Text("💳", fontSize = 18.sp) }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$count new transaction${if (count > 1) "s" else ""} detected",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = "Tap to review and confirm",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PREVIEWS
// ══════════════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeTabContentPreview() {
    val fmt = remember { currencyFormat("INR") }
    val sampleTx = listOf(
        HomeTransactionItem("1", "Swiggy", "Food", 450.0, "INR"),
        HomeTransactionItem("2", "Uber", "Transport", 320.0, "INR"),
        HomeTransactionItem("3", "Amazon", "Shopping", 1250.0, "INR"),
    )
    MaterialTheme {
        Scaffold(
            containerColor = BgColor,
            topBar = {
                ScreenTopBar(
                    headerTitle = "TruXpense, Tushar",
                    showProfileIcons = true,
                    unreadCount = 2,
                    onNotificationsClick = {},
                    onProfileClick = {},
                )
            },
        ) { pad ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { SmsBanner(modifier = Modifier.fillMaxWidth(), onEnable = {}) }
                item {
                    SpendThisMonthCard(
                        monthlySpend = 18550.0,
                        income = 45000.0,
                        savings = 26450.0,
                        budgetLeft = 26000.0,
                        monthlyChange = MonthlyChangeData(percentChange = 12.0, prevMonthLabel = "Feb"),
                        fmt = fmt,
                    )
                }
                item {
                    QuickActionsRow(
                        onAddExpense = {},
                        onAddIncome = {},
                        onSetBudget = {},
                        onSavings = {},
                    )
                }
                item { SpendingTrendsCard() }
                item {
                    InsightCard(
                        message = "Food makes up 55% of your spending this month.",
                        actionText = "View Food breakdown",
                        onAction = {},
                    )
                }
                item { RecentTransactionsCard(transactions = sampleTx, onViewAll = {}) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BudgetOverviewPreview() {
    // sample fmt
    val fmt = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
        currency = Currency.getInstance("INR")
    }

    val sampleBudgetDisplays = listOf(
        BudgetCategoryDisplay(
            category = BudgetCategory(id = 1, name = "Food", spent = 1200, total = 3000, barColor = Color(0xFFEF4444)),
            amountText = "₹1,200 / ₹3,000",
            progress = 1200f / 3000f,
        ),
        BudgetCategoryDisplay(
            category = BudgetCategory(
                id = 2, name = "Shopping", spent = 500, total = 2000, barColor = Color(0xFFF59E0B)
            ),
            amountText = "₹500 / ₹2,000",
            progress = 500f / 2000f,
        ),
        BudgetCategoryDisplay(
            category = BudgetCategory(
                id = 3, name = "Transport", spent = 200, total = 800, barColor = Color(0xFF14B8A6)
            ),
            amountText = "₹200 / ₹800",
            progress = 200f / 800f,
        ),
        BudgetCategoryDisplay(
            category = BudgetCategory(id = 4, name = "Bills", spent = 700, total = 1000, barColor = Color(0xFFEF4444)),
            amountText = "₹700 / ₹1,000",
            progress = 700f / 1000f,
        ),
    )

    TruXpenseTheme {
        BudgetOverviewCard(
            budgetLeft = 3500.0,
            budgetDisplayItems = sampleBudgetDisplays,
            progMul = 1f,
            fmt = fmt,
            onDetails = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecentTransactionsPreview() {
    val sampleTx = listOf(
        HomeTransactionItem("1", "Swiggy", "Food", 450.0, "INR"),
        HomeTransactionItem("2", "Uber", "Transport", 320.0, "INR"),
        HomeTransactionItem("3", "Amazon", "Shopping", 1250.0, "INR"),
        HomeTransactionItem("4", "Starbucks", "Coffee", 250.0, "INR"),
    )

    TruXpenseTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            RecentTransactionsCard(transactions = sampleTx, onViewAll = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InsightCardPreview() {
    TruXpenseTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            InsightCard(
                message = "You spent more on food this week than usual",
                actionText = "Review food spending",
                onAction = {})
        }
    }
}

@Preview(showBackground = true, name = "Trend – with data (light)")
@Composable
private fun SpendingTrendsPreview() {
    TruXpenseTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            SpendingTrendsCard(
                dailySpendPoints = floatArrayOf(
                    200f, 0f, 450f, 300f, 150f, 600f, 50f,
                    400f, 320f, 0f, 750f, 250f, 180f, 420f,
                    0f, 310f, 500f, 95f, 630f, 200f, 0f,
                    410f, 280f, 360f, 140f, 0f, 800f, 230f,
                    170f, 520f, 390f,
                ),
            )
        }
    }
}
