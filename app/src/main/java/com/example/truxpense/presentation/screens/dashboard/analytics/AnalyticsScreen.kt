package com.example.truxpense.presentation.screens.dashboard.analytics

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.screens.dashboard.components.SpendPoint
import com.example.truxpense.presentation.screens.dashboard.components.SpendingTrendCard
import com.example.truxpense.presentation.theme.DashboardDimens
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatINRFull(amount: Double) = "₹${"%,.0f".format(abs(amount))}"

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun AnalyticsScreen(
    // Legacy preview params kept so existing call-sites don't break
    totalSpent: Double = 0.0,
    totalBudget: Double = 0.0,
    changePercent: Int = 0,
    categories: List<CategorySpend> = emptyList(),
    vm: AnalyticsViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()

    // Don't render anything until Room has emitted its first real value — prevents skeleton flash.
    if (!state.roomLoaded) return

    // One-shot animation trigger for the donut chart: fires once when the screen is navigated to
    var chartAnimTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { chartAnimTriggered = true }
    val chartAnimProgress by animateFloatAsState(
        targetValue = if (chartAnimTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "chart_entry_anim",
    )

    // Trend chart animation: fires once when the trend card scrolls into view
    // LazyListState used by the list
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    val effectiveState = if (state.totalSpent > 0 || state.categories.isNotEmpty()) state
    else state.copy(
        totalSpent = totalSpent,
        totalBudget = totalBudget,
        changePercent = changePercent,
        categories = categories,
    )

    val summaryUtilisation = if (effectiveState.totalBudget > 0)
        (effectiveState.totalSpent / effectiveState.totalBudget).toFloat().coerceIn(0f, 1f) else 0f
    var summaryProgTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { summaryProgTriggered = true }
    val summaryProgressAnim by animateFloatAsState(
        targetValue = if (summaryProgTriggered) summaryUtilisation else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "summary_progress",
    )


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                headerTitle = "Analytics",
                showBack = false,
                modifier = Modifier.padding(bottom = DashboardDimens.spaceLg),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(
                start = DashboardDimens.screenPaddingH,
                end = DashboardDimens.screenPaddingH,
                bottom = DashboardDimens.spaceXxl,
            ),
            verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg),
        ) {

            // ── Period tabs ───────────────────────────────────────────────────
            item {
                PeriodTabs(
                    selected = effectiveState.period,
                    onSelect = { vm.setPeriod(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Date navigator ────────────────────────────────────────────────
            item {
                PeriodNavigatorRow(
                    label = effectiveState.periodLabel,
                    canBack = effectiveState.canGoBack,
                    canForward = effectiveState.canGoForward,
                    onBack = { vm.goBack() },
                    onForward = { vm.goForward() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Summary card ──────────────────────────────────────────────────
            item {
                SummaryCard(
                    state = effectiveState,
                    summaryProgressAnim = summaryProgressAnim,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Donut chart card ──────────────────────────────────────────────
            if (effectiveState.categories.isNotEmpty()) {
                item {
                    DonutChartCard(
                        categories = effectiveState.categories,
                        total = effectiveState.totalSpent,
                        chartAnimProgress = chartAnimProgress,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // ── Category legend ───────────────────────────────────────────
                item {
                    LabelsCard(
                        categories = effectiveState.categories,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ── Spending trend ────────────────────────────────────────────────
            if (effectiveState.trendPoints.isNotEmpty()) {
                item {
                    SpendingTrendCard(
                        points = effectiveState.trendPoints,
                        period = effectiveState.period,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ── Insights ──────────────────────────────────────────────────────
            if (effectiveState.topMerchant != null || effectiveState.topCategory != null) {
                item {
                    InsightsCard(state = effectiveState, modifier = Modifier.fillMaxWidth())
                }
            }

            item { Spacer(Modifier.height(DashboardDimens.spaceXxl)) }
        }
    }
}

// AnimatedSlideIn removed — charts use their own entry animations instead

// ── Period tabs ───────────────────────────────────────────────────────────────

@Composable
private fun PeriodTabs(
    selected: AnalyticsPeriod,
    onSelect: (AnalyticsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        AnalyticsPeriod.WEEK to "Week",
        AnalyticsPeriod.MONTH to "Month",
        AnalyticsPeriod.YEAR to "Year",
    )
    Row(
        modifier = modifier.clip(RoundedCornerShape(DashboardDimens.cornerChip))
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items.forEach { (period, label) ->
            val isSelected = period == selected
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(DashboardDimens.cornerToggleInner)).background(
                    if (isSelected) MaterialTheme.colorScheme.background
                    else Color.Transparent
                ).pointerInput(period) {
                    detectTapGestures { onSelect(period) }
                }.padding(vertical = DashboardDimens.spaceSm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Period navigator row ──────────────────────────────────────────────────────

@Composable
private fun PeriodNavigatorRow(
    label: String,
    canBack: Boolean,
    canForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onBack, enabled = canBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous",
                tint = if (canBack) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(DashboardDimens.iconSm),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        IconButton(onClick = onForward, enabled = canForward) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next",
                tint = if (canForward) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(DashboardDimens.iconSm),
            )
        }
    }
}

// ── Summary card ──────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(
    state: AnalyticsUiState,
    summaryProgressAnim: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val utilisation =
        if (state.totalBudget > 0) (state.totalSpent / state.totalBudget).toFloat().coerceIn(0f, 1f) else 0f
    // animProgress is driven from parent — hoisted so it survives recompositions
    val animProgress = summaryProgressAnim
    val barColor = when {
        utilisation >= 1f -> MaterialTheme.colorScheme.error
        utilisation >= 0.8f -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(DashboardDimens.cardElevation),
    ) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = formatINRFull(state.totalSpent),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "spent · ${state.periodLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.hasComparison) {
                    val up = state.changePercent >= 0
                    val sign = if (up) "▲" else "▼"
                    val col = if (up) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                    Surface(
                        shape = RoundedCornerShape(DashboardDimens.cornerChip),
                        color = col.copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = "$sign ${abs(state.changePercent)}%",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = col,
                        )
                    }
                }
            }

            if (state.totalBudget > 0) {
                Spacer(Modifier.height(DashboardDimens.spaceMdL))
                // Progress bar
                Box(
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                ) {
                    Box(
                        modifier = Modifier.fillMaxHeight().fillMaxWidth(animProgress).clip(RoundedCornerShape(3.dp))
                            .background(barColor),
                    )
                }
                Spacer(Modifier.height(DashboardDimens.spaceXs))
                Text(
                    text = "${formatINRFull(state.totalSpent)} of ${formatINRFull(state.totalBudget)} budget",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Donut chart card ──────────────────────────────────────────────────────────

@Composable
private fun DonutChartCard(
    categories: List<CategorySpend>,
    total: Double,
    chartAnimProgress: Float = 1f,
    modifier: Modifier = Modifier,
) {
    // Which segment is currently pressed (index or -1)
    var pressedIndex by remember { mutableIntStateOf(-1) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier.padding(DashboardDimens.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Spending by Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(DashboardDimens.spaceMdL))

            Box(
                modifier = Modifier.size(DashboardDimens.donutChartSize),
                contentAlignment = Alignment.Center,
            ) {
                InteractiveDonutChart(
                    categories = categories,
                    total = total,
                    pressedIndex = pressedIndex,
                    onPress = { pressedIndex = it },
                    onRelease = { pressedIndex = -1 },
                    entryProgress = chartAnimProgress,
                )
                // Center label: show pressed category name or total
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val centerText =
                        if (pressedIndex >= 0 && pressedIndex < categories.size) formatINRFull(categories[pressedIndex].amount)
                        else formatINRFull(total)

                    val centerLabel =
                        if (pressedIndex >= 0 && pressedIndex < categories.size) categories[pressedIndex].name
                        else "Total spent"

                    Text(
                        text = centerText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (pressedIndex >= 0) categories[pressedIndex].color
                        else MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = centerLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Floating tooltip when a segment is pressed
            if (pressedIndex >= 0 && pressedIndex < categories.size) {
                val cat = categories[pressedIndex]
                val pct = if (total > 0) ((cat.amount / total) * 100).toInt() else 0
                Spacer(Modifier.height(DashboardDimens.spaceMd))
                Surface(
                    shape = RoundedCornerShape(DashboardDimens.cornerCard),
                    color = cat.color.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = DashboardDimens.spaceMdL, vertical = DashboardDimens.spaceMd
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceSm),
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(cat.color, CircleShape))
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatINRFull(cat.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = cat.color,
                            )
                            Text(
                                text = "$pct% of total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Interactive donut ─────────────────────────────────────────────────────────

@Composable
private fun InteractiveDonutChart(
    categories: List<CategorySpend>,
    total: Double,
    pressedIndex: Int,
    onPress: (Int) -> Unit,
    onRelease: () -> Unit,
    entryProgress: Float = 1f,
) {
    if (total <= 0 || categories.isEmpty()) return

    val sweepAngles = categories.map { ((it.amount / total) * 360f).toFloat() }
    val density = LocalDensity.current
    val strokeWidth = with(density) { DashboardDimens.donutStrokeWidth.toPx() }

    // Scale each segment's sweep by entryProgress so arcs fill in from 0→full simultaneously
    val animSweeps = sweepAngles.map { it * entryProgress }

    // Animate pressed segment expansion
    val scaleFactors = sweepAngles.indices.map { i ->
        animateFloatAsState(
            targetValue = if (i == pressedIndex) 1.06f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "scale_$i",
        ).value
    }

    Canvas(
        modifier = Modifier.fillMaxSize().pointerInput(categories) {
            detectTapGestures(
                onPress = { tapOffset ->
                    // Determine which segment was tapped
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val dx = tapOffset.x - cx
                    val dy = tapOffset.y - cy
                    val dist = sqrt(dx * dx + dy * dy)
                    val radius = (minOf(size.width, size.height) / 2f)
                    val innerRadius = radius - strokeWidth * 1.5f
                    if (dist in innerRadius..radius) {
                        // Angle in degrees from top (-90° offset)
                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                        if (angle < 0f) angle += 360f
                        var cumAngle = 0f
                        val hit = sweepAngles.indexOfFirst { sweep ->
                            val start = cumAngle; cumAngle += sweep
                            angle in start..cumAngle
                        }
                        if (hit >= 0) onPress(hit)
                        tryAwaitRelease()
                        onRelease()
                    }
                },
            )
        },
    ) {
        // Use precomputed stroke width in pixels
        val sw = strokeWidth
        val diameter = size.minDimension - sw
        var startAngle = -90f

        animSweeps.forEachIndexed { i, sweep ->
            val scale = scaleFactors[i]
            val center = Offset(size.width / 2f, size.height / 2f)
            val tl = Offset(
                center.x - (diameter / 2f) * scale,
                center.y - (diameter / 2f) * scale,
            )
            val scaledArcSize = Size(diameter * scale, diameter * scale)
            drawArc(
                color = categories[i].color,
                startAngle = startAngle,
                sweepAngle = sweep - 2f,
                useCenter = false,
                topLeft = tl,
                size = scaledArcSize,
                style = Stroke(width = sw * scale, cap = StrokeCap.Butt),
            )
            startAngle += sweep
        }
    }
}

// ── Category legend ───────────────────────────────────────────────────────────

@Composable
private fun LabelsCard(
    categories: List<CategorySpend>,
    modifier: Modifier = Modifier,
) {
    val total = categories.sumOf { it.amount }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPadding)) {
            Text(
                text = "Breakdown",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = DashboardDimens.spaceMdL),
            )
            categories.forEachIndexed { i, cat ->
                val pct = if (total > 0) ((cat.amount / total) * 100).toInt() else 0
                // Keyed on cat.name so Animatable is stable per category across recompositions.
                // hasAnimated flag ensures animateTo only fires once, not on every screen focus.
                val animPct = remember(cat.name) { Animatable(0f) }
                val barAnimDone = remember(cat.name) { mutableStateOf(false) }
                LaunchedEffect(cat.name) {
                    if (!barAnimDone.value) {
                        barAnimDone.value = true
                        animPct.animateTo(
                            targetValue = pct / 100f,
                            animationSpec = tween(
                                durationMillis = 800,
                                delayMillis = i * 80,
                                easing = FastOutSlowInEasing
                            ),
                        )
                    }
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = DashboardDimens.spaceMd),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(cat.color, CircleShape))
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
                        ) {
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = formatINRFull(cat.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                    // Percentage bar
                    Box(
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(1.5.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxHeight().fillMaxWidth(animPct.value)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(cat.color),
                        )
                    }
                    if (i < categories.lastIndex) {
                        Spacer(Modifier.height(DashboardDimens.spaceXs))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

// ── Spending trend card ───────────────────────────────────────────────────────

@Composable
private fun SpendingTrendCard(
    points: List<TrendPoint>,
    period: AnalyticsPeriod,
    modifier: Modifier = Modifier,
) {
    val title = when (period) {
        AnalyticsPeriod.WEEK -> "Daily spending — this week"
        AnalyticsPeriod.MONTH -> "Weekly spending — this month"
        AnalyticsPeriod.YEAR -> "Monthly spending — this year"
    }

    // Convert analytics TrendPoint → shared SpendPoint
    val spendPoints = remember(points) {
        points.map { tp ->
            SpendPoint(
                amount = tp.amount.toFloat(),
                xLabel = tp.label,
                tooltipDate = tp.tooltipDate,
                isToday = false,
            )
        }
    }

    SpendingTrendCard(
        points = spendPoints,
        modifier = modifier,
        title = title,
        pillLabel = null,
        showStats = false,
        chartHeight = DashboardDimens.trendChartHeight,
    )
}

// ── Insights card ─────────────────────────────────────────────────────────────

@Composable
private fun InsightsCard(
    state: AnalyticsUiState,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPadding)) {
            Text(
                text = "Insights",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = DashboardDimens.spaceMdL),
            )
            state.topCategory?.let { (cat, amt) ->
                InsightRow(
                    icon = "🔥",
                    label = "Top category",
                    value = "$cat · ${formatINRFull(amt)}",
                )
            }
            state.topMerchant?.let { (merchant, amt) ->
                InsightRow(
                    icon = "🏪",
                    label = "Top merchant",
                    value = "$merchant · ${formatINRFull(amt)}",
                )
            }
            if (state.hasComparison) {
                val arrow = if (state.changePercent >= 0) "📈" else "📉"
                val sign = if (state.changePercent >= 0) "up" else "down"
                InsightRow(
                    icon = arrow,
                    label = "vs. last period",
                    value = "$sign ${abs(state.changePercent)}%",
                )
            }
        }
    }
}

@Composable
private fun InsightRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = DashboardDimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
    ) {
        Text(text = icon, fontSize = 16.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

val sampleCategories = listOf(
    CategorySpend("Food", 4250.0, Color(0xFFE53935)),
    CategorySpend("Shopping", 2250.0, Color(0xFFFFA726)),
    CategorySpend("Transport", 450.0, Color(0xFF1E88E5)),
    CategorySpend("Bills", 340.0, Color(0xFF43A047)),
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AnalyticsScreenPreview() {
    MaterialTheme {
        AnalyticsScreen(
            totalSpent = 7290.0,
            totalBudget = 20_000.0,
            changePercent = 12,
            categories = sampleCategories,
        )
    }
}