package com.example.truxpense.presentation.screens.dashboard.savings

import android.graphics.Color.rgb
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.truxpense.data.repository.savings.ContributeFrequency
import com.example.truxpense.data.repository.savings.SavingsGoalUi
import com.example.truxpense.presentation.screens.dashboard.components.GradientCard
import com.example.truxpense.presentation.screens.dashboard.components.GradientCardShape
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.theme.TruXpenseTheme
import com.example.truxpense.presentation.utils.SimpleTextField
import java.util.*

// ══════════════════════════════════════════════════════════════════════════════
// Progress-fill colors per % interval (no gradient)
//   0–25 %  → red-ish   (danger)
//  26–50 %  → amber     (caution)
//  51–75 %  → blue      (progress)
//  76–100 % → green     (healthy)
// ══════════════════════════════════════════════════════════════════════════════

private val ProgressColorLow    = Color(rgb(47, 164, 169)).copy(0.3f)   // 0–25 %
private val ProgressColorMid    = Color(rgb(47, 164, 169)).copy(0.5f)    // 26–50 %
private val ProgressColorGood   = Color(rgb(47, 164, 169)).copy(0.7f)    // 51–75 %  (teal)
private val ProgressColorFull   = Color(rgb(47, 164, 169)).copy(0.9f)   // 76–100 %

private fun progressFillColor(fraction: Float): Color = when {
    fraction <= 0.25f -> ProgressColorLow
    fraction <= 0.50f -> ProgressColorMid
    fraction <= 0.75f -> ProgressColorGood
    else              -> ProgressColorFull
}

// ══════════════════════════════════════════════════════════════════════════════
// ViewModel wrapper
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun DistributiveScreen(
    onBack: () -> Unit,
    onConfirmed: () -> Unit,
    vm: DistributeViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val totalDistributed by vm.totalDistributed.collectAsStateWithLifecycle()

    DistributiveScreenContent(
        totalAvailable = state.totalAvailable,
        goals = state.goals,
        allocations = state.allocations,
        totalDistributed = totalDistributed,
        onBack = onBack,
        onAdjust = { id, delta -> vm.adjust(id, delta) },
        onSetAllocation = { id, amt -> vm.setAllocation(id, amt) },
        onConfirm = { vm.confirmDistribution(); onConfirmed() },
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// Stateless content
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun DistributiveScreenContent(
    totalAvailable: Double,
    goals: List<SavingsGoalUi>,
    allocations: Map<Long, Double>,
    totalDistributed: Double,
    onBack: () -> Unit,
    onAdjust: (Long, Double) -> Unit,
    onSetAllocation: (Long, Double) -> Unit,
    onConfirm: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    val overAllocated = totalDistributed > totalAvailable
    val remaining = totalAvailable - totalDistributed
    val distributePct = if (totalAvailable > 0) (totalDistributed / totalAvailable).toFloat().coerceIn(0f, 1f)
    else 0f

    val animPct by animateFloatAsState(
        targetValue = distributePct,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "dist_pct",
    )

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        ScreenTopBar(headerTitle = "Distribute savings", showBack = true, onBack = onBack)

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = DashboardDimens.screenPaddingH),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Summary card ─────────────────────────────────────────────────
            DistributeSummarySection(
                totalAvailable = totalAvailable,
                totalDistributed = totalDistributed,
                remaining = remaining,
                overAllocated = overAllocated,
                animPct = animPct,
                error = error,
            )

            // ── Goals header ──────────────────────────────────────────────────
            Text(
                text = "Goals",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp),
            )

            // ── Per-goal cards ────────────────────────────────────────────────
            goals.forEach { goal ->
                val alloc = allocations[goal.id] ?: goal.savedAmount
                val added = (alloc - goal.savedAmount).coerceAtLeast(0.0)
                val step = goal.targetAmount * 0.05
                GoalAllocationCard(
                    goal = goal,
                    alloc = alloc,
                    added = added,
                    primary = primary,
                    error = error,
                    onMinus = { onAdjust(goal.id, -step) },
                    onPlus = { onAdjust(goal.id, +step) },
                    onSliderChange = { onSetAllocation(goal.id, it) },
                )
            }

            Spacer(Modifier.height(4.dp))

            // ── Confirm button ────────────────────────────────────────────────
            Button(
                onClick = onConfirm,
                enabled = totalDistributed > 0 && !overAllocated,
                modifier = Modifier.fillMaxWidth().height(DashboardDimens.buttonHeight),
                shape = RoundedCornerShape(DashboardDimens.cornerCard),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primary,
                    disabledContainerColor = primary.copy(alpha = 0.40f),
                ),
            ) {
                Text(
                    text = "Confirm distribution",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Summary card: all stats + progress in one gradient card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DistributeSummarySection(
    totalAvailable: Double,
    totalDistributed: Double,
    remaining: Double,
    overAllocated: Boolean,
    animPct: Float,
    error: Color,
) {
    val primary = MaterialTheme.colorScheme.primary
    val fillColor = progressFillColor(animPct)

    GradientCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(DashboardDimens.borderStroke, primary.copy(alpha = 0.18f)),
                GradientCardShape,
            ),
        elevation = DashboardDimens.cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

            // ── Three stat columns ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SummaryStatColumn(
                    label = "Available",
                    value = "₹${"%.0f".format(totalAvailable)}",
                    valueColor = MaterialTheme.colorScheme.onBackground,
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(primary.copy(alpha = 0.18f)),
                )
                SummaryStatColumn(
                    label = "Distributing",
                    value = "₹${"%.0f".format(totalDistributed)}",
                    valueColor = if (overAllocated) error else MaterialTheme.colorScheme.onBackground,
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(primary.copy(alpha = 0.18f)),
                )
                SummaryStatColumn(
                    label = if (overAllocated) "Over by" else "Remaining",
                    value = "₹${"%.0f".format(if (overAllocated) -remaining else remaining)}",
                    valueColor = if (overAllocated) error else Color(0xFFF5A623),
                )
            }

            // ── Horizontal divider ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(primary.copy(alpha = 0.12f)),
            )

            // ── Progress bar + labels ─────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                IntervalProgressBar(
                    fraction = animPct,
                    height = 8.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${(animPct * 100).toInt()}% allocated",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (overAllocated) "Over-allocated!" else "${(100 - (animPct * 100).toInt())}% free",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (overAllocated) error else fillColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStatColumn(
    label: String,
    value: String,
    valueColor: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = valueColor,
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Interval progress bar  (solid colour, no gradient)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun IntervalProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = Color.Black.copy(alpha = 0.08f),
) {
    val clipped = fraction.coerceIn(0f, 1f)
    val fillColor = progressFillColor(clipped)
    Box(
        modifier = modifier.height(height).clip(RoundedCornerShape(999.dp)).background(trackColor),
    ) {
        if (clipped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clipped)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(fillColor),
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Goal allocation card
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalAllocationCard(
    goal: SavingsGoalUi,
    alloc: Double,
    added: Double,
    primary: Color,
    error: Color,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onSliderChange: (Double) -> Unit,
) {
    val iconBg = MaterialTheme.colorScheme.primary.copy(0.15f)
    val iconTint = Color.Unspecified

    val pct = if (goal.targetAmount > 0) (alloc / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    else 0f
    val animPct by animateFloatAsState(
        targetValue = pct,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "gpct_${goal.id}",
    )
    val needAmt = (goal.targetAmount - alloc).coerceAtLeast(0.0)
    val isComplete = alloc >= goal.targetAmount

    // Slider bounds — lower end is the already-saved amount; upper is the target
    val sliderMin = goal.savedAmount.toFloat()
    val sliderMax = goal.targetAmount.toFloat()
    val sliderRange = (sliderMax - sliderMin).coerceAtLeast(0f)
    // ₹100 per step; coerce into [0, 999] as required by Slider API
    val steps = if (sliderRange > 0) ((sliderRange / 100f).toInt() - 1).coerceIn(0, 999)
    else 0

    var inputText by remember(alloc) { mutableStateOf(alloc.toLong().toString()) }

    GradientCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(DashboardDimens.borderStroke, MaterialTheme.colorScheme.outlineVariant),
                GradientCardShape,
            ),
        elevation = DashboardDimens.cardElevation,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header: icon · name · need-badge ─────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)).background(iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(goalIconToDrawable(goal.icon)),
                        contentDescription = goal.name,
                        modifier = Modifier.size(20.dp),
                        tint = iconTint,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "₹${"%.0f".format(goal.savedAmount)} / ₹${"%.0f".format(goal.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
                // Need / complete badge
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(
                        if (isComplete) primary.copy(alpha = 0.10f)
                        else error.copy(alpha = 0.09f)
                    ).padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (isComplete) "✓ Done"
                        else "Need ₹${"%.0f".format(needAmt)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isComplete) primary else error,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Gradient progress bar + % ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IntervalProgressBar(
                    fraction = animPct,
                    height = 7.dp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${(pct * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = progressFillColor(pct),
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Amount input + "added" tag ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SimpleTextField(
                    value = inputText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }
                        inputText = digits
                        digits.toDoubleOrNull()?.let {
                            onSliderChange(it.coerceIn(sliderMin.toDouble(), sliderMax.toDouble()))
                        }
                    },
                    prefix = {
                        Text(
                            "₹",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = primary,
                        )
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    ),
                    modifier = Modifier.weight(1f),
                    bgColor = MaterialTheme.colorScheme.surfaceContainer,
                    height = 44.dp,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )

                if (added > 0) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(primary.copy(alpha = 0.10f))
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = "+₹${"%.0f".format(added)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Slider with small thumb + stepper buttons ─────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // − button
                OutlinedIconButton(
                    onClick = onMinus,
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(9.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Text(
                        "−",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Slider(
                    value = alloc.toFloat().coerceIn(sliderMin, sliderMax),
                    onValueChange = { onSliderChange(it.toDouble()) },
                    valueRange = sliderMin..sliderMax,
                    steps = steps,
                    modifier = Modifier.weight(1f),
                    // Custom thumb: 14dp circle — visually smaller than the 20dp default
                    thumb = {
                        Box(
                            modifier = Modifier.size(14.dp).clip(CircleShape).background(primary)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        )
                    },
                    // Custom track: solid teal active portion, no interval-color override
                    track = { sliderState ->
                        val filled =
                            if (sliderMax > sliderMin) ((sliderState.value - sliderMin) / (sliderMax - sliderMin)).coerceIn(
                                0f,
                                1f
                            )
                            else 0f
                        Box(
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(999.dp))
                                .background(primary.copy(alpha = 0.14f)),
                        ) {
                            if (filled > 0f) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(filled).fillMaxHeight()
                                        .clip(RoundedCornerShape(999.dp)).background(primary),
                                )
                            }
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = primary,
                        activeTrackColor = primary,
                        inactiveTrackColor = primary.copy(alpha = 0.14f),
                    ),
                )

                // + button
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(primary.copy(0.10f))
                        .border(BorderStroke(1.dp, primary.copy(0.28f)), RoundedCornerShape(9.dp))
                        .clickable(onClick = onPlus),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", fontSize = 17.sp, fontWeight = FontWeight.Normal, color = primary)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Preview helpers
// ══════════════════════════════════════════════════════════════════════════════

private fun distributePreviewGoals() = listOf(
    SavingsGoalUi(
        id = 1, name = "iPhone 15", icon = "iphone", colorHex = "#9B59F5",
        targetAmount = 80000.0, savedAmount = 18450.0,
        targetDateEpoch = epochDayMonthsFromNow(1),
        autoContribute = true, autoContributeAmount = 500.0,
        autoContributeFrequency = ContributeFrequency.DAILY, isCompleted = false,
    ),
    SavingsGoalUi(
        id = 2, name = "Camera", icon = "camera", colorHex = "#3498DB",
        targetAmount = 85000.0, savedAmount = 5450.0,
        targetDateEpoch = epochDayMonthsFromNow(3),
        autoContribute = true, autoContributeAmount = 320.0,
        autoContributeFrequency = ContributeFrequency.DAILY, isCompleted = false,
    ),
)

private fun epochDayMonthsFromNow(months: Int): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, months)
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis / 86_400_000L
}

@Preview(
    name = "Distribute – initial",
    showBackground = true,
    device = "spec:width=390dp,height=844dp,dpi=430",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewDistributeInitial() {
    val goals = distributePreviewGoals()
    TruXpenseTheme {
        DistributiveScreenContent(
            totalAvailable = 27450.0,
            goals = goals,
            allocations = goals.associate { it.id to it.savedAmount },
            totalDistributed = 0.0,
            onBack = {}, onAdjust = { _, _ -> },
            onSetAllocation = { _, _ -> }, onConfirm = {},
        )
    }
}

@Preview(
    name = "Distribute – partial",
    showBackground = true,
    device = "spec:width=390dp,height=844dp,dpi=430",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewDistributePartial() {
    val goals = distributePreviewGoals()
    TruXpenseTheme {
        DistributiveScreenContent(
            totalAvailable = 27450.0,
            goals = goals,
            allocations = mapOf(1L to 30000.0, 2L to 12000.0),
            totalDistributed = 18000.0,
            onBack = {}, onAdjust = { _, _ -> },
            onSetAllocation = { _, _ -> }, onConfirm = {},
        )
    }
}

@Preview(
    name = "Distribute – over-allocated",
    showBackground = true,
    device = "spec:width=390dp,height=844dp,dpi=430",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewDistributeOver() {
    val goals = distributePreviewGoals()
    TruXpenseTheme {
        DistributiveScreenContent(
            totalAvailable = 27450.0,
            goals = goals,
            allocations = mapOf(1L to 36450.0, 2L to 12450.0),
            totalDistributed = 31000.0,
            onBack = {}, onAdjust = { _, _ -> },
            onSetAllocation = { _, _ -> }, onConfirm = {},
        )
    }
}

