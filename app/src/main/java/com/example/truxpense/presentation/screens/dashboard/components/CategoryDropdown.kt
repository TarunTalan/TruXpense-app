package com.example.truxpense.presentation.screens.dashboard.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.truxpense.R
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.utils.AppCategories
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    selected: String?,
    categories: List<String>,
    onSelect: (String) -> Unit,
    iconForCategory: (String) -> Int,
    modifier: Modifier = Modifier,
    placeholder: String = "Select category",
    existingBudgetedCategories: Set<String> = emptySet(),
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    var localExpanded by remember { mutableStateOf(false) }
    var customMode by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf("") }

    val dismissDropdown = {
        localExpanded = false
        customMode = false
        customInput = ""
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    // Inline trigger row
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DashboardDimens.detailRowHeight)
            .clickable {
                keyboardController?.hide()
                focusManager.clearFocus()
                localExpanded = !localExpanded
            }
            .padding(horizontal = DashboardDimens.screenPaddingH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
    ) {
        // Leading icon
        Box(modifier = Modifier.size(DashboardDimens.iconMd), contentAlignment = Alignment.Center) {
            val selTrim = selected?.trim().orEmpty()
            val isGenericIcon = selTrim.isEmpty() || selTrim.lowercase() == AppCategories.CUSTOM.lowercase()
            val iconRes = if (!isGenericIcon) iconForCategory(selected ?: "") else R.drawable.category_icon
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (selected != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DashboardDimens.iconMd),
            )
        }

        // Label + value
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(DashboardDimens.spaceXxs))
            Text(
                text = selected ?: placeholder,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected != null) FontWeight.Medium else FontWeight.Normal,
                color = if (selected.isNullOrEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onBackground,
            )
        }

        // Trailing chevron
        Box(modifier = Modifier.size(DashboardDimens.iconMd), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.drop_down_icon),
                contentDescription = "Open category",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DashboardDimens.iconMd),
            )
        }
    }

    // Dropdown list as a bottom sheet overlay (component-local state)
    BackHandler(enabled = localExpanded) { dismissDropdown() }

    if (localExpanded) {
        // Use ModalBottomSheet from Material3 to show overlay list
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true, confirmValueChange = { true })

        ModalBottomSheet(
            onDismissRequest = { dismissDropdown() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            if (customMode) {
                // ── Custom category input ──────────────────────────────────
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DashboardDimens.screenPaddingH)
                        .padding(bottom = DashboardDimens.spaceXxxl),
                    verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
                ) {
                    Spacer(Modifier.height(DashboardDimens.spaceSm))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = "Custom category", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(DashboardDimens.spaceSm))

                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { customInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = {
                            Text(
                                text = "e.g. Rent, Gym, Pet care…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            val trimmed = customInput.trim()
                            if (trimmed.isNotEmpty()) {
                                onSelect(trimmed)
                                scope.launch { sheetState.hide() }
                                dismissDropdown()
                            }
                        }),
                        shape = RoundedCornerShape(DashboardDimens.cornerCard),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
                    ) {
                        OutlinedButton(
                            onClick = { customMode = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(DashboardDimens.cornerCard),
                        ) { Text("Back") }

                        Button(
                            onClick = {
                                val trimmed = customInput.trim()
                                if (trimmed.isNotEmpty()) {
                                    onSelect(trimmed)
                                    scope.launch { sheetState.hide() }
                                    dismissDropdown()
                                }
                            },
                            enabled = customInput.trim().isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(DashboardDimens.cornerCard),
                        ) { Text("Confirm") }
                    }
                }
            } else {
                // ── Normal category list ───────────────────────────────────

                // Top chips row for already-budgeted categories
                if (existingBudgetedCategories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(existingBudgetedCategories.toList()) { cat ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 2.dp,
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                Text(
                                    text = cat.replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = DashboardDimens.spaceMd)
                ) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = "Select category", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(DashboardDimens.spaceSm))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth(),
                    ) {
                        itemsIndexed(categories) { _, item ->
                            val isCustomItem = item == AppCategories.CUSTOM
                            val isSelected = selected == item
                            val containerColor =
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surface
                            val iconRes = iconForCategory(item)
                            val isBudgeted = item.trim().lowercase() in existingBudgetedCategories

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = DashboardDimens.listPaddingH),
                            ) {
                                // The list item — blurred + dimmed when already budgeted
                                Row(
                                    modifier = (if (isBudgeted)
                                        Modifier.blur(3.dp).alpha(0.4f)
                                    else Modifier)
                                        .clickable(enabled = !isBudgeted) {
                                            if (isCustomItem) {
                                                customMode = true
                                            } else {
                                                onSelect(item)
                                                scope.launch { sheetState.hide() }
                                                dismissDropdown()
                                            }
                                        },
                                ) {
                                    ListItem(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(DashboardDimens.cornerChip)),
                                        colors = ListItemDefaults.colors(containerColor = containerColor),
                                        leadingContent = {
                                            if (isCustomItem) {
                                                Icon(
                                                    painter = painterResource(R.drawable.add_notes_icon),
                                                    contentDescription = "Custom category",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(DashboardDimens.iconMd),
                                                )
                                            } else {
                                                Icon(
                                                    painter = painterResource(iconRes),
                                                    contentDescription = "$item icon",
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onBackground,
                                                    modifier = Modifier.size(DashboardDimens.iconMd),
                                                )
                                            }
                                        },
                                        headlineContent = {
                                            Text(
                                                text = if (isCustomItem) "Custom category" else item,
                                                color = if (isCustomItem) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onBackground,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isSelected || isCustomItem) FontWeight.SemiBold
                                                else FontWeight.Normal,
                                            )
                                        },
                                        trailingContent = if (isCustomItem) ({
                                            Icon(
                                                painter = painterResource(R.drawable.drop_down_icon),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(DashboardDimens.iconMd),
                                            )
                                        }) else null,
                                    )
                                }

                                // "Budget set" overlay badge — shown on top of the blurred row
                                if (isBudgeted) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable(enabled = false) {},
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            tonalElevation = 2.dp,
                                            modifier = Modifier.padding(end = DashboardDimens.screenPaddingH),
                                        ) {
                                            Text(
                                                text = "Budget set",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            if (categories.indexOf(item) < categories.lastIndex) {
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(DashboardDimens.spaceLg))
                }
            }
        }
    }
}

// Preview for the CategoryDropdown composable
@Preview(showBackground = true, widthDp = 360)
@Composable
fun CategoryDropdownPreview() {
    MaterialTheme {
        CategoryDropdown(
            selected = null,
            categories = AppCategories.all,
            onSelect = {},
            iconForCategory = { R.drawable.category_icon },
            existingBudgetedCategories = setOf("food", "bills")
        )
    }
}

// Helper to render only the sheet content (chips + list) for previewing the bottom sheet
@Composable
private fun CategoryDropdownSheetContent(
    selected: String? = null,
    categories: List<String> = AppCategories.all,
    onSelect: (String) -> Unit = {},
    iconForCategory: (String) -> Int = { R.drawable.category_icon },
    existingBudgetedCategories: Set<String> = emptySet(),
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DashboardDimens.spaceMd)

    ) {
        if (existingBudgetedCategories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(existingBudgetedCategories.toList()) { cat ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp,
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Text(
                            text = cat.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(text = "Select category", style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(DashboardDimens.spaceSm))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.listPaddingH),
        ) {
            itemsIndexed(categories) { _, item ->
                val isCustomItem = item == AppCategories.CUSTOM
                val isSelected = selected == item
                val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surface
                val isBudgeted = item.trim().lowercase() in existingBudgetedCategories
                val rowModifier = if (isBudgeted) Modifier.blur(6.dp).alpha(0.6f) else Modifier

                Row(modifier = rowModifier.clickable(enabled = !isBudgeted) { onSelect(item) }) {
                    ListItem(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(DashboardDimens.cornerChip)),
                        colors = ListItemDefaults.colors(containerColor = containerColor),
                        leadingContent = {
                            Icon(
                                painter = painterResource(iconForCategory(item)),
                                contentDescription = null,
                                tint = when {
                                    isCustomItem -> MaterialTheme.colorScheme.primary
                                    isSelected   -> MaterialTheme.colorScheme.primary
                                    else         -> MaterialTheme.colorScheme.onBackground
                                },
                                modifier = Modifier.size(DashboardDimens.iconMd),
                            )
                        },
                        headlineContent = {
                            Text(
                                text = if (isCustomItem) "Custom category" else item,
                                color = if (isCustomItem) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected || isCustomItem) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    )
                }
                if (categories.indexOf(item) < categories.lastIndex) Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 700)
@Composable
fun CategoryDropdownBottomSheetPreview() {
    MaterialTheme {
        CategoryDropdownSheetContent(
            selected = "Food",
            existingBudgetedCategories = setOf("food", "bills")
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 300, name = "Custom input mode")
@Composable
fun CategoryDropdownCustomInputPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DashboardDimens.screenPaddingH)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
        ) {
            Spacer(Modifier.height(DashboardDimens.spaceSm))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Custom category", style = MaterialTheme.typography.titleSmall)
            }
            OutlinedTextField(
                value = "Pet care",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Rent, Gym, Pet care…") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd)
            ) {
                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Text("Back")
                }
                Button(onClick = {}, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Text("Confirm")
                }
            }
        }
    }
}

