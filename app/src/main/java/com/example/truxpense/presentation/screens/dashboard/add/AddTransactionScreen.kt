package com.example.truxpense.presentation.screens.dashboard.add

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.presentation.screens.dashboard.components.AppDatePickerDialog
import com.example.truxpense.presentation.screens.dashboard.components.AppTimePickerDialog
import com.example.truxpense.presentation.screens.dashboard.components.iconForIncomeSource
import com.example.truxpense.presentation.screens.dashboard.expense.AddExpenseScreenContent
import com.example.truxpense.presentation.screens.dashboard.expense.AddExpenseViewModel
import com.example.truxpense.presentation.screens.dashboard.income.AddIncomeViewModel
import com.example.truxpense.presentation.theme.TruXpenseTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class,
)
@Composable
fun AddTransactionScreen(
    initialTab: Int = 0,
    onBack: () -> Unit = {},
) {
    val tabs = listOf("Expense", "Income")
    val pagerState = rememberPagerState(
        initialPage = initialTab.coerceIn(0, tabs.lastIndex),
        pageCount = { tabs.size },
    )
    val scope = rememberCoroutineScope()
    val currentTab = pagerState.currentPage

    val expenseVm: AddExpenseViewModel = hiltViewModel()
    val incomeVm: AddIncomeViewModel = hiltViewModel()

    val expenseFormValid by expenseVm.isFormValid.collectAsState()
    val incomeFormValid by incomeVm.isFormValid.collectAsState()
    val isFormValid = if (currentTab == 0) expenseFormValid else incomeFormValid

    LaunchedEffect(Unit) {
        launch { expenseVm.saveComplete.collect { onBack() } }
        launch { incomeVm.saveComplete.collect { onBack() } }
    }

    // ── Shared date / time pickers ────────────────────────────────────────────
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().get(Calendar.MINUTE),
    )

    if (showDatePicker) {
        AppDatePickerDialog(
            state = datePickerState,
            onDismiss = { showDatePicker = false },
            onConfirm = { ms ->
                ms?.let {
                    val label = SimpleDateFormat("MMM d", Locale.getDefault()).format(
                        Calendar.getInstance().apply { timeInMillis = it }.time
                    )
                    if (currentTab == 0) expenseVm.setDate(label) else incomeVm.setDate(label)
                }
                showDatePicker = false
            },
        )
    }

    if (showTimePicker) {
        AppTimePickerDialog(
            state = timePickerState,
            onDismiss = { showTimePicker = false },
            onConfirm = {
                val label = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(
                    Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                    }.time
                )
                if (currentTab == 0) expenseVm.setTime(label) else incomeVm.setTime(label)
                showTimePicker = false
            },
        )
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            // Respect status bar; navigation bar padding is handled by inner
            // content (imePadding / navigationBarsPadding inside AddExpenseScreenContent).
            .statusBarsPadding(),
    ) {
        // ── Custom top bar ────────────────────────────────────────────────────
        AddTransactionTopBar(
            tabs = tabs,
            selectedTab = currentTab,
            isFormValid = isFormValid,
            onCancel = onBack,
            // Use scrollToPage for an instant switch without animation when the toggle is used.
            onTabSelected = { idx -> scope.launch { pagerState.scrollToPage(idx) } },
            onSave = {
                if (currentTab == 0) expenseVm.saveExpense() else incomeVm.saveIncome()
            },
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // ── Tab content ───────────────────────────────────────────────────────
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true,
        ) { page ->
            when (page) {
                0 -> ExpenseTabContent(
                    vm = expenseVm,
                    onDatePick = { showDatePicker = true },
                    onTimePick = { showTimePicker = true },
                    onSave = { expenseVm.saveExpense() },
                    onBack = onBack,
                )

                1 -> IncomeTabContent(
                    vm = incomeVm,
                    onDatePick = { showDatePicker = true },
                    onTimePick = { showTimePicker = true },
                    onSave = { incomeVm.saveIncome() },
                    onBack = onBack,
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Custom top bar  —  plain Row, no TopAppBar internal constraints
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AddTransactionTopBar(
    tabs: List<String>,
    selectedTab: Int,
    isFormValid: Boolean,
    onCancel: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onSave: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // ── Row 1: Cancel ──────────────────────────────── Save ───────────────
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onCancel) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            TextButton(
                onClick = onSave,
                enabled = isFormValid,
            ) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (isFormValid) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    maxLines = 1,
                )
            }
        }

        // ── Row 2: Expense | Income toggle ────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            TabTogglePill(
                tabs = tabs,
                selectedIdx = selectedTab,
                onSelect = onTabSelected,
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Pill toggle  (Expense | Income)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TabTogglePill(
    tabs: List<String>,
    selectedIdx: Int,
    onSelect: (Int) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            tabs.forEachIndexed { index, label ->
                val selected = index == selectedIdx

                val bgColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary
                    else Color.Transparent,
                    animationSpec = tween(180),
                    label = "pillBg$index",
                )
                val textColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(180),
                    label = "pillText$index",
                )

                Box(
                    modifier = Modifier.clip(MaterialTheme.shapes.medium).background(bgColor).clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(index) },
                    )
                        // Larger tap target + more breathing room for the label
                        .padding(horizontal = 20.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Per-tab content wrappers  (no nested Scaffold chrome)
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class,
)
@Composable
private fun ExpenseTabContent(
    vm: AddExpenseViewModel,
    onDatePick: () -> Unit,
    onTimePick: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val rawAmount by vm.rawAmount.collectAsState()
    val merchant by vm.merchant.collectAsState()
    val notes by vm.notes.collectAsState()
    val selectedCategory by vm.selectedCategory.collectAsState()
    val selectedAccount by vm.selectedAccount.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    val selectedTime by vm.selectedTime.collectAsState()
    val isFormValid by vm.isFormValid.collectAsState()

    AddExpenseScreenContent(
        rawAmount = rawAmount,
        merchant = merchant,
        notes = notes,
        selectedCategory = selectedCategory,
        selectedAccount = selectedAccount,
        categories = vm.categories,
        accounts = vm.accountList,
        selectedDate = selectedDate,
        selectedTime = selectedTime,
        isFormValid = isFormValid,
        onRawChange = { vm.setRawAmount(it) },
        onMerchantChange = { vm.setMerchant(it) },
        onNotesChange = { vm.setNotes(it) },
        onSelectCategory = { vm.selectCategory(it) },
        onSelectAccount = { vm.selectAccount(it) },
        onDatePick = onDatePick,
        onTimePick = onTimePick,
        onSave = onSave,
        onBack = onBack,
        showTopBar = false,
        showBottomSaveBar = false,
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class,
)
@Composable
private fun IncomeTabContent(
    vm: AddIncomeViewModel,
    onDatePick: () -> Unit,
    onTimePick: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val rawAmount by vm.rawAmount.collectAsState()
    val sourceName by vm.sourceName.collectAsState()
    val notes by vm.notes.collectAsState()
    val selectedSource by vm.selectedSource.collectAsState()
    val selectedAccount by vm.selectedAccount.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    val selectedTime by vm.selectedTime.collectAsState()
    val isFormValid by vm.isFormValid.collectAsState()

    AddExpenseScreenContent(
        rawAmount = rawAmount,
        merchant = sourceName,
        notes = notes,
        selectedCategory = selectedSource,
        selectedAccount = selectedAccount,
        categories = vm.sourceOptions,
        accounts = vm.accountList,
        selectedDate = selectedDate,
        selectedTime = selectedTime,
        isFormValid = isFormValid,
        onRawChange = { vm.setRawAmount(it) },
        onMerchantChange = { vm.setSourceName(it) },
        onNotesChange = { vm.setNotes(it) },
        onSelectCategory = { vm.selectSource(it) },
        onSelectAccount = { vm.selectAccount(it) },
        onDatePick = onDatePick,
        onTimePick = onTimePick,
        onSave = onSave,
        onBack = onBack,
        categoryLabel = "Income source",
        merchantLabel = "Source name",
        merchantPlaceholder = "e.g. Acme Corp, Client name...",
        paymentMethodOptions = vm.accountList,
        paymentMethodLabel = "Received via",
        categoryIconResolver = ::iconForIncomeSource,
        showTopBar = false,
        showBottomSaveBar = false,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun AddTransactionScreenPreview() {
    TruXpenseTheme {
        var selected by remember { mutableStateOf(0) }
        val tabs = listOf("Expense", "Income")

        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
            AddTransactionTopBar(
                tabs = tabs,
                selectedTab = selected,
                isFormValid = true,
                onCancel = {},
                onTabSelected = { selected = it },
                onSave = {},
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (selected == 0) {
                    Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(8.dp), tonalElevation = 2.dp) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(text = "Expense form preview", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                } else {
                    Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(8.dp), tonalElevation = 2.dp) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(text = "Income form preview", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
