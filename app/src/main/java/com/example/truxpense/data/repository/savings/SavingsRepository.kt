package com.example.truxpense.data.repository.savings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ──────────────────────────────────────────────────────────────────────────────
// Domain model surfaced to the UI layer
// ──────────────────────────────────────────────────────────────────────────────

data class SavingsGoalUi(
    val id: Long,
    val name: String,
    val icon: String,
    val colorHex: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val targetDateEpoch: Long,
    val autoContribute: Boolean,
    val autoContributeAmount: Double,
    val autoContributeFrequency: ContributeFrequency,
    val isCompleted: Boolean,
) {
    val progressFraction: Float
        get() =
            if (targetAmount <= 0) 0f else (savedAmount / targetAmount).toFloat().coerceIn(0f, 1f)

    val progressPercent: Int get() = (progressFraction * 100).toInt()

    val remaining: Double get() = (targetAmount - savedAmount).coerceAtLeast(0.0)

    fun daysLeft(): Int {
        // epoch-day is days since 1970-01-01
        val todayEpochDay = System.currentTimeMillis() / 86_400_000L
        return (targetDateEpoch - todayEpochDay).toInt().coerceAtLeast(0)
    }

    fun targetDateDisplay(): String {
        return try {
            val millis = targetDateEpoch * 86_400_000L
            val cal = java.util.Calendar.getInstance().also { it.timeInMillis = millis }
            val month = cal.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.LONG, java.util.Locale.getDefault())
                ?: ""
            "$month ${cal.get(java.util.Calendar.YEAR)}"
        } catch (e: Exception) {
            ""
        }
    }
}

data class SavingsContributionUi(
    val id: Long,
    val goalId: Long,
    val amount: Double,
    val label: String,
    val timestampMs: Long,
) {
    fun timeDisplay(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestampMs
        return when {
            diff < 86_400_000 -> "Today · ${formatTime(timestampMs)}"
            diff < 172_800_000 -> "Yesterday · ${formatTime(timestampMs)}"
            else -> {
                val cal = java.util.Calendar.getInstance()
                    .also { it.timeInMillis = timestampMs }
                val month = cal.getDisplayName(
                    java.util.Calendar.MONTH,
                    java.util.Calendar.SHORT, java.util.Locale.getDefault()
                ) ?: ""
                "$month ${cal.get(java.util.Calendar.DAY_OF_MONTH)} · ${formatTime(timestampMs)}"
            }
        }
    }

    private fun formatTime(ms: Long): String {
        val cal = java.util.Calendar.getInstance().also { it.timeInMillis = ms }
        val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val m = cal.get(java.util.Calendar.MINUTE)
        val amPm = if (h < 12) "AM" else "PM"
        val hDisplay = if (h % 12 == 0) 12 else h % 12
        return "$hDisplay:${m.toString().padStart(2, '0')} $amPm"
    }
}

data class SavingsEntryUi(
    val id: Long,
    val label: String,
    val amount: Double,
    val timestampMs: Long,
) {
    fun dateDisplay(): String {
        val cal = java.util.Calendar.getInstance().also { it.timeInMillis = timestampMs }
        val month = cal.getDisplayName(
            java.util.Calendar.MONTH,
            java.util.Calendar.SHORT, java.util.Locale.getDefault()
        ) ?: ""
        return "$month ${cal.get(java.util.Calendar.DAY_OF_MONTH)}"
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Repository
// ──────────────────────────────────────────────────────────────────────────────

@Singleton
class SavingsRepository @Inject constructor(private val dao: SavingsDao) {

    val goals: Flow<List<SavingsGoalUi>> = dao.observeGoals().map { list ->
        list.map { it.toUi() }
    }

    fun goal(id: Long): Flow<SavingsGoalUi?> = dao.observeGoal(id).map { it?.toUi() }

    fun contributions(goalId: Long): Flow<List<SavingsContributionUi>> =
        dao.observeContributions(goalId).map { list -> list.map { it.toUi() } }

    val recentSavingsEntries: Flow<List<SavingsEntryUi>> =
        dao.observeEntries().map { list -> list.map { it.toUi() } }

    val totalSavings: Flow<Double> = dao.observeTotalSavings()

    suspend fun createGoal(goal: SavingsGoalUi): Long = dao.upsertGoal(goal.toEntity())

    suspend fun updateGoal(goal: SavingsGoalUi) {
        dao.upsertGoal(goal.toEntity())
    }

    suspend fun deleteGoal(id: Long) {
        dao.observeGoal(id).collect { entity ->
            entity?.let { dao.deleteGoal(it) }
        }
    }

    suspend fun addContribution(goalId: Long, amount: Double, label: String) {
        dao.addToSaved(goalId, amount)
        dao.insertContribution(SavingsContribution(goalId = goalId, amount = amount, label = label))
        // Check completion
        dao.observeGoal(goalId).collect { g ->
            if (g != null && g.savedAmount >= g.targetAmount) dao.markCompleted(goalId)
        }
    }

    /** Bulk-set savedAmount for distribution confirm */
    suspend fun distributeGoals(allocations: Map<Long, Double>) {
        allocations.forEach { (id, amount) -> dao.setSaved(id, amount) }
    }

    /** Record an arbitrary savings entry (adds to entries table). Notes appended to label when provided. */
    suspend fun addSavingsEntry(amount: Double, label: String, notes: String) {
        val fullLabel = if (notes.isNotBlank()) "$label · ${notes.trim()}" else label
        dao.insertEntry(SavingsEntry(label = fullLabel, amount = amount))
    }

    // Backwards-compatible wrapper used by some ViewModels
    suspend fun addSavingsEntryInternal(amount: Double, label: String, notes: String) =
        addSavingsEntry(amount, label, notes)

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun SavingsGoal.toUi() = SavingsGoalUi(
        id, name, icon, colorHex,
        targetAmount, savedAmount, targetDateEpoch,
        autoContribute, autoContributeAmount, autoContributeFrequency, isCompleted
    )

    private fun SavingsGoalUi.toEntity() = SavingsGoal(
        id, name, icon, colorHex,
        targetAmount, savedAmount, targetDateEpoch,
        autoContribute, autoContributeAmount, autoContributeFrequency, isCompleted = isCompleted
    )

    private fun SavingsContribution.toUi() =
        SavingsContributionUi(id, goalId, amount, label, timestampMs)

    private fun SavingsEntry.toUi() = SavingsEntryUi(id, label, amount, timestampMs)
}