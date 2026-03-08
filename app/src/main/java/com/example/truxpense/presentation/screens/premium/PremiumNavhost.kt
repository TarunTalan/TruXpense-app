package com.example.truxpense.presentation.screens.premium

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.example.truxpense.presentation.screens.premium.model.PlanType
import com.example.truxpense.presentation.theme.TruXpenseTheme

// ...existing code...

@Composable
fun PremiumNavHost(onExitPremiumFlow: () -> Unit = {}) {
    // ...existing code...
    var destination: PremiumDestination by rememberSaveable(stateSaver = premiumDestinationSaver()) {
        mutableStateOf(PremiumDestination.Paywall)
    }

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            val goingForward = targetState !is PremiumDestination.Paywall
            val spec = tween<IntOffset>(320)
            val fadeSpec = tween<Float>(320)
            if (goingForward) {
                // Push: new screen slides in from right, current slides out to left (parallax)
                (slideInHorizontally(animationSpec = spec) { it } + fadeIn(fadeSpec)) togetherWith
                        (slideOutHorizontally(animationSpec = tween(320)) { -it / 3 } + fadeOut(fadeSpec))
            } else {
                // Pop: previous screen slides in from left (parallax), current slides out to right
                (slideInHorizontally(animationSpec = spec) { -it / 3 } + fadeIn(fadeSpec)) togetherWith
                        (slideOutHorizontally(animationSpec = tween(320)) { it } + fadeOut(fadeSpec))
            }
        },
        label = "premium_nav",
    ) { dest ->
        when (dest) {
            is PremiumDestination.Paywall ->
                PaywallScreen(
                    onNavigateBack = onExitPremiumFlow,
                    onStartTrial = { plan -> destination = PremiumDestination.Payment(plan) },
                )
            is PremiumDestination.Payment ->
                PaymentGatewayScreen(
                    selectedPlan = dest.plan,
                    onNavigateBack = { destination = PremiumDestination.Paywall },
                    onPaymentSuccess = { destination = PremiumDestination.Success },
                )
            is PremiumDestination.Success ->
                PaymentSuccessScreen(onDone = onExitPremiumFlow)
        }
    }
}

// ...existing code...


// ─── Saveable helper ──────────────────────────────────────────────────────────

/**
 * Simple saver: encodes destination as an Int + optional plan ordinal.
 * 0 = Paywall, 1 = Payment(plan ordinal), 2 = Success.
 */
private fun premiumDestinationSaver() = androidx.compose.runtime.saveable.Saver<PremiumDestination, List<Int>>(
    save = { dest ->
        when (dest) {
            is PremiumDestination.Paywall -> listOf(0, 0)
            is PremiumDestination.Payment -> listOf(1, dest.plan.ordinal)
            is PremiumDestination.Success -> listOf(2, 0)
        }
    },
    restore = { list ->
        when (list[0]) {
            1 -> PremiumDestination.Payment(PlanType.entries[list[1]])
            2 -> PremiumDestination.Success
            else -> PremiumDestination.Paywall
        }
    },
)

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PremiumNavHostPreview() {
    TruXpenseTheme { PremiumNavHost() }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PaymentSuccessPreview() {
    TruXpenseTheme { PaymentSuccessScreen() }
}