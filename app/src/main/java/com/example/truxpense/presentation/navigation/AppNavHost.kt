package com.example.truxpense.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.truxpense.presentation.screens.auth.intro.IntroScreen
import com.example.truxpense.presentation.screens.auth.login.LoginScreen
import com.example.truxpense.presentation.screens.auth.signup.SignupScreen
import com.example.truxpense.presentation.screens.auth.otp.OtpScreen
import com.example.truxpense.presentation.screens.onboarding.username.UsernameScreen
import com.example.truxpense.presentation.screens.onboarding.permission.SmsPermission
import com.example.truxpense.presentation.screens.splash.SplashScreen
import com.example.truxpense.presentation.screens.home.HomeScreen
import com.example.truxpense.presentation.screens.onboarding.loading.LoadingScreen
import com.example.truxpense.presentation.screens.onboarding.complete.CompleteSetupScreen
import com.example.truxpense.presentation.screens.splash.SplashViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Splash,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onSplashEnter: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Splash Screen
        composable(Screen.Splash) {
            val viewModel: SplashViewModel = hiltViewModel()
            val onboardingComplete by viewModel.onboardingComplete.collectAsState(initial = false)

            Box(modifier = Modifier.padding(contentPadding)) {
                LaunchedEffect(Unit) { onSplashEnter() }

                SplashScreen(
                    onFinished = {
                        val destination = if (onboardingComplete) Screen.Home else Screen.Intro
                        navController.navigate(destination) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    }
                )
            }
        }

        // Intro Screen
        composable(Screen.Intro) {
            Box(modifier = Modifier.padding(contentPadding)) {
                IntroScreen(
                    onGetStarted = {
                        navController.navigate(Screen.Signup)
                    },
                    onLogin = {
                        navController.navigate(Screen.Login)
                    }
                )
            }
        }

        // Login Screen
        composable(Screen.Login) {
            Box(modifier = Modifier.padding(contentPadding)) {
                LoginScreen(
                    onBack = {
                        navController.navigate(Screen.Intro) {
                            popUpTo(Screen.Login) { inclusive = true }
                        }
                    },
                    onNavigateToOtp = {
                        navController.navigate(Screen.Otp)
                    },
                    onNavigateToHome = { token ->
                        // TODO: Save token
                        navController.navigate(Screen.Home) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                    onNavigateToSignup = {
                        navController.navigate(Screen.Signup) {
                            popUpTo(Screen.Login) { inclusive = true }
                        }
                    }
                )
            }
        }

        // Signup Screen
        composable(Screen.Signup) {
            Box(modifier = Modifier.padding(contentPadding)) {
                SignupScreen(
                    onBack = {
                        navController.navigate(Screen.Intro) {
                            popUpTo(Screen.Signup) { inclusive = true }
                        }
                    },
                    onNavigateToOtp = {
                        navController.navigate(Screen.Otp)
                    },
                    onNavigateToUsername = { token ->
                        // TODO: Save token
                        navController.navigate(Screen.Username)
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login) {
                            popUpTo(Screen.Signup) { inclusive = true }
                        }
                    }
                )
            }
        }

        // OTP Screen
        composable(Screen.Otp) {
            Box(modifier = Modifier.padding(contentPadding)) {
                val authFlowViewModel: AuthFlowViewModel = hiltViewModel()
                val flowType by authFlowViewModel.flow.collectAsState()

                OtpScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onVerified = {
                        // TODO: Save token
                        when (flowType) {
                            AuthFlowType.SIGNUP -> {
                                navController.navigate(Screen.Username) {
                                    popUpTo(Screen.Signup) { inclusive = true }
                                }
                            }
                            AuthFlowType.LOGIN -> {
                                navController.navigate(Screen.Home) {
                                    popUpTo(Screen.Splash) { inclusive = true }
                                }
                            }
                            null -> {
                                // Fallback if flow type not set
                                navController.navigate(Screen.Username)
                            }
                        }
                    },
                    onResend = {
                        // Resend logic handled in OtpViewModel
                    }
                )
            }
        }

        // Username Screen
        composable(Screen.Username) {
            Box(modifier = Modifier.padding(contentPadding)) {
                UsernameScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onComplete = {
                        navController.navigate(Screen.SmsPermission)
                    }
                )
            }
        }

        // SMS Permission Screen
        composable(Screen.SmsPermission) {
            Box(modifier = Modifier.padding(contentPadding)) {
                SmsPermission(
                    onAllow = {
                        navController.navigate(Screen.Loading)
                    },
                    onSkip = {
                        navController.navigate(Screen.Loading)
                    }
                )
            }
        }

        // Loading Screen
        composable(Screen.Loading) {
            Box(modifier = Modifier.padding(contentPadding)) {
                LoadingScreen(
                    onFinished = {
                        navController.navigate(Screen.CompleteSetup)
                    }
                )
            }
        }

        // Complete Setup Screen
        composable(Screen.CompleteSetup) {
            Box(modifier = Modifier.padding(contentPadding)) {
                CompleteSetupScreen(
                    onFinished = {
                        navController.navigate(Screen.Home) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    }
                )
            }
        }

        // Home Screen
        composable(Screen.Home) {
            Box(modifier = Modifier.padding(contentPadding)) {
                HomeScreen()
            }
        }
    }
}