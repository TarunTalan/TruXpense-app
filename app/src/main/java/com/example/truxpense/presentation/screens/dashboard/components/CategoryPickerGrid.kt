package com.example.truxpense.presentation.screens.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.truxpense.R
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.utils.AppCategories
import com.example.truxpense.presentation.utils.SimpleTextField

// ── All icons available in the custom-category icon picker ───────────────────
private val customIconOptions: List<Pair<String, Int>> = listOf(
    "water" to R.drawable.water,
    "wifi" to R.drawable.wifi,
    "Bills" to R.drawable.bills_ic,
    "yoga" to R.drawable.yoga,
    "Travel" to R.drawable.travel,
    "books" to R.drawable.reading_books,
    "maintenance" to R.drawable.maintenance,
    "insurance" to R.drawable.insurance,
    "home" to R.drawable.home_investment__1_,
    "fuel" to R.drawable.fuel,
    "fitness" to R.drawable.fitness,
    "emi" to R.drawable.emi_calculator,
    "electricity" to R.drawable.electricity,
    "Savings" to R.drawable.savings,
    "Cashback" to R.drawable.cashback,
    "Budget" to R.drawable.budget,
    "Bulb" to R.drawable.bulb,
    "Archive" to R.drawable.archive,
    "Other" to R.drawable.categories,
)

// ── Custom category dialog ────────────────────────────────────────────────────

@Composable
fun CustomCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, iconRes: Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedIconRes by remember { mutableIntStateOf(customIconOptions.first().second) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 6.dp,
        ) {
            Column(
                // keep vertical spacing but use 16.dp horizontal padding for inner content
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ── Title ──────────────────────────────────────────────────
                Text(
                    text = "Create custom category",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                // ── Name input ─────────────────────────────────────────────
                // Use the new SimpleTextField (no trailing icon) with same visual style.
                SimpleTextField(
                    value = name,
                    onValueChange = { if (it.length <= 20) name = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                        .height(DashboardDimens.buttonHeight),
                    bgColor = MaterialTheme.colorScheme.surfaceContainer,
                    placeholder = "Category name (max 20 chars)",
                    label = "",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    imeAction = ImeAction.Done,
                    onImeAction = { if (name.isNotBlank()) onConfirm(name.trim(), selectedIconRes) },
                    singleLine = true,
                    contentPadding = 0,
                )

                // ── Icon picker label ──────────────────────────────────────
                Text(
                    text = "Select icon",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                // ── Horizontally scrollable icon row ───────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    customIconOptions.forEach { (label, iconRes) ->
                        val isSelected = iconRes == selectedIconRes
                        val borderColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            animationSpec = tween(200),
                            label = "icon_border",
                        )

                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.background.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surfaceContainer,
                            animationSpec = tween(200),
                            label = "icon_bg",
                        )
                        Column(
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(bgColor).border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(12.dp),
                            ).clickable { selectedIconRes = iconRes }.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = label,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }

                // ── Cancel / Add buttons ───────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Cancel") }
                    Button(
                        onClick = { if (name.isNotBlank()) onConfirm(name.trim(), selectedIconRes) },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Add") }
                }
            }
        }
    }
}

// ── Category picker grid ──────────────────────────────────────────────────────

@Composable
fun CategoryPickerGrid(
    categories: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    disabledCategories: Set<String> = emptySet(),
    label: String? = "Category",
    /** Override to supply a different icon per label (e.g. income sources). */
    iconResolver: (String) -> Int = ::iconForCategory,
    /** Whether to show the chip label text under each icon. */
    showChipLabel: Boolean = true,
    /** Override icon size/shape; defaults to a 32×32 square. */
    iconModifier: Modifier = Modifier.size(32.dp),
    /**
     * Called when the user creates a custom category (name + chosen icon res).
     * If null the Custom chip behaves like a normal selectable chip.
     */
    onCustomCategoryAdded: ((name: String, iconRes: Int) -> Unit)? = null,
) {
    var showCustomDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // ensure consistent inner horizontal padding when a bare modifier is provided
        val innerModifier = if (modifier == Modifier) Modifier else modifier

        if (label != null) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = innerModifier.then(Modifier.padding(bottom = 8.dp)),
            )
        }

        val rows = categories.chunked(4)
        Column(modifier = innerModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowItems.forEach { cat ->
                        val isCustomChip = cat == AppCategories.CUSTOM && onCustomCategoryAdded != null
                        val isDisabled = cat.trim().lowercase() in disabledCategories
                        CategoryGridChip(
                            category = cat,
                            isSelected = cat == selected,
                            isDisabled = isDisabled,
                            onSelect = {
                                if (!isDisabled) {
                                    if (isCustomChip) showCustomDialog = true
                                    else onSelect(cat)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            iconRes = iconResolver(cat),
                            showLabel = showChipLabel,
                            iconModifier = iconModifier,
                        )
                    }
                    // Fill trailing empty slots so last row stays aligned
                    repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomCategoryDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { name, iconRes ->
                showCustomDialog = false
                onCustomCategoryAdded?.invoke(name, iconRes)
                onSelect(name)
            },
        )
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
    iconRes: Int = iconForCategory(category),
    showLabel: Boolean = true,
    iconModifier: Modifier = Modifier.size(32.dp),
) {
    val borderColor = when {
        isDisabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    }
    val bgColor = when {
        isDisabled -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
        isSelected -> MaterialTheme.colorScheme.background.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val borderWidth = if (isSelected && !isDisabled) 1.5.dp else 1.dp

    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp)).clickable(enabled = !isDisabled) { onSelect() }
            .alpha(if (isDisabled) 0.38f else 1f).padding(horizontal = 4.dp).padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = category,
            tint = Color.Unspecified,
            modifier = iconModifier,
        )
        if (showLabel) {
            Text(
                text = category,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected && !isDisabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Icon resolvers ────────────────────────────────────────────────────────────

internal fun iconForCategory(category: String?): Int = when (category?.trim()?.lowercase()) {
    "food" -> R.drawable.food
    "transport" -> R.drawable.transport
    "bills" -> R.drawable.bills_ic
    "shopping" -> R.drawable.shopping
    "travel" -> R.drawable.travel
    "health" -> R.drawable.health
    "education" -> R.drawable.education
    "entertainment" -> R.drawable.entertainment
    "groceries" -> R.drawable.drink
    else -> R.drawable.categories
}

/** Icon resolver for income source categories. */
fun iconForIncomeSource(source: String?): Int = when (source?.trim()?.lowercase()) {
    "salary" -> R.drawable.salary
    "freelance" -> R.drawable.freelance
    "bonus" -> R.drawable.commission
    "business" -> R.drawable.business
    "investment" -> R.drawable.home_investment
    "gift" -> R.drawable.gift
    "rental" -> R.drawable.savings
    "refund" -> R.drawable.refund
    "cashback" -> R.drawable.cashback
    else -> R.drawable.categories
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

@Preview(showBackground = true, widthDp = 390, name = "Custom category dialog")
@Composable
private fun CustomCategoryDialogPreview() {
    MaterialTheme {
        CustomCategoryDialog(onDismiss = {}, onConfirm = { _, _ -> })
    }
}