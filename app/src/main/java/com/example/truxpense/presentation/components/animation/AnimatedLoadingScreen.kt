package com.example.truxpense.presentation.components.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedLoadingScreen(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    rotationDurationMs: Int = 1400,
    barsCount: Int = 18
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading-rotation")

    // Continuous rotation 0..360 then derive current bar index
    val rotationDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationDurationMs, easing = LinearEasing)
        ),
        label = "bars-rotation"
    )
    val anglePerBar = 360f / barsCount
    val stepIndex = ((rotationDeg / anglePerBar).toInt()) % barsCount

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            RotatingRadialBarsOverlay(
                sizeDp = 120,
                barsCount = barsCount,
                currentIndex = stepIndex,
                baseColor = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun RotatingRadialBarsOverlay(
    sizeDp: Int,
    barsCount: Int,
    currentIndex: Int,
    baseColor: Color
) {
    Canvas(
        modifier = Modifier.size(sizeDp.dp)
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Define inner and outer radii for bar length
        val innerRadius = radius * 0.45f
        val outerRadius = radius * 0.90f

        // Stroke width relative to size (slightly thinner when many bars)
        val densityFactor = (12f / barsCount.coerceAtLeast(12))
        val strokeWidthPx = size.minDimension * 0.085f * densityFactor

        // Angle per bar
        val anglePerBar = 360f / barsCount

        // Build increasing opacity ramp from ~0.0 to 1.0 across all bars
        val opacities = FloatArray(barsCount) { idx ->
            if (barsCount <= 1) 1f else idx.toFloat() / (barsCount - 1).toFloat()
        }

        // Draw bars at each angle; apply rotated opacity ramp by currentIndex
        for (i in 0 until barsCount) {
            val rampIndex = (i - currentIndex + barsCount) % barsCount
            val alpha = opacities[rampIndex]
            val color = baseColor.copy(alpha = alpha)
            val angle = i * anglePerBar

            // Compute direction vector for this angle
            val rad = Math.toRadians(angle.toDouble())
            val dx = cos(rad).toFloat()
            val dy = sin(rad).toFloat()

            val start = Offset(
                center.x + dx * innerRadius,
                center.y + dy * innerRadius
            )
            val end = Offset(
                center.x + dx * outerRadius,
                center.y + dy * outerRadius
            )

            drawLine(
                color = color,
                start = start,
                end = end,
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AnimatedLoadingScreenPreview() {
    AnimatedLoadingScreen()
}
