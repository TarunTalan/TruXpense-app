package com.example.truxpense

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
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

        splashScreen.setKeepOnScreenCondition { isColdStart && keepSplashOn }

        Handler(Looper.getMainLooper()).postDelayed({
            Log.w("MainActivity", "Splash fallback timeout reached — dismissing system splash")
            keepSplashOn = false
        }, 5000)

        setContent {
            TruXpenseTheme(darkTheme = isSystemInDarkTheme()) {
                val navController = rememberNavController()
                Scaffold(modifier = androidx.compose.ui.Modifier.fillMaxSize()) { innerPadding: PaddingValues ->
                    val layoutDirection = LocalLayoutDirection.current

                    val contentPadding = PaddingValues(
                        start = innerPadding.calculateLeftPadding(layoutDirection) + 10.dp,
                        top = 10.dp ,
                        end = innerPadding.calculateRightPadding(layoutDirection) + 10.dp,
                        bottom = innerPadding.calculateBottomPadding() + 10.dp
                    )

                    Box(modifier = androidx.compose.ui.Modifier.padding(contentPadding)) {
                        AppNavHost(navController = navController, startDestination = Screen.Splash, contentPadding = contentPadding, onSplashEnter = {
                            Log.d("MainActivity", "onSplashEnter received — dismissing system splash")
                            keepSplashOn = false
                        })
                    }
                }
            }
        }

        try {
            val decorView = window.decorView
            val listener = object : android.view.ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    decorView.viewTreeObserver.removeOnPreDrawListener(this)
                    Log.d("MainActivity", "decorView pre-draw -> dismissing system splash")
                    keepSplashOn = false
                    return true
                }
            }
            decorView.viewTreeObserver.addOnPreDrawListener(listener)
        } catch (t: Throwable) {
            Log.w("MainActivity", "Failed to add decorView pre-draw listener: ${t.message}")
        }
    }
}
