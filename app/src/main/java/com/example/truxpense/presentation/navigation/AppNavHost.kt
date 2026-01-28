package com.example.truxpense.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.truxpense.presentation.screens.auth.intro.IntroScreen
import com.example.truxpense.presentation.screens.auth.login.LoginScreen
import com.example.truxpense.presentation.screens.auth.signup.SignUpScreen
import com.example.truxpense.presentation.screens.auth.otp.OtpScreen
import com.example.truxpense.presentation.screens.onboarding.username.UsernameScreen
import com.example.truxpense.presentation.screens.onboarding.permission.SmsPermission
import com.example.truxpense.presentation.screens.splash.SplashScreen
import com.example.truxpense.presentation.screens.home.HomeScreen
import com.example.truxpense.presentation.screens.onboarding.loading.LoadingScreen
import com.example.truxpense.presentation.screens.onboarding.complete.CompleteSetupScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Splash,
    onSplashEnter: () -> Unit = {}
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Splash) {
            SplashScreen(
                onEnter = { onSplashEnter() },
                onFinished = { navController.navigate(Screen.Intro) {
                    popUpTo(Screen.Splash) { inclusive = true }
                } }
            )
        }
        composable(Screen.Intro) {
            IntroScreen(
                onGetStarted = { navController.navigate(Screen.Signup) },
                onLogin = { navController.navigate(Screen.Login) }
            )
        }
        composable(Screen.Login) {
            LoginScreen(
                onBack = {
                    if (!navController.popBackStack(Screen.Intro, false)) {
                        navController.navigateUp()
                    }
                },
                onSignUpNavigate = { navController.navigate(Screen.Signup) },
                onOtpNavigate = { navController.navigate(Screen.Otp) }
            )
        }
        composable(Screen.Signup) {
            SignUpScreen(
                onBack = { navController.popBackStack() },
                onLoginNavigate = {
                    if (!navController.popBackStack(Screen.Login, false)) {
                        navController.navigate(Screen.Login)
                    }
                },
                onOtpNavigate = { navController.navigate(Screen.Otp) }
            )
        }
        composable(Screen.Otp) {
            OtpScreen(
                onBack = { navController.popBackStack() },
                onVerified = {
                    navController.popBackStack(Screen.Login, true)
                    navController.popBackStack(Screen.Signup, true)
                    navController.navigate(Screen.Username)
                },
                onResend = { /* implement if needed */ }
            )
        }
        composable(Screen.Username) {
            UsernameScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Screen.SmsPermission) }
            )
        }
        composable(Screen.SmsPermission) {
            SmsPermission(
                onAllow = { navController.navigate(Screen.Loading) },
                onSkip = { navController.navigate(Screen.Loading) }
            )
        }
        composable(Screen.Loading) {
            LoadingScreen(onFinished = { navController.navigate(Screen.CompleteSetup) })
        }
        composable(Screen.CompleteSetup) {
            CompleteSetupScreen(onFinished = { navController.navigate(Screen.Home) })
        }
        composable(Screen.Home) {
            HomeScreen()
        }
    }
}
