package com.example.truxpense.presentation.screens.dashboard.savings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.truxpense.R
import com.example.truxpense.data.repository.savings.ContributeFrequency
import com.example.truxpense.data.repository.savings.SavingsContributionUi
import com.example.truxpense.data.repository.savings.SavingsGoalUi
import com.example.truxpense.presentation.screens.dashboard.components.AppConfirmDialog
import com.example.truxpense.presentation.screens.dashboard.components.GradientCard
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.theme.AppDialogTheme
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.theme.TruXpenseTheme
import com.example.truxpense.presentation.utils.SimpleTextField
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════════════════════
// Thin ViewModel wrapper
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun GoalDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onGoalCompleted: () -> Unit,
    vm: GoalDetailViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val monthlyData by vm.monthlyData.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.completedEvent.collect { onGoalCompleted() } }

    val goal = state.goal ?: return
    GoalDetailScreenContent(
        goal = goal,
        contributions = state.contributions,
        monthlyData = monthlyData,
        onBack = onBack,
        onEdit = { onEdit(goal.id) },
        onDelete = { vm.deleteGoal { onBack() } },
        onAddContribution = { amt -> vm.addContribution(amt) },
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// Stateless content
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreenContent(
    goal: SavingsGoalUi,
    contributions: List<SavingsContributionUi>,
    monthlyData: List<Pair<String, Double>>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddContribution: (Double) -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var menuOpen by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var addAmount by remember { mutableStateOf("") }

    // ── Delete confirmation dialog ────────────────────────────────────────────
    if (showDeleteDialog) {
        AppConfirmDialog(
            title = "Delete goal?",
            message = "\"${goal.name}\" and all contribution history will be permanently deleted.",
            confirmLabel = "Delete",
            onConfirm = { showDeleteDialog = false; onDelete() },
            onDismiss = { showDeleteDialog = false },
        )
    }

    // ── Add savings bottom sheet ──────────────────────────────────────────────
    if (showAddSheet) {
        AppDialogTheme {
            ModalBottomSheet(
                onDismissRequest = { showAddSheet = false; addAmount = "" },
                sheetState = sheetState,
                dragHandle = {
                    Box(
                        Modifier.padding(vertical = 10.dp)
                            .size(width = DashboardDimens.sheetHandleWidth, height = DashboardDimens.sheetHandleHeight)
                            .clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant),
                    )
                },
            ) {
                Column(
                    Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Add to ${goal.name}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    )

                    // ── Quick-amount chips ────────────────────────────────────────
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        listOf("500", "1000", "2000", "5000").forEach { v ->
                            val sel = addAmount == v
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { addAmount = v }.padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "₹$v",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sel) Color.White else MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }
                    }

                    // ── Custom amount using SimpleTextField ───────────────────────
                    SimpleTextField(
                        value = addAmount,
                        onValueChange = { addAmount = it.filter(Char::isDigit) },
                        placeholder = "Custom amount",
                        prefix = {
                            Text(
                                "₹",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )

                    // ── Action buttons ────────────────────────────────────────────
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch { sheetState.hide() }
                                showAddSheet = false
                                addAmount = ""
                            },
                            modifier = Modifier.weight(1f).height(DashboardDimens.buttonHeight),
                            shape = RoundedCornerShape(DashboardDimens.cornerCard),
                        ) { Text("Cancel") }

                        Button(
                            onClick = {
                                addAmount.toDoubleOrNull()?.let { onAddContribution(it) }
                                scope.launch { sheetState.hide() }
                                showAddSheet = false
                                addAmount = ""
                            },
                            enabled = (addAmount.toDoubleOrNull() ?: 0.0) > 0,
                            modifier = Modifier.weight(2f).height(DashboardDimens.buttonHeight),
                            shape = RoundedCornerShape(DashboardDimens.cornerCard),
                            colors = ButtonDefaults.buttonColors(containerColor = primary),
                        ) {
                            Text(
                                "Add ₹${addAmount.ifEmpty { "0" }}",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ── TopBar ────────────────────────────────────────────────────────────
        ScreenTopBar(
            headerTitle = "Active Goals",
            showBack = true,
            onBack = onBack,
            actions = {
                Box {
                    IconButton(onClick = { menuOpen = !menuOpen }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit goal") },
                            leadingIcon = {
                                Icon(
                                    painterResource(R.drawable.edit),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = { menuOpen = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Goal", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    painterResource(R.drawable.delete),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = { menuOpen = false; showDeleteDialog = true },
                        )
                    }
                }
            },
        )

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = DashboardDimens.screenPaddingH),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Hero icon + name ──────────────────────────────────────────────
            val iconBg = Color.Transparent
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(160.dp)
                        .rotate(-8f),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = goalIconToDrawable(goal.icon)),
                        contentDescription = goal.name,
                        tint = Color.Unspecified,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    goal.name,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                )
                Text(
                    "Target ${goal.targetDateDisplay()} · ${goal.daysLeft()} days left",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // ── Donut + stats card ────────────────────────────────────────────
            val donutColor = Color(0xFF1B6F73)
            GradientCard(modifier = Modifier.fillMaxWidth(), elevation = DashboardDimens.cardElevation) {
                Column(Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier.size(DashboardDimens.donutChartSize * 0.75f)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Animate from 0 → actual progress once on first composition
                        var triggered by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { triggered = true }
                        val animPct by animateFloatAsState(
                            targetValue = if (triggered) goal.progressFraction else 0f,
                            animationSpec = tween(900, easing = FastOutSlowInEasing),
                            label = "donut_anim",
                        )

                        val density = LocalDensity.current
                        // Use a slightly thinner stroke for the goal detail donut than the global token
                        val strokePx = with(density) { (DashboardDimens.donutStrokeWidth * 0.6f).toPx() }
                        Canvas(Modifier.fillMaxSize()) {
                            val sw = strokePx
                            val diameter = size.minDimension - sw
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val topLeft = Offset(center.x - diameter / 2f, center.y - diameter / 2f)
                            val arcSize = Size(diameter, diameter)

                            // background track — teal at 0.2 alpha
                            drawArc(
                                color = donutColor.copy(alpha = 0.2f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = sw, cap = StrokeCap.Butt),
                            )

                            // foreground sweep — teal at full alpha
                            if (animPct > 0f) {
                                val sweepAngle = (360f * animPct - 2f).coerceAtLeast(0f)
                                drawArc(
                                    color = donutColor,
                                    startAngle = -90f,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = sw, cap = StrokeCap.Butt),
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${goal.progressPercent}%",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = donutColor,
                            )
                            Text(
                                "Saved",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf(
                            Triple(
                                "₹${"%,.0f".format(goal.remaining)}",
                                "Remaining",
                                MaterialTheme.colorScheme.onBackground
                            ),
                            Triple("₹${"%,.0f".format(goal.savedAmount)}", "Saved", primary),
                            Triple("₹${"%,.0f".format(goal.autoContributeAmount)}", "Daily avg", primary),
                        ).forEachIndexed { i, (v, l, c) ->
                            if (i > 0) VerticalDivider(
                                Modifier.height(40.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    v,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = c,
                                )
                                Text(
                                    l,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // ── Monthly bar chart card (use same style as BudgetDetailScreen WeeklyBreakdownCard) ─────
            GradientCard(modifier = Modifier.fillMaxWidth(), elevation = DashboardDimens.cardElevation) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Monthly progress",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Chip("6 months", color = MaterialTheme.colorScheme.secondary)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Prepare points and animation
                    val displayPoints = monthlyData.ifEmpty {
                        listOf("M1" to 0.0, "M2" to 0.0, "M3" to 0.0, "M4" to 0.0)
                    }

                    val maxVal = displayPoints.maxOf { it.second }.coerceAtLeast(1.0)

                    // Colour by rank like BudgetDetail: highest = red, 2nd = amber, rest = teal, zero = empty
                    val sortedAmounts = displayPoints.map { it.second }.sortedDescending()
                    fun barColor(amount: Double): Color = when {
                        amount <= 0 -> Color(0xFFE0E4EA)
                        amount == sortedAmounts.getOrNull(0) -> Color(0xFFE53935)
                        sortedAmounts.size > 1 && amount == sortedAmounts.getOrNull(1) -> Color(0xFFF5A623)
                        else -> Color(0xFF1BAF9D)
                    }

                    // animate chart on composition
                    var chartAnimTriggered by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { chartAnimTriggered = true }
                    val animProgress by animateFloatAsState(
                        targetValue = if (chartAnimTriggered) 1f else 0f,
                        animationSpec = tween(700, easing = FastOutSlowInEasing),
                        label = "monthly_chart_anim",
                    )

                    VerticalBarChart(
                        points = displayPoints,
                        maxVal = maxVal,
                        barColorFn = { barColor(it) },
                        animProgress = animProgress,
                        modifier = Modifier.fillMaxWidth().height(88.dp),
                    )
                }
            }

            // ── Recent contributions ──────────────────────────────────────────
            SectionLabel("Recent contributions")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DashboardDimens.cornerCard),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(DashboardDimens.borderStroke, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                if (contributions.isEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().padding(24.dp),
                        Alignment.Center,
                    ) {
                        Text(
                            "No contributions yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else {
                    contributions.forEachIndexed { i, c ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    c.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Text(
                                    c.timeDisplay(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            Text(
                                "₹${"%,.0f".format(c.amount)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        if (i < contributions.size - 1) HorizontalDivider(
                            thickness = DashboardDimens.dividerThin,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }

            // ── Add savings button ────────────────────────────────────────────
            Button(
                onClick = { showAddSheet = true },
                modifier = Modifier.fillMaxWidth().height(DashboardDimens.buttonHeight),
                shape = RoundedCornerShape(DashboardDimens.cornerCard),
                colors = ButtonDefaults.buttonColors(containerColor = primary),
            ) {
                Text(
                    "Add savings",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Local VerticalBarChart (adapted from BudgetDetailScreen) ─────────────────
@Composable
private fun VerticalBarChart(
    points: List<Pair<String, Double>>,
    maxVal: Double,
    barColorFn: (Double) -> Color,
    animProgress: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textSzPx = with(density) { 10.sp.toPx() }
    val amtSzPx = with(density) { 11.sp.toPx() }
    val topTextColorInt = MaterialTheme.colorScheme.onBackground.copy(0.8f).toArgb()

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val labelH = with(density) { 24.dp.toPx() }
        val amtLabelH = with(density) { 20.dp.toPx() }
        val chartH = h - labelH - amtLabelH
        val barCount = points.size
        val totalGap = with(density) { 8.dp.toPx() } * (barCount + 1)
        val barW = ((w - totalGap) / barCount).coerceAtLeast(8f)
        val cornerR = barW * 0.18f

        points.forEachIndexed { i, pt ->
            val x = with(density) { 8.dp.toPx() } + i * (barW + with(density) { 8.dp.toPx() })
            val frac = if (pt.second > 0) (pt.second / maxVal * animProgress).toFloat() else 0f
            val barH = (chartH * frac).coerceAtLeast(0f)
            val barTop = amtLabelH + (chartH - barH)
            val color = barColorFn(pt.second)

            if (pt.second > 0) {
                // Bar
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, barTop),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(cornerR, cornerR),
                )
                // Amount label above bar
                if (animProgress > 0.5f) {
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            textSize = amtSzPx
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                            this.color = topTextColorInt
                        }
                        val lbl = "₹${"%,.0f".format(pt.second)}"
                        canvas.nativeCanvas.drawText(lbl, x + barW / 2, barTop - with(density) { 5.dp.toPx() }, paint)
                    }
                }
            } else {
                // Empty bar — just a horizontal dash
                val dashY = amtLabelH + chartH - with(density) { 4.dp.toPx() }
                drawLine(
                    color = Color(0xFFE0E4EA),
                    start = Offset(x + barW * 0.25f, dashY),
                    end = Offset(x + barW * 0.75f, dashY),
                    strokeWidth = with(density) { 2.dp.toPx() },
                    cap = StrokeCap.Round,
                )
            }

            // Month label below
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    textSize = textSzPx
                    textAlign = android.graphics.Paint.Align.CENTER
                    this.color = topTextColorInt
                }
                canvas.nativeCanvas.drawText(pt.first, x + barW / 2, h - with(density) { 4.dp.toPx() }, paint)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Preview helpers
// ══════════════════════════════════════════════════════════════════════════════

private fun previewGoal(saved: Double = 18450.0) = SavingsGoalUi(
    id = 1, name = "iPhone 15", icon = "📱", colorHex = "#9B59F5",
    targetAmount = 80000.0, savedAmount = saved,
    targetDateEpoch = epochDayMonthsFromNow(1),
    autoContribute = true, autoContributeAmount = 500.0,
    autoContributeFrequency = ContributeFrequency.DAILY, isCompleted = false,
)

// Helper: return epoch-day (days since 1970-01-01) for N months from now (avoids java.time.LocalDate API)
private fun epochDayMonthsFromNow(months: Int): Long {
    val cal = java.util.Calendar.getInstance()
    cal.add(java.util.Calendar.MONTH, months)
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis / 86_400_000L
}

private fun previewContributions() = listOf(
    SavingsContributionUi(1, 1, 500.0, "Auto-save", System.currentTimeMillis()),
    SavingsContributionUi(2, 1, 2400.0, "Manual add", System.currentTimeMillis() - 86_400_000),
    SavingsContributionUi(3, 1, 2400.0, "Last month", System.currentTimeMillis() - 86_400_000 * 25),
)

private val previewMonthly = listOf("Oct", "Nov", "Dec", "Jan", "Feb", "Mar", "Apr").mapIndexed { i, m ->
    m to listOf(3200.0, 4100.0, 5500.0, 3800.0, 2600.0, 4300.0, 5000.0)[i]
}

@Preview(
    name = "Goal Detail – in progress",
    showBackground = true,
    device = "spec:width=390dp,height=844dp,dpi=430",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewGoalDetailInProgress() {
    TruXpenseTheme {
        GoalDetailScreenContent(
            goal = previewGoal(18450.0),
            contributions = previewContributions(),
            monthlyData = previewMonthly,
            onBack = {}, onEdit = {}, onDelete = {}, onAddContribution = {},
        )
    }
}

@Preview(
    name = "Goal Detail – nearly complete",
    showBackground = true,
    device = "spec:width=390dp,height=844dp,dpi=430",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewGoalDetailNearComplete() {
    TruXpenseTheme {
        GoalDetailScreenContent(
            goal = previewGoal(74000.0),
            contributions = previewContributions(),
            monthlyData = previewMonthly,
            onBack = {}, onEdit = {}, onDelete = {}, onAddContribution = {},
        )
    }
}

@Preview(
    name = "Goal Detail – no contributions",
    showBackground = true,
    device = "spec:width=390dp,height=844dp,dpi=430",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewGoalDetailEmpty() {
    TruXpenseTheme {
        GoalDetailScreenContent(
            goal = previewGoal(0.0),
            contributions = emptyList(),
            monthlyData = previewMonthly.map { it.first to 0.0 },
            onBack = {}, onEdit = {}, onDelete = {}, onAddContribution = {},
        )
    }
}

