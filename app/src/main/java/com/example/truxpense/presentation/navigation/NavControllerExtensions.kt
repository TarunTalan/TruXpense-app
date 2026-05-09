package com.example.truxpense.presentation.navigation

import android.os.Handler
import android.os.Looper
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

// Safe navigate helper that ensures calls run on main thread
fun NavController.safeNavigate(
    route: String,
    navOptionsBuilder: (NavOptionsBuilder.() -> Unit)? = null
) {
    val performNavigation = {
        try {
            // Always apply provided nav options, then force animations to zero so navigation appears instant
            this.navigate(route) {
                // Apply caller options first (popUpTo, launchSingleTop, etc.)
                navOptionsBuilder?.invoke(this)

                // Override animation to none for a snappier navigation experience
                anim {
                    enter = 0
                    exit = 0
                    popEnter = 0
                    popExit = 0
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    if (Looper.myLooper() == Looper.getMainLooper()) {
        performNavigation()
    } else {
        Handler(Looper.getMainLooper()).post {
            performNavigation()
        }
    }
}