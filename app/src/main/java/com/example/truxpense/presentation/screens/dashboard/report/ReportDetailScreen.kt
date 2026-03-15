package com.example.truxpense.presentation.screens.dashboard.report

import android.os.Environment
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.truxpense.R
import com.example.truxpense.data.repository.report.ReportCategoryRow
import com.example.truxpense.data.repository.report.ReportTransactionRow
import com.example.truxpense.data.repository.report.ReportTrendPoint
import com.example.truxpense.data.repository.report.ReportType
import com.example.truxpense.presentation.screens.dashboard.components.AppConfirmDialog
import com.example.truxpense.presentation.screens.dashboard.components.GradientCard
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.screens.dashboard.vault.VaultSaveBottomSheet
import com.example.truxpense.presentation.theme.DashboardDimens
import java.io.File
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

// ─── Bar color constants (mirror BudgetDetailScreen palette) ─────────────────

private val BarTeal = Color(0xFF1BAF9D)
private val BarAmber = Color(0xFFF5A623)
private val BarRed = Color(0xFFE53935)
private val BarEmpty = Color(0xFFE0E4EA)

// ─── Formatter ────────────────────────────────────────────────────────────────

private fun fmt(amount: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"))
    nf.maximumFractionDigits = 0
    return "₹${nf.format(abs(amount))}"
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun ReportDetailScreen(
    reportId: String,
    onBack: () -> Unit = {},
    onNavigateToVault: () -> Unit = {},
    vm: ReportDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val deleteComplete by vm.deleteComplete.collectAsStateWithLifecycle()
    val exportStatus by vm.exportStatus.collectAsStateWithLifecycle()
    val exportResult by vm.exportResult.collectAsStateWithLifecycle()
    val showAllTx by vm.showAllTransactions.collectAsStateWithLifecycle()
    val vaultStatus by vm.vaultSaveStatus.collectAsStateWithLifecycle()
    val vaultError by vm.vaultSaveError.collectAsStateWithLifecycle()

    val snackbarHost = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showVaultSheet by remember { mutableStateOf(false) }
    var isShareExport by remember { mutableStateOf(false) }

    LaunchedEffect(reportId) { vm.loadReport(reportId) }
    LaunchedEffect(deleteComplete) {
        if (deleteComplete) {
            vm.onDeleteHandled(); onBack()
        }
    }
    LaunchedEffect(exportResult) {
        val r = exportResult ?: return@LaunchedEffect
        if (r.error == null) {
            try {
                val src = File(r.filePath)
                if (!src.exists()) {
                    snackbarHost.showSnackbar("Export failed: file not found")
                    vm.consumeExportResult(); return@LaunchedEffect
                }
                if (isShareExport) {
                    ReportExporter.share(context, r); isShareExport = false
                } else {
                    val dir =
                        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS
                        )
                        else File(context.cacheDir, "Download")
                    dir?.mkdirs()
                    src.copyTo(File(dir, r.fileName), overwrite = true)
                    snackbarHost.showSnackbar("Saved to Downloads: ${r.fileName}")
                }
                vm.consumeExportResult()
            } catch (e: Exception) {
                snackbarHost.showSnackbar("Export failed: ${e.localizedMessage}")
                vm.consumeExportResult()
            }
        } else {
            snackbarHost.showSnackbar("Export failed: ${r.error}")
            vm.consumeExportResult()
        }
    }
    LaunchedEffect(vaultStatus) {
        when (vaultStatus) {
            VaultSaveStatus.SUCCESS -> {
                val res =
                    snackbarHost.showSnackbar("Saved to Vault", actionLabel = "View", duration = SnackbarDuration.Long)
                if (res == SnackbarResult.ActionPerformed) onNavigateToVault()
                vm.resetVaultSaveStatus()
            }

            VaultSaveStatus.ERROR -> {
                snackbarHost.showSnackbar("Save failed: ${vaultError ?: "Unknown error"}")
                vm.resetVaultSaveStatus()
            }

            else -> Unit
        }
    }
    LaunchedEffect(vaultStatus) { if (vaultStatus == VaultSaveStatus.SUCCESS) showVaultSheet = false }

    if (showDeleteDialog) {
        AppConfirmDialog(
            title = "Delete report?",
            message = "\"${uiState.title}\" will be permanently removed.",
            confirmLabel = "Delete",
            onConfirm = { showDeleteDialog = false; vm.deleteReport() },
            onDismiss = { showDeleteDialog = false },
        )
    }
    if (showExportSheet) {
        ExportBottomSheet(
            isExporting = exportStatus == ExportStatus.EXPORTING,
            isShare = isShareExport,
            onExport = { fmt -> showExportSheet = false; vm.export(fmt) },
            onDismiss = { showExportSheet = false },
        )
    }
    if (showVaultSheet) {
        VaultSaveBottomSheet(
            reportTitle = uiState.title,
            isSaving = vaultStatus == VaultSaveStatus.SAVING,
            saveError = if (vaultStatus == VaultSaveStatus.ERROR) vaultError else null,
            onSave = { format, storage, tags -> vm.saveToVault(format, storage, tags) },
            onRetry = { vm.resetVaultSaveStatus() },
            onDismiss = {
                if (vaultStatus != VaultSaveStatus.SAVING) {
                    showVaultSheet = false; vm.resetVaultSaveStatus()
                }
            },
        )
    }

    // ── Guards ────────────────────────────────────────────────────────────────

    if (!uiState.isLoaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }
    if (uiState.notFound) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Report not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    // ── Scaffold ──────────────────────────────────────────────────────────────

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            ScreenTopBar(
                headerTitle = uiState.title,
                showBack = true,
                onBack = onBack,
                actions = {
                    IconButton(onClick = onNavigateToVault) {
                        Icon(
                            painter = painterResource(R.drawable.vault),
                            contentDescription = "Open Vault",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(DashboardDimens.iconMd),
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = "Delete report",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(DashboardDimens.iconSm),
                        )
                    }
                    IconButton(onClick = { isShareExport = true; showExportSheet = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
        },
        bottomBar = {
            ReportBottomBar(
                isExporting = exportStatus == ExportStatus.EXPORTING,
                isSavingVault = vaultStatus == VaultSaveStatus.SAVING,
                onExport = { vm.export(it) },
                onSaveToVault = { showVaultSheet = true },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())
                .padding(horizontal = DashboardDimens.screenPaddingH),
        ) {
            Spacer(Modifier.height(4.dp))

            // ① Hero
            ReportHeroHeader(uiState = uiState)
            Spacer(Modifier.height(24.dp))

            // ② Category breakdown — interactive donut (Analytics-style)
            if (uiState.categoryRows.isNotEmpty()) {
                ReportSection("Spending by category") { CategoryCard(rows = uiState.categoryRows) }
                Spacer(Modifier.height(24.dp))
            }

            // ③ Spending trend — vertical bar chart with smart granularity
            if (uiState.trendPoints.isNotEmpty()) {
                ReportSection("Spending trend") {
                    TrendCard(
                        points = uiState.trendPoints,
                        granularity = uiState.trendGranularity,
                        onGranularity = { vm.setGranularity(it) },
                        spanDays = uiState.spanDays,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // ④ Highlights
            val hasInsights =
                uiState.topMerchant != null || uiState.topCategory != null || uiState.highestSpendDay != null
            if (hasInsights) {
                ReportSection("Highlights") { InsightsCard(uiState = uiState) }
                Spacer(Modifier.height(24.dp))
            }

            // ⑤ Transactions
            if (uiState.transactions.isNotEmpty()) {
                ReportSection("Transactions (${uiState.transactionCount})") {
                    TransactionListCard(
                        transactions = uiState.transactions,
                        showAll = showAllTx,
                        onToggleAll = { vm.toggleShowAll() },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Hero Header ──────────────────────────────────────────────────────────────

@Composable
private fun ReportHeroHeader(uiState: ReportDetailUiState) {
    val primary = MaterialTheme.colorScheme.primary
    val colorErr = MaterialTheme.colorScheme.error
    val colorSec = MaterialTheme.colorScheme.secondary

    GradientCard(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            val typeLabel = when (uiState.reportType) {
                ReportType.EXPENSE -> "Expense Report"
                ReportType.INCOME -> "Income Report"
                ReportType.ALL -> "Full Financial Report"
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(primary.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) { Text(typeLabel, fontSize = 10.sp, color = primary, fontWeight = FontWeight.SemiBold) }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(
                    painter = painterResource(R.drawable.calender), contentDescription = null,
                    modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(uiState.dateRangeLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(18.dp))

            if (uiState.reportType == ReportType.ALL) {
                // 2 × 2 grid — no ellipsis needed, each stat has full half-width
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeroStat("Total expenses", fmt(uiState.totalExpenses), colorErr, modifier = Modifier.weight(1f))
                        HeroStat("Total income", fmt(uiState.totalIncome), colorSec, modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeroStat(
                            "Net",
                            fmt(uiState.netAmount),
                            if (uiState.netAmount >= 0) colorSec else colorErr,
                            modifier = Modifier.weight(1f)
                        )
                        HeroStat(
                            "Transactions",
                            uiState.transactionCount.toString(),
                            MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    if (uiState.reportType != ReportType.INCOME) HeroStat(
                        "Total expenses", fmt(uiState.totalExpenses), colorErr
                    )
                    if (uiState.reportType != ReportType.EXPENSE) HeroStat(
                        "Total income", fmt(uiState.totalIncome), colorSec
                    )
                    HeroStat(
                        "Transactions", uiState.transactionCount.toString(), MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

// ─── Category Card — interactive donut (Analytics-screen pattern) ─────────────

@Composable
private fun CategoryCard(rows: List<ReportCategoryRow>) {
    val total = rows.sumOf { it.amount }.coerceAtLeast(0.01)
    var pressedIdx by remember { mutableIntStateOf(-1) }
    val entryAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entryAnim.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }

    val pressed = pressedIdx.takeIf { it in rows.indices }
    val centerAmt = pressed?.let { rows[it].amount } ?: total
    val centerColor = pressed?.let { rows[it].color } ?: MaterialTheme.colorScheme.primary
    val centerLabel = pressed?.let { rows[it].name } ?: "Total"
    val centerPct = pressed?.let { if (total > 0) "${((rows[it].amount / total) * 100).toInt()}%" else "" } ?: ""

    GradientCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPadding)) {

            Text(
                "Spending by category",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(16.dp))

            // ── Interactive donut ─────────────────────────────────────────────
            Box(
                modifier = Modifier.size(DashboardDimens.donutChartSize).align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center,
            ) {
                InteractiveDonutChart(
                    rows = rows,
                    total = total,
                    pressedIndex = pressedIdx,
                    onPress = { pressedIdx = it },
                    onRelease = { pressedIdx = -1 },
                    entryProgress = entryAnim.value,
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text(
                        fmt(centerAmt),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = centerColor,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    if (centerPct.isNotEmpty()) {
                        Text(
                            centerPct,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = centerColor.copy(alpha = 0.75f)
                        )
                    }
                    Text(
                        centerLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Breakdown list ────────────────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), thickness = 0.5.dp)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Breakdown",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    fmt(total),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            rows.forEachIndexed { i, row ->
                val pct = if (total > 0) ((row.amount / total) * 100).toInt() else 0
                val animPct = remember(row.name) { Animatable(0f) }
                val animated = remember(row.name) { mutableStateOf(false) }
                LaunchedEffect(row.name) {
                    if (!animated.value) {
                        animated.value = true
                        animPct.animateTo(pct / 100f, tween(750, delayMillis = i * 75, easing = FastOutSlowInEasing))
                    }
                }
                val isPressed = i == pressedIdx

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(row.color))
                            Text(
                                row.name,
                                fontSize = 13.sp,
                                fontWeight = if (isPressed) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(shape = RoundedCornerShape(20.dp), color = row.color.copy(alpha = 0.12f)) {
                                Text(
                                    "$pct%",
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = row.color
                                )
                            }
                            Text(
                                fmt(row.amount),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    // Animated fill bar
                    Box(
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxHeight().fillMaxWidth(animPct.value)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Brush.horizontalGradient(listOf(row.color.copy(0.55f), row.color)))
                        )
                    }
                }
                if (i < rows.lastIndex) Spacer(Modifier.height(2.dp))
            }
        }
    }
}

// ─── Interactive Donut Chart (copied from AnalyticsScreen) ───────────────────

@Composable
private fun InteractiveDonutChart(
    rows: List<ReportCategoryRow>,
    total: Double,
    pressedIndex: Int,
    onPress: (Int) -> Unit,
    onRelease: () -> Unit,
    entryProgress: Float = 1f,
) {
    if (total <= 0 || rows.isEmpty()) return

    val sweepAngles = rows.map { ((it.amount / total) * 360f).toFloat() }
    val density = LocalDensity.current
    val strokeWidth = with(density) { DashboardDimens.donutStrokeWidth.toPx() }
    val animSweeps = sweepAngles.map { it * entryProgress }

    val scaleFactors = sweepAngles.indices.map { i ->
        animateFloatAsState(
            targetValue = if (i == pressedIndex) 1.07f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "scale_$i",
        ).value
    }

    Canvas(
        modifier = Modifier.fillMaxSize().pointerInput(rows) {
            detectTapGestures(
                onPress = { tap ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val dx = tap.x - cx
                    val dy = tap.y - cy
                    val dist = sqrt(dx * dx + dy * dy)
                    val radius = minOf(size.width, size.height) / 2f
                    val inner = radius - strokeWidth * 1.5f
                    if (dist in inner..radius) {
                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                        if (angle < 0f) angle += 360f
                        var cum = 0f
                        val hit = sweepAngles.indexOfFirst { s -> val s0 = cum; cum += s; angle in s0..cum }
                        if (hit >= 0) onPress(hit)
                        tryAwaitRelease(); onRelease()
                    }
                },
            )
        },
    ) {
        val diameter = size.minDimension - strokeWidth
        var start = -90f
        animSweeps.forEachIndexed { i, sweep ->
            val scale = scaleFactors[i]
            val center = Offset(size.width / 2f, size.height / 2f)
            val tl = Offset(center.x - (diameter / 2f) * scale, center.y - (diameter / 2f) * scale)
            drawArc(
                color = rows[i].color,
                startAngle = start,
                sweepAngle = sweep - 2f,
                useCenter = false,
                topLeft = tl,
                size = Size(diameter * scale, diameter * scale),
                style = Stroke(width = strokeWidth * scale, cap = StrokeCap.Butt),
            )
            start += sweep
        }
    }
}

// ─── Trend Card ───────────────────────────────────────────────────────────────
// Granularity pill visibility rules:
//   • Daily  — always shown
//   • Weekly — only when spanDays > 7
//   • Monthly — only when spanDays > 31

@Composable
private fun TrendCard(
    points: List<ReportTrendPoint>,
    granularity: ReportTrendGranularity,
    onGranularity: (ReportTrendGranularity) -> Unit,
    spanDays: Int,
) {
    val available = remember(spanDays) {
        buildList {
            add(ReportTrendGranularity.DAILY)
            if (spanDays > 7) add(ReportTrendGranularity.WEEKLY)
            if (spanDays > 31) add(ReportTrendGranularity.MONTHLY)
        }
    }
    LaunchedEffect(available) {
        if (granularity !in available) onGranularity(available.first())
    }

    val sortedAmounts = remember(points) { points.map { it.amount }.sortedDescending() }
    fun barColor(amount: Double): Color = when {
        amount <= 0 -> BarEmpty
        amount == sortedAmounts.firstOrNull() -> BarRed
        sortedAmounts.size > 1 && amount == sortedAmounts[1] -> BarAmber
        else -> BarTeal
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(points, granularity) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }

    val labelTransform: (String) -> String = if (granularity == ReportTrendGranularity.DAILY) { label ->
        label.trim().split(" ").firstOrNull { it.toIntOrNull() != null } ?: label
    } else {
        { it }
    }

    GradientCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // ── Granularity pill ──────────────────────────────────────────────
            if (available.size > 1) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)).padding(3.dp),
                ) {
                    available.forEach { g ->
                        val label = when (g) {
                            ReportTrendGranularity.DAILY -> "Daily"
                            ReportTrendGranularity.WEEKLY -> "Weekly"
                            ReportTrendGranularity.MONTHLY -> "Monthly"
                        }
                        val selected = granularity == g
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onGranularity(g) }.padding(horizontal = 12.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Daily",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            // ── Chart ─────────────────────────────────────────────────────────
            VerticalBarChart(
                points = points,
                barColorFn = { barColor(it) },
                animProgress = animProgress.value,
                modifier = Modifier.fillMaxWidth().height(180.dp),
                labelTransform = labelTransform,
            )
        }
    }
}

// ─── Vertical Bar Chart ───────────────────────────────────────────────────────
// Left y-axis with smart nice-interval grid lines and compact amount labels.

private fun niceYInterval(maxVal: Double, steps: Int = 4): Double {
    if (maxVal <= 0) return 1.0
    val raw = maxVal / steps
    val mag = Math.pow(10.0, Math.floor(Math.log10(raw)))
    val norm = raw / mag
    val nice = when {
        norm <= 1.5 -> 1.0
        norm <= 3.5 -> 2.0
        norm <= 7.5 -> 5.0
        else -> 10.0
    }
    return nice * mag
}

private fun compactAmount(value: Double): String = when {
    value >= 1_00_000 -> "₹${"%.0f".format(value / 1_00_000)}L"
    value >= 1_000 -> "₹${"%.0f".format(value / 1_000)}K"
    else -> "₹${"%.0f".format(value)}"
}

@Composable
private fun VerticalBarChart(
    points: List<ReportTrendPoint>,
    barColorFn: (Double) -> Color,
    animProgress: Float,
    modifier: Modifier = Modifier,
    labelTransform: (String) -> String = { it },
) {
    val display = points.ifEmpty { listOf(ReportTrendPoint("–", 0.0, "–")) }
    val maxVal = display.maxOf { it.amount }.coerceAtLeast(1.0)

    val density = LocalDensity.current
    val xLabelSzPx = with(density) { 10.sp.toPx() }
    val yLabelSzPx = with(density) { 9.sp.toPx() }
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val gridColor = MaterialTheme.colorScheme.outline
    val labelColorInt = onBgColor.copy(alpha = 0.75f).toArgb()
    val gridColorInt = gridColor.copy(alpha = 0.10f).toArgb()

    // Pre-compute y-axis intervals outside the Canvas draw loop
    val interval = remember(maxVal) { niceYInterval(maxVal) }
    val steps = remember(maxVal, interval) {
        val count = Math.ceil(maxVal / interval).toInt().coerceIn(2, 6)
        (1..count).map { it * interval }
    }

    Canvas(modifier = modifier.clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))) {
        val w = size.width
        val h = size.height

        val labelH = with(density) { 24.dp.toPx() }   // x-axis label row height
        val topPad = with(density) { 8.dp.toPx() }    // breathing room above tallest bar
        val yAxisW = with(density) { 42.dp.toPx() }   // left margin for y-axis labels

        val chartH = h - labelH - topPad              // drawable bar area height
        val chartW = w - yAxisW                       // drawable bar area width

        val n = display.size
        val gap = with(density) { 5.dp.toPx() }
        val barW = ((chartW - gap * (n + 1)) / n).coerceAtLeast(6f)
        val cornerR = barW * 0.20f

        // ── Y-axis grid lines + labels ────────────────────────────────────
        steps.forEach { stepVal ->
            if (stepVal > maxVal * 1.05) return@forEach  // skip lines above data
            val lineY = topPad + chartH * (1f - (stepVal / maxVal).toFloat())

            // Dashed horizontal grid line
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawLine(
                    yAxisW, lineY, w, lineY, android.graphics.Paint().apply {
                        setColor(gridColorInt)
                        strokeWidth = with(density) { 1.dp.toPx() }
                        pathEffect = android.graphics.DashPathEffect(
                            floatArrayOf(with(density) { 4.dp.toPx() }, with(density) { 4.dp.toPx() }), 0f
                        )
                    })
                // Y-axis label — right-aligned inside the left margin
                canvas.nativeCanvas.drawText(
                    compactAmount(stepVal),
                    yAxisW - with(density) { 6.dp.toPx() },
                    lineY + yLabelSzPx / 2.5f,
                    android.graphics.Paint().apply {
                        textSize = yLabelSzPx
                        textAlign = android.graphics.Paint.Align.RIGHT
                        setColor(labelColorInt)
                    })
            }
        }

        // ── Bars + x-axis labels ──────────────────────────────────────────
        display.forEachIndexed { i, pt ->
            val x = yAxisW + gap + i * (barW + gap)
            val frac = if (pt.amount > 0) (pt.amount / maxVal * animProgress).toFloat() else 0f
            val barH = (chartH * frac).coerceAtLeast(0f)
            val barTop = topPad + (chartH - barH)
            var color = barColorFn(pt.amount)

            if (pt.amount > 0) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, barTop),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(cornerR, cornerR),
                )
            } else {
                val dashY = topPad + chartH - with(density) { 4.dp.toPx() }
                drawLine(
                    BarEmpty,
                    Offset(x + barW * 0.25f, dashY),
                    Offset(x + barW * 0.75f, dashY),
                    strokeWidth = with(density) { 2.dp.toPx() },
                    cap = StrokeCap.Round,
                )
            }

            // X-axis label
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    labelTransform(pt.label),
                    x + barW / 2,
                    h - with(density) { 4.dp.toPx() },
                    android.graphics.Paint().apply {
                        textSize = xLabelSzPx
                        textAlign = android.graphics.Paint.Align.CENTER
                        setColor(labelColorInt)
                    },
                )
            }
        }
    }
}

// ─── Insights Card ────────────────────────────────────────────────────────────

@Composable
private fun InsightsCard(uiState: ReportDetailUiState) {
    data class Insight(val label: String, val name: String, val amount: String, val color: Color)

    val colorErr = MaterialTheme.colorScheme.error
    val colorPri = MaterialTheme.colorScheme.primary
    val colorTert = MaterialTheme.colorScheme.tertiary
    val colorSec = MaterialTheme.colorScheme.secondary

    val items = buildList {
        uiState.topMerchant?.let { add(Insight("Top merchant", it.first, fmt(it.second), colorErr)) }
        uiState.topCategory?.let { add(Insight("Top category", it.first, fmt(it.second), colorPri)) }
        uiState.highestSpendDay?.let { add(Insight("Highest spend day", it.first, fmt(it.second), colorTert)) }
        add(Insight("Avg. daily spend", "", fmt(uiState.avgDailySpend), colorSec))
    }

    GradientCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            items.forEachIndexed { idx, item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.width(3.dp).height(34.dp).clip(RoundedCornerShape(2.dp)).background(item.color))
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(item.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.name.isNotEmpty()) {
                            Text(
                                item.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(item.amount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = item.color)
                }
                if (idx < items.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.07f))
            }
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
    val grouped = displayed.groupBy { it.dateLabel }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(bottom = 4.dp)) {
            grouped.entries.forEachIndexed { gIdx, (date, txns) ->
                Box(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.outline.copy(0.05f))
                        .padding(horizontal = 16.dp, vertical = 7.dp)
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
                    if (idx < txns.lastIndex) HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(0.07f)
                    )
                }
                if (gIdx < grouped.size - 1) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.1f))
            }
            if (transactions.size > 10) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.1f))
                Box(
                    modifier = Modifier.fillMaxWidth().clickable { onToggleAll() }.padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showAll) "Show less" else "Show all ${transactions.size} transactions",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TxRow(txn: ReportTransactionRow) {
    val txColor = if (txn.isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(txColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(txn.merchant.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = txColor)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                txn.merchant,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                CategoryPill(txn.category)
            }
        }
        Column(
            modifier = Modifier.wrapContentWidth(align = Alignment.End),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                if (txn.isExpense) "-${fmt(txn.amount)}" else "+${fmt(txn.amount)}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = txColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
            Row(
                modifier = Modifier.wrapContentWidth(align = Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(txn.timeLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (txn.paymentMethod != "—")
                    Text("·", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (txn.paymentMethod != "—") {
                    Text(
                        txn.paymentMethod,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryPill(label: String) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            label,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Bottom Bar ───────────────────────────────────────────────────────────────

@Composable
private fun ReportBottomBar(
    isExporting: Boolean,
    isSavingVault: Boolean,
    onExport: (ExportFormat) -> Unit,
    onSaveToVault: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = DashboardDimens.screenPaddingH, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExportFormatButton(
                    "PDF", MaterialTheme.colorScheme.error, !isExporting, Modifier.weight(1f)
                ) { onExport(ExportFormat.PDF) }
                ExportFormatButton(
                    "CSV", MaterialTheme.colorScheme.secondary, !isExporting, Modifier.weight(1f)
                ) { onExport(ExportFormat.CSV) }
                ExportFormatButton(
                    "Excel", MaterialTheme.colorScheme.tertiary, !isExporting, Modifier.weight(1f)
                ) { onExport(ExportFormat.EXCEL) }
            }
            Button(
                onClick = onSaveToVault,
                enabled = !isSavingVault && !isExporting,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isSavingVault) {
                    CircularProgressIndicator(
                        Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Saving…",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                } else {
                    Icon(
                        painterResource(R.drawable.vault),
                        null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Save to Vault",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportFormatButton(
    label: String, color: Color, enabled: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit,
) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.12f), contentColor = color,
            disabledContainerColor = color.copy(alpha = 0.06f), disabledContentColor = color.copy(alpha = 0.4f),
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp), contentPadding = PaddingValues(horizontal = 10.dp),
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.width(5.dp))
        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
    }
}

// ─── Export Bottom Sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportBottomSheet(
    isExporting: Boolean, isShare: Boolean,
    onExport: (ExportFormat) -> Unit, onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                if (isShare) "Share" else "Download",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                if (isShare) "Select the format to share." else "Select the report format.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))

            listOf(
                Triple(ExportFormat.PDF, "PDF Document", "Best for sharing and printing"),
                Triple(ExportFormat.CSV, "CSV Spreadsheet", "For data analysis and import"),
                Triple(ExportFormat.EXCEL, "Excel (.xlsx)", "For Microsoft Excel / Google Sheets"),
            ).forEach { (fmt, title, subtitle) ->
                val fmtColor = when (fmt) {
                    ExportFormat.PDF -> MaterialTheme.colorScheme.error
                    ExportFormat.CSV -> MaterialTheme.colorScheme.secondary
                    ExportFormat.EXCEL -> MaterialTheme.colorScheme.tertiary
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = fmtColor.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !isExporting) { onExport(fmt) }) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                                .background(fmtColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center
                        ) {
                            Text(fmt.name, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = fmtColor)
                        }
                        Column(Modifier.weight(1f)) {
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

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
private fun ReportSection(title: String, content: @Composable () -> Unit) {
    content()
}