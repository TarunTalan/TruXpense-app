package com.example.truxpense.presentation.screens.dashboard.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.dashboard.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    init {
        // If the dashboard sets a 'preselectCategory' in the savedStateHandle before or after
        // navigating to the Transactions tab, observe it and apply the category filter once.
        viewModelScope.launch {
            savedStateHandle.getStateFlow<String?>("preselectCategory", null).filterNotNull().collect { category ->
                setCategory(category)
                // remove to avoid reapplying on process death/restore
                savedStateHandle.remove<String>("preselectCategory")
            }
        }
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
        _selectedCategory.value = category?.trim()?.takeIf { it.isNotBlank() }
    }

    // ── Payment method filter ─────────────────────────────────────────────────

    private val _paymentMethod = MutableStateFlow<String?>(null)
    val paymentMethod: StateFlow<String?> = _paymentMethod.asStateFlow()

    fun setPaymentMethod(method: String?) {
        _paymentMethod.value = method?.trim()?.takeIf { it.isNotBlank() }
    }

    // ── Month / date range (missing declarations were causing type inference failures) ──

    private val _selectedMonth = MutableStateFlow<Int?>(null)
    val selectedMonth: StateFlow<Int?> = _selectedMonth.asStateFlow()

    fun setMonth(month: Int?) {
        _selectedMonth.value = month
    }

    private val _dateFrom = MutableStateFlow<Long?>(null)
    val dateFrom: StateFlow<Long?> = _dateFrom.asStateFlow()

    fun setDateFrom(ts: Long?) {
        _dateFrom.value = ts
    }

    private val _dateTo = MutableStateFlow<Long?>(null)
    val dateTo: StateFlow<Long?> = _dateTo.asStateFlow()

    fun setDateTo(ts: Long?) {
        _dateTo.value = ts
    }

    fun clearDateRange() {
        _dateFrom.value = null
        _dateTo.value = null
    }

    // ── Clear all filters ─────────────────────────────────────────────────────

    fun clearAllFilters() {
        _selectedCategory.value = null
        _paymentMethod.value = null
        _searchQuery.value = ""
        _selectedMonth.value = null
        _dateFrom.value = null
        _dateTo.value = null
    }

    /** Number of active filters (for badge on the filter button). */
    val activeFilterCount: StateFlow<Int> = combine(
        _selectedCategory, _paymentMethod, _selectedMonth, _dateFrom,
    ) { cat, pay, month, from ->
        (if (cat != null) 1 else 0) + (if (pay != null) 1 else 0) + (if (month != null) 1 else 0) + (if (from != null) 1 else 0)
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

    /** Filtered + searched flat list — drives both [resultCount] and [monthGroups].
     *  Month and date-range are applied on the raw entity list (which has timestamps).
     *  Category, payment-method and search are applied on the mapped UI list. */
    val transactions: StateFlow<List<TransactionItem>> = combine(
        // Pre-filter by month + date range on the raw entity list first
        combine(repository.transactions, _selectedMonth, _dateFrom, _dateTo) { raw, month, from, to ->
            raw.filter { t ->
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = t.timestamp }
                val matchesMonth = month == null || cal.get(java.util.Calendar.MONTH) + 1 == month
                val matchesFrom = from == null || t.timestamp >= from
                val matchesTo = to == null || t.timestamp <= to
                matchesMonth && matchesFrom && matchesTo
            }.sortedByDescending { it.timestamp }.map { t ->
                TransactionItem(
                    id = t.id,
                    merchant = t.merchant,
                    category = t.category,
                    timeLabel = formatRelativeTime(t.timestamp),
                    amount = -t.amount,
                    paymentMethod = t.paymentMethod,
                )
            }
        },
        _selectedCategory,
        _paymentMethod,
        _searchQuery,
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
    val hasActiveFiltersOrSearch: StateFlow<Boolean> = combine(
        _selectedCategory, _paymentMethod, _searchQuery, _selectedMonth, _dateFrom
    ) { cat, pay, q, month, from ->
        cat != null || pay != null || q.isNotBlank() || month != null || from != null
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

}