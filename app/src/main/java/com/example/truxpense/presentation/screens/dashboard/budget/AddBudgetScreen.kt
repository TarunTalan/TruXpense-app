package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.components.CategoryDropdown
import com.example.truxpense.presentation.components.NumberField
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.screens.dashboard.theme.DashboardDimens
import com.example.truxpense.presentation.utils.clearFocusOnTap


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetScreen(
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    val viewModel: AddBudgetViewModel = hiltViewModel()

    val amountInput by viewModel.amountInput.collectAsState()
    val selected by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val isFormValid by viewModel.isFormValid.collectAsState()

    // Pure UI state — dropdown open/close is presentational only, not business logic
    var expanded by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val dismissDropdown = {
        expanded = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    Scaffold(
        topBar = { ScreenTopBar(headerTitle = "Add Budget", showBack = true, onBack = onBack) },
        bottomBar = {
            if (!expanded) {
                Column(
                    modifier = Modifier.padding(
                        top = DashboardDimens.spaceMdL,
                        start = DashboardDimens.screenPaddingH,
                        end = DashboardDimens.screenPaddingH,
                        bottom = DashboardDimens.spaceXxxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
                ) {
                    Button(
                        onClick = { viewModel.createBudget(onSave) },
                        enabled = isFormValid,
                        modifier = Modifier.fillMaxWidth().height(DashboardDimens.buttonHeight),
                        shape = RoundedCornerShape(DashboardDimens.cornerCard),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(0.5f),
                            disabledContentColor = MaterialTheme.colorScheme.background.copy(0.4f),
                        ),
                    ) {
                        Text(
                            text = "Create budget",
                            color = MaterialTheme.colorScheme.background,
                            fontSize = DashboardDimens.textXxl,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
    ) { contentPadding ->

        val bottomPad = if (expanded) 0.dp else contentPadding.calculateBottomPadding()

        Column(
            modifier = Modifier.fillMaxSize().clearFocusOnTap().padding(
                start = DashboardDimens.screenPaddingH,
                end = DashboardDimens.screenPaddingH,
                top = contentPadding.calculateTopPadding(),
                bottom = bottomPad,
            ),
            verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg),
        ) {

            // Category selector
            CategoryDropdown(
                selected = selected,
                categories = categories,
                onSelect = { viewModel.setSelected(it) },
                iconForCategory = { viewModel.iconForCategory(it) },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Dropdown list
            BackHandler(enabled = expanded) { dismissDropdown() }

            if (expanded) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium,
                    ).padding(bottom = DashboardDimens.spaceMdL),
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
                            modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium).padding(
                                horizontal = DashboardDimens.listPaddingH,
                                vertical = DashboardDimens.listPaddingV,
                            ),
                        ) {
                            itemsIndexed(categories) { _, item ->
                                val isSelected = selected == item
                                val containerColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.primaryContainer

                                // Icon resolved by VM — no when-block in the composable
                                val iconRes = viewModel.iconForCategory(item)

                                ListItem(
                                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable {
                                        viewModel.setSelected(item)
                                        // no search/query handling — simple dropdown-only
                                        dismissDropdown()
                                    },
                                    colors = ListItemDefaults.colors(containerColor = containerColor),
                                    trailingContent = {
                                        // Show icon at trailing for all categories except 'Other'/'Others'
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

            // Budget limit section (hidden while dropdown is open)
            if (!expanded) {
                SectionCard {
                    Column(modifier = Modifier.padding(DashboardDimens.cardPaddingComp)) {
                        Text(
                            text = "Monthly Budget limit",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = DashboardDimens.textXl,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(DashboardDimens.spaceLg))

                        Text(
                            text = "Enter amount",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = DashboardDimens.textMd,
                        )
                        Spacer(Modifier.height(DashboardDimens.spaceXs))

                        NumberField(
                            value = amountInput,
                            onValueChange = { viewModel.setAmountInput(it) },
                            leadingIcon = {
                                Text(
                                    text = "₹",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = DashboardDimens.textXl,
                                )
                            },
                            placeholder = "0",
                            bgColor = MaterialTheme.colorScheme.background,
                            contentPadding = DashboardDimens.cardPaddingComp.value.toInt(),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(DashboardDimens.spaceXs))
                        Text(
                            text = "This resets every month",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = DashboardDimens.textMd,
                        )
                    }
                }

                Spacer(Modifier.height(DashboardDimens.spaceLg))

                Column {
                    Text(
                        text = "Budget period",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = DashboardDimens.textMd,
                    )
                    Spacer(Modifier.height(DashboardDimens.spaceMd))
                    Text(
                        text = "Monthly (resets on the 1st)",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = DashboardDimens.textLg,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.height(DashboardDimens.spaceLg))

                Text(
                    text = "Budgets help you stay on top of your spending. You can edit or remove them anytime.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = DashboardDimens.textMd,
                    lineHeight = DashboardDimens.lineHeightHelper,
                )

                Spacer(Modifier.weight(1f))
            }
        }
    }
}


// Section card shell

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(DashboardDimens.cornerCard))
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        content()
    }
}

// Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 800, backgroundColor = 0xFFFFFFFF)
@Composable
fun AddBudgetScreenPreview() {
    MaterialTheme { AddBudgetScreen() }
}