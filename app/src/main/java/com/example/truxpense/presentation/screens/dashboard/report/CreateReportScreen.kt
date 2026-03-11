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

// ─── Data ────────────────────────────────────────────────────────────────────

private val timePeriods = listOf("7D", "1M", "3M", "6M", "1y", "Custom")

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportScreen(
    onBack: () -> Unit = {},
    /** Called with the saved reportId so the caller can navigate to detail. */
    onPreview: (reportId: String) -> Unit = {},
    vm: CreateReportViewModel = hiltViewModel(),
) {
    val primary = MaterialTheme.colorScheme.primary
    val uiState by vm.uiState.collectAsState()

    // Navigate to detail when report is saved
    LaunchedEffect(uiState.savedReportId) {
        val id = uiState.savedReportId
        if (id != null) {
            vm.onNavigatedToDetail()
            onPreview(id)
        }
    }

    // Quick-period picker — maps pill → date range
    fun applyPeriod(period: String) {
        val now = Calendar.getInstance()
        val from = Calendar.getInstance()
        when (period) {
            "7D" -> from.add(Calendar.DAY_OF_YEAR, -7)
            "1M" -> from.add(Calendar.MONTH, -1)
            "3M" -> from.add(Calendar.MONTH, -3)
            "6M" -> from.add(Calendar.MONTH, -6)
            "1y" -> from.add(Calendar.YEAR, -1)
            else -> return   // "Custom" → user picks manually
        }
        vm.setFromDate(from.timeInMillis)
        vm.setToDate(now.timeInMillis)
    }

    var selectedPeriod by remember { mutableStateOf("1M") }

    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    val fromPickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.fromDate)
    val toPickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.toDate)

    fun epochToDisplay(ms: Long): String =
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(ms))

    // Date picker dialogs
    if (showFromPicker) {
        AppDatePickerDialog(
            state = fromPickerState,
            onDismiss = { showFromPicker = false },
            onConfirm = { ms ->
                ms?.let { vm.setFromDate(it) }
                showFromPicker = false
            },
        )
    }
    if (showToPicker) {
        AppDatePickerDialog(
            state = toPickerState,
            onDismiss = { showToPicker = false },
            onConfirm = { ms ->
                ms?.let { vm.setToDate(it) }
                showToPicker = false
            },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .padding(horizontal = DashboardDimens.screenPaddingH, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        vm.suggestTitle()
                        vm.saveReport()
                    },
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
                // Error messages
                uiState.titleError?.let {
                    Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                }
                uiState.dateError?.let {
                    Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = DashboardDimens.screenPaddingH)
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        ReportType.EXPENSE to "Expense",
                        ReportType.INCOME to "Income",
                        ReportType.ALL to "All"
                    ).forEach { (type, label) ->
                        val isSelected = uiState.reportType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) primary else MaterialTheme.colorScheme.surfaceContainer)
                                .border(
                                    1.dp,
                                    if (isSelected) primary else MaterialTheme.colorScheme.outline.copy(0.25f),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { vm.setReportType(type) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            }

            // ── Time period ───────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Time period")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    timePeriods.forEach { period ->
                        val isSelected = selectedPeriod == period
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) primary else MaterialTheme.colorScheme.surfaceContainer)
                                .border(
                                    1.dp,
                                    if (isSelected) primary else MaterialTheme.colorScheme.outline.copy(0.25f),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    selectedPeriod = period
                                    applyPeriod(period)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                period,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }

                // From / To date row — only visible when Custom is selected
                if (selectedPeriod == "Custom") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "All" chip
                        val allSelected = uiState.allCategoriesSelected
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (allSelected) primary else MaterialTheme.colorScheme.surfaceContainer)
                                .border(
                                    1.dp,
                                    if (allSelected) primary else MaterialTheme.colorScheme.outline.copy(0.2f),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { vm.selectAllCategories() }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                "All",
                                fontSize = 13.sp,
                                fontWeight = if (allSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (allSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        // Per-category chips
                        uiState.availableCategories.forEach { cat ->
                            val isSelected = cat in uiState.selectedCategories
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) primary else MaterialTheme.colorScheme.surfaceContainer)
                                    .border(
                                        1.dp,
                                        if (isSelected) primary else MaterialTheme.colorScheme.outline.copy(0.2f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { vm.toggleCategory(cat) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    cat,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
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
    GradientCard(modifier = modifier.clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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