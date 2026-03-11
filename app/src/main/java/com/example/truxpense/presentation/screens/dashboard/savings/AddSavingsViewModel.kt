package com.example.truxpense.presentation.screens.dashboard.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.savings.SavingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Predefined sources the user can multi-select from. */
val SAVINGS_SOURCES = listOf("Salary", "Bonus", "Cash", "Bank Transfer", "Other")

@HiltViewModel
class AddSavingsViewModel @Inject constructor(
    private val repository: SavingsRepository,
) : ViewModel() {

    // ── Form fields ───────────────────────────────────────────────────────────

    /** Raw digit string coming from AmountInputCard (no formatting). */
    private val _rawAmount = MutableStateFlow("")
    val rawAmount: StateFlow<String> = _rawAmount.asStateFlow()

    /** Free-text source description (e.g. "salary", "bank transfer"). */
    private val _source = MutableStateFlow("")
    val source: StateFlow<String> = _source.asStateFlow()

    /** Multi-select set of source-of-savings categories. */
    private val _selectedSources = MutableStateFlow<Set<String>>(emptySet())
    val selectedSources: StateFlow<Set<String>> = _selectedSources.asStateFlow()

    /** Optional notes. */
    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    // ── Derived ───────────────────────────────────────────────────────────────

    val isFormValid: StateFlow<Boolean> = _rawAmount
        .map { it.toDoubleOrNull().let { v -> v != null && v > 0 } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── Mutators ──────────────────────────────────────────────────────────────

    fun setRawAmount(v: String) {
        _rawAmount.value = v.filter { it.isDigit() }
    }

    /** Adds [delta] to the current raw amount (used by quick-add chips). */
    fun addAmount(delta: Long) {
        val current = _rawAmount.value.toLongOrNull() ?: 0L
        _rawAmount.value = (current + delta).toString()
    }

    fun setSource(v: String) {
        _source.value = v
    }

    fun toggleSource(source: String) {
        val current = _selectedSources.value.toMutableSet()
        if (source in current) current.remove(source) else current.add(source)
        _selectedSources.value = current
    }

    fun setNotes(v: String) {
        _notes.value = v
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    /** Persists the new savings entry and emits [saved] on success. */
    private val _saved = MutableSharedFlow<Unit>()
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    fun saveSavingsEntry() = viewModelScope.launch {
        val amount = _rawAmount.value.toDoubleOrNull() ?: return@launch
        val label = buildLabel()
        repository.addSavingsEntryInternal(amount = amount, label = label, notes = _notes.value)
        _saved.emit(Unit)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildLabel(): String {
        val text = _source.value.trim()
        val tags = _selectedSources.value.sorted()
        return when {
            text.isNotBlank() && tags.isNotEmpty() -> "$text · ${tags.joinToString(", ")}"
            text.isNotBlank() -> text
            tags.isNotEmpty() -> tags.joinToString(", ")
            else -> "Manual deposit"
        }
    }
}