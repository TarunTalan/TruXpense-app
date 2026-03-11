package com.example.truxpense.presentation.screens.premium

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.example.truxpense.presentation.screens.premium.model.PlanType
import com.example.truxpense.presentation.theme.TruXpenseTheme

// ─── Destination ──────────────────────────────────────────────────────────────

private sealed interface PremiumDestination {
    data object Paywall : PremiumDestination
    data class Payment(val plan: PlanType) : PremiumDestination
    data object Success : PremiumDestination
}

// ─── Nav Host ─────────────────────────────────────────────────────────────────

/**
 * Self-contained nav host for the full Premium upgrade flow:
 *   Paywall → Payment → Success
 */
@Composable
fun PremiumNavHost(onExitPremiumFlow: () -> Unit = {}) {
    var destination: PremiumDestination by rememberSaveable(stateSaver = premiumDestinationSaver()) {
        mutableStateOf(PremiumDestination.Paywall)
    }

    // Ensure system/back gesture navigates within the flow (Payment -> Paywall)
    BackHandler(enabled = destination is PremiumDestination.Payment) {
        destination = PremiumDestination.Paywall
    }

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            val goingForward = targetState !is PremiumDestination.Paywall
            val fadeSpec = tween<Float>(320)
            if (goingForward) {
                (slideInHorizontally(tween(320)) { it }        + fadeIn(fadeSpec)) togetherWith
                        (slideOutHorizontally(tween(320)) { -it / 3 } + fadeOut(fadeSpec))
            } else {
                (slideInHorizontally(tween(320)) { -it / 3 }  + fadeIn(fadeSpec)) togetherWith
                        (slideOutHorizontally(tween(320)) { it }      + fadeOut(fadeSpec))
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

// ─── Saveable helper ──────────────────────────────────────────────────────────

private fun premiumDestinationSaver() = androidx.compose.runtime.saveable.Saver<PremiumDestination, List<Int>>(
    save = { dest ->
        when (dest) {
            is PremiumDestination.Paywall  -> listOf(0, 0)
            is PremiumDestination.Payment  -> listOf(1, dest.plan.ordinal)
            is PremiumDestination.Success  -> listOf(2, 0)
        }
    },
    restore = { list ->
        when (list[0]) {
            1    -> PremiumDestination.Payment(PlanType.entries[list[1]])
            2    -> PremiumDestination.Success
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