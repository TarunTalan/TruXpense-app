package com.example.truxpense.presentation.screens.dashboard.report

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.data.repository.report.ReportType
import com.example.truxpense.presentation.screens.dashboard.components.AppDatePickerDialog
import com.example.truxpense.presentation.screens.dashboard.components.GradientCard
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.theme.TruXpenseTheme
import com.example.truxpense.presentation.utils.SimpleTextField
import java.text.SimpleDateFormat
import java.util.*

// ─── Data ─────────────────────────────────────────────────────────────────────

// "Custom" is intentionally kept short so it fits in the pill.
// The five quick-select pills sit on one row; "Custom" gets its own row-end slot
// via the layout fix below.
private val timePeriods = listOf("7D", "1M", "3M", "6M", "1y", "Custom")

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportScreen(
    onBack: () -> Unit = {},
    onPreview: (reportId: String) -> Unit = {},
    vm: CreateReportViewModel = hiltViewModel(),
) {
    val primary = MaterialTheme.colorScheme.primary
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(uiState.savedReportId) {
        val id = uiState.savedReportId
        if (id != null) {
            vm.onNavigatedToDetail(); onPreview(id)
        }
    }

    fun applyPeriod(period: String) {
        val now = Calendar.getInstance()
        val from = Calendar.getInstance()
        when (period) {
            "7D" -> from.add(Calendar.DAY_OF_YEAR, -7)
            "1M" -> from.add(Calendar.MONTH, -1)
            "3M" -> from.add(Calendar.MONTH, -3)
            "6M" -> from.add(Calendar.MONTH, -6)
            "1y" -> from.add(Calendar.YEAR, -1)
            else -> return
        }
        vm.setFromDate(from.timeInMillis)
        vm.setToDate(now.timeInMillis)
    }

    var selectedPeriod by remember { mutableStateOf("1M") }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    val fromPickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.fromDate)
    val toPickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.toDate)

    fun epochToDisplay(ms: Long): String = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(ms))

    if (showFromPicker) {
        AppDatePickerDialog(
            state = fromPickerState,
            onDismiss = { showFromPicker = false },
            onConfirm = { ms -> ms?.let { vm.setFromDate(it) }; showFromPicker = false },
        )
    }
    if (showToPicker) {
        AppDatePickerDialog(
            state = toPickerState,
            onDismiss = { showToPicker = false },
            onConfirm = { ms -> ms?.let { vm.setToDate(it) }; showToPicker = false },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(headerTitle = "Create Report", showBack = true, onBack = onBack)
        },
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding().padding(horizontal = DashboardDimens.screenPaddingH, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { vm.suggestTitle(); vm.saveReport() },
                    enabled = uiState.isValid,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            "Preview Report",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                uiState.titleError?.let { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
                uiState.dateError?.let { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = DashboardDimens.screenPaddingH)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Report name ───────────────────────────────────────────────
            GradientCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Report name",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SimpleTextField(
                        value = uiState.title,
                        onValueChange = { vm.setTitle(it) },
                        placeholder = "e.g. Financial report",
                        modifier = Modifier.fillMaxWidth(),
                        bgColor = MaterialTheme.colorScheme.background,
                    )
                }
            }

            // ── Report type ───────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Report type")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    listOf(
                        ReportType.EXPENSE to "Expense",
                        ReportType.INCOME to "Income",
                        ReportType.ALL to "All",
                    ).forEach { (type, label) ->
                        val isSelected = uiState.reportType == type
                        PillChip(
                            label = label,
                            isSelected = isSelected,
                            primary = primary,
                            modifier = Modifier.weight(1f),
                            onClick = { vm.setReportType(type) },
                        )
                    }
                }
            }

            // ── Time period ───────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Time period")

                // FIX 1: Split into two rows so "Custom" is never squeezed next
                // to five other pills. Row 1 → the five short quick-select pills,
                // each with equal weight. Row 2 → "Custom" full-width pill with a
                // fixed height so text never wraps.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    timePeriods.dropLast(1).forEach { period ->
                        val isSelected = selectedPeriod == period
                        PillChip(
                            label = period,
                            isSelected = isSelected,
                            primary = primary,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedPeriod = period
                                applyPeriod(period)
                            },
                        )
                    }
                }

                // "Custom" pill — full width, single line guaranteed
                val customSelected = selectedPeriod == "Custom"
                PillChip(
                    label = "Custom range",
                    isSelected = customSelected,
                    primary = primary,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { selectedPeriod = "Custom" },
                )

                // From / To date row — only visible when Custom is selected
                if (selectedPeriod == "Custom") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        DatePickerField(
                            label = "From",
                            date = epochToDisplay(uiState.fromDate),
                            modifier = Modifier.weight(1f),
                            onClick = { showFromPicker = true },
                        )
                        DatePickerField(
                            label = "To",
                            date = epochToDisplay(uiState.toDate),
                            modifier = Modifier.weight(1f),
                            onClick = { showToPicker = true },
                        )
                    }
                }
            }

            // ── Categories ────────────────────────────────────────────────
            if (uiState.availableCategories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Categories")

                    // FIX 2: Always render chip text at FontWeight.SemiBold so
                    // the chip's intrinsic width never changes on selection.
                    // Selection state is communicated through background / border
                    // / text colour only — not weight — so FlowRow never re-flows.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val allSelected = uiState.allCategoriesSelected
                        CategoryChip(
                            label = "All",
                            isSelected = allSelected,
                            primary = primary,
                            onClick = { vm.selectAllCategories() },
                        )
                        uiState.availableCategories.forEach { cat ->
                            CategoryChip(
                                label = cat,
                                isSelected = cat in uiState.selectedCategories,
                                primary = primary,
                                onClick = { vm.toggleCategory(cat) },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Shared pill composables ──────────────────────────────────────────────────

/**
 * Generic pill chip used for Report type and Time period rows.
 *
 * • [maxLines] = 1 + [softWrap] = false ensure the label never wraps,
 *   regardless of how narrow the pill gets.
 * • A minimum height of 36 dp gives every pill the same touch target even
 *   when the pill is very narrow.
 */
@Composable
private fun PillChip(
    label: String,
    isSelected: Boolean,
    primary: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier.heightIn(min = 36.dp)               // ← uniform height, never collapses
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) primary else MaterialTheme.colorScheme.surfaceContainer).border(
                1.dp,
                if (isSelected) primary else MaterialTheme.colorScheme.outline.copy(0.25f),
                RoundedCornerShape(20.dp),
            ).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,        // ← always SemiBold; weight never changes
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onBackground,
            maxLines = 1,                           // ← never wraps
            softWrap = false,                       // ← hard single-line guarantee
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Category chip inside FlowRow.
 *
 * Key fix: text is ALWAYS FontWeight.SemiBold. The only things that change on
 * selection are background colour, border colour, and text colour — none of
 * which affect the chip's measured width, so FlowRow never re-flows other chips.
 */
@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    primary: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) primary else MaterialTheme.colorScheme.surfaceContainer).border(
                1.dp,
                if (isSelected) primary else MaterialTheme.colorScheme.outline.copy(0.2f),
                RoundedCornerShape(20.dp),
            ).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,        // ← always SemiBold — width is stable
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    date: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    date,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Icon(
                    painter = painterResource(R.drawable.calender),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 390, heightDp = 900, name = "Create Report – Light")
@Composable
private fun CreateReportScreenPreview() {
    TruXpenseTheme { CreateReportScreen(onPreview = {}) }
}