package com.example.truxpense.presentation.screens.onboarding.complete

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
fun CompleteSetupScreen(onFinished: () -> Unit, delayMs: Long = 800) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(painter = painterResource(id = R.drawable.setup_complete_icon), contentDescription = "Complete", modifier = Modifier.size(120.dp))
    }

    LaunchedEffect(Unit) {
        delay(delayMs)
        onFinished()
    }
}

@Preview
@Composable
fun CompletePreview() {
    CompleteSetupScreen(onFinished = {})
}
