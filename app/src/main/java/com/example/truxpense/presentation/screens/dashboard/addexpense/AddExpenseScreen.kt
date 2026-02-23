package com.example.truxpense.presentation.screens.dashboard.addexpense

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.home.HomeTransactionItem
import com.example.truxpense.presentation.screens.dashboard.theme.DashboardDimens
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onSave: (HomeTransactionItem) -> Unit = {},
    onCancel: () -> Unit = {},
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
        onDatePick = onDatePick,
        onSave = { // build tx and forward to parent onSave
            val amt = vm.rawAmount.value.toDoubleOrNull() ?: 0.0
            val merchantVal = vm.merchant.value.ifBlank { "Expense" }
            val categoryVal = vm.selectedCategory.value ?: "Other"
            val accountVal = vm.selectedAccount.value ?: ""
            // Persist via ViewModel
            vm.saveExpense(
                amount = amt,
                category = categoryVal,
                paymentMethod = accountVal,
                merchant = merchantVal
            )
            val tx = HomeTransactionItem(
                id = UUID.randomUUID().toString(),
                title = merchantVal,
                category = categoryVal,
                amount = amt,
                currencyCode = "INR",
            )
            onSave(tx)
        },
        onCancel = onCancel,
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
    onCancel: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add expense",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.back_icon),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = DashboardDimens.screenPaddingH)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(DashboardDimens.spaceMd))

            // Amount input zone
            AmountInputZone(rawAmount = rawAmount, onRawChange = onRawChange)

            Spacer(Modifier.height(DashboardDimens.spaceXl))

            // Details card (category uses shared CategoryDropdown component)
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

            Spacer(Modifier.height(DashboardDimens.spaceXl))

            // Notes
            NotesCard(notes = notes, onChange = onNotesChange)

            Spacer(Modifier.height(DashboardDimens.spaceXxl))

            // Action buttons
            SaveButton(enabled = isFormValid, onClick = onSave)
            Spacer(Modifier.height(DashboardDimens.spaceMd))
            CancelButton(onClick = onCancel)
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
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "How much did you spend?",
            modifier = Modifier,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.tertiary
        )

        Spacer(Modifier.height(DashboardDimens.spaceLg))

        // Single BasicTextField whose decoration composes the currency symbol + inner text
        // Focus and keyboard controller moved here so the full-width box can request focus.
        val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

        // Measurement helpers to size the composed currency+amount tightly
        val textMeasurer = rememberTextMeasurer()
        val displayStyle = MaterialTheme.typography.displaySmall.copy(
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
            textAlign = TextAlign.Center,
        )
        val amountText = rawAmount.ifEmpty { "0" }
        val currencyText = "₹"
        val amountLayout = textMeasurer.measure(AnnotatedString(amountText), style = displayStyle)
        val currencyLayout = textMeasurer.measure(AnnotatedString(currencyText), style = displayStyle)
        val totalPx = amountLayout.size.width + currencyLayout.size.width
        val density = LocalDensity.current
        val totalDp = with(density) { totalPx.toDp() }
        val finalWidth = totalDp + 6.dp

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
                    .widthIn(min = 40.dp)
                    .width(finalWidth)
                    .focusRequester(focusRequester),
                textStyle = displayStyle,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.wrapContentWidth()) {
                        // currency symbol immediately before the inner content
                        Text(
                            text = currencyText,
                            style = displayStyle,
                        )

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.wrapContentWidth()) {
                            if (rawAmount.isEmpty()) {
                                Text(
                                    text = "0",
                                    style = displayStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    modifier = Modifier.padding(end = 4.dp)
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
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(DashboardDimens.spaceXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPaddingComp)) {
            // Row 1 — Category selector (inline, no label/bg/border)
            Box(
                modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { if (!categoryExpanded) onCategoryToggle() else onCategoryDismiss() }
                .padding(horizontal = rowPadH, vertical = rowPadV)
            ) {
                // measure the anchor width so the dropdown can match the card width
                val anchorWidthPx = remember { mutableStateOf(0) }
                Box(
                    modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { anchorWidthPx.value = it.size.width }
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { if (!categoryExpanded) onCategoryToggle() else onCategoryDismiss() }
                    .padding(horizontal = rowPadH, vertical = rowPadV)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val selTrim = selectedCategory?.trim().orEmpty()
                            val showIcon =
                                selTrim.isNotEmpty() && selTrim.lowercase() != "other" && selTrim.lowercase() != "others"
                            if (showIcon) {
                                Icon(
                                    painter = painterResource(iconForCategory(selectedCategory!!)),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(DashboardDimens.iconMd)
                                )
                                Spacer(Modifier.width(DashboardDimens.spaceMd))
                            }

                            Text(
                                text = selectedCategory ?: "Select category",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selectedCategory.isNullOrEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                            )
                        }

                        Icon(
                            painter = painterResource(R.drawable.drop_down_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(DashboardDimens.iconMd)
                        )
                    }

                    // Dropdown menu rendered in a popup so it doesn't affect measurement
                    // compute width in dp from measured pixels
                    val menuWidthDp = with(LocalDensity.current) { anchorWidthPx.value.toDp() }
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = onCategoryDismiss,
                        modifier = Modifier.width(menuWidthDp).padding(top = 8.dp),
                        offset = DpOffset(0.dp, 0.dp)
                    ) {
                        if (categories.isEmpty()) {
                            DropdownMenuItem(text = { Text("No categories found") }, onClick = { onCategoryDismiss() })
                        } else {
                            categories.forEach { item ->
                                val trimmed = item.trim().lowercase()
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = item,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    },
                                    onClick = {
                                        onCategorySelect(item)
                                        onCategoryDismiss()
                                    },
                                    trailingIcon = {
                                        if (trimmed != "other" && trimmed != "others") {
                                            Icon(
                                                painter = painterResource(iconForCategory(item)),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onBackground,
                                                modifier = Modifier.size(DashboardDimens.iconMd)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Row 2 — Merchant text input
            TextInputRow(
                value = merchant,
                onChange = onMerchantChange,
            )


            // Row 3 — Account dropdown
            DropdownRow(
                label = selectedAccount ?: "Account",
                isHint = selectedAccount == null,
                expanded = accountExpanded,
                items = accounts,
                onToggle = onAccountToggle,
                onSelect = onAccountSelect,
                onDismiss = onAccountDismiss,
            )


            // Row 4 — Date picker trigger
            DateRow(value = selectedDate, onClick = onDateClick)
        }
    }
}

// ─── Card row primitives ──────────────────────────────────────────────────────

private val rowPadH = 16.dp
private val rowPadV = 15.dp

@Composable
private fun DropdownRow(
    label: String,
    isHint: Boolean,
    expanded: Boolean,
    items: List<String>,
    onToggle: () -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onToggle,
                )
                .padding(horizontal = rowPadH, vertical = rowPadV),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isHint) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(DashboardDimens.iconLg),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = { onSelect(item) },
                )
            }
        }
    }
}

@Composable
private fun TextInputRow(
    value: String,
    onChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = rowPadH, vertical = rowPadV),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onBackground,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(
                    text = "Enter merchant name",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            inner()
        },
    )
}

@Composable
private fun DateRow(value: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = rowPadH, vertical = rowPadV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = value ?: "Date",
            style = MaterialTheme.typography.bodyMedium,
            color = if (value != null) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            painter = painterResource(R.drawable.calender),
            contentDescription = "Pick date",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(DashboardDimens.iconMd),
        )
    }
}


// ─── ③ Notes Card ────────────────────────────────────────────────────────────

@Composable
private fun NotesCard(notes: String, onChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DashboardDimens.spaceXl))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(DashboardDimens.spaceXl))
            .padding(
                horizontal = DashboardDimens.screenPaddingH,
                vertical = DashboardDimens.spaceLg,
            ),
    ) {
        // Notes header row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.add_notes_icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(DashboardDimens.iconSm),
            )
            Spacer(Modifier.width(DashboardDimens.spaceMd))
            Text(
                text = "Notes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.height(DashboardDimens.spaceMd))

        // Note input field
        BasicTextField(
            value = notes,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DashboardDimens.spaceMd))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(DashboardDimens.spaceMd))
                .padding(
                    horizontal = DashboardDimens.spaceLg,
                    vertical = DashboardDimens.spaceMdL,
                )
                .defaultMinSize(minHeight = 52.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onBackground,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                if (notes.isEmpty()) {
                    Text(
                        text = "Add a few notes to help you later",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                inner()
            },
        )
    }
}

// ─── ④ Action Buttons ────────────────────────────────────────────────────────

@Composable
private fun SaveButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(DashboardDimens.buttonHeight),
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp),
    ) {
        Text(
            text = "Save expense",
            fontWeight = FontWeight.SemiBold,
            fontSize = DashboardDimens.textXl,
        )
    }
}

@Composable
private fun CancelButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(DashboardDimens.buttonHeight),
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Text(
            text = "Cancel",
            fontWeight = FontWeight.Medium,
            fontSize = DashboardDimens.textXl,
        )
    }
}

// ─── Preview ─────────────────────────────────────────────────────────────────

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
        onCancel = {},
        onBack = {},
    )
}

// Local mapping for category icon resources (mirror AddBudgetViewModel.iconForCategory)
private fun iconForCategory(category: String): Int = when (category.trim().lowercase()) {
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
