package com.example.truxpense.presentation.screens.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.utils.clearFocusOnTap
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.tooling.preview.Preview
import android.util.Log
import kotlin.math.roundToInt

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onEnter: (() -> Unit)? = null,
    onFinished: (() -> Unit)? = null,
    viewModel: SplashViewModel = hiltViewModel()
) {
    // animation controllers
    val contentAlpha = remember { Animatable(0f) }

    // X (center large) controls
    val xAlpha = remember { Animatable(0f) }
    val xScale = remember { Animatable(0.8f) }

    // Tru controls (left text)
    val truAlpha = remember { Animatable(0f) }
    val truOffset = remember { Animatable(0f) }

    // Pense controls (right text)
    val penseAlpha = remember { Animatable(0f) }
    val penseOffset = remember { Animatable(0f) }

    // Icon controls
    val iconAlpha = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0.8f) }

    val density = LocalDensity.current


    var enteredOnEnter by remember { mutableStateOf(false) }
    val localView = LocalView.current
    Log.d("SplashScreen", "DisposableEffect registering pre-draw listener; enteredOnEnter=$enteredOnEnter")
    DisposableEffect(localView) {
        if (!enteredOnEnter) {
            Log.d("SplashScreen", "markReady() called on ViewModel")
            viewModel.markReady()
            val listener = object : android.view.ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    // Remove listener and notify host once
                    if (!enteredOnEnter) {
                        enteredOnEnter = true
                        Log.d("SplashScreen", "onPreDraw triggered - calling onEnter")
                        localView.viewTreeObserver.removeOnPreDrawListener(this)
                        onEnter?.invoke()
                    }
                    return true
                }
            }
            localView.viewTreeObserver.addOnPreDrawListener(listener)
            onDispose {
                Log.d("SplashScreen", "DisposableEffect onDispose")
                localView.viewTreeObserver.removeOnPreDrawListener(listener)
            }
        } else {
            onDispose { }
        }
    }

    LaunchedEffect(Unit) {
        // Overall fade in
        contentAlpha.animateTo(1f, animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))

        // X appears and scales (center focus)
        coroutineScope {
            launch { xAlpha.animateTo(1f, animationSpec = tween(220, easing = FastOutSlowInEasing)) }
            launch {
                xScale.snapTo(0.85f)
                xScale.animateTo(1.18f, animationSpec = tween(300, easing = FastOutSlowInEasing))
                xScale.animateTo(1f, animationSpec = tween(180, easing = FastOutSlowInEasing))
            }
        }

        // slight pause so X is perceived
        delay(180)

        // Prepare slide distances
        val slideDp = 12.dp // bigger slide for a smooth motion
        val slidePx = with(density) { slideDp.toPx() }

        // set starting offsets off-center
        truOffset.snapTo(slidePx)
        penseOffset.snapTo(-slidePx)

        // animate Tru and Pense into place concurrently with alpha
        coroutineScope {
            launch {
                // Tru: from right-to-left to final position
                truAlpha.animateTo(1f, animationSpec = tween(300, easing = FastOutSlowInEasing))
            }
            launch {
                truOffset.animateTo(0f, animationSpec = tween(420, easing = FastOutSlowInEasing))
            }
            // small stagger for Pense
            launch {
                delay(100)
                penseAlpha.animateTo(1f, animationSpec = tween(300, easing = FastOutSlowInEasing))
            }
            launch {
                delay(100)
                penseOffset.animateTo(0f, animationSpec = tween(420, easing = FastOutSlowInEasing))
            }
        }

        // show icon with scale
        delay(150)
        coroutineScope {
            launch { iconAlpha.animateTo(1f, animationSpec = tween(240, easing = FastOutSlowInEasing)) }
            launch {
                iconScale.snapTo(0.85f)
                iconScale.animateTo(1f, animationSpec = tween(300, easing = FastOutSlowInEasing))
            }
        }

        // keep visible a bit
        delay(700)
        Log.d("SplashScreen", "animation finished - calling onFinished")
        onFinished?.invoke()
    }

    Box(
        modifier = modifier.fillMaxSize().clearFocusOnTap(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer { alpha = contentAlpha.value }
        ) {
            // 'Tru' - slides from right to left
            Box(modifier = Modifier.offset { IntOffset(truOffset.value.roundToInt(), 0) }) {
                Text(
                    text = "Tru",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.graphicsLayer { alpha = truAlpha.value }
                )
            }

            // Large 'X' - center
            Box(modifier = Modifier
                .padding(horizontal = 6.dp)
                .scale(xScale.value)
                .graphicsLayer { alpha = xAlpha.value }) {
                Text(
                    text = "X",
                    fontSize = 80.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 'pense' - slides from left to right
            Box(modifier = Modifier.offset { IntOffset(penseOffset.value.roundToInt(), 0) }) {
                Text(
                    text = "pense",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.graphicsLayer { alpha = penseAlpha.value }
                )
            }

            // icon at the end
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier
                .graphicsLayer { alpha = iconAlpha.value }
                .scale(iconScale.value)) {
                Image(
                    painter = painterResource(id = R.drawable.splash_screen_icon),
                    contentDescription = "App icon",
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen()
}