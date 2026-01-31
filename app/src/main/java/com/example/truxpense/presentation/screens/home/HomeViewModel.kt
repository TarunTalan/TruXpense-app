package com.example.truxpense.presentation.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.prefs.AuthPreferences
import com.google.android.gms.auth.api.identity.Identity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HomeViewModel handles logout including clearing local data and signing out of Google if needed.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prefs: AuthPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Expose stored username Flow from preferences so UI can collect and display it
    val username: Flow<String?> = prefs.username

    fun logout(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                // clear all saved preferences (tokens, username, onboarding flags)
                prefs.clear()

                // Attempt to sign out from Google Identity (One Tap) if configured. Ignore failures.
                try {
                    val clientId = try { context.getString(com.example.truxpense.R.string.default_web_client_id) } catch (_: Exception) { null }
                    if (!clientId.isNullOrBlank() && !clientId.startsWith("REPLACE_WITH")) {
                        val oneTapClient = Identity.getSignInClient(context)
                        oneTapClient.signOut().addOnCompleteListener { /* no-op */ }
                    }
                } catch (_: Throwable) {
                    // ignore sign-out errors; logout still proceeds
                }
            } finally {
                onComplete?.invoke()
            }
        }
    }
}
