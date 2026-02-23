package com.example.truxpense.presentation.screens.dashboard.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.DateNavigatorRow
import com.example.truxpense.presentation.screens.dashboard.components.PeriodTabRow
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.screens.dashboard.theme.DashboardDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    vm: TransactionsViewModel = hiltViewModel(),
) {
    val selectedPeriod by vm.selectedPeriod.collectAsState()
    val periodNavLabel by vm.periodNavLabel.collectAsState()
    val canNavBack by vm.canNavBack.collectAsState()
    val canNavForward by vm.canNavForward.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val selectedCategory by vm.selectedCategory.collectAsState()
    val selectedPayment by vm.paymentMethod.collectAsState()
    val availableCats by vm.availableCategories.collectAsState()
    val availablePayments by vm.availablePaymentMethods.collectAsState()
    val monthGroupsVal by vm.monthGroups.collectAsState()
    val totalExpanded by vm.totalExpanded.collectAsState()
    val activeFilterCount by vm.activeFilterCount.collectAsState()
    val hasActiveFilter by vm.hasActiveFiltersOrSearch.collectAsState()

    // ── Bottom-sheet state ────────────────────────────────────────────────────
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFilterSheet by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TransactionTopBar()
        },
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(top = DashboardDimens.heroZonePadTop),
            contentPadding = PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = DashboardDimens.spaceXxl),
        ) {

            // ── ① Period tab row ──────────────────────────────────────────────
            item {
                PeriodTabRow(
                    selected = selectedPeriod,
                    onSelect = { vm.selectPeriod(it) },
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = DashboardDimens.screenPaddingH, end = DashboardDimens.screenPaddingH),
                )
            }

            // ── ② Date navigator ─────────────────────────────────────────────
            item {
                DateNavigatorRow(
                    label = periodNavLabel,
                    canBack = canNavBack,
                    canForward = canNavForward,
                    onBack = { vm.navBack() },
                    onForward = { vm.navForward() },
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = DashboardDimens.spaceMd, end = DashboardDimens.spaceMd),
                )
            }

            item { Spacer(Modifier.height(DashboardDimens.spaceMd)) }

            // ── ③ Search bar + Filter button ──────────────────────────────────
            item {
                SearchAndFilterRow(
                    query = searchQuery,
                    onQueryChange = { vm.setSearchQuery(it) },
                    onClearSearch = {
                        vm.clearSearch()
                        focusManager.clearFocus()
                    },
                    activeFilterCount = activeFilterCount,
                    onFilterClick = { showFilterSheet = true },
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = DashboardDimens.screenPaddingH, end = DashboardDimens.screenPaddingH),
                )
            }

            // ── ④ Active filter chips (animated) ──────────────────────────────
            item {
                AnimatedVisibility(
                    visible = selectedCategory != null || selectedPayment != null,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    ActiveFilterChipsRow(
                        selectedCategory = selectedCategory,
                        selectedPayment = selectedPayment,
                        onClearCategory = { vm.setCategory(null) },
                        onClearPayment = { vm.setPaymentMethod(null) },
                        modifier = Modifier.fillMaxWidth().padding(
                            start = DashboardDimens.screenPaddingH,
                            end = DashboardDimens.screenPaddingH,
                            top = DashboardDimens.spaceMd,
                        ),
                    )
                }
            }

            item { Spacer(Modifier.height(DashboardDimens.spaceMd)) }

            // ── ⑥ Empty state ────────────────────────────────────────────────
            if (monthGroupsVal.isEmpty()) {
                item {
                    TransactionsEmptyState(
                        hasFilter = hasActiveFilter,
                        onClearAll = { vm.clearAllFilters() },
                        modifier = Modifier.fillMaxWidth()
                            .padding(top = DashboardDimens.fabClearance - DashboardDimens.spaceXxl),
                    )
                }
            } else {
                // ── ⑦ Month groups ───────────────────────────────────────────
                monthGroupsVal.forEach { monthGroup ->

                    item(key = "header_${monthGroup.monthLabel}") {
                        MonthGroupHeader(
                            monthLabel = monthGroup.monthLabel,
                            totalSpent = monthGroup.totalSpent,
                            expanded = totalExpanded,
                            onToggle = { vm.toggleTotalExpanded() },
                            modifier = Modifier.fillMaxWidth()
                                .padding(start = DashboardDimens.screenPaddingH, end = DashboardDimens.screenPaddingH)
                                .padding(bottom = DashboardDimens.spaceMd),
                        )
                    }

                    monthGroup.days.forEach { dayGroup ->

                        item(key = "day_${monthGroup.monthLabel}_${dayGroup.dayLabel}") {
                            Text(
                                text = dayGroup.dayLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.padding(
                                    start = DashboardDimens.screenPaddingH,
                                    end = DashboardDimens.screenPaddingH,
                                    top = DashboardDimens.spaceXs,
                                    bottom = DashboardDimens.spaceXs,
                                    ),
                            )
                        }

                        itemsIndexed(
                            items = dayGroup.items,
                            key = { idx, tx -> "${monthGroup.monthLabel}_${dayGroup.dayLabel}_${tx.id}_$idx" },
                        ) { _, tx ->
                            TransactionRow(
                                tx = tx,
                                query = searchQuery,
                                modifier = Modifier.fillMaxWidth().padding(
                                    start = DashboardDimens.screenPaddingH,
                                    end = DashboardDimens.screenPaddingH
                                ),
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                thickness = DividerDefaults.Thickness,
                            )
                        }

                        item { Spacer(Modifier.height(DashboardDimens.spaceMd)) }
                    }
                }
            }
        }

        // ── Filter bottom sheet ────────────────────────────────────────────────
        if (showFilterSheet) {
            FilterBottomSheet(
                sheetState = sheetState,
                availableCategories = availableCats,
                availablePayments = availablePayments,
                selectedCategory = selectedCategory,
                selectedPayment = selectedPayment,
                onSelectCategory = { vm.setCategory(it) },
                onSelectPayment = { vm.setPaymentMethod(it) },
                onClearAll = { vm.setCategory(null); vm.setPaymentMethod(null) },
                onDismiss = { showFilterSheet = false },
            )
        }
    }
}

// ─── Top bar ──────────────────────────────────────────────────────────────────

@Composable
private fun TransactionTopBar() {
    ScreenTopBar(title = "Transactions", showBack = false)
}

@Composable
private fun SearchAndFilterRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    activeFilterCount: Int,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(DashboardDimens.buttonHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
    ) {
        // Search field — takes remaining width
        SearchField(
            query = query,
            onChange = onQueryChange,
            onClear = onClearSearch,
            modifier = Modifier.weight(1f).height(DashboardDimens.buttonHeight),
        )

        // Filter button with badge
        FilterButton(
            activeCount = activeFilterCount,
            onClick = onFilterClick,
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    BasicTextField(
        value = query,
        onValueChange = onChange,
        modifier = modifier.clip(RoundedCornerShape(DashboardDimens.cornerChip))
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(
                horizontal = DashboardDimens.spaceLg,
                vertical = DashboardDimens.spaceMdL,
            ).focusRequester(focusRequester),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onBackground,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onClear() }),
        singleLine = true,
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.size(DashboardDimens.iconMd),
                )
                Spacer(Modifier.width(DashboardDimens.spaceMd))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search merchant or category…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    }
                    inner()
                }
                // Clear X — visible only when query is non-empty
                if (query.isNotEmpty()) {
                    Spacer(Modifier.width(DashboardDimens.spaceSm))
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.size(DashboardDimens.iconSm),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun FilterButton(
    activeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVar = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = modifier) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(DashboardDimens.buttonHeight - DashboardDimens.spaceXs)
                .clip(RoundedCornerShape(DashboardDimens.cornerChip))
                .background(if (activeCount > 0) primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Icon(
                painter = painterResource(R.drawable.filter),
                contentDescription = "Filter",
                tint = if (activeCount > 0) primary else onSurfaceVar,
                modifier = Modifier.size(DashboardDimens.iconMd),
            )
        }
        // Badge
        if (activeCount > 0) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd)
                    .offset(x = DashboardDimens.badgeOffset, y = -DashboardDimens.badgeOffset)
                    .size(DashboardDimens.badgeSize).clip(CircleShape)
                    .background(primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$activeCount",
                    color = Color.White,
                    fontSize = DashboardDimens.textXs,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ─── ④ Active Filter Chips ────────────────────────────────────────────────────

@Composable
private fun ActiveFilterChipsRow(
    selectedCategory: String?,
    selectedPayment: String?,
    onClearCategory: () -> Unit,
    onClearPayment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceSm),
    ) {
        if (selectedCategory != null) {
            item {
                ActiveFilterChip(
                    label = selectedCategory,
                    prefix = "Category",
                    onClear = onClearCategory,
                )
            }
        }
        if (selectedPayment != null) {
            item {
                ActiveFilterChip(
                    label = selectedPayment,
                    prefix = "via",
                    onClear = onClearPayment,
                )
            }
        }
    }
}

@Composable
private fun ActiveFilterChip(
    label: String,
    prefix: String,
    onClear: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(primary.copy(alpha = 0.10f))
            .padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$prefix: $label",
            style = MaterialTheme.typography.labelMedium,
            color = primary,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(DashboardDimens.spaceXs))
        Box(
            modifier = Modifier.size(DashboardDimens.iconSm).clip(CircleShape).background(primary.copy(alpha = 0.18f))
                .clickable(onClick = onClear),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove filter",
                tint = primary,
                modifier = Modifier.size(10.dp),
            )
        }
    }
}


// ─── ⑥ Filter Bottom Sheet ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    sheetState: SheetState,
    availableCategories: List<String>,
    availablePayments: List<String>,
    selectedCategory: String?,
    selectedPayment: String?,
    onSelectCategory: (String?) -> Unit,
    onSelectPayment: (String?) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val sheetHandleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val errorColor = MaterialTheme.colorScheme.error


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = {
            Box(
                modifier = Modifier.padding(top = DashboardDimens.spaceLg, bottom = DashboardDimens.spaceXs)
                    .width(DashboardDimens.sheetHandleWidth).height(DashboardDimens.sheetHandleHeight)
                    .clip(RoundedCornerShape(DashboardDimens.cornerSheetHandle)).background(sheetHandleColor),
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(bottom = DashboardDimens.sheetBottomPadding),
        ) {
            // ── Sheet header ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH)
                    .padding(bottom = DashboardDimens.spaceLg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Filter transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (selectedCategory != null || selectedPayment != null) {
                    TextButton(onClick = onClearAll) {
                        Text(
                            text = "Clear all",
                            color = errorColor,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            HorizontalDivider(color = dividerColor, thickness = DividerDefaults.Thickness)
            Spacer(Modifier.height(DashboardDimens.spaceLg))

            // ── Category section ─────────────────────────────────────────────
            if (availableCategories.isNotEmpty()) {
                FilterSectionLabel(
                    text = "Category",
                    modifier = Modifier.padding(horizontal = DashboardDimens.screenPaddingH),
                )
                Spacer(Modifier.height(DashboardDimens.spaceMd))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = DashboardDimens.screenPaddingH),
                    horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceSm),
                ) {
                    // "All" chip
                    item {
                        SheetFilterChip(
                            label = "All",
                            isSelected = selectedCategory == null,
                            onClick = { onSelectCategory(null) },
                        )
                    }
                    items(availableCategories) { cat ->
                        SheetFilterChip(
                            label = cat,
                            isSelected = selectedCategory == cat,
                            onClick = { onSelectCategory(if (selectedCategory == cat) null else cat) },
                        )
                    }
                }

                Spacer(Modifier.height(DashboardDimens.spaceXl))
            }

            // ── Payment method section ────────────────────────────────────────
            if (availablePayments.isNotEmpty()) {
                FilterSectionLabel(
                    text = "Payment method",
                    modifier = Modifier.padding(horizontal = DashboardDimens.screenPaddingH),
                )
                Spacer(Modifier.height(DashboardDimens.spaceMd))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = DashboardDimens.screenPaddingH),
                    horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceSm),
                ) {
                    // "All" chip
                    item {
                        SheetFilterChip(
                            label = "All",
                            isSelected = selectedPayment == null,
                            onClick = { onSelectPayment(null) },
                        )
                    }
                    items(availablePayments) { method ->
                        SheetFilterChip(
                            label = method,
                            isSelected = selectedPayment == method,
                            onClick = { onSelectPayment(if (selectedPayment == method) null else method) },
                        )
                    }
                }

                Spacer(Modifier.height(DashboardDimens.spaceXl))
            }

            // ── Empty state for the sheet ─────────────────────────────────────
            if (availableCategories.isEmpty() && availablePayments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No data to filter yet.\nAdd some expenses first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    )
                }
            }

            // ── Done button ───────────────────────────────────────────────────
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = DashboardDimens.screenPaddingH),
                shape = RoundedCornerShape(DashboardDimens.cornerCard),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    text = "Done",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun FilterSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier,
    )
}

@Composable
private fun SheetFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer
        ).clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick,
        ).padding(horizontal = DashboardDimens.chipPaddingH, vertical = DashboardDimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceXs),
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(DashboardDimens.iconXs),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ─── Month Group Header ───────────────────────────────────────────────────────

@Composable
private fun MonthGroupHeader(
    monthLabel: String,
    totalSpent: Double,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedTotal = "₹${"%,.0f".format(totalSpent)}"

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onToggle,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceXxs),
        ) {
            Text(
                text = formattedTotal,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

// ─── Transaction Row ─────────────────────────────────────────────────────────

@Composable
private fun TransactionRow(
    tx: TransactionItem,
    query: String,
    modifier: Modifier = Modifier,
) {
    val formattedAmount = "−₹${"%,.0f".format(-tx.amount)}"

    Row(
        modifier = modifier.padding(vertical = DashboardDimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Merchant initial avatar
        Box(
            modifier = Modifier.size(DashboardDimens.avatarSize).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tx.merchant.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = DashboardDimens.textXl,
            )
        }

        Spacer(Modifier.width(DashboardDimens.spaceMd))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.merchant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(DashboardDimens.spaceXxs))
            Text(
                text = tx.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(DashboardDimens.spaceXxs))
            if (tx.paymentMethod.isNotBlank()) {
                PaymentBadge(label = tx.paymentMethod)
            }
        }
    }
}

@Composable
private fun PaymentBadge(label: String) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(DashboardDimens.cornerBadge))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = DashboardDimens.spaceSm, vertical = DashboardDimens.spaceXxs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
    }
}

// ─── Empty state ─────────────────────────────────────────────────────────────

@Composable
private fun TransactionsEmptyState(
    hasFilter: Boolean,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (hasFilter) "No results found" else "No transactions yet",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(DashboardDimens.spaceSm))
        Text(
            text = if (hasFilter) "Try adjusting your search or filters" else "Your transactions will appear here once you add an expense",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        if (hasFilter) {
            Spacer(Modifier.height(DashboardDimens.spaceLg))
            OutlinedButton(
                onClick = onClearAll,
                shape = RoundedCornerShape(DashboardDimens.cornerCard),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = SolidColor(MaterialTheme.colorScheme.primary)
                ),
            ) {
                Text("Clear all filters", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ─── Preview ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "TransactionScreen Preview")
@Composable
fun TransactionsScreenPreview() {
    var query by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf(TransactionPeriod.MONTH) }
    var showSheet by remember { mutableStateOf(false) }
    var selectedCat by remember { mutableStateOf<String?>(null) }

    MaterialTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { TransactionTopBar() },
        ) { inner ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner).padding(top = DashboardDimens.heroZonePadTop),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                item {
                    PeriodTabRow(
                        selected = selectedPeriod,
                        onSelect = { selectedPeriod = it },
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = DashboardDimens.screenPaddingH, end = DashboardDimens.screenPaddingH),
                    )
                }
                item { Spacer(Modifier.height(DashboardDimens.spaceMd)) }
                item {
                    SearchAndFilterRow(
                        query = query,
                        onQueryChange = { query = it },
                        onClearSearch = { query = "" },
                        activeFilterCount = if (selectedCat != null) 1 else 0,
                        onFilterClick = { showSheet = true },
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = DashboardDimens.screenPaddingH, end = DashboardDimens.screenPaddingH),
                    )
                }
                item {
                    AnimatedVisibility(visible = selectedCat != null) {
                        ActiveFilterChipsRow(
                            selectedCategory = selectedCat,
                            selectedPayment = null,
                            onClearCategory = { selectedCat = null },
                            onClearPayment = {},
                            modifier = Modifier.fillMaxWidth().padding(
                                horizontal = DashboardDimens.screenPaddingH,
                                vertical = DashboardDimens.spaceMd
                            ),
                        )
                    }
                }
                item { Spacer(Modifier.height(DashboardDimens.spaceMd)) }
                sampleMonthGroups.forEach { mg ->
                    item {
                        MonthGroupHeader(
                            monthLabel = mg.monthLabel,
                            totalSpent = mg.totalSpent,
                            expanded = false,
                            onToggle = {},
                            modifier = Modifier.fillMaxWidth()
                                .padding(start = DashboardDimens.screenPaddingH, end = DashboardDimens.screenPaddingH)
                                .padding(bottom = 8.dp),
                        )
                    }
                    mg.days.forEach { dg ->
                        item {
                            Text(
                                dg.dayLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.padding(
                                    start = DashboardDimens.screenPaddingH,
                                    end = DashboardDimens.screenPaddingH,
                                    top = DashboardDimens.spaceXs,
                                    bottom = DashboardDimens.spaceXs

                                ),
                            )
                        }
                        items(dg.items) { tx ->
                            TransactionRow(
                                tx = tx,
                                query = query,
                                modifier = Modifier.fillMaxWidth().padding(
                                    start = DashboardDimens.screenPaddingH,
                                    end = DashboardDimens.screenPaddingH
                                )
                            )
                            HorizontalDivider(
                                Modifier,
                                DividerDefaults.Thickness,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        }
                    }
                }
            }
        }
    }
}