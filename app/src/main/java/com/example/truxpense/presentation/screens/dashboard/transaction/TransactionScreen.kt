package com.example.truxpense.presentation.screens.dashboard.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.theme.DashboardDimens

// ─── Local colour tokens ──────────────────────────────────────────────────────

private val ColorTeal          = Color(0xFF4DB6B6)
private val ColorAmountRed     = Color(0xFFE53935)
private val ColorSubtext       = Color(0xFF8A95A3)
private val ColorDivider       = Color(0xFFEEF0F4)
private val ColorSearchBg      = Color(0xFFF4F5F7)
private val ColorChipBorder    = Color(0xFFDDE2E8)
private val ColorTabIndicator  = ColorTeal
private val ColorBadgeBg       = Color(0xFFF0F2F5)
private val ColorFilterIcon    = Color(0xFF5B6470)

// ─── Screen ───────────────────────────────────────────────────────────────────

/**
 * Transactions screen — pure UI layer.
 *
 * Reads everything from [TransactionsViewModel]; no logic, formatting, or
 * filtering happens in this file.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onBack: () -> Unit = {},
    vm: TransactionsViewModel = hiltViewModel(),
) {
    val selectedPeriod  by vm.selectedPeriod.collectAsState()
    val periodNavLabel  by vm.periodNavLabel.collectAsState()
    val canNavBack      by vm.canNavBack.collectAsState()
    val canNavForward   by vm.canNavForward.collectAsState()
    val searchQuery     by vm.searchQuery.collectAsState()
    val activeFilters   by vm.activeFilters.collectAsState()
    val monthGroups     by vm.monthGroups.collectAsState()
    val totalExpanded   by vm.totalExpanded.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "Transactions",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter            = painterResource(R.drawable.back_icon),
                            contentDescription = "Back",
                            tint               = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = DashboardDimens.spaceXxl),
        ) {

            // ── ① Period tab row ──────────────────────────────────────────
            item {
                PeriodTabRow(
                    selected = selectedPeriod,
                    onSelect = { vm.selectPeriod(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DashboardDimens.screenPaddingH),
                )
            }

            // ── ② Date navigator ─────────────────────────────────────────
            item {
                DateNavigatorRow(
                    label      = periodNavLabel,
                    canBack    = canNavBack,
                    canForward = canNavForward,
                    onBack     = { vm.navBack() },
                    onForward  = { vm.navForward() },
                    modifier   = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DashboardDimens.spaceMd),
                )
            }

            item { Spacer(Modifier.height(DashboardDimens.spaceMd)) }

            // ── ③ Search bar ──────────────────────────────────────────────
            item {
                SearchBar(
                    query    = searchQuery,
                    onChange = { vm.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DashboardDimens.screenPaddingH),
                )
            }

            item { Spacer(Modifier.height(DashboardDimens.spaceLg)) }

            // ── ④ Filter row ──────────────────────────────────────────────
            item {
                FilterRow(
                    activeFilters = activeFilters,
                    onToggle      = { vm.toggleFilter(it) },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DashboardDimens.screenPaddingH),
                )
            }

            item { Spacer(Modifier.height(DashboardDimens.spaceXl)) }

            // ── ⑤ Month groups ────────────────────────────────────────────
            monthGroups.forEach { monthGroup ->

                // Month header
                item(key = "header_${monthGroup.monthLabel}") {
                    MonthGroupHeader(
                        monthLabel   = monthGroup.monthLabel,
                        totalSpent   = monthGroup.totalSpent,
                        expanded     = totalExpanded,
                        onToggle     = { vm.toggleTotalExpanded() },
                        modifier     = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DashboardDimens.screenPaddingH)
                            .padding(bottom = DashboardDimens.spaceMd),
                    )
                }

                // Day sub-groups
                monthGroup.days.forEach { dayGroup ->

                    // Day label
                    item(key = "day_${monthGroup.monthLabel}_${dayGroup.dayLabel}") {
                        Text(
                            text     = dayGroup.dayLabel,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = ColorSubtext,
                            modifier = Modifier
                                .padding(
                                    horizontal = DashboardDimens.screenPaddingH,
                                    vertical   = DashboardDimens.spaceXs,
                                ),
                        )
                    }

                    // Transaction rows with dividers
                    items(
                        items = dayGroup.items,
                        key   = { "${monthGroup.monthLabel}_${dayGroup.dayLabel}_${it.id}" },
                    ) { tx ->
                        TransactionRow(
                            tx       = tx,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DashboardDimens.screenPaddingH),
                        )
                        HorizontalDivider(
                            thickness = DashboardDimens.dividerThin,
                            color     = ColorDivider,
                        )
                    }

                    item { Spacer(Modifier.height(DashboardDimens.spaceMd)) }
                }
            }
        }
    }
}

// ─── ① Period Tab Row ─────────────────────────────────────────────────────────

@Composable
private fun PeriodTabRow(
    selected: TransactionPeriod,
    onSelect: (TransactionPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        TransactionPeriod.entries.forEach { period ->
            val isSelected = period == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick           = { onSelect(period) },
                    )
                    .padding(
                        horizontal = DashboardDimens.spaceXl,
                        vertical   = DashboardDimens.spaceMd,
                    ),
            ) {
                Text(
                    text       = period.label,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (isSelected) ColorTeal else ColorSubtext,
                )
                // Teal underline indicator for selected tab
                Spacer(Modifier.height(DashboardDimens.spaceXs))
                Box(
                    modifier = Modifier
                        .width(DashboardDimens.spaceXl + DashboardDimens.spaceXl)  // ~32dp
                        .height(if (isSelected) 2.dp else 0.dp)
                        .background(
                            color = ColorTabIndicator,
                            shape = RoundedCornerShape(1.dp),
                        ),
                )
            }
        }
    }
}

// ─── ② Date Navigator Row ────────────────────────────────────────────────────

@Composable
private fun DateNavigatorRow(
    label: String,
    canBack: Boolean,
    canForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick  = onBack,
            enabled  = canBack,
            modifier = Modifier.size(DashboardDimens.iconButtonSm + DashboardDimens.spaceMd),
        ) {
            Icon(
                painter            = painterResource(R.drawable.left_arrow),
                contentDescription = "Previous period",
                tint = MaterialTheme.colorScheme.onBackground
                    .copy(alpha = if (canBack) 1f else 0.35f),
                modifier = Modifier.size(DashboardDimens.iconSm),
            )
        }

        Text(
            text       = label,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.onBackground,
        )

        IconButton(
            onClick  = onForward,
            enabled  = canForward,
            modifier = Modifier.size(DashboardDimens.iconButtonSm + DashboardDimens.spaceMd),
        ) {
            Icon(
                painter            = painterResource(R.drawable.right_arrow),
                contentDescription = "Next period",
                tint = MaterialTheme.colorScheme.onBackground
                    .copy(alpha = if (canForward) 1f else 0.35f),
                modifier = Modifier.size(DashboardDimens.iconSm),
            )
        }
    }
}

// ─── ③ Search Bar ────────────────────────────────────────────────────────────

@Composable
private fun SearchBar(
    query: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value         = query,
        onValueChange = onChange,
        modifier      = modifier
            .clip(RoundedCornerShape(DashboardDimens.cornerChip))
            .background(ColorSearchBg)
            .padding(
                horizontal = DashboardDimens.spaceLg,
                vertical   = DashboardDimens.spaceMdL,
            ),
        textStyle       = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onBackground,
        ),
        cursorBrush     = SolidColor(ColorTeal),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        decorationBox   = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Default.Search,
                    contentDescription = "Search",
                    tint               = ColorSubtext,
                    modifier           = Modifier.size(DashboardDimens.iconMd),
                )
                Spacer(Modifier.width(DashboardDimens.spaceMd))
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text  = "Search transactions…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorSubtext,
                        )
                    }
                    inner()
                }
            }
        },
    )
}

// ─── ④ Filter Row ────────────────────────────────────────────────────────────

@Composable
private fun FilterRow(
    activeFilters: Set<TransactionFilter>,
    onToggle: (TransactionFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier              = modifier,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
    ) {
        // "⊞ Filter" label
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter            = painterResource(R.drawable.filter),
                contentDescription = null,
                tint               = ColorFilterIcon,
                modifier           = Modifier.size(DashboardDimens.iconSm),
            )
            Spacer(Modifier.width(DashboardDimens.spaceXs))
            Text(
                text  = "Filter",
                style = MaterialTheme.typography.bodySmall,
                color = ColorFilterIcon,
            )
        }

        // Filter chips
        TransactionFilter.entries.forEach { filter ->
            val isActive = filter in activeFilters
            FilterChip(
                selected = isActive,
                onClick  = { onToggle(filter) },
                label    = {
                    Text(
                        text  = filter.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isActive) ColorTeal else MaterialTheme.colorScheme.onBackground,
                    )
                },
                colors   = FilterChipDefaults.filterChipColors(
                    containerColor         = Color.Transparent,
                    selectedContainerColor = ColorTeal.copy(alpha = 0.08f),
                    labelColor             = MaterialTheme.colorScheme.onBackground,
                    selectedLabelColor     = ColorTeal,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled          = true,
                    selected         = isActive,
                    borderColor      = ColorChipBorder,
                    selectedBorderColor = ColorTeal,
                    borderWidth      = 1.dp,
                    selectedBorderWidth = 1.dp,
                ),
                shape = RoundedCornerShape(DashboardDimens.cornerBadge + 2.dp),
            )
        }
    }
}

// ─── ⑤ Month Group Header ────────────────────────────────────────────────────

@Composable
private fun MonthGroupHeader(
    monthLabel: String,
    totalSpent: Double,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Format total as "₹25,000" — the VM passes the raw Double, we format once here
    val formattedTotal = "₹${"%,.0f".format(totalSpent)}"

    Row(
        modifier              = modifier,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text       = monthLabel,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onBackground,
        )

        // Total + dropdown chevron — tappable
        Row(
            modifier = Modifier.clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onToggle,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceXxs),
        ) {
            Text(
                text       = formattedTotal,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground,
            )
            Icon(
                imageVector        = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint               = ColorTeal,
                modifier           = Modifier.size(DashboardDimens.iconMd),
            )
        }
    }
}

// ─── Transaction Row ─────────────────────────────────────────────────────────

@Composable
private fun TransactionRow(
    tx: TransactionItem,
    modifier: Modifier = Modifier,
) {
    val formattedAmount = "−₹${"%,.0f".format(-tx.amount)}"   // always show positive with − prefix

    Row(
        modifier = modifier.padding(vertical = DashboardDimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left — merchant + category·time
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = tx.merchant,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(DashboardDimens.spaceXxs))
            Text(
                text  = "${tx.category}.${tx.timeLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = ColorSubtext,
            )
        }

        // Right — amount + payment badge
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text       = formattedAmount,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color      = ColorAmountRed,
            )
            Spacer(Modifier.height(DashboardDimens.spaceXxs))
            PaymentBadge(label = tx.paymentMethod)
        }
    }
}

// ─── Payment Method Badge ─────────────────────────────────────────────────────

@Composable
private fun PaymentBadge(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(DashboardDimens.cornerBadge))
            .background(ColorBadgeBg)
            .padding(
                horizontal = DashboardDimens.spaceSm,
                vertical   = DashboardDimens.spaceXxs,
            ),
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = ColorSubtext,
        )
    }
}

// ─── Preview ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun TransactionsScreenPreview() {
    MaterialTheme { TransactionsScreen() }
}