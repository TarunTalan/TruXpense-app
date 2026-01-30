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

            // Read persisted values
            val onboardingComplete by viewModel.onboardingComplete.collectAsState(initial = false)
            val accessToken by viewModel.accessToken.collectAsState(initial = null)
            val username by viewModel.username.collectAsState(initial = null)
            val signupStarted by viewModel.signupStarted.collectAsState(initial = false)

            Box(modifier = Modifier.padding(contentPadding)) {
                LaunchedEffect(Unit) { onSplashEnter() }

                SplashScreen(
                    onFinished = {
                        // routing decision:
                        // 1) If accessToken exists -> Home
                        // 2) else if username exists -> Username
                        // 3) else if signupStarted -> Username (user started signup previously)
                        // 4) else if onboardingComplete -> Intro
                        // 5) else -> Intro (default)
                        val destination = when {
                            !accessToken.isNullOrBlank() -> Screen.Home
                            !username.isNullOrBlank() -> Screen.Username
                            signupStarted -> Screen.Username
                            onboardingComplete -> Screen.Intro
                            else -> Screen.Intro
                        }

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
                    onNavigateToOtp = { email, flow ->
                        // store flow/email on the current backstack entry's SavedStateHandle so OTP dest can read it
                        navController.currentBackStackEntry?.savedStateHandle?.set("auth_email", email)
                        navController.currentBackStackEntry?.savedStateHandle?.set("auth_flow", flow.name)
                        navController.navigate(Screen.Otp)
                    },
                    onNavigateToHome = { _ ->
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
                    onNavigateToOtp = { email, flow ->
                        // store flow/email on the current backstack entry's SavedStateHandle so OTP dest can read it
                        navController.currentBackStackEntry?.savedStateHandle?.set("auth_email", email)
                        navController.currentBackStackEntry?.savedStateHandle?.set("auth_flow", flow.name)
                        navController.navigate(Screen.Otp)
                    },
                    onNavigateToUsername = { _ ->
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
                // Read saved auth_flow/auth_email from the previous backstack entry (where we stored them)
                val prevEntry = navController.previousBackStackEntry
                val savedFlowName: String? = prevEntry?.savedStateHandle?.get<String>("auth_flow")
                val savedEmail: String? = prevEntry?.savedStateHandle?.get<String>("auth_email")
                val savedFlow: AuthFlowType? = when (savedFlowName) {
                    AuthFlowType.SIGNUP.name -> AuthFlowType.SIGNUP
                    AuthFlowType.LOGIN.name -> AuthFlowType.LOGIN
                    else -> null
                }

                OtpScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onVerified = {
                        // TODO: Save token
                        when (savedFlow) {
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
                            else -> {
                                // Fallback if flow type not set
                                navController.navigate(Screen.Username)
                            }
                        }
                    },
                    onResend = {
                        // Resend logic handled in OtpViewModel
                    },
                    authEmailParam = savedEmail,
                    flowParam = savedFlow
                )
            }
        }

        // Username Screen
        composable(Screen.Username) {
            Box(modifier = Modifier.padding(contentPadding)) {
                UsernameScreen(
                    onBack = {
                        navController.navigate(Screen.Intro) {
                            popUpTo(Screen.Username) { inclusive = true }
                        }
                    },
                    onComplete = {
                        // TODO: Save username
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
                HomeScreen(onLogout = {
                    // Clear back stack and navigate to Intro
                    navController.navigate(Screen.Intro) {
                        popUpTo(Screen.Home) { inclusive = true }
                    }
                })
            }
        }

        // Loading Screen
        composable(Screen.Loading) {
            Box(modifier = Modifier.padding(contentPadding)) {
                LoadingScreen(onFinished = {
                    navController.navigate(Screen.CompleteSetup)
                })
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

        // SMS Permission Screen
        composable(Screen.SmsPermission) {
            Box(modifier = Modifier.padding(contentPadding)) {
                SmsPermission(
                    onAllow = {
                        navController.navigate(Screen.Loading)
                    },
                    onSkip = {
                        // On skip navigate to Loading so it shows continue without SMS flow
                        navController.navigate(Screen.Loading)
                    }
                )
            }
        }
    }
}
