package com.example.truxpense.notification.deeplink

import android.content.Intent
import com.example.truxpense.notification.channels.NotificationConstants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Destinations a notification tap can route to inside the dashboard.
 */
sealed class NotificationDeepLink {
    object AddExpense  : NotificationDeepLink()
    object BudgetTab   : NotificationDeepLink()
    /** Navigate to the full budget list, then push the detail screen for [category]. */
    data class BudgetDetail(val category: String) : NotificationDeepLink()
    /** Same as [BudgetDetail] but triggered from a notification — looks up live budget data. */
    data class BudgetDetailByCategory(val category: String) : NotificationDeepLink()
    object Analytics   : NotificationDeepLink()
    object Transactions: NotificationDeepLink()
    object Home        : NotificationDeepLink()
}


@Singleton
class NotificationDeepLinkManager @Inject constructor() {

    private val _pending = MutableSharedFlow<NotificationDeepLink>(replay = 1)
    val pendingDeepLink: SharedFlow<NotificationDeepLink> = _pending.asSharedFlow()

    fun handle(intent: Intent?): Boolean {
        val dest     = intent?.getStringExtra(NotificationConstants.EXTRA_DESTINATION) ?: return false
        val category = intent.getStringExtra(NotificationConstants.EXTRA_CATEGORY)

        val link: NotificationDeepLink = when (dest) {
            NotificationConstants.DEST_ADD_EXPENSE   -> NotificationDeepLink.AddExpense
            NotificationConstants.DEST_BUDGET_DETAIL -> {
                if (!category.isNullOrBlank())
                    NotificationDeepLink.BudgetDetailByCategory(category)
                else
                    NotificationDeepLink.BudgetTab
            }
            NotificationConstants.DEST_BUDGET_TAB    -> NotificationDeepLink.BudgetTab
            NotificationConstants.DEST_ANALYTICS     -> NotificationDeepLink.Analytics
            NotificationConstants.DEST_TRANSACTIONS  -> NotificationDeepLink.Transactions
            NotificationConstants.DEST_DASHBOARD     -> NotificationDeepLink.Home
            else                                     -> NotificationDeepLink.Home
        }

        _pending.tryEmit(link)
        return true
    }

    /**
     * Call after successfully navigating so that the replay slot is cleared and
     * the same deep link is not re-processed on the next recomposition.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun consume() = _pending.resetReplayCache()
}