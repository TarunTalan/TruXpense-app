package com.example.truxpense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.truxpense.presentation.screens.auth.login.LoginScreen
import com.example.truxpense.presentation.screens.auth.otp.OtpScreen
import com.example.truxpense.presentation.screens.splash.SplashScreen
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

        setContent {
            TruXpenseTheme(darkTheme = isSystemInDarkTheme()) {
                val navController = rememberNavController()
                Scaffold(modifier = androidx.compose.ui.Modifier.fillMaxSize()) { _ ->
                    Box(modifier = androidx.compose.ui.Modifier) {
                        NavHost(navController = navController, startDestination = "splash") {
                            composable("splash") {
                                SplashScreen(
                                    onEnter = {
                                        // dismiss system splash immediately when Compose content is ready
                                        keepSplashOn = false
                                    },
                                    onFinished = {
                                        // navigate to intro and remove splash from the back stack
                                        navController.navigate("intro") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("intro") {
                                com.example.truxpense.presentation.screens.auth.intro.IntroScreen(
                                    onGetStarted = { navController.navigate("signup") },
                                    onLogin = { navController.navigate("login") }
                                )
                            }
                            composable("login") {
                                LoginScreen(
                                    onBack = {
                                        // From login, back should go to intro
                                        if (!navController.popBackStack("intro", false)) {
                                            finish()
                                        }
                                    },
                                    onSignUpNavigate = { navController.navigate("signup") },
                                    onOtpNavigate = { navController.navigate("otp") }
                                )
                            }
                            composable("signup") {
                                com.example.truxpense.presentation.screens.auth.signup.SignUpScreen(
                                    onBack = { navController.popBackStack() },
                                    onLoginNavigate = {
                                        // Navigate back to login (pop if possible, otherwise navigate)
                                        if (!navController.popBackStack("login", false)) {
                                            navController.navigate("login")
                                        }
                                    },
                                    onOtpNavigate = { navController.navigate("otp") }
                                )
                            }
                            composable("otp") {
                                OtpScreen(
                                    onBack = { navController.popBackStack() },
                                    onVerified = {
                                        // after verifying OTP, navigate to intro or main screen
                                        navController.popBackStack("login", true)
                                        navController.popBackStack("signup", true)
                                        navController.navigate("intro")
                                    },
                                    onResend = { /* handle resend */ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
