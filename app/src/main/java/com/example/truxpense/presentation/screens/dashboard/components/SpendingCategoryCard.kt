package com.example.truxpense.presentation.screens.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.truxpense.util.progressColor
import com.example.truxpense.presentation.screens.dashboard.home.HomeSpendingCategory
import com.example.truxpense.util.toCurrency
import java.text.NumberFormat


@Composable
fun BudgetProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    errorColor: Color = MaterialTheme.colorScheme.error,
    label: String = "used",
    // Driven by the screen-level animateFloatAsState trigger (0f→1f once on navigation).
    // Multiply the real progress by this so the bar fills in on entry.
    progressMultiplier: Float = 1f,
) {
    val progressForBar = (progress.coerceIn(0f, 1f) * progressMultiplier)
    val renderColor = progressColor(progressForBar, errorColor)
    val trackColor = Color(0xFFD9DEE3)
    val barHeight = 6.dp

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(barHeight)) {
            val w = size.width
            val h = size.height
            val corner = h / 2f

            // Draw track (full width)
            drawRoundRect(color = trackColor, size = Size(w, h), cornerRadius = CornerRadius(corner, corner))

            // Draw progress overlay: special-case very small widths to render as a circle (perfect round end)
            val progressWidth = (w * progressForBar)
            if (progressWidth <= 0f) {
                // nothing
            } else if (progressWidth < h) {
                // very small progress — draw a circle with radius = h/2 centered at progressWidth/2
                drawCircle(
                    color = renderColor,
                    radius = h / 2f,
                    center = Offset(x = progressWidth / 2f, y = h / 2f),
                )
            } else {
                // normal progress — draw rounded rect matching the track corner radius
                // Slightly expand width by 0.5px to avoid a hairline seam on some devices/antialiasing
                val adj = 0.5f
                val pw = (progressWidth + adj).coerceAtMost(w)
                drawRoundRect(color = renderColor, size = Size(pw, h), cornerRadius = CornerRadius(corner, corner))
            }
        }

        Spacer(Modifier.height(4.dp))
        val displayPercent = (progressForBar * 100).toInt()
        Text(
            // label is customizable (default "used"). For spending cards we pass "spent".
            text = "${displayPercent}% ${label}",
            style = MaterialTheme.typography.bodySmall,
            color = renderColor
        )
    }
}

@Composable
fun SpendingCategoryCard(
    name: String,
    amountText: String,
    progress: Float,
    modifier: Modifier = Modifier,
    errorColor: Color = MaterialTheme.colorScheme.error,
    titleColor: Color? = null,
    progressMultiplier: Float = 1f,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: name
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = titleColor ?: MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right: amount
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            // For spending category card we show percent as "X% spent"
            BudgetProgressBar(progress = progress, errorColor = errorColor, label = "spent", progressMultiplier = progressMultiplier)
        }
    }
}

// Overload used by HomeTabScreen: accept a HomeSpendingCategory and a NumberFormat (fmt)
@Composable
fun SpendingCategoryCard(
    category: HomeSpendingCategory,
    fmt: NumberFormat,
    modifier: Modifier = Modifier,
    errorColor: Color = MaterialTheme.colorScheme.error,
    titleColor: Color? = null,
    progressMultiplier: Float = 1f,
) {
    val raw = category.amount.toCurrency(fmt)
    // Trim trailing decimals when not abbreviated (e.g., remove ".00"), but keep abbreviated forms like "1.2K"
    fun trimmedAmount(s: String): String {
        // if contains alphabetic abbreviation (K/M etc), return as-is
        if (s.contains(Regex("[A-Za-z]"))) return s
        val dot = s.lastIndexOf('.')
        if (dot == -1) return s
        val frac = s.substring(dot + 1).replace(Regex("\\D"), "")
        return if (frac.all { it == '0' }) s.substring(0, dot) else s
    }

    val amountText = trimmedAmount(raw)
    SpendingCategoryCard(
        name = category.name,
        amountText = amountText,
        progress = category.progress,
        modifier = modifier,
        errorColor = errorColor,
        titleColor = titleColor,
        progressMultiplier = progressMultiplier,
    )
}