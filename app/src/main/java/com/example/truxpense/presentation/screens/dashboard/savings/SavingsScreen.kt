package com.example.truxpense.presentation.screens.dashboard.savings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.truxpense.R
import com.example.truxpense.data.repository.savings.ContributeFrequency
import com.example.truxpense.data.repository.savings.SavingsEntryUi
import com.example.truxpense.data.repository.savings.SavingsGoalUi
import com.example.truxpense.presentation.screens.dashboard.components.BudgetProgressBar
import com.example.truxpense.presentation.screens.dashboard.components.GradientCard
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.theme.DashboardDimens.screenPaddingH
import com.example.truxpense.presentation.theme.TruXpenseTheme
import java.util.*

@Composable
fun SavingsScreen(
    onCreateGoal: () -> Unit,
    onAddSavings: () -> Unit,
    onGoalClick: (Long) -> Unit,
    onDistribute: () -> Unit,
    onBack: () -> Unit,
    vm: SavingsViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    val cal = remember { Calendar.getInstance() }
    val curMonth = remember { cal.get(Calendar.MONTH) }
    val curYear = remember { cal.get(Calendar.YEAR) }
    val lastCal = remember { Calendar.getInstance().apply { add(Calendar.MONTH, -1) } }
    val lastMo = remember { lastCal.get(Calendar.MONTH) }
    val lastYr = remember { lastCal.get(Calendar.YEAR) }

    val thisMonthSaved = remember(state.recentEntries) {
        state.recentEntries.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestampMs }
            c.get(Calendar.MONTH) == curMonth && c.get(Calendar.YEAR) == curYear
        }.sumOf { it.amount }
    }
    val lastMonthSaved = remember(state.recentEntries) {
        state.recentEntries.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestampMs }
            c.get(Calendar.MONTH) == lastMo && c.get(Calendar.YEAR) == lastYr
        }.sumOf { it.amount }
    }

    val allocatedToGoals = remember(state.goals) { state.goals.sumOf { it.savedAmount } }
    val unallocated = remember(state.totalSavings, allocatedToGoals) {
        (state.totalSavings - allocatedToGoals).coerceAtLeast(0.0)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        ScreenTopBar(
            headerTitle = "Savings",
            showBack = true,
            onBack = onBack,
            actions = {
                Box {
                    IconButton(onClick = onAddSavings) {
                        Box(
                            modifier = Modifier.size(38.dp).border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                                CircleShape,
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier.matchParentSize().background(
                                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.15f), CircleShape,
                                ).blur(8.dp)
                            )
                            Icon(
                                painter = painterResource(R.drawable.add),
                                contentDescription = "Add savings",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            },
        )

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = screenPaddingH),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SavingsTotalCard(
                total = state.totalSavings,
                thisMonthSaved = thisMonthSaved,
                lastMonthSaved = lastMonthSaved,
                unallocated = unallocated,
                onDistribute = onDistribute,
            )

//            if (state.recentEntries.isNotEmpty()) {
//                SectionLabel("Recent Savings")
//                RecentSavingsCard(entries = state.recentEntries)
//            }

            SectionLabel("Saving Goals")
            if (state.goals.isEmpty()) {
                CreateGoalRow(onClick = onCreateGoal)
            } else {
                state.goals.forEach { goal ->
                    GoalCard(goal = goal, onClick = { onGoalClick(goal.id) })
                }
                CreateGoalRow(onClick = onCreateGoal)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Total savings card (now uses GradientCard)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SavingsTotalCard(
    total: Double,
    thisMonthSaved: Double,
    lastMonthSaved: Double,
    unallocated: Double,
    onDistribute: () -> Unit,
) {
    val pctVsLast = if (lastMonthSaved > 0) ((thisMonthSaved - lastMonthSaved) / lastMonthSaved * 100).toInt()
    else null

    GradientCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = DashboardDimens.cardElevation,
    ) {
        // Draw inner content with the same border and padding as before
        Column(modifier = Modifier.padding(DashboardDimens.cardPadding)) {

            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Total savings",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Button(
                    onClick = onDistribute,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1BAF9D),
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp),
                ) {
                    Text(
                        text = "Distribute to goals",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = formatInr(total),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-1).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                text = "Available to distribute into your goals",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                MiniStatSavings(
                    label = "This month",
                    amount = thisMonthSaved,
                    subLabel = if (pctVsLast != null) "↑${kotlin.math.abs(pctVsLast)}% vs last"
                    else "Baseline",
                    subColor = if (pctVsLast != null && pctVsLast > 0) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider(
                    modifier = Modifier.height(48.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                MiniStatSavings(
                    label = "Last month",
                    amount = lastMonthSaved,
                    subLabel = "Baseline",
                    subColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider(
                    modifier = Modifier.height(48.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                MiniStatSavings(
                    label = "Unallocated",
                    amount = unallocated,
                    subLabel = "Not in goals",
                    subColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MiniStatSavings(
    label: String,
    amount: Double,
    subLabel: String,
    subColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "+${formatInr(amount)}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subLabel,
            fontSize = 11.sp,
            color = subColor,
            textAlign = TextAlign.Center,
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Recent savings card
// ══════════════════════════════════════════════════════════════════════════════

//@Composable
//private fun RecentSavingsCard(entries: List<SavingsEntryUi>) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(DashboardDimens.cornerCard),
//        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
//        border = BorderStroke(DashboardDimens.borderStroke, MaterialTheme.colorScheme.outlineVariant),
//        elevation = CardDefaults.cardElevation(0.dp),
//    ) {
//        entries.forEachIndexed { i, entry ->
//            Row(
//                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically,
//            ) {
//                Row(
//                    modifier = Modifier.weight(1f),
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(12.dp),
//                ) {
//                    Box(
//                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
//                            .background(MaterialTheme.colorScheme.surfaceContainer),
//                        contentAlignment = Alignment.Center,
//                    ) {
//                        Icon(
//                            painter = painterResource(savingsEntryIcon(entry.label)),
//                            contentDescription = null,
//                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
//                            modifier = Modifier.size(18.dp),
//                        )
//                    }
//                    Column {
//                        Text(
//                            text = entry.label,
//                            style = MaterialTheme.typography.bodyMedium,
//                            fontWeight = FontWeight.SemiBold,
//                            color = MaterialTheme.colorScheme.onBackground,
//                            maxLines = 1,
//                            overflow = TextOverflow.Ellipsis,
//                        )
//                        Text(
//                            text = entry.dateDisplay(),
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                            modifier = Modifier.padding(top = 2.dp),
//                        )
//                    }
//                }
//                Text(
//                    text = "+${formatInr(entry.amount)}",
//                    style = MaterialTheme.typography.titleSmall,
//                    color = MaterialTheme.colorScheme.primary,
//                    fontWeight = FontWeight.Bold,
//                )
//            }
//            if (i < entries.size - 1) {
//                HorizontalDivider(
//                    thickness = DashboardDimens.dividerThin,
//                    color = MaterialTheme.colorScheme.outlineVariant,
//                )
//            }
//        }
//    }
//}

private fun savingsEntryIcon(label: String): Int {
    val l = label.lowercase()
    return when {
        "bank" in l || "transfer" in l -> R.drawable.account_balance
        "salary" in l || "pay" in l -> R.drawable.payments
        "cash" in l -> R.drawable.wallet_
        else -> R.drawable.add_notes_icon
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Goal card (now uses GradientCard)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun GoalCard(goal: SavingsGoalUi, onClick: () -> Unit) {
    val iconBg = MaterialTheme.colorScheme.primary.copy(0.15f)
    val primary = MaterialTheme.colorScheme.primary
    val daysLeft = goal.daysLeft()
    val isUrgent = daysLeft in 0..30
    // choose icon + label instead of emoji in-string
    val urgencyIconRes = when {
        isUrgent && daysLeft <= 7 -> R.drawable.alert_1
        isUrgent -> R.drawable.alert_
        else -> R.drawable.calender
    }
    val urgencyText = when {
        isUrgent && daysLeft <= 7 -> "Urgent · Deadline very near"
        isUrgent -> "Urgent · Deadline near"
        else -> "${goal.targetDateDisplay()} · deadline"
    }

    GradientCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = DashboardDimens.cardElevation,
    ) {
        // inner bordered container to mimic previous Card border
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Icon + name + amounts + chevron ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = goalIconToDrawable(goal.icon)),
                        contentDescription = goal.name,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Target · ${goal.targetDateDisplay()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                // Saved / Target
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${formatInr(goal.savedAmount)}/",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primary,
                    )
                    Text(
                        text = formatInr(goal.targetAmount),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 1.dp),
                    )
                }

                Icon(
                    painter = painterResource(R.drawable.right_arrow),
                    contentDescription = "Open goal",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(DashboardDimens.iconMd),
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── % + progress bar ──────────────────────────────────────────────
            val animPct by animateFloatAsState(
                targetValue = goal.progressFraction,
                animationSpec = tween(600, easing = FastOutSlowInEasing),
                label = "gpct_${goal.id}",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "${goal.progressPercent}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primary,
                )
                BudgetProgressBar(
                    progress = goal.progressFraction,
                    progressMultiplier = animPct / goal.progressFraction.coerceAtLeast(0.001f),
                    showLabel = false,
                    height = DashboardDimens.progressBarHeight2,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(6.dp))

            // ── Remaining + days left ─────────────────────────────────────────
            Text(
                text = "${formatInr(goal.remaining)} remaining · $daysLeft days left",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))

            // ── Urgency label + Add button ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = painterResource(id = urgencyIconRes),
                        contentDescription = null,
                        tint = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = urgencyText,
                        fontSize = 11.sp,
                        fontWeight = if (isUrgent) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isUrgent) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (goal.autoContribute && goal.autoContributeAmount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { /* navigate to detail for quick-add */ },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp),
                    ) {
                        Text(
                            text = "Add ${formatInr(goal.autoContributeAmount)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// "Create a new goal" row
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CreateGoalRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(DashboardDimens.cornerCard))
            .background(MaterialTheme.colorScheme.surfaceContainer).border(
                border = BorderStroke(DashboardDimens.borderStroke, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(DashboardDimens.cornerCard),
            ).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = {}) {
            Box(
                modifier = Modifier.size(38.dp).border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                    CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.matchParentSize().background(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.85f), CircleShape,
                    ).blur(8.dp)
                )
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = "Add savings",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Column {
            Text(
                text = "Create a new goal",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Set a target amount and time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Shared helpers (exported for other screens)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun SectionLabel(label: String) {
    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
fun Chip(label: String, color: Color = MaterialTheme.colorScheme.primary) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun QuickAddChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** ₹1,23,456 formatting (Indian numbering). */
private fun formatInr(amount: Double): String {
    val long = amount.toLong()
    val s = long.toString()
    if (s.length <= 3) return "₹$s"
    val last3 = s.takeLast(3)
    val rest = s.dropLast(3)
    val groups = buildString {
        rest.reversed().chunked(2).forEachIndexed { i, chunk ->
            if (i > 0) append(',')
            append(chunk.reversed())
        }
    }.reversed()
    return "₹$groups,$last3"
}

/** Parse hex "#RRGGBB" to Compose Color safely. */
fun parseHexColor(hex: String): Color = try {
    Color(hex.toColorInt())
} catch (e: Exception) {
    Color(0xFF2FA4A9)
}

// ══════════════════════════════════════════════════════════════════════════════
// Preview
// ══════════════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, widthDp = 390, heightDp = 960, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Composable
private fun SavingsScreenPreview() {
    TruXpenseTheme {
        val goals = listOf(
            SavingsGoalUi(
                id = 1L, name = "iPhone", icon = "📱", colorHex = "#9B59F5",
                targetAmount = 80000.0, savedAmount = 18450.0, targetDateEpoch = 19900L,
                autoContribute = true, autoContributeAmount = 500.0,
                autoContributeFrequency = ContributeFrequency.DAILY, isCompleted = false,
            ),
            SavingsGoalUi(
                id = 2L, name = "Camera", icon = "📷", colorHex = "#3498DB",
                targetAmount = 85000.0, savedAmount = 8450.0, targetDateEpoch = 20000L,
                autoContribute = true, autoContributeAmount = 500.0,
                autoContributeFrequency = ContributeFrequency.DAILY, isCompleted = false,
            ),
        )
        val entries = listOf(
            SavingsEntryUi(1L, "Bank transfer", 2000.0, System.currentTimeMillis() - 86_400_000L),
            SavingsEntryUi(2L, "Salary Saved", 1000.0, System.currentTimeMillis() - 86_400_000L),
        )
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        ) {
            ScreenTopBar(
                headerTitle = "Savings",
                showBack = false,
                actions = {
                    Box {
                        IconButton(onClick = {}) {
                            Box(
                                modifier = Modifier.size(38.dp).border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                                    CircleShape,
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier.matchParentSize().background(
                                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.15f), CircleShape,
                                    ).blur(8.dp)
                                )
                                Icon(
                                    painter = painterResource(R.drawable.add),
                                    contentDescription = "Add savings",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                },
            )
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = screenPaddingH),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SavingsTotalCard(
                    total = 27450.0,
                    thisMonthSaved = 5000.0,
                    lastMonthSaved = 2600.0,
                    unallocated = 8500.0,
                    onDistribute = {},
                )
//                SectionLabel("Recent Savings")
//                RecentSavingsCard(entries = entries)
                SectionLabel("Saving Goals")
                goals.forEach { GoalCard(goal = it, onClick = {}) }
                CreateGoalRow(onClick = {})
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
