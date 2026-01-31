package com.example.truxpense.presentation.navigation

import android.os.Handler
import android.os.Looper
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

/**
 * Safely navigate ensuring the NavController.navigate call runs on the main thread.
 * Accepts an optional [navOptionsBuilder] lambda matching NavController.navigate(route, builder).
 */
fun NavController.safeNavigate(route: String, navOptionsBuilder: (NavOptionsBuilder.() -> Unit)? = null) {
    val perform = {
        if (navOptionsBuilder != null) {
            this.navigate(route, navOptionsBuilder)
        } else {
            this.navigate(route)
        }
    }

    if (Looper.myLooper() == Looper.getMainLooper()) {
        perform()
    } else {
        Handler(Looper.getMainLooper()).post { perform() }
    }
}
