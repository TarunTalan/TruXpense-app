package com.example.truxpense.presentation.screens.dashboard.sms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.sms.PendingTransactionRepository
import com.example.truxpense.data.sms.model.Category
import com.example.truxpense.data.sms.model.ParsedTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PendingTransactionsViewModel @Inject constructor(
    private val repository: PendingTransactionRepository
) : ViewModel() {

    val pendingTransactions: StateFlow<List<ParsedTransaction>> =
        repository.pendingTransactions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingCount: StateFlow<Int> =
        repository.pendingCount
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun confirm(id: String, category: Category? = null) {
        viewModelScope.launch { repository.confirm(id, category) }
    }

    fun reject(id: String) {
        viewModelScope.launch { repository.reject(id) }
    }

    fun confirmAll() {
        viewModelScope.launch {
            pendingTransactions.value.forEach { repository.confirm(it.id) }
        }
    }

    fun rejectAll() {
        viewModelScope.launch {
            pendingTransactions.value.forEach { repository.reject(it.id) }
        }
    }
}

