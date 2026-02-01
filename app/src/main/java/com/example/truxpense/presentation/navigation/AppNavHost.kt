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
import com.example.truxpense.presentation.screens.auth.intro.IntroViewModel
import com.example.truxpense.presentation.screens.auth.login.LoginScreen
import com.example.truxpense.presentation.screens.auth.otp.OtpScreen
import com.example.truxpense.presentation.screens.auth.signup.SignupScreen
import com.example.truxpense.presentation.screens.home.HomeScreen
import com.example.truxpense.presentation.screens.onboarding.complete.CompleteSetupScreen
import com.example.truxpense.presentation.screens.onboarding.loading.LoadingScreen
import com.example.truxpense.presentation.screens.onboarding.permission.SmsPermission
import com.example.truxpense.presentation.screens.onboarding.username.UsernameScreen
import com.example.truxpense.presentation.screens.splash.SplashScreen
import com.example.truxpense.presentation.screens.splash.SplashViewModel
import kotlinx.coroutines.flow.first

/**
 * App lifecycle stages used by splash routing
 */
private enum class AppStage {
    AUTH,        // User needs to login/signup
    ONBOARDING,  // User authenticated but needs to complete onboarding
    HOME         // User fully onboarded, go to home
}

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
        // ==================== SPLASH SCREEN ====================
        composable(Screen.Splash) {
            val viewModel: SplashViewModel = hiltViewModel()

            val onboardingComplete by viewModel.onboardingComplete.collectAsState(initial = false)
            val accessToken by viewModel.accessToken.collectAsState(initial = null)
            val username by viewModel.username.collectAsState(initial = null)
            val signupStarted by viewModel.signupStarted.collectAsState(initial = false)

            Box(modifier = Modifier.padding(contentPadding)) {
                LaunchedEffect(Unit) {
                    onSplashEnter()
                }

                SplashScreen(
                    onFinished = {
                        val stage = determineAppStage(
                            onboardingComplete = onboardingComplete,
                            signupStarted = signupStarted,
                            accessToken = accessToken,
                            username = username
                        )

                        val destination = when (stage) {
                            AppStage.HOME -> Screen.Home
                            AppStage.ONBOARDING -> Screen.Username
                            AppStage.AUTH -> Screen.Intro
                        }

                        navController.safeNavigate(destination) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    }
                )
            }
        }

        // ==================== INTRO SCREEN ====================
        composable(Screen.Intro) {
            Box(modifier = Modifier.padding(contentPadding)) {
                val introViewModel: IntroViewModel = hiltViewModel()
                val introState by introViewModel.state.collectAsState()

                // Handle navigation from IntroViewModel (OAuth flows)
                LaunchedEffect(introState.navigateToHome, introState.navigateToUsername) {
                    when {
                        introState.navigateToHome -> {
                            navController.safeNavigate(Screen.Home) {
                                popUpTo(Screen.Splash) { inclusive = true }
                            }
                        }
                        introState.navigateToUsername -> {
                            navController.safeNavigate(Screen.Username) {
                                popUpTo(Screen.Splash) { inclusive = true }
                            }
                        }
                    }
                }

                IntroScreen(
                    onGetStarted = {
                        navController.safeNavigate(Screen.Signup)
                    }
                )
            }
        }

        // ==================== LOGIN SCREEN ====================
        composable(Screen.Login) {
            Box(modifier = Modifier.padding(contentPadding)) {
                LoginScreen(
                    onBack = {
                        navController.safeNavigate(Screen.Intro) {
                            popUpTo(Screen.Login) { inclusive = true }
                        }
                    },
                    onNavigateToOtp = { email, flow ->
                        // Store auth context in SavedStateHandle for OTP screen
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            set(NavKeys.AUTH_EMAIL, email)
                            set(NavKeys.AUTH_FLOW, flow.name)
                        }
                        navController.safeNavigate(Screen.Otp)
                    },
                    onNavigateToHome = { _ ->
                        navController.safeNavigate(Screen.Home) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                    onNavigateToUsername = { _ ->
                        navController.safeNavigate(Screen.Username) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                    onNavigateToSignup = {
                        navController.safeNavigate(Screen.Signup) {
                            popUpTo(Screen.Login) { inclusive = true }
                        }
                    }
                )
            }
        }

        // ==================== SIGNUP SCREEN ====================
        composable(Screen.Signup) {
            Box(modifier = Modifier.padding(contentPadding)) {
                SignupScreen(
                    onBack = {
                        navController.safeNavigate(Screen.Intro) {
                            popUpTo(Screen.Signup) { inclusive = true }
                        }
                    },
                    onNavigateToOtp = { email, flow ->
                        // Store auth context in SavedStateHandle for OTP screen
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            set(NavKeys.AUTH_EMAIL, email)
                            set(NavKeys.AUTH_FLOW, flow.name)
                        }
                        navController.safeNavigate(Screen.Otp)
                    },
                    onNavigateToUsername = { _ ->
                        navController.safeNavigate(Screen.Username) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.safeNavigate(Screen.Login) {
                            popUpTo(Screen.Signup) { inclusive = true }
                        }
                    }
                )
            }
        }

        // ==================== OTP SCREEN ====================
        composable(Screen.Otp) {
            Box(modifier = Modifier.padding(contentPadding)) {
                // Retrieve auth context from previous screen's SavedStateHandle
                val authContext = retrieveAuthContext(navController)

                val splashViewModel: SplashViewModel = hiltViewModel()
                var otpVerifiedTrigger by remember { mutableStateOf(false) }

                // Handle post-verification routing
                LaunchedEffect(otpVerifiedTrigger) {
                    if (!otpVerifiedTrigger) return@LaunchedEffect

                    val destination = determinePostOtpDestination(splashViewModel)

                    navController.safeNavigate(destination) {
                        popUpTo(Screen.Splash) { inclusive = true }
                    }

                    otpVerifiedTrigger = false
                }

                OtpScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onVerified = {
                        otpVerifiedTrigger = true
                    },
                    onResend = {
                        // Resend handled in OtpViewModel
                    },
                    authEmailParam = authContext.email,
                    flowParam = authContext.flow
                )
            }
        }

        // ==================== USERNAME SCREEN ====================
        composable(Screen.Username) {
            Box(modifier = Modifier.padding(contentPadding)) {
                UsernameScreen(
                    onComplete = {
                        navController.safeNavigate(Screen.SmsPermission) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    }
                )
            }
        }

        // ==================== SMS PERMISSION SCREEN ====================
        composable(Screen.SmsPermission) {
            Box(modifier = Modifier.padding(contentPadding)) {
                SmsPermission(
                    onAllow = {
                        navController.safeNavigate(Screen.Loading)
                    },
                    onSkip = {
                        navController.safeNavigate(Screen.Loading)
                    }
                )
            }
        }

        // ==================== LOADING SCREEN ====================
        composable(Screen.Loading) {
            Box(modifier = Modifier.padding(contentPadding)) {
                LoadingScreen(
                    onFinished = {
                        navController.safeNavigate(Screen.CompleteSetup)
                    }
                )
            }
        }

        // ==================== COMPLETE SETUP SCREEN ====================
        composable(Screen.CompleteSetup) {
            Box(modifier = Modifier.padding(contentPadding)) {
                CompleteSetupScreen(
                    onFinished = {
                        navController.safeNavigate(Screen.Home) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    }
                )
            }
        }

        // ==================== HOME SCREEN ====================
        composable(Screen.Home) {
            Box(modifier = Modifier.padding(contentPadding)) {
                HomeScreen(
                    onLogout = {
                        navController.safeNavigate(Screen.Intro) {
                            popUpTo(Screen.Home) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

// ==================== HELPER FUNCTIONS ====================

/**
 * Determine which app stage the user is in based on their state
 */
private fun determineAppStage(
    onboardingComplete: Boolean,
    signupStarted: Boolean,
    accessToken: String?,
    username: String?
): AppStage {
    return when {
        // Fully onboarded user
        onboardingComplete -> AppStage.HOME

        // User started signup or has token but no username
        signupStarted || (!accessToken.isNullOrBlank() && username.isNullOrBlank()) -> {
            AppStage.ONBOARDING
        }

        // Default: needs authentication
        else -> AppStage.AUTH
    }
}

/**
 * Data class to hold auth context
 */
private data class AuthContext(
    val email: String?,
    val flow: AuthFlowType?
)

/**
 * Retrieve auth context from navigation
 */
private fun retrieveAuthContext(navController: NavHostController): AuthContext {
    val prevEntry = navController.previousBackStackEntry
    val currentEntry = navController.currentBackStackEntry

    // Try SavedStateHandle first (in-app navigation)
    val savedEmail = prevEntry?.savedStateHandle?.get<String>(NavKeys.AUTH_EMAIL)
    val savedFlowName = prevEntry?.savedStateHandle?.get<String>(NavKeys.AUTH_FLOW)

    // Fallback to nav arguments (deep links)
    val argEmail = currentEntry?.arguments?.getString(NavKeys.AUTH_EMAIL)
    val argFlowName = currentEntry?.arguments?.getString(NavKeys.AUTH_FLOW)

    val email = savedEmail ?: argEmail
    val flowName = savedFlowName ?: argFlowName

    val flow = when (flowName) {
        AuthFlowType.SIGNUP.name -> AuthFlowType.SIGNUP
        AuthFlowType.LOGIN.name -> AuthFlowType.LOGIN
        else -> null
    }

    return AuthContext(email, flow)
}

/**
 * Determine destination after OTP verification
 */
private suspend fun determinePostOtpDestination(
    splashViewModel: SplashViewModel
): String {
    val persistedUsername = try {
        splashViewModel.username.first()
    } catch (_: Exception) {
        null
    }

    return if (!persistedUsername.isNullOrBlank()) {
        Screen.Home
    } else {
        Screen.Username
    }
}

/**
 * Navigation keys for SavedStateHandle
 */
private object NavKeys {
    const val AUTH_EMAIL = "auth_email"
    const val AUTH_FLOW = "auth_flow"
}