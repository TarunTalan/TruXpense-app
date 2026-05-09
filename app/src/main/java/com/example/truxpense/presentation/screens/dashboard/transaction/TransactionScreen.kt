package com.example.truxpense.presentation.screens.dashboard.transaction

import android.graphics.Typeface
import android.view.Gravity
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.ActiveFilterChipsRow
import com.example.truxpense.presentation.screens.dashboard.components.FilterBottomSheet
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.utils.clearFocusOnTap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    // optional backStackEntry passed from the NavHost so we can read SavedStateHandle keys reliably
    navBackStackEntry: NavBackStackEntry? = null,
    onTransactionClick: (transactionId: String) -> Unit = {},
    onAddTransaction: () -> Unit = {},
    vm: TransactionsViewModel = hiltViewModel(),
) {
    // If a preselectCategory was set on the backStackEntry's SavedStateHandle by the navigator,
    // apply it here directly to the ViewModel. This is robust and avoids timing issues.
    LaunchedEffect(navBackStackEntry) {
        val pre = navBackStackEntry?.savedStateHandle?.get<String>("preselectCategory")
        if (!pre.isNullOrBlank()) {
            vm.setCategory(pre.trim())
            navBackStackEntry.savedStateHandle.remove<String>("preselectCategory")
        }
    }

    val searchQuery by vm.searchQuery.collectAsState()
    val selectedCategory by vm.selectedCategory.collectAsState()
    val selectedPayment by vm.paymentMethod.collectAsState()
    val selectedMonth by vm.selectedMonth.collectAsState()
    val selectedYear by vm.selectedYear.collectAsState()
    val dateFrom by vm.dateFrom.collectAsState()
    val dateTo by vm.dateTo.collectAsState()
    val availableCats by vm.availableCategories.collectAsState()
    val availablePayments by vm.availablePaymentMethods.collectAsState()
    val availableYears by vm.availableYears.collectAsState()
    val monthGroupsVal by vm.monthGroups.collectAsState()
    val activeFilterCount by vm.activeFilterCount.collectAsState()
    val isLoaded by vm.isLoaded.collectAsState()
    val typeFilter by vm.typeFilter.collectAsState()

    // Per-group toggle state — absent key = true (all start expanded)
    val monthExpandedStates = remember { mutableStateMapOf<String, Boolean>() }
    val dayExpandedStates = remember { mutableStateMapOf<String, Boolean>() }

    // ── Bottom-sheet state ────────────────────────────────────────────────────
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFilterSheet by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // True when the empty list is caused by an active filter/search (not because there are no txns at all)
    val hasActiveFiltersOrSearch = activeFilterCount > 0 || searchQuery.isNotBlank()

    AnimatedVisibility(
        visible = isLoaded,
        enter = fadeIn(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)),
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                ScreenTopBar(
                    headerTitle = "Transaction", showBack = false
                )
            },
        ) { innerPadding ->

            if (monthGroupsVal.isEmpty() && !hasActiveFiltersOrSearch) {
                // Truly no transactions at all — show the full onboarding empty screen
                TransactionsEmptyContent(
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                        .padding(horizontal = DashboardDimens.screenPaddingH),
                    onAddTransaction = onAddTransaction,
                )
            } else if (monthGroupsVal.isEmpty()) {
                // Filters/search active but produced zero results — keep search bar visible
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                        .padding(top = DashboardDimens.spaceLg)
                        .clearFocusOnTap(),
                    contentPadding = PaddingValues(bottom = DashboardDimens.spaceXxl),
                ) {
                    stickyHeader {
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
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
                                        .padding(horizontal = DashboardDimens.screenPaddingH),
                                )
                                AnimatedVisibility(
                                    visible = selectedCategory != null || selectedPayment != null || selectedMonth != null || selectedYear != null || dateFrom != null || dateTo != null,
                                    enter = expandVertically(),
                                    exit = shrinkVertically(),
                                ) {
                                    ActiveFilterChipsRow(
                                        selectedCategory = selectedCategory,
                                        selectedPayment = selectedPayment,
                                        selectedMonth = selectedMonth,
                                        selectedYear = selectedYear,
                                        dateFrom = dateFrom,
                                        dateTo = dateTo,
                                        onClearCategory = { vm.setCategory(null) },
                                        onClearPayment = { vm.setPaymentMethod(null) },
                                        onClearMonth = { vm.setMonth(null) },
                                        onClearYear = { vm.setYear(null) },
                                        onClearDateRange = { vm.clearDateRange() },
                                        modifier = Modifier.fillMaxWidth().padding(
                                            start = DashboardDimens.screenPaddingH,
                                            end = DashboardDimens.screenPaddingH,
                                            top = DashboardDimens.spaceMd,
                                        ),
                                    )
                                }
                                Spacer(Modifier.height(DashboardDimens.spaceMd))
                            }
                        }
                    }

                    item {
                        TransactionsEmptyState(
                            hasFilter = true,
                            onClearAll = {
                                vm.clearSearch()
                                vm.setCategory(null)
                                vm.setPaymentMethod(null)
                                vm.setMonth(null)
                                vm.setYear(null)
                                vm.clearDateRange()
                                vm.setTypeFilter(null)
                                focusManager.clearFocus()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp)
                                .padding(horizontal = DashboardDimens.screenPaddingH),
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                        .padding(top = DashboardDimens.spaceLg)
                        .clearFocusOnTap(),
                    contentPadding = PaddingValues(bottom = DashboardDimens.spaceXxl),
                ) {

                    // ── ① Search bar + Active filter chips (sticky) ───────────────────
                    stickyHeader {
                        // Keep same background as scaffold so the header appears seamless
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
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
                                        .padding(horizontal = DashboardDimens.screenPaddingH),
                                )

                                AnimatedVisibility(
                                    visible = selectedCategory != null || selectedPayment != null || selectedMonth != null || selectedYear != null || dateFrom != null || dateTo != null,
                                    enter = expandVertically(),
                                    exit = shrinkVertically(),
                                ) {
                                    ActiveFilterChipsRow(
                                        selectedCategory = selectedCategory,
                                        selectedPayment = selectedPayment,
                                        selectedMonth = selectedMonth,
                                        selectedYear = selectedYear,
                                        dateFrom = dateFrom,
                                        dateTo = dateTo,
                                        onClearCategory = { vm.setCategory(null) },
                                        onClearPayment = { vm.setPaymentMethod(null) },
                                        onClearMonth = { vm.setMonth(null) },
                                        onClearYear = { vm.setYear(null) },
                                        onClearDateRange = { vm.clearDateRange() },
                                        modifier = Modifier.fillMaxWidth().padding(
                                            start = DashboardDimens.screenPaddingH,
                                            end = DashboardDimens.screenPaddingH,
                                            top = DashboardDimens.spaceMd,
                                        ),
                                    )
                                }

                                Spacer(Modifier.height(DashboardDimens.spaceMd))
                            }
                        }
                    }

                    item { Spacer(Modifier.height(DashboardDimens.spaceXxl)) }

                    // ── ④ Month groups ────────────────────────────────────────────────
                    monthGroupsVal.forEachIndexed { monthIndex, monthGroup ->
                        val headerKey = "month-$monthIndex"
                        val isMonthExpanded = monthExpandedStates[headerKey] ?: true

                        item(key = headerKey) {
                            MonthGroupHeader(
                                monthLabel = monthGroup.monthLabel,
                                totalExpense = monthGroup.totalExpense,
                                totalIncome = monthGroup.totalIncome,
                                expanded = isMonthExpanded,
                                onToggle = {
                                    monthExpandedStates[headerKey] = !isMonthExpanded
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH)
                                    .padding(top = DashboardDimens.spaceMd, bottom = DashboardDimens.spaceXs),
                            )
                        }

                        if (isMonthExpanded) {
                            monthGroup.days.forEachIndexed { dayIndex, dayGroup ->
                                val dayKey = "day-$monthIndex-$dayIndex"
                                val isDayExpanded = dayExpandedStates[dayKey] ?: true

                                item(key = dayKey) {
                                    DayGroupHeader(
                                        dayLabel = dayGroup.dayLabel,
                                        expanded = isDayExpanded,
                                        onToggle = {
                                            dayExpandedStates[dayKey] = !isDayExpanded
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(horizontal = DashboardDimens.screenPaddingH).padding(
                                                top = DashboardDimens.spaceLg,
                                                bottom = DashboardDimens.spaceXs,
                                            ),
                                    )
                                }

                                // Emit each transaction as its own lazy item.
                                // Key is prefixed with dayKey so it stays globally unique
                                // across all day-groups even if the same id somehow appeared twice.
                                if (isDayExpanded) {
                                    itemsIndexed(
                                        items = dayGroup.items,
                                        key = { _, tx -> "$dayKey-${tx.id}" },
                                    ) { index, tx ->
                                        TransactionRow(
                                            tx = tx,
                                            query = searchQuery,
                                            modifier = Modifier.fillMaxWidth().clickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() },
                                            ) { onTransactionClick(tx.id) }
                                                .padding(horizontal = DashboardDimens.screenPaddingH),
                                        )
                                        // Only show divider if this is not the last item of the day
                                        if (index < dayGroup.items.lastIndex) {
                                            HorizontalDivider(
                                                thickness = DashboardDimens.dividerThin,
                                                modifier = Modifier.padding(horizontal = DashboardDimens.screenPaddingH),
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                                            )
                                        }
                                    }
                                }
                            }

                            item(key = "foot-$monthIndex") {
                                Spacer(Modifier.height(DashboardDimens.spaceLg))
                            }
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
                    availableYears = availableYears,
                    selectedCategory = selectedCategory,
                    selectedPayment = selectedPayment,
                    selectedMonth = selectedMonth,
                    selectedYear = selectedYear,
                    selectedType = typeFilter,
                    onSelectType = { vm.setTypeFilter(it) },
                    dateFrom = dateFrom,
                    dateTo = dateTo,
                    onSelectCategory = { vm.setCategory(it) },
                    onSelectPayment = { vm.setPaymentMethod(it) },
                    onSelectMonth = { vm.setMonth(it) },
                    onSelectYear = { vm.setYear(it) },
                    onDateFromChange = { vm.setDateFrom(it) },
                    onDateToChange = { vm.setDateTo(it) },
                    onClearAll = {
                        vm.setCategory(null)
                        vm.setPaymentMethod(null)
                        vm.setMonth(null)
                        vm.setYear(null)
                        vm.setTypeFilter(null)
                        vm.clearDateRange()
                    },
                    onDismiss = { showFilterSheet = false },
                )
            }
        }
    } // end AnimatedVisibility
}

// ─── Top bar ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
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
        modifier = modifier.height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
    ) {
        // Search field — takes remaining width
        SearchField(
            query = query,
            onChange = onQueryChange,
            onClear = onClearSearch,
            modifier = Modifier.weight(1f).height(48.dp),
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
        modifier = modifier.clip(RoundedCornerShape(DashboardDimens.cornerCard))
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
                            style = MaterialTheme.typography.labelMedium,
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
                            modifier = Modifier.size(16.dp),
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

    // Overlay container so we can precisely position the circular badge without Badge padding
    Box(modifier = modifier.size(width = 56.dp, height = 56.dp)) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(DashboardDimens.cornerCard))
                .background(if (activeCount > 0) primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Icon(
                painterResource(R.drawable.filter),
                contentDescription = "Filter",
                tint = if (activeCount > 0) primary else onSurfaceVar,
                modifier = Modifier.size(DashboardDimens.iconMd),
            )
        }

        if (activeCount > 0) {
            Badge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp),
                containerColor = primary,
            ) {
                AndroidView(
                    factory = { ctx ->
                        TextView(ctx).apply {
                            text = "$activeCount"
                            setTextColor(android.graphics.Color.WHITE)
                            textSize = 12f
                            typeface = Typeface.DEFAULT_BOLD
                            gravity = Gravity.CENTER
                            includeFontPadding = false
                        }
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ─── ④ Active Filter Chips / Filter Sheet — defined in components/FilterBottomSheet.kt ─────────

// ─── Month Group Header ───────────────────────────────────────────────────────

@Composable
private fun MonthGroupHeader(
    monthLabel: String,
    totalExpense: Double,
    totalIncome: Double,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val net = totalIncome - totalExpense
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "monthChevron",
    )

    Column(
        modifier = modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onToggle,
        ),
        verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceSm),
    ) {
        // ── Month label + animated chevron ────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(DashboardDimens.iconMd)
                    .graphicsLayer { rotationZ = chevronRotation },
            )
        }

        // ── Income / Expense / Net summary chips ──────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceSm),
        ) {
            PeriodStatChip(
                label = "Income",
                amount = totalIncome,
                positive = true,
                modifier = Modifier.weight(1f),
            )
            PeriodStatChip(
                label = "Expense",
                amount = totalExpense,
                positive = false,
                modifier = Modifier.weight(1f),
            )
            PeriodStatChip(
                label = "Net",
                amount = net,
                positive = net >= 0,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PeriodStatChip(
    label: String,
    amount: Double,
    positive: Boolean,
    modifier: Modifier = Modifier,
) {
    val tint = if (positive) Color(0xFF1BAF9D) else MaterialTheme.colorScheme.error
    val prefix = if (positive) "+" else "−"
    // Always format absolute value — prefix supplies the sign, avoiding double minus
    val absAmt = kotlin.math.abs(amount)
    val formatted = when {
        absAmt >= 1_00_00_000.0 -> "₹${"%.1f".format(absAmt / 1_00_00_000.0)}Cr"
        absAmt >= 1_00_000.0    -> "₹${"%.1f".format(absAmt / 1_00_000.0)}L"
        else                    -> "₹${"%,.0f".format(absAmt)}"
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        color = tint.copy(alpha = 0.08f),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = DashboardDimens.spaceMd,
                vertical = DashboardDimens.spaceSm,
            ),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$prefix$formatted",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = tint,
            )
        }
    }
}

// ─── Day Group Header ─────────────────────────────────────────────────────────

@Composable
private fun DayGroupHeader(
    dayLabel: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "dayChevron",
    )

    Row(
        modifier = modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onToggle,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = dayLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .size(DashboardDimens.iconSm)
                .graphicsLayer { rotationZ = chevronRotation },
        )
    }
}


@Composable
private fun TransactionRow(
    tx: TransactionItem,
    query: String,
    modifier: Modifier = Modifier,
) {
    val isIncome = tx.entryType == EntryType.INCOME
    val amountColor = if (isIncome) Color(0xFF1BAF9D) else MaterialTheme.colorScheme.error
    val avatarBg = if (isIncome)
        Color(0xFF1BAF9D).copy(alpha = 0.12f)
    else
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    val avatarFg = if (isIncome) Color(0xFF1BAF9D) else MaterialTheme.colorScheme.primary
    val prefix = if (isIncome) "+" else "−"
    val formattedAmount = "$prefix₹${"%,.0f".format(tx.amount)}"

    Row(
        modifier = modifier.padding(vertical = DashboardDimens.spaceLg, horizontal = DashboardDimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(avatarBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tx.merchant.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = avatarFg,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }

        Spacer(Modifier.width(DashboardDimens.spaceMd))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.merchant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.85f),
            )
            Spacer(Modifier.height(DashboardDimens.spaceXs))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tx.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
//                if (isIncome) {
//                    Text(
//                        text = "Income",
//                        style = MaterialTheme.typography.labelSmall,
//                        color = Color(0xFF1BAF9D),
//                        modifier = Modifier
//                            .background(Color(0xFF1BAF9D).copy(alpha = 0.10f), RoundedCornerShape(4.dp))
//                            .padding(horizontal = 4.dp, vertical = 1.dp),
//                    )
//                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = amountColor,
            )
            Spacer(Modifier.height(DashboardDimens.spaceXs))
            if (tx.paymentMethod.isNotBlank()) {
                PaymentBadge(label = tx.paymentMethod)
            }
        }
    }
}

@Composable
private fun PaymentBadge(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
    )
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
                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
            ) {
                Text("Clear all filters", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ─── Preview ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "TransactionsScreen Preview")
@Composable
fun TransactionsScreenPreview() {
    var query by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<String?>(null) }

    MaterialTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { TransactionTopBar() },
        ) { inner ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner).padding(top = DashboardDimens.spaceXxl),
                contentPadding = PaddingValues(bottom = DashboardDimens.fabClearance),
                verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
            ) {
                // Search + filter
                item {
                    SearchAndFilterRow(
                        query = query,
                        onQueryChange = { query = it },
                        onClearSearch = { query = "" },
                        activeFilterCount = if (selectedCat != null) 1 else 0,
                        onFilterClick = { /* no-op in preview */ },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH),
                    )
                }

                // Active filters
                item {
                    AnimatedVisibility(visible = selectedCat != null) {
                        ActiveFilterChipsRow(
                            selectedCategory = selectedCat,
                            selectedPayment = null,
                            selectedMonth = null,
                            selectedYear = null,
                            dateFrom = null,
                            dateTo = null,
                            onClearCategory = { selectedCat = null },
                            onClearPayment = {},
                            onClearMonth = {},
                            onClearYear = {},
                            onClearDateRange = {},
                            modifier = Modifier.fillMaxWidth().padding(
                                horizontal = DashboardDimens.screenPaddingH, vertical = DashboardDimens.spaceMd
                            ),
                        )
                    }
                }

                // Sample month groups + transactions
                sampleMonthGroups.forEach { mg ->
                    item {
                        MonthGroupHeader(
                            monthLabel = mg.monthLabel,
                            totalExpense = mg.totalExpense,
                            totalIncome = mg.totalIncome,
                            expanded = true,
                            onToggle = {},
                            modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH)
                                .padding(bottom = DashboardDimens.spaceSm),
                        )
                    }

                    mg.days.forEach { dg ->
                        item {
                            DayGroupHeader(
                                dayLabel = dg.dayLabel,
                                expanded = true,
                                onToggle = {},
                                modifier = Modifier.fillMaxWidth().padding(
                                    horizontal = DashboardDimens.screenPaddingH,
                                    vertical = DashboardDimens.spaceSm,
                                ),
                            )
                        }

                        itemsIndexed(dg.items) { index, tx ->
                            TransactionRow(
                                tx = tx,
                                query = query,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH),
                            )
                            if (index < dg.items.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }
    }
}