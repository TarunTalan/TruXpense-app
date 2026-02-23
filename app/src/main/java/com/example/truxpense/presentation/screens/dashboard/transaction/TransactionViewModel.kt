package com.example.truxpense.presentation.screens.dashboard.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.dashboard.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: ExpenseRepository,
) : ViewModel() {

    // ── Period tab ────────────────────────────────────────────────────────────

    private val _selectedPeriod = MutableStateFlow(TransactionPeriod.YEAR)
    val selectedPeriod: StateFlow<TransactionPeriod> = _selectedPeriod.asStateFlow()

    fun selectPeriod(period: TransactionPeriod) {
        _selectedPeriod.value = period
        _navOffset.value = 0
    }

    // ── Date navigator ────────────────────────────────────────────────────────

    private val _navOffset = MutableStateFlow(0)

    val periodNavLabel: StateFlow<String> = combine(_selectedPeriod, _navOffset) { period, offset ->
        when (period) {
            TransactionPeriod.YEAR -> if (offset == 0) "This year" else "${2026 + offset}"
            TransactionPeriod.MONTH -> if (offset == 0) "This month" else monthOffset(offset)
            TransactionPeriod.WEEK -> if (offset == 0) "This week" else weekOffset(offset)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "This year")

    val canNavBack: StateFlow<Boolean> =
        _navOffset.map { it > -24 }.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val canNavForward: StateFlow<Boolean> =
        _navOffset.map { it < 0 }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun navBack() {
        _navOffset.value--
    }

    fun navForward() {
        if (_navOffset.value < 0) _navOffset.value++
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    // ── Category filter ───────────────────────────────────────────────────────

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    fun setCategory(category: String?) {
        _selectedCategory.value = category
    }

    // ── Payment method filter ─────────────────────────────────────────────────

    private val _paymentMethod = MutableStateFlow<String?>(null)
    val paymentMethod: StateFlow<String?> = _paymentMethod.asStateFlow()

    fun setPaymentMethod(method: String?) {
        _paymentMethod.value = method
    }

    // ── Clear all filters ─────────────────────────────────────────────────────

    fun clearAllFilters() {
        _selectedCategory.value = null
        _paymentMethod.value = null
        _searchQuery.value = ""
    }

    /** Number of active filters (for badge on the filter button). */
    val activeFilterCount: StateFlow<Int> = combine(_selectedCategory, _paymentMethod) { cat, pay ->
        listOfNotNull(cat, pay).size
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ── Month total dropdown ──────────────────────────────────────────────────

    private val _totalExpanded = MutableStateFlow(false)
    val totalExpanded: StateFlow<Boolean> = _totalExpanded.asStateFlow()

    fun toggleTotalExpanded() {
        _totalExpanded.update { !it }
    }

    // ── Raw data from Room ────────────────────────────────────────────────────

    private val _rawTransactions: StateFlow<List<TransactionItem>> = repository.transactions.map { list ->
        list.sortedByDescending { it.timestamp }.map { t ->
            TransactionItem(
                id = t.id,
                merchant = t.merchant,
                category = t.category,
                timeLabel = formatRelativeTime(t.timestamp),
                amount = -t.amount,
                paymentMethod = t.paymentMethod,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All unique categories found in the user's expense data. */
    val availableCategories: StateFlow<List<String>> =
        _rawTransactions.map { txs -> txs.map { it.category }.distinct().sorted() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All unique payment methods found in the user's expense data. */
    val availablePaymentMethods: StateFlow<List<String>> = _rawTransactions.map { txs ->
        txs.map { it.paymentMethod }.filter { it.isNotBlank() }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Total count before any filter/search (for the "X of Y" display). */
    val totalTransactionCount: StateFlow<Int> =
        _rawTransactions.map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Filtered + searched flat list — drives both [resultCount] and [monthGroups]. */
    val transactions: StateFlow<List<TransactionItem>> = combine(
        _rawTransactions, _selectedCategory, _paymentMethod, _searchQuery
    ) { list, category, payment, query ->
        list.filter { tx ->
            val matchesCategory = category == null || tx.category.equals(category, ignoreCase = true)
            val matchesPayment = payment == null || tx.paymentMethod.equals(payment, ignoreCase = true)
            val matchesSearch =
                query.isBlank() || tx.merchant.contains(query, ignoreCase = true) || tx.category.contains(
                    query, ignoreCase = true
                )
            matchesCategory && matchesPayment && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Count of results after applying all active filters + search. */
    val resultCount: StateFlow<Int> =
        transactions.map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Whether any filter or search is currently active. */
    val hasActiveFiltersOrSearch: StateFlow<Boolean> =
        combine(_selectedCategory, _paymentMethod, _searchQuery) { cat, pay, q ->
            cat != null || pay != null || q.isNotBlank()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Grouped for the UI list (derived from already-filtered [transactions]). */
    val monthGroups: StateFlow<List<TransactionMonthGroup>> =
        transactions.map { buildMonthGroups(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totalSpent: StateFlow<Double> = transactions.map { it.sumOf { tx -> -tx.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildMonthGroups(items: List<TransactionItem>): List<TransactionMonthGroup> {
        if (items.isEmpty()) return emptyList()
        val grouped = items.groupBy { it.timeLabel }
        val days = grouped.entries.map { (label, txs) ->
            TransactionDayGroup(dayLabel = label, items = txs)
        }
        val total = items.sumOf { -it.amount }
        return listOf(TransactionMonthGroup("Recent", total, days))
    }

    private fun formatRelativeTime(timestamp: Long): String {
        val delta = System.currentTimeMillis() - timestamp
        val days = delta / (1_000 * 60 * 60 * 24)
        return when {
            delta < 1_000 * 60 * 60 -> "Today"
            days == 1L -> "Yesterday"
            else -> {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
                val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                "${months[cal.get(java.util.Calendar.MONTH)]} ${cal.get(java.util.Calendar.DAY_OF_MONTH)}"
            }
        }
    }

    private fun monthOffset(offset: Int): String {
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val base = 1
        val total = base + offset
        val year = 2026 + (total / 12)
        val month = ((total % 12) + 12) % 12
        return "${months[month]} $year"
    }

    private fun weekOffset(offset: Int): String = "Week of ${16 + offset * 7} Feb"
}