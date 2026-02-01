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
            if (navOptionsBuilder != null) {
                this.navigate(route, navOptionsBuilder)
            } else {
                this.navigate(route)
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