package com.example.truxpense.presentation.screens.dashboard.budget

import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.BudgetProgressBar
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.screens.dashboard.theme.DashboardDimens
import com.example.truxpense.util.currencyFormat
import com.example.truxpense.util.progressColor

private fun formatINR(amount: Double): String = runCatching {
    currencyFormat("INR").format(amount)
}.getOrDefault("₹$amount")

private fun formatINRFull(amount: Double): String = "₹${"%,.0f".format(kotlin.math.abs(amount))}"

private const val MAX_RECENT_TRANSACTIONS = 5

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

    // Period / navigation states from VM
    val selectedPeriod by vm.selectedPeriod.collectAsState()
    val periodOffset by vm.periodOffset.collectAsState()
    val periodLabel by vm.periodLabel.collectAsState()
    val canGoForward by vm.canGoForward.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val budgetNameFinal = budgetName ?: vmBudgetName
    val monthlyLimitFinal = monthlyLimit ?: vmMonthlyLimit
    val spentFinal = spent ?: vmSpent

    LaunchedEffect(budgetNameFinal) {
        if (budgetNameFinal.isNotBlank()) vm.loadBudget(budgetNameFinal)
    }

    val left = (monthlyLimitFinal - spentFinal).coerceAtLeast(0.0)
    val progress = if (monthlyLimitFinal > 0) (spentFinal / monthlyLimitFinal).toFloat().coerceIn(0f, 1f) else 0f

    var progTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { progTriggered = true }
    val progMultiplier by animateFloatAsState(
        targetValue = if (progTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "detail_progress",
    )

    var menuExpanded by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(deleteComplete) {
        if (deleteComplete) {
            snackbarHostState.showSnackbar("Budget deleted")
            vm.resetDeleteComplete()
            onDeleted()
        }
    }
    LaunchedEffect(updateComplete) {
        if (updateComplete) {
            showEditSheet = false
            vm.resetUpdateComplete()
        }
    }

    // Trend chart in-view animation
    val lazyListState = rememberLazyListState()
    var trendAnimTriggered by remember { mutableStateOf(false) }
    val trendAnimProgress by animateFloatAsState(
        targetValue = if (trendAnimTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "trend_entry_anim",
    )
    LaunchedEffect(lazyListState.layoutInfo) {
        if (!trendAnimTriggered) {
            val visible = lazyListState.layoutInfo.visibleItemsInfo.any { it.index == 2 }
            if (visible) trendAnimTriggered = true
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showEditSheet) {
        EditBudgetBottomSheet(
            currentLimit = monthlyLimitFinal,
            sheetState = sheetState,
            onApply = { newLimit -> vm.updateBudgetLimit(newLimit) },
            onDismiss = { showEditSheet = false },
        )
    }
    if (showDeleteDialog) {
        DeleteBudgetDialog(
            budgetName = budgetNameFinal,
            onConfirm = { showDeleteDialog = false; vm.deleteBudget() },
            onDismiss = { showDeleteDialog = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ScreenTopBar(
                headerTitle = budgetNameFinal,
                showBack = true,
                onBack = onBack,
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        painterResource(R.drawable.edit),
                                        contentDescription = "Edit",
                                        modifier = Modifier.size(DashboardDimens.iconNav),
                                        tint = MaterialTheme.colorScheme.onBackground,
                                    )
                                },
                                text = { Text("Edit budget", color = MaterialTheme.colorScheme.onBackground) },
                                onClick = { menuExpanded = false; showEditSheet = true },
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        painterResource(R.drawable.delete),
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(DashboardDimens.iconNav),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                text = { Text("Delete budget", color = MaterialTheme.colorScheme.error) },
                                onClick = { menuExpanded = false; showDeleteDialog = true },
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        painterResource(R.drawable.archive),
                                        contentDescription = "Archive",
                                        modifier = Modifier.size(DashboardDimens.iconNav),
                                        tint = MaterialTheme.colorScheme.onBackground,
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

        val recentTxs = transactions.take(MAX_RECENT_TRANSACTIONS)

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = DashboardDimens.screenPaddingH,
                vertical = DashboardDimens.spaceLg,
            ),
            verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg),
        ) {
            // Monthly budget card
            item {
                MonthlyBudgetCard(
                    limit = monthlyLimitFinal,
                    spent = spentFinal,
                    left = left,
                    progress = progress,
                    progressMultiplier = progMultiplier,
                )
            }

            // Week / Month toggle (now delegates to VM)
            item {
                PeriodToggle(
                    selected = selectedPeriod,
                    onSelect = { vm.setPeriod(it) },
                )
            }

            // Spending trend chart — fully interactive with offset navigation + dot tooltip
            item {
                SpendingTrendCard(
                    points = spendPoints,
                    periodTab = selectedPeriod,
                    periodLabel = periodLabel,
                    canGoForward = canGoForward,
                    onBack = { vm.goBack() },
                    onForward = { vm.goForward() },
                    trendAnimProgress = trendAnimProgress,
                )
            }

            // Transactions header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = DashboardDimens.spaceXs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (transactions.isNotEmpty()) {
                        Text(
                            text = if (transactions.size > MAX_RECENT_TRANSACTIONS) "See all (${transactions.size})" else "See all",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                Log.d("BudgetDetail", "See all clicked — sending category='${budgetNameFinal}'")
                                onSeeAll(budgetNameFinal)
                            },
                        )
                    }
                }
            }

            if (recentTxs.isEmpty()) {
                item {
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
            } else {
                items(recentTxs, key = { it.id }) { tx ->
                    TransactionSummaryCard(tx = tx, onClick = { onTransactionClick(tx.id) })
                }
            }

            item { Spacer(Modifier.height(DashboardDimens.spaceXl)) }
        }
    }
}

// ─── Edit budget bottom sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBudgetBottomSheet(
    currentLimit: Double,
    sheetState: SheetState,
    onApply: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var rawAmount by remember { mutableStateOf("") }
    val isValid = rawAmount.isNotBlank() && rawAmount.toDoubleOrNull() != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = DashboardDimens.cornerCard, topEnd = DashboardDimens.cornerCard),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH)
                .padding(bottom = DashboardDimens.spaceXxl),
            verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg),
        ) {
            Text(
                text = "Adjust budget for this month",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Current limit: ${formatINR(currentLimit)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceXs)) {
                Text(
                    text = "New limit for this month",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Card(
                    shape = RoundedCornerShape(DashboardDimens.cornerCard),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = DashboardDimens.screenPaddingH, vertical = DashboardDimens.spaceMdL),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceXs),
                    ) {
                        Text(
                            text = "₹",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        BasicTextField(
                            value = rawAmount,
                            onValueChange = { rawAmount = it.filter { c -> c.isDigit() || c == '.' } },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            decorationBox = { inner ->
                                if (rawAmount.isEmpty()) {
                                    Text(
                                        text = "Enter amount",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                inner()
                            },
                        )
                    }
                }
                Text(
                    text = "This change applies only for the current month",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(DashboardDimens.spaceMd))
            Button(
                onClick = { rawAmount.toDoubleOrNull()?.let(onApply) },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth().height(DashboardDimens.buttonHeight),
                shape = RoundedCornerShape(DashboardDimens.cornerCard),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp),
            ) {
                Text(
                    text = "Apply limit",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = DashboardDimens.textXl,
                    color = MaterialTheme.colorScheme.background,
                )
            }
        }
    }
}

// ─── Delete confirm dialog ────────────────────────────────────────────────────

@Composable
private fun DeleteBudgetDialog(
    budgetName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Delete \"$budgetName\" budget?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        },
        text = {
            Text(
                text = "This will permanently remove the budget. Your existing transactions won't be affected.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                elevation = ButtonDefaults.buttonElevation(0.dp),
            ) { Text("Delete", color = MaterialTheme.colorScheme.onError) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
    )
}

// ─── Monthly budget card ──────────────────────────────────────────────────────

@Composable
private fun MonthlyBudgetCard(
    limit: Double, spent: Double, left: Double, progress: Float, progressMultiplier: Float = 1f,
) {
    val barColor = progressColor(progress, MaterialTheme.colorScheme.error)
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = DashboardDimens.spaceMd),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(DashboardDimens.cardElevation),
    ) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPaddingComp)) {
            Text(
                "Monthly budget",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(DashboardDimens.spaceMd))
            Text(
                formatINR(limit),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(DashboardDimens.spaceSm))
            Text(
                "${formatINR(spent)} spent",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(DashboardDimens.spaceMdL))
            BudgetProgressBar(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                errorColor = barColor,
                progressMultiplier = progressMultiplier
            )
            Spacer(Modifier.height(DashboardDimens.spaceSm))
            Text(
                "${formatINR(left)} left this month",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = barColor
            )
        }
    }
}

// ─── Period toggle ────────────────────────────────────────────────────────────

@Composable
private fun PeriodToggle(selected: PeriodTab, onSelect: (PeriodTab) -> Unit) {
    Row(
        modifier = Modifier.wrapContentWidth().background(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(DashboardDimens.cornerChip),
        ).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceXs),
    ) {
        PeriodTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(
                modifier = Modifier.background(
                    color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shape = RoundedCornerShape(DashboardDimens.cornerToggleInner),
                ).clickable { onSelect(tab) }
                    .padding(horizontal = DashboardDimens.spaceXl, vertical = DashboardDimens.spaceSm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─── Spending trend card ──────────────────────────────────────────────────────

@Composable
private fun SpendingTrendCard(
    points: List<SpendPoint>,
    periodTab: PeriodTab,
    periodLabel: String,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    trendAnimProgress: Float = 1f,
) {
    val chartTitle = when (periodTab) {
        PeriodTab.WEEK -> "Daily spending — this week"
        PeriodTab.MONTH -> "Weekly spending — this month"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPadding)) {
            // ── Header row: back arrow · period label · forward arrow ─────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous",
                        modifier = Modifier.size(DashboardDimens.iconSm),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = periodLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                IconButton(onClick = onForward, enabled = canGoForward) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        modifier = Modifier.size(DashboardDimens.iconSm),
                        tint = if (canGoForward) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    )
                }
            }

            Spacer(Modifier.height(DashboardDimens.spaceXs))

            Text(
                text = chartTitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(DashboardDimens.spaceLg))

            // ── Interactive chart ─────────────────────────────────────────────
            InteractiveBudgetTrendChart(
                points = points,
                entryProgress = trendAnimProgress,
                modifier = Modifier.fillMaxWidth().height(DashboardDimens.trendChartHeight),
            )
        }
    }
}

// ─── Interactive trend chart ──────────────────────────────────────────────────

/**
 * Drop-in replacement for the old static [TrendLineChart].
 *
 * Additions vs. the original:
 * - Tap-and-hold on any dot shows a floating tooltip (label + amount) identical
 *   to the Analytics screen implementation.
 * - All dots are drawn with inner white highlights on press (consistent with Analytics).
 * - Subtle dashed grid lines at 25 / 50 / 75 % of chart height.
 * - Entry animation works the same way (rise from baseline via [entryProgress]).
 */
@Composable
private fun InteractiveBudgetTrendChart(
    points: List<SpendPoint>,
    entryProgress: Float = 1f,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) return

    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val maxVal = points.maxOf { it.amount }.coerceAtLeast(1.0)

    // Index of the currently-pressed dot (-1 = none)
    var heldIndex by remember(points) { mutableStateOf(-1) }

    // Capture dot positions for hit-testing — reset whenever `points` changes
    val dotPositions = remember(points) { Array(points.size) { Offset.Zero } }

    // Density-based pixel pre-computation (must happen in Composable scope)
    val density = LocalDensity.current
    val hitRadiusPx = with(density) { 28.dp.toPx() }
    val padTopPx = with(density) { DashboardDimens.chartPadV.toPx() }
    val padBottomPx = with(density) { DashboardDimens.spaceXxl.toPx() }
    val padHPx = with(density) { DashboardDimens.chartPadH.toPx() }
    val chartLineStrokePx = with(density) { DashboardDimens.chartLineStroke.toPx() }
    val dotHighlightPx = with(density) { DashboardDimens.chartDotHighlight.toPx() }
    val dotNormalPx = with(density) { DashboardDimens.chartDotNormal.toPx() }
    val textSmPx = with(density) { DashboardDimens.textSm.toPx() }
    val textXsPx = with(density) { DashboardDimens.textXs.toPx() }
    val twoDpPx = with(density) { 2.dp.toPx() }
    val fourDpPx = with(density) { 4.dp.toPx() }
    val eightDpPx = with(density) { 8.dp.toPx() }

    Box(modifier = modifier) {

        Canvas(
            modifier = Modifier.fillMaxSize().pointerInput(points) {
                detectTapGestures(
                    onPress = { tapOffset ->
                        val hit = dotPositions.indexOfFirst { pos ->
                            (tapOffset - pos).getDistance() <= hitRadiusPx
                        }
                        if (hit >= 0) {
                            heldIndex = hit
                            tryAwaitRelease()
                            heldIndex = -1
                        }
                    },
                )
            },
        ) {
            val w = size.width
            val h = size.height
            val padTop = padTopPx
            val padBottom = padBottomPx
            val padH = padHPx
            val chartLeft = padH
            val chartRight = w - padH
            val chartW = (chartRight - chartLeft).coerceAtLeast(1f)
            val chartH = (h - padTop - padBottom).coerceAtLeast(1f)
            val step = chartW / (points.size - 1).coerceAtLeast(1)
            val maxIdx = points.indexOfFirst { it.amount == points.maxOf { p -> p.amount } }

            fun xAt(i: Int) = chartLeft + i * step

            // Rise-from-baseline entry animation
            fun yAt(v: Double): Float {
                val realY = (padTop + chartH - (v / maxVal * chartH * 0.82f)).toFloat()
                val baseY = (padTop + chartH).toFloat()
                return baseY + (realY - baseY) * entryProgress
            }

            val allPts = points.mapIndexed { i, p -> Offset(xAt(i), yAt(p.amount)) }
            allPts.forEachIndexed { i, pt -> dotPositions[i] = pt }

            // ── Fill ──────────────────────────────────────────────────────────
            val fillPath = Path().apply {
                moveTo(allPts.first().x, padTop + chartH)
                allPts.forEach { lineTo(it.x, it.y) }
                lineTo(allPts.last().x, padTop + chartH)
                close()
            }
            drawPath(
                fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(fillColor, Color.Transparent),
                    startY = padTop, endY = padTop + chartH,
                ),
            )

            // ── Line ──────────────────────────────────────────────────────────
            val linePath = Path().apply {
                moveTo(allPts.first().x, allPts.first().y)
                allPts.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(linePath, color = lineColor, style = Stroke(chartLineStrokePx, cap = StrokeCap.Round))

            // ── Grid lines at 25 / 50 / 75 % ─────────────────────────────────
            listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                val gridY = padTop + chartH * (1f - frac * 0.82f)
                drawLine(
                    color = lineColor.copy(alpha = 0.07f),
                    start = Offset(chartLeft, gridY),
                    end = Offset(chartRight, gridY),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                )
            }

            // ── Dots ──────────────────────────────────────────────────────────
            allPts.forEachIndexed { i, pt ->
                val isHeld = i == heldIndex
                val isMax = i == maxIdx
                val dotR = when {
                    isHeld -> dotHighlightPx * 1.4f
                    isMax -> dotHighlightPx
                    else -> dotNormalPx
                }

                when {
                    isHeld || isMax -> {
                        drawCircle(color = lineColor, radius = dotR, center = pt)
                        drawCircle(color = Color.White, radius = dotR * 0.5f, center = pt)
                    }

                    else -> drawCircle(color = lineColor.copy(0.45f), radius = dotR, center = pt)
                }

                // Day / week label at bottom
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = if (isMax) android.graphics.Color.DKGRAY else android.graphics.Color.LTGRAY
                        textSize = textXsPx
                        textAlign = android.graphics.Paint.Align.CENTER
                        if (isMax) isFakeBoldText = true
                    }
                    val labelY = (padTop + chartH + padBottom * 0.55f).coerceAtMost(h - twoDpPx)
                    canvas.nativeCanvas.drawText(points[i].dayLabel, pt.x, labelY, paint)
                }

                // Amount label above max dot (when not held — held uses the floating tooltip)
                if (isMax && !isHeld) {
                    drawIntoCanvas { canvas ->
                        val label = formatINRFull(points[i].amount)
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = textSmPx
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                        val liftY = pt.y - dotR - eightDpPx
                        canvas.nativeCanvas.drawText(
                            label,
                            pt.x.coerceIn(chartLeft + 20f, chartRight - 20f),
                            liftY.coerceAtLeast(padTop + fourDpPx),
                            paint,
                        )
                    }
                }
            }
        }

        // ── Floating tooltip — shown when a dot is pressed-and-held ──────────
        if (heldIndex >= 0 && heldIndex < points.size) {
            val point = points[heldIndex]
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH)
                    .align(Alignment.TopCenter).padding(top = DashboardDimens.spaceMd),
            ) {
                Surface(
                    shape = RoundedCornerShape(DashboardDimens.cornerCard),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.shadow(8.dp, RoundedCornerShape(DashboardDimens.cornerCard))
                        .align(Alignment.TopCenter),
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = DashboardDimens.spaceMdL,
                            vertical = DashboardDimens.spaceSm,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
                    ) {
                        // Tooltip date — "Mon 3 Feb" or "W2 · 8–14 Feb" etc.
                        Text(
                            text = point.tooltipDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                        )
                        Text(
                            text = formatINRFull(point.amount),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.background,
                        )
                    }
                }
            }
        }
    }
}

// ─── Transaction summary card ─────────────────────────────────────────────────

@Composable
private fun TransactionSummaryCard(tx: BudgetTransaction, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = DashboardDimens.screenPaddingH, vertical = DashboardDimens.spaceMdL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tx.merchant.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
                Spacer(Modifier.width(DashboardDimens.spaceMd))
                Column {
                    Text(
                        text = tx.merchant,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                    )
                    Text(
                        text = "${tx.date}  •  ${tx.time}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "−${formatINR(tx.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(name = "BudgetDetail - Light", showBackground = true, showSystemUi = true)
@Composable
fun BudgetDetailScreenPreviewLight() {
    MaterialTheme { BudgetDetailPreviewContent() }
}

@Preview(
    name = "BudgetDetail - Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true,
)
@Composable
fun BudgetDetailScreenPreviewDark() {
    MaterialTheme { BudgetDetailPreviewContent() }
}

@Composable
private fun BudgetDetailPreviewContent() {
    val sampleWeekPoints = listOf(
        SpendPoint("Mon", 120.0, "Mon 23 Feb"),
        SpendPoint("Tue", 80.0, "Tue 24 Feb"),
        SpendPoint("Wed", 200.0, "Wed 25 Feb"),
        SpendPoint("Thu", 40.0, "Thu 26 Feb"),
        SpendPoint("Fri", 160.0, "Fri 27 Feb"),
        SpendPoint("Sat", 60.0, "Sat 28 Feb"),
        SpendPoint("Sun", 100.0, "Sun 1 Mar"),
    )
    val sampleMonthPoints = listOf(
        SpendPoint("W1", 1200.0, "1–7 Feb"),
        SpendPoint("W2", 950.0, "8–14 Feb"),
        SpendPoint("W3", 1800.0, "15–21 Feb"),
        SpendPoint("W4", 700.0, "22–28 Feb"),
    )
    val sampleTransactions = listOf(
        BudgetTransaction(
            "1", 120.0, "Expense", "ADDED MANUALLY", "Coffee Shop", "Food", "HDFC Bank", "01 Feb 2026", "9:15 AM"
        ),
        BudgetTransaction(
            "2", 450.0, "Expense", "ADDED MANUALLY", "Supermarket", "Food", "UPI", "02 Feb 2026", "2:30 PM"
        ),
    )
    val monthlyLimit = 5000.0;
    val spent = 4200.0
    val left = (monthlyLimit - spent).coerceAtLeast(0.0)
    val progress = (spent / monthlyLimit).toFloat().coerceIn(0f, 1f)

    var selectedTab by remember { mutableStateOf(PeriodTab.WEEK) }
    val points = if (selectedTab == PeriodTab.WEEK) sampleWeekPoints else sampleMonthPoints

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenTopBar(headerTitle = "Food Budget", showBack = true, onBack = {}, actions = {})
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = DashboardDimens.screenPaddingH, vertical = DashboardDimens.spaceLg
            ),
            verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg),
        ) {
            item { MonthlyBudgetCard(monthlyLimit, spent, left, progress, 1f) }
            item { PeriodToggle(selectedTab, { selectedTab = it }) }
            item {
                SpendingTrendCard(
                    points = points,
                    periodTab = selectedTab,
                    periodLabel = if (selectedTab == PeriodTab.WEEK) "23 Feb – 1 Mar 2026" else "February 2026",
                    canGoForward = false,
                    onBack = {},
                    onForward = {},
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Transactions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "See all (${sampleTransactions.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            items(sampleTransactions, key = { it.id }) { TransactionSummaryCard(it, onClick = {}) }
            item { Spacer(Modifier.height(DashboardDimens.spaceXl)) }
        }
    }
}