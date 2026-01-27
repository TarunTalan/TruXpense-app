package com.example.truxpense.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthPreferences(private val context: Context) {
    companion object {
        private const val DATASTORE_NAME = "auth_prefs"
        private val Context.dataStore by preferencesDataStore(DATASTORE_NAME)
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val EXPIRES_IN = longPreferencesKey("expires_in")
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
}
