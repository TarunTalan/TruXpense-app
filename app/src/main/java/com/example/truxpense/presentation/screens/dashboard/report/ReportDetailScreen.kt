package com.example.truxpense.presentation.screens.dashboard.report

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.truxpense.R
import com.example.truxpense.data.repository.report.ReportCategoryRow
import com.example.truxpense.data.repository.report.ReportTransactionRow
import com.example.truxpense.data.repository.report.ReportTrendPoint
import com.example.truxpense.data.repository.report.ReportType
import com.example.truxpense.presentation.screens.dashboard.components.AppConfirmDialog
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.theme.TruXpenseTheme
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs

// ─── Palette constants ────────────────────────────────────────────────────────

private val ColorIncome = Color(0xFF2ECC71)
private val ColorExpense = Color(0xFFEF4444)
private val ColorAccent = Color(0xFF6C63FF)
private val ColorGold = Color(0xFFFFBE0B)

private fun fmt(amount: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale("en", "IN"))
    nf.maximumFractionDigits = 0
    return "₹${nf.format(abs(amount))}"
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun ReportDetailScreen(
    reportId: String,
    onBack: () -> Unit = {},
    vm: ReportDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val deleteComplete by vm.deleteComplete.collectAsStateWithLifecycle()
    val exportStatus by vm.exportStatus.collectAsStateWithLifecycle()
    val exportResult by vm.exportResult.collectAsStateWithLifecycle()
    val showAllTx by vm.showAllTransactions.collectAsStateWithLifecycle()

    LaunchedEffect(reportId) { vm.loadReport(reportId) }
    LaunchedEffect(deleteComplete) {
        if (deleteComplete) {
            vm.onDeleteHandled(); onBack()
        }
    }

    // Handle export result → share sheet
    LaunchedEffect(exportResult) {
        val r = exportResult ?: return@LaunchedEffect
        if (r.error == null) {
            ReportExporter.share(context, r)
            vm.consumeExportResult()
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AppConfirmDialog(
            title = "Delete report?",
            message = "\"${uiState.title}\" will be permanently removed.",
            confirmLabel = "Delete",
            onConfirm = { showDeleteDialog = false; vm.deleteReport() },
            onDismiss = { showDeleteDialog = false },
        )
    }

    // Export format bottom sheet
    if (showExportSheet) {
        ExportBottomSheet(
            isExporting = exportStatus == ExportStatus.EXPORTING,
            onExport = { fmt ->
                showExportSheet = false
                vm.export(fmt)
            },
            onDismiss = { showExportSheet = false },
        )
    }

    // Loading / not-found guards
    if (!uiState.isLoaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ColorAccent)
        }
        return
    }
    if (uiState.notFound) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Report not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                headerTitle = uiState.title,
                showBack = true,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(DashboardDimens.iconMd),
                        )
                    }
                    // Download button
                    IconButton(onClick = { showExportSheet = true }) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(ColorAccent),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (exportStatus == ExportStatus.EXPORTING) {
                                CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.download),
                                    contentDescription = "Export",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            ExportBar(
                isExporting = exportStatus == ExportStatus.EXPORTING,
                onExport = { vm.export(it) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()),
        ) {
            // ── 1. Document header hero ──────────────────────────────────────
            ReportHeroHeader(uiState = uiState)

            Column(
                modifier = Modifier.padding(horizontal = DashboardDimens.screenPaddingH),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                // ── 2. KPI strip ─────────────────────────────────────────────
                KpiStrip(uiState = uiState)

                // ── 3. Category donut + legend ───────────────────────────────
                if (uiState.categoryRows.isNotEmpty()) {
                    ReportSection(title = "Breakdown by category") {
                        CategoryBreakdownCard(rows = uiState.categoryRows)
                    }
                }

                // ── 4. Trend chart ───────────────────────────────────────────
                if (uiState.trendPoints.isNotEmpty()) {
                    ReportSection(title = "Spending trend") {
                        TrendSection(
                            points = uiState.trendPoints,
                            granularity = uiState.trendGranularity,
                            onGranularity = { vm.setGranularity(it) },
                        )
                    }
                }

                // ── 5. Insight pills ─────────────────────────────────────────
                InsightRow(uiState = uiState)

                // ── 6. Transaction list ──────────────────────────────────────
                if (uiState.transactions.isNotEmpty()) {
                    ReportSection(title = "Transactions (${uiState.transactionCount})") {
                        TransactionListCard(
                            transactions = uiState.transactions,
                            showAll = showAllTx,
                            onToggleAll = { vm.toggleShowAll() },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ─── Hero Header ──────────────────────────────────────────────────────────────

@Composable
private fun ReportHeroHeader(uiState: ReportDetailUiState) {
    Box(
        modifier = Modifier.fillMaxWidth().drawBehind {
            // Gradient background
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF6C63FF), Color(0xFF4C46C8)),
                )
            )
            // Decorative circle top-right
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.85f, -size.width * 0.15f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.04f),
                radius = size.width * 0.35f,
                center = Offset(size.width * 0.1f, size.height * 1.2f),
            )
        }.padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Type badge
            val typeLabel = when (uiState.reportType) {
                ReportType.EXPENSE -> "Expense Report"
                ReportType.INCOME -> "Income Report"
                ReportType.ALL -> "Full Financial Report"
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    typeLabel,
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = uiState.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.calender),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color.White.copy(alpha = 0.7f),
                )
                Text(
                    text = uiState.dateRangeLabel,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

// ─── KPI Strip ────────────────────────────────────────────────────────────────

@Composable
private fun KpiStrip(uiState: ReportDetailUiState) {
    val kpis = buildList {
        if (uiState.reportType != ReportType.INCOME) add(Triple(fmt(uiState.totalExpenses), "Expenses", ColorExpense))
        if (uiState.reportType != ReportType.EXPENSE) add(Triple(fmt(uiState.totalIncome), "Income", ColorIncome))
        if (uiState.reportType == ReportType.ALL) {
            val c = if (uiState.netAmount >= 0) ColorIncome else ColorExpense
            add(Triple(fmt(uiState.netAmount), "Net", c))
        }
        add(Triple(uiState.transactionCount.toString(), "Transactions", ColorAccent))
        add(Triple(fmt(uiState.avgDailySpend) + "/d", "Avg Daily", ColorGold))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        kpis.forEach { (value, label, color) ->
            KpiCard(value = value, label = label, color = color, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun KpiCard(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Color dot
            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─── Category Breakdown ───────────────────────────────────────────────────────

@Composable
private fun CategoryBreakdownCard(rows: List<ReportCategoryRow>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            // Donut + legend side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Donut chart
                val total = rows.sumOf { it.amount }.coerceAtLeast(0.01)
                Box(Modifier.size(108.dp), contentAlignment = Alignment.Center) {
                    DonutChart(
                        segments = rows.map { it.share },
                        colors = rows.map { it.color },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 20f,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            fmt(total),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text("Total", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Legend
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.take(5).forEach { row ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(row.color))
                            Text(
                                row.name,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    fmt(row.amount),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    "${(row.share * 100).toInt()}%",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Full-width progress bars for all categories
            if (rows.size > 1) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.1f))
                Spacer(Modifier.height(14.dp))

                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(row.color))
                        Text(
                            row.name,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${(row.share * 100).toInt()}%",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(28.dp),
                            textAlign = TextAlign.End
                        )
                    }
                    // Progress bar
                    Box(
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(0.1f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(row.share).fillMaxHeight().clip(RoundedCornerShape(2.dp))
                                .background(row.color),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

// ─── Trend Section ────────────────────────────────────────────────────────────

@Composable
private fun TrendSection(
    points: List<ReportTrendPoint>,
    granularity: ReportTrendGranularity,
    onGranularity: (ReportTrendGranularity) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Granularity selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReportTrendGranularity.entries.forEach { g ->
                    val label = when (g) {
                        ReportTrendGranularity.DAILY -> "Daily"; ReportTrendGranularity.WEEKLY -> "Weekly"; ReportTrendGranularity.MONTHLY -> "Monthly"
                    }
                    val selected = granularity == g
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(if (selected) ColorAccent else Color.Transparent).clickable { onGranularity(g) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            label,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bar chart
            BarChart(points = points)
        }
    }
}

@Composable
private fun BarChart(points: List<ReportTrendPoint>) {
    if (points.isEmpty()) return

    var pressedIndex by remember { mutableIntStateOf(-1) }
    val maxAmt = points.maxOf { it.amount }.coerceAtLeast(1.0)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Tooltip
        AnimatedVisibility(
            visible = pressedIndex >= 0 && pressedIndex < points.size,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut(),
        ) {
            val pt = points.getOrNull(pressedIndex)
            if (pt != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(ColorAccent.copy(alpha = 0.1f)).padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(pt.tooltipDate, fontSize = 11.sp, color = ColorAccent, fontWeight = FontWeight.Medium)
                        Text(fmt(pt.amount), fontSize = 13.sp, color = ColorAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Bars
        Row(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            points.forEachIndexed { i, pt ->
                val barFraction = (pt.amount / maxAmt).toFloat().coerceIn(0.03f, 1f)
                val isPressed = pressedIndex == i
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(barFraction)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(
                                brush = Brush.verticalGradient(
                                    colors = if (isPressed) listOf(ColorAccent, ColorAccent.copy(alpha = 0.7f))
                                    else listOf(ColorAccent.copy(alpha = 0.8f), ColorAccent.copy(alpha = 0.35f))
                                )
                            ).clickable { pressedIndex = if (pressedIndex == i) -1 else i },
                    )
                }
            }
        }

        // X-axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─── Insight Highlights Row ───────────────────────────────────────────────────

@Composable
private fun InsightRow(uiState: ReportDetailUiState) {
    val insights = buildList {
        uiState.topMerchant?.let {
            add(InsightData("Top Merchant", it.first, fmt(it.second), ColorExpense))
        }
        uiState.topCategory?.let {
            add(InsightData("Top Category", it.first, fmt(it.second), ColorAccent))
        }
        uiState.highestSpendDay?.let {
            add(InsightData("Highest Day", it.first, fmt(it.second), ColorGold))
        }
    }
    if (insights.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        insights.forEach { insight ->
            InsightCard(insight = insight, modifier = Modifier.weight(1f))
        }
    }
}

private data class InsightData(val label: String, val primary: String, val amount: String, val color: Color)

@Composable
private fun InsightCard(insight: InsightData, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(insight.label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                insight.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(insight.amount, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = insight.color)
        }
    }
}

// ─── Transaction List ─────────────────────────────────────────────────────────

@Composable
private fun TransactionListCard(
    transactions: List<ReportTransactionRow>,
    showAll: Boolean,
    onToggleAll: () -> Unit,
) {
    val displayed = if (showAll) transactions else transactions.take(10)

    // Group by date
    val grouped = displayed.groupBy { it.dateLabel }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(bottom = 4.dp)) {
            grouped.entries.forEachIndexed { gIdx, (date, txns) ->
                // Date group header
                Box(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.outline.copy(0.05f))
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                ) {
                    Text(
                        date,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.3.sp
                    )
                }

                txns.forEachIndexed { idx, txn ->
                    TxRow(txn = txn)
                    if (idx < txns.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(0.07f)
                        )
                    }
                }

                if (gIdx < grouped.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.12f))
                }
            }

            // Show more / less
            if (transactions.size > 10) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.12f))
                Box(
                    modifier = Modifier.fillMaxWidth().clickable { onToggleAll() }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (showAll) "Show less" else "Show all ${transactions.size} transactions",
                        fontSize = 12.sp,
                        color = ColorAccent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TxRow(txn: ReportTransactionRow) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Avatar
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(
                if (txn.isExpense) ColorExpense.copy(alpha = 0.1f) else ColorIncome.copy(alpha = 0.1f)
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                txn.merchant.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (txn.isExpense) ColorExpense else ColorIncome,
            )
        }
        // Details
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                txn.merchant,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                CategoryPill(txn.category)
                Text("·", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(txn.timeLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (txn.paymentMethod != "—") {
                    Text("·", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        txn.paymentMethod,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        // Amount
        Text(
            text = if (txn.isExpense) "-${fmt(txn.amount)}" else "+${fmt(txn.amount)}",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (txn.isExpense) ColorExpense else ColorIncome,
        )
    }
}

@Composable
private fun CategoryPill(label: String) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ColorAccent.copy(alpha = 0.1f))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            label,
            fontSize = 9.sp,
            color = ColorAccent,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Export Bottom Bar ────────────────────────────────────────────────────────

@Composable
private fun ExportBar(
    isExporting: Boolean,
    onExport: (ExportFormat) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = DashboardDimens.screenPaddingH, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Export as",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.wrapContentWidth()
            )

            ExportFormatButton(
                label = "PDF",
                color = Color(0xFFEF4444),
                enabled = !isExporting,
                modifier = Modifier.weight(1f),
                onClick = { onExport(ExportFormat.PDF) },
            )
            ExportFormatButton(
                label = "CSV",
                color = Color(0xFF2ECC71),
                enabled = !isExporting,
                modifier = Modifier.weight(1f),
                onClick = { onExport(ExportFormat.CSV) },
            )
            ExportFormatButton(
                label = "Excel",
                color = Color(0xFF1D6F42),
                enabled = !isExporting,
                modifier = Modifier.weight(1f),
                onClick = { onExport(ExportFormat.EXCEL) },
            )
        }
    }
}

@Composable
private fun ExportFormatButton(
    label: String,
    color: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.12f),
            contentColor = color,
            disabledContainerColor = color.copy(alpha = 0.06f),
            disabledContentColor = color.copy(alpha = 0.4f),
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// ─── Export Bottom Sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportBottomSheet(
    isExporting: Boolean,
    onExport: (ExportFormat) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Export Report",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Choose a format to export and share this report.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))

            listOf(
                Triple(ExportFormat.PDF, "PDF Document", "Best for sharing and printing"),
                Triple(ExportFormat.CSV, "CSV Spreadsheet", "For data analysis and import"),
                Triple(ExportFormat.EXCEL, "Excel (.xls)", "For Microsoft Excel / Google Sheets"),
            ).forEach { (fmt, title, subtitle) ->
                val fmtColor = when (fmt) {
                    ExportFormat.PDF -> Color(0xFFEF4444)
                    ExportFormat.CSV -> Color(0xFF2ECC71)
                    ExportFormat.EXCEL -> Color(0xFF1D6F42)
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = fmtColor.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !isExporting) { onExport(fmt) },
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                                .background(fmtColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(fmt.name, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = fmtColor)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isExporting) CircularProgressIndicator(
                            Modifier.size(18.dp), color = fmtColor, strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}

// ─── Section Wrapper ──────────────────────────────────────────────────────────

@Composable
private fun ReportSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(ColorAccent))
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        content()
    }
}

// ─── Donut Chart ──────────────────────────────────────────────────────────────

@Composable
private fun DonutChart(
    segments: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 22f,
    gapDegrees: Float = 2f,
) {
    val total = segments.sum().coerceAtLeast(0.001f)
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        var startAngle = -90f
        segments.forEachIndexed { i, fraction ->
            val sweep = (fraction / total) * (360f - gapDegrees * segments.size)
            drawArc(
                color = colors.getOrElse(i) { Color.Gray },
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            startAngle += sweep + gapDegrees
        }
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun ReportDetailScreenPreview() {
    TruXpenseTheme { ReportDetailScreen(reportId = "preview") }
}