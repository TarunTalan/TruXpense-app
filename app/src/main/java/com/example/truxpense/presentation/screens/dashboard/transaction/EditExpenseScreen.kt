package com.example.truxpense.presentation.screens.dashboard.transaction


import android.app.DatePickerDialog
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.presentation.screens.dashboard.addexpense.AddExpenseScreenContent
import com.example.truxpense.presentation.theme.DashboardDimens
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun EditExpenseScreen(
    transactionId: String,
    onSaved: () -> Unit = {},
    onCancel: () -> Unit = {},
    vm: EditExpenseViewModel = hiltViewModel(),
) {
    // Load the transaction once
    LaunchedEffect(transactionId) { vm.loadTransaction(transactionId) }

    // Navigate once the update finishes
    LaunchedEffect(Unit) { vm.updateComplete.collect { onSaved() } }

    val rawAmount    by vm.rawAmount.collectAsState()
    val merchant     by vm.merchant.collectAsState()
    val notes        by vm.notes.collectAsState()
    val selectedCat  by vm.selectedCategory.collectAsState()
    val selectedAcc  by vm.selectedAccount.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    val isFormValid  by vm.isFormValid.collectAsState()
    val isSaving     by vm.isSaving.collectAsState()

    val context = LocalContext.current

    // Capture theme ARGB values at composable scope
    val surfaceArgb = MaterialTheme.colorScheme.surface.toArgb()
    val onPrimaryArgb = MaterialTheme.colorScheme.onPrimary.toArgb()
    val onBackgroundArgb = MaterialTheme.colorScheme.onBackground.toArgb()

    fun openDatePicker() {
        val now = Calendar.getInstance()
        val picker = DatePickerDialog(
            context,
            { _, y, m, d ->
                val cal = Calendar.getInstance().apply { set(y, m, d) }
                vm.setDate(SimpleDateFormat("MMM d", Locale.getDefault()).format(cal.time))
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH),
        )
        picker.setOnShowListener {
            try {
                val window = picker.window
                window?.setBackgroundDrawable(ColorDrawable(surfaceArgb))
                picker.getButton(DatePickerDialog.BUTTON_POSITIVE)?.setTextColor(onPrimaryArgb)
                picker.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setTextColor(onBackgroundArgb)
            } catch (_: Exception) { }
        }
        picker.show()
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
        isFormValid      = isFormValid && !isSaving,
        onRawChange      = vm::setRawAmount,
        onMerchantChange = vm::setMerchant,
        onNotesChange    = vm::setNotes,
        onSelectCategory = vm::selectCategory,
        onSelectAccount  = vm::selectAccount,
        onDatePick       = { openDatePicker() },
        onSave           = vm::saveChanges,
        onBack           = onCancel,
        screenTitle      = "Edit expense",
        saveLabel        = if (isSaving) "Saving…" else "Save changes",
        extraBottomContent = {
            OutlinedButton(
                onClick  = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DashboardDimens.buttonHeight),
                shape  = RoundedCornerShape(DashboardDimens.cornerCard),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
            ) {
                Text(
                    text       = "Cancel",
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
    )
}