package com.example.truxpense.presentation.navigation

import android.os.Handler
import android.os.Looper
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

fun NavController.safeNavigate(
    route: String,
    navOptionsBuilder: (NavOptionsBuilder.() -> Unit)? = null
) {
    // Post a tiny delay (~1 frame at 30fps) before navigating so we don't race
    // Compose's first layout/measuring on cold start. This prevents missing-first-frame
    // animations and reduces jank when the app restarts.
    Handler(Looper.getMainLooper()).postDelayed({
        try {
            if (navOptionsBuilder != null) {
                navigate(route, navOptionsBuilder)
            } else {
                navigate(route)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }, 32L)
}