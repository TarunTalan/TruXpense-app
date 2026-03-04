package com.example.truxpense.presentation.screens.dashboard.addexpense

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.AccountDropdown
import com.example.truxpense.presentation.screens.dashboard.components.CategoryDropdown
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.utils.AppCategories
import com.example.truxpense.presentation.screens.dashboard.home.HomeTransactionItem
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.utils.clearFocusOnTap
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onSave: (HomeTransactionItem) -> Unit = {},
    onBack: () -> Unit = {},
    onDatePick: () -> Unit = {},
) {
    val vm = hiltViewModel<AddExpenseViewModel>()

    val rawAmount by vm.rawAmount.collectAsState()
    val merchant by vm.merchant.collectAsState()
    val notes by vm.notes.collectAsState()
    val selectedCategory by vm.selectedCategory.collectAsState()
    val selectedAccount by vm.selectedAccount.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    val selectedTime by vm.selectedTime.collectAsState()
    val isFormValid by vm.isFormValid.collectAsState()

    val context = LocalContext.current

    val surfaceArgb = MaterialTheme.colorScheme.surface.toArgb()
    val onPrimaryArgb = MaterialTheme.colorScheme.onPrimary.toArgb()
    val onBackgroundArgb = MaterialTheme.colorScheme.onBackground.toArgb()

    fun openDatePicker() {
        val now = Calendar.getInstance()
        val picker = DatePickerDialog(context, { _: android.widget.DatePicker, y: Int, m: Int, d: Int ->
            val cal = Calendar.getInstance().apply { set(y, m, d) }
            vm.setDate(SimpleDateFormat("MMM d", Locale.getDefault()).format(cal.time))
            onDatePick()
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
        picker.setOnShowListener {
            try {
                picker.window?.setBackgroundDrawable(ColorDrawable(surfaceArgb))
                picker.getButton(DatePickerDialog.BUTTON_POSITIVE)?.setTextColor(onPrimaryArgb)
                picker.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setTextColor(onBackgroundArgb)
            } catch (_: Exception) { }
        }
        picker.show()
    }

    fun openTimePicker() {
        val now = Calendar.getInstance()
        val dialog = TimePickerDialog(
            context,
            { _, hour, minute ->
                val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute) }
                vm.setTime(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time))
            },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            false,
        )
        dialog.setOnShowListener {
            try {
                dialog.window?.setBackgroundDrawable(ColorDrawable(surfaceArgb))
                dialog.getButton(TimePickerDialog.BUTTON_POSITIVE)?.setTextColor(onPrimaryArgb)
                dialog.getButton(TimePickerDialog.BUTTON_NEGATIVE)?.setTextColor(onBackgroundArgb)
            } catch (_: Exception) { }
        }
        dialog.show()
    }

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
        onDatePick = { openDatePicker() },
        onTimePick = { openTimePicker() },
        onSave = {
            val amt = vm.rawAmount.value.toDoubleOrNull() ?: 0.0
            val merchantVal = vm.merchant.value.ifBlank { "Anonymous" }
            val categoryVal = vm.selectedCategory.value ?: "Other"
            vm.saveExpense()
            onSave(HomeTransactionItem(
                id = UUID.randomUUID().toString(),
                title = merchantVal,
                category = categoryVal,
                amount = amt,
                currencyCode = "INR",
            ))
        },
        onBack = onBack,
    )
}

// Parameterized content for AddExpense screen — no Hilt inside, friendly for Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreenContent(
    rawAmount: String,
    merchant: String,
    notes: String,
    selectedCategory: String?,
    selectedAccount: String?,
    categories: List<String>,
    accounts: List<String>,
    selectedDate: String?,
    selectedTime: String? = null,
    isFormValid: Boolean,
    onRawChange: (String) -> Unit,
    onMerchantChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectAccount: (String) -> Unit,
    onDatePick: () -> Unit,
    onTimePick: () -> Unit = {},
    onSave: () -> Unit,
    onBack: () -> Unit,
    // New optional parameters for overrides (Edit screen passes these)
    screenTitle: String = "Add expense",
    saveLabel: String? = null,
    extraBottomContent: (@Composable () -> Unit)? = null,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(headerTitle = screenTitle, showBack = true, onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .padding(horizontal = DashboardDimens.screenPaddingH)
                .verticalScroll(rememberScrollState())
                .clearFocusOnTap(),
        ) {
            Spacer(Modifier.height(DashboardDimens.spaceMd))

            // Amount card (separate card above details)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DashboardDimens.cornerCard),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = DashboardDimens.cardElevation),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(DashboardDimens.cardPaddingComp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AmountInputZone(rawAmount = rawAmount, onRawChange = onRawChange)
                }
            }

            Spacer(Modifier.height(DashboardDimens.spaceLg))

            // Details card (category, merchant, account, date)
            DetailsCard(
                selectedCategory = selectedCategory,
                categories = categories,
                onCategorySelect = onSelectCategory,
                merchant = merchant,
                onMerchantChange = onMerchantChange,
                selectedAccount = selectedAccount,
                accounts = accounts,
                onAccountSelect = onSelectAccount,
                selectedDate = selectedDate,
                selectedTime = selectedTime,
                onDateClick = onDatePick,
                onTimeClick = onTimePick,
            )

            Spacer(Modifier.height(DashboardDimens.spaceXxl))

            // Notes
            NotesCard(notes = notes, onChange = onNotesChange)

            Spacer(Modifier.height(DashboardDimens.spaceXxl))

            // Action buttons
            SaveButton(enabled = isFormValid, onClick = onSave, label = saveLabel ?: "Save expense")
            Spacer(Modifier.height(DashboardDimens.spaceMd))

            // Optional extra bottom content (e.g., Cancel button for Edit screen)
            if (extraBottomContent != null) {
                Spacer(Modifier.height(DashboardDimens.spaceMd))
                extraBottomContent()
            }

            Spacer(Modifier.height(DashboardDimens.spaceXxl))
        }
    }
}

// ─── ① Amount Input Zone ─────────────────────────────────────────────────────

@Composable
private fun AmountInputZone(
    rawAmount: String,
    onRawChange: (String) -> Unit,
) {
    // Single centered text field for amount input
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "How much did you spend?",
            modifier = Modifier,
            fontSize = DashboardDimens.textInput,
            color = MaterialTheme.colorScheme.tertiary
        )

        Spacer(Modifier.height(DashboardDimens.spaceLg))

        val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

        // Measurement helpers to size the currency+amount tightly
        val textMeasurer = rememberTextMeasurer()
        val displayStyle = MaterialTheme.typography.displaySmall.copy(
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            letterSpacing = 1.15.sp
        )
        val amountText = rawAmount.ifEmpty { "0" }
        val currencyText = "₹"
        val amountLayout = textMeasurer.measure(AnnotatedString(amountText), style = displayStyle)
        val currencyLayout = textMeasurer.measure(AnnotatedString(currencyText), style = displayStyle)
        val totalPx = amountLayout.size.width + currencyLayout.size.width
        val density = LocalDensity.current
        val totalDp = with(density) { totalPx.toDp() }
        val finalWidth = totalDp + DashboardDimens.spaceSm

        Box(
            modifier = Modifier.fillMaxWidth().clickable(
            indication = null, interactionSource = remember { MutableInteractionSource() }) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }, contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = rawAmount,
                onValueChange = onRawChange,
                singleLine = true,
                modifier = Modifier.widthIn(min = DashboardDimens.iconButtonMd).width(finalWidth)
                    .focusRequester(focusRequester),
                textStyle = displayStyle,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                decorationBox = { inner ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.wrapContentWidth()) {
                        Text(
                            text = currencyText,
                            style = displayStyle,
                            color = MaterialTheme.colorScheme.onBackground,
                        )

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.wrapContentWidth()) {
                            if (rawAmount.isEmpty()) {
                                Text(
                                    text = "0",
                                    style = displayStyle.copy(color = MaterialTheme.colorScheme.onBackground.copy(0.7f)),
                                    modifier = Modifier.padding(end = DashboardDimens.spaceXs)
                                )
                            }
                            inner()
                        }
                    }
                })
        }
    }

    Spacer(Modifier.height(DashboardDimens.spaceXs))
}

// ─── ② Details Card ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsCard(
    selectedCategory: String?,
    categories: List<String>,
    onCategorySelect: (String) -> Unit,
    merchant: String,
    onMerchantChange: (String) -> Unit,
    selectedAccount: String?,
    accounts: List<String>,
    onAccountSelect: (String) -> Unit,
    selectedDate: String?,
    selectedTime: String?,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Row 1: Category ──────────────────────────────────────────────
            CategoryDropdown(
                selected = selectedCategory,
                categories = categories,
                onSelect = { onCategorySelect(it) },
                iconForCategory = { cat -> iconForCategory(cat) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Select category",
            )

            RowDivider()

            // ── Row 2: Merchant ───────────────────────────────────────────────
            DetailRow(
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.add_notes_icon),
                        contentDescription = null,
                        tint = if (merchant.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(DashboardDimens.iconMd),
                    )
                },
                label = "Merchant",
                modifier = Modifier.fillMaxWidth(),
            ) {
                BasicTextField(
                    value = merchant,
                    onValueChange = onMerchantChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (merchant.isEmpty()) {
                                Text(
                                    text = "Where did you spend?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                    },
                )
            }

            RowDivider()

            // ── Row 3: Account ────────────────────────────────────────────────
            AccountDropdown(
                selected = selectedAccount,
                accounts = accounts,
                onSelect = { onAccountSelect(it) },
                iconForAccount = { acc -> iconForAccount(acc) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Select account",
                inline = true,
            )

            RowDivider()

            // ── Row 4: Date + Time (same row) ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DashboardDimens.detailRowHeight)
                    .padding(horizontal = DashboardDimens.screenPaddingH),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
            ) {
                // Leading calendar icon
                Icon(
                    painter = painterResource(R.drawable.calender),
                    contentDescription = null,
                    tint = if (selectedDate != null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DashboardDimens.iconMd),
                )

                // Date chip
                DateTimeChip(
                    label = "Date",
                    value = selectedDate ?: "Pick date",
                    hasValue = selectedDate != null,
                    onClick = onDateClick,
                    modifier = Modifier.weight(1f),
                )

                // Divider between date and time
                VerticalDivider(
                    modifier = Modifier.height(24.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = DashboardDimens.dividerThin,
                )

                // Time chip
                DateTimeChip(
                    label = "Time",
                    value = selectedTime ?: "Pick time",
                    hasValue = selectedTime != null,
                    onClick = onTimeClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}


@Composable
private fun DetailRow(
    leadingIcon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(DashboardDimens.detailRowHeight)
            .padding(horizontal = DashboardDimens.screenPaddingH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
    ) {
        // Leading icon — consistent slot
        Box(
            modifier = Modifier.size(DashboardDimens.iconMd),
            contentAlignment = Alignment.Center,
        ) {
            leadingIcon()
        }

        // Label + value stacked
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(DashboardDimens.spaceXxs))
            content()
        }

        // Optional trailing icon
        if (trailingIcon != null) {
            Box(
                modifier = Modifier.size(DashboardDimens.iconMd),
                contentAlignment = Alignment.Center,
            ) {
                trailingIcon()
            }
        }
    }
}

/** Thin divider with screen-edge inset */
@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth()
            .padding(start = DashboardDimens.screenPaddingH + DashboardDimens.iconMd + DashboardDimens.spaceMd),
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = DashboardDimens.dividerThin,
    )
}

/** Small tappable chip used for the Date and Time cells in the same row. */
@Composable
private fun DateTimeChip(
    label: String,
    value: String,
    hasValue: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(DashboardDimens.cornerChip))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(vertical = DashboardDimens.spaceXs),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(DashboardDimens.spaceXxs))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (hasValue) FontWeight.Medium else FontWeight.Normal,
            color = if (hasValue) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


// ─── ③ Notes card, buttons, preview and helpers ───────────────────────────────

private const val NOTES_MAX_CHARS = 100

@Composable
private fun NotesCard(notes: String, onChange: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = DashboardDimens.screenPaddingH, vertical = DashboardDimens.spaceLg),
        ) {
            // Header row — icon + label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
            ) {
                Icon(
                    painter = painterResource(R.drawable.add_notes_icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DashboardDimens.iconMd),
                )
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(DashboardDimens.spaceMd))

            // Text input — hard-capped at NOTES_MAX_CHARS, max 3 visual lines
            BasicTextField(
                value = notes,
                onValueChange = { if (it.length <= NOTES_MAX_CHARS) onChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DashboardDimens.cornerChip))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = DashboardDimens.spaceLg, vertical = DashboardDimens.spaceMdL)
                    .defaultMinSize(minHeight = DashboardDimens.inputMinHeight),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                maxLines = 3,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (notes.isEmpty()) {
                        Text(
                            text = "Add a few notes to help you remember…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                },
            )

            // Character counter
            Spacer(Modifier.height(DashboardDimens.spaceXs))
            Text(
                text = "${notes.length}/$NOTES_MAX_CHARS",
                style = MaterialTheme.typography.labelSmall,
                color = if (notes.length >= NOTES_MAX_CHARS)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun SaveButton(enabled: Boolean, onClick: () -> Unit, label: String = "Save expense") {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(DashboardDimens.buttonHeight),
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = DashboardDimens.textXl, color = MaterialTheme.colorScheme.background)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, backgroundColor = 0xFFFFFFFF)
@Composable
fun AddExpenseScreenPreview() {
    AddExpenseScreenContent(
        rawAmount = "1200",
        merchant = "Cafe",
        notes = "Lunch",
        selectedCategory = "Food",
        selectedAccount = "Cash",
        categories = AppCategories.all,
        accounts = listOf("HDFC Bank", "SBI", "ICICI Bank", "Axis Bank", "Cash", "UPI"),
        selectedDate = "Mar 4",
        selectedTime = "09:30 AM",
        isFormValid = true,
        onRawChange = {},
        onMerchantChange = {},
        onNotesChange = {},
        onSelectCategory = {},
        onSelectAccount = {},
        onDatePick = {},
        onSave = {},
        onBack = {})
}

private fun iconForCategory(category: String?): Int = when (category?.trim()?.lowercase()) {
    "food" -> R.drawable.food
    "transport" -> R.drawable.transport
    "bills" -> R.drawable.bills
    "shopping" -> R.drawable.shopping
    "travel" -> R.drawable.category_icon
    "health" -> R.drawable.health
    "education" -> R.drawable.category_icon
    "entertainment" -> R.drawable.entertainment
    "groceries" -> R.drawable.groceries
    else -> R.drawable.category_icon
}

private fun iconForAccount(account: String?): Int = when (account?.trim()?.lowercase()) {
    "hdfc bank", "hdfc" -> R.drawable.bills
    "sbi", "sbi bank" -> R.drawable.bills
    "icici bank", "icici" -> R.drawable.bills
    "axis bank", "axis" -> R.drawable.bills
    "cash" -> R.drawable.bills
    "upi" -> R.drawable.bills
    else -> R.drawable.bills
}