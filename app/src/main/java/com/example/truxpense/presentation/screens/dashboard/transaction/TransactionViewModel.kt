package com.example.truxpense.presentation.screens.dashboard.transaction


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.dashboard.RepositoryProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject


@HiltViewModel
class TransactionsViewModel @Inject constructor() : ViewModel() {

    // ── Period tab ────────────────────────────────────────────────────────────

    private val _selectedPeriod = MutableStateFlow(TransactionPeriod.YEAR)
    val selectedPeriod: StateFlow<TransactionPeriod> = _selectedPeriod.asStateFlow()

    fun selectPeriod(period: TransactionPeriod) {
        _selectedPeriod.value = period
        _navOffset.value = 0   // reset nav when switching period
    }

    // ── Date navigator ───────────────────────────────────────────────────────

    private val _navOffset = MutableStateFlow(0)   // 0 = current, -1 = prev, +1 = next

    /**
     * Human-readable label for the navigator row.
     * Derived from period + offset — the composable never builds strings.
     */
    val periodNavLabel: StateFlow<String> = combine(_selectedPeriod, _navOffset) { period, offset ->
        when (period) {
            TransactionPeriod.YEAR -> if (offset == 0) "This year" else "${2026 + offset}"
            TransactionPeriod.MONTH -> if (offset == 0) "This month" else monthOffset(offset)
            TransactionPeriod.WEEK -> if (offset == 0) "This week" else weekOffset(offset)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "This year")

    val canNavBack: StateFlow<Boolean> = _navOffset.map { it > -24 }  // limit 2 years back
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val canNavForward: StateFlow<Boolean> = _navOffset.map { it < 0 }    // can't navigate into the future
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun navBack() {
        _navOffset.value--
    }

    fun navForward() {
        if (_navOffset.value < 0) _navOffset.value++
    }

    // ── Search ───────────────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    // ── Filters ─────────────────────────────────────────────────────────────

    private val _activeFilters = MutableStateFlow<Set<TransactionFilter>>(emptySet())
    val activeFilters: StateFlow<Set<TransactionFilter>> = _activeFilters.asStateFlow()

    fun toggleFilter(filter: TransactionFilter) {
        _activeFilters.update { current ->
            if (filter in current) {
                // removing the filter: also clear the selected value so filtering stops
                when (filter) {
                    TransactionFilter.CATEGORY -> _selectedCategories.value = emptySet()
                    TransactionFilter.ACCOUNT -> _paymentMethod.value = null
                }
                current - filter
            } else current + filter
        }
    }

    // Selected category / payment method for filtering (used by transactions combine)
    // multi-select categories (single source of truth for selected categories)
    private val _selectedCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategories: StateFlow<Set<String>> = _selectedCategories.asStateFlow()

    private val _paymentMethod = MutableStateFlow<String?>(null)

    // Expose selected filters as public state
    val paymentMethod: StateFlow<String?> = _paymentMethod.asStateFlow()

    // UI actions: allow selecting/toggling categories and selecting payment methods
    fun toggleCategorySelection(category: String) {
        _selectedCategories.update { current ->
            val next = if (category in current) current - category else current + category
            // update activeFilters accordingly
            if (next.isNotEmpty()) _activeFilters.update { it + TransactionFilter.CATEGORY } else _activeFilters.update { it - TransactionFilter.CATEGORY }
            next
        }
    }

    fun setSelectedCategories(categories: Set<String>) {
        _selectedCategories.value = categories
        _activeFilters.update { current -> if (categories.isNotEmpty()) current + TransactionFilter.CATEGORY else current - TransactionFilter.CATEGORY }
    }

    fun clearSelectedCategories() {
        _selectedCategories.value = emptySet()
        _activeFilters.update { it - TransactionFilter.CATEGORY }
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

    fun toggleTotalExpanded() {
        _totalExpanded.update { !it }
    }

    // ── Transaction data (single source) ──────────────────────────────────────

    private val repository = RepositoryProvider.expenseRepository

    // Internal raw list mapped from repo into TransactionItem models
    private val _rawTransactions: StateFlow<List<TransactionItem>> = repository.transactions.map { list ->
        list.sortedByDescending { it.timestamp }.map { t ->
            TransactionItem(
                id = t.id,
                merchant = t.merchant,
                category = t.category,
                timeLabel = "Now",
                amount = -t.amount,
                paymentMethod = t.paymentMethod,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Available category/payment method lists derived from raw transactions
    val availableCategories: StateFlow<List<String>> = _rawTransactions.map { list ->
        list.map { it.category }.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availablePaymentMethods: StateFlow<List<String>> = _rawTransactions.map { list ->
        list.map { it.paymentMethod }.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Exposed filtered transactions (category/payment/search)
    val transactions: StateFlow<List<TransactionItem>> = combine(
        _rawTransactions, _selectedCategories, _paymentMethod, _searchQuery
    ) { list, categories, payment, query ->
        list.filter { tx ->
            (categories.isEmpty() || tx.category in categories) && (payment == null || tx.paymentMethod == payment) && (query.isBlank() || tx.merchant.contains(
                query, true
            ) || tx.category.contains(query, true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month groups derived from the filtered flat `transactions` flow so that search/filter
    // changes are reflected immediately in the grouped UI.
    val monthGroups: StateFlow<List<TransactionMonthGroup>> = transactions.map { list ->
        buildMonthGroupsFromItems(list)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Build month/day groups from a flat list of TransactionItem (used after filtering)
    private fun buildMonthGroupsFromItems(items: List<TransactionItem>): List<TransactionMonthGroup> {
        if (items.isEmpty()) return emptyList()
        // For now we place everything under a single "Recent" month + "Today" day (keeps UI simple)
        val dayGroup = TransactionDayGroup("Today", items)
        val total = items.sumOf { it.amount }
        return listOf(TransactionMonthGroup("Recent", total, listOf(dayGroup)))
    }


    // leftover helpers for UI nav labels
    private fun monthOffset(offset: Int): String {
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val base = 1  // February = index 1
        val total = base + offset
        val year = 2026 + (total / 12)
        val month = ((total % 12) + 12) % 12
        return "${months[month]} $year"
    }

    private fun weekOffset(offset: Int): String = "Week of ${16 + offset * 7} Feb"
}