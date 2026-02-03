package com.example.truxpense.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.truxpense.presentation.screens.onboarding.currency.CurrencyViewModel
import com.example.truxpense.presentation.screens.splash.SplashScreen
import com.example.truxpense.presentation.screens.splash.SplashViewModel

@Composable
fun SplashNavigator(
    navController: NavHostController,
    contentPadding: PaddingValues,
    onSplashEnter: () -> Unit = {}
) {
    val viewModel: SplashViewModel = hiltViewModel()
    // CurrencyViewModel is needed to ensure a default currency (INR) is set when auto-login happens
    val currencyViewModel: CurrencyViewModel = hiltViewModel()

    val onboardingComplete by viewModel.onboardingComplete.collectAsState(initial = false)
    val accessToken by viewModel.accessToken.collectAsState(initial = null)
    val username by viewModel.username.collectAsState(initial = null)
    val signupStarted by viewModel.signupStarted.collectAsState(initial = false)
    val onboardingStep by viewModel.onboardingStep.collectAsState(initial = null)

    Box(modifier = Modifier.padding(contentPadding)) {
        LaunchedEffect(Unit) { onSplashEnter() }

        SplashScreen(
            onFinished = {
                val stage = determineAppStage(
                    onboardingComplete = onboardingComplete,
                    signupStarted = signupStarted,
                    accessToken = accessToken,
                    username = username,
                    onboardingStep = onboardingStep
                )

                // Auto-complete onboarding if user was at SMS Permission or beyond
                if (stage == AppStage.ONBOARDING &&
                    (onboardingStep == "sms_permission" ||
                     onboardingStep == "loading" ||
                     onboardingStep == "complete_setup")) {
                    // Ensure default currency is set (INR) if nothing selected before marking onboarding complete
                    val current = currencyViewModel.selectedCurrency.value
                    if (current == null) {
                        val default = currencyViewModel.available.firstOrNull { it.code == "INR" } ?: currencyViewModel.available.firstOrNull()
                        if (default != null) currencyViewModel.selectCurrency(default)
                    }
                    viewModel.markOnboardingComplete()
                }

                val destination = when (stage) {
                    AppStage.HOME -> Screen.Home
                    AppStage.ONBOARDING -> determineOnboardingScreen(
                        onboardingStep = onboardingStep,
                        username = username
                    )
                    AppStage.AUTH -> Screen.Intro
                }

                navController.safeNavigate(destination) {
                    popUpTo(Screen.Splash) { inclusive = true }
                }
            }
        )
    }
}
