package com.example.truxpense.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry

// ── Duration ──────────────────────────────────────────────────────────────────
// 300 ms matches PremiumNavHost exactly. Compose's default FastOutSlowIn easing
// is used (no custom easing) — the same spec that makes premium screens feel
// snappy. Using longer durations or EaseOutCubic here desynchronised the feel.
private const val DURATION = 300

// ── Forward push ──────────────────────────────────────────────────────────────

/** New screen slides in from the right. */
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromRight(): EnterTransition =
    slideInHorizontally(tween(DURATION)) { it } + fadeIn(tween(DURATION))

/**
 * Current screen slides out fully to the left with a fade.
 *
 * Full-width exit (-it) + fadeOut matches PremiumNavHost's ContentTransform
 * exactly. The earlier parallax (-it/4, no fade) felt slower because the old
 * screen stayed visible for the entire duration, desynchronising the two layers.
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToLeft(): ExitTransition =
    slideOutHorizontally(tween(DURATION)) { -it } + fadeOut(tween(DURATION))

// ── Back pop ──────────────────────────────────────────────────────────────────

/** Previous screen slides back in from the left. */
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromLeft(): EnterTransition =
    slideInHorizontally(tween(DURATION)) { -it } + fadeIn(tween(DURATION))

/** Current screen slides out to the right. */
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToRight(): ExitTransition =
    slideOutHorizontally(tween(DURATION)) { it } + fadeOut(tween(DURATION))