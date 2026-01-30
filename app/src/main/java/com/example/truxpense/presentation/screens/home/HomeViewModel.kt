package com.example.truxpense.presentation.screens.home

import android.content.Context
import com.example.truxpense.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.prefs.AuthPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Suppress("DEPRECATION")
    fun logout(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                // clear all saved preferences (tokens, username, onboarding flags)
                prefs.clear()

                // Attempt to sign out from Google if configured. Ignore failures.
                try {
                    val clientId = try {
                        context.getString(R.string.default_web_client_id)
                    } catch (_: Exception) {
                        null
                    }

                    if (!clientId.isNullOrBlank() && !clientId.startsWith("REPLACE_WITH")) {
                        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                        ).requestIdToken(clientId).requestEmail().build()

                        val googleClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                        // signOut returns a Task — run it and ignore the result (best-effort)
                        googleClient.signOut().addOnCompleteListener { /* no-op */ }
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
