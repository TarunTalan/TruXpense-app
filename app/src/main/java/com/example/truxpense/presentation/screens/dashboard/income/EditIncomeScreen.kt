package com.example.truxpense.presentation.screens.dashboard.income

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.drawable.ColorDrawable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.graphics.drawable.toDrawable
import com.example.truxpense.presentation.screens.dashboard.expense.AddExpenseScreenContent
import com.example.truxpense.presentation.screens.dashboard.components.iconForIncomeSource
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EditIncomeScreen(
    incomeId: String,
    onSaved: () -> Unit = {},
    onCancel: () -> Unit = {},
    vm: EditIncomeViewModel = hiltViewModel(),
) {
    LaunchedEffect(incomeId) { vm.loadIncome(incomeId) }
    LaunchedEffect(Unit) { vm.updateComplete.collect { onSaved() } }

    val rawAmount     by vm.rawAmount.collectAsState()
    val sourceName    by vm.sourceName.collectAsState()
    val notes         by vm.notes.collectAsState()
    val selectedSrc   by vm.selectedSource.collectAsState()
    val selectedAcc   by vm.selectedAccount.collectAsState()
    val selectedDate  by vm.selectedDate.collectAsState()
    val selectedTime  by vm.selectedTime.collectAsState()
    val isFormValid   by vm.isFormValid.collectAsState()
    val isSaving      by vm.isSaving.collectAsState()

    val context          = LocalContext.current
    val surfaceArgb      = MaterialTheme.colorScheme.surface.toArgb()
    val onPrimaryArgb    = MaterialTheme.colorScheme.onPrimary.toArgb()
    val onBackgroundArgb = MaterialTheme.colorScheme.onBackground.toArgb()

    fun openDatePicker() {
        val now = Calendar.getInstance()
        val picker = DatePickerDialog(
            context,
            { _, y, m, d ->
                val cal = Calendar.getInstance().apply { set(y, m, d) }
                vm.setDate(SimpleDateFormat("MMM d", Locale.getDefault()).format(cal.time))
            },
            now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH),
        )
        picker.setOnShowListener {
            try {
                picker.window?.setBackgroundDrawable(surfaceArgb.toDrawable())
                picker.getButton(DatePickerDialog.BUTTON_POSITIVE)?.setTextColor(onPrimaryArgb)
                picker.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setTextColor(onBackgroundArgb)
            } catch (_: Exception) {}
        }
        picker.show()
    }

    fun openTimePicker() {
        val now = Calendar.getInstance()
        val dialog = TimePickerDialog(
            context,
            { _, hour, minute ->
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
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
            } catch (_: Exception) {}
        }
        dialog.show()
    }

    AddExpenseScreenContent(
        rawAmount            = rawAmount,
        merchant             = sourceName,
        notes                = notes,
        selectedCategory     = selectedSrc,
        selectedAccount      = selectedAcc,
        categories           = vm.sourceOptions,
        accounts             = vm.accountList,
        selectedDate         = selectedDate,
        selectedTime         = selectedTime,
        isFormValid          = isFormValid && !isSaving,
        onRawChange          = vm::setRawAmount,
        onMerchantChange     = vm::setSourceName,
        onNotesChange        = vm::setNotes,
        onSelectCategory     = vm::selectSource,
        onSelectAccount      = vm::selectAccount,
        onDatePick           = { openDatePicker() },
        onTimePick           = { openTimePicker() },
        onSave               = vm::saveChanges,
        onBack               = onCancel,
        screenTitle          = "Edit income",
        saveLabel            = if (isSaving) "Saving…" else "Save changes",
        categoryLabel        = "Income source",
        merchantLabel        = "Source name",
        merchantPlaceholder  = "e.g. Acme Corp, Client name...",
        paymentMethodOptions = vm.accountList,
        paymentMethodLabel   = "Received via",
        categoryIconResolver = ::iconForIncomeSource,
    )
}
