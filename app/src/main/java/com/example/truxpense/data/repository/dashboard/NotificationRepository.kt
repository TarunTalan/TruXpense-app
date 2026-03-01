package com.example.truxpense.data.repository.dashboard

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.truxpense.presentation.screens.dashboard.notifications.NotificationDestination
import com.example.truxpense.presentation.screens.dashboard.notifications.NotificationIconType
import com.example.truxpense.presentation.screens.dashboard.notifications.NotificationItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton


private val Context.dataStore by preferencesDataStore(name = "truxpense_prefs")


@Singleton
class NotificationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val READ_IDS_KEY = stringSetPreferencesKey("read_notification_ids")

    private val seed = listOf(
        NotificationItem(
            id = "n1",
            title = "Food budget exceeded",
            body = "You've crossed your set limit. You can adjust it anytime.",
            iconType = NotificationIconType.BUDGET_EXCEEDED,
            timeLabel = "10 min ago",
            isRead = false,
            destination = NotificationDestination.BudgetDetail("Food", 3000.0, 3200.0),
        ),
        NotificationItem(
            id = "n2",
            title = "Food budget almost reached",
            body = "You've used 80% of your monthly limit.",
            iconType = NotificationIconType.BUDGET_WARNING,
            timeLabel = "2h ago",
            isRead = false,
            destination = NotificationDestination.BudgetDetail("Food", 3000.0, 2400.0),
        ),
        NotificationItem(
            id = "n3",
            title = "Your spend more this weekend",
            body = "Your weekend spending was higher than usual.",
            iconType = NotificationIconType.SPENDING_INSIGHT,
            timeLabel = "3h ago",
            isRead = false,
            destination = NotificationDestination.WeeklyAnalytics,
        ),
        NotificationItem(
            id = "n4",
            title = "Unusual expense detected",
            body = "This transaction is higher than your typical spend.",
            iconType = NotificationIconType.SPENDING_INSIGHT,
            timeLabel = "3h ago",
            isRead = false,
            destination = NotificationDestination.TransactionDetail("latest"),
        ),
        NotificationItem(
            id = "n5",
            title = "Your weekly spending summary is ready",
            body = "See where most of your money went this week.",
            iconType = NotificationIconType.SPENDING_INSIGHT,
            timeLabel = "3h ago",
            isRead = false,
            destination = NotificationDestination.WeeklyAnalytics,
        ),
        NotificationItem(
            id = "n6",
            title = "Haven't logged expense lately",
            body = "Add your recent spending to keep insights accurate.",
            iconType = NotificationIconType.ADD_EXPENSE_PROMPT,
            timeLabel = "2 weeks ago",
            isRead = true,
            destination = NotificationDestination.AddExpense,
        ),
        NotificationItem(
            id = "n7",
            title = "Transaction synced successfully",
            body = "Your latest SMS expenses have been added.",
            iconType = NotificationIconType.SYNC_SUCCESS,
            timeLabel = "1 month ago",
            isRead = true,
            destination = NotificationDestination.TransactionList,
        ),
    )

    // Single source of truth — lives as long as the process lives.
    private val _notifications = MutableStateFlow(seed)
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            // Load saved read IDs from DataStore and update the in-memory notification list
            val savedReadIds = context.dataStore.data.first()[READ_IDS_KEY] ?: emptySet()
            _notifications.update { list ->
                list.map { item ->
                    if (item.id in savedReadIds) item.copy(isRead = true) else item
                }
            }
        }
    }

    fun markRead(id: String) {
        _notifications.update { list ->
            list.map { if (it.id == id) it.copy(isRead = true) else it }
        }
        CoroutineScope(Dispatchers.IO).launch {
            // Update DataStore when a notification is marked as read
            context.dataStore.edit { preferences ->
                val currentReadIds = preferences[READ_IDS_KEY] ?: emptySet()
                preferences[READ_IDS_KEY] = currentReadIds + id
            }
        }
    }

    fun markAllRead() {
        _notifications.update { list -> list.map { it.copy(isRead = true) } }
        CoroutineScope(Dispatchers.IO).launch {
            // Update DataStore when all notifications are marked as read
            context.dataStore.edit { preferences ->
                val allIds = _notifications.value.map { it.id }.toSet()
                preferences[READ_IDS_KEY] = allIds
            }
        }
    }
}