package com.example.truxpense.data.repository.notification

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.truxpense.presentation.screens.dashboard.notifications.NotificationDestination
import com.example.truxpense.presentation.screens.dashboard.notifications.NotificationIconType
import com.example.truxpense.presentation.screens.dashboard.notifications.NotificationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

// Ensure repository dependencies are imported so annotation processors (Hilt/KAPT)
// can resolve the constructor parameter types at compile time.
import com.example.truxpense.data.repository.expense.ExpenseRepository
import com.example.truxpense.data.repository.expense.Transaction
import com.example.truxpense.data.repository.budget.BudgetRepository
import com.example.truxpense.data.repository.budget.Budget


/**
 * Singleton repository that generates notifications dynamically from live
 * [ExpenseRepository] and [BudgetRepository] data.
 *
 * Persistence
 * ───────────
 * Read IDs and dismissed IDs are stored in [DataStore<Preferences>] so they
 * survive app restarts and process death.  On cold start DataStore is read
 * first; the notification list is only emitted once both sets are loaded.
 *
 * Dynamic generation
 * ──────────────────
 * Notifications are derived entirely from live data — no hardcoded sample items.
 * Every time transactions, budgets, readIds or dismissedIds change, the full list
 * is recomputed and emitted.  If a condition resolves (e.g. budget is raised),
 * the notification disappears automatically on the next emission.
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val dataStore: DataStore<Preferences>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ── DataStore keys ────────────────────────────────────────────────────────

    private val KEY_READ = stringSetPreferencesKey("notif_read_ids")
    private val KEY_DISMISSED = stringSetPreferencesKey("notif_dismissed_ids")

    // ── In-memory mirrors of the persisted sets ───────────────────────────────
    // Initialised from DataStore on startup; every write goes to both the
    // MutableStateFlow (instant UI update) and DataStore (persistence).

    private val _readIds = MutableStateFlow<Set<String>>(emptySet())
    private val _dismissedIds = MutableStateFlow<Set<String>>(emptySet())

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications

    // Expose load status so UI doesn't flash transient seed/empty states
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    init {
        // ── Step 1: restore persisted sets from disk ──────────────────────────
        scope.launch {
            dataStore.data.collect { prefs ->
                _readIds.value = prefs[KEY_READ] ?: emptySet()
                _dismissedIds.value = prefs[KEY_DISMISSED] ?: emptySet()
            }
        }

        // ── Step 2: recompute list whenever any upstream changes ──────────────
        scope.launch {
            combine(
                expenseRepository.transactions,
                budgetRepository.budgets,
                _dismissedIds,
                _readIds,
            ) { txList, budgetList, dismissed, readIds ->
                buildNotifications(txList, budgetList, dismissed, readIds)
            }.collect { _notifications.value = it
                // Mark repository as loaded after first emission
                if (!_isLoaded.value) _isLoaded.value = true
            }
        }
    }

    // ── Notification generation ───────────────────────────────────────────────

    private fun buildNotifications(
        txList: List<Transaction>,
        budgetList: List<Budget>,
        dismissed: Set<String>,
        readIds: Set<String>,
    ): List<NotificationItem> {

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val result = mutableListOf<NotificationItem>()

        fun addIfNotDismissed(item: NotificationItem) {
            if (item.id !in dismissed) result.add(item)
        }

        // ── 1 & 2 — Budget exceeded / warning ────────────────────────────────
        val monthKey = "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.MONTH)}"
        val spentByCategory: Map<String, Double> =
            txList.groupBy { it.category }.mapValues { (_, ts) -> ts.sumOf { t -> t.amount } }

        for (budget in budgetList) {
            val total = budget.amount
            if (total <= 0.0) continue
            val spent = spentByCategory[budget.category] ?: 0.0
            val ratio = spent / total

            val latestTs: Long = txList.filter { it.category == budget.category }.maxOfOrNull { it.timestamp } ?: now

            when {
                ratio >= 1.0 -> {
                    val id = "budget_exceeded_${budget.category}_$monthKey"
                    addIfNotDismissed(
                        NotificationItem(
                            id = id,
                            title = "${budget.category} budget exceeded",
                            body = "You've crossed your ${budget.category} limit " + "(${fmt(spent)} of ${fmt(total)} spent).",
                            iconType = NotificationIconType.BUDGET_EXCEEDED,
                            timeLabel = timeLabel(latestTs, now),
                            timeMs = latestTs,
                            isRead = id in readIds,
                            destination = NotificationDestination.BudgetDetail(budget.category, total, spent),
                        )
                    )
                }

                ratio >= 0.8 -> {
                    val id = "budget_warning_${budget.category}_$monthKey"
                    addIfNotDismissed(
                        NotificationItem(
                            id = id,
                            title = "${budget.category} budget almost reached",
                            body = "You've used ${(ratio * 100).toInt()}% of your " + "${budget.category} limit (${
                                fmt(spent)
                            } of ${fmt(total)}).",
                            iconType = NotificationIconType.BUDGET_WARNING,
                            timeLabel = timeLabel(latestTs, now),
                            timeMs = latestTs,
                            isRead = id in readIds,
                            destination = NotificationDestination.BudgetDetail(budget.category, total, spent),
                        )
                    )
                }
            }
        }

        // ── 3 — Unusual expense ───────────────────────────────────────────────
        if (txList.size >= 3) {
            val mean = txList.sumOf { it.amount } / txList.size
            val variance = txList.sumOf { t -> val d = t.amount - mean; d * d } / txList.size
            val stdDev = sqrt(variance)
            val threshold = mean + 2.0 * stdDev

            txList.filter { it.timestamp >= now - 7 * DAY_MS && it.amount > threshold && it.amount > mean * 2 }
                .sortedByDescending { it.timestamp }.take(3).forEach { tx ->
                    val id = "unusual_${tx.id}"
                    addIfNotDismissed(
                        NotificationItem(
                            id = id,
                            title = "Unusual expense detected",
                            body = "${tx.merchant} — ${fmt(tx.amount)} is significantly " + "higher than your average spend (${
                                fmt(mean)
                            }).",
                            iconType = NotificationIconType.SPENDING_INSIGHT,
                            timeLabel = timeLabel(tx.timestamp, now),
                            timeMs = tx.timestamp,
                            isRead = id in readIds,
                            destination = NotificationDestination.TransactionDetail(tx.id),
                        )
                    )
                }
        }

        // ── 4 & 5 — Weekly spike and weekly summary ───────────────────────────
        val (weekStart, weekEnd) = currentWeekBounds(cal)
        val lastWeekEnd = weekStart - 1L
        val lastWeekStart = weekStart - 7 * DAY_MS
        val thisWeekSpend = txList.filter { it.timestamp in weekStart..weekEnd }.sumOf { it.amount }
        val lastWeekSpend = txList.filter { it.timestamp in lastWeekStart..lastWeekEnd }.sumOf { it.amount }
        val weekKey = "${cal.get(Calendar.YEAR)}_W${cal.get(Calendar.WEEK_OF_YEAR)}"

        if (thisWeekSpend > 0 && lastWeekSpend > 0 && thisWeekSpend > lastWeekSpend * 1.2) {
            val id = "weekly_spike_$weekKey"
            addIfNotDismissed(
                NotificationItem(
                    id = id,
                    title = "Spending higher than last week",
                    body = "This week: ${fmt(thisWeekSpend)}  ·  Last week: ${fmt(lastWeekSpend)}.",
                    iconType = NotificationIconType.SPENDING_INSIGHT,
                    timeLabel = "This week",
                    timeMs = weekStart,
                    isRead = id in readIds,
                    destination = NotificationDestination.WeeklyAnalytics,
                )
            )
        }

        if (thisWeekSpend > 0) {
            val id = "weekly_summary_$weekKey"
            addIfNotDismissed(
                NotificationItem(
                    id = id,
                    title = "Your weekly spending summary is ready",
                    body = "You've spent ${fmt(thisWeekSpend)} this week. " + "See where most of your money went.",
                    iconType = NotificationIconType.SPENDING_INSIGHT,
                    timeLabel = "This week",
                    timeMs = weekStart,
                    isRead = id in readIds,
                    destination = NotificationDestination.WeeklyAnalytics,
                )
            )
        }

        // ── 6 — Inactivity reminder ───────────────────────────────────────────
        val hasRecentTx = txList.any { it.timestamp >= now - 3 * DAY_MS }
        if (!hasRecentTx) {
            val id = "no_expense_log"
            addIfNotDismissed(
                NotificationItem(
                    id = id,
                    title = "Haven't logged an expense lately",
                    body = "Add your recent spending to keep insights accurate.",
                    iconType = NotificationIconType.ADD_EXPENSE_PROMPT,
                    timeLabel = "Reminder",
                    timeMs = 0L,
                    isRead = id in readIds,
                    destination = NotificationDestination.AddExpense,
                )
            )
        }

        return result.sortedWith(compareBy({ it.isRead }, { typePriority(it.iconType) }))
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun markRead(id: String) {
        scope.launch {
            _readIds.value = _readIds.value + id          // instant UI update
            dataStore.edit { prefs ->                      // persist to disk
                prefs[KEY_READ] = (prefs[KEY_READ] ?: emptySet()) + id
            }
        }
    }

    fun markAllRead() {
        val allIds = _notifications.value.map { it.id }.toSet()
        scope.launch {
            _readIds.value = _readIds.value + allIds
            dataStore.edit { prefs ->
                prefs[KEY_READ] = (prefs[KEY_READ] ?: emptySet()) + allIds
            }
        }
    }

    fun deleteByIds(ids: Set<String>) {
        scope.launch {
            _dismissedIds.value = _dismissedIds.value + ids
            dataStore.edit { prefs ->
                prefs[KEY_DISMISSED] = (prefs[KEY_DISMISSED] ?: emptySet()) + ids
            }
        }
    }

    fun deleteAll() {
        val allIds = _notifications.value.map { it.id }.toSet()
        scope.launch {
            _dismissedIds.value = _dismissedIds.value + allIds
            dataStore.edit { prefs ->
                prefs[KEY_DISMISSED] = (prefs[KEY_DISMISSED] ?: emptySet()) + allIds
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun typePriority(type: NotificationIconType): Int = when (type) {
        NotificationIconType.BUDGET_EXCEEDED -> 0
        NotificationIconType.BUDGET_WARNING -> 1
        NotificationIconType.SPENDING_INSIGHT -> 2
        NotificationIconType.ADD_EXPENSE_PROMPT -> 3
        NotificationIconType.SYNC_SUCCESS -> 4
    }

    private fun timeLabel(timestamp: Long, now: Long): String {
        if (timestamp <= 0L) return "Reminder"
        val delta = now - timestamp
        val calTs = Calendar.getInstance().apply { timeInMillis = timestamp }
        val calNow = Calendar.getInstance()
        return when {
            delta < HOUR_MS -> "Just now"
            isSameDay(calTs, calNow) -> "Today"
            delta < 2 * DAY_MS -> "Yesterday"
            delta < 7 * DAY_MS -> "This week"
            delta < 14 * DAY_MS -> "Last week"
            else -> {
                val m = MONTHS[calTs.get(Calendar.MONTH)]
                val y = calTs.get(Calendar.YEAR)
                "$m $y"
            }
        }
    }

    private fun currentWeekBounds(cal: Calendar): Pair<Long, Long> {
        val c = cal.clone() as Calendar
        val dow = c.get(Calendar.DAY_OF_WEEK)
        val toMon = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
        c.add(Calendar.DAY_OF_YEAR, -toMon)
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        val start = c.timeInMillis
        return start to (start + 7 * DAY_MS - 1L)
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun fmt(amount: Double) = "₹${"%,.0f".format(amount)}"

    companion object {
        private const val HOUR_MS = 3_600_000L
        private const val DAY_MS = 86_400_000L
        private val MONTHS = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        )
    }
}