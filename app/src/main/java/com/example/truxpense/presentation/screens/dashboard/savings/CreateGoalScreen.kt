package com.example.truxpense.presentation.screens.dashboard.savings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import com.example.truxpense.R
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.truxpense.data.repository.savings.ContributeFrequency
import com.example.truxpense.presentation.screens.dashboard.components.*
import com.example.truxpense.presentation.screens.dashboard.settings.Toggle
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.theme.TruXpenseTheme
import com.example.truxpense.presentation.utils.SimpleTextField

// ── ViewModel-wired entry point ───────────────────────────────────────────────

@Composable
fun CreateGoalScreen(
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
    vm: CreateGoalViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        vm.created.collect { id -> onCreated(id) }
    }

    CreateGoalContent(
        isEditMode = vm.isEditMode,
        name = vm.name.collectAsStateWithLifecycle().value,
        icon = vm.icon.collectAsStateWithLifecycle().value,
        targetAmount = vm.targetAmount.collectAsStateWithLifecycle().value,
        targetDateEpoch = vm.targetDateEpoch.collectAsStateWithLifecycle().value,
        autoContribute = vm.autoContribute.collectAsStateWithLifecycle().value,
        dailyAmount = vm.dailyAmount.collectAsStateWithLifecycle().value,
        frequency = vm.frequency.collectAsStateWithLifecycle().value,
        showAllIcons = vm.showAllIcons.collectAsStateWithLifecycle().value,
        canCreate = vm.canCreate.collectAsStateWithLifecycle().value,
        onBack = onBack,
        onNameChange = vm::onNameChange,
        onIconSelect = vm::onIconSelect,
        onAmountChange = vm::onAmountChange,
        onDateChange = vm::onDateChange,
        onAutoContributeToggle = vm::onAutoContributeToggle,
        onDailyAmountChange = vm::onDailyAmountChange,
        onFrequencyChange = vm::onFrequencyChange,
        onToggleShowAllIcons = vm::onToggleShowAllIcons,
        onCreateOrUpdate = vm::createOrUpdate,
    )
}

// ── Stateless UI ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalContent(
    isEditMode: Boolean,
    name: String,
    icon: String,
    targetAmount: String,
    targetDateEpoch: Long,
    autoContribute: Boolean,
    dailyAmount: String,
    frequency: ContributeFrequency,
    showAllIcons: Boolean,
    canCreate: Boolean,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onIconSelect: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onAutoContributeToggle: () -> Unit,
    onDailyAmountChange: (String) -> Unit,
    onFrequencyChange: (ContributeFrequency) -> Unit,
    onToggleShowAllIcons: () -> Unit,
    onCreateOrUpdate: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary

    // ── Date picker dialog ────────────────────────────────────────────────────
    val initialSelectedMillis = remember(targetDateEpoch) { targetDateEpoch * 86_400_000L }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialSelectedMillis)

    if (showDatePicker) {
        AppDatePickerDialog(
            state = datePickerState,
            onDismiss = { showDatePicker = false },
            onConfirm = { ms ->
                ms?.let { onDateChange(it / 86_400_000L) }
                showDatePicker = false
            },
        )
    }

    // ── Scaffold mirrors AddExpenseScreen: sticky bottomBar + scrollable body ─
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                headerTitle = if (isEditMode) "Edit Goal" else "Create Goal",
                showBack = true,
                onBack = onBack,
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
                        .padding(horizontal = DashboardDimens.screenPaddingH, vertical = 12.dp),
                ) {
                    SaveButton(
                        enabled = canCreate,
                        onClick = onCreateOrUpdate,
                        label = if (isEditMode) "Update Goal" else "Create Goal",
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = DashboardDimens.screenPaddingH)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Target amount ─────────────────────────────────────────────
            AmountInputCard(
                question = "What is your Target Amount?", rawAmount = targetAmount, onRawChange = onAmountChange
            )

            // ── Goal name ─────────────────────────────────────────────────
            LabeledTextField(
                label = "Goal name",
                placeholder = "e.g. iPhone 16",
                value = name,
                onValueChange = onNameChange,
            )

            // ── Icon picker ───────────────────────────────────────────────
            val initialCount = 8
            val visibleIcons = if (showAllIcons) GOAL_ICONS else GOAL_ICONS.take(initialCount)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryPickerGrid(
                    categories = visibleIcons,
                    selected = icon,
                    onSelect = onIconSelect,
                    modifier = Modifier.fillMaxWidth(),
                    label = "Icon",
                    iconResolver = ::goalIconToDrawable,
                    showChipLabel = false,
                    iconModifier = Modifier.width(24.dp).height(40.dp),
                )
                if (!showAllIcons) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(primary.copy(alpha = 0.10f))
                            .border(1.dp, primary.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                            .clickable { onToggleShowAllIcons() }.padding(horizontal = 16.dp, vertical = 7.dp),
                    ) {
                        Text(
                            "+${GOAL_ICONS.size - initialCount} more",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primary,
                        )
                    }
                }
            }

            // ── Target date (full-width, no time) ────────────────────────
            val displayDate = remember(targetDateEpoch) {
                val cal = Calendar.getInstance().apply { timeInMillis = targetDateEpoch * 86_400_000L }
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Target Date",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Box(
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), MaterialTheme.shapes.medium)
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = displayDate,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Icon(
                            painter = painterResource(R.drawable.calender),
                            contentDescription = "Open calendar",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(DashboardDimens.iconMd)
                        )
                    }
                }
            }

            // ── Smart auto-contribute (only section with FormCard) ─────────
            FormCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Smart Auto-contribute",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "₹${dailyAmount.ifEmpty { "0" }} · Every ${frequency.name.lowercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Toggle(checked = autoContribute, onCheckedChange = { onAutoContributeToggle() })
                }

                AnimatedVisibility(visible = autoContribute) {
                    Column(
                        Modifier.padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Quick-amount chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("100", "250", "500", "1000").forEach { v ->
                                val selected = dailyAmount == v
                                Box(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).height(40.dp)
                                        .border(if (selected) 0.dp else 1.dp, primary, RoundedCornerShape(12.dp))
                                        .background(if (selected) primary else Color.Transparent)
                                        .clickable { onDailyAmountChange(v) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "₹$v",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                                    )
                                }
                            }
                        }

                        // Custom daily amount
                        SimpleTextField(
                            value = if (dailyAmount !in listOf("100", "250", "500", "1000")) dailyAmount else "",
                            onValueChange = onDailyAmountChange,
                            placeholder = "Custom amount",
                            label = "Or enter custom",
                            prefix = {
                                Text(
                                    "₹",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                            ),
                            bgColor = MaterialTheme.colorScheme.background,
                        )

                        // Frequency chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ContributeFrequency.entries.forEach { f ->
                                val selected = frequency == f
                                FilterChip(
                                    selected = selected,
                                    onClick = { onFrequencyChange(f) },
                                    label = { Text(f.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
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

// ── Shared layout helpers ─────────────────────────────────────────────────────

@Composable
fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(DashboardDimens.cornerCard))
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(DashboardDimens.cornerCard))
            .padding(DashboardDimens.cardPadding)
    ) {
        content()
    }
}

@Composable
fun FormLabel(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    name = "Create Goal",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
private fun CreateGoalScreenPreview() {
    TruXpenseTheme {
        CreateGoalContent(
            isEditMode = false,
            name = "iPhone 16",
            icon = "iphone",
            targetAmount = "120000",
            targetDateEpoch = 19_900L,
            autoContribute = true,
            dailyAmount = "500",
            frequency = ContributeFrequency.DAILY,
            showAllIcons = false,
            canCreate = true,
            onBack = {},
            onNameChange = {},
            onIconSelect = {},
            onAmountChange = {},
            onDateChange = {},
            onAutoContributeToggle = {},
            onDailyAmountChange = {},
            onFrequencyChange = {},
            onToggleShowAllIcons = {},
            onCreateOrUpdate = {},
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    name = "Edit Goal – auto-contribute off",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
private fun EditGoalScreenPreview() {
    TruXpenseTheme {
        CreateGoalContent(
            isEditMode = true,
            name = "Vacation Trip",
            icon = "airplane",
            targetAmount = "60000",
            targetDateEpoch = 20_000L,
            autoContribute = false,
            dailyAmount = "0",
            frequency = ContributeFrequency.WEEKLY,
            showAllIcons = false,
            canCreate = true,
            onBack = {},
            onNameChange = {},
            onIconSelect = {},
            onAmountChange = {},
            onDateChange = {},
            onAutoContributeToggle = {},
            onDailyAmountChange = {},
            onFrequencyChange = {},
            onToggleShowAllIcons = {},
            onCreateOrUpdate = {},
        )
    }
}