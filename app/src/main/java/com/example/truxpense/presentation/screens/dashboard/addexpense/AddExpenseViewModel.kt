package com.example.truxpense.presentation.screens.dashboard.addexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddExpenseViewModel @Inject constructor() : ViewModel() {
    private val _amount = MutableStateFlow("₹5000")
    val amount: StateFlow<String> = _amount

    private val _category = MutableStateFlow("Food")
    val category: StateFlow<String> = _category

    private val _merchant = MutableStateFlow("Swiggy")
    val merchant: StateFlow<String> = _merchant

    private val _account = MutableStateFlow("HDFC Bank")
    val account: StateFlow<String> = _account

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes

    fun setNotes(v: String) { _notes.value = v }
    fun setAmount(v: String) { _amount.value = v }
    fun setCategory(v: String) { _category.value = v }
    fun setMerchant(v: String) { _merchant.value = v }
    fun setAccount(v: String) { _account.value = v }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            // TODO: persist expense via repository
            onSaved()
        }
    }
}

