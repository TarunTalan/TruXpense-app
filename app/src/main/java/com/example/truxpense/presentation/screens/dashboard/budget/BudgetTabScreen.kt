package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.SpendingCategoryCard
import java.text.NumberFormat
import java.util.*
import androidx.hilt.navigation.compose.hiltViewModel


val months = listOf(
    "January 2026",
    "February 2026",
    "March 2026"
)

// ─── Helpers ─────────────────────────────────────────────────────────────────
// Match HomeTabScreen's currency formatter so both screens display amounts the same way
private fun currencyFormat(currencyCode: String?): NumberFormat =
    runCatching {
        val locale = Locale.Builder().setLanguage("en").setRegion("IN").build()
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(currencyCode ?: "INR")
        }
    }.getOrElse { NumberFormat.getCurrencyInstance() }

private fun Double.toCurrency(fmt: NumberFormat) = runCatching { fmt.format(this) }.getOrDefault("$this")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "Budgets",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun BudgetScreen(
    previewBudgets: List<BudgetCategory>? = null,
    vm: BudgetViewModel = hiltViewModel(),
    onAddBudget: () -> Unit = {},
    currencyCode: String? = "INR",
    onNavigateToDetail: ((BudgetCategory) -> Unit)? = null
) {
    var monthIndex by remember { mutableIntStateOf(1) }
    // selected budget category to preview at top (used only when onNavigateToDetail is null)
    var selectedCategory by remember { mutableStateOf<BudgetCategory?>(null) }

    // Observe UI categories provided by ViewModel
    val displayedBudgets by vm.categories.collectAsState()
    val totalBudget by vm.totalBudget.collectAsState()
    val totalSpent by vm.totalSpent.collectAsState()

    // Allow preview override for design previews
    val budgetsToShow = previewBudgets ?: displayedBudgets

    // If there are no real budgets, show the empty state with CTA
    if (budgetsToShow.isEmpty()) {
        BudgetsEmptyScreen(onAddBudget = onAddBudget)
        return
    }

    val fmt = remember(currencyCode) { currencyFormat(currencyCode) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BudgetTopBar() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBudget,
                shape = MaterialTheme.shapes.medium,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.background,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add budget")
            }
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 10.dp), // match HomeTabScreen horizontal spacing
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {


            // ── Month Selector ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (monthIndex > 0) monthIndex-- },
                        enabled = monthIndex > 0
                    ) {
                        Icon(
                            painter = painterResource(id= R.drawable.left_arrow),
                            contentDescription = "Previous month",
                            tint = if (monthIndex > 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f)
                        )
                    }

                    Text(
                        text = months[monthIndex],
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    IconButton(
                        onClick = { if (monthIndex < months.lastIndex) monthIndex++ },
                        enabled = monthIndex < months.lastIndex
                    ) {
                        Icon(
                            painter = painterResource(id= R.drawable.right_arrow),
                            contentDescription = "Next month",
                            tint = if (monthIndex < months.lastIndex) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f)
                        )
                    }
                }
            }

            // ── Total Budget Card ──────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Total budget",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = totalBudget.toDouble().toCurrency(fmt),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${totalSpent.toDouble().toCurrency(fmt)} spent",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Budget Category Items ──────────────────────────────────────
            items(budgetsToShow, key = { it.id }) { category ->
                val amountText = "${category.spent.toDouble().toCurrency(fmt)} / ${category.total.toDouble().toCurrency(fmt)}"
                val progress = if (category.total > 0) (category.spent.toFloat() / category.total.toFloat()).coerceIn(0f, 1f) else 0f
                SpendingCategoryCard(
                    name = category.name,
                    amountText = amountText,
                    progress = progress,
                    titleColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // if navigation callback provided, navigate to detail screen
                            if (onNavigateToDetail != null) {
                                onNavigateToDetail.invoke(category)
                            } else {
                                // toggle selection preview when no navigation provided
                                selectedCategory = if (selectedCategory?.id == category.id) null else category
                            }
                        }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
            }
        }
    }
}



@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BudgetScreenPreview() {
    val sample = listOf(
        BudgetCategory(id = 1, name = "Food", spent = 1200, total = 3000, barColor = Color(0xFFEF4444)),
        BudgetCategory(id = 2, name = "Shopping", spent = 500, total = 2000, barColor = Color(0xFFF59E0B))
    )

    MaterialTheme {
        BudgetScreen(previewBudgets = sample, currencyCode = "INR")
    }
 }
