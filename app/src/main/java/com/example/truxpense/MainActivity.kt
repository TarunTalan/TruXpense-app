package com.example.truxpense

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.example.truxpense.presentation.navigation.AppNavHost
import com.example.truxpense.presentation.navigation.Screen
import com.example.truxpense.presentation.theme.TruXpenseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Volatile
    private var keepSplashOn = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Determine whether this is a cold start. If not (e.g. theme change), don't keep system splash.
        val isColdStart = savedInstanceState == null

        // Install the system splash screen
        val splashScreen: SplashScreen = installSplashScreen()

        // Add a quick exit animation so the system splash doesn't show a white frame when dismissed
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val view = splashScreenViewProvider.view
            // fade out quickly
            view.animate().alpha(0f).setDuration(120).withEndAction {
                // remove the splash view when animation ends
                splashScreenViewProvider.remove()
            }
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep the system splash only for cold starts while our Compose splash is not ready
        splashScreen.setKeepOnScreenCondition { isColdStart && keepSplashOn }

        // Safety fallback: if something blocks the Compose splash entering (rare), ensure the
        // system splash is dismissed after a short timeout to avoid a permanent frozen splash.
        Handler(Looper.getMainLooper()).postDelayed({
            keepSplashOn = false
        }, 5000)

        setContent {
            TruXpenseTheme(darkTheme = isSystemInDarkTheme()) {
                val navController = rememberNavController()
                Scaffold(modifier = androidx.compose.ui.Modifier.fillMaxSize()) { _ ->
                    Box(modifier = androidx.compose.ui.Modifier) {
                        AppNavHost(navController = navController, startDestination = Screen.Splash, onSplashEnter = { keepSplashOn = false })
                    }
                }
            }
        }
    }
}
