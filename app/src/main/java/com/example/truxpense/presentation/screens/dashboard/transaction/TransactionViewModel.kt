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
            TransactionPeriod.YEAR  -> if (offset == 0) "This year"  else "${2026 + offset}"
            TransactionPeriod.MONTH -> if (offset == 0) "This month" else monthOffset(offset)
            TransactionPeriod.WEEK  -> if (offset == 0) "This week"  else weekOffset(offset)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "This year")

    val canNavBack: StateFlow<Boolean>    = _navOffset.map { it > -24 }.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val canNavForward: StateFlow<Boolean> = _navOffset.map { it < 0  }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun navBack()    { _navOffset.value-- }
    fun navForward() { if (_navOffset.value < 0) _navOffset.value++ }

    // ── Search ────────────────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    // ── Filters ───────────────────────────────────────────────────────────────

    private val _activeFilters = MutableStateFlow<Set<TransactionFilter>>(emptySet())
    val activeFilters: StateFlow<Set<TransactionFilter>> = _activeFilters.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _paymentMethod = MutableStateFlow<String?>(null)
    val paymentMethod: StateFlow<String?> = _paymentMethod.asStateFlow()

    fun toggleFilter(filter: TransactionFilter) {
        _activeFilters.update { current ->
            if (filter in current) {
                when (filter) {
                    TransactionFilter.CATEGORY -> _selectedCategory.value = null
                    TransactionFilter.ACCOUNT  -> _paymentMethod.value = null
                }
                current - filter
            } else current + filter
        }
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
        _activeFilters.update { current ->
            if (category != null) current + TransactionFilter.CATEGORY else current - TransactionFilter.CATEGORY
        }
    }

    fun setPaymentMethod(method: String?) {
        _paymentMethod.value = method
        _activeFilters.update { current ->
            if (method != null) current + TransactionFilter.ACCOUNT else current - TransactionFilter.ACCOUNT
        }
    }

    // ── Month total dropdown ──────────────────────────────────────────────────

    private val _totalExpanded = MutableStateFlow(false)
    val totalExpanded: StateFlow<Boolean> = _totalExpanded.asStateFlow()

    fun toggleTotalExpanded() { _totalExpanded.update { !it } }

    // ── Data from Room ────────────────────────────────────────────────────────

    private val _rawTransactions: StateFlow<List<TransactionItem>> =
        repository.transactions.map { list ->
            list.sortedByDescending { it.timestamp }.map { t ->
                TransactionItem(
                    id            = t.id,
                    merchant      = t.merchant,
                    category      = t.category,
                    timeLabel     = formatRelativeTime(t.timestamp),
                    amount        = -t.amount,          // negative = expense
                    paymentMethod = t.paymentMethod,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableCategories: StateFlow<List<String>> =
        _rawTransactions.map { it.map { tx -> tx.category }.distinct() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availablePaymentMethods: StateFlow<List<String>> =
        _rawTransactions.map { it.map { tx -> tx.paymentMethod }.distinct() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Multi-select categories for filtering (used by dialog in TransactionScreen)
    private val _selectedCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategories: StateFlow<Set<String>> = _selectedCategories.asStateFlow()

    fun toggleCategorySelection(category: String) {
        _selectedCategories.update { current ->
            if (category in current) current - category else current + category
        }
    }

    fun clearSelectedCategories() {
        _selectedCategories.value = emptySet()
    }

    /** Filtered + searched flat list. */
    val transactions: StateFlow<List<TransactionItem>> = combine(
        _rawTransactions, _selectedCategory, _paymentMethod, _searchQuery, _selectedCategories
    ) { list, category, payment, query, selectedCats ->
        val filtered = list.filter { tx ->
            (category == null || tx.category == category) &&
                    (payment  == null || tx.paymentMethod == payment) &&
                    (query.isBlank() || tx.merchant.contains(query, ignoreCase = true) ||
                            tx.category.contains(query, ignoreCase = true))
        }
        // If multi-category selection is active, further filter
        if (selectedCats.isEmpty()) filtered else filtered.filter { it.category in selectedCats }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Grouped for the UI list. */
    val monthGroups: StateFlow<List<TransactionMonthGroup>> =
        transactions.map { buildMonthGroups(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totalSpent: StateFlow<Double> =
        transactions.map { it.sumOf { tx -> -tx.amount } }   // amounts are negative
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildMonthGroups(items: List<TransactionItem>): List<TransactionMonthGroup> {
        if (items.isEmpty()) return emptyList()
        // Group by date label (today / yesterday / date) then by pseudo-month "Recent"
        val grouped = items.groupBy { it.timeLabel }
        val days = grouped.entries.map { (label, txs) ->
            TransactionDayGroup(dayLabel = label, items = txs)
        }
        val total = items.sumOf { -it.amount }
        return listOf(TransactionMonthGroup("Recent", total, days))
    }

    private fun formatRelativeTime(timestamp: Long): String {
        val now   = System.currentTimeMillis()
        val delta = now - timestamp
        val days  = delta / (1_000 * 60 * 60 * 24)
        return when {
            delta < 1_000 * 60 * 60      -> "Today"
            days == 1L                   -> "Yesterday"
            days < 7                     -> "$days days ago"
            else -> {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
                val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
                "${months[cal.get(java.util.Calendar.MONTH)]} ${cal.get(java.util.Calendar.DAY_OF_MONTH)}"
            }
        }
    }

    private fun monthOffset(offset: Int): String {
        val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        val base  = 1   // February = index 1
        val total = base + offset
        val year  = 2026 + (total / 12)
        val month = ((total % 12) + 12) % 12
        return "${months[month]} $year"
    }

    private fun weekOffset(offset: Int): String = "Week of ${16 + offset * 7} Feb"
}