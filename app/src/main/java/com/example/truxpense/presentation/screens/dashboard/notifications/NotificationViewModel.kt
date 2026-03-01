package com.example.truxpense.presentation.screens.dashboard.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.dashboard.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository,
) : ViewModel() {

    /** Full notification list, always in sync with the repository. */
    val notifications: StateFlow<List<NotificationItem>> = repository.notifications


    val unreadCount: StateFlow<Int> = repository.notifications.map { list -> list.count { item -> !item.isRead } }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = repository.notifications.value.count { item -> !item.isRead },
    )

    /** Drives the "Mark all read" button visibility in the top bar. */
    val hasUnread: StateFlow<Boolean> = repository.notifications.map { list -> list.any { item -> !item.isRead } }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = repository.notifications.value.any { item -> !item.isRead },
    )

    /** Mark a single notification as read. */
    fun markRead(id: String) = repository.markRead(id)

    /** Mark all notifications as read. */
    fun markAllRead() = repository.markAllRead()
}