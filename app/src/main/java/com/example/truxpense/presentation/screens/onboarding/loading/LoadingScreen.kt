package com.example.truxpense.presentation.screens.onboarding.loading

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.truxpense.presentation.components.animation.AnimatedLoadingScreen
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(onFinished: () -> Unit, delayMs: Long = 1200) {
    // Animated loading screen centered
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedLoadingScreen()
    }

    val context = LocalContext.current
    val activity = context as? android.app.Activity
    // When user presses system back on this screen, exit the app
    BackHandler(enabled = true) {
        activity?.finishAffinity()
    }

    LaunchedEffect(Unit) {
        delay(delayMs)
        onFinished()
    }
}

@Preview
@Composable
fun LoadingPreview() {
    LoadingScreen(onFinished = {})
}
