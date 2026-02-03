package com.example.truxpense.data.otp

// OTP lock manager

import com.example.truxpense.data.prefs.AuthPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encapsulates OTP lockout logic:
 * - sliding window for counting failures
 * - per-email fail count and last-failure timestamp
 * - lock duration
 */
@Singleton
class OtpLockManager @Inject constructor(
    private val prefs: AuthPreferences
) {
    // Defaults (can be made configurable via DI later)
    private val maxAttempts = 5
    private val failureWindowMs = 30 * 60 * 1000L // 30 minutes sliding window
    private val lockDurationMs = 5 * 60 * 1000L // 5 minutes lock

    private fun normalizeEmail(email: String) = email.trim().lowercase()

    /**
     * Returns true if the email is currently locked. If the persisted lock is expired
     * this will proactively clear stale state so the caller can proceed with fresh counters.
     */
    suspend fun isLocked(email: String): Boolean = withContext(Dispatchers.IO) {
        val e = normalizeEmail(email)
        val lockUntil = prefs.otpLockUntilFor(e).first()
        val now = System.currentTimeMillis()
        if (lockUntil > now) return@withContext true
        if (lockUntil > 0L) {
            // expired stale lock: clear persisted lock+counts atomically
            prefs.clearOtpLockAndFailDataFor(e)
        }
        return@withContext false
    }

    suspend fun getRemainingLockSeconds(email: String): Int = withContext(Dispatchers.IO) {
        val e = normalizeEmail(email)
        val lockUntil = prefs.otpLockUntilFor(e).first()
        val now = System.currentTimeMillis()
        return@withContext if (lockUntil > now) ((lockUntil - now) / 1000L).toInt() else 0
    }

    /**
     * Register a failure for an email. Returns Pair(locked, attemptsLeft).
     * attemptsLeft is the number of attempts remaining before lock (0 when locked).
     * Uses a sliding window: failures outside the window reset the counter.
     */
    suspend fun registerFailure(email: String): Pair<Boolean, Int> = withContext(Dispatchers.IO) {
        val e = normalizeEmail(email)
        val now = System.currentTimeMillis()

        // Read lock & last-failure in the same IO context
        val lockUntil = prefs.otpLockUntilFor(e).first()
        if (lockUntil > now) return@withContext Pair(true, 0) // already locked

        val lastFailTs = prefs.otpLastFailFor(e).first()
        if (lastFailTs == 0L || (now - lastFailTs) > failureWindowMs) {
            // Reset stale counters atomically
            prefs.clearOtpLockAndFailDataFor(e)
        }

        // Increment fail count and update last failure timestamp atomically
        val newCount = prefs.incrementOtpFailCountAndSetLastFailFor(e, now)

        if (newCount >= maxAttempts) {
            val until = now + lockDurationMs
            prefs.setOtpLockUntilFor(e, until)
            return@withContext Pair(true, 0)
        }

        val attemptsLeft = maxAttempts - newCount
        return@withContext Pair(false, attemptsLeft)
    }

    suspend fun resetFailures(email: String) = withContext(Dispatchers.IO) {
        val e = normalizeEmail(email)
        prefs.clearOtpLockAndFailDataFor(e)
    }

    suspend fun clearLock(email: String) = withContext(Dispatchers.IO) {
        val e = normalizeEmail(email)
        prefs.clearOtpLockAndFailDataFor(e)
    }
}
