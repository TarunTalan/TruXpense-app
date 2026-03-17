package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.BudgetProgressBar
import com.example.truxpense.presentation.screens.dashboard.components.GradientCard
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.utils.currencyFormat
import com.example.truxpense.presentation.utils.formatAbbreviatedAmount
import com.example.truxpense.presentation.utils.progressColor
import com.example.truxpense.presentation.utils.toCurrency
import java.util.*

// ── Design tokens (mirror HTML mockup) ───────────────────────────────────────
private val TealColor = Color(0xFF1BAF9D)
private val AmberColor = Color(0xFFF5A623)
private val RedColor = Color(0xFFE53935)
private val SubColor = Color(0xFF8490A8)

// ── Interval-based card colors ────────────────────────────────────────────────
// Interval 1: progress >= 80%  (danger/red)
private val BarColorDanger = Color(0xFFD64545)
private val BgDangerLight = Color(0xFFFDECEC)
private val BorderDangerLight = Color(0xFF9C2D2D)
private val BgDangerDark = Color(0xFF9C2D2D)
private val BorderDangerDark = Color(0xFFE07A7A)

// Interval 2: progress >= 45% and < 80%  (warning/amber)
private val BarColorWarning = Color(0xFFF2C06A)
private val BgWarningLight = Color(0xFFFFF4E5)
private val BorderWarningLight = Color(0xFFB3741A)
private val BgWarningDark = Color(0xFFB3741A)
private val BorderWarningDark = Color(0xFFF4A62A)

// Interval 3: progress < 45%  (safe/teal)
private val BarColorSafe = Color(0xFF1B6F73)
private val BgSafeLight = Color(0xFFE6F4F2)
private val BorderSafeLight = Color(0xFF1B6F73)
private val BgSafeDark = Color(0xFF2FA4A9)
private val BorderSafeDark = Color(0xFFE6F4F2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetTab(
    vm: BudgetViewModel = hiltViewModel(),
    onNavigateToAddBudget: () -> Unit = {},
    onNavigateToBudgetDetail: (BudgetCategory) -> Unit = {},
    previewBudgets: List<BudgetCategory>? = null,
    currencyCode: String = "INR",
) {
    val displayItems by vm.categoryDisplayItems.collectAsState()
    val totalBudget by vm.totalBudget.collectAsState()
    val totalSpent by vm.totalSpent.collectAsState()
    val currentMonth by vm.currentMonth.collectAsState()
    val canGoBack by vm.canGoBack.collectAsState()
    val canGoForward by vm.canGoForward.collectAsState()
    val isLoaded by vm.isLoaded.collectAsState()

    val budgetsToShow = previewBudgets?.mapIndexed { _, cat ->
        BudgetCategoryDisplay(
            category = cat,
            amountText = "${cat.spent} / ${cat.total}",
            progress = if (cat.total > 0) cat.spent.toFloat() / cat.total else 0f,
        )
    } ?: displayItems

    val fmt = remember(currencyCode) { currencyFormat(currencyCode) }

    AnimatedVisibility(
        visible = isLoaded,
        enter = fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)),
    ) {
        val noBudget = budgetsToShow.isEmpty() || totalBudget <= 0
        val isPastMonth = canGoForward  // canGoForward == true means offset < 0, i.e. viewing history

        if (noBudget && !isPastMonth) {
            // Current month with no budgets → full empty/onboarding screen
            BudgetsEmptyScreen(onAddBudget = onNavigateToAddBudget)
        } else {
            // Past months: always show the tab content so the navigator stays accessible,
            // and BudgetTabContent handles the no-budget state with zeros + placeholder text.
            BudgetTabContent(
                budgetsToShow = budgetsToShow,
                totalBudget = totalBudget,
                totalSpent = totalSpent,
                currentMonth = currentMonth,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                onPrevious = { vm.previousMonth() },
                onNext = { vm.nextMonth() },
                onNavigateToAddBudget = onNavigateToAddBudget,
                onNavigateToBudgetDetail = onNavigateToBudgetDetail,
                fmt = fmt,
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CONTENT
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetTabContent(
    budgetsToShow: List<BudgetCategoryDisplay>,
    totalBudget: Int,
    totalSpent: Int,
    currentMonth: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onNavigateToAddBudget: () -> Unit,
    onNavigateToBudgetDetail: (BudgetCategory) -> Unit,
    fmt: java.text.NumberFormat = currencyFormat("INR"),
) {
    var progTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { progTriggered = true }
    val progMul by animateFloatAsState(
        targetValue = if (progTriggered) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "budget_progress",
    )

    // Derived values for overview card.
    // NOTE: for past months the ViewModel's `categories` flow already filters out
    // categories with zero spend, so totalBudget / totalSpent naturally come back as
    // 0 for months that had no activity — no manual clamping needed here.
    val isCurrentMonth = !canGoForward
    val noBudgetPastMonth = !isCurrentMonth && totalBudget <= 0

    val budgetLeft = (totalBudget - totalSpent).coerceAtLeast(0)
    val usedPct = if (totalBudget > 0) (totalSpent.toFloat() / totalBudget).coerceIn(0f, 1f) else 0f
    val cal = remember { Calendar.getInstance() }
    val daysInMonth = remember { cal.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val daysLeft = remember { (daysInMonth - cal.get(Calendar.DAY_OF_MONTH) + 1).coerceAtLeast(1) }
    // Daily avg = actual average spend per day so far this month.
    // For past months the full month has elapsed, so use daysInMonth as the divisor.
    val daysElapsed = remember { cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1) }
    val effectiveDaysElapsed = if (isCurrentMonth) daysElapsed else daysInMonth
    val dailyAvg = totalSpent.toDouble() / effectiveDaysElapsed

    // Alert: first category that is >80% used
    val alertCategory = budgetsToShow.firstOrNull { it.progress > 0.80f }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                headerTitle = "Budgets", showBack = false, actions = {
                    Box {
                        IconButton(onClick = onNavigateToAddBudget) {
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
                                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.15f), CircleShape
                                    ).blur(8.dp)
                                )

                                Icon(
                                    painter = painterResource(R.drawable.add),
                                    contentDescription = "Add budget",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                })
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .padding(horizontal = DashboardDimens.screenPaddingH),
            contentPadding = PaddingValues(bottom = DashboardDimens.spaceXxxl),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── Month navigator ─────────────────────────────────────────────
            item { MonthNavigatorRow(currentMonth, canGoBack, canGoForward, onPrevious, onNext) }

            // ── Overview card (GradientCard / glass) ─────────────────────────
            // Not rendered when no budget was set for a past month.
            if (!noBudgetPastMonth) {
                item {
                    BudgetOverviewCard(
                        totalBudget = totalBudget.toDouble(),
                        totalSpent = totalSpent.toDouble(),
                        budgetLeft = budgetLeft.toDouble(),
                        usedPct = usedPct,
                        daysLeft = daysLeft,
                        dailyAvg = dailyAvg,
                        alertCategory = alertCategory,
                        fmt = fmt,
                        isCurrentMonth = isCurrentMonth,
                    )
                }
            }

            // ── Section label ────────────────────────────────────────────────
            if (!noBudgetPastMonth) {
                item {
                    Text(
                        text = "${budgetsToShow.size} budgets. ${currentMonth.take(3)} ${currentMonth.takeLast(4)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }

            // ── Per-category cards or no-budget placeholder ──────────────────
            if (noBudgetPastMonth) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "No budget set",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "No budgets were tracked for ${currentMonth}.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                items(budgetsToShow, key = { it.category.id }) { display ->
                    BudgetCategoryCard(
                        display = display,
                        progMul = progMul,
                        fmt = fmt,
                        onClick = { onNavigateToBudgetDetail(display.category) },
                    )
                }
            }
        }
    }
}

// ── Month navigator ────────────────────────────────────────────────────────────

@Composable
private fun MonthNavigatorRow(
    currentMonth: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious, enabled = canGoBack) {
                Icon(
                    painter = painterResource(R.drawable.left_arrow),
                    contentDescription = "Previous month",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (canGoBack) 1f else 0.3f),
                    modifier = Modifier.size(DashboardDimens.iconMd),
                )
            }
            Text(
                text = currentMonth,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            IconButton(onClick = onNext, enabled = canGoForward) {
                Icon(
                    painter = painterResource(R.drawable.right_arrow),
                    contentDescription = "Next month",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (canGoForward) 1f else 0.3f),
                    modifier = Modifier.size(DashboardDimens.iconMd),
                )
            }
        }
    }
}

// ── Budget Overview Card (GradientCard / glass) ────────────────────────────────

@Composable
private fun BudgetOverviewCard(
    totalBudget: Double,
    totalSpent: Double,
    budgetLeft: Double,
    usedPct: Float,
    daysLeft: Int,
    dailyAvg: Double,
    alertCategory: BudgetCategoryDisplay?,
    fmt: java.text.NumberFormat,
    isCurrentMonth: Boolean = true,
) {
    var arcTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { arcTriggered = true }
    val arcSweep by animateFloatAsState(
        targetValue = if (arcTriggered) usedPct * 360f else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "donut_arc",
    )

    val arcColor = progressColor(usedPct)

    GradientCard(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {

            // ── Header: label + amount stacked ───────────────────────────────
            Text(
                text = "Total Monthly Budget",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.4.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = totalBudget.toCurrency(fmt),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.8).sp,
            )

            Spacer(Modifier.height(16.dp))

            // ── Donut + stats row ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Donut
                Box(
                    modifier = Modifier.size(88.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.size(88.dp)) {
                        val stroke = 8.dp.toPx()
                        val inset = stroke / 2f
                        val arcSz = Size(size.width - stroke, size.height - stroke)
                        val arcOff = Offset(inset, inset)
                        drawArc(
                            color = arcColor.copy(alpha = 0.15f),
                            startAngle = 0f, sweepAngle = 360f,
                            useCenter = false,
                            topLeft = arcOff, size = arcSz,
                            style = Stroke(width = stroke),
                        )
                        drawArc(
                            color = arcColor,
                            startAngle = -90f, sweepAngle = arcSweep,
                            useCenter = false,
                            topLeft = arcOff, size = arcSz,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(usedPct * 100).toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = arcColor,
                        )
                        Text(
                            text = "used",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Stats 2×2 grid with dividers
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    // Row 1: Spent | Remaining
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        OverviewStat(
                            value = totalSpent.toCurrency(fmt),
                            label = "Spent",
                            modifier = Modifier.weight(1f),
                        )
                        // vertical divider
                        Box(
                            modifier = Modifier.width(1.dp).height(36.dp).align(Alignment.CenterVertically)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                        )
                        // Remaining: abbreviated so it never overflows maxLines=1
                        val exceededTotal = (totalSpent - totalBudget).coerceAtLeast(0.0)
                        val remainingValueText = when {
                            exceededTotal > 0.0 -> "−₹${formatAbbreviatedAmount(exceededTotal)}"
                            budgetLeft == 0.0 -> "100% used"
                            else -> "₹${formatAbbreviatedAmount(budgetLeft.toDouble())}"
                        }
                        val remainingColor = when {
                            exceededTotal > 0.0 -> MaterialTheme.colorScheme.error
                            budgetLeft == 0.0 -> AmberColor
                            else -> Color.Unspecified
                        }
                        OverviewStat(
                            value = remainingValueText,
                            label = "Remaining",
                            valueColor = remainingColor,
                            modifier = Modifier.weight(1f),
                            align = Alignment.End,
                        )
                    }

                    // horizontal divider
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    )

                    // Row 2: Days left (current month only) | Daily avg
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        if (isCurrentMonth) {
                            OverviewStat(
                                value = "$daysLeft days",
                                label = "Days left",
                                valueColor = AmberColor,
                                modifier = Modifier.weight(1f),
                            )
                            // vertical divider
                            Box(
                                modifier = Modifier.width(1.dp).height(36.dp).align(Alignment.CenterVertically)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                            )
                            OverviewStat(
                                value = dailyAvg.toCurrency(fmt),
                                label = "Daily avg",
                                valueColor = arcColor,
                                modifier = Modifier.weight(1f),
                                align = Alignment.End,
                            )
                        } else {
                            // Past month: days left is meaningless — show daily avg full-width
                            OverviewStat(
                                value = dailyAvg.toCurrency(fmt),
                                label = "Daily avg",
                                valueColor = arcColor,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            // ── Alert banner ──────────────────────────────────────────────────
            if (alertCategory != null) {
                val overshoot = (alertCategory.category.spent - alertCategory.category.total).coerceAtLeast(0)
                val colorVar = if (isSystemInDarkTheme()) Color(0xFFE07A7A)
                else MaterialTheme.colorScheme.onErrorContainer
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Transparent)
                        .border(1.dp, color = colorVar, shape = RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        painterResource(R.drawable.alert_),
                        contentDescription = null,
                        tint = colorVar,
                        modifier = Modifier.size(16.dp).padding(top = 1.dp),
                    )
                    Text(
                        text = if (overshoot > 0) "Heads up! At this pace you'll exceed ${alertCategory.category.name} budget by ${
                            overshoot.toDouble().toCurrency(fmt)
                        } by month-end."
                        else "You're close to exceeding your ${alertCategory.category.name} budget.",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorVar,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
    align: Alignment.Horizontal = Alignment.Start,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = align,
    ) {
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onBackground else valueColor,
            letterSpacing = (-0.3).sp,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = SubColor,
        )
    }
}

// ── Budget Category Card ───────────────────────────────────────────────────────

// Tiny data class for card theming
private data class CardColors(val border: Color, val bg: Color, val footer: Color)

@Composable
private fun BudgetCategoryCard(
    display: BudgetCategoryDisplay,
    progMul: Float,
    fmt: java.text.NumberFormat,
    onClick: () -> Unit,
) {
    val p = display.progress
    val isDark = isSystemInDarkTheme()

    val colors = when {
        p >= 0.80f -> if (isDark) CardColors(BorderDangerDark, BgDangerDark, BarColorDanger)
        else CardColors(BorderDangerLight, BgDangerLight, BarColorDanger)

        p >= 0.45f -> if (isDark) CardColors(BorderWarningDark, BgWarningDark, BarColorWarning)
        else CardColors(BorderWarningLight, BgWarningLight, BarColorWarning)

        else -> if (isDark) CardColors(BorderSafeDark, BgSafeDark, BarColorSafe)
        else CardColors(BorderSafeLight, BgSafeLight, BarColorSafe)
    }
    val (borderColor, _, footerColor) = colors

    val remaining = (display.category.total - display.category.spent).coerceAtLeast(0)
    val pctUsed = (p * 100).toInt()

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer).clickable(onClick = onClick),
    ) {
        // Colored left accent
        Box(
            modifier = Modifier.align(Alignment.CenterStart).width(4.dp).fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)).background(borderColor),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
        ) {
            // Name + amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = display.category.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = display.amountText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(Modifier.height(8.dp))

            BudgetProgressBar(
                progress = p,
                progressMultiplier = progMul,
                height = 7.dp,
                showLabel = false,
            )

            Spacer(Modifier.height(7.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val overshoot = (display.category.spent - display.category.total).coerceAtLeast(0)
                val footerText = when {
                    overshoot > 0 -> "Over by ₹${formatAbbreviatedAmount(overshoot.toDouble())} · $pctUsed% used"

                    remaining == 0 -> "100% used"

                    else -> "₹${formatAbbreviatedAmount(remaining.toDouble())} left · $pctUsed% used"
                }
                Text(
                    text = footerText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = footerColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                )
                Icon(
                    painterResource(R.drawable.right_arrow),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}


// ── Preview ────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BudgetScreenPreview() {
    val sample = listOf(
        BudgetCategory(1, "Food", spent = 4_200, total = 5_000, barColor = RedColor),
        BudgetCategory(2, "Transport", spent = 100, total = 500, barColor = TealColor),
        BudgetCategory(3, "Shopping", spent = 1_000, total = 2_000, barColor = AmberColor),
        BudgetCategory(4, "Bills", spent = 900, total = 1_000, barColor = RedColor),
    )
    val displays = sample.map {
        val fmt = currencyFormat("INR")
        BudgetCategoryDisplay(
            category = it,
            amountText = "${it.spent.toDouble().toCurrency(fmt)} / ${it.total.toDouble().toCurrency(fmt)}",
            progress = if (it.total > 0) it.spent.toFloat() / it.total else 0f,
        )
    }
    val fmt = currencyFormat("INR")
    MaterialTheme {
        BudgetTabContent(
            budgetsToShow = displays,
            totalBudget = sample.sumOf { it.total },
            totalSpent = sample.sumOf { it.spent },
            currentMonth = "February 2026",
            canGoBack = true,
            canGoForward = false,
            onPrevious = {},
            onNext = {},
            onNavigateToAddBudget = {},
            onNavigateToBudgetDetail = {},
            fmt = fmt,
        )
    }
}