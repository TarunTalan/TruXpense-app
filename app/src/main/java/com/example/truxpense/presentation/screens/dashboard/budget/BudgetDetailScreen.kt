package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.BudgetProgressBar
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.screens.dashboard.theme.DashboardDimens
import com.example.truxpense.util.currencyFormat
import com.example.truxpense.util.progressColor


// use util currency formatter where needed; keep small helper for previews
private fun formatINR(amount: Double): String = runCatching {
    val fmt = currencyFormat("INR")
    fmt.format(amount)
}.getOrDefault("₹$amount")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailScreen(
    budgetName: String? = null,
    monthlyLimit: Double? = null,
    spent: Double? = null,
    vm: BudgetDetailViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onArchive: () -> Unit = {},
    onSeeAll: () -> Unit = {},
) {
    // If explicit values are supplied (via nav args), use them; otherwise fall back to ViewModel state
    val vmBudgetName by vm.budgetName.collectAsState()
    val vmMonthlyLimit by vm.monthlyLimit.collectAsState()
    val vmSpent by vm.spent.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val spendPoints by vm.spendPoints.collectAsState()

    val budgetNameFinal = budgetName ?: vmBudgetName
    val monthlyLimitFinal = monthlyLimit ?: vmMonthlyLimit
    val spentFinal = spent ?: vmSpent

    val left = (monthlyLimitFinal - spentFinal).coerceAtLeast(0.0)
    val progress = if (monthlyLimitFinal > 0) (spentFinal / monthlyLimitFinal).toFloat().coerceIn(0f, 1f) else 0f

    var menuExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(PeriodTab.WEEK) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress, animationSpec = tween(durationMillis = 800), label = "budget_progress"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.edit),
                                        contentDescription = "Edit",
                                        modifier = Modifier.size(DashboardDimens.iconNav),
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                },
                                text = { Text("Edit budget", color = MaterialTheme.colorScheme.onBackground) },
                                onClick = { menuExpanded = false; onEdit() })
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.delete),
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(DashboardDimens.iconNav),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                text = { Text("Delete budget", color = MaterialTheme.colorScheme.error) },
                                onClick = { menuExpanded = false; onDelete() })
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.archive),
                                        contentDescription = "Archive",
                                        modifier = Modifier.size(DashboardDimens.iconNav),
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                },
                                text = { Text("Archive budget", color = MaterialTheme.colorScheme.onBackground) },
                                onClick = { menuExpanded = false; onArchive() })
                        }
                    }
                }
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = DashboardDimens.screenPaddingH,
                vertical = DashboardDimens.spaceLg
            ),
            verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg)
        ) {

            // Monthly budget card
            item {
                MonthlyBudgetCard(
                    limit = monthlyLimitFinal,
                    spent = spentFinal,
                    left = left,
                    animatedProgress = animatedProgress,
                    progress = progress
                )
            }

            // Week / Month toggle
            item {
                PeriodToggle(
                    selected = selectedTab, onSelect = { selectedTab = it })
            }

            // Spending trend chart
            item {
                SpendingTrendCard(
                    points = spendPoints, periodTab = selectedTab
                )
            }

            // Transactions header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = DashboardDimens.spaceXs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "See all",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onSeeAll)
                    )
                }
            }

            // Transaction rows
            items(transactions, key = { it.id }) { tx ->
                TransactionCard(tx = tx)
            }

            item { Spacer(Modifier.height(DashboardDimens.spaceXl)) }
        }
    }
}

// Monthly budget card

@Composable
private fun MonthlyBudgetCard(
    limit: Double, spent: Double, left: Double, animatedProgress: Float, progress: Float
) {
    val barColor = progressColor(progress, MaterialTheme.colorScheme.error)

    // Total budget card
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = DashboardDimens.spaceMd),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPaddingComp)) {
            Text(
                text = "Total budget",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(DashboardDimens.spaceMd))
            Text(
                text = formatINR(limit),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(DashboardDimens.spaceSm))
            Text(
                text = "${formatINR(spent)} spent",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(DashboardDimens.spaceMdL))

            // progress bar showing used portion
            BudgetProgressBar(
                progress = animatedProgress,
                modifier = Modifier.fillMaxWidth(),
                errorColor = barColor,
            )
            Spacer(Modifier.height(DashboardDimens.spaceSm))

            // Remaining budget
            Text(
                text = "${formatINR(left)} left this month",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = barColor
            )
        }
    }


}

// Period toggle

@Composable
private fun PeriodToggle(
    selected: PeriodTab, onSelect: (PeriodTab) -> Unit
) {
    Row(
        modifier = Modifier.wrapContentWidth().background(
            color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(DashboardDimens.cornerChip)
        ).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceXs)
    ) {
        PeriodTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(
                modifier = Modifier.background(
                    color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shape = RoundedCornerShape(DashboardDimens.cornerToggleInner)
                ).clickable { onSelect(tab) }
                    .padding(horizontal = DashboardDimens.spaceXl, vertical = DashboardDimens.spaceSm),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Spending trend card

@Composable
private fun SpendingTrendCard(
    points: List<SpendPoint>, periodTab: PeriodTab
) {
    var weekOffset by remember { mutableIntStateOf(0) }
    val rangeLabel = if (periodTab == PeriodTab.WEEK) "1–7 Feb" else "February 2026"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPadding)) {
            // Date range nav
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd)
            ) {
                IconButton(
                    onClick = { weekOffset-- }, modifier = Modifier.size(DashboardDimens.iconButtonSm)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous",
                        modifier = Modifier.size(DashboardDimens.iconSm)
                    )
                }
                Text(
                    text = rangeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = { weekOffset++ }, modifier = Modifier.size(DashboardDimens.iconButtonSm)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        modifier = Modifier.size(DashboardDimens.iconSm)
                    )
                }
            }

            Spacer(Modifier.height(DashboardDimens.spaceXs))
            Text(
                text = "Spending trend",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(DashboardDimens.spaceLg))

            // Chart
            TrendLineChart(
                points = points, modifier = Modifier.fillMaxWidth().height(110.dp)
            )
        }
    }
}

// Trend line chart

@Composable
private fun TrendLineChart(
    points: List<SpendPoint>, modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val dotColor = MaterialTheme.colorScheme.primary

    val maxVal = points.maxOf { it.amount }.coerceAtLeast(1.0)
    // Highlight highest and lowest
    val maxIdx = points.indexOfFirst { it.amount == points.maxOf { p -> p.amount } }
    val minIdx = points.indexOfFirst { it.amount == points.minOf { p -> p.amount } }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val labelH = 20.dp.toPx()
        val chartH = h - labelH
        val step = w / (points.size - 1).coerceAtLeast(1)

        fun xAt(i: Int) = i * step
        fun yAt(v: Double) = chartH - (v / maxVal * chartH * 0.85f).toFloat()

        val pts = points.mapIndexed { i, p -> Offset(xAt(i), yAt(p.amount)) }

        // Fill area under line
        val fillPath = Path().apply {
            moveTo(pts.first().x, chartH)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(pts.last().x, chartH)
            close()
        }
        drawPath(
            fillPath, brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent), startY = 0f, endY = chartH
            )
        )

        // Line
        val linePath = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            linePath,
            color = lineColor,
            style = Stroke(width = DashboardDimens.chartLineStroke.toPx(), cap = StrokeCap.Round)
        )

        // Dots + tooltip labels for max & min
        pts.forEachIndexed { i, pt ->
            val isHighlight = i == maxIdx || i == minIdx
            drawCircle(
                color = if (isHighlight) dotColor else lineColor.copy(alpha = 0.5f),
                radius = if (isHighlight) DashboardDimens.chartDotHighlight.toPx() else DashboardDimens.chartDotNormal.toPx(),
                center = pt
            )
            // Day labels at bottom
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = DashboardDimens.textSm.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText(points[i].dayLabel, pt.x, h, paint)
            }
            // Amount labels for highlights
            if (isHighlight) {
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        textSize = DashboardDimens.textSm.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                    val label =
                        runCatching { currencyFormat("INR").format(points[i].amount) }.getOrDefault("₹${points[i].amount}")
                    val yPos = pt.y - 10.dp.toPx()
                    drawText(label, pt.x, yPos, paint)
                }
            }
        }
    }
}

// Transaction card

@Composable
private fun TransactionCard(tx: BudgetTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPadding)) {
            // Amount + type
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = formatINR(-tx.amount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = tx.type,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (tx.addedFrom.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(DashboardDimens.cornerBadge),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = tx.addedFrom,
                            modifier = Modifier.padding(
                                horizontal = DashboardDimens.spaceMd,
                                vertical = DashboardDimens.spaceXs
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.background,
                            fontSize = DashboardDimens.textXs
                        )
                    }
                }
            }

            Spacer(Modifier.height(DashboardDimens.spaceLg))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(DashboardDimens.spaceLg))

            // Details section header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.transaction_detail),
                    contentDescription = null,
                    modifier = Modifier.size(DashboardDimens.iconXs),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(DashboardDimens.spaceSm))
                Text(
                    text = "Transaction details",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(DashboardDimens.spaceMdL))

            // Detail rows
            TransactionDetailRow("Merchant", tx.merchant)
            TransactionDetailRow("Category", tx.category)
            TransactionDetailRow("Account", tx.account)
            TransactionDetailRow("Date", tx.date)
            TransactionDetailRow("Time", tx.time)
        }
    }
}

@Composable
private fun TransactionDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = DashboardDimens.spaceSm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

// Preview

@Preview(name = "BudgetDetail - Light", showBackground = true, showSystemUi = true)
@Composable
fun BudgetDetailScreenPreviewLight() {
    MaterialTheme {
        BudgetDetailPreviewContent(isDark = false)
    }
}

@Preview(name = "BudgetDetail - Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
fun BudgetDetailScreenPreviewDark() {
    MaterialTheme {
        BudgetDetailPreviewContent(isDark = true)
    }
}

@Composable
private fun BudgetDetailPreviewContent(isDark: Boolean) {
    // Sample data for preview
    val samplePoints = listOf(
        SpendPoint(amount = 120.0, dayLabel = "Mon"),
        SpendPoint(amount = 80.0, dayLabel = "Tue"),
        SpendPoint(amount = 200.0, dayLabel = "Wed"),
        SpendPoint(amount = 40.0, dayLabel = "Thu"),
        SpendPoint(amount = 160.0, dayLabel = "Fri"),
        SpendPoint(amount = 60.0, dayLabel = "Sat"),
        SpendPoint(amount = 100.0, dayLabel = "Sun")
    )

    val sampleTransactions = listOf(
        BudgetTransaction(
            id = "1",
            amount = 120.0,
            type = "Payment",
            addedFrom = "Mobile",
            merchant = "Coffee Shop",
            category = "Food",
            account = "Card ****1234",
            date = "01 Feb 2026",
            time = "09:15"
        ),
        BudgetTransaction(
            id = "2",
            amount = 450.0,
            type = "Transfer",
            addedFrom = "",
            merchant = "Supermarket",
            category = "Groceries",
            account = "Card ****5678",
            date = "02 Feb 2026",
            time = "14:30"
        ),
        BudgetTransaction(
            id = "3",
            amount = 75.0,
            type = "Payment",
            addedFrom = "POS",
            merchant = "Bakery",
            category = "Food",
            account = "Cash",
            date = "03 Feb 2026",
            time = "08:05"
        )
    )

    // Compose the screen using internal components to avoid Hilt/ViewModel in preview
    val monthlyLimit = 3000.0
    val spent = 985.0
    val left = (monthlyLimit - spent).coerceAtLeast(0.0)
    val progress = if (monthlyLimit > 0) (spent / monthlyLimit).toFloat().coerceIn(0f, 1f) else 0f

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top bar preview (simple)
        ScreenTopBar(headerTitle = "Groceries", showBack = true, onBack = {}, actions = {})

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = DashboardDimens.screenPaddingH,
                vertical = DashboardDimens.spaceLg
            ),
            verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg)
        ) {
            item {
                MonthlyBudgetCard(
                    limit = monthlyLimit,
                    spent = spent,
                    left = left,
                    animatedProgress = progress,
                    progress = progress
                )
            }

            item {
                PeriodToggle(selected = PeriodTab.WEEK, onSelect = {})
            }

            item {
                SpendingTrendCard(points = samplePoints, periodTab = PeriodTab.WEEK)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = DashboardDimens.spaceXs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "See all",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            items(sampleTransactions, key = { it.id }) { tx ->
                TransactionCard(tx = tx)
            }

            item { Spacer(Modifier.height(DashboardDimens.spaceXl)) }
        }
    }
}
