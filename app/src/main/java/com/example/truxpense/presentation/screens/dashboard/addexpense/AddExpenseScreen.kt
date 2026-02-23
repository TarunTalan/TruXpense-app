package com.example.truxpense.presentation.screens.dashboard.addexpense

import android.app.DatePickerDialog
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
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.screens.dashboard.home.HomeTransactionItem
import com.example.truxpense.presentation.screens.dashboard.theme.DashboardDimens
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
    val categoryExpanded by vm.categoryExpanded.collectAsState()
    val selectedAccount by vm.selectedAccount.collectAsState()
    val accountExpanded by vm.accountExpanded.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    val isFormValid by vm.isFormValid.collectAsState()

    // Delegate UI to a parameterized content function (makes preview easier)
    val context = LocalContext.current

    fun openDatePicker() {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)
        val day = now.get(Calendar.DAY_OF_MONTH)

        val picker = DatePickerDialog(context, { _: android.widget.DatePicker, y: Int, m: Int, d: Int ->
            val cal = Calendar.getInstance()
            cal.set(y, m, d)
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            vm.setDate(sdf.format(cal.time))
            onDatePick()
        }, year, month, day)
        picker.show()
    }

    AddExpenseScreenContent(
        rawAmount = rawAmount,
        merchant = merchant,
        notes = notes,
        selectedCategory = selectedCategory,
        categoryExpanded = categoryExpanded,
        selectedAccount = selectedAccount,
        accountExpanded = accountExpanded,
        categories = vm.categories,
        accounts = vm.accountList,
        selectedDate = selectedDate,
        isFormValid = isFormValid,
        onRawChange = { vm.setRawAmount(it) },
        onMerchantChange = { vm.setMerchant(it) },
        onNotesChange = { vm.setNotes(it) },
        onSetCategoryExpanded = { vm.setCategoryExpanded(it) },
        onSelectCategory = { vm.selectCategory(it) },
        onSetAccountExpanded = { vm.setAccountExpanded(it) },
        onSelectAccount = { vm.selectAccount(it) },
        onDatePick = { openDatePicker() },
        onSave = { // build tx and forward to parent onSave
            val amt = vm.rawAmount.value.toDoubleOrNull() ?: 0.0
            val merchantVal = vm.merchant.value.ifBlank { "Expense" }
            val categoryVal = vm.selectedCategory.value ?: "Other"
            vm.saveExpense()
            val tx = HomeTransactionItem(
                id = UUID.randomUUID().toString(),
                title = merchantVal,
                category = categoryVal,
                amount = amt,
                currencyCode = "INR",
            )
            onSave(tx)
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
    categoryExpanded: Boolean,
    selectedAccount: String?,
    accountExpanded: Boolean,
    categories: List<String>,
    accounts: List<String>,
    selectedDate: String?,
    isFormValid: Boolean,
    onRawChange: (String) -> Unit,
    onMerchantChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSetCategoryExpanded: (Boolean) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSetAccountExpanded: (Boolean) -> Unit,
    onSelectAccount: (String) -> Unit,
    onDatePick: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(title = "Add expense", showBack = true, onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = DashboardDimens.screenPaddingH)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(DashboardDimens.spaceMd))

            // Amount card (separate card above details) — use same inner padding as DetailsCard
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DashboardDimens.cornerCard),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = DashboardDimens.cardElevation),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(DashboardDimens.cardPaddingComp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AmountInputZone(rawAmount = rawAmount, onRawChange = onRawChange)
                }
            }

            Spacer(Modifier.height(DashboardDimens.spaceLg))

            // Details card (category, merchant, account, date)
            DetailsCard(
                selectedCategory = selectedCategory,
                categoryExpanded = categoryExpanded,
                categories = categories,
                onCategoryToggle = { onSetCategoryExpanded(!categoryExpanded) },
                onCategorySelect = onSelectCategory,
                onCategoryDismiss = { onSetCategoryExpanded(false) },
                merchant = merchant,
                onMerchantChange = onMerchantChange,
                selectedAccount = selectedAccount,
                accountExpanded = accountExpanded,
                accounts = accounts,
                onAccountToggle = { onSetAccountExpanded(!accountExpanded) },
                onAccountSelect = onSelectAccount,
                onAccountDismiss = { onSetAccountExpanded(false) },
                selectedDate = selectedDate,
                onDateClick = onDatePick,
            )

            Spacer(Modifier.height(DashboardDimens.spaceXxl))

            // Notes
            NotesCard(notes = notes, onChange = onNotesChange)

            Spacer(Modifier.height(DashboardDimens.spaceXxl))

            // Action buttons
            SaveButton(enabled = isFormValid, onClick = onSave)
            Spacer(Modifier.height(DashboardDimens.spaceMd))
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
    // Single centered text field for amount input. No border and no background by design.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
    ) {
        Text(
            text = "How much did you spend?",
            modifier = Modifier,
            fontSize = DashboardDimens.textInput,
            color = MaterialTheme.colorScheme.tertiary
        )

        Spacer(Modifier.height(DashboardDimens.spaceLg))

        // Focus and keyboard controller moved here so the full-width box can request focus.
        val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

        // Measurement helpers to size the composed currency+amount tightly
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
            modifier = Modifier
                .fillMaxWidth()
                // Make the whole row tappable: request focus and show keyboard without ripple
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                },
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = rawAmount,
                onValueChange = onRawChange,
                singleLine = true,
                modifier = Modifier
                    .widthIn(min = DashboardDimens.iconButtonMd)
                    .width(finalWidth)
                    .focusRequester(focusRequester),
                textStyle = displayStyle,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                decorationBox = { inner ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.wrapContentWidth()) {
                        // currency symbol immediately before the inner content
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
                }
            )
        }
    }

    Spacer(Modifier.height(DashboardDimens.spaceXs))
}

// ─── ② Details Card ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsCard(
    selectedCategory: String?,
    categoryExpanded: Boolean,
    categories: List<String>,
    onCategoryToggle: () -> Unit,
    onCategorySelect: (String) -> Unit,
    onCategoryDismiss: () -> Unit,
    merchant: String,
    onMerchantChange: (String) -> Unit,
    selectedAccount: String?,
    accountExpanded: Boolean,
    accounts: List<String>,
    onAccountToggle: () -> Unit,
    onAccountSelect: (String) -> Unit,
    onAccountDismiss: () -> Unit,
    selectedDate: String?,
    onDateClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Row 1: Category ──────────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { if (it) onCategoryToggle() else onCategoryDismiss() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                DetailRow(
                    leadingIcon = {
                        val selTrim = selectedCategory?.trim().orEmpty()
                        val hasIcon = selTrim.isNotEmpty() && selTrim.lowercase() !in listOf("other", "others")
                        val iconRes = if (hasIcon) iconForCategory(selectedCategory) else R.drawable.category_icon
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = if (selectedCategory != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(DashboardDimens.iconMd),
                        )
                    },
                    label = "Category",
                    trailingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.drop_down_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(DashboardDimens.iconMd),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                ) {
                    Text(
                        text = selectedCategory ?: "Select category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedCategory == null) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onBackground,
                    )
                }

                // ExposedDropdownMenu naturally anchors BELOW the menuAnchor composable
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = onCategoryDismiss,
                    modifier = Modifier.exposedDropdownSize(false),
                ) {
                    if (categories.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No categories found", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { onCategoryDismiss() },
                        )
                    } else {
                        categories.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (item == selectedCategory) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onBackground,
                                    )
                                },
                                onClick = { onCategorySelect(item); onCategoryDismiss() },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(iconForCategory(item)),
                                        contentDescription = null,
                                        tint = if (item == selectedCategory) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(DashboardDimens.iconMd),
                                    )
                                },
                                trailingIcon = {
                                    if (item == selectedCategory) {
                                        Icon(
                                            painter = painterResource(R.drawable.drop_down_icon),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(DashboardDimens.iconSm),
                                        )
                                    }
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.onBackground,
                                ),
                            )
                        }
                    }
                }
            }

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
            ExposedDropdownMenuBox(
                expanded = accountExpanded,
                onExpandedChange = { if (it) onAccountToggle() else onAccountDismiss() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                DetailRow(
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.bills),
                            contentDescription = null,
                            tint = if (selectedAccount != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(DashboardDimens.iconMd),
                        )
                    },
                    label = "Account",
                    trailingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.drop_down_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(DashboardDimens.iconMd),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                ) {
                    Text(
                        text = selectedAccount ?: "Select account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedAccount == null) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onBackground,
                    )
                }

                ExposedDropdownMenu(
                    expanded = accountExpanded,
                    onDismissRequest = onAccountDismiss,
                    modifier = Modifier.exposedDropdownSize(false),
                ) {
                    accounts.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (item == selectedAccount) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onBackground,
                                )
                            },
                            onClick = { onAccountSelect(item); onAccountDismiss() },
                            trailingIcon = {
                                if (item == selectedAccount) {
                                    Icon(
                                        painter = painterResource(R.drawable.drop_down_icon),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(DashboardDimens.iconSm),
                                    )
                                }
                            },
                        )
                    }
                }
            }

            RowDivider()

            // ── Row 4: Date ───────────────────────────────────────────────────
            DetailRow(
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.calender),
                        contentDescription = null,
                        tint = if (selectedDate != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(DashboardDimens.iconMd),
                    )
                },
                label = "Date",
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.calender),
                        contentDescription = "Pick date",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(DashboardDimens.iconMd),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDateClick,
                    ),
            ) {
                Text(
                    text = selectedDate ?: "Pick a date",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedDate != null) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurfaceVariant,
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
        modifier = modifier
            .fillMaxWidth()
            .height(DashboardDimens.detailRowHeight)
            .padding(horizontal = DashboardDimens.screenPaddingH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
    ) {
        // Leading icon — consistent 20 dp slot
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

        // Optional trailing icon — consistent 20 dp slot
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

/** Thin divider with screen-edge inset, matching the leading-icon column offset. */
@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = DashboardDimens.screenPaddingH + DashboardDimens.iconMd + DashboardDimens.spaceMd),
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = DashboardDimens.dividerThin,
    )
}


// ─── ③ Notes card, buttons, preview and helpers ───────────────────────────────

@Composable
private fun NotesCard(notes: String, onChange: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DashboardDimens.screenPaddingH, vertical = DashboardDimens.spaceLg),
        ) {
            // Header row — icon + label, mirroring the DetailRow leading-icon style
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

            // Text input — borderless, blends into the card surface
            BasicTextField(
                value = notes,
                onValueChange = onChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DashboardDimens.cornerChip))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = DashboardDimens.spaceLg, vertical = DashboardDimens.spaceMdL)
                    .defaultMinSize(minHeight = DashboardDimens.inputMinHeight),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                maxLines = 4,
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
        }
    }
}

@Composable
private fun SaveButton(enabled: Boolean, onClick: () -> Unit) {
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
        Text(text = "Save expense", fontWeight = FontWeight.SemiBold, fontSize = DashboardDimens.textXl)
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
        categoryExpanded = false,
        selectedAccount = "Cash",
        accountExpanded = false,
        categories = listOf("Food", "Transport", "Shopping", "Bills", "Health", "Entertainment", "Groceries", "Other"),
        accounts = listOf("HDFC Bank", "SBI", "ICICI Bank", "Axis Bank", "Cash", "UPI"),
        selectedDate = null,
        isFormValid = true,
        onRawChange = {},
        onMerchantChange = {},
        onNotesChange = {},
        onSetCategoryExpanded = {},
        onSelectCategory = {},
        onSetAccountExpanded = {},
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