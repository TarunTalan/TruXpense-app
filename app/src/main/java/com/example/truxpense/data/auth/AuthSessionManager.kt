package com.example.truxpense.data.auth

import com.example.truxpense.data.prefs.AuthPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages session logout events. UI can collect `logoutEvents` to navigate to login/intro.
 */
@Singleton
class AuthSessionManager @Inject constructor(private val prefs: AuthPreferences) {

    private val _logoutEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val logoutEvents = _logoutEvents.asSharedFlow()

    fun handleRefreshFailure() {
        // Clear stored tokens and emit a logout event
        CoroutineScope(Dispatchers.IO).launch {
            prefs.clear()
            _logoutEvents.tryEmit(Unit)
        }
    }
}
