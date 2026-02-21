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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.util.progressColor
import com.example.truxpense.util.currencyFormat


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
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "budget_progress"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = budgetNameFinal,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.back_icon),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options",tint = MaterialTheme.colorScheme.onBackground)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit budget", color = MaterialTheme.colorScheme.onBackground) },
                                onClick = { menuExpanded = false; onEdit() }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete budget", color = MaterialTheme.colorScheme.error) },
                                onClick = { menuExpanded = false; onDelete() }
                            )
                            DropdownMenuItem(
                                text = { Text("Archive budget",color = MaterialTheme.colorScheme.onBackground) },
                                onClick = { menuExpanded = false; onArchive() }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {


            // ── Monthly Budget Card ───────────────────────────────────────
            item {
                MonthlyBudgetCard(
                    limit = monthlyLimitFinal,
                    spent = spentFinal,
                    left = left,
                    animatedProgress = animatedProgress,
                    progress = progress
                )
            }

            // ── Week / Month toggle ───────────────────────────────────────
            item {
                PeriodToggle(
                    selected = selectedTab,
                    onSelect = { selectedTab = it }
                )
            }

            // ── Spending trend chart ──────────────────────────────────────
            item {
                SpendingTrendCard(
                    points = spendPoints,
                    periodTab = selectedTab
                )
            }

            // ── Transactions header ───────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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

            // ── Transaction rows ──────────────────────────────────────────
            items(transactions, key = { it.id }) { tx ->
                TransactionCard(tx = tx)
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ─── Monthly Budget Card ──────────────────────────────────────────────────────

@Composable
private fun MonthlyBudgetCard(
    limit: Double,
    spent: Double,
    left: Double,
    animatedProgress: Float,
    progress: Float
) {
    val barColor = progressColor(progress, MaterialTheme.colorScheme.error)

    // ── Total Budget Card ──────────────────────────────────────────
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Total budget",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatINR(limit),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${formatINR(spent)} spent",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            // progress bar showing used portion
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = barColor,
                trackColor = Color(0xFFD9DEE3),
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
            )
            Spacer(Modifier.height(6.dp))

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

// ─── Period Toggle ────────────────────────────────────────────────────────────

@Composable
private fun PeriodToggle(
    selected: PeriodTab,
    onSelect: (PeriodTab) -> Unit
) {
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PeriodTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(
                modifier = Modifier
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
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

// ─── Spending Trend Card ──────────────────────────────────────────────────────

@Composable
private fun SpendingTrendCard(
    points: List<SpendPoint>,
    periodTab: PeriodTab
) {
    var weekOffset by remember { mutableIntStateOf(0) }
    val rangeLabel = if (periodTab == PeriodTab.WEEK) "1–7 Feb" else "February 2026"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Date range nav
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { weekOffset-- },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous",
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = rangeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = { weekOffset++ },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "Spending trend",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            // Chart
            TrendLineChart(
                points = points,
                modifier = Modifier.fillMaxWidth().height(110.dp)
            )
        }
    }
}

// ─── Trend Line Chart ─────────────────────────────────────────────────────────

@Composable
private fun TrendLineChart(
    points: List<SpendPoint>,
    modifier: Modifier = Modifier
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
            fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent),
                startY = 0f,
                endY = chartH
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
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Dots + tooltip labels for max & min
        pts.forEachIndexed { i, pt ->
            val isHighlight = i == maxIdx || i == minIdx
            drawCircle(
                color = if (isHighlight) dotColor else lineColor.copy(alpha = 0.5f),
                radius = if (isHighlight) 5.dp.toPx() else 3.dp.toPx(),
                center = pt
            )
            // Day labels at bottom
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 10.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText(points[i].dayLabel, pt.x, h, paint)
            }
            // Amount labels for highlights
            if (isHighlight) {
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        textSize = 10.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                    val label = runCatching { currencyFormat("INR").format(points[i].amount) }.getOrDefault("₹${points[i].amount}")
                    val yPos = pt.y - 10.dp.toPx()
                    drawText(label, pt.x, yPos, paint)
                }
            }
        }
    }
}

// ─── Transaction Card ─────────────────────────────────────────────────────────

@Composable
private fun TransactionCard(tx: BudgetTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = tx.addedFrom,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            // Details section header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.transaction_detail),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Transaction details",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

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
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
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


// ─── Preview ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BudgetDetailScreenPreview() {
    MaterialTheme {
        BudgetDetailScreen()
    }
}