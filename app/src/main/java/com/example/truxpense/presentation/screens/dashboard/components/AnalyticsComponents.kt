package com.example.truxpense.presentation.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.truxpense.presentation.screens.dashboard.theme.DashboardDimens


@Composable
fun <T : Enum<T>> PeriodTabRow(
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelMapper: (T) -> String = { it.toString() },
) {
    val entries = selected::class.java.enumConstants as Array<T>
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(DashboardDimens.cornerToggleOuter))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(DashboardDimens.spaceXs),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            entries.forEach { mode ->
                val isSelected = mode == selected
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(DashboardDimens.cornerToggleInner))
                        .background(if (isSelected) Color.White else Color.Transparent).clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { onSelect(mode) },
                        ).padding(vertical = DashboardDimens.spaceMdL),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Text(
                        text = labelMapper(mode),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF4DB6B6) else Color(0xFF8A95A3),
                    )
                }
            }
        }
    }
}

/**
 * Reusable Date / Month navigator row used across screens.
 */
@Composable
fun DateNavigatorRow(
    label: String,
    canBack: Boolean,
    canForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = onBack,
            enabled = canBack,
            modifier = Modifier.size(DashboardDimens.iconButtonMd),
        ) {
            Icon(
                painter = painterResource(id = com.example.truxpense.R.drawable.left_arrow),
                contentDescription = "Previous",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (canBack) 1f else 0.3f),
                modifier = Modifier.size(DashboardDimens.iconNav),
            )
        }

        androidx.compose.material3.Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        IconButton(
            onClick = onForward,
            enabled = canForward,
            modifier = Modifier.size(DashboardDimens.iconButtonMd),
        ) {
            Icon(
                painter = painterResource(id = com.example.truxpense.R.drawable.right_arrow),
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (canForward) 1f else 0.3f),
                modifier = Modifier.size(DashboardDimens.iconNav),
            )
        }
    }
}

private enum class SamplePeriod { WEEK, MONTH, YEAR }

@Preview(showBackground = true, widthDp = 360)
@Composable
fun PeriodTabRowPreview() {
    var sel by remember { mutableStateOf(SamplePeriod.MONTH) }

    MaterialTheme {
        PeriodTabRow(
            selected = sel,
            onSelect = { sel = it },
            modifier = Modifier.fillMaxWidth().padding(DashboardDimens.spaceXl),
            labelMapper = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun DateNavigatorRowPreview() {
    MaterialTheme {
        DateNavigatorRow(
            label = "Feb 2026",
            canBack = true,
            canForward = false,
            onBack = {},
            onForward = {},
            modifier = Modifier.fillMaxWidth().padding(DashboardDimens.spaceXl)
        )
    }
}