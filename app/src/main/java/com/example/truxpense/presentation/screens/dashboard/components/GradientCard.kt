package com.example.truxpense.presentation.screens.dashboard.components

import android.os.Build
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
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.theme.TruXpenseTheme

val GradientCardShape = RoundedCornerShape(DashboardDimens.cornerCard)

/**
 * Reusable card with a blurred diagonal gradient background (background → surfaceContainer).
 * Content is rendered sharp on top of the blurred background.
 *
 * Blur requires API 31+; on older devices the gradient renders without blur.
 *
 * @param modifier      Applied to the outer container.
 * @param cardBrush     Optional custom brush; defaults to background→surfaceContainer gradient.
 * @param elevation     Shadow elevation. Defaults to 2.dp.
 * @param blurRadius    Blur radius for the background layer (API 31+). Defaults to 40f.
 * @param content       Sharp content rendered on top.
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    cardBrush: Brush? = null,
    elevation: Dp = 2.dp,
    blurRadius: Float = 40f,
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
    ) {
        // Blurred gradient background layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(brush)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.graphicsLayer {
                            renderEffect = BlurEffect(
                                radiusX = blurRadius,
                                radiusY = blurRadius,
                                edgeTreatment = TileMode.Clamp,
                            )
                        }
                    } else Modifier
                )
        )
        // Sharp content layer
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
