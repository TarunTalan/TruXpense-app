package com.example.truxpense.presentation.screens.dashboard.analytics

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.*
import com.example.truxpense.presentation.theme.DashboardDimens
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun fmt(amount: Double) = "₹${"%,.0f".format(abs(amount))}"

private val MONTH_LABELS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

private val sampleCategories = listOf(
    CategorySpend("Food", 3_200.0, Color(0xFFEF4444)),
    CategorySpend("Shopping", 1_420.0, Color(0xFF10B981)),
    CategorySpend("Transport", 950.0, Color(0xFFF59E0B)),
    CategorySpend("Bills", 600.0, Color(0xFF3B82F6)),
)
private val sampleTrend = listOf(
    TrendPoint("W1", 1_200.0, "1–7 Mar"),
    TrendPoint("W2", 1_700.0, "8–14 Mar"),
    TrendPoint("W3", 900.0, "15–21 Mar"),
    TrendPoint("W4", 2_000.0, "22–28 Mar"),
)

// ── Gradient brush helper ─────────────────────────────────────────────────────

// A finite diagonal end keeps the gradient fully resolved inside the clipped card bounds.
// Float.POSITIVE_INFINITY causes Compose to fail resolving the gradient when the card is
// inside a nested clipped container, producing the blurry / bleeding artefact.
private fun cardBrush(c1: Color, c2: Color, c3: Color? = null): Brush = Brush.linearGradient(
    colors = if (c3 != null) listOf(c1, c2, c3) else listOf(c1, c2),
    start = Offset(0f, 0f),
    end = Offset(900f, 900f),          // large enough for any card, always finite
)

// ══════════════════════════════════════════════════════════════════════════════
// SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    vm: AnalyticsViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()
    val filterCategory by vm.filterCategory.collectAsState()
    val filterMonth by vm.filterMonth.collectAsState()
    val filterYear by vm.filterYear.collectAsState()
    val filterDateFrom by vm.filterDateFrom.collectAsState()
    val filterDateTo by vm.filterDateTo.collectAsState()
    val activeFilterCount by vm.activeFilterCount.collectAsState()
    val availableCategories by vm.availableCategories.collectAsState()
    val availableYears by vm.availableYears.collectAsState()
    val selectedPeriod by vm.selectedPeriod.collectAsState()
    val filterType by vm.filterType.collectAsState()

    if (!state.roomLoaded) return

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Donut entry animation
    var chartAnimTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { chartAnimTriggered = true }
    val chartAnimProgress by animateFloatAsState(
        targetValue = if (chartAnimTriggered) 1f else 0f,
        animationSpec = tween(950, easing = FastOutSlowInEasing),
        label = "donut_entry",
    )

    // Budget progress animation
    val utilisation =
        if (state.totalBudget > 0) (state.totalSpent / state.totalBudget).toFloat().coerceIn(0f, 1f) else 0f
    var progTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { progTriggered = true }
    val progressAnim by animateFloatAsState(
        targetValue = if (progTriggered) utilisation else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "budget_progress",
    )

    val hasFilters =
        filterCategory != null || filterMonth != null || filterYear != null || filterType != null || filterDateFrom != null || filterDateTo != null

    // --------------------------
    // Apply filters to stats
    // --------------------------
    // Note: data shapes available in `state` are limited (TrendPoint.tooltipDate is a string).
    // We apply pragmatic string-based filtering for month/year and category name filtering for categories.
    // Date-range filtering is best handled inside the ViewModel where raw transaction dates exist; here
    // we provide reasonable client-side filtering so the cards immediately reflect selected filters.

    fun trendPointMatchesTimeFilter(tp: TrendPoint): Boolean {
        // Safely capture delegated properties to local vals to avoid smart-cast issues
        val fm = filterMonth
        if (fm != null) {
            val mlabel = MONTH_LABELS.getOrNull(fm - 1) ?: ""
            if (!tp.tooltipDate.contains(mlabel, ignoreCase = true) && !tp.label.contains(mlabel, ignoreCase = true)) return false
        }
        val fy = filterYear
        if (fy != null) {
            if (!tp.tooltipDate.contains(fy.toString())) return false
        }
        // We do not robustly parse dateFrom/dateTo here — keep those for VM filtering.
        return true
    }

    val filteredTrendPoints by remember(state.trendPoints, filterMonth, filterYear, filterDateFrom, filterDateTo) {
        derivedStateOf {
            state.trendPoints.filter { trendPointMatchesTimeFilter(it) }
        }
    }

    val filteredCategories by remember(state.categories, filterCategory) {
        derivedStateOf {
            if (filterCategory.isNullOrBlank()) state.categories
            else state.categories.filter { it.name.equals(filterCategory, ignoreCase = true) }
        }
    }

    val filteredTotalSpent by remember(filteredCategories) { derivedStateOf { filteredCategories.sumOf { it.amount } } }

    val topCategoryFiltered by remember(filteredCategories) {
        derivedStateOf {
            filteredCategories.maxByOrNull { it.amount }?.let { it.name to it.amount }
        }
    }

    val topMerchantFiltered by remember(state.topMerchant, filterCategory) {
        derivedStateOf {
            // If category filter is present we conservatively drop top-merchant (we lack merchant → category mapping here).
            if (!filterCategory.isNullOrBlank()) null else state.topMerchant
        }
    }

    // --------------------------
    // UI
    // --------------------------

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                headerTitle = "Analytics",
                showBack = false,
                actions = {
                    // Glassy filter button with badge
                    Box {
                        IconButton(
                            onClick = { showFilterSheet = true },
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(DashboardDimens.cornerCard))
                        ) {
                            Box(
                                modifier = Modifier.size(38.dp).border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                                    CircleShape
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                // blurred background layer (behind the icon)
                                Box(
                                    modifier = Modifier.matchParentSize().background(
                                        if (activeFilterCount > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.15f),
                                        CircleShape
                                    ).blur(8.dp)
                                )

                                Icon(
                                    painter = painterResource(R.drawable.filter),
                                    contentDescription = "Filter",
                                    tint = if (activeFilterCount > 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(DashboardDimens.iconMd),
                                )
                            }
                        }

                        if (activeFilterCount > 0) {
                            Badge(
                                modifier = Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                            ) {
                                Text(
                                    activeFilterCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.background,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(DashboardDimens.spaceMd))
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(
                start = DashboardDimens.screenPaddingH,
                end = DashboardDimens.screenPaddingH,
                bottom = DashboardDimens.spaceXxl,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── Active filter chips ──────────────────────────────────────────
            if (hasFilters) {
                item {
                    ActiveFilterChipsRow(
                        selectedCategory = filterCategory,
                        selectedPayment = null,
                        selectedMonth = filterMonth,
                        selectedYear = filterYear,
                        dateFrom = filterDateFrom,
                        dateTo = filterDateTo,
                        onClearCategory = { vm.setFilterCategory(null) },
                        onClearPayment = {},
                        onClearMonth = { vm.setFilterMonth(null) },
                        onClearYear = { vm.setFilterYear(null) },
                        onClearDateRange = { vm.setFilterDateFrom(null); vm.setFilterDateTo(null) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ── Period selector (pill tabs + ← label →) ──────────────────────
            item {
                PeriodNavigatorRow(
                    selected = selectedPeriod,
                    periodLabel = state.periodLabel,
                    canGoBack = state.canGoBack,
                    canGoForward = state.canGoForward,
                    onSelect = { vm.setPeriod(it) },
                    onBack = { vm.goBack() },
                    onForward = { vm.goForward() },
                )
            }

            // ── Summary card ─────────────────────────────────────────────────
            item {
                SummaryCard(
                    state = state,
                    progressAnim = progressAnim,
                    utilisation = if (state.totalBudget > 0) (filteredTotalSpent / state.totalBudget).toFloat() else 0f,
                    modifier = Modifier.fillMaxWidth(),
                    displayTotalSpent = filteredTotalSpent,
                    displayCategories = filteredCategories,
                    displayTrendPoints = filteredTrendPoints,
                )
            }

            // ── Category donut + breakdown (single merged card) ───────────────
            if (filteredCategories.isNotEmpty()) {
                item {
                    CategoryCard(
                        categories = filteredCategories,
                        total = filteredTotalSpent,
                        chartAnimProgress = chartAnimProgress,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ── Spending trend ────────────────────────────────────────────────
            if (filteredTrendPoints.isNotEmpty()) {
                item {
                    val period = state.period
                    val offset = state.offset
                    val trendTitle = when (period) {
                        AnalyticsPeriod.WEEK  -> "Daily spending"
                        AnalyticsPeriod.MONTH -> "Weekly spending"
                        AnalyticsPeriod.YEAR  -> "Monthly spending"
                    }

                    val spendPoints = remember(filteredTrendPoints, period, offset) {
                        val now = java.util.Calendar.getInstance()
                        val todayDow = now.get(java.util.Calendar.DAY_OF_WEEK)
                        // DAY_OF_WEEK: Sun=1..Sat=7 → Mon=2→"Mon", etc.
                        val dowLabels = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")
                        val todayDowLabel = dowLabels.getOrNull(todayDow - 1) ?: ""
                        val todayMonthIdx = now.get(java.util.Calendar.MONTH) // 0-based
                        val todayMonthShort = MONTH_LABELS.getOrNull(todayMonthIdx) ?: ""
                        val todayDom = now.get(java.util.Calendar.DAY_OF_MONTH)
                        val isCurrentPeriod = offset == 0

                        filteredTrendPoints.mapIndexed { _, tp ->
                            val isToday = isCurrentPeriod && when (period) {
                                AnalyticsPeriod.WEEK -> tp.label == todayDowLabel
                                AnalyticsPeriod.MONTH -> {
                                    // tooltipDate format: "D1–D2 Month Year", e.g. "1–7 March 2026"
                                    // extract the two day numbers around the dash
                                    val nums = tp.tooltipDate.split(" ").firstOrNull()
                                        ?.split("–")?.mapNotNull { it.trim().toIntOrNull() }
                                        ?: emptyList()
                                    nums.size >= 2 && todayDom in nums[0]..nums[1]
                                }
                                AnalyticsPeriod.YEAR -> tp.label == todayMonthShort
                            }
                            SpendPoint(
                                amount = tp.amount.toFloat(),
                                xLabel = tp.label,
                                tooltipDate = tp.tooltipDate,
                                isToday = isToday,
                            )
                        }
                    }

                    SpendingTrendCard(
                        points = spendPoints,
                        modifier = Modifier.fillMaxWidth(),
                        title = trendTitle,
                        pillLabel = state.periodLabel,
                        showStats = true,
                        chartHeight = DashboardDimens.trendChartHeight,
                    )
                }
            }

            // ── Insights ──────────────────────────────────────────────────────
            if (topMerchantFiltered != null || topCategoryFiltered != null || filteredTrendPoints.isNotEmpty() || state.hasComparison) {
                item {
                    InsightsCard(
                        state = state,
                        filterCategory = filterCategory,
                        filterMonth = filterMonth,
                        filterYear = filterYear,
                        modifier = Modifier.fillMaxWidth(),
                        topCategoryOverride = topCategoryFiltered,
                        topMerchantOverride = topMerchantFiltered,
                        trendPointsOverride = filteredTrendPoints,
                        totalSpentOverride = filteredTotalSpent,
                    )
                }
            }

            item { Spacer(Modifier.height(DashboardDimens.spaceXxl)) }
        }

        // ── Filter bottom sheet ──────────────────────────────────────────────
        if (showFilterSheet) {
            FilterBottomSheet(
                sheetState = sheetState,
                availableCategories = availableCategories,
                availablePayments = emptyList(),
                availableYears = availableYears,
                selectedCategory = filterCategory,
                selectedPayment = null,
                selectedMonth = filterMonth,
                selectedYear = filterYear,
                selectedType = filterType,
                onSelectType = { vm.setFilterType(it) },
                dateFrom = filterDateFrom,
                dateTo = filterDateTo,
                onSelectCategory = { vm.setFilterCategory(it) },
                onSelectPayment = {},
                onSelectMonth = { vm.setFilterMonth(it) },
                onSelectYear = { vm.setFilterYear(it) },
                onDateFromChange = { vm.setFilterDateFrom(it) },
                onDateToChange = { vm.setFilterDateTo(it) },
                onClearAll = { vm.clearFilters() },
                onDismiss = { showFilterSheet = false },
                title = "Filter analytics",
                showPaymentFilter = false,
            )
        }
    }

    // NOTE: QuickStat and StatDivider must be top-level so SummaryCard (a sibling composable)
    // can call them — see below where they are defined at file-level.
}

// Small helpers used by SummaryCard (top-level so they're accessible everywhere in this file)
@Composable
private fun QuickStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.width(1.dp).height(32.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// PERIOD NAVIGATOR
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PeriodNavigatorRow(
    selected: AnalyticsPeriod,
    periodLabel: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onSelect: (AnalyticsPeriod) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // Pill selector: Week · Month · Year
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AnalyticsPeriod.entries.forEach { period ->
                val isSelected = period == selected
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    ).clickable { onSelect(period) }.padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = period.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ← Period label →
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, enabled = canGoBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, "Previous",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (canGoBack) 1f else 0.28f),
                )
            }
            Text(
                text = periodLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            IconButton(onClick = onForward, enabled = canGoForward, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward, "Next",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (canGoForward) 1f else 0.28f),
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SUMMARY CARD
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SummaryCard(
    state: AnalyticsUiState,
    progressAnim: Float,
    utilisation: Float,
    modifier: Modifier = Modifier,
    // Optional overrides to show filtered values without changing the underlying state object
    displayTotalSpent: Double? = null,
    displayCategories: List<CategorySpend>? = null,
    displayTrendPoints: List<TrendPoint>? = null,
) {
    val totalSpentShown = displayTotalSpent ?: state.totalSpent
    val categoriesShown = displayCategories ?: state.categories
    val trendPointsShown = displayTrendPoints ?: state.trendPoints

    val accentColor = when {
        utilisation >= 1f -> MaterialTheme.colorScheme.error
        utilisation >= 0.80f -> Color(0xFFF2A93B)
        else -> MaterialTheme.colorScheme.primary
    }

    // Avg label adapts to period
    val avgLabel = when (state.period) {
        AnalyticsPeriod.WEEK  -> "Avg / day"
        AnalyticsPeriod.MONTH -> "Avg / week"
        AnalyticsPeriod.YEAR  -> "Avg / month"
    }

    GradientCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Column(modifier = Modifier.padding(DashboardDimens.cardPadding)) {

                // Top row: change badge right-aligned
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    // Hero amount (left)
                    Column {
                        Text(
                            text = "Total spent",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = fmt(totalSpentShown),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = (-0.8).sp,
                            lineHeight = 36.sp,
                        )
                    }
                    // Change badge (right)
                    if (state.hasComparison) {
                        val up = state.changePercent >= 0
                        val badgeColor = if (up) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = badgeColor.copy(alpha = 0.12f),
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    text = if (up) "▲" else "▼",
                                    fontSize = 10.sp,
                                    color = badgeColor,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${abs(state.changePercent)}% vs last",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = badgeColor,
                                )
                            }
                        }
                    }
                }

                // Budget progress bar
                if (state.totalBudget > 0) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Budget used",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${fmt(totalSpentShown)} / ${fmt(state.totalBudget)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxHeight().fillMaxWidth(progressAnim)
                                .clip(RoundedCornerShape(4.dp)).background(
                                    Brush.horizontalGradient(
                                        listOf(accentColor.copy(alpha = 0.65f), accentColor)
                                    )
                                ),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${(utilisation * 100).toInt()}% of budget used",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = accentColor,
                    )
                }

                // Quick-stat strip
                if (categoriesShown.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f))
                    Spacer(Modifier.height(14.dp))

                    val remaining = (state.totalBudget - totalSpentShown).coerceAtLeast(0.0)
                    // Use non-empty buckets so future zero-spend slots don't deflate the average
                    val nonEmptyBuckets = trendPointsShown.count { it.amount > 0 }.coerceAtLeast(1)
                    val avgPerBucket = if (totalSpentShown > 0) totalSpentShown / nonEmptyBuckets else 0.0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        QuickStat(
                            value = categoriesShown.firstOrNull()?.name ?: "–",
                            label = "Top category",
                        )
                        StatDivider()
                        QuickStat(
                            value = if (avgPerBucket > 0) fmt(avgPerBucket) else "–",
                            label = avgLabel,
                        )
                        if (state.totalBudget > 0) {
                            StatDivider()
                            QuickStat(
                                value = fmt(remaining),
                                label = "Remaining",
                                valueColor = accentColor,
                            )
                        }
                    }
                }
            }   // end inner padding Column
        }       // end outer wrapper Column
    }           // end GradientCard
}

// ══════════════════════════════════════════════════════════════════════════════
// CATEGORY CARD — donut + inline legend + animated breakdown (merged)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CategoryCard(
    categories: List<CategorySpend>,
    total: Double,
    chartAnimProgress: Float,
    modifier: Modifier = Modifier,
) {
    var pressedIndex by remember { mutableIntStateOf(-1) }

    GradientCard(modifier = modifier) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPadding)) {

            // ── Card header ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Spending by category",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Centred donut (legend lives in the breakdown list below) ─────
            val pressed = pressedIndex.takeIf { it in categories.indices }
            val centerAmt = pressed?.let { categories[it].amount } ?: total
            val centerColor = pressed?.let { categories[it].color } ?: MaterialTheme.colorScheme.primary
            val centerLabel = pressed?.let { categories[it].name } ?: "Total"
            val centerPct = pressed?.let {
                if (total > 0) "${((categories[it].amount / total) * 100).toInt()}%" else ""
            } ?: ""

            Box(
                modifier = Modifier.size(DashboardDimens.donutChartSize).align(Alignment.CenterHorizontally),
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
                // Centre label
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    Text(
                        text = fmt(centerAmt),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = centerColor,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    if (centerPct.isNotEmpty()) {
                        Text(
                            text = centerPct,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = centerColor.copy(alpha = 0.75f),
                        )
                    }
                    Text(
                        text = centerLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // ── Full animated breakdown ───────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.5.dp,
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Breakdown",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = fmt(total),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(10.dp))

            categories.forEachIndexed { i, cat ->
                val pct = if (total > 0) ((cat.amount / total) * 100).toInt() else 0
                val animPct = remember(cat.name) { Animatable(0f) }
                val animDone = remember(cat.name) { mutableStateOf(false) }
                LaunchedEffect(cat.name) {
                    if (!animDone.value) {
                        animDone.value = true
                        animPct.animateTo(
                            pct / 100f,
                            tween(750, delayMillis = i * 75, easing = FastOutSlowInEasing),
                        )
                    }
                }
                val isPressed = i == pressedIndex

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                Modifier.size(10.dp).clip(CircleShape).background(cat.color)
                            )
                            Text(
                                text = cat.name,
                                fontSize = 13.sp,
                                fontWeight = if (isPressed) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = cat.color.copy(alpha = 0.12f),
                            ) {
                                Text(
                                    text = "$pct%",
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = cat.color,
                                )
                            }
                            Text(
                                text = fmt(cat.amount),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                    // Animated bar
                    Box(
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxHeight().fillMaxWidth(animPct.value)
                                .clip(RoundedCornerShape(3.dp)).background(
                                    Brush.horizontalGradient(
                                        listOf(cat.color.copy(0.55f), cat.color)
                                    )
                                ),
                        )
                    }
                }
                if (i < categories.lastIndex) {
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

// ── Interactive donut chart ────────────────────────────────────────────────────

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
    val animSweeps = sweepAngles.map { it * entryProgress }

    val scaleFactors = sweepAngles.indices.map { i ->
        animateFloatAsState(
            targetValue = if (i == pressedIndex) 1.07f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "scale_$i",
        ).value
    }

    Canvas(
        modifier = Modifier.fillMaxSize().pointerInput(categories) {
            detectTapGestures(
                onPress = { tapOffset ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val dx = tapOffset.x - cx
                    val dy = tapOffset.y - cy
                    val dist = sqrt(dx * dx + dy * dy)
                    val radius = minOf(size.width, size.height) / 2f
                    val inner = radius - strokeWidth * 1.5f
                    if (dist in inner..radius) {
                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                        if (angle < 0f) angle += 360f
                        var cum = 0f
                        val hit = sweepAngles.indexOfFirst { s ->
                            val s0 = cum; cum += s; angle in s0..cum
                        }
                        if (hit >= 0) onPress(hit)
                        tryAwaitRelease(); onRelease()
                    }
                },
            )
        },
    ) {
        val sw = strokeWidth
        val diameter = size.minDimension - sw
        var start = -90f
        animSweeps.forEachIndexed { i, sweep ->
            val scale = scaleFactors[i]
            val center = Offset(size.width / 2f, size.height / 2f)
            val tl = Offset(center.x - (diameter / 2f) * scale, center.y - (diameter / 2f) * scale)
            drawArc(
                color = categories[i].color,
                startAngle = start,
                sweepAngle = sweep - 2f,
                useCenter = false,
                topLeft = tl,
                size = Size(diameter * scale, diameter * scale),
                style = Stroke(width = sw * scale, cap = StrokeCap.Butt),
            )
            start += sweep
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// INSIGHTS CARD — 2-column metric grid
// Modified to accept optional overrides for top category/merchant/trend/total
// ══════════════════════════════════════════════════════════════════════════════

private data class InsightTile(
    val emoji: String,
    val label: String,
    val primaryText: String,
    val secondaryText: String?,
    val accentColor: Color,
)

@Composable
private fun InsightsCard(
    state: AnalyticsUiState,
    modifier: Modifier = Modifier,
    filterCategory: String? = null,
    filterMonth: Int? = null,
    filterYear: Int? = null,
    topCategoryOverride: Pair<String, Double>? = null,
    topMerchantOverride: Pair<String, Double>? = null,
    trendPointsOverride: List<TrendPoint>? = null,
    totalSpentOverride: Double? = null,
) {
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    val surf = MaterialTheme.colorScheme.surfaceContainer
    val bg = MaterialTheme.colorScheme.background
    val brush = remember(surf, bg) { cardBrush(surf, bg) }

    val trendPts = trendPointsOverride ?: state.trendPoints
    val totalSpentVal = totalSpentOverride ?: state.totalSpent
    val nonZeroPts = trendPts.filter { it.amount > 0 }
    val avg = if (nonZeroPts.isNotEmpty()) totalSpentVal / nonZeroPts.size else 0.0
    val peakPt = nonZeroPts.maxByOrNull { it.amount }

    val tiles = buildList {
        val tcat = topCategoryOverride ?: state.topCategory
        tcat?.let { (cat, amt) ->
            add(InsightTile("🔥", "Top category", cat, fmt(amt), error))
        }
        val tmerch = topMerchantOverride ?: state.topMerchant
        tmerch?.let { (merchant, amt) ->
            add(InsightTile("🏪", "Top merchant", merchant, fmt(amt), primary))
        }
        if (avg > 0) {
            add(InsightTile("📊", "Avg / period", fmt(avg), null, Color(0xFF8B5CF6)))
        }
        if (peakPt != null) {
            add(InsightTile("📈", "Peak period", peakPt.label, fmt(peakPt.amount), Color(0xFFF59E0B)))
        }
        if (state.hasComparison) {
            val up = state.changePercent >= 0
            val color = if (up) error else primary
            add(
                InsightTile(
                    if (up) "📈" else "📉",
                    "vs last period",
                    "${if (up) "+" else ""}${state.changePercent}%",
                    if (up) "Spent more" else "Spent less",
                    color,
                )
            )
        }
    }

    if (tiles.isEmpty()) return

    GradientCard(modifier = modifier, cardBrush = brush) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPadding)) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Insights",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                val filterLabels = buildList {
                    filterCategory?.let { add(it) }
                    filterMonth?.let { add(MONTH_LABELS[it - 1]) }
                    filterYear?.let { add(it.toString()) }
                }
                val filterContext = filterLabels.joinToString(" · ").ifBlank { null }
                if (filterContext != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = primary.copy(alpha = 0.10f),
                    ) {
                        Text(
                            text = filterContext,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = primary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 2-column grid of tiles
            tiles.chunked(2).forEachIndexed { rowIdx, row ->
                if (rowIdx > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                        thickness = 0.5.dp,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { tile ->
                        InsightTileCell(tile = tile, modifier = Modifier.weight(1f))
                    }
                    // Pad last row if odd count
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun InsightTileCell(tile: InsightTile, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(11.dp))
                .background(tile.accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(tile.emoji, fontSize = 18.sp)
        }
        Column {
            Text(
                text = tile.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = tile.primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = tile.accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            tile.secondaryText?.let { sec ->
                Text(
                    text = sec,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PREVIEW
// ══════════════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true, name = "Analytics – Full")
@Composable
fun AnalyticsScreenFullPreview() {
    val sampleState = AnalyticsUiState(
        period = AnalyticsPeriod.MONTH,
        offset = 0,
        periodLabel = "March 2026",
        canGoBack = true,
        canGoForward = false,
        totalSpent = sampleCategories.sumOf { it.amount },
        totalBudget = 12_000.0,
        changePercent = 12,
        hasComparison = true,
        categories = sampleCategories,
        trendPoints = sampleTrend,
        topMerchant = "SuperMart" to 2_345.0,
        topCategory = sampleCategories.first().let { it.name to it.amount },
        roomLoaded = true,
    )
    val utilisation = (sampleState.totalSpent / sampleState.totalBudget).toFloat()

    MaterialTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                ScreenTopBar(
                    headerTitle = "Analytics",
                    showBack = false,
                    actions = {
                        Box {
                            IconButton(
                                onClick = {},
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(DashboardDimens.cornerCard))
                                    .background(MaterialTheme.colorScheme.surfaceContainer),
                            ) {
                                Icon(
                                    painterResource(R.drawable.filter), "Filter",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(DashboardDimens.iconMd),
                                )
                            }
                        }
                        Spacer(Modifier.width(DashboardDimens.spaceMd))
                    },
                )
            },
        ) { pad ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(
                    horizontal = DashboardDimens.screenPaddingH,
                    vertical = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    PeriodNavigatorRow(
                        selected = sampleState.period,
                        periodLabel = sampleState.periodLabel,
                        canGoBack = true,
                        canGoForward = false,
                        onSelect = {},
                        onBack = {},
                        onForward = {},
                    )
                }
                item {
                    SummaryCard(
                        state = sampleState,
                        progressAnim = utilisation,
                        utilisation = utilisation,
                        modifier = Modifier.fillMaxWidth(),
                        displayTrendPoints = sampleTrend,
                    )
                }
                item {
                    CategoryCard(
                        categories = sampleState.categories,
                        total = sampleState.totalSpent,
                        chartAnimProgress = 1f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    SpendingTrendCard(
                        points = sampleTrend.mapIndexed { i, tp ->
                            SpendPoint(
                                amount = tp.amount.toFloat(),
                                xLabel = tp.label,
                                tooltipDate = tp.tooltipDate,
                                isToday = i == sampleTrend.lastIndex, // W4 = "this week" in the preview
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        title = "Weekly spending",
                        pillLabel = sampleState.periodLabel,
                        showStats = true,
                        chartHeight = DashboardDimens.trendChartHeight,
                    )
                }
                item {
                    InsightsCard(
                        state = sampleState,
                        filterCategory = null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

