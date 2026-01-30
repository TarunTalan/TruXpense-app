package com.example.truxpense.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        private const val DATASTORE_NAME = "auth_prefs"
        private val Context.dataStore by preferencesDataStore(DATASTORE_NAME)
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val EXPIRES_IN = longPreferencesKey("expires_in")
        private val USERNAME = stringPreferencesKey("username")
        private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    suspend fun saveTokens(access: String, refresh: String, expiresIn: Long) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = access
            prefs[REFRESH_TOKEN] = refresh
            prefs[EXPIRES_IN] = expiresIn
        }
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    val expiresIn: Flow<Long?> = context.dataStore.data.map { it[EXPIRES_IN] }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    // Username and onboarding flag helpers
    suspend fun saveUsername(username: String) {
        context.dataStore.edit { prefs ->
            prefs[USERNAME] = username
        }
    }

    val username: Flow<String?> = context.dataStore.data.map { it[USERNAME] }

    suspend fun setOnboardingComplete(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETE] = value
        }
    }

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_COMPLETE] ?: false }
}
