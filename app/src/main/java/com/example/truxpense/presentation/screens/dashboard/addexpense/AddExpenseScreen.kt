package com.example.truxpense.presentation.screens.dashboard.addexpense

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.drawable.ColorDrawable
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.CategoryPickerGrid
import com.example.truxpense.presentation.screens.dashboard.components.AmountInputCard
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.screens.dashboard.home.HomeTransactionItem
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.utils.AppCategories
import com.example.truxpense.presentation.utils.clearFocusOnTap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        val picker = DatePickerDialog(
            context,
            { _: android.widget.DatePicker, y: Int, m: Int, d: Int ->
                val cal = Calendar.getInstance().apply { set(y, m, d) }
                vm.setDate(SimpleDateFormat("MMM d", Locale.getDefault()).format(cal.time))
                onDatePick()
            },
            now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH),
        )
        picker.setOnShowListener {
            try {
                picker.window?.setBackgroundDrawable(ColorDrawable(surfaceArgb))
                picker.getButton(DatePickerDialog.BUTTON_POSITIVE)?.setTextColor(onPrimaryArgb)
                picker.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setTextColor(onBackgroundArgb)
            } catch (_: Exception) {
            }
        }
        picker.show()
    }

    fun openTimePicker() {
        val now = Calendar.getInstance()
        val dialog = TimePickerDialog(
            context,
            { _, hour, minute ->
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
                }
                vm.setTime(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time))
            },
            now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false,
        )
        dialog.setOnShowListener {
            try {
                dialog.window?.setBackgroundDrawable(ColorDrawable(surfaceArgb))
                dialog.getButton(TimePickerDialog.BUTTON_POSITIVE)?.setTextColor(onPrimaryArgb)
                dialog.getButton(TimePickerDialog.BUTTON_NEGATIVE)?.setTextColor(onBackgroundArgb)
            } catch (_: Exception) {
            }
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
            onSave(
                HomeTransactionItem(
                    id = UUID.randomUUID().toString(),
                    title = merchantVal, category = categoryVal,
                    amount = amt, currencyCode = "INR",
                )
            )
        },
        onBack = onBack,
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// SCREEN CONTENT
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class
)
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
    screenTitle: String = "Add expense",
    saveLabel: String? = null,
    extraBottomContent: (@Composable () -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible

    // When the system dismisses the keyboard (back gesture / done action),
    // clear focus so no field inadvertently re-summons it on recomposition.
    LaunchedEffect(isImeVisible) {
        if (!isImeVisible) focusManager.clearFocus(force = false)
    }

    Scaffold(
        // ── Zero out Scaffold's own window-inset handling so we control every
        //    edge ourselves.  This prevents double-inset application.
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(headerTitle = screenTitle, showBack = true, onBack = onBack) },

        // ── The bottomBar lifts above the keyboard via imePadding() and stays
        //    above the system gesture bar via navigationBarsPadding().
        //    Scaffold measures the resulting height and passes it back as
        //    innerPadding.bottom, so the scroll column shrinks correctly.
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()   // respects gesture-nav bar
                        .imePadding()              // rises above the keyboard
                        .padding(
                            horizontal = DashboardDimens.screenPaddingH,
                            vertical = 12.dp,
                        ),
                ) {
                    SaveButton(
                        enabled = isFormValid,
                        onClick = {
                            // Dismiss keyboard before saving so the transition is clean
                            focusManager.clearFocus()
                            onSave()
                        },
                        label = saveLabel ?: "Save expense",
                    )
                }
            }
        },
    ) { innerPadding ->
        // innerPadding.bottom is the live bottomBar height (grows as keyboard opens).
        // We do NOT add imePadding() here — that would double-count it.
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)            // tracks bottomBar height automatically
                .padding(horizontal = DashboardDimens.screenPaddingH).verticalScroll(rememberScrollState())
                .clearFocusOnTap(),               // tap outside a field → dismiss keyboard
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(2.dp))

            AmountInputCard(rawAmount = rawAmount, onRawChange = onRawChange)

            CategoryPickerGrid(
                categories = categories,
                selected = selectedCategory,
                onSelect = onSelectCategory,
            )

            MerchantField(value = merchant, onChange = onMerchantChange)

            PaymentMethodField(selectedAccount = selectedAccount, onSelect = onSelectAccount)

            DateTimeRow(
                selectedDate = selectedDate,
                selectedTime = selectedTime,
                // Clear focus (keyboard) before opening a system dialog — prevents
                // the dialog appearing behind a still-animating keyboard.
                onDateClick = {
                    focusManager.clearFocus()
                    onDatePick()
                },
                onTimeClick = {
                    focusManager.clearFocus()
                    onTimePick()
                },
            )

            NotesCard(notes = notes, onChange = onNotesChange)

            if (extraBottomContent != null) {
                extraBottomContent()
            }

            // Bottom guard — ensures the last card never sits flush against the
            // bottomBar even if innerPadding.bottom momentarily lags.
            Spacer(Modifier.height(8.dp))
        }
    }
}


// ─── ③ Merchant Name Field ────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MerchantField(value: String, onChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.bringIntoViewRequester(bringIntoView)) {
        Text(
            text = "Merchant name",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 6.dp),
        )

        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onBackground,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().height(48.dp).focusRequester(focusRequester).onFocusChanged { state ->
                isFocused = state.isFocused
                if (state.isFocused) {
                    scope.launch { delay(320); bringIntoView.bringIntoView() }
                }
            }.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer).border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp),
            ).padding(horizontal = 16.dp),
            decorationBox = { inner ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = "e.g. Zomato, Swiggy, Ub...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

// ─── ④ Date + Time Row ────────────────────────────────────────────────────────

@Composable
private fun DateTimeRow(
    selectedDate: String?,
    selectedTime: String?,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
) {
    val today = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()) }
    val displayDate = selectedDate ?: today
    val displayTime = selectedTime ?: remember {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Date
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Date",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onDateClick).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = displayDate, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                Icon(
                    painter = painterResource(R.drawable.calender),
                    contentDescription = "Pick date",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Time
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Time",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onTimeClick).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = displayTime, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                Icon(
                    painter = painterResource(R.drawable.time_),
                    contentDescription = "Pick time",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ─── ⑤ Payment Method Field ──────────────────────────────────────────────────

private val paymentMethods = listOf("Card", "Cash", "UPI", "Net Banking")

@Composable
private fun PaymentMethodField(
    selectedAccount: String?,
    onSelect: (String) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    val triggerWidthPx = remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val cornerRadius = 12.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Payment method",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 6.dp),
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp)
                    .onGloballyPositioned { triggerWidthPx.value = it.size.width }
                    .clip(RoundedCornerShape(cornerRadius)).background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(
                        width = if (expanded) 1.5.dp else 1.dp,
                        color = if (expanded) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(cornerRadius),
                    ).clickable { expanded = true }.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = selectedAccount ?: "Select payment method",
                    fontSize = 14.sp,
                    color = if (selectedAccount != null) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                val rotation by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    animationSpec = tween(200),
                    label = "chevron_rotation",
                )
                Icon(
                    painter = painterResource(R.drawable.drop_down_icon),
                    contentDescription = "Expand payment method",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = rotation },
                )
            }

            val menuWidthDp = with(density) { triggerWidthPx.value.toDp() }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(x = 0.dp, y = 4.dp),
                modifier = Modifier.width(menuWidthDp).clip(RoundedCornerShape(cornerRadius))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                paymentMethods.forEach { method ->
                    val isSelected = method == selectedAccount
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = method,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onBackground,
                            )
                        },
                        onClick = { onSelect(method); expanded = false },
                        trailingIcon = if (isSelected) ({
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }) else null,
                        colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

// ─── ⑥ Notes Card ─────────────────────────────────────────────────────────────

private const val NOTES_MAX_CHARS = 100

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotesCard(notes: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(notes.isNotEmpty()) }
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(bringIntoView).animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        ),
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add_notes_icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Notes",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Crossfade(targetState = expanded, label = "notes_icon") { isExpanded ->
                        if (isExpanded) {
                            Icon(
                                Icons.Filled.Close,
                                "Collapse notes",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                Icons.Filled.Add,
                                "Add notes",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                    expandFrom = Alignment.Top,
                ) + fadeIn(tween(220, delayMillis = 40)),
                exit = shrinkVertically(tween(180), Alignment.Top) + fadeOut(tween(150)),
            ) {
                Column {
                    BasicTextField(
                        value = notes,
                        onValueChange = { if (it.length <= NOTES_MAX_CHARS) onChange(it) },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 14.dp, vertical = 12.dp).defaultMinSize(minHeight = 64.dp)
                            .onFocusChanged { state ->
                                if (state.isFocused) {
                                    scope.launch { delay(320); bringIntoView.bringIntoView() }
                                }
                            },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                        ),
                        maxLines = 3,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            if (notes.isEmpty()) {
                                Text(
                                    "Add a few notes to help you later",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        },
                    )
                    Text(
                        text = "${notes.length}/$NOTES_MAX_CHARS",
                        fontSize = 11.sp,
                        color = if (notes.length >= NOTES_MAX_CHARS) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

// ─── Save Button ──────────────────────────────────────────────────────────────

@Composable
private fun SaveButton(
    enabled: Boolean,
    onClick: () -> Unit,
    label: String = "Save expense",
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp),
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun AddExpenseScreenPreview() {
    MaterialTheme {
        AddExpenseScreenContent(
            rawAmount = "", merchant = "", notes = "",
            selectedCategory = "Food", selectedAccount = null,
            categories = AppCategories.all, accounts = listOf("Card", "Cash", "UPI"),
            selectedDate = null, selectedTime = null, isFormValid = true,
            onRawChange = {}, onMerchantChange = {}, onNotesChange = {},
            onSelectCategory = {}, onSelectAccount = {},
            onDatePick = {}, onTimePick = {}, onSave = {}, onBack = {},
        )
    }
}