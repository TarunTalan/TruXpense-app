package com.example.truxpense.presentation.screens.dashboard.transaction


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    // ── Date navigator ────────────────────────────────────────────────────────

    private val _navOffset = MutableStateFlow(0)   // 0 = current, -1 = prev, +1 = next

    /**
     * Human-readable label for the navigator row.
     * Derived from period + offset — the composable never builds strings.
     */
    val periodNavLabel: StateFlow<String> = combine(_selectedPeriod, _navOffset) { period, offset ->
        when (period) {
            TransactionPeriod.YEAR  -> if (offset == 0) "This year"  else "${2026 + offset}"
            TransactionPeriod.MONTH -> if (offset == 0) "This month" else monthOffset(offset)
            TransactionPeriod.WEEK  -> if (offset == 0) "This week"  else weekOffset(offset)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "This year")

    val canNavBack: StateFlow<Boolean> = _navOffset
        .map { it > -24 }  // limit 2 years back
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val canNavForward: StateFlow<Boolean> = _navOffset
        .map { it < 0 }    // can't navigate into the future
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun navBack()    { _navOffset.value-- }
    fun navForward() { if (_navOffset.value < 0) _navOffset.value++ }

    // ── Search ────────────────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    // ── Filters ───────────────────────────────────────────────────────────────

    private val _activeFilters = MutableStateFlow<Set<TransactionFilter>>(emptySet())
    val activeFilters: StateFlow<Set<TransactionFilter>> = _activeFilters.asStateFlow()

    fun toggleFilter(filter: TransactionFilter) {
        _activeFilters.update { current ->
            if (filter in current) current - filter else current + filter
        }
    }

    // ── Month total dropdown ──────────────────────────────────────────────────

    private val _totalExpanded = MutableStateFlow(false)
    val totalExpanded: StateFlow<Boolean> = _totalExpanded.asStateFlow()

    fun toggleTotalExpanded() { _totalExpanded.update { !it } }

    // ── Transaction data ──────────────────────────────────────────────────────

    private val _allMonthGroups = MutableStateFlow(sampleMonthGroups)

    /**
     * Filtered transaction groups — search and active filters applied.
     * The composable iterates these directly; no filtering inside a composable.
     */
    val monthGroups: StateFlow<List<TransactionMonthGroup>> =
        combine(_allMonthGroups, _searchQuery, _activeFilters) { groups, query, filters ->
            applyFilters(groups, query, filters)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, sampleMonthGroups)

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun applyFilters(
        groups: List<TransactionMonthGroup>,
        query: String,
        filters: Set<TransactionFilter>,
    ): List<TransactionMonthGroup> {
        val q = query.trim().lowercase()
        return groups.mapNotNull { month ->
            val filteredDays = month.days.mapNotNull { day ->
                val filteredItems = day.items.filter { tx ->
                    val matchesSearch = q.isEmpty() ||
                            tx.merchant.lowercase().contains(q) ||
                            tx.category.lowercase().contains(q)
                    val matchesCategory = TransactionFilter.CATEGORY !in filters  // placeholder: show all
                    val matchesAccount  = TransactionFilter.ACCOUNT  !in filters  // placeholder: show all
                    matchesSearch && matchesCategory && matchesAccount
                }
                if (filteredItems.isEmpty()) null else day.copy(items = filteredItems)
            }
            if (filteredDays.isEmpty()) null else month.copy(days = filteredDays)
        }
    }

    /** "Feb 2025", "Dec 2025", etc. for month-level nav offsets. */
    private fun monthOffset(offset: Int): String {
        val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        val base   = 1  // February = index 1
        val total  = base + offset
        val year   = 2026 + (total / 12)
        val month  = ((total % 12) + 12) % 12
        return "${months[month]} $year"
    }

    /** "Week of 16 Feb", etc. */
    private fun weekOffset(offset: Int): String = "Week of ${16 + offset * 7} Feb"

    // ── Load real data ────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            // TODO: load from repository, map to MonthGroup list, update _allMonthGroups
        }
    }
}