package com.example.truxpense.presentation.screens.onboarding.loading

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.truxpense.R
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(onFinished: () -> Unit, delayMs: Long = 1200) {
    // Simple loading icon center screen
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(painter = painterResource(id = R.drawable.loading_icon), contentDescription = "Loading", modifier = Modifier.size(120.dp))
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
