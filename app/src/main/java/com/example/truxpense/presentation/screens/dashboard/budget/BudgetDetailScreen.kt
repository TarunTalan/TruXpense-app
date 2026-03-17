package com.example.truxpense.presentation.screens.dashboard.budget

import android.annotation.SuppressLint
import android.graphics.Paint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.AppConfirmDialog
import com.example.truxpense.presentation.screens.dashboard.components.GradientCard
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.theme.AppDialogTheme
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.utils.*
import java.util.*
import kotlin.math.abs

// ── Design tokens ─────────────────────────────────────────────────────────────
private val BarTeal = Color(0xFF1BAF9D)
private val BarAmber = Color(0xFFF5A623)
private val BarRed = Color(0xFFE53935)
private val BarEmpty = Color(0xFFE0E4EA)

private fun fmtINR(amount: Double): String = runCatching {
    currencyFormat("INR").format(amount)
}.getOrDefault("₹${"%,.0f".format(amount)}")

private fun fmtINRAbs(amount: Double): String = "₹${"%,.0f".format(abs(amount))}"

/**
 * Derives a stable, visually distinct colour from a merchant name.
 * The same name always produces the same colour so avatars are consistent across recompositions.
 */
private val avatarPalette = listOf(
    Color(0xFF5C6BC0), // indigo
    Color(0xFF26A69A), // teal
    Color(0xFFEF5350), // red
    Color(0xFFFF7043), // deep orange
    Color(0xFF66BB6A), // green
    Color(0xFFAB47BC), // purple
    Color(0xFF29B6F6), // light blue
    Color(0xFFFFCA28), // amber
    Color(0xFF8D6E63), // brown
    Color(0xFF78909C), // blue grey
)

private fun merchantAvatarColor(name: String): Color {
    val index = abs(name.hashCode()) % avatarPalette.size
    return avatarPalette[index]
}

private const val MAX_RECENT = 5

// ══════════════════════════════════════════════════════════════════════════════
// SCREEN ENTRY POINT
// ══════════════════════════════════════════════════════════════════════════════

@SuppressLint("FrequentlyChangingValue")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailScreen(
    budgetName: String? = null,
    monthlyLimit: Double? = null,
    spent: Double? = null,
    vm: BudgetDetailViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onDeleted: () -> Unit = {},
    onArchive: () -> Unit = {},
    onSeeAll: (category: String) -> Unit = {},
    onTransactionClick: (transactionId: String) -> Unit = {},
) {
    val vmBudgetName by vm.budgetName.collectAsState()
    val vmMonthlyLimit by vm.monthlyLimit.collectAsState()
    val vmSpent by vm.spent.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val spendPoints by vm.spendPoints.collectAsState()
    val deleteComplete by vm.deleteComplete.collectAsState()
    val updateComplete by vm.updateComplete.collectAsState()
    val selectedPeriod by vm.selectedPeriod.collectAsState()
    val periodLabel by vm.periodLabel.collectAsState()
    val periodOffset by vm.periodOffset.collectAsState()
    val canGoBackPeriod by vm.canGoBack.collectAsState()
    val canGoForwardPeriod by vm.canGoForward.collectAsState()

    // True only when the user is viewing the current month/week — used to hide
    // time-remaining stats (days left, /day budget) that are meaningless for past periods.
    val isCurrentPeriod = periodOffset == 0

    val snackbarHostState = remember { SnackbarHostState() }

    // Nav args are used only to seed the VM on first load.
    // After that, always use live VM state so edits/deletes are reflected instantly.
    val budgetNameFinal = budgetName ?: vmBudgetName
    val limitFinal = vmMonthlyLimit.takeIf { it > 0 } ?: (monthlyLimit ?: 0.0)
    val spentFinal = vmSpent

    LaunchedEffect(budgetName) {
        val name = budgetName ?: return@LaunchedEffect
        if (name.isNotBlank()) {
            vm.loadBudget(name)
            vm.setPeriod(PeriodTab.MONTH)
        }
    }

    var progTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { progTriggered = true }
    val progMul by animateFloatAsState(
        targetValue = if (progTriggered) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "detail_progress",
    )

    var menuExpanded by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // ── Edit sheet state — auto-show / hide with animation ────────────────────
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(showEditSheet) {
        if (showEditSheet) editSheetState.show() else editSheetState.hide()
    }

    // ── Delete: navigate away immediately ─────────────────────────────────────
    LaunchedEffect(deleteComplete) {
        if (deleteComplete) {
            vm.resetDeleteComplete()
            onDeleted()
        }
    }

    // ── Edit: hide sheet on success ───────────────────────────────────────────
    LaunchedEffect(updateComplete) {
        if (updateComplete) {
            vm.resetUpdateComplete()
            editSheetState.hide()
            showEditSheet = false
        }
    }

    val lazyListState = rememberLazyListState()
    var chartAnimTriggered by remember { mutableStateOf(false) }
    val chartAnimProg by animateFloatAsState(
        targetValue = if (chartAnimTriggered) 1f else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "chart_anim",
    )
    LaunchedEffect(lazyListState.layoutInfo) {
        if (!chartAnimTriggered) {
            val visible = lazyListState.layoutInfo.visibleItemsInfo.any { it.index == 1 }
            if (visible) chartAnimTriggered = true
        }
    }

    // Edit sheet and delete dialog rendered at root so they overlay the Scaffold
    if (showEditSheet) {
        EditBudgetBottomSheet(
            currentLimit = limitFinal,
            sheetState = editSheetState,
            onApply = { newLimit -> vm.updateBudgetLimit(newLimit) },
            onDismiss = { showEditSheet = false },
        )
    }
    if (showDeleteDialog) {
        DeleteBudgetDialog(
            budgetName = budgetNameFinal,
            onConfirm = {
                showDeleteDialog = false
                vm.deleteBudget()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ScreenTopBar(
                headerTitle = "$budgetNameFinal Budget",
                showBack = true,
                onBack = onBack,
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                Icons.Default.MoreVert, "More options", tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        painterResource(R.drawable.edit),
                                        "Edit",
                                        modifier = Modifier.size(DashboardDimens.iconNav),
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                },
                                text = { Text("Edit budget", color = MaterialTheme.colorScheme.onBackground) },
                                onClick = { menuExpanded = false; showEditSheet = true },
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        painterResource(R.drawable.delete),
                                        "Delete",
                                        modifier = Modifier.size(DashboardDimens.iconNav),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                text = { Text("Delete budget", color = MaterialTheme.colorScheme.error) },
                                onClick = { menuExpanded = false; showDeleteDialog = true },
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        painterResource(R.drawable.archive),
                                        "Archive",
                                        modifier = Modifier.size(DashboardDimens.iconNav),
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                },
                                text = { Text("Archive budget", color = MaterialTheme.colorScheme.onBackground) },
                                onClick = { menuExpanded = false; onArchive() },
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->

        // Derive top merchants from transactions
        val topMerchants = remember(transactions) {
            transactions.groupBy { it.merchant }.map { (merchant, txs) ->
                Triple(merchant, txs.size, txs.sumOf { it.amount })
            }.sortedByDescending { it.third }.take(3)
        }

        // Group recent transactions by date label (Today / Yesterday / date string)
        val recentTxs = transactions.take(MAX_RECENT)
        val groupedTxs = remember(recentTxs) { groupTransactionsByDay(recentTxs) }

        val cal = remember { Calendar.getInstance() }
        val daysLeft = remember {
            (cal.getActualMaximum(Calendar.DAY_OF_MONTH) - cal.get(Calendar.DAY_OF_MONTH) + 1).coerceAtLeast(1)
        }

        // For past periods where no budget was ever set (limit == 0), show ₹0 for
        // all monetary stats — displaying real spending without a budget reference
        // is misleading (Budget:₹0 / Spent:₹5k / Remaining:₹0).
        val displaySpent = if (!isCurrentPeriod && limitFinal == 0.0) 0.0 else spentFinal
        val left = (limitFinal - displaySpent).coerceAtLeast(0.0)
        val dailyBudget = if (daysLeft > 0) left / daysLeft else 0.0

        // Avg spend per bucket — daily when in Week view, weekly when in Month view
        val avgPerDay = remember(spendPoints, selectedPeriod) {
            val nonZero = spendPoints.filter { it.amount > 0 }
            if (nonZero.isEmpty()) 0.0
            else when (selectedPeriod) {
                PeriodTab.WEEK -> nonZero.sumOf { it.amount } / 7.0          // avg per day
                PeriodTab.MONTH -> nonZero.sumOf { it.amount } / nonZero.size  // avg per week
            }
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize().padding(innerPadding).clearFocusOnTap(),
            contentPadding = PaddingValues(
                horizontal = DashboardDimens.screenPaddingH, vertical = DashboardDimens.spaceLg
            ),
            verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg),
        ) {

            // ── ① Budget summary card ────────────────────────────────────────
            item {
                BudgetSummaryCard(
                    categoryName = budgetNameFinal,
                    limit = limitFinal,
                    spent = displaySpent,
                    left = left,
                    progress = if (limitFinal > 0) (displaySpent / limitFinal).toFloat().coerceIn(0f, 1f) else 0f,
                    progressMultiplier = progMul,
                    daysLeft = daysLeft,
                    dailyBudget = dailyBudget,
                    isCurrentPeriod = isCurrentPeriod,
                    periodLabel = periodLabel,
                )
            }

            // ── ② Breakdown chart with period tab + navigator ─────────────────
            item {
                SpendBreakdownCard(
                    points = spendPoints,
                    avgPerDay = avgPerDay,
                    animProgress = chartAnimProg,
                    selectedPeriod = selectedPeriod,
                    periodLabel = periodLabel,
                    canGoBack = canGoBackPeriod,
                    canGoForward = canGoForwardPeriod,
                    onPeriodSelected = { vm.setPeriod(it) },
                    onGoBack = { vm.goBack() },
                    onGoForward = { vm.goForward() },
                )
            }

            // ── ③ Top merchants ────────────────────────────────────────────────
            if (topMerchants.isNotEmpty()) {
                item {
                    TopMerchantsCard(
                        merchants = topMerchants,
                        totalSpent = spentFinal,
                    )
                }
            }
            item { Spacer(Modifier.height(4.dp)) }

            // ── ④ Recent transactions header ───────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = DashboardDimens.spaceXs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Recent transactions",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (transactions.isNotEmpty()) {
                        Text(
                            text = "See all",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                onSeeAll(budgetNameFinal)
                            },
                        )
                    }
                }
            }

            // ── ⑤ Recent transactions (plain white card, no GradientCard) ─────
            if (recentTxs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = DashboardDimens.spaceXxl),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No transactions yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                groupedTxs.forEach { (dayLabel, txsForDay) ->
                    // Day header
                    item(key = "header_$dayLabel") {
                        Text(
                            text = dayLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    // Transactions for that day in one plain card
                    item(key = "group_$dayLabel") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(DashboardDimens.cornerCard),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            elevation = CardDefaults.cardElevation(0.dp),
                        ) {
                            txsForDay.forEachIndexed { idx, tx ->
                                TransactionRow(
                                    tx = tx, onClick = { onTransactionClick(tx.id) })
                                if (idx < txsForDay.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 64.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        thickness = 0.5.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(DashboardDimens.spaceXl)) }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ① BUDGET SUMMARY CARD  (GradientCard)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun BudgetSummaryCard(
    categoryName: String,
    limit: Double,
    spent: Double,
    left: Double,
    progress: Float,
    progressMultiplier: Float,
    daysLeft: Int,
    dailyBudget: Double,
    isCurrentPeriod: Boolean,
    periodLabel: String,
) {
    val barColor = progressColor(progress, MaterialTheme.colorScheme.error)

    GradientCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Category name + subtitle
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    painter = painterResource(iconForCategory(categoryName)),
                    contentDescription = categoryName,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(28.dp),
                )
                Column {
                    Text(
                        text = categoryName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "$periodLabel · monthly",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 3-column stats: Budget | Spent | Remaining / Over budget
            // When no budget is set (limit == 0), show all zeros cleanly.
            val exceeded = if (limit > 0) (spent - limit).coerceAtLeast(0.0) else 0.0
            val remainingValue = when {
                limit == 0.0 -> "₹0"
                exceeded > 0.0 -> "+₹${formatAbbreviatedAmount(exceeded)}"
                left == 0.0 -> "₹0"
                else -> "₹${formatAbbreviatedAmount(left)}"
            }
            val remainingLabel = if (exceeded > 0.0) "Over budget" else "Remaining"
            val remainingColor = when {
                exceeded > 0.0 -> MaterialTheme.colorScheme.error
                left == 0.0 -> BarAmber
                else -> MaterialTheme.colorScheme.onBackground
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                StatColumn("₹${formatAbbreviatedAmount(limit)}", "Budget", Modifier.weight(1f))
                StatColumn("₹${formatAbbreviatedAmount(spent)}", "Spent", Modifier.weight(1f))
                StatColumn(remainingValue, remainingLabel, Modifier.weight(1f), valueColor = remainingColor)
            }

            Spacer(Modifier.height(12.dp))

            // Progress bar
            Box(
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(progress * progressMultiplier).fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(Brush.horizontalGradient(listOf(barColor, barColor.copy(alpha = 0.7f)))),
                )
            }

            Spacer(Modifier.height(8.dp))

            // Footer — for past periods only show % used (days left is meaningless)
            val over = if (limit > 0) (spent - limit).coerceAtLeast(0.0) else 0.0
            if (over > 0.0) {
                // ── Exceeded: single full-width pill banner ─────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}% used",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "₹${formatAbbreviatedAmount(over)} over limit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                // ── Normal / fully used ─────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left side: always show % used
                    // Right side: show days-left info only for the current period
                    val pctText = "${(progress * 100).toInt()}% used"
                    Text(
                        text = if (isCurrentPeriod) "$pctText · $daysLeft days left" else pctText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = barColor,
                    )
                    if (isCurrentPeriod) {
                        val dailyText = if (left == 0.0) "Budget fully used"
                        else "₹${formatAbbreviatedAmount(dailyBudget)}/day left"
                        Text(
                            text = dailyText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onBackground else valueColor,
            letterSpacing = (-0.3).sp,
            maxLines = 1,
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ② SPEND BREAKDOWN CARD — period tab (Week / Month) + navigator + bar chart
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SpendBreakdownCard(
    points: List<SpendPoint>,
    avgPerDay: Double,
    animProgress: Float,
    selectedPeriod: PeriodTab,
    periodLabel: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPeriodSelected: (PeriodTab) -> Unit,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
) {
    val displayPoints = points.ifEmpty {
        if (selectedPeriod == PeriodTab.WEEK)
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").map { SpendPoint(it, 0.0) }
        else
            listOf(SpendPoint("W1", 0.0), SpendPoint("W2", 0.0), SpendPoint("W3", 0.0), SpendPoint("W4", 0.0))
    }

    val maxVal = displayPoints.maxOf { it.amount }.coerceAtLeast(1.0)
    val sortedAmounts = displayPoints.map { it.amount }.sortedDescending()
    fun barColor(amount: Double): Color = when {
        amount <= 0 -> BarEmpty
        amount == sortedAmounts[0] -> BarRed
        sortedAmounts.size > 1 && amount == sortedAmounts[1] -> BarAmber
        else -> BarTeal
    }

    GradientCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Period tab row (Week | Month) ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                PeriodTab.entries.forEach { tab ->
                    val selected = tab == selectedPeriod
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { onPeriodSelected(tab) }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Period navigator (← label →) ──────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onGoBack,
                    enabled = canGoBack,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.left_arrow),
                        contentDescription = "Previous period",
                        tint = MaterialTheme.colorScheme.onBackground.copy(
                            alpha = if (canGoBack) 1f else 0.3f
                        ),
                        modifier = Modifier.size(DashboardDimens.iconMd),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = periodLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (avgPerDay > 0) {
                        val avgLabel = if (selectedPeriod == PeriodTab.WEEK) "/day" else "/week"
                        Text(
                            text = "Avg ${fmtINRAbs(avgPerDay)}$avgLabel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                IconButton(
                    onClick = onGoForward,
                    enabled = canGoForward,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.right_arrow),
                        contentDescription = "Next period",
                        tint = MaterialTheme.colorScheme.onBackground.copy(
                            alpha = if (canGoForward) 1f else 0.3f
                        ),
                        modifier = Modifier.size(DashboardDimens.iconMd),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Bar chart ─────────────────────────────────────────────────────
            VerticalBarChart(
                points = displayPoints,
                maxVal = maxVal,
                barColorFn = { barColor(it) },
                animProgress = animProgress,
                modifier = Modifier.fillMaxWidth().height(160.dp),
            )
        }
    }
}

@Composable
private fun VerticalBarChart(
    points: List<SpendPoint>,
    maxVal: Double,
    barColorFn: (Double) -> Color,
    animProgress: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textSzPx = with(density) { 10.sp.toPx() }
    val amtSzPx = with(density) { 11.sp.toPx() }
    val topTextColorInt = MaterialTheme.colorScheme.onBackground.copy(0.8f).toArgb()

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val labelH = with(density) { 24.dp.toPx() }
        val amtLabelH = with(density) { 20.dp.toPx() }
        val chartH = h - labelH - amtLabelH
        val barCount = points.size
        val totalGap = with(density) { 8.dp.toPx() } * (barCount + 1)
        val barW = ((w - totalGap) / barCount).coerceAtLeast(8f)
        val cornerR = barW * 0.18f

        points.forEachIndexed { i, pt ->
            val x = with(density) { 8.dp.toPx() } + i * (barW + with(density) { 8.dp.toPx() })
            val frac = if (pt.amount > 0) (pt.amount / maxVal * animProgress).toFloat() else 0f
            val barH = (chartH * frac).coerceAtLeast(0f)
            val barTop = amtLabelH + (chartH - barH)
            val color = barColorFn(pt.amount)

            if (pt.amount > 0) {
                // Bar
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, barTop),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(cornerR, cornerR),
                )
                // Amount label above bar
                if (animProgress > 0.5f) {
                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            textSize = amtSzPx
                            textAlign = Paint.Align.CENTER
                            isFakeBoldText = true
                            this.color = topTextColorInt
                        }
                        val lbl = "₹${"%,.0f".format(pt.amount)}"
                        canvas.nativeCanvas.drawText(lbl, x + barW / 2, barTop - with(density) { 5.dp.toPx() }, paint)
                    }
                }
            } else {
                // Empty bar — just a horizontal dash
                val dashY = amtLabelH + chartH - with(density) { 4.dp.toPx() }
                drawLine(
                    color = BarEmpty,
                    start = Offset(x + barW * 0.25f, dashY),
                    end = Offset(x + barW * 0.75f, dashY),
                    strokeWidth = with(density) { 2.dp.toPx() },
                    cap = StrokeCap.Round,
                )
            }

            // Day / week label below
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    textSize = textSzPx
                    textAlign = Paint.Align.CENTER
                    this.color = topTextColorInt
                }
                canvas.nativeCanvas.drawText(pt.dayLabel, x + barW / 2, h - with(density) { 4.dp.toPx() }, paint)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ③ TOP MERCHANTS CARD  (GradientCard)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TopMerchantsCard(
    merchants: List<Triple<String, Int, Double>>,   // name, orderCount, totalSpent
    totalSpent: Double,
) {
    GradientCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Top merchants",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            merchants.forEachIndexed { idx, (name, orders, amount) ->
                MerchantRow(
                    merchantName = name,
                    orderCount = orders,
                    amount = amount,
                    pctOfBudget = if (totalSpent > 0) (amount / totalSpent * 100).toInt() else 0,
                )
                if (idx < merchants.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp).padding(start = 48.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        thickness = 0.5.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun MerchantRow(
    merchantName: String,
    orderCount: Int,
    amount: Double,
    pctOfBudget: Int,
) {
    // Derive a consistent colour from the merchant name so each merchant has a unique avatar tint
    val avatarColor = remember(merchantName) { merchantAvatarColor(merchantName) }
    val initial = merchantName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Avatar + name + orders
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(avatarColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = avatarColor,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = merchantName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$orderCount ${if (orderCount == 1) "order" else "orders"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    lineHeight = 16.sp
                )
            }
        }
        // Amount + pct
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "−${fmtINRAbs(amount)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "$pctOfBudget% of budget",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary,
                lineHeight = 16.sp
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ④ RECENT TRANSACTION ROW  (plain card, no GradientCard)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TransactionRow(
    tx: BudgetTransaction,
    onClick: () -> Unit,
) {
    val avatarColor = remember(tx.merchant) { merchantAvatarColor(tx.merchant) }
    val initial = tx.merchant.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            // Merchant initial avatar
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(avatarColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = avatarColor,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = tx.merchant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // "2:34 PM . HDFC....697" style
                val accountShort =
                    if (tx.account.length > 8) "${tx.account.take(4)}....${tx.account.takeLast(3)}" else tx.account
                Text(
                    text = "${tx.time} . $accountShort",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    lineHeight = 16.sp
                )
            }
        }
        Text(
            text = "−${fmtINRAbs(tx.amount)}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// EDIT BOTTOM SHEET  (unchanged)
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBudgetBottomSheet(
    currentLimit: Double,
    sheetState: SheetState,
    onApply: (Double) -> Unit,
    onDismiss: () -> Unit,
    initialAmount: String = "",
) {
    var rawAmount by remember { mutableStateOf(initialAmount) }
    val isValid = rawAmount.isNotBlank() && rawAmount.toDoubleOrNull()?.let { it > 0 } == true
    val abbrevHint = amountAbbreviationHint(rawAmount)
    var isFocused by remember { mutableStateOf(false) }

    val borderColor = when {
        rawAmount.isNotBlank() && !isValid -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }

    AppDialogTheme {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            tonalElevation = 0.dp,
            dragHandle = {
                Box(
                    modifier = Modifier.padding(top = DashboardDimens.spaceMd, bottom = DashboardDimens.spaceSm)
                        .size(width = DashboardDimens.sheetHandleWidth, height = DashboardDimens.sheetHandleHeight)
                        .clip(RoundedCornerShape(DashboardDimens.cornerSheetHandle))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                    .padding(horizontal = DashboardDimens.screenPaddingH)
                    .padding(vertical = DashboardDimens.sheetBottomPadding),
            ) {

                // ── Header ───────────────────────────────────────────────────────
                Text(
                    text = "Edit budget limit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(DashboardDimens.spaceXs))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceXs),
                ) {
                    Text(
                        text = "Current limit:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = fmtINR(currentLimit),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

                Spacer(Modifier.height(DashboardDimens.spaceXxl))

                // ── Input label ──────────────────────────────────────────────────
                Text(
                    text = "New monthly limit",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isFocused) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(DashboardDimens.spaceSm))

                // ── Amount input field ───────────────────────────────────────────
                Box(
                    modifier = Modifier.fillMaxWidth().height(DashboardDimens.buttonHeight)
                        .clip(RoundedCornerShape(DashboardDimens.cornerCard))
                        .background(MaterialTheme.colorScheme.background).border(
                            width = if (isFocused || (rawAmount.isNotBlank() && !isValid)) 2.dp else DashboardDimens.borderStroke,
                            color = borderColor,
                            shape = RoundedCornerShape(DashboardDimens.cornerCard),
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = DashboardDimens.screenPaddingH),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceSm),
                    ) {
                        Text(
                            text = "₹",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (rawAmount.isNotBlank()) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        BasicTextField(
                            value = rawAmount,
                            onValueChange = { rawAmount = sanitizeAmountInput(it) },
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.weight(1f).onFocusChanged { isFocused = it.isFocused },
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            decorationBox = { inner ->
                                if (rawAmount.isEmpty()) {
                                    Text(
                                        text = "Enter amount",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    )
                                }
                                inner()
                            },
                        )
                        if (rawAmount.isNotBlank()) {
                            IconButton(
                                onClick = { rawAmount = "" },
                                modifier = Modifier.size(DashboardDimens.iconButtonMd),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(DashboardDimens.iconMd),
                                )
                            }
                        }
                    }
                }

                // ── Abbreviation hint + helper / error text ──────────────────────
                Spacer(Modifier.height(DashboardDimens.spaceXs))
                if (abbrevHint != null) {
                    Text(
                        text = abbrevHint,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = when {
                        rawAmount.isNotBlank() && !isValid -> "Enter a valid amount greater than 0"
                        else -> "Changes apply to the current month only"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        rawAmount.isNotBlank() && !isValid -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )

                Spacer(Modifier.height(DashboardDimens.spaceXxl))

                // ── Divider ───────────────────────────────────────────────────────
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    thickness = DashboardDimens.dividerThin,
                )

                Spacer(Modifier.height(DashboardDimens.spaceXl))

                // ── Action buttons ────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
                ) {
                    // Cancel
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(DashboardDimens.buttonHeight),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(
                            DashboardDimens.borderStroke,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Text(
                            "Cancel",
                            fontWeight = FontWeight.Medium,
                            fontSize = DashboardDimens.textXl,
                        )
                    }
                    Button(
                        onClick = {
                            rawAmount.toDoubleOrNull()?.let { amount ->
                                onApply(amount)
                                onDismiss()
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier.weight(1f).height(DashboardDimens.buttonHeight),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.background,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            disabledContentColor = MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                    ) {
                        Text(
                            "Apply limit",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = DashboardDimens.textXl,
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// DELETE DIALOG  (unchanged)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DeleteBudgetDialog(budgetName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AppConfirmDialog(
        title = "Delete \"$budgetName\"?",
        message = "This budget will be permanently removed. Your existing transactions will not be affected.",
        confirmLabel = "Delete",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// HELPERS
// ══════════════════════════════════════════════════════════════════════════════

/** Group a flat list of transactions into (dayLabel → list) ordered map. */
private fun groupTransactionsByDay(txs: List<BudgetTransaction>): Map<String, List<BudgetTransaction>> {
    val todayCal = Calendar.getInstance()
    val yestCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    fun dayLabel(dateStr: String): String {
        val parts = dateStr.split(" ")
        return when {
            parts.size >= 3 && parts[0] == todayCal.get(Calendar.DAY_OF_MONTH).toString() -> "Today"
            parts.size >= 3 && parts[0] == yestCal.get(Calendar.DAY_OF_MONTH).toString() -> "Yesterday"
            else -> dateStr
        }
    }
    return txs.groupByTo(LinkedHashMap()) { dayLabel(it.date) }
}

private fun iconForCategory(category: String?): Int = when (category?.trim()?.lowercase()) {
    "food" -> R.drawable.food
    "transport" -> R.drawable.transport
    "bills" -> R.drawable.bills_ic
    "shopping" -> R.drawable.shopping
    "health" -> R.drawable.health
    "entertainment" -> R.drawable.entertainment
    "groceries" -> R.drawable.drink
    else -> R.drawable.category_icon
}

// ══════════════════════════════════════════════════════════════════════════════
// PREVIEWS
// ══════════════════════════════════════════════════════════════════════════════

@Preview(name = "BudgetDetail", showBackground = true, showSystemUi = true)
@Composable
fun BudgetDetailScreenPreview() {
    val weekPoints = listOf(
        SpendPoint("W1", 900.0, "1–7 Feb"),
        SpendPoint("W2", 1200.0, "8–14 Feb"),
        SpendPoint("W3", 1900.0, "15–21 Feb"),
        SpendPoint("W4", 0.0, "22–28 Feb"),
    )
    val txs = listOf(
        BudgetTransaction("1", 2200.0, "Expense", "manual", "Swiggy", "Food", "HDFC Bank", "6 Mar 2026", "2:34 PM"),
        BudgetTransaction("2", 1000.0, "Expense", "manual", "Zomato", "Food", "HDFC Bank", "5 Mar 2026", "5:50 PM"),
        BudgetTransaction(
            "3", 500.0, "Expense", "manual", "Cafe coffee hall", "Food", "HDFC Bank", "5 Mar 2026", "9:00 AM"
        ),
    )
    val limit = 8000.0;
    val spent = 4200.0;
    val left = limit - spent
    val progress = (spent / limit).toFloat()
    val topMerchants = txs.groupBy { it.merchant }.map { (m, ts) -> Triple(m, ts.size, ts.sumOf { it.amount }) }
        .sortedByDescending { it.third }
    val grouped = groupTransactionsByDay(txs)

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            ScreenTopBar(headerTitle = "Food Budget", showBack = true, onBack = {}, actions = {})
            LazyColumn(
                contentPadding = PaddingValues(
                    horizontal = DashboardDimens.screenPaddingH, vertical = DashboardDimens.spaceLg
                ),
                verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg),
            ) {
                item {
                    BudgetSummaryCard(
                        "Food", limit, spent, left, progress, 1f, 15, left / 15,
                        isCurrentPeriod = true,
                        periodLabel = "",
                    )
                }
                item {
                    SpendBreakdownCard(
                        points = weekPoints,
                        avgPerDay = 280.0,
                        animProgress = 1f,
                        selectedPeriod = PeriodTab.MONTH,
                        periodLabel = "March 2026",
                        canGoBack = true,
                        canGoForward = false,
                        onPeriodSelected = {},
                        onGoBack = {},
                        onGoForward = {},
                    )
                }
                item { TopMerchantsCard(topMerchants, spent) }
                item {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Recent transactions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("See all", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1BAF9D))
                    }
                }
                grouped.forEach { (dayLabel, dayTxs) ->
                    item {
                        Text(
                            dayLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(DashboardDimens.cornerCard),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            dayTxs.forEachIndexed { idx, tx ->
                                TransactionRow(tx, onClick = {})
                                if (idx < dayTxs.lastIndex) HorizontalDivider(
                                    Modifier.padding(start = 64.dp),
                                    color = MaterialTheme.colorScheme.onBackground.copy(0.1f),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Preview: Recent Transactions – populated ──────────────────────────────────

@Preview(name = "RecentTransactions – populated", showBackground = true, widthDp = 390)
@Composable
private fun RecentTransactionsPreview() {
    val txs = listOf(
        BudgetTransaction("1", 2200.0, "Expense", "manual", "Swiggy", "Food", "HDFC Bank", "6 Mar 2026", "2:34 PM"),
        BudgetTransaction(
            "2", 1000.0, "Expense", "manual", "Zomato", "Food", "HDFC Bank", "6 Mar 2026", "11:15 AM"
        ),
        BudgetTransaction(
            "3", 500.0, "Expense", "manual", "Cafe Coffee Hall", "Food", "ICICI Bank", "5 Mar 2026", "9:00 AM"
        ),
        BudgetTransaction("4", 340.0, "Expense", "manual", "Blinkit", "Food", "Paytm", "5 Mar 2026", "7:45 PM"),
        BudgetTransaction("5", 180.0, "Expense", "sms", "Domino's", "Food", "SBI", "4 Mar 2026", "8:20 PM"),
    )
    val grouped = groupTransactionsByDay(txs)

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
                .padding(horizontal = DashboardDimens.screenPaddingH, vertical = DashboardDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceSm),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    text = "See all",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            grouped.forEach { (dayLabel, dayTxs) ->
                Text(
                    text = dayLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(DashboardDimens.cornerCard),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    dayTxs.forEachIndexed { idx, tx ->
                        TransactionRow(tx = tx, onClick = {})
                        if (idx < dayTxs.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 64.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                thickness = 0.5.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Preview: Recent Transactions – empty ─────────────────────────────────────

@Preview(name = "RecentTransactions – empty", showBackground = true, widthDp = 390)
@Composable
private fun RecentTransactionsEmptyPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
                .padding(horizontal = DashboardDimens.screenPaddingH, vertical = DashboardDimens.spaceLg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent transactions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = DashboardDimens.spaceXxl),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No transactions yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}