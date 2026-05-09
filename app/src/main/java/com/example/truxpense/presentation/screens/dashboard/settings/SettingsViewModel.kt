package com.example.truxpense.presentation.screens.dashboard.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.truxpense.data.local.datastore.AuthPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AuthPreferences,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // ── Profile — Eagerly-started so the value is ready before first composition ──
    val username: StateFlow<String?> = prefs.username.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )
    val phone: StateFlow<String?> = prefs.phone.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    // ── SMS permission (runtime) ───────────────────────────────────────────────
    private val _smsEnabled = MutableStateFlow(
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    )
    val smsEnabled: StateFlow<Boolean> = _smsEnabled

    fun refreshSmsPermission() {
        _smsEnabled.value = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ── Notification preferences ──────────────────────────────────────────────
    private val _notifPrefs = MutableStateFlow(NotificationPrefs())
    val notifPrefs: StateFlow<NotificationPrefs> = _notifPrefs

    fun updateNotifPrefs(new: NotificationPrefs) {
        val old = _notifPrefs.value
        _notifPrefs.value = new
        // Re-schedule daily reminder whenever the toggle or time changes
        if (new.dailyReminderEnabled != old.dailyReminderEnabled || new.dailyReminderHour != old.dailyReminderHour || new.dailyReminderMinute != old.dailyReminderMinute) {
            if (new.dailyReminderEnabled) {
                scheduleDailyReminder(new.dailyReminderHour, new.dailyReminderMinute)
            } else {
                cancelDailyReminder()
            }
        }
        // TODO: persist via DataStore / repository
    }

    // ── Daily reminder — WorkManager ──────────────────────────────────────────

    /**
     * Schedules a periodic daily notification at the chosen [hour]:[minute].
     * Uses WorkManager with a 24-hour repeat interval; the initial delay is
     * calculated so the first fire lands at the correct wall-clock time today
     * (or tomorrow if that time has already passed).
     *
     * Replace [DailyExpenseReminderWorker] with your actual Worker class.
     */
    private fun scheduleDailyReminder(hour: Int, minute: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If target is in the past, push to tomorrow
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val initialDelay = target.timeInMillis - now.timeInMillis

        val request = PeriodicWorkRequestBuilder<DailyExpenseReminderWorker>(
            repeatInterval = 24, repeatIntervalTimeUnit = TimeUnit.HOURS
        ).setInitialDelay(initialDelay, TimeUnit.MILLISECONDS).addTag(REMINDER_WORK_TAG).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            REMINDER_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE,  // replace any existing schedule
            request
        )
    }

    private fun cancelDailyReminder() {
        WorkManager.getInstance(context).cancelUniqueWork(REMINDER_WORK_NAME)
    }

    // ── Security ──────────────────────────────────────────────────────────────
    private val _biometricsEnabled = MutableStateFlow(false)
    val biometricsEnabled: StateFlow<Boolean> = _biometricsEnabled

    private val _appLockEnabled = MutableStateFlow(false)
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled

    fun setBiometricsEnabled(enabled: Boolean) {
        _biometricsEnabled.value = enabled
    }

    fun setAppLockEnabled(enabled: Boolean) {
        _appLockEnabled.value = enabled
    }

    fun changePassword(old: String, new: String) {
        viewModelScope.launch {
            // TODO: call auth repository to update password
        }
    }

    // ── Linked accounts ───────────────────────────────────────────────────────
    private val _linkedAccounts = MutableStateFlow<List<LinkedAccount>>(emptyList())
    val linkedAccounts: StateFlow<List<LinkedAccount>> = _linkedAccounts

    fun removeLinkedAccount(account: LinkedAccount) {
        _linkedAccounts.value = _linkedAccounts.value - account
        // TODO: persist via repository
    }

    // ── Profile edit ──────────────────────────────────────────────────────────
    private val _isSavingProfile = MutableStateFlow(false)
    val isSavingProfile: StateFlow<Boolean> = _isSavingProfile

    fun saveProfile(name: String, phone: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isSavingProfile.value = true
            prefs.savePhone(phone)
            // TODO: prefs.saveName(name) once added to AuthPreferences
            _isSavingProfile.value = false
            onComplete()
        }
    }

    // ── Account deletion ──────────────────────────────────────────────────────
    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount

    fun deleteAccount(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isDeletingAccount.value = true
            cancelDailyReminder()   // clean up scheduled work before wiping prefs
            prefs.clear()
            // TODO: call remote API to delete server-side data
            _isDeletingAccount.value = false
            onComplete()
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    fun logout(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            prefs.clear()
            onComplete?.invoke()
        }
    }

    companion object {
        private const val REMINDER_WORK_NAME = "daily_expense_reminder"
        private const val REMINDER_WORK_TAG = "expense_reminder"
    }
}

// ─── Worker stub — replace body with your real notification logic ─────────────

class DailyExpenseReminderWorker(
    ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        // TODO: build and show a local notification reminding the user to log expenses.
        // Example:
        //   val notificationManager = applicationContext
        //       .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        //   val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
        //       .setSmallIcon(R.drawable.notifications)
        //       .setContentTitle("Don't forget!")
        //       .setContentText("Log today's expenses in TruXpense 💸")
        //       .setAutoCancel(true)
        //       .build()
        //   notificationManager.notify(NOTIFICATION_ID, notification)
        return Result.success()
    }
}