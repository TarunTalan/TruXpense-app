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
import com.example.truxpense.presentation.screens.onboarding.currency.CurrencyScreen
import com.example.truxpense.presentation.screens.onboarding.currency.CurrencyViewModel
import com.example.truxpense.presentation.screens.onboarding.loading.LoadingScreen
import com.example.truxpense.presentation.screens.onboarding.permission.SmsPermission
import com.example.truxpense.presentation.screens.onboarding.username.UsernameScreen
import com.example.truxpense.presentation.screens.splash.SplashViewModel
import kotlinx.coroutines.flow.first


// App lifecycle stages used by splash routing
// Make public for use in SplashNavigator
enum class AppStage {
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
            SplashNavigator(
                navController = navController,
                contentPadding = contentPadding,
                onSplashEnter = onSplashEnter
            )
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
                val splashViewModel: SplashViewModel = hiltViewModel()
                // Mark current onboarding step so restart resumes here
                LaunchedEffect(Unit) { splashViewModel.saveOnboardingStep("username") }

                UsernameScreen(
                    onComplete = {
                        // Username is saved in UsernameViewModel along with onboarding step
                        // navigate to currency selection after username
                        navController.safeNavigate(Screen.Currency) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                    onSkip = {
                        // User skipped username -> save step and proceed to currency selection
                        splashViewModel.saveOnboardingStep("currency")
                        navController.safeNavigate(Screen.Currency) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    }
                )
            }
        }

        // ==================== CURRENCY SCREEN ====================
        composable(Screen.Currency) {
            Box(modifier = Modifier.padding(contentPadding)) {
                val splashViewModel: SplashViewModel = hiltViewModel()
                val currencyViewModel: CurrencyViewModel = hiltViewModel()

                CurrencyScreen(
                    onContinue = {
                        // Persist selected currency, then save step and proceed to sms permission
                        currencyViewModel.persistSelectedCurrency()
                        splashViewModel.saveOnboardingStep("sms_permission")
                        navController.safeNavigate(Screen.SmsPermission) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                    onSkip = {
                        // User skipped currency selection, save step and proceed to sms permission
                        // Ensure default currency is set if nothing selected
                        val sel = currencyViewModel.selectedCurrency.value
                        if (sel == null) {
                            val default = currencyViewModel.localeDefaultCurrency()
                            if (default != null) {
                                currencyViewModel.selectCurrency(default)
                                // persist the default choice
                                currencyViewModel.persistSelectedCurrency()
                            }
                        }
                        // persist current selection (if user had selected earlier)
                        currencyViewModel.persistSelectedCurrency()
                        splashViewModel.saveOnboardingStep("sms_permission")
                        navController.safeNavigate(Screen.SmsPermission) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                    onBack = {
                        // Navigate back to username screen
                        navController.safeNavigate(Screen.Username) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    }
                )
            }
        }

        // ==================== SMS PERMISSION SCREEN ====================
        composable(Screen.SmsPermission) {
            Box(modifier = Modifier.padding(contentPadding)) {
                val splashViewModel: SplashViewModel = hiltViewModel()

                SmsPermission(
                    onAllow = {
                        // Save onboarding step before navigating
                        splashViewModel.saveOnboardingStep("loading")
                        navController.safeNavigate(Screen.Loading)
                    },
                    onSkip = {
                        // Save onboarding step before navigating
                        splashViewModel.saveOnboardingStep("loading")
                        navController.safeNavigate(Screen.Loading)
                    }
                )
            }
        }

        // ==================== LOADING SCREEN ====================
        composable(Screen.Loading) {
            Box(modifier = Modifier.padding(contentPadding)) {
                val splashViewModel: SplashViewModel = hiltViewModel()

                LoadingScreen(
                    onFinished = {
                        // Save onboarding step before navigating
                        splashViewModel.saveOnboardingStep("complete_setup")
                        navController.safeNavigate(Screen.CompleteSetup)
                    }
                )
            }
        }

        // ==================== COMPLETE SETUP SCREEN ====================
        composable(Screen.CompleteSetup) {
            Box(modifier = Modifier.padding(contentPadding)) {
                val splashViewModel: SplashViewModel = hiltViewModel()

                CompleteSetupScreen(
                    onFinished = {
                        // Mark onboarding as complete and clear the step
                        splashViewModel.markOnboardingComplete()
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
 * onboardingStep takes precedence to keep the user inside onboarding
 */
fun determineAppStage(
    onboardingComplete: Boolean,
    signupStarted: Boolean,
    accessToken: String?,
    username: String?,
    onboardingStep: String?
): AppStage {
    return when {
        // Fully onboarded user
        onboardingComplete -> AppStage.HOME

        // If any onboarding step is saved (username/currency/sms/loading/complete_setup), remain in onboarding
        !onboardingStep.isNullOrBlank() && onboardingStep != "completed" -> AppStage.ONBOARDING

        // User started signup or has token but no username
        signupStarted || (!accessToken.isNullOrBlank() && username.isNullOrBlank()) -> AppStage.ONBOARDING

        // Default: needs authentication
        else -> AppStage.AUTH
    }
}


/**
 * Determine which onboarding screen to resume from based on saved step
 * This enables users to continue from where they left off if app restarts
 *
 * Special behavior: If user was at SMS Permission or beyond, auto-complete
 * onboarding and login user directly to Home screen (critical data already collected)
 */
fun determineOnboardingScreen(
    onboardingStep: String?,
    username: String?
): String {
    return when (onboardingStep) {
        "username" -> {
            Screen.Username
        }

        "currency" -> {
            Screen.Currency
        }

        "sms_permission", "loading", "complete_setup" -> {
            Screen.Home
        }

        "completed" -> {
            // Onboarding was completed, go to home
            Screen.Home
        }

        else -> {
            // No saved step or unknown step - start from beginning
            // Also restart if username is blank (safety check)
            if (username.isNullOrBlank()) {
                Screen.Username
            } else {
                // Username exists but step is unclear, resume at currency
                Screen.Currency
            }
        }
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