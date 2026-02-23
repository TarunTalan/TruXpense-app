package com.example.truxpense.presentation.screens.dashboard.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.home.HomeTransactionItem
import com.example.truxpense.presentation.screens.dashboard.theme.DashboardDimens

// ─── Local colour tokens ──────────────────────────────────────────────────────

private val ColorTeal = Color(0xFF4DB6B6)
private val ColorAmountRed = Color(0xFFE53935)
private val ColorSubtext = Color(0xFF8A95A3)
private val ColorDivider = Color(0xFFEEF0F4)
private val ColorSearchBg = Color(0xFFF4F5F7)
private val ColorChipBorder = Color(0xFFDDE2E8)
private val ColorTabIndicator = ColorTeal
private val ColorBadgeBg = Color(0xFFF0F2F5)
private val ColorFilterIcon = Color(0xFF5B6470)

// Helper: map known category names to drawable resources
private fun categoryIconRes(category: String): Int = when (category.lowercase()) {
    "food" -> R.drawable.food
    "groceries" -> R.drawable.groceries
    "transport" -> R.drawable.transport
    "entertainment" -> R.drawable.entertainment
    "shopping" -> R.drawable.shopping
    "bills" -> R.drawable.bills
    "health" -> R.drawable.health
    else -> R.drawable.category_icon
}

/**
 * Transactions screen — pure UI layer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onBack: () -> Unit = {},
    vm: TransactionsViewModel = hiltViewModel(),
    recentHome: List<HomeTransactionItem>? = null,
) {
    // Local dialog state to show bottom-like dialog
    var catDialogOpen by remember { mutableStateOf(false) }

    // If recentHome is provided (called from Dashboard/Home), build monthGroups from it
    val monthGroupsLocal: List<TransactionMonthGroup>? = recentHome?.let { list ->
        val items = list.map { h ->
            TransactionItem(
                id = h.id,
                merchant = h.title,
                category = h.category,
                timeLabel = "Now",
                amount = -h.amount, // TransactionItem expects negative for expense
                paymentMethod = ""
            )
        }
        val day = TransactionDayGroup(dayLabel = "Today", items = items)
        val total = list.sumOf { it.amount }
        listOf(TransactionMonthGroup(monthLabel = "Recent", totalSpent = total, days = listOf(day)))
    }

    // If recentHome provided, use local placeholders for controls; otherwise read from vm
    val selectedPeriod by if (recentHome == null) vm.selectedPeriod.collectAsState() else remember { mutableStateOf(TransactionPeriod.MONTH) }
    val periodNavLabel by if (recentHome == null) vm.periodNavLabel.collectAsState() else remember { mutableStateOf("This month") }
    val canNavBack by if (recentHome == null) vm.canNavBack.collectAsState() else remember { mutableStateOf(false) }
    val canNavForward by if (recentHome == null) vm.canNavForward.collectAsState() else remember { mutableStateOf(false) }
    val searchQuery by if (recentHome == null) vm.searchQuery.collectAsState() else remember { mutableStateOf("") }
    val activeFilters by if (recentHome == null) vm.activeFilters.collectAsState() else remember { mutableStateOf(emptySet<TransactionFilter>()) }
    val monthGroupsVal by if (recentHome == null) vm.monthGroups.collectAsState() else remember(recentHome) { mutableStateOf(monthGroupsLocal ?: emptyList()) }
    val totalExpanded by if (recentHome == null) vm.totalExpanded.collectAsState() else remember { mutableStateOf(false) }

    // Selected categories now live in ViewModel
    val selectedCategoriesSet by vm.selectedCategories.collectAsState()

    // Derived displayed groups after applying VM-selected categories filter (multi-select)
    val displayedMonthGroups by remember(monthGroupsVal, selectedCategoriesSet) {
        derivedStateOf {
            if (selectedCategoriesSet.isEmpty()) return@derivedStateOf monthGroupsVal
            monthGroupsVal.mapNotNull { mg ->
                val daysFiltered = mg.days.mapNotNull { dg ->
                    val itemsFiltered = dg.items.filter { it.category in selectedCategoriesSet }
                    if (itemsFiltered.isEmpty()) null else dg.copy(items = itemsFiltered)
                }
                if (daysFiltered.isEmpty()) null else mg.copy(days = daysFiltered)
            }
        }
    }

    // Main content (Scaffold) — the dialog below will overlay this when open
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = "Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(R.drawable.back_icon), contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->

        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding), contentPadding = PaddingValues(bottom = DashboardDimens.spaceXxl)) {

            // ── ① Period tab row ──────────────────────────────────────────
            item {
                PeriodTabRow(
                    selected = selectedPeriod,
                    onSelect = { if (recentHome == null) vm.selectPeriod(it) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH),
                )
            }

            // ── ② Date navigator ─────────────────────────────────────────
            item {
                DateNavigatorRow(
                    label = periodNavLabel,
                    canBack = canNavBack,
                    canForward = canNavForward,
                    onBack = { if (recentHome == null) vm.navBack() },
                    onForward = { if (recentHome == null) vm.navForward() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.spaceMd),
                )
            }

            item { Spacer(Modifier.height(DashboardDimens.spaceMd)) }

            // ── ③ Search bar ──────────────────────────────────────────────
            item {
                SearchBar(
                    query = searchQuery,
                    onChange = { if (recentHome == null) vm.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH),
                )
            }

            item { Spacer(Modifier.height(DashboardDimens.spaceLg)) }

            // ── ④ Filter row ──────────────────────────────────────────────
            item {
                FilterRow(
                    activeFilters = activeFilters,
                    onToggle = { if (recentHome == null) vm.toggleFilter(it) },
                    onCategoryClick = { catDialogOpen = true },
                    selectedCategoriesSet = selectedCategoriesSet,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH),
                )
            }

            // Show category and payment dropdowns (if data available). These are always visible
            // when we're in the real screen (not the `recentHome` preview) so the user can pick filters
            // without first toggling chips. Selecting a value will also enable the corresponding chip.
            item {
                if (recentHome == null) {
                    val availableCats by vm.availableCategories.collectAsState()
                    val availablePayments by vm.availablePaymentMethods.collectAsState()
                    val selectedCatSet by vm.selectedCategories.collectAsState()
                    val selectedPayment by vm.paymentMethod.collectAsState()

                    // Category dropdown (show only when categories exist)
                    if (availableCats.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH)) {
                            TextButton(onClick = { /*noop*/ }) {
                                Text(if (selectedCatSet.isEmpty()) "Category: All" else "Category: ${selectedCatSet.first()}${if (selectedCatSet.size>1) " +${selectedCatSet.size-1}" else ""}")
                                Spacer(Modifier.width(8.dp))
                                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                        }
                    }

                    // Payment method dropdown (show only when methods exist)
                    if (availablePayments.isNotEmpty()) {
                        var payExpanded by remember { mutableStateOf(false) }
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH)) {
                            TextButton(onClick = { payExpanded = !payExpanded }) {
                                Text(if (selectedPayment.isNullOrBlank()) "Payment: All" else "Payment: ${selectedPayment!!}")
                                Spacer(Modifier.width(8.dp))
                                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = payExpanded, onDismissRequest = { payExpanded = false }) {
                                DropdownMenuItem(text = { Text("All methods") }, onClick = {
                                    vm.setPaymentMethod(null)
                                    if (TransactionFilter.ACCOUNT in activeFilters) vm.toggleFilter(TransactionFilter.ACCOUNT)
                                    payExpanded = false
                                })
                                availablePayments.forEach { p ->
                                    DropdownMenuItem(text = { Text(p) }, onClick = {
                                        vm.setPaymentMethod(p)
                                        if (TransactionFilter.ACCOUNT !in activeFilters) vm.toggleFilter(TransactionFilter.ACCOUNT)
                                        payExpanded = false
                                    })
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(DashboardDimens.spaceXl)) }

            // If there are no grouped results, show the empty state inside the list (keeps controls visible)
            if (displayedMonthGroups.isEmpty()) {
                item {
                    TransactionsEmptyScreen(modifier = Modifier.fillMaxWidth())
                }
            } else {
                // ── ⑤ Month groups ────────────────────────────────────────────
                displayedMonthGroups.forEach { monthGroup ->

                    // Month header
                    item(key = "header_${'$'}{monthGroup.monthLabel}") {
                        MonthGroupHeader(
                            monthLabel = monthGroup.monthLabel,
                            totalSpent = monthGroup.totalSpent,
                            expanded = totalExpanded,
                            onToggle = { if (recentHome == null) vm.toggleTotalExpanded() },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH)
                                .padding(bottom = DashboardDimens.spaceMd),
                        )
                    }

                    // Day sub-groups
                    monthGroup.days.forEach { dayGroup ->

                        // Day label
                        item(key = "day_${'$'}{monthGroup.monthLabel}_${'$'}{dayGroup.dayLabel}") {
                            Text(
                                text = dayGroup.dayLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8A95A3),
                                modifier = Modifier.padding(
                                    horizontal = DashboardDimens.screenPaddingH,
                                    vertical = DashboardDimens.spaceXs,
                                ),
                            )
                        }

                        // Transaction rows with dividers
                        itemsIndexed(
                            items = dayGroup.items,
                            key = { index, tx -> "${monthGroup.monthLabel}_${dayGroup.dayLabel}_${tx.id}_$index" }
                        ) { _, tx ->
                            TransactionRow(
                                tx = tx,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH),
                            )
                            HorizontalDivider(
                                thickness = DashboardDimens.dividerThin,
                                color = ColorDivider,
                            )
                        }

                        item { Spacer(Modifier.height(DashboardDimens.spaceMd)) }
                    }
                }
            }
        }
    }

    // Dialog acting as bottom sheet
    if (catDialogOpen) {
        Dialog(onDismissRequest = { catDialogOpen = false }) {
            Surface(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Select categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { vm.clearSelectedCategories() }) { Text(text = "Clear all") }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    val availableCats by vm.availableCategories.collectAsState()
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (availableCats.isEmpty()) {
                            Text("No categories", modifier = Modifier.padding(16.dp))
                        } else {
                            availableCats.forEach { cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { vm.toggleCategorySelection(cat) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(painter = painterResource(id = categoryIconRes(cat)), contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(text = cat, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Checkbox(checked = cat in selectedCategoriesSet, onCheckedChange = { vm.toggleCategorySelection(cat) })
                                }
                                HorizontalDivider()
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { catDialogOpen = false }) { Text("Apply") }
                    }
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
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { onSelect(period) },
                ).padding(
                    horizontal = DashboardDimens.spaceXl,
                    vertical = DashboardDimens.spaceMd,
                ),
            ) {
                Text(
                    text = period.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) ColorTeal else ColorSubtext,
                )
                // Teal underline indicator for selected tab
                Spacer(Modifier.height(DashboardDimens.spaceXs))
                Box(
                    modifier = Modifier.width(DashboardDimens.spaceXl + DashboardDimens.spaceXl)  // ~32dp
                        .height(if (isSelected) 2.dp else 0.dp).background(
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = onBack,
            enabled = canBack,
            modifier = Modifier.size(DashboardDimens.iconButtonSm + DashboardDimens.spaceMd),
        ) {
            Icon(
                painter = painterResource(R.drawable.left_arrow),
                contentDescription = "Previous period",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (canBack) 1f else 0.35f),
                modifier = Modifier.size(DashboardDimens.iconSm),
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        IconButton(
            onClick = onForward,
            enabled = canForward,
            modifier = Modifier.size(DashboardDimens.iconButtonSm + DashboardDimens.spaceMd),
        ) {
            Icon(
                painter = painterResource(R.drawable.right_arrow),
                contentDescription = "Next period",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (canForward) 1f else 0.35f),
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
        value = query,
        onValueChange = onChange,
        modifier = modifier.clip(RoundedCornerShape(DashboardDimens.cornerChip)).background(ColorSearchBg).padding(
            horizontal = DashboardDimens.spaceLg,
            vertical = DashboardDimens.spaceMdL,
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onBackground,
        ),
        cursorBrush = SolidColor(ColorTeal),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = ColorSubtext,
                    modifier = Modifier.size(DashboardDimens.iconMd),
                )
                Spacer(Modifier.width(DashboardDimens.spaceMd))
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search transactions…",
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

// ─── ④ Filter Row ───────────────────────────────────────────────────────────

@Composable
private fun FilterRow(
    activeFilters: Set<TransactionFilter>,
    onToggle: (TransactionFilter) -> Unit,
    onCategoryClick: () -> Unit = {},
    selectedCategoriesSet: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
    ) {
        // "⊞ Filter" label
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.filter),
                contentDescription = null,
                tint = ColorFilterIcon,
                modifier = Modifier.size(DashboardDimens.iconSm),
            )
            Spacer(Modifier.width(DashboardDimens.spaceXs))
            Text(
                text = "Filter",
                style = MaterialTheme.typography.bodySmall,
                color = ColorFilterIcon,
            )
        }

        // Filter chips
        TransactionFilter.entries.forEach { filter ->
            val isActive = if (filter == TransactionFilter.CATEGORY) selectedCategoriesSet.isNotEmpty() else filter in activeFilters
            FilterChip(
                selected = isActive,
                onClick = {
                    if (filter == TransactionFilter.CATEGORY) onCategoryClick() else onToggle(filter)
                },
                label = {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isActive) ColorTeal else MaterialTheme.colorScheme.onBackground,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    selectedContainerColor = ColorTeal.copy(alpha = 0.08f),
                    labelColor = MaterialTheme.colorScheme.onBackground,
                    selectedLabelColor = ColorTeal,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isActive,
                    borderColor = ColorChipBorder,
                    selectedBorderColor = ColorTeal,
                    borderWidth = 1.dp,
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

        // Total + dropdown chevron — tappable
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
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = ColorTeal,
                modifier = Modifier.size(DashboardDimens.iconMd),
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
                text = tx.merchant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(DashboardDimens.spaceXxs))
            Text(
                text = "${tx.category}.${tx.timeLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = ColorSubtext,
            )
        }

        // Right — amount + payment badge
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = ColorAmountRed,
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
        modifier = Modifier.clip(RoundedCornerShape(DashboardDimens.cornerBadge)).background(ColorBadgeBg).padding(
            horizontal = DashboardDimens.spaceSm,
            vertical = DashboardDimens.spaceXxs,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ColorSubtext,
        )
    }
}

// ─── Preview ─────────────────────────────────────────────────────────────────


@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Transactions - Static Preview")
@Composable
fun TransactionsScreenStaticPreview() {
    // Local, preview-only state (no ViewModel)
    var selectedPeriod by remember { mutableStateOf(TransactionPeriod.MONTH) }
    var searchQuery by remember { mutableStateOf("") }
    var totalExpanded by remember { mutableStateOf(false) }
    var activeFilters by remember { mutableStateOf(emptySet<TransactionFilter>()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = DashboardDimens.spaceXxl),
        ) {
            item {
                PeriodTabRow(
                    selected = selectedPeriod,
                    onSelect = { selectedPeriod = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH),
                )
            }

            item {
                DateNavigatorRow(
                    label = "This month",
                    canBack = true,
                    canForward = false,
                    onBack = { /*noop*/ },
                    onForward = { /*noop*/ },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.spaceMd),
                )
            }

            item { Spacer(Modifier.height(DashboardDimens.spaceMd)) }

            item {
                SearchBar(
                    query = searchQuery,
                    onChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH),
                )
            }

            item { Spacer(Modifier.height(DashboardDimens.spaceLg)) }

            item {
                FilterRow(
                    activeFilters = activeFilters,
                    onToggle = { f ->
                        activeFilters = if (f in activeFilters) activeFilters - f else activeFilters + f
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH),
                )
            }

            item { Spacer(Modifier.height(DashboardDimens.spaceXl)) }

            sampleMonthGroups.forEach { monthGroup ->
                item {
                    MonthGroupHeader(
                        monthLabel = monthGroup.monthLabel,
                        totalSpent = monthGroup.totalSpent,
                        expanded = totalExpanded,
                        onToggle = { totalExpanded = !totalExpanded },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH)
                            .padding(bottom = DashboardDimens.spaceMd),
                    )
                }

                monthGroup.days.forEach { dayGroup ->
                    item {
                        Text(
                            text = dayGroup.dayLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8A95A3),
                            modifier = Modifier.padding(
                                horizontal = DashboardDimens.screenPaddingH, vertical = DashboardDimens.spaceXs
                            ),
                        )
                    }

                    items(dayGroup.items) { tx ->
                        TransactionRow(
                            tx = tx,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH)
                        )
                        HorizontalDivider(thickness = DashboardDimens.dividerThin, color = Color(0xFFEEF0F4))
                    }

                    item { Spacer(Modifier.height(DashboardDimens.spaceMd)) }
                }
            }
        }
    }
}
