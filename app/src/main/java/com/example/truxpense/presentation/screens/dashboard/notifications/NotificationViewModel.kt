package com.example.truxpense.presentation.screens.dashboard.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.dashboard.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository,
) : ViewModel() {

    // ── Notification list (from singleton repository) ─────────────────────────

    val notifications: StateFlow<List<NotificationItem>> = repository.notifications

    val unreadCount: StateFlow<Int> =
        repository.notifications.map { list: List<NotificationItem> -> list.count { item -> !item.isRead } }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = repository.notifications.value.count { item -> !item.isRead },
        )

    val hasUnread: StateFlow<Boolean> =
        repository.notifications.map { list: List<NotificationItem> -> list.any { item -> !item.isRead } }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = repository.notifications.value.any { item -> !item.isRead },
        )

    // ── Selection state ───────────────────────────────────────────────────────

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> =
        _selectedIds.map { ids -> ids.isNotEmpty() }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isAllSelected: StateFlow<Boolean> = _selectedIds.map { ids ->
        val all = repository.notifications.value
        all.isNotEmpty() && ids.size == all.size
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val selectedCount: StateFlow<Int> =
        _selectedIds.map { ids -> ids.size }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ── Selection actions ─────────────────────────────────────────────────────

    fun startSelection(id: String) {
        _selectedIds.update { setOf(id) }
    }

    fun toggleSelection(id: String) {
        _selectedIds.update { current ->
            if (id in current) current - id else current + id
        }
    }

    fun toggleSelectAll() {
        val allIds = repository.notifications.value.
        map { it.id }.toSet()
        _selectedIds.update { current ->
            if (current.size == allIds.size) emptySet() else allIds
        }
    }

    fun clearSelection() {
        _selectedIds.update { emptySet() }
    }

    // ── Read actions ──────────────────────────────────────────────────────────

    fun markRead(id: String) = repository.markRead(id)
    fun markAllRead() = repository.markAllRead()

    // ── Delete actions ────────────────────────────────────────────────────────

    fun deleteSelected() {
        val toDelete = _selectedIds.value
        repository.deleteByIds(toDelete)
        _selectedIds.update { emptySet() }
    }

    fun deleteAll() {
        repository.deleteAll()
        _selectedIds.update { emptySet() }
    }
}