package com.example.truxpense.presentation.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.truxpense.presentation.theme.DashboardDimens
import java.util.*
import com.example.truxpense.presentation.screens.dashboard.transaction.EntryType

// ── Shared month labels ───────────────────────────────────────────────────────

val FILTER_MONTH_LABELS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

fun formatFilterDateChip(ts: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = ts }
    val d = cal.get(Calendar.DAY_OF_MONTH)
    val m = FILTER_MONTH_LABELS[cal.get(Calendar.MONTH)]
    return "$d $m"
}

// ── Reusable Filter Bottom Sheet ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    sheetState: SheetState,
    availableCategories: List<String>,
    availablePayments: List<String>,
    availableYears: List<Int>,
    selectedCategory: String?,
    selectedPayment: String?,
    selectedMonth: Int?,
    selectedYear: Int?,
    selectedType: EntryType?,
    onSelectType: (EntryType?) -> Unit,
    dateFrom: Long?,
    dateTo: Long?,
    onSelectCategory: (String?) -> Unit,
    onSelectPayment: (String?) -> Unit,
    onSelectMonth: (Int?) -> Unit,
    onSelectYear: (Int?) -> Unit,
    onDateFromChange: (Long?) -> Unit,
    onDateToChange: (Long?) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
    /** Override the sheet title — default "Filter" */
    title: String = "Filter",
    /** Hide payment method section when not applicable (e.g. Analytics) */
    showPaymentFilter: Boolean = true,
) {
    val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val sheetHandleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val errorColor = MaterialTheme.colorScheme.error

    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    val fromPickerState = rememberDatePickerState(initialSelectedDateMillis = dateFrom)
    val toPickerState = rememberDatePickerState(initialSelectedDateMillis = dateTo)

    if (showFromPicker) {
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateFromChange(fromPickerState.selectedDateMillis)
                    showFromPicker = false
                }) { Text("OK", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showFromPicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        ) { DatePicker(state = fromPickerState) }
    }

    if (showToPicker) {
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val endOfDay = toPickerState.selectedDateMillis?.let { it + 86_399_999L }
                    onDateToChange(endOfDay)
                    showToPicker = false
                }) { Text("OK", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showToPicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        ) { DatePicker(state = toPickerState) }
    }

    val hasAnyFilter =
        selectedCategory != null || selectedPayment != null || selectedMonth != null || selectedYear != null || selectedType != null || dateFrom != null || dateTo != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = {
            Box(
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).width(36.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(sheetHandleColor),
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 24.dp),
        ) {
            // ── Header ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH)
                    .padding(bottom = DashboardDimens.spaceLg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (hasAnyFilter) {
                    TextButton(onClick = onClearAll) {
                        Text(
                            text = "Clear all",
                            color = errorColor,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            // ── Type (Expense/Income) ─────────────────────────────────────────
            FilterSectionLabel(
                text = "Type",
                modifier = Modifier.padding(horizontal = DashboardDimens.screenPaddingH),
            )
            Spacer(Modifier.height(DashboardDimens.spaceMd))
            LazyRow(
                contentPadding = PaddingValues(horizontal = DashboardDimens.screenPaddingH),
                horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceSm),
            ) {
                item {
                    SheetFilterChip(label = "All", isSelected = selectedType == null, onClick = { onSelectType(null) })
                }
                item {
                    SheetFilterChip(
                        label = "Expense",
                        isSelected = selectedType == EntryType.EXPENSE,
                        onClick = { onSelectType(if (selectedType == EntryType.EXPENSE) null else EntryType.EXPENSE) },
                    )
                }
                item {
                    SheetFilterChip(
                        label = "Income",
                        isSelected = selectedType == EntryType.INCOME,
                        onClick = { onSelectType(if (selectedType == EntryType.INCOME) null else EntryType.INCOME) },
                    )
                }
            }
            Spacer(Modifier.height(DashboardDimens.spaceXl))

            HorizontalDivider(color = dividerColor)
            Spacer(Modifier.height(DashboardDimens.spaceLg))

            // ── Category ─────────────────────────────────────────────────────
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
                    item {
                        SheetFilterChip(
                            label = "All", isSelected = selectedCategory == null, onClick = { onSelectCategory(null) })
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

            // ── Payment method ────────────────────────────────────────────────
            if (showPaymentFilter && availablePayments.isNotEmpty()) {
                FilterSectionLabel(
                    text = "Payment method",
                    modifier = Modifier.padding(horizontal = DashboardDimens.screenPaddingH),
                )
                Spacer(Modifier.height(DashboardDimens.spaceMd))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = DashboardDimens.screenPaddingH),
                    horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceSm),
                ) {
                    item {
                        SheetFilterChip(
                            label = "All", isSelected = selectedPayment == null, onClick = { onSelectPayment(null) })
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

            HorizontalDivider(color = dividerColor)
            Spacer(Modifier.height(DashboardDimens.spaceLg))

            // ── Year ─────────────────────────────────────────────────────────
            if (availableYears.isNotEmpty()) {
                FilterSectionLabel(
                    text = "Year",
                    modifier = Modifier.padding(horizontal = DashboardDimens.screenPaddingH),
                )
                Spacer(Modifier.height(DashboardDimens.spaceMd))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = DashboardDimens.screenPaddingH),
                    horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceSm),
                ) {
                    item {
                        SheetFilterChip(
                            label = "All", isSelected = selectedYear == null, onClick = { onSelectYear(null) })
                    }
                    items(availableYears) { year ->
                        SheetFilterChip(
                            label = year.toString(),
                            isSelected = selectedYear == year,
                            onClick = { onSelectYear(if (selectedYear == year) null else year) },
                        )
                    }
                }
                Spacer(Modifier.height(DashboardDimens.spaceXl))
            }

            // ── Month ─────────────────────────────────────────────────────────
            FilterSectionLabel(
                text = "Month",
                modifier = Modifier.padding(horizontal = DashboardDimens.screenPaddingH),
            )
            Spacer(Modifier.height(DashboardDimens.spaceMd))
            LazyRow(
                contentPadding = PaddingValues(horizontal = DashboardDimens.screenPaddingH),
                horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceSm),
            ) {
                item {
                    SheetFilterChip(
                        label = "All", isSelected = selectedMonth == null, onClick = { onSelectMonth(null) })
                }
                items(12) { idx ->
                    val monthNum = idx + 1
                    SheetFilterChip(
                        label = FILTER_MONTH_LABELS[idx],
                        isSelected = selectedMonth == monthNum,
                        onClick = { onSelectMonth(if (selectedMonth == monthNum) null else monthNum) },
                    )
                }
            }

            Spacer(Modifier.height(DashboardDimens.spaceXl))
            HorizontalDivider(color = dividerColor)
            Spacer(Modifier.height(DashboardDimens.spaceLg))

            // ── Date range ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterSectionLabel(text = "Date range")
                if (dateFrom != null || dateTo != null) {
                    TextButton(
                        onClick = { onDateFromChange(null); onDateToChange(null) },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(text = "Clear", color = errorColor, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(DashboardDimens.spaceMd))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH),
                horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
            ) {
                FilterDateRangeButton(
                    label = "From",
                    dateText = dateFrom?.let { formatFilterDateChip(it) } ?: "Any date",
                    isSet = dateFrom != null,
                    onClick = { showFromPicker = true },
                    modifier = Modifier.weight(1f),
                )
                FilterDateRangeButton(
                    label = "To",
                    dateText = dateTo?.let { formatFilterDateChip(it) } ?: "Any date",
                    isSet = dateTo != null,
                    onClick = { showToPicker = true },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(DashboardDimens.spaceXl))

            // ── Done button ───────────────────────────────────────────────────
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = DashboardDimens.screenPaddingH),
                shape = RoundedCornerShape(DashboardDimens.cornerCard),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    text = "Done",
                    color = MaterialTheme.colorScheme.background,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ── Active Filter Chips Row ───────────────────────────────────────────────────

@Composable
fun ActiveFilterChipsRow(
    selectedCategory: String?,
    selectedPayment: String?,
    selectedMonth: Int?,
    selectedYear: Int?,
    dateFrom: Long?,
    dateTo: Long?,
    onClearCategory: () -> Unit,
    onClearPayment: () -> Unit,
    onClearMonth: () -> Unit,
    onClearYear: () -> Unit,
    onClearDateRange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceSm),
    ) {
        if (selectedCategory != null) {
            item { ActiveFilterChip(label = selectedCategory, prefix = "Category", onClear = onClearCategory) }
        }
        if (selectedPayment != null) {
            item { ActiveFilterChip(label = selectedPayment, prefix = "via", onClear = onClearPayment) }
        }
        if (selectedYear != null) {
            item { ActiveFilterChip(label = selectedYear.toString(), prefix = "Year", onClear = onClearYear) }
        }
        if (selectedMonth != null) {
            item {
                ActiveFilterChip(
                    label = FILTER_MONTH_LABELS[selectedMonth - 1],
                    prefix = "Month",
                    onClear = onClearMonth,
                )
            }
        }
        if (dateFrom != null || dateTo != null) {
            item {
                val label = when {
                    dateFrom != null && dateTo != null -> "${formatFilterDateChip(dateFrom)} – ${
                        formatFilterDateChip(
                            dateTo
                        )
                    }"

                    dateFrom != null -> "From ${formatFilterDateChip(dateFrom)}"
                    else -> "Until ${formatFilterDateChip(dateTo!!)}"
                }
                ActiveFilterChip(label = label, prefix = "Date", onClear = onClearDateRange)
            }
        }
    }
}

@Composable
fun ActiveFilterChip(label: String, prefix: String, onClear: () -> Unit) {
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
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier.size(16.dp).clip(CircleShape).background(primary.copy(alpha = 0.18f))
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

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
fun FilterSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier,
    )
}

@Composable
fun SheetFilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceContainer,
        ).clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick,
        ).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.background,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
fun FilterDateRangeButton(
    label: String,
    dateText: String,
    isSet: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier.clip(RoundedCornerShape(DashboardDimens.cornerCard))
            .background(if (isSet) primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainer)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ).padding(horizontal = DashboardDimens.spaceMd, vertical = DashboardDimens.spaceMdL),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSet) primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceXs),
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = if (isSet) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DashboardDimens.iconSm),
            )
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSet) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSet) primary else MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "FilterBottomSheet Preview")
@Composable
fun FilterBottomSheetPreview() {
    MaterialTheme {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        FilterBottomSheet(
            sheetState = sheetState,
            availableCategories = listOf("Food", "Transport", "Shopping", "Bills"),
            availablePayments = listOf("UPI", "Cash", "Card"),
            availableYears = listOf(2024, 2025, 2026),
            selectedCategory = "Food",
            selectedPayment = null,
            selectedMonth = 3,
            selectedYear = 2026,
            selectedType = null,
            onSelectType = {},
            dateFrom = null,
            dateTo = null,
            onSelectCategory = {},
            onSelectPayment = {},
            onSelectMonth = {},
            onSelectYear = {},
            onDateFromChange = {},
            onDateToChange = {},
            onClearAll = {},
            onDismiss = {},
            title = "Filter",
        )
    }
}
