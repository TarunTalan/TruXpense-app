package com.example.truxpense.presentation.screens.dashboard.expense

import com.example.truxpense.presentation.screens.dashboard.components.AppDatePickerDialog
import com.example.truxpense.presentation.screens.dashboard.components.AppTimePickerDialog
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseScreen(
    transactionId: String,
    onSaved: () -> Unit = {},
    onCancel: () -> Unit = {},
    vm: EditExpenseViewModel = hiltViewModel(),
) {
    LaunchedEffect(transactionId) { vm.loadTransaction(transactionId) }
    LaunchedEffect(Unit) { vm.updateComplete.collect { onSaved() } }

    val rawAmount    by vm.rawAmount.collectAsState()
    val merchant     by vm.merchant.collectAsState()
    val notes        by vm.notes.collectAsState()
    val selectedCat  by vm.selectedCategory.collectAsState()
    val selectedAcc  by vm.selectedAccount.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    val selectedTime by vm.selectedTime.collectAsState()
    val isFormValid  by vm.isFormValid.collectAsState()
    val isSaving     by vm.isSaving.collectAsState()

    // ── Compose Material3 date / time pickers (theme-aware, light + dark) ────
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
                    val cal = Calendar.getInstance().apply { timeInMillis = it }
                    vm.setDate(SimpleDateFormat("MMM d", Locale.getDefault()).format(cal.time))
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
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    set(Calendar.MINUTE, timePickerState.minute)
                }
                vm.setTime(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time))
                showTimePicker = false
            },
        )
    }

    AddExpenseScreenContent(
        rawAmount        = rawAmount,
        merchant         = merchant,
        notes            = notes,
        selectedCategory = selectedCat,
        selectedAccount  = selectedAcc,
        categories       = vm.categories,
        accounts         = vm.accountList,
        selectedDate     = selectedDate,
        selectedTime     = selectedTime,
        isFormValid      = isFormValid && !isSaving,
        onRawChange      = vm::setRawAmount,
        onMerchantChange = vm::setMerchant,
        onNotesChange    = vm::setNotes,
        onSelectCategory = vm::selectCategory,
        onSelectAccount  = vm::selectAccount,
        onDatePick       = { showDatePicker = true },
        onTimePick       = { showTimePicker = true },
        onSave           = vm::saveChanges,
        onBack           = onCancel,
        screenTitle      = "Edit expense",
        saveLabel        = if (isSaving) "Saving…" else "Save changes",
    )
}
