package com.example.truxpense.presentation.screens.dashboard.analytic


import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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


data class CategorySpend(
    val name: String,
    val amount: Double,
    val color: Color
)

data class TrendPoint(
    val label: String,
    val amount: Double
)

enum class PeriodMode { MONTH, WEEK }
enum class TrendRange { WEEKLY, MONTHLY, YEARLY }

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun formatINR(amount: Double): String {
    val abs = kotlin.math.abs(amount)
    return "₹${"%,.0f".format(abs)}"
}

// ─── Sample Data ─────────────────────────────────────────────────────────────

val sampleCategories = listOf(
    CategorySpend("Food", 4250.0, Color(0xFFE53935)),
    CategorySpend("Shopping", 2250.0, Color(0xFFFFA726)),
    CategorySpend("Transport", 450.0, Color(0xFF1E88E5)),
    CategorySpend("Bills", 340.0, Color(0xFF43A047)),
    CategorySpend("Others", 250.0, Color(0xFFBDBDBD)),
)

val sampleTrendPointsMonth = listOf(
    TrendPoint("1", 300.0), TrendPoint("7", 1200.0), TrendPoint("14", 800.0),
    TrendPoint("21", 500.0), TrendPoint("28", 950.0),
)

val sampleTrendPointsWeek = listOf(
    TrendPoint("Mon", 300.0), TrendPoint("Tue", 1200.0), TrendPoint("Wed", 600.0),
    TrendPoint("Thu", 400.0), TrendPoint("Fri", 800.0), TrendPoint("Sat", 500.0),
    TrendPoint("Sun", 350.0),
)

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun AnalyticsScreen(
    totalSpent: Double = 12_450.0,
    totalBudget: Double = 20_000.0,
    changePercent: Int = 8,
    categories: List<CategorySpend> = sampleCategories,
) {
    var periodMode by remember { mutableStateOf(PeriodMode.MONTH) }
    var periodExpanded by remember { mutableStateOf(false) }
    var trendRange by remember { mutableStateOf(TrendRange.MONTHLY) }

    // Observe view model when available (fallback to samples for preview)
    val vm: AnalyticsViewModel = hiltViewModel()
    val vmCategories by vm.categories.collectAsState()
    val vmTrendMonth by vm.trendMonth.collectAsState()
    val vmTrendWeek by vm.trendWeek.collectAsState()

    val periodLabel = if (periodMode == PeriodMode.MONTH) "February 2026" else "This week"
    val trendPoints = if (periodMode == PeriodMode.MONTH) {
        if (vmTrendMonth.isNotEmpty()) vmTrendMonth else sampleTrendPointsMonth
    } else {
        if (vmTrendWeek.isNotEmpty()) vmTrendWeek else sampleTrendPointsWeek
    }
    val trendTitle = if (periodMode == PeriodMode.MONTH) "Spending trend this month" else "Spending trend"
    val insightText = if (periodMode == PeriodMode.MONTH)
        "Your spending increased during the second half of the month."
    else
        "Your spending increased during the first half of the week."

    val categoriesToUse = if (vmCategories.isNotEmpty()) vmCategories else categories
    val donutTotal = categoriesToUse.sumOf { it.amount }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Summary Card ──────────────────────────────────────────────
            item {
                SummaryCard(
                    totalSpent = totalSpent,
                    totalBudget = totalBudget,
                    changePercent = changePercent,
                    periodMode = periodMode
                )
            }

            // ── Period Selector + Date Nav ─────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Dropdown pill
                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.clickable { periodExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (periodMode == PeriodMode.MONTH) "Month" else "Week",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = periodExpanded,
                            onDismissRequest = { periodExpanded = false }
                        ) {
                            listOf(PeriodMode.WEEK, PeriodMode.MONTH).forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = { periodMode = mode; periodExpanded = false }
                                )
                            }
                        }
                    }

                    // ── Date nav
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = periodLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            painter = painterResource(R.drawable.right_arrow),
                            contentDescription = "Next",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Donut Chart ───────────────────────────────────────────────
            item {
                DonutChartCard(
                    categories = categoriesToUse,
                    total = donutTotal,
                    periodMode = periodMode
                )
            }

            // ── Labels ───────────────────────────────────────────────────
            item {
                LabelsCard(categories = categoriesToUse)
            }

            // ── Spending Trend ────────────────────────────────────────────
            item {
                SpendingTrendCard(
                    title = trendTitle,
                    points = trendPoints,
                    trendRange = trendRange,
                    onRangeChange = { trendRange = it },
                    insightText = insightText
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ─── Summary Card ─────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(
    totalSpent: Double,
    totalBudget: Double,
    changePercent: Int,
    periodMode: PeriodMode
) {
    val monthName = if (periodMode == PeriodMode.MONTH) "February" else "this week"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${formatINR(totalSpent)} spent in $monthName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "of ${formatINR(totalBudget)} Budget",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.upward_arrow),
                    contentDescription = null,
                    tint = Color(0xFF43A047),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "↑ $changePercent% vs January",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF43A047)
                )
            }
        }
    }
}

// ─── Donut Chart Card ─────────────────────────────────────────────────────────

@Suppress("UNUSED_PARAMETER")
@Composable
private fun DonutChartCard(
    categories: List<CategorySpend>,
    total: Double,
    periodMode: PeriodMode
) {
    val insights = listOf(
        "• Food accounts for your highest spending this month.",
        "• You stayed within budget for transport."
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                DonutChart(categories = categories, total = total)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatINR(total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))

            insights.forEach { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun DonutChart(
    categories: List<CategorySpend>,
    total: Double
) {
    val sweeps = categories.map { ((it.amount / total) * 360f).toFloat() }

    // Animate each sweep
    val animatedSweeps = sweeps.map { target ->
        animateFloatAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 900),
            label = "donut_sweep"
        ).value
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 38.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val arcSize = Size(diameter, diameter)
        var startAngle = -90f

        animatedSweeps.forEachIndexed { i, sweep ->
            drawArc(
                color = categories[i].color,
                startAngle = startAngle,
                sweepAngle = sweep - 2f, // 2° gap between segments
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweep
        }
    }
}

// ─── Labels Card ─────────────────────────────────────────────────────────────

@Composable
private fun LabelsCard(categories: List<CategorySpend>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Labels",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            categories.forEachIndexed { index, cat ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(cat.color, CircleShape)
                        )

                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = formatINR(cat.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (index < categories.lastIndex)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

// ─── Spending Trend Card ──────────────────────────────────────────────────────

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
        Column(modifier = Modifier.padding(16.dp)) {
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
                Box {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { rangeMenuExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = trendRange.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall
                            )
                            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp))
                        }
                    }
                    DropdownMenu(
                        expanded = rangeMenuExpanded,
                        onDismissRequest = { rangeMenuExpanded = false }
                    ) {
                        TrendRange.entries.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = { onRangeChange(r); rangeMenuExpanded = false }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Chart
            TrendLineChart(
                points = points,
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

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

// ─── Trend Line Chart ─────────────────────────────────────────────────────────

@Composable
private fun TrendLineChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier
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
        val labelH = 18.dp.toPx()
        val chartH = h - labelH
        val step = w / (points.size - 1).coerceAtLeast(1)

        fun xAt(i: Int) = i * step
        fun yAt(v: Double) = (chartH - (v / maxVal * chartH * 0.82f)).toFloat()

        val pts = points.mapIndexed { i, p -> Offset(xAt(i), yAt(p.amount)) }

        // Fill
        val fillPath = Path().apply {
            moveTo(pts.first().x, chartH)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(pts.last().x, chartH)
            close()
        }
        drawPath(
            fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.18f), Color.Transparent),
                startY = 0f, endY = chartH
            )
        )

        // Line
        val linePath = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(linePath, color = lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        // Dots
        pts.forEachIndexed { i, pt ->
            val isHighlight = i == maxIdx || i == secondIdx
            if (isHighlight) {
                drawCircle(color = lineColor, radius = 5.dp.toPx(), center = pt)
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = pt)
            } else {
                drawCircle(color = lineColor.copy(alpha = 0.4f), radius = 2.5.dp.toPx(), center = pt)
            }
        }

        // Tooltip labels for highlights
        val nativeCanvas = drawContext.canvas.nativeCanvas
        listOf(maxIdx, secondIdx).forEach { idx ->
            if (idx < 0) return@forEach
            val pt = pts[idx]
            val label = formatINR(points[idx].amount)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.DKGRAY
                textSize = 10.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            // Tooltip background rect
            val fm = paint.fontMetrics
            val tw = paint.measureText(label)
            val pad = 4.dp.toPx()
            val boxL = pt.x - tw / 2 - pad
            val boxR = pt.x + tw / 2 + pad
            val boxT = pt.y - (-fm.ascent) - pad - 14.dp.toPx()
            val boxB = pt.y - 14.dp.toPx() + fm.descent + pad
            nativeCanvas.drawRoundRect(
                boxL, boxT, boxR, boxB, 4.dp.toPx(), 4.dp.toPx(),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(230, 245, 245, 245)
                }
            )
            nativeCanvas.drawText(label, pt.x, pt.y - 16.dp.toPx(), paint)

            // Day label below dot
            val dayPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 9.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            nativeCanvas.drawText(points[idx].label, pt.x, h, dayPaint)
        }

        // All day labels at bottom
        pts.forEachIndexed { i, pt ->
            val dayPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.LTGRAY
                textSize = 9.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            nativeCanvas.drawText(points[i].label, pt.x, h, dayPaint)
        }
    }
}

// ─── Preview ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AnalyticsScreenPreview() {
    MaterialTheme {
        AnalyticsScreen()
    }
}