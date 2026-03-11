package com.example.truxpense.presentation.screens.dashboard.income

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.presentation.screens.dashboard.expense.AddExpenseScreenContent
import com.example.truxpense.presentation.screens.dashboard.components.iconForIncomeSource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AddIncomeScreen(
    vm: AddIncomeViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val rawAmount by vm.rawAmount.collectAsState()
    val sourceName by vm.sourceName.collectAsState()
    val notes by vm.notes.collectAsState()
    val selectedSource by vm.selectedSource.collectAsState()
    val selectedAccount by vm.selectedAccount.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    val selectedTime by vm.selectedTime.collectAsState()
    val isFormValid by vm.isFormValid.collectAsState()

    LaunchedEffect(Unit) {
        vm.saveComplete.collect { onBack() }
    }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

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
        dialog.show()
    }

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
        onDatePick = {
            focusManager.clearFocus()
            openDatePicker()
        },
        onTimePick = {
            focusManager.clearFocus()
            openTimePicker()
        },
        onSave = { vm.saveIncome() },
        onBack = onBack,
        screenTitle = "Add income",
        saveLabel = "Save income",
        categoryLabel = "Income source",
        merchantLabel = "Source name",
        merchantPlaceholder = "e.g. Acme Corp, Client name...",
        paymentMethodOptions = vm.accountList,
        paymentMethodLabel   = "Received via",
        categoryIconResolver = ::iconForIncomeSource,
    )
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "AddIncomeScreen")
@Composable
fun AddIncomeScreenPreview() {
    val incomeSources = listOf(
        "Salary", "Freelance", "Business", "Investment",
        "Gift", "Rental", "Refund", "Other",
    )
    MaterialTheme {
        AddExpenseScreenContent(
            rawAmount = "50000",
            merchant = "Acme Corp",
            notes = "",
            selectedCategory = "Salary",
            selectedAccount = "Bank Transfer",
            categories = incomeSources,
            accounts = listOf("Bank Transfer", "Cash", "UPI", "Card"),
            selectedDate = "Mar 6",
            selectedTime = "09:00 AM",
            isFormValid = true,
            onRawChange = {},
            onMerchantChange = {},
            onNotesChange = {},
            onSelectCategory = {},
            onSelectAccount = {},
            onDatePick = {},
            onTimePick = {},
            onSave = {},
            onBack = {},
            screenTitle = "Add income",
            saveLabel = "Save income",
            categoryLabel = "Income source",
            merchantLabel = "Source name",
            merchantPlaceholder = "e.g. Acme Corp, Client name...",
            paymentMethodOptions = listOf("Bank Transfer", "Cash", "UPI", "Card"),
            paymentMethodLabel   = "Received via",
            categoryIconResolver = ::iconForIncomeSource,
        )
    }
}
