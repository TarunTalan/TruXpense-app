package com.example.truxpense.presentation.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truxpense.R
import com.example.truxpense.presentation.utils.AppCategories


@Composable
fun CategoryPickerGrid(
    categories: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    disabledCategories: Set<String> = emptySet(),
    label: String? = "Category",
) {
    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        val rows = categories.chunked(4)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowItems.forEach { cat ->
                        val isDisabled = cat.trim().lowercase() in disabledCategories
                        CategoryGridChip(
                            category = cat,
                            isSelected = cat == selected,
                            isDisabled = isDisabled,
                            onSelect = { if (!isDisabled) onSelect(cat) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Fill any trailing empty slots so the last row is aligned
                    repeat(4 - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── Private chip ─────────────────────────────────────────────────────────────

@Composable
private fun CategoryGridChip(
    category: String,
    isSelected: Boolean,
    isDisabled: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = when {
        isDisabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        isSelected -> MaterialTheme.colorScheme.primary
        else       -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    }
    val bgColor = when {
        isDisabled -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        else       -> MaterialTheme.colorScheme.surfaceContainer
    }
    val borderWidth = if (isSelected && !isDisabled) 1.5.dp else 1.dp

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = !isDisabled) { onSelect() }
            .alpha(if (isDisabled) 0.38f else 1f)
            .padding(horizontal = 4.dp)
            .padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(iconForCategory(category)),
            contentDescription = category,
            tint = Color.Unspecified,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = category,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Icon resolver (shared, internal to this file) ────────────────────────────

internal fun iconForCategory(category: String?): Int =
    when (category?.trim()?.lowercase()) {
        "food"          -> R.drawable.food
        "transport"     -> R.drawable.transport
        "bills"         -> R.drawable.bills_ic
        "shopping"      -> R.drawable.shopping
        "travel"        -> R.drawable.category_icon
        "health"        -> R.drawable.health
        "education"     -> R.drawable.education
        "entertainment" -> R.drawable.entertainment
        "groceries"     -> R.drawable.drink
        else            -> R.drawable.categories
    }

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun CategoryPickerGridPreview() {
    MaterialTheme {
        CategoryPickerGrid(
            categories = AppCategories.all,
            selected = "Food",
            onSelect = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, name = "With disabled categories")
@Composable
private fun CategoryPickerGridDisabledPreview() {
    MaterialTheme {
        CategoryPickerGrid(
            categories = AppCategories.all,
            selected = "Transport",
            onSelect = {},
            disabledCategories = setOf("food", "shopping", "bills"),
            modifier = Modifier.padding(16.dp),
        )
    }
}

