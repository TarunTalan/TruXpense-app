package com.example.truxpense.presentation.screens.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.presentation.screens.dashboard.budget.BudgetCategoryDisplay
import com.example.truxpense.presentation.screens.dashboard.home.HomeSpendingCategory
import com.example.truxpense.util.progressColor
import com.example.truxpense.util.toCurrency
import java.text.NumberFormat


@Composable
fun BudgetProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    errorColor: Color = MaterialTheme.colorScheme.error
) {
    val color = progressColor(progress, errorColor)
    // Read debug overrides from ViewModel so UI file contains no debug state
    val debugVm: DashboardDebugViewModel = hiltViewModel()
    val debugPercent = debugVm.debugPercent.collectAsState().value
    val debugProgressValue = debugVm.debugProgressValue.collectAsState().value

    val progressForBar = debugProgressValue ?: progress
    val renderColor = if (debugProgressValue != null) MaterialTheme.colorScheme.primary else color

    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { progressForBar },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = renderColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )
        Spacer(Modifier.height(4.dp))
        val displayPercent = debugPercent ?: (progressForBar * 100).toInt()
        Text(
            text = "${displayPercent}% used",
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
    titleColor: Color? = null
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
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = titleColor ?: MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            BudgetProgressBar(progress = progress, errorColor = errorColor)
        }
    }
}

// Overload to accept the Home screen's SpendingCategory model and a NumberFormat
@Composable
fun SpendingCategoryCard(
    category: HomeSpendingCategory,
    fmt: NumberFormat,
    modifier: Modifier = Modifier,
    errorColor: Color = MaterialTheme.colorScheme.error,
    titleColor: Color? = null
) {
    val amountText = "${category.amount.toCurrency(fmt)}"
    val progress = category.progress.coerceIn(0f, 1f)
    SpendingCategoryCard(
        name = category.name,
        amountText = amountText,
        progress = progress,
        modifier = modifier,
        errorColor = errorColor,
        titleColor = titleColor
    )
}

// New overload: accept BudgetCategoryDisplay (from BudgetViewModel)
@Composable
fun SpendingCategoryCard(
    display: BudgetCategoryDisplay,
    modifier: Modifier = Modifier,
    titleColor: Color? = null
) {
    SpendingCategoryCard(
        name = display.category.name,
        amountText = display.amountText,
        progress = display.progress,
        modifier = modifier,
        errorColor = display.category.barColor,
        titleColor = titleColor
    )
}
