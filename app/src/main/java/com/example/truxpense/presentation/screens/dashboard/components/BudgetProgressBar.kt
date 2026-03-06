package com.example.truxpense.presentation.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truxpense.presentation.utils.progressColor

/**
 * Reusable interval-themed progress bar.
 *
 * Bar color (and optional label color) follows the 3-interval budget theme:
 *   progress >= 0.80 → danger  red    #D64545
 *   progress >= 0.45 → warning amber  #F2C06A
 *   progress <  0.45 → safe    teal   #1B6F73
 *
 * @param progress        0f..1f — actual completion fraction
 * @param progressMultiplier  animated fill multiplier (0f→1f on entry); default 1f
 * @param showLabel       whether to show the "X% <label>" text below the bar
 * @param label           the word appended after the percentage (e.g. "used", "spent")
 * @param height          bar height
 * @param trackColor      background track colour
 */
@Composable
fun BudgetProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    progressMultiplier: Float = 1f,
    showLabel: Boolean = true,
    label: String = "used",
    height: Dp = 6.dp,
    trackColor: Color = Color.Black.copy(alpha = 0.08f),
    // kept for API compatibility – ignored, color derives from interval
    @Suppress("UNUSED_PARAMETER") errorColor: Color = MaterialTheme.colorScheme.error,
) {
    val filled = (progress.coerceIn(0f, 1f) * progressMultiplier)
    val barColor = progressColor(progress)   // colour driven by *real* progress, not animated fill

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(999.dp))
                .background(trackColor),
        ) {
            if (filled > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(filled)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(barColor),
                )
            }
        }

        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}% $label",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = barColor,
            )
        }
    }
}