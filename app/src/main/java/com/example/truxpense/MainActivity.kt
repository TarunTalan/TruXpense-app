package com.example.truxpense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.truxpense.presentation.screens.auth.login.LoginScreen
import com.example.truxpense.presentation.theme.TruXpenseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TruXpenseTheme(darkTheme = isSystemInDarkTheme()) {
                val navController = rememberNavController()
                Scaffold(modifier = androidx.compose.ui.Modifier.fillMaxSize()) { contentPadding ->
                    Box(modifier = androidx.compose.ui.Modifier) {
                        NavHost(navController = navController, startDestination = "intro") {
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
                                    onSignUpNavigate = { navController.navigate("signup") }
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
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
