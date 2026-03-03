package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.components.NumberField
import com.example.truxpense.presentation.screens.dashboard.components.CategoryDropdown
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.theme.DashboardDimens
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

    AddBudgetScreenContent(
        amountInput = amountInput,
        selected = selected,
        categories = categories,
        isFormValid = isFormValid,
        onSelectCategory = { viewModel.setSelected(it) },
        onAmountChange = { viewModel.setAmountInput(it) },
        onCreateBudget = { viewModel.createBudget(onSave) },
        onBack = onBack,
    )
}

// Stateless, previewable UI separated from ViewModel usage
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetScreenContent(
    amountInput: String,
    selected: String?,
    categories: List<String>,
    isFormValid: Boolean,
    onSelectCategory: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCreateBudget: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                headerTitle = "Add Budget", showBack = true, onBack = onBack
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.padding(
                    start = DashboardDimens.screenPaddingH,
                    end = DashboardDimens.screenPaddingH,
                    bottom = DashboardDimens.spaceXxxl,
                ),
            ) {
                Button(
                    onClick = onCreateBudget,
                    enabled = isFormValid,
                    modifier = Modifier.fillMaxWidth().height(DashboardDimens.buttonHeight),
                    shape = RoundedCornerShape(DashboardDimens.cornerCard),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(0.5f),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(0.6f),
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text(
                        text = "Create budget",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = DashboardDimens.textXl,
                        color = MaterialTheme.colorScheme.background
                    )
                }
            }
        },
    ) { contentPadding ->

        Column(
            modifier = Modifier.fillMaxSize().clearFocusOnTap().padding(
                start = DashboardDimens.screenPaddingH,
                end = DashboardDimens.screenPaddingH,
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg),
        ) {
            Spacer(Modifier.height(DashboardDimens.spaceMd))

            // Details card with category and budget amount
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DashboardDimens.cornerCard),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // Category selector (bottom sheet handled inside CategoryDropdown)
                    CategoryDropdown(
                        selected = selected,
                        categories = categories,
                        onSelect = { onSelectCategory(it) },
                        iconForCategory = { cat -> iconForCategory(cat) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Select category",
                        inline = true,
                    )

                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = DashboardDimens.screenPaddingH + DashboardDimens.iconMd + DashboardDimens.spaceMd),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = DashboardDimens.dividerThin,
                    )

                    // Budget amount section
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(DashboardDimens.screenPaddingH)
                    ) {
                        Text(
                            text = "Monthly Budget limit",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Spacer(Modifier.height(DashboardDimens.spaceMd))

                        NumberField(
                            value = amountInput,
                            onValueChange = { onAmountChange(it) },
                            leadingIcon = {
                                Text(
                                    text = "₹",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = DashboardDimens.textXl,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            placeholder = "0",
                            bgColor = MaterialTheme.colorScheme.background,
                            contentPadding = DashboardDimens.cardPaddingComp.value.toInt(),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(DashboardDimens.spaceSm))
                        Text(
                            text = "This resets every month",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            // Budget period info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DashboardDimens.cornerCard),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(DashboardDimens.screenPaddingH)
                ) {
                    Text(
                        text = "Budget period",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.height(DashboardDimens.spaceMd))
                    Text(
                        text = "Monthly (resets on the 1st)",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Help text
            Text(
                text = "Budgets help you stay on top of your spending. You can edit or remove them anytime.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = DashboardDimens.lineHeightHelper,
                modifier = Modifier.padding(horizontal = DashboardDimens.spaceXs)
            )

            Spacer(Modifier.weight(1f))
        }
    }
}

// Helper function for category icons
private fun iconForCategory(category: String): Int = when (category.trim().lowercase()) {
    "food" -> R.drawable.food
    "transport" -> R.drawable.transport
    "bills" -> R.drawable.bills
    "shopping" -> R.drawable.shopping
    "travel" -> R.drawable.category_icon
    "health" -> R.drawable.health
    "education" -> R.drawable.category_icon
    "entertainment" -> R.drawable.entertainment
    "groceries" -> R.drawable.groceries
    else -> R.drawable.category_icon
}

// Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun AddBudgetScreenPreview() {
    MaterialTheme {
        AddBudgetScreenContent(
            amountInput = "",
            selected = null,
            categories = listOf("Food", "Transport", "Shopping", "Bills", "Health", "Other"),
            isFormValid = false,
            onSelectCategory = {},
            onAmountChange = {},
            onCreateBudget = {},
            onBack = {},
        )
    }
}