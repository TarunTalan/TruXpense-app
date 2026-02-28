package com.example.truxpense.presentation.screens.dashboard.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.truxpense.R

@Composable
fun EmptyScreenContent(
    modifier: Modifier = Modifier,
    @DrawableRes illustrationRes: Int = R.drawable.illustration_home,
    title: String,
    subtitle: String,
    ctaText: String,
    onCta: () -> Unit = {},
    topBanner: (@Composable () -> Unit)? = null,
    contentWeightFraction: Float = 0.5f,
) {
    BoxWithConstraints(modifier = modifier) {
        val availableHeight = this.maxHeight

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (topBanner != null) {
                topBanner()
                Spacer(Modifier.height(8.dp))
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    .heightIn(max = availableHeight * contentWeightFraction),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = illustrationRes),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.45f)
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onCta, modifier = Modifier.fillMaxWidth().height(48.dp), shape = MaterialTheme.shapes.medium
            ) {
                Text(ctaText, color = MaterialTheme.colorScheme.background)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

