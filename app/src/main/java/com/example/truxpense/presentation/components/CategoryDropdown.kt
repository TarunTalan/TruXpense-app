package com.example.truxpense.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.truxpense.presentation.screens.dashboard.theme.DashboardDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    selected: String?,
    categories: List<String>,
    onSelect: (String) -> Unit,
    iconForCategory: (String) -> Int,
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    placeholder: String = "Select category",
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val dismissDropdown = {
        onExpandedChange(false)
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    // Card wrapper (same visual as SectionCard in-screen)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DashboardDimens.cornerCard))
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(DashboardDimens.cardPaddingComp)) {
            Text(
                text = "Category",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = DashboardDimens.textLg,
                fontWeight = MaterialTheme.typography.labelLarge.fontWeight,
            )

            Spacer(Modifier.height(DashboardDimens.spaceMd))

            val rotation by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                label = "dropdown_chevron",
            )

            Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onExpandedChange(!expanded)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DashboardDimens.cardPaddingComp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val selectedForIcon = selected?.trim() ?: ""
                        val iconForSel: Int? = when {
                            selectedForIcon.isEmpty() -> com.example.truxpense.R.drawable.category_icon
                            selectedForIcon.lowercase() == "other" || selectedForIcon.lowercase() == "others" -> null
                            else -> iconForCategory(selectedForIcon)
                        }

                        if (iconForSel != null) {
                            Icon(
                                painter = painterResource(iconForSel),
                                contentDescription = "category icon",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(28.dp).padding(end = 10.dp)
                            )
                        }

                        Text(
                            text = selected ?: placeholder,
                            color = if (selected.isNullOrEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge
                        )

                        IconButton(onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onExpandedChange(!expanded)
                        }) {
                            Icon(
                                painter = painterResource(com.example.truxpense.R.drawable.drop_down_icon),
                                contentDescription = if (expanded) "Close" else "Open",
                                modifier = Modifier.rotate(rotation),
                            )
                        }
                    }
                }
            }
        }
    }

    // Dropdown list
    BackHandler(enabled = expanded) { dismissDropdown() }

    if (expanded) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(bottom = DashboardDimens.spaceMdL),
        ) {
            if (categories.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No categories found", color = MaterialTheme.colorScheme.onBackground)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium)
                        .padding(
                            horizontal = DashboardDimens.listPaddingH,
                            vertical = DashboardDimens.listPaddingV,
                        ),
                ) {
                    itemsIndexed(categories) { _, item ->
                        val isSelected = selected == item
                        val containerColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.primaryContainer

                        val iconRes = iconForCategory(item)

                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    onSelect(item)
                                    dismissDropdown()
                                },
                            colors = ListItemDefaults.colors(containerColor = containerColor),
                            trailingContent = {
                                val trimmed = item.trim().lowercase()
                                if (trimmed != "other" && trimmed != "others") {
                                    Icon(
                                        painter = painterResource(iconRes),
                                        contentDescription = "$item icon",
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.size(DashboardDimens.iconMd),
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(DashboardDimens.iconMd))
                                }
                            },
                            headlineContent = {
                                Text(
                                    text = item,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
