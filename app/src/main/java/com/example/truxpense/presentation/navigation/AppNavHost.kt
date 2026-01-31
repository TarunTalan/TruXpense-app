package com.example.truxpense.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.truxpense.presentation.screens.auth.intro.IntroScreen
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

// App lifecycle stages used by splash routing
private enum class Stage { AUTH, ONBOARDING, HOME }

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
                        // Determine app stage and route accordingly.
                        // Stages (priority order):
                        // 1) HOME: onboardingComplete == true -> Home
                        // 2) ONBOARDING: signupStarted == true OR (accessToken exists but username missing) -> Username
                        // 3) AUTH: default -> Intro

                        val stage = when {
                            onboardingComplete -> Stage.HOME
                            signupStarted -> Stage.ONBOARDING
                            !accessToken.isNullOrBlank() && username.isNullOrBlank() -> Stage.ONBOARDING
                            else -> Stage.AUTH
                        }

                        val destination = when (stage) {
                            Stage.HOME -> Screen.Home
                            Stage.ONBOARDING -> Screen.Username
                            Stage.AUTH -> Screen.Intro
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
                val introViewModel: com.example.truxpense.presentation.screens.auth.intro.IntroViewModel = hiltViewModel()
                val introState by introViewModel.state.collectAsState()

                // Observe viewmodel navigation signals and navigate from the NavHost (main thread)
                LaunchedEffect(introState.navigateToHome, introState.navigateToUsername) {
                    if (introState.navigateToHome) {
                        navController.navigate(Screen.Home) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    } else if (introState.navigateToUsername) {
                        navController.navigate(Screen.Username) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    }
                }

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
                    onNavigateToUsername = { _ ->
                        // route newly-signed-up OAuth users to username onboarding
                        navController.navigate(Screen.Username) {
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
                // Read auth_flow/auth_email from either the previous backstack's SavedStateHandle (normal in-app navigation)
                // or from the current NavBackStackEntry arguments (supports deep-links / cold starts that provide params).
                val prevEntry = navController.previousBackStackEntry
                val savedFlowNameFromSavedState: String? = prevEntry?.savedStateHandle?.get<String>("auth_flow")
                val savedEmailFromSavedState: String? = prevEntry?.savedStateHandle?.get<String>("auth_email")

                val currentArgs = navController.currentBackStackEntry?.arguments
                val savedFlowNameFromArgs: String? = currentArgs?.getString("auth_flow")
                val savedEmailFromArgs: String? = currentArgs?.getString("auth_email")

                // Prefer savedStateHandle (in-app navigation), fallback to nav args (deep links / cold start)
                val savedFlowName: String? = savedFlowNameFromSavedState ?: savedFlowNameFromArgs
                val savedEmail: String? = savedEmailFromSavedState ?: savedEmailFromArgs

                val savedFlow: AuthFlowType? = when (savedFlowName) {
                    AuthFlowType.SIGNUP.name -> AuthFlowType.SIGNUP
                    AuthFlowType.LOGIN.name -> AuthFlowType.LOGIN
                    else -> null
                }

                // We will trigger navigation after OTP verification by checking persisted username
                val splashViewModel: SplashViewModel = hiltViewModel()
                var otpVerifiedTrigger by remember { mutableStateOf(false) }

                // When otpVerifiedTrigger toggles, read persisted username and route accordingly
                LaunchedEffect(otpVerifiedTrigger) {
                    if (!otpVerifiedTrigger) return@LaunchedEffect

                    // Wait for persisted username (or empty) from DataStore
                    val persistedUsername = try {
                        splashViewModel.username.first()
                    } catch (_: Exception) { null }

                    if (!persistedUsername.isNullOrBlank()) {
                        // existing user -> Home
                        navController.safeNavigate(Screen.Home) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    } else {
                        // new user -> Username onboarding
                        navController.safeNavigate(Screen.Username) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    }

                    // reset trigger
                    otpVerifiedTrigger = false
                }

                OtpScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onVerified = {
                        // Set trigger which will cause the LaunchedEffect above to check persisted username
                        otpVerifiedTrigger = true
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
                        navController.safeNavigate(Screen.Home) {
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
