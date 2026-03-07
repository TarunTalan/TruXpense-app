package com.example.truxpense.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class AuthPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        // Tokens
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val EXPIRES_IN = longPreferencesKey("expires_in")

        // User
        private val USERNAME = stringPreferencesKey("username")
        private val PHONE    = stringPreferencesKey("phone")
        private val EMAIL    = stringPreferencesKey("email")

        // Onboarding flags
        private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val SIGNUP_STARTED = booleanPreferencesKey("signup_started")
        private val ONBOARDING_STEP = stringPreferencesKey("onboarding_step")

        // Per-email OTP keys
        private const val OTP_FAIL_COUNT_PREFIX = "otp_fail_count_"
        private const val OTP_LOCK_UNTIL_PREFIX = "otp_lock_until_"
        private const val OTP_FAIL_TS_PREFIX = "otp_fail_ts_"
    }

    // Tokens
    suspend fun saveTokens(access: String, refresh: String, expiresIn: Long) {
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = access
            prefs[REFRESH_TOKEN] = refresh
            prefs[EXPIRES_IN] = expiresIn
        }
    }

    val accessToken: Flow<String?> = dataStore.data.map { prefs -> prefs[ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = dataStore.data.map { prefs -> prefs[REFRESH_TOKEN] }

    // Username
    suspend fun saveUsername(username: String) {
        dataStore.edit { prefs -> prefs[USERNAME] = username }
    }

    val username: Flow<String?> = dataStore.data.map { prefs -> prefs[USERNAME] }

    // Phone
    suspend fun savePhone(phone: String) {
        dataStore.edit { prefs -> prefs[PHONE] = phone }
    }

    val phone: Flow<String?> = dataStore.data.map { prefs -> prefs[PHONE] }

    // Email
    suspend fun saveEmail(email: String) {
        dataStore.edit { prefs -> prefs[EMAIL] = email }
    }

    val email: Flow<String?> = dataStore.data.map { prefs -> prefs[EMAIL] }

    // Onboarding
    suspend fun setOnboardingComplete(value: Boolean) {
        dataStore.edit { prefs -> prefs[ONBOARDING_COMPLETE] = value }
    }

    val onboardingComplete: Flow<Boolean> = dataStore.data.map { prefs -> prefs[ONBOARDING_COMPLETE] ?: false }

    suspend fun setSignupStarted(value: Boolean) {
        dataStore.edit { prefs -> prefs[SIGNUP_STARTED] = value }
    }

    val signupStarted: Flow<Boolean> = dataStore.data.map { prefs -> prefs[SIGNUP_STARTED] ?: false }

    // Onboarding step tracking for resume functionality
    suspend fun saveOnboardingStep(step: String) {
        dataStore.edit { prefs -> prefs[ONBOARDING_STEP] = step }
    }

    val onboardingStep: Flow<String?> = dataStore.data.map { prefs -> prefs[ONBOARDING_STEP] }

    // OTP per-email helpers
    private fun hashEmail(email: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val normalizedEmail = email.trim().lowercase()
            val bytes = md.digest(normalizedEmail.toByteArray(Charsets.UTF_8))
            bytes.joinToString(separator = "") { byte -> String.format("%02x", byte) }
        } catch (_: Exception) {
            email.trim().lowercase().replace(Regex("[^a-z0-9]"), "_")
        }
    }

    private fun failCountKeyFor(emailHash: String): Preferences.Key<Int> = intPreferencesKey(OTP_FAIL_COUNT_PREFIX + emailHash)
    private fun lockUntilKeyFor(emailHash: String): Preferences.Key<Long> = longPreferencesKey(OTP_LOCK_UNTIL_PREFIX + emailHash)
    private fun lastFailKeyFor(emailHash: String): Preferences.Key<Long> = longPreferencesKey(OTP_FAIL_TS_PREFIX + emailHash)

    fun otpLastFailFor(email: String): Flow<Long> {
        val key = lastFailKeyFor(hashEmail(email))
        return dataStore.data.map { prefs -> prefs[key] ?: 0L }
    }

    fun otpLockUntilFor(email: String): Flow<Long> {
        val key = lockUntilKeyFor(hashEmail(email))
        return dataStore.data.map { prefs -> prefs[key] ?: 0L }
    }

    suspend fun resetOtpFailCountFor(email: String) {
        val key = failCountKeyFor(hashEmail(email))
        dataStore.edit { prefs -> prefs[key] = 0 }
    }

    suspend fun setOtpLockUntilFor(email: String, millis: Long) {
        val key = lockUntilKeyFor(hashEmail(email))
        dataStore.edit { prefs -> prefs[key] = millis }
    }

    suspend fun clearOtpLockFor(email: String) {
        val key = lockUntilKeyFor(hashEmail(email))
        dataStore.edit { prefs -> prefs[key] = 0L }
    }

    suspend fun incrementOtpFailCountAndSetLastFailFor(email: String, millis: Long): Int {
        val key = failCountKeyFor(hashEmail(email))
        val tsKey = lastFailKeyFor(hashEmail(email))
        var newCount = 0
        dataStore.edit { prefs ->
            val current = prefs[key] ?: 0
            newCount = current + 1
            prefs[key] = newCount
            prefs[tsKey] = millis
        }
        return newCount
    }

    suspend fun clearOtpLockAndFailDataFor(email: String) {
        val lockKey = lockUntilKeyFor(hashEmail(email))
        val countKey = failCountKeyFor(hashEmail(email))
        val tsKey = lastFailKeyFor(hashEmail(email))
        dataStore.edit { prefs ->
            prefs[lockKey] = 0L
            prefs[countKey] = 0
            prefs[tsKey] = 0L
        }
    }

    // Utility
    suspend fun clear() { dataStore.edit { prefs -> prefs.clear() } }
}