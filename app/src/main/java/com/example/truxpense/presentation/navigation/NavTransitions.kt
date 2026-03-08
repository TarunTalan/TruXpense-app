package com.example.truxpense.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

private const val SLIDE_DURATION = 320

/**
 * Forward push — new screen slides in from the right.
 * Call inside a NavHost enterTransition / exitTransition lambda.
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromRight(): EnterTransition =
    slideInHorizontally(tween(SLIDE_DURATION)) { it } +
            fadeIn(tween(SLIDE_DURATION))

/**
 * Forward push — current screen slides out to the left (parallax).
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToLeft(): ExitTransition =
    slideOutHorizontally(tween(SLIDE_DURATION)) { -it / 3 } +
            fadeOut(tween(SLIDE_DURATION))

/**
 * Back pop — previous screen slides back in from the left (parallax).
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromLeft(): EnterTransition =
    slideInHorizontally(tween(SLIDE_DURATION)) { -it / 3 } +
            fadeIn(tween(SLIDE_DURATION))

/**
 * Back pop — current screen slides out to the right.
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToRight(): ExitTransition =
    slideOutHorizontally(tween(SLIDE_DURATION)) { it } +
            fadeOut(tween(SLIDE_DURATION))
