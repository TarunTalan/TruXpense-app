package com.example.truxpense.presentation.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.theme.TruXpenseTheme

val GradientCardShape = RoundedCornerShape(DashboardDimens.cornerCard)

/**
 * Reusable card with a clean diagonal gradient background (surfaceContainer → background).
 * Content is rendered on top of the gradient.
 *
 * Blur was removed — BlurEffect bleeds outside clip boundaries in Compose, causing
 * visible distortion around card edges.
 *
 * @param modifier    Applied to the outer container.
 * @param cardBrush   Optional custom brush; defaults to surfaceContainer→background gradient.
 * @param elevation   Shadow elevation. Defaults to 2.dp.
 * @param content     Content rendered on top of the gradient.
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    cardBrush: Brush? = null,
    elevation: Dp = 2.dp,
    // kept for binary compat — ignored now that blur is removed
    @Suppress("UNUSED_PARAMETER") blurRadius: Float = 0f,
    content: @Composable () -> Unit,
) {
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val background = MaterialTheme.colorScheme.background

    val brush = cardBrush ?: remember(background, surfaceContainer) {
        Brush.linearGradient(
            colors = listOf(surfaceContainer, background),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )
    }

    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = GradientCardShape, clip = false)
            .clip(GradientCardShape)
            .background(brush)           // gradient applied directly to the outer Box — no inner layer needed
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun GradientCardPreview() {
    TruXpenseTheme {
        GradientCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "GradientCard preview",
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
