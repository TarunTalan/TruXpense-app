package com.example.truxpense.presentation.theme

import android.graphics.Color.rgb
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(rgb(159, 214, 207)),
    secondary = Color(rgb(193, 199, 205)),
    tertiary = Color(rgb(247, 249, 250)),
    background = Color(rgb(18, 20, 23)),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color(rgb(58, 63, 69)),
    onTertiary = Color(rgb(58, 63, 69)),
    onBackground = Color(rgb(247, 249, 250)),
    onSurface = Color(rgb(154, 163, 171)),
    error = Color(rgb(224, 122, 122)),
    outline = Color(rgb(110, 119, 129,)),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(rgb(47, 164, 169)),
    secondary = Color(rgb(110, 119, 129)),
    tertiary = Color(rgb(58, 63, 69)),
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color(rgb(193, 199, 205)),
    onTertiary = Color(rgb(247, 249, 250)),
    onBackground = Color(rgb(58, 63, 69)),
    onSurface = Color(rgb(154, 163, 171)),
    error = Color(rgb(214, 69, 69)),
    outline = Color(rgb(154, 163, 171)),
)

@Composable
fun TruXpenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography(),
        content = content,
        shapes = Shapes,
    )
}