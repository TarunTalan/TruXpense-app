package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.presentation.screens.dashboard.components.AmountInputCard
import com.example.truxpense.presentation.screens.dashboard.components.CategoryPickerGrid
import com.example.truxpense.presentation.screens.dashboard.components.GradientCard
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.utils.AppCategories
import com.example.truxpense.presentation.utils.clearFocusOnTap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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
    val isDuplicateCategory by viewModel.isDuplicateCategory.collectAsState()
    val existingBudgetedCategories by viewModel.existingBudgetedCategories.collectAsState()

    AddBudgetScreenContent(
        amountInput = amountInput,
        selected = selected,
        categories = categories,
        isFormValid = isFormValid,
        isDuplicateCategory = isDuplicateCategory,
        existingBudgetedCategories = existingBudgetedCategories,
        onSelectCategory = { viewModel.setSelected(it) },
        onAmountChange = { viewModel.setAmountInput(it) },
        onCreateBudget = { viewModel.createBudget(onSave) },
        onBack = onBack,
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// SCREEN CONTENT  (stateless, previewable)
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class
)
@Composable
fun AddBudgetScreenContent(
    amountInput: String,
    selected: String?,
    categories: List<String>,
    isFormValid: Boolean,
    isDuplicateCategory: Boolean = false,
    existingBudgetedCategories: Set<String> = emptySet(),
    onSelectCategory: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCreateBudget: () -> Unit,
    onBack: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible

    // When the system dismisses the keyboard (back gesture / done action),
    // clear focus so no field inadvertently re-summons it on recomposition.
    LaunchedEffect(isImeVisible) {
        if (!isImeVisible) focusManager.clearFocus(force = false)
    }

    // BringIntoViewRequester for the amount card — scrolls it into view
    // after the keyboard finishes animating in (~300 ms).
    val amountBringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    Scaffold(
        // Zero out Scaffold's own window-inset handling so we control every
        // edge ourselves — prevents double-inset application.
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(headerTitle = "Add Budget", showBack = true, onBack = onBack)
        },

        // The Save button is pinned above the keyboard via imePadding() and stays
        // above the system gesture bar via navigationBarsPadding().
        // Scaffold measures the resulting height and passes it back as
        // innerPadding.bottom, so the scroll column shrinks correctly —
        // no double-counting.
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()   // respects gesture-nav bar
                        .imePadding()              // rises above keyboard
                        .padding(
                            horizontal = DashboardDimens.screenPaddingH,
                            vertical = 12.dp,
                        ),
                ) {
                    Button(
                        onClick = {
                            // Dismiss keyboard before saving so the transition is clean
                            focusManager.clearFocus()
                            onCreateBudget()
                        },
                        enabled = isFormValid,
                        modifier = Modifier.fillMaxWidth().height(DashboardDimens.buttonHeight),
                        shape = RoundedCornerShape(DashboardDimens.cornerCard),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(0.5f),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(0.6f),
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                    ) {
                        Text(
                            text = "Create budget",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = DashboardDimens.textXl,
                            color = MaterialTheme.colorScheme.background,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        // innerPadding.bottom is the live bottomBar height (grows as keyboard opens).
        // We do NOT add imePadding() here — that would double-count it.
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)             // tracks bottomBar height automatically
                .padding(horizontal = DashboardDimens.screenPaddingH).verticalScroll(rememberScrollState())
                .clearFocusOnTap(),                // tap outside a field → dismiss keyboard
            verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg),
        ) {
            Spacer(Modifier.height(DashboardDimens.spaceMd))

            // ── Amount input card ─────────────────────────────────────────
            // Wrapped in bringIntoViewRequester so the keyboard doesn't
            // cover the amount field when it opens.
            AmountInputCard(
                rawAmount = amountInput,
                onRawChange = onAmountChange,
                question = "What's the monthly budget limit?",
                modifier = Modifier.fillMaxWidth().bringIntoViewRequester(amountBringIntoView),
                onFocused = {
                    // Delay matches the keyboard slide-in animation (~300 ms)
                    scope.launch { delay(320); amountBringIntoView.bringIntoView() }
                },
            )

            // ── Category picker card ──────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DashboardDimens.cornerCard),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                elevation = CardDefaults.cardElevation(DashboardDimens.cardElevation),
            ) {
                CategoryPickerGrid(
                    categories = categories,
                    selected = selected,
                    onSelect = onSelectCategory,
                    disabledCategories = existingBudgetedCategories,
                    label = "Category",
                    modifier = Modifier.fillMaxWidth().padding(
                        horizontal = DashboardDimens.screenPaddingH,
                        vertical = DashboardDimens.spaceMd,
                    ),
                )
            }

            // ── Budget period info card ───────────────────────────────────
            GradientCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(DashboardDimens.screenPaddingH),
                ) {
                    Text(
                        text = "Budget period",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.height(DashboardDimens.spaceMd))
                    Text(
                        text = "Monthly (resets on the 1st)",
                        color = MaterialTheme.colorScheme.errorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // ── Help text ─────────────────────────────────────────────────
            Text(
                text = "*Budget help you stay of your spending. You can edit or remove them anytime.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                fontSize = 10.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(horizontal = DashboardDimens.screenPaddingH),
            )

            // Bottom guard — ensures last card never sits flush against the
            // bottomBar even if innerPadding.bottom momentarily lags.
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun AddBudgetScreenPreview() {
    MaterialTheme {
        AddBudgetScreenContent(
            amountInput = "",
            selected = null,
            categories = AppCategories.all,
            isFormValid = false,
            isDuplicateCategory = false,
            existingBudgetedCategories = emptySet(),
            onSelectCategory = {},
            onAmountChange = {},
            onCreateBudget = {},
            onBack = {},
        )
    }
}