package com.example.truxpense.presentation.screens.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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

// ── All icons available in the custom-category icon picker ───────────────────
private val customIconOptions: List<Pair<String, Int>> = listOf(
    "Food" to R.drawable.food,
    "Transport" to R.drawable.transport,
    "Bills" to R.drawable.bills_ic,
    "Shopping" to R.drawable.shopping,
    "Travel" to R.drawable.category_icon,
    "Health" to R.drawable.health,
    "Education" to R.drawable.education,
    "Entertainment" to R.drawable.entertainment,
    "Groceries" to R.drawable.drink,
    "Money" to R.drawable.money,
    "Business" to R.drawable.business,
    "Investment" to R.drawable.home_investment,
    "Gift" to R.drawable.gift,
    "Savings" to R.drawable.savings,
    "Refund" to R.drawable.refund,
    "Cashback" to R.drawable.cashback,
    "Analytics" to R.drawable.analytics,
    "Budget" to R.drawable.budget,
    "Commission" to R.drawable.commission,
    "Cosmetics" to R.drawable.cosmetics,
    "Bulb" to R.drawable.bulb,
    "Archive" to R.drawable.archive,
    "Report" to R.drawable.report_ic,
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
                modifier = Modifier.padding(20.dp),
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 20) name = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).height(DashboardDimens.buttonHeight),
                    placeholder = {
                        Text(
                            "Category name (max 20 chars)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (name.isNotBlank()) onConfirm(name.trim(), selectedIconRes)
                    }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        cursorColor = MaterialTheme.colorScheme.onBackground,
                    ),
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
    /**
     * Called when the user creates a custom category (name + chosen icon res).
     * If null the Custom chip behaves like a normal selectable chip.
     */
    onCustomCategoryAdded: ((name: String, iconRes: Int) -> Unit)? = null,
) {
    var showCustomDialog by remember { mutableStateOf(false) }

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
            modifier = Modifier.size(32.dp),
        )
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

// ── Icon resolvers ────────────────────────────────────────────────────────────

internal fun iconForCategory(category: String?): Int = when (category?.trim()?.lowercase()) {
    "food" -> R.drawable.food
    "transport" -> R.drawable.transport
    "bills" -> R.drawable.bills_ic
    "shopping" -> R.drawable.shopping
    "travel" -> R.drawable.category_icon
    "health" -> R.drawable.health
    "education" -> R.drawable.education
    "entertainment" -> R.drawable.entertainment
    "groceries" -> R.drawable.drink
    else -> R.drawable.categories
}

/** Icon resolver for income source categories. */
fun iconForIncomeSource(source: String?): Int = when (source?.trim()?.lowercase()) {
    "salary" -> R.drawable.money
    "freelance" -> R.drawable.freelance
    "business" -> R.drawable.business
    "investment" -> R.drawable.home_investment
    "gift" -> R.drawable.gift
    "rental" -> R.drawable.savings
    "refund" -> R.drawable.refund
    "cashback" -> R.drawable.cashback
    "other" -> R.drawable.categories
    else -> R.drawable.add_inocme
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
