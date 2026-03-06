package com.example.truxpense.presentation.screens.dashboard.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.expense.ExpenseRepository
import com.example.truxpense.data.repository.income.IncomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    init {
        viewModelScope.launch {
            savedStateHandle.getStateFlow<String?>("preselectCategory", null)
                .filterNotNull().collect { category ->
                    setCategory(category)
                    // remove to avoid reapplying on process death/restore
                    savedStateHandle.remove<String>("preselectCategory")
                }
        }
        viewModelScope.launch {
            combine(
                repository.transactions,
                incomeRepository.allIncome,
            ) { _, _ -> }.first()
            _isLoaded.value = true
        }
    }

    // ── Type filter (All / Expense / Income) ──────────────────────────────────

    private val _typeFilter = MutableStateFlow<EntryType?>(null) // null = All
    val typeFilter: StateFlow<EntryType?> = _typeFilter.asStateFlow()

    fun setTypeFilter(type: EntryType?) { _typeFilter.value = type }

    // ── Search ────────────────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun clearSearch() { _searchQuery.value = "" }

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

    // ── Month / Year / Date range ─────────────────────────────────────────────

    private val _selectedMonth = MutableStateFlow<Int?>(null)
    val selectedMonth: StateFlow<Int?> = _selectedMonth.asStateFlow()
    fun setMonth(month: Int?) { _selectedMonth.value = month }

    private val _selectedYear = MutableStateFlow<Int?>(null)
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()
    fun setYear(year: Int?) { _selectedYear.value = year }

    private val _dateFrom = MutableStateFlow<Long?>(null)
    val dateFrom: StateFlow<Long?> = _dateFrom.asStateFlow()
    fun setDateFrom(ts: Long?) { _dateFrom.value = ts }

    private val _dateTo = MutableStateFlow<Long?>(null)
    val dateTo: StateFlow<Long?> = _dateTo.asStateFlow()
    fun setDateTo(ts: Long?) { _dateTo.value = ts }

    fun clearDateRange() { _dateFrom.value = null; _dateTo.value = null }

    fun clearAllFilters() {
        _selectedCategory.value = null
        _paymentMethod.value = null
        _searchQuery.value = ""
        _selectedMonth.value = null
        _selectedYear.value = null
        _dateFrom.value = null
        _dateTo.value = null
        _typeFilter.value = null
    }

    // ── isLoaded ──────────────────────────────────────────────────────────────

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    // ── Active filter count ───────────────────────────────────────────────────

    val activeFilterCount: StateFlow<Int> =
        combine(_selectedCategory, _paymentMethod) { cat: String?, pay: String? ->
            (if (cat != null) 1 else 0) + (if (pay != null) 1 else 0)
        }.combine(combine(_selectedMonth, _selectedYear) { m: Int?, y: Int? ->
            (if (m != null) 1 else 0) + (if (y != null) 1 else 0)
        }) { a, b -> a + b }
        .combine(combine(_dateFrom, _typeFilter) { from: Long?, type: EntryType? ->
            (if (from != null) 1 else 0) + (if (type != null) 1 else 0)
        }) { ab, cd -> ab + cd }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ── Raw merged feed ───────────────────────────────────────────────────────

    /** All expenses + income merged, newest first, as unified TransactionItem list. */
    private val _allItems: Flow<List<TransactionItem>> = combine(
        repository.transactions,
        incomeRepository.allIncome,
    ) { expenses, incomes ->
        val expenseItems = expenses.map { t ->
            TransactionItem(
                id = t.id,
                merchant = t.merchant,
                category = t.category,
                timeLabel = formatRelativeTime(t.timestamp),
                amount = t.amount,
                paymentMethod = t.paymentMethod,
                entryType = EntryType.EXPENSE,
                timestamp = t.timestamp,
            )
        }
        val incomeItems = incomes.map { inc ->
            TransactionItem(
                id = inc.id,
                merchant = inc.source,
                category = inc.source,
                timeLabel = formatRelativeTime(inc.timestamp),
                amount = inc.amount,
                paymentMethod = inc.paymentMethod,
                entryType = EntryType.INCOME,
                timestamp = inc.timestamp,
            )
        }
        (expenseItems + incomeItems).sortedByDescending { it.timestamp }
    }

    // ── Available filter options ──────────────────────────────────────────────

    val availableCategories: StateFlow<List<String>> = repository.transactions
        .map { list -> list.map { it.category }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val availablePaymentMethods: StateFlow<List<String>> = repository.transactions
        .map { list -> list.map { it.paymentMethod }.filter { it.isNotBlank() }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val availableYears: StateFlow<List<Int>> = _allItems
        .map { list ->
            list.map { Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.YEAR) }
                .distinct().sortedDescending()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totalTransactionCount: StateFlow<Int> =
        _allItems.map { it.size }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ── Filtered transactions ─────────────────────────────────────────────────

    val transactions: StateFlow<List<TransactionItem>> = combine(
        _allItems, _typeFilter, _selectedCategory, _paymentMethod,
        _searchQuery, _selectedMonth, _selectedYear, _dateFrom, _dateTo,
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val items = arr[0] as List<TransactionItem>
        val type = arr[1] as EntryType?
        val cat = arr[2] as String?
        val pay = arr[3] as String?
        val q = arr[4] as String
        val month = arr[5] as Int?
        val year = arr[6] as Int?
        val from = arr[7] as Long?
        val to = arr[8] as Long?

        items.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            val matchType = type == null || tx.entryType == type
            val matchCat = cat == null || tx.category.equals(cat, ignoreCase = true)
            val matchPay = pay == null || tx.paymentMethod.equals(pay, ignoreCase = true)
            val matchSearch = q.isBlank() ||
                tx.merchant.contains(q, ignoreCase = true) ||
                tx.category.contains(q, ignoreCase = true)
            val matchMonth = month == null || cal.get(Calendar.MONTH) + 1 == month
            val matchYear = year == null || cal.get(Calendar.YEAR) == year
            val matchFrom = from == null || tx.timestamp >= from
            val matchTo = to == null || tx.timestamp <= to
            matchType && matchCat && matchPay && matchSearch && matchMonth && matchYear && matchFrom && matchTo
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val resultCount: StateFlow<Int> =
        transactions.map { it.size }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val hasActiveFiltersOrSearch: StateFlow<Boolean> = combine(
        _selectedCategory, _paymentMethod, _searchQuery,
        _selectedMonth, _selectedYear,
    ) { cat: String?, pay: String?, q: String, month: Int?, year: Int? ->
        cat != null || pay != null || q.isNotBlank() || month != null || year != null
    }.combine(combine(_dateFrom, _typeFilter) { from, type -> from != null || type != null }) { a, b -> a || b }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Grouped for the UI list. */
    val monthGroups: StateFlow<List<TransactionMonthGroup>> =
        transactions.map { buildMonthGroups(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Total spent (expenses only) from current filtered set. */
    val totalSpent: StateFlow<Double> = transactions
        .map { it.filter { tx -> tx.entryType == EntryType.EXPENSE }.sumOf { tx -> tx.amount } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    /** Total income from current filtered set. */
    val totalIncome: StateFlow<Double> = transactions
        .map { it.filter { tx -> tx.entryType == EntryType.INCOME }.sumOf { tx -> tx.amount } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // ── Bottom-sheet toggle ───────────────────────────────────────────────────

    private val _totalExpanded = MutableStateFlow(false)
    val totalExpanded: StateFlow<Boolean> = _totalExpanded.asStateFlow()
    fun toggleTotalExpanded() { _totalExpanded.update { !it } }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildMonthGroups(items: List<TransactionItem>): List<TransactionMonthGroup> {
        if (items.isEmpty()) return emptyList()
        val grouped = items.groupBy { it.timeLabel }
        val days = grouped.entries.map { (label, txs) ->
            TransactionDayGroup(dayLabel = label, items = txs)
        }
        val total = items.filter { it.entryType == EntryType.EXPENSE }.sumOf { it.amount }
        return listOf(TransactionMonthGroup("Recent", total, days))
    }

    private fun formatRelativeTime(timestamp: Long): String {
        val delta = System.currentTimeMillis() - timestamp
        val days = delta / (1_000 * 60 * 60 * 24)
        return when {
            delta < 1_000 * 60 * 60 * 24 -> "Today"
            days == 1L -> "Yesterday"
            else -> {
                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
                "${months[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.DAY_OF_MONTH)}"
            }
        }
    }

}