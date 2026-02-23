package com.example.truxpense.presentation.screens.dashboard.analytic


import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.budget.budgetColorForCategory
import com.example.truxpense.presentation.screens.dashboard.components.DateNavigatorRow
import com.example.truxpense.presentation.screens.dashboard.components.PeriodTabRow
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.screens.dashboard.home.HomeTransactionItem
import com.example.truxpense.presentation.screens.dashboard.home.HomeViewModel
import com.example.truxpense.presentation.screens.dashboard.theme.DashboardDimens

enum class PeriodMode(val label: String) {
    WEEK("week"), MONTH("Month"), YEAR("Year"),
}

enum class TrendRange { WEEKLY, MONTHLY, YEARLY }

// Helpers

private fun formatINR(amount: Double): String {
    val abs = kotlin.math.abs(amount)
    return "₹${"%,.0f".format(abs)}"
}

// Sample data (kept for previews)

val sampleCategories = listOf(
    CategorySpend("Food", 4250.0, Color(0xFFE53935)),
    CategorySpend("Shopping", 2250.0, Color(0xFFFFA726)),
    CategorySpend("Transport", 450.0, Color(0xFF1E88E5)),
    CategorySpend("Bills", 340.0, Color(0xFF43A047)),
    CategorySpend("Others", 250.0, Color(0xFFBDBDBD)),
)

val sampleTrendPointsMonth = listOf(
    TrendPoint("1", 300.0),
    TrendPoint("7", 1200.0), TrendPoint("14", 800.0),
    TrendPoint("21", 500.0), TrendPoint("28", 950.0),
)

val sampleTrendPointsWeek = listOf(
    TrendPoint("Mon", 300.0),
    TrendPoint("Tue", 1200.0), TrendPoint("Wed", 600.0),
    TrendPoint("Thu", 400.0),
    TrendPoint("Fri", 800.0), TrendPoint("Sat", 500.0),
    TrendPoint("Sun", 350.0),
)

// Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    // keep parameters for preview usage
    totalSpent: Double = 12_450.0,
    totalBudget: Double = 20_000.0,
    changePercent: Int = 8,
    categories: List<CategorySpend> = sampleCategories,
    // Optional overrides for preview / non-Hilt usage
    recentTransactionsOverride: List<HomeTransactionItem>? = null,
    budgetLimitOverride: Double? = null,
) {
    var periodMode by remember { mutableStateOf(PeriodMode.MONTH) }
    var trendRange by remember { mutableStateOf(TrendRange.MONTHLY) }
    // Month navigator offset (0 = current month, -1 = prev, etc.)
    var monthOffset by remember { mutableStateOf(0) }

    // If overrides are provided (preview), use them; otherwise read actual data from HomeViewModel
    val recentTx: List<HomeTransactionItem>
    val budgetLimit: Double

    if (recentTransactionsOverride != null && budgetLimitOverride != null) {
        recentTx = recentTransactionsOverride
        budgetLimit = budgetLimitOverride
    } else {
        val homeVm: HomeViewModel = hiltViewModel()
        val recentTxState by homeVm.recentTransactions.collectAsState()
        val budgetLimitState by homeVm.budgetLimit.collectAsState()
        recentTx = recentTransactionsOverride ?: recentTxState
        budgetLimit = budgetLimitOverride ?: budgetLimitState
    }

    // Compute aggregates from recent transactions
    val computedCategories = remember(recentTx) {
        recentTx.groupBy { it.category }.map { (cat, items) ->
            val amt = items.sumOf { it.amount }
            CategorySpend(cat, amt, budgetColorForCategory(cat))
        }.sortedByDescending { it.amount }
    }

    val computedTotalSpent = remember(recentTx) { recentTx.sumOf { it.amount } }
    val computedTotalBudget = budgetLimit

    // Build simple trend points by splitting the recent transactions into buckets.
    fun buildTrendPoints(
        trans: List<HomeTransactionItem>, buckets: Int, labelsProvider: (Int) -> String
    ): List<TrendPoint> {
        if (trans.isEmpty()) return emptyList()
        val per = (trans.size + buckets - 1) / buckets
        return trans.chunked(per)
            .mapIndexed { idx, chunk -> TrendPoint(labelsProvider(idx), chunk.sumOf { it.amount }) }
    }

    val vmTrendMonth = remember(recentTx) {
        if (recentTx.isEmpty()) sampleTrendPointsMonth
        else buildTrendPoints(recentTx, 5) { i -> "${(i + 1) * 7}" }
    }

    val vmTrendWeek = remember(recentTx) {
        if (recentTx.isEmpty()) sampleTrendPointsWeek
        else buildTrendPoints(recentTx, 7) { i ->
            listOf(
                "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
            )[i.coerceAtMost(6)]
        }
    }

    val periodLabel = when (periodMode) {
        PeriodMode.MONTH -> {
            val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val base = 1 // February = index 1
            val total = base + monthOffset
            val year = 2026 + (total / 12)
            val month = ((total % 12) + 12) % 12
            "${
                listOf(
                    "January",
                    "February",
                    "March",
                    "April",
                    "May",
                    "June",
                    "July",
                    "August",
                    "September",
                    "October",
                    "November",
                    "December"
                )[month]
            } $year"
        }

        PeriodMode.WEEK -> if (monthOffset == 0) "This week" else "Week of ${16 + monthOffset * 7} Feb"
        PeriodMode.YEAR -> if (monthOffset == 0) "2026" else "${2026 + monthOffset}"
    }
    val canNavBack = monthOffset > -12
    val canNavForward = monthOffset < 0
    val trendPoints = if (periodMode == PeriodMode.MONTH) vmTrendMonth else vmTrendWeek
    val trendTitle = if (periodMode == PeriodMode.MONTH) "Spending trend this month" else "Spending trend"
    val insightText = if (periodMode == PeriodMode.MONTH) "Your spending increased during the second half of the month."
    else "Your spending increased during the first half of the week."

    val categoriesToUse = computedCategories.ifEmpty { categories }
    val donutTotal = categoriesToUse.sumOf { it.amount }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(title = "Analytics", showBack = false) },
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding), contentPadding = PaddingValues(
                start = DashboardDimens.screenPaddingH,
                end = DashboardDimens.screenPaddingH,
                bottom = DashboardDimens.spaceLg
            ), verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg)
        ) {

            // Summary card — show computed totals if available
            item {
                SummaryCard(
                    totalSpent = if (computedTotalSpent > 0.0) computedTotalSpent else totalSpent,
                    totalBudget = if (computedTotalBudget > 0.0) computedTotalBudget else totalBudget,
                    changePercent = changePercent,
                    periodMode = periodMode
                )
            }

            // ── Period segmented control ──────────────────────────────────────
            item {
                PeriodTabRow(
                    selected = periodMode,
                    onSelect = { periodMode = it as PeriodMode; monthOffset = 0 },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Month / period navigator ──────────────────────────────────────
            item {
                DateNavigatorRow(
                    label = periodLabel,
                    canBack = canNavBack,
                    canForward = canNavForward,
                    onBack = { monthOffset-- },
                    onForward = { if (monthOffset < 0) monthOffset++ },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Donut chart
            item {
                DonutChartCard(
                    categories = categoriesToUse, total = donutTotal, periodMode = periodMode
                )
            }

            // Labels
            item {
                LabelsCard(categories = categoriesToUse)
            }

            // Spending trend
            item {
                SpendingTrendCard(
                    title = trendTitle,
                    points = trendPoints,
                    trendRange = trendRange,
                    onRangeChange = { trendRange = it },
                    insightText = insightText
                )
            }

            item { Spacer(Modifier.height(DashboardDimens.spaceXl)) }
        }
    }
}

// Summary card

@Composable
private fun SummaryCard(
    totalSpent: Double, totalBudget: Double, changePercent: Int, periodMode: PeriodMode
) {
    val monthName = if (periodMode == PeriodMode.MONTH) "February" else "this week"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPadding)) {
            Text(
                text = "${formatINR(totalSpent)} spent in $monthName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(DashboardDimens.spaceXxs))
            Text(
                text = "of ${formatINR(totalBudget)} Budget",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(DashboardDimens.spaceSm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.upward_arrow),
                    contentDescription = null,
                    tint = Color(0xFF43A047),
                    modifier = Modifier.size(DashboardDimens.iconXs)
                )
                Spacer(Modifier.width(DashboardDimens.spaceXs))
                Text(
                    text = "$changePercent% vs January",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF43A047)
                )
            }
        }
    }
}

// Donut chart card

@Suppress("UNUSED_PARAMETER")
@Composable
private fun DonutChartCard(
    categories: List<CategorySpend>, total: Double, periodMode: PeriodMode
) {
    val insights = listOf(
        "• Food accounts for your highest spending this month.", "• You stayed within budget for transport."
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(DashboardDimens.cardPadding), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(DashboardDimens.donutChartSize), contentAlignment = Alignment.Center
            ) {
                DonutChart(categories = categories, total = total)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatINR(total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Total spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(DashboardDimens.spaceLg))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(DashboardDimens.spaceMdL))

            insights.forEach { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = DashboardDimens.spaceXxs)
                )
            }
        }
    }
}

@Composable
private fun DonutChart(
    categories: List<CategorySpend>, total: Double
) {
    val sweeps = categories.map { ((it.amount / total) * 360f).toFloat() }

    // Animate each sweep
    val animatedSweeps = sweeps.map { target ->
        animateFloatAsState(
            targetValue = target, animationSpec = tween(durationMillis = 900), label = "donut_sweep"
        ).value
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = DashboardDimens.donutStrokeWidth.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val arcSize = Size(diameter, diameter)
        var startAngle = -90f

        animatedSweeps.forEachIndexed { i, sweep ->
            drawArc(
                color = categories[i].color,
                startAngle = startAngle,
                sweepAngle = sweep - 2f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweep
        }
    }
}

// Labels card

@Composable
private fun LabelsCard(categories: List<CategorySpend>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPadding)) {
            Text(
                text = "Labels",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = DashboardDimens.spaceMdL),
                color = MaterialTheme.colorScheme.onBackground
            )
            categories.forEachIndexed { index, cat ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = DashboardDimens.spaceMdL - 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMdL)
                    ) {
                        Box(
                            modifier = Modifier.size(10.dp).background(cat.color, CircleShape)
                        )

                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        text = formatINR(cat.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (index < categories.lastIndex) HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(
                        alpha = 0.5f
                    )
                )
            }
        }
    }
}

// Spending trend card

@Composable
private fun SpendingTrendCard(
    title: String,
    points: List<TrendPoint>,
    trendRange: TrendRange,
    onRangeChange: (TrendRange) -> Unit,
    insightText: String
) {
    var rangeMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPadding)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(DashboardDimens.spaceLg))

            // Chart
            TrendLineChart(
                points = points, modifier = Modifier.fillMaxWidth().height(DashboardDimens.trendChartHeight)
            )

            Spacer(Modifier.height(DashboardDimens.spaceMdL))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(DashboardDimens.spaceMd))

            Text(
                text = insightText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Trend line chart

@Composable
private fun TrendLineChart(
    points: List<TrendPoint>, modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    val lineColor = MaterialTheme.colorScheme.primary
    val maxVal = points.maxOf { it.amount }.coerceAtLeast(1.0)
    val maxIdx = points.indexOfFirst { it.amount == points.maxOf { p -> p.amount } }
    val secondMax = points.sortedByDescending { it.amount }.getOrNull(1)
    val secondIdx = points.indexOf(secondMax)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Internal padding so everything (points, labels, tooltips) stays inside the card bounds
        val paddingTop = DashboardDimens.chartPadV.toPx()
        val paddingBottom = DashboardDimens.spaceXxl.toPx() // leave space for day labels
        val paddingHorizontal = DashboardDimens.chartPadH.toPx()

        val chartLeft = paddingHorizontal
        val chartRight = (w - paddingHorizontal)
        val chartWidth = (chartRight - chartLeft).coerceAtLeast(1f)
        val chartH = (h - paddingTop - paddingBottom).coerceAtLeast(1f)

        val step = chartWidth / (points.size - 1).coerceAtLeast(1)

        fun xAt(i: Int) = chartLeft + i * step
        fun yAt(v: Double) = (paddingTop + chartH - (v / maxVal * chartH * 0.82f)).toFloat()

        val pts = points.mapIndexed { i, p -> Offset(xAt(i), yAt(p.amount)) }

        // Fill area under line
        val fillPath = Path().apply {
            moveTo(pts.first().x, paddingTop + chartH)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(pts.last().x, paddingTop + chartH)
            close()
        }
        drawPath(
            fillPath, brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.18f), Color.Transparent), startY = 0f, endY = chartH
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

        // Dots
        pts.forEachIndexed { i, pt ->
            val isHighlight = i == maxIdx || i == secondIdx
            if (isHighlight) {
                drawCircle(color = lineColor, radius = DashboardDimens.chartDotHighlight.toPx(), center = pt)
                drawCircle(color = Color.White, radius = DashboardDimens.chartDotNormal.toPx(), center = pt)
            } else {
                drawCircle(
                    color = lineColor.copy(alpha = 0.4f), radius = DashboardDimens.spaceXxs.toPx() + 0.5f, center = pt
                )
            }
        }

        // Tooltip labels for highlights (draw above the point, keep inside top padding)
        val nativeCanvas = drawContext.canvas.nativeCanvas
        listOf(maxIdx, secondIdx).forEach { idx ->
            if (idx < 0) return@forEach
            val pt = pts[idx]
            val label = formatINR(points[idx].amount)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.DKGRAY
                textSize = DashboardDimens.textSm.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            val fm = paint.fontMetrics
            val tw = paint.measureText(label)
            val pad = DashboardDimens.spaceXs.toPx()
            val liftPx = DashboardDimens.chartLabelLift.toPx()
            val boxL = (pt.x - tw / 2 - pad).coerceAtLeast(chartLeft)
            val boxR = (pt.x + tw / 2 + pad).coerceAtMost(chartRight)
            val boxT = (pt.y - (-fm.ascent) - pad - liftPx).coerceAtLeast(pad)
            val boxB = (pt.y - liftPx + fm.descent + pad).coerceAtLeast(boxT + 1f)

            val boxWidth = boxR - boxL
            val neededWidth = tw + pad * 2
            if (boxWidth >= neededWidth) {
                nativeCanvas.drawRoundRect(
                    boxL,
                    boxT,
                    boxR,
                    boxB,
                    DashboardDimens.cornerBadge.toPx(),
                    DashboardDimens.cornerBadge.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(230, 245, 245, 245)
                    })
                val textMinX = boxL + tw / 2
                val textMaxX = boxR - tw / 2
                val textX = if (textMinX <= textMaxX) pt.x.coerceIn(textMinX, textMaxX) else (boxL + boxR) / 2f
                nativeCanvas.drawText(label, textX, pt.y - liftPx, paint)
            } else {
                val textMinX = chartLeft + tw / 2
                val textMaxX = chartRight - tw / 2
                val textX = if (textMinX <= textMaxX) pt.x.coerceIn(textMinX, textMaxX) else pt.x
                nativeCanvas.drawText(label, textX, (pt.y - liftPx).coerceAtLeast(pad), paint)
            }

            val dayPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = DashboardDimens.textXs.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val safeBottom = DashboardDimens.spaceXxs.toPx()
            val dayY = (paddingTop + chartH + (paddingBottom / 2)).coerceAtMost(h - safeBottom)
            val dayX = if (chartLeft <= chartRight) pt.x.coerceIn(chartLeft, chartRight) else pt.x
            nativeCanvas.drawText(points[idx].label, dayX, dayY, dayPaint)
        }

        // All day labels at bottom (inside padding)
        pts.forEachIndexed { i, pt ->
            val dayPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.LTGRAY
                textSize = DashboardDimens.textXs.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val safeBottom = DashboardDimens.spaceXxs.toPx()
            val dayY = (paddingTop + chartH + (paddingBottom / 2)).coerceAtMost(h - safeBottom)
            val dayX = if (chartLeft <= chartRight) pt.x.coerceIn(chartLeft, chartRight) else pt.x
            nativeCanvas.drawText(points[i].label, dayX, dayY, dayPaint)
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AnalyticsScreenPreview() {
    MaterialTheme {
        AnalyticsScreen(
            recentTransactionsOverride = listOf(
                HomeTransactionItem(id = "tx1", title = "Cafe", category = "Food", amount = 1500.0),
                HomeTransactionItem(id = "tx2", title = "Metro", category = "Transport", amount = 300.0),
                HomeTransactionItem(id = "tx3", title = "Bistro", category = "Food", amount = 1800.0),
                HomeTransactionItem(id = "tx4", title = "Electricity", category = "Bills", amount = 500.0),
                HomeTransactionItem(id = "tx5", title = "Mall", category = "Shopping", amount = 2200.0),
                HomeTransactionItem(id = "tx6", title = "Taxi", category = "Transport", amount = 350.0),
                HomeTransactionItem(id = "tx7", title = "Cafe", category = "Food", amount = 1600.0),
                HomeTransactionItem(id = "tx8", title = "ISP", category = "Bills", amount = 550.0),
                HomeTransactionItem(id = "tx9", title = "Market", category = "Shopping", amount = 2100.0),
                HomeTransactionItem(id = "tx10", title = "Bus", category = "Transport", amount = 400.0),
            ), budgetLimitOverride = 25000.0
        )
    }
}