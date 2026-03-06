package com.example.truxpense.presentation.screens.dashboard.expense

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.presentation.screens.dashboard.components.*
import com.example.truxpense.presentation.screens.dashboard.home.HomeTransactionItem
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.utils.AppCategories
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
    /** Label shown above the category/source picker grid. */
    categoryLabel: String = "Category",
    /** Label for the free-text merchant / source-name field. */
    merchantLabel: String = "Merchant name",
    /** Placeholder text inside the merchant / source-name field. */
    merchantPlaceholder: String = "e.g. Zomato, Swiggy, Ub...",
    /** Options shown in the payment method dropdown. Defaults to standard expense methods. */
    paymentMethodOptions: List<String> = paymentMethods,
    /** Label shown above the payment method field. */
    paymentMethodLabel: String = "Payment method",
    /** Icon resolver for the category/source picker grid. Defaults to expense category icons. */
    categoryIconResolver: (String) -> Int = ::iconForCategory,
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
                label = categoryLabel,
                iconResolver = categoryIconResolver,
            )

            LabeledTextField(
                label = merchantLabel,
                placeholder = merchantPlaceholder,
                value = merchant,
                onValueChange = onMerchantChange,
            )

            if (accounts.isNotEmpty()) {
                PaymentMethodField(
                    selectedAccount = selectedAccount,
                    onSelect = onSelectAccount,
                    options = paymentMethodOptions,
                    fieldLabel = paymentMethodLabel,
                )
            }

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