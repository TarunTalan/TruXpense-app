package com.example.truxpense.presentation.screens.premium

import android.graphics.Color.rgb
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truxpense.R
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.theme.TruXpenseTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Display model (UI-only) ──────────────────────────────────────────────────

private data class UnlockedFeature(
    @DrawableRes val iconRes: Int,
    val title: String,
    val description: String,
)

private val unlockedFeatures = listOf(
    UnlockedFeature(R.drawable.sms, "SMS Auto-parsing", "Transactions detected automatically"),
    UnlockedFeature(R.drawable.analytics, "Advanced Analytics", "Custom ranges & year-over-year"),
    UnlockedFeature(R.drawable.report, "Export Reports", "PDF, CSV & Excel ready to share"),
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun PaymentSuccessScreen(onDone: () -> Unit = {}) {
    val primary = if (isSystemInDarkTheme())Color(rgb(203, 233, 228)) else MaterialTheme.colorScheme.primary

    // ── Per-section animatable alpha + translateY ──────────────────────────────
    // 0 = icon, 1 = title, 2 = features, 3 = button
    val alphas = remember { List(4) { Animatable(0f) } }
    val offsets = remember { List(4) { Animatable(40f) } }   // start 40dp below

    LaunchedEffect(Unit) {
        val delays = listOf(0L, 220L, 420L, 600L)
        alphas.zip(offsets).forEachIndexed { i, (alpha, offset) ->
            launch {
                delay(delays[i])
                launch { alpha.animateTo(1f, tween(450, easing = EaseOut)) }
                launch { offset.animateTo(0f, tween(450, easing = EaseOutCubic)) }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(color = primary),
    ) {
        // ── Decorative background circles ─────────────────────────────────────
        Box(
            modifier = Modifier.size(260.dp).align(Alignment.TopEnd)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.06f), CircleShape)
        )
        Box(
            modifier = Modifier.size(160.dp).align(Alignment.TopEnd)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.05f), CircleShape)
        )
        Box(
            modifier = Modifier.size(180.dp).align(Alignment.BottomStart)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.05f), CircleShape)
        )
        Box(
            modifier = Modifier.size(100.dp).align(Alignment.CenterStart)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.04f), CircleShape)
        )

        // ── Scrollable content ────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
                .navigationBarsPadding().padding(horizontal = DashboardDimens.screenPaddingH),
        ) {
            Spacer(modifier = Modifier.height(DashboardDimens.spaceXxxl))

            // ── Pulsing check icon ────────────────────────────────────────────
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = alphas[0].value
                    translationY = offsets[0].value.dp.toPx()
                },
            ) {
                PulsingSuccessIcon()
            }

            Spacer(modifier = Modifier.height(DashboardDimens.spaceXxl))

            // ── Headline + subtitle ───────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = alphas[1].value
                    translationY = offsets[1].value.dp.toPx()
                },
            ) {
                Text(
                    text = "You're all set!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp,
                )
                Spacer(modifier = Modifier.height(DashboardDimens.spaceMd))
                Text(
                    text = "Your 7-day free trial has started.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(DashboardDimens.spaceXs))
                Text(
                    text = "Unlimited access to every Pro feature.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(DashboardDimens.spaceXxl))

            // ── Unlocked features ─────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth().graphicsLayer {
                        alpha = alphas[2].value
                        translationY = offsets[2].value.dp.toPx()
                    },
                verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
            ) {
                unlockedFeatures.forEach { feature ->
                    FeatureUnlockedCard(feature = feature)
                }
            }

            Spacer(modifier = Modifier.height(DashboardDimens.spaceXxl))

            // ── CTA button ────────────────────────────────────────────────────
            Button(
                onClick = onDone,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().height(DashboardDimens.buttonHeight).graphicsLayer {
                        alpha = alphas[3].value
                        translationY = offsets[3].value.dp.toPx()
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = primary,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.diamond),
                    contentDescription = null,
                    modifier = Modifier.size(DashboardDimens.iconNav),
                )
                Spacer(modifier = Modifier.width(DashboardDimens.spaceMd))
                Text(
                    text = "Start Exploring",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Spacer(modifier = Modifier.height(DashboardDimens.spaceXxl))
        }
    }
}

// ─── Pulsing check icon ───────────────────────────────────────────────────────

@Composable
private fun PulsingSuccessIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse_scale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse_alpha",
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(96.dp).scale(pulseScale).clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = pulseAlpha), CircleShape)
        )
        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f), CircleShape)
        )
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f), CircleShape)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(64.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.95f), CircleShape),
        ) {
            Icon(
                painter = painterResource(R.drawable.tick),
                contentDescription = "Success",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

// ─── Feature unlocked card ────────────────────────────────────────────────────

@Composable
private fun FeatureUnlockedCard(feature: UnlockedFeature) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f), MaterialTheme.shapes.medium).padding(
                horizontal = DashboardDimens.cardPaddingComp,
                vertical = DashboardDimens.spaceMdL,
            ),
    ) {
        // Icon circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f), CircleShape),
        ) {
            Icon(
                painter = painterResource(id = feature.iconRes),
                contentDescription = feature.title,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(DashboardDimens.iconMd),
            )
        }

        Spacer(modifier = Modifier.width(DashboardDimens.spaceMdL))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = feature.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            )
        }

        // Unlocked checkmark
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(DashboardDimens.iconLg)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f), CircleShape),
        ) {
            Icon(
                painter = painterResource(R.drawable.tick),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(DashboardDimens.iconXs),
            )
        }
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PaymentSuccessScreenPreview() {
    TruXpenseTheme {
        PaymentSuccessScreen()
    }
}