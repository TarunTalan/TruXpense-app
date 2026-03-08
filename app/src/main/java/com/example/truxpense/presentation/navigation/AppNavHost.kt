package com.example.truxpense.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import com.example.truxpense.presentation.screens.auth.AuthFlowType
import com.example.truxpense.presentation.screens.auth.intro.IntroScreen
import com.example.truxpense.presentation.screens.auth.intro.IntroViewModel
import com.example.truxpense.presentation.screens.auth.login.LoginScreen
import com.example.truxpense.presentation.screens.auth.otp.OtpScreen
import com.example.truxpense.presentation.screens.auth.signup.SignupScreen
import com.example.truxpense.presentation.screens.dashboard.home.DashboardScreen
import com.example.truxpense.presentation.screens.onboarding.currency.CurrencyScreen
import com.example.truxpense.presentation.screens.onboarding.currency.CurrencyViewModel
import com.example.truxpense.presentation.screens.onboarding.loading.LoadingScreen
import com.example.truxpense.presentation.screens.onboarding.permission.SmsPermission
import com.example.truxpense.presentation.screens.onboarding.username.UsernameScreen
import com.example.truxpense.presentation.screens.splash.SplashViewModel
import kotlinx.coroutines.flow.first
import com.example.truxpense.presentation.navigation.slideInFromLeft
import com.example.truxpense.presentation.navigation.slideInFromRight
import com.example.truxpense.presentation.navigation.slideOutToLeft
import com.example.truxpense.presentation.navigation.slideOutToRight

/**
 * Root navigation host for the entire application.
 *
 * Responsibilities:
 *  - Auth flow: Splash → Intro → Signup/Login → OTP
 *  - Onboarding flow: Username → Currency → SmsPermission → Loading
 *  - Dashboard: single [Screen.Dashboard.Root] composable that owns
 *    its own bottom-nav NavController and sub-graph (see [DashboardScreen]).
 *
 * What is intentionally NOT here:
 *  - AddExpense, AddBudget, BudgetDetail — these are inner-dashboard
 *    destinations managed by DashboardScreen's own NavController.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Splash,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    onSplashEnter: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        // Slide transitions for auth + onboarding flow
        enterTransition    = { slideInFromRight() },
        exitTransition     = { slideOutToLeft()   },
        popEnterTransition = { slideInFromLeft()  },
        popExitTransition  = { slideOutToRight()  },
    ) {

        // ══════════════════════════════════════════════════════════════════════
        // SPLASH  — instant, no transition
        // ══════════════════════════════════════════════════════════════════════
        composable(
            route = Screen.Splash,
            enterTransition    = { EnterTransition.None },
            exitTransition     = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition  = { ExitTransition.None },
        ) {
            SplashNavigator(
                navController = navController,
                contentPadding = contentPadding,
                onSplashEnter = onSplashEnter,
            )
        }

        // ══════════════════════════════════════════════════════════════════════
        // INTRO
        // ══════════════════════════════════════════════════════════════════════
        composable(Screen.Intro) {
            Box(modifier = Modifier.padding(contentPadding)) {
                val introViewModel: IntroViewModel = hiltViewModel()
                val introState by introViewModel.state.collectAsState()

                LaunchedEffect(introState.navigateToHome, introState.navigateToUsername) {
                    when {
                        introState.navigateToHome -> navController.safeNavigate(Screen.Dashboard.Root) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }

                        introState.navigateToUsername -> navController.safeNavigate(Screen.Username) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    }
                }

                IntroScreen(onGetStarted = { navController.safeNavigate(Screen.Signup) })
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // LOGIN
        // ══════════════════════════════════════════════════════════════════════
        composable(Screen.Login) {
            Box(modifier = Modifier.padding(contentPadding)) {
                LoginScreen(
                    onBack = {
                        navController.safeNavigate(Screen.Intro) {
                            popUpTo(Screen.Login) { inclusive = true }
                        }
                    },
                    onNavigateToOtp = { email, flow: AuthFlowType ->
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            set<String>(NavKeys.AUTH_EMAIL, email)
                            set<String>(NavKeys.AUTH_FLOW, flow.name)
                        }
                        navController.safeNavigate(Screen.Otp)
                    },
                    onNavigateToHome = {
                        navController.safeNavigate(Screen.Dashboard.Root) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                    onNavigateToUsername = {
                        navController.safeNavigate(Screen.Username) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                    onNavigateToSignup = {
                        navController.safeNavigate(Screen.Signup) {
                            popUpTo(Screen.Login) { inclusive = true }
                        }
                    },
                )
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // SIGNUP
        // ══════════════════════════════════════════════════════════════════════
        composable(Screen.Signup) {
            Box(modifier = Modifier.padding(contentPadding)) {
                SignupScreen(
                    onBack = {
                        navController.safeNavigate(Screen.Intro) {
                            popUpTo(Screen.Signup) { inclusive = true }
                        }
                    },
                    onNavigateToOtp = { email, flow: AuthFlowType ->
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            set<String>(NavKeys.AUTH_EMAIL, email)
                            set<String>(NavKeys.AUTH_FLOW, flow.name)
                        }
                        navController.safeNavigate(Screen.Otp)
                    },
                    onNavigateToUsername = {
                        navController.safeNavigate(Screen.Username) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.safeNavigate(Screen.Login) {
                            popUpTo(Screen.Signup) { inclusive = true }
                        }
                    },
                )
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // OTP
        // ══════════════════════════════════════════════════════════════════════
        composable(Screen.Otp) {
            Box(modifier = Modifier.padding(contentPadding)) {
                val authContext = retrieveAuthContext(navController)
                val splashViewModel: SplashViewModel = hiltViewModel()
                var otpVerifiedTrigger by remember { mutableStateOf(false) }

                LaunchedEffect(otpVerifiedTrigger) {
                    if (!otpVerifiedTrigger) return@LaunchedEffect
                    val destination = determinePostOtpDestination(splashViewModel, authContext.flow)
                    navController.safeNavigate(destination) {
                        popUpTo(Screen.Splash) { inclusive = true }
                    }
                    otpVerifiedTrigger = false
                }

                OtpScreen(
                    onBack = { navController.popBackStack() },
                    onVerified = { otpVerifiedTrigger = true },
                    onResend = { /* Resend handled in OtpViewModel */ },
                    authEmailParam = authContext.email,
                    flowParam = authContext.flow,
                )
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // ONBOARDING — USERNAME
        // ══════════════════════════════════════════════════════════════════════
        composable(Screen.Username) {
            Box(modifier = Modifier.padding(contentPadding)) {
                val splashViewModel: SplashViewModel = hiltViewModel()
                LaunchedEffect(Unit) { splashViewModel.saveOnboardingStep("username") }

                UsernameScreen(
                    onComplete = {
                        navController.safeNavigate(Screen.Currency) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                    onSkip = {
                        splashViewModel.saveOnboardingStep("currency")
                        navController.safeNavigate(Screen.Currency) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                )
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // ONBOARDING — CURRENCY
        // ══════════════════════════════════════════════════════════════════════
        composable(Screen.Currency) {
            Box(modifier = Modifier.padding(contentPadding)) {
                val splashViewModel: SplashViewModel = hiltViewModel()
                val currencyViewModel: CurrencyViewModel = hiltViewModel()

                CurrencyScreen(
                    onContinue = {
                        currencyViewModel.persistSelectedCurrency()
                        splashViewModel.saveOnboardingStep("sms_permission")
                        navController.safeNavigate(Screen.SmsPermission) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                    onSkip = {
                        val sel = currencyViewModel.selectedCurrency.value
                        if (sel == null) {
                            currencyViewModel.localeDefaultCurrency()?.let { default ->
                                currencyViewModel.selectCurrency(default)
                            }
                        }
                        currencyViewModel.persistSelectedCurrency()
                        splashViewModel.saveOnboardingStep("sms_permission")
                        navController.safeNavigate(Screen.SmsPermission) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                    onBack = {
                        navController.safeNavigate(Screen.Username) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                )
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // ONBOARDING — SMS PERMISSION
        // ══════════════════════════════════════════════════════════════════════
        composable(Screen.SmsPermission) {
            Box(modifier = Modifier.padding(contentPadding)) {
                val splashViewModel: SplashViewModel = hiltViewModel()

                SmsPermission(
                    onAllow = {
                        splashViewModel.saveOnboardingStep("loading")
                        navController.safeNavigate(Screen.Loading)
                    },
                    onSkip = {
                        splashViewModel.saveOnboardingStep("loading")
                        navController.safeNavigate(Screen.Loading)
                    },
                )
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // ONBOARDING — LOADING
        // ══════════════════════════════════════════════════════════════════════
        composable(Screen.Loading) {
            Box(modifier = Modifier.padding(contentPadding)) {
                val splashViewModel: SplashViewModel = hiltViewModel()

                LoadingScreen(
                    onFinished = {
                        splashViewModel.markOnboardingComplete()
                        navController.safeNavigate(Screen.Dashboard.Root) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                )
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // DASHBOARD
        //
        // One composable hosts the entire bottom-nav shell, its own NavController,
        // and all tab sub-graphs (Home, Transactions, Budget, Analytics, Settings).
        // Nothing inside the dashboard bleeds back into this outer NavHost.
        // ══════════════════════════════════════════════════════════════════════
        composable(
            route = Screen.Dashboard.Root,
            enterTransition    = { EnterTransition.None },
            exitTransition     = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition  = { ExitTransition.None },
        ) {
            DashboardScreen(
                onLogout = {
                    navController.safeNavigate(Screen.Intro) {
                        popUpTo(Screen.Dashboard.Root) { inclusive = true }
                    }
                },
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Shared helpers (unchanged from original)
// ══════════════════════════════════════════════════════════════════════════════

enum class AppStage { AUTH, ONBOARDING, HOME }

fun determineAppStage(
    onboardingComplete: Boolean,
    signupStarted: Boolean,
    accessToken: String?,
    username: String?,
    onboardingStep: String?,
): AppStage = when {
    onboardingComplete -> AppStage.HOME
    !onboardingStep.isNullOrBlank() && onboardingStep != "completed" -> AppStage.ONBOARDING
    signupStarted || (!accessToken.isNullOrBlank() && username.isNullOrBlank()) -> AppStage.ONBOARDING
    else -> AppStage.AUTH
}

fun determineOnboardingScreen(onboardingStep: String?, username: String?): String =
    when (onboardingStep) {
        "username" -> Screen.Username
        "currency" -> Screen.Currency
        "sms_permission", "loading" -> Screen.Dashboard.Root
        else -> if (username.isNullOrBlank()) Screen.Username else Screen.Currency
    }

private data class AuthContext(val email: String?, val flow: AuthFlowType?)

private fun retrieveAuthContext(navController: NavHostController): AuthContext {
    val prev = navController.previousBackStackEntry
    val current = navController.currentBackStackEntry

    val email = prev?.savedStateHandle?.get<String>(NavKeys.AUTH_EMAIL)
        ?: current?.arguments?.getString(NavKeys.AUTH_EMAIL)
    val flowName = prev?.savedStateHandle?.get<String>(NavKeys.AUTH_FLOW)
        ?: current?.arguments?.getString(NavKeys.AUTH_FLOW)

    val flow = when (flowName) {
        AuthFlowType.SIGNUP.name -> AuthFlowType.SIGNUP
        AuthFlowType.LOGIN.name -> AuthFlowType.LOGIN
        else -> null
    }
    return AuthContext(email, flow)
}

private suspend fun determinePostOtpDestination(
    splashViewModel: SplashViewModel,
    authFlow: AuthFlowType?,
): String {
    if (authFlow == AuthFlowType.SIGNUP) return Screen.Username
    val onboardingComplete = runCatching { splashViewModel.onboardingComplete.first() }.getOrDefault(false)
    return if (onboardingComplete) Screen.Dashboard.Root else Screen.Username
}

private object NavKeys {
    const val AUTH_EMAIL = "auth_email"
    const val AUTH_FLOW = "auth_flow"
}