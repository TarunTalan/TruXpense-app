package com.example.truxpense.presentation.screens.dashboard.savings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.theme.TruXpenseTheme

// ══════════════════════════════════════════════════════════════════════════════
// No ViewModel needed — data arrives as nav args / saved state from parent.
// Thin wrapper kept for symmetry; could be called directly from NavHost.
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun GoalCompletedScreen(
    goalName: String,
    savedAmount: Double,
    onSetNewGoal: () -> Unit,
    onGoHome: () -> Unit,
) {
    GoalCompletedScreenContent(
        goalName = goalName,
        savedAmount = savedAmount,
        onSetNewGoal = onSetNewGoal,
        onGoHome = onGoHome,
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// Stateless content
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun GoalCompletedScreenContent(
    goalName: String,
    savedAmount: Double,
    onSetNewGoal: () -> Unit,
    onGoHome: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary

    // Pulsing trophy
    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infinite.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    // Staggered entrance
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()
            .navigationBarsPadding().padding(horizontal = DashboardDimens.screenPaddingH),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Trophy with glow rings
        AnimatedVisibility(
            visible = visible, enter = scaleIn(tween(400, easing = FastOutSlowInEasing)) + fadeIn(tween(400))
        ) {
            Box(contentAlignment = Alignment.Center) {
                listOf(96.dp to 0.08f, 80.dp to 0.12f, 64.dp to 0.18f).forEach { (size, alpha) ->
                    Box(Modifier.size(size).background(primary.copy(alpha = alpha), CircleShape))
                }
                Text("🏆", fontSize = 56.sp, modifier = Modifier.scale(pulseScale))
            }
        }

        Spacer(Modifier.height(28.dp))

        // Headline
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(400, delayMillis = 120)) { it / 2 } + fadeIn(tween(400, 120))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Goal Achieved! 🎉", style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp
                    ), color = primary, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "You saved up for \"$goalName\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Stats card
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(400, delayMillis = 240)) { it / 2 } + fadeIn(tween(400, 240))) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DashboardDimens.cornerCard),
                colors = CardDefaults.cardColors(containerColor = primary.copy(alpha = 0.08f)),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "Total saved",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "₹${"%,.0f".format(savedAmount)}",
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = primary
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        // Buttons
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(400, delayMillis = 360)) { it / 2 } + fadeIn(tween(400, 360))) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onSetNewGoal,
                    modifier = Modifier.fillMaxWidth().height(DashboardDimens.buttonHeight),
                    shape = RoundedCornerShape(DashboardDimens.cornerCard),
                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                ) {
                    Text(
                        "Set a new goal", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall
                    )
                }
                OutlinedButton(
                    onClick = onGoHome,
                    modifier = Modifier.fillMaxWidth().height(DashboardDimens.buttonHeight),
                    shape = RoundedCornerShape(DashboardDimens.cornerCard),
                ) {
                    Text(
                        "Go to Savings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Previews
// ══════════════════════════════════════════════════════════════════════════════
@Preview(
    name = "Goal Completed", showBackground = true, device = "spec:width=390dp,height=844dp,dpi=430",uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewGoalCompleted() {
    TruXpenseTheme {
        GoalCompletedScreenContent(
            goalName = "iPhone 15",
            savedAmount = 80000.0,
            onSetNewGoal = {},
            onGoHome = {},
        )
    }
}

@Preview(
    name = "Goal Completed – long name", showBackground = true, device = "spec:width=390dp,height=844dp,dpi=430",uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewGoalCompletedLongName() {
    TruXpenseTheme {
        GoalCompletedScreenContent(
            goalName = "Sony A7 IV Full-Frame Camera",
            savedAmount = 254999.0,
            onSetNewGoal = {},
            onGoHome = {},
        )
    }
}