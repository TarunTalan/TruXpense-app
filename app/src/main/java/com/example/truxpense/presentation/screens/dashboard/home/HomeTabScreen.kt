package com.example.truxpense.presentation.screens.dashboard.home

import android.graphics.Color.rgb
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.onboarding.currency.CurrencyViewModel
import java.text.NumberFormat
import java.util.*

// Helper: continuous color interpolation across multiple stops (green->yellow->orange->error)
private fun interpolatedProgressColor(progress: Float, errorColor: Color): Color {
    val p = progress.coerceIn(0f, 1f)
    val green = Color(0xFF4CAF50)
    val yellow = Color(0xFFFFC107)
    val orange = Color(0xFFFFA726)
    val red = errorColor

    return when {
        p <= 0.4f -> lerp(green, yellow, p / 0.4f)
        p <= 0.6f -> lerp(yellow, orange, (p - 0.4f) / 0.2f)
        p <= 0.8f -> lerp(orange, red, (p - 0.6f) / 0.2f)
        else -> red
    }
}

// New helper: split amount into (prefixSymbol, numericPart, suffix) for aligned display
private fun formatAmountParts(amount: Double, currencyCode: String?): Triple<String, String, String> {
    val sign = if (amount < 0) "-" else ""
    val abs = kotlin.math.abs(amount)
    val (value, suffix) = when {
        abs >= 1_000_000 -> Pair(abs / 1_000_000.0, "M")
        abs >= 1_000 -> Pair(abs / 1_000.0, "K")
        else -> Pair(abs, "")
    }

    // numeric formatting:
    // - If abbreviated (suffix != ""), keep 2 decimals (e.g. 1.23K)
    // - If not abbreviated, omit trailing .00 for integer values and trim unnecessary zeroes for fractions
    val numeric = try {
        if (suffix.isNotEmpty()) {
            String.format(Locale.ENGLISH, "%.2f", value)
        } else {
            // value like 540.0 -> "540"
            if (value % 1.0 == 0.0) {
                String.format(Locale.ENGLISH, "%d", value.toLong())
            } else {
                // show up to 2 decimals, strip trailing zeros
                var s = String.format(Locale.ENGLISH, "%.2f", value)
                if (s.contains('.')) {
                    s = s.trimEnd('0').trimEnd('.')
                }
                s
            }
        }
    } catch (_: Exception) {
        // fallback
        String.format(Locale.ENGLISH, "%.2f", value)
    }

    val symbol = try {
        Currency.getInstance(currencyCode ?: "INR").getSymbol(Locale.getDefault())
    } catch (_: Exception) {
        (currencyCode ?: "INR")
    }
    val prefix = if (symbol.isNotBlank()) sign + symbol else sign
    return Triple(prefix, numeric, suffix)
}

@Composable
fun HomeTabScreen(
    username: String?,
    vm: HomeViewModel,
    onLogout: (() -> Unit)?,
    onAddExpense: (() -> Unit)? = null
) {
    // Use ViewModel's permission state so the banner can update VM directly
    val hasSmsPermission by vm.hasSmsPermission.collectAsState()

    // Ensure ViewModel refreshes permission status when entering this screen
    LaunchedEffect(Unit) {
        vm.refreshSmsPermission()
    }

    // Collect expense count from ViewModel
    val expenseCount by vm.expenseCount.collectAsState()
    val monthlySpend by vm.monthlySpend.collectAsState()
    val budgetLimit by vm.budgetLimit.collectAsState()
    val budgetLeft by vm.budgetLeft.collectAsState()

    // Obtain selected currency from CurrencyViewModel (reads persisted pref on init)
    val currencyViewModel: CurrencyViewModel = hiltViewModel()
    val selectedCurrency by currencyViewModel.selectedCurrency.collectAsState()
    val currencyCode = selectedCurrency?.code ?: "INR"

    if (expenseCount == 0) {
        EmptyHomeContent(onAddExpense = onAddExpense, onLogout = onLogout, hasSmsPermission = hasSmsPermission)
        return
    }

    HomeTabScreenContent(
        expenseCount = expenseCount,
        monthlySpend = monthlySpend,
        budgetLimit = budgetLimit,
        budgetLeft = budgetLeft,
        hasSmsPermission = hasSmsPermission,
        onAddExpense = onAddExpense,
        onLogout = onLogout,
        onSmsGranted = { vm.onSmsPermissionResult(true); vm.refreshSmsPermission() },
        username = username,
        currencyCode = currencyCode
    )
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun HomeTabScreenContent(
    expenseCount: Int,
    monthlySpend: Double,
    budgetLimit: Double,
    budgetLeft: Double,
    hasSmsPermission: Boolean,
    onAddExpense: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onSmsGranted: (() -> Unit)? = null,
    username: String?, // Add username parameter
    currencyCode: String? = "INR"
) {
    // Wrap content in a Box so we can overlay a FAB at bottom-end
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // shared banner state
        val bannerVisible = !hasSmsPermission
        val bannerHeight = 72.dp

        // Fixed banner pinned to top when required
        if (bannerVisible) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .zIndex(12f)
                    .background(color = MaterialTheme.colorScheme.background)
                    .padding(bottom = 16.dp)

            ) {
                SmsPermissionBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bannerHeight)
                        .padding(horizontal = 10.dp),
                    onGranted = { onSmsGranted?.invoke() }
                )
            }
        }

        // Scrollable content placed below the banner (uses top padding when banner is visible)
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(
                    start = 10.dp,
                    end = 10.dp,
                    top = if (bannerVisible) bannerHeight + 16.dp else 16.dp,
                    bottom = 16.dp
                ),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Spend this month card (styled to match Figma tokens)
            val currencyFormat = remember(currencyCode) {
                try {
                    val indiaLocale = Locale.Builder().setLanguage("en").setRegion("IN").build()
                    val nf = NumberFormat.getCurrencyInstance(indiaLocale)
                    val code = currencyCode ?: "INR"
                    try {
                        nf.currency = Currency.getInstance(code)
                    } catch (_: Exception) {
                        nf.currency = Currency.getInstance("INR")
                    }
                    nf
                } catch (_: Exception) {
                    NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
                        currency = try {
                            Currency.getInstance("INR")
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
            }

            val formatted = try {
                currencyFormat.format(monthlySpend)
            } catch (_: Exception) {
                "$monthlySpend"
            }


            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Spend this month",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$formatted",
                            style = TextStyle(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    // Placeholder area for sparkline or action
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }

            // Budget card: shows left money out of total budget and a Details button plus progress
            val budgetFormattedLeft = try {
                currencyFormat.format(budgetLeft)
            } catch (_: Exception) {
                "$budgetLeft"
            }
            val budgetFormattedTotal = try {
                currencyFormat.format(budgetLimit)
            } catch (_: Exception) {
                "$budgetLimit"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Budget",
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Button(
                            onClick = { /* TODO: navigate to budget details */ },
                            modifier = Modifier.height(32.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentColor = MaterialTheme.colorScheme.secondary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Details",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "$budgetFormattedLeft of $budgetFormattedTotal",
                        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress indicator: used portion
                    val used = (budgetLimit - budgetLeft).coerceAtLeast(0.0)
                    val progress = if (budgetLimit > 0.0) (used / budgetLimit).toFloat().coerceIn(0f, 1f) else 0f
                    // continuous interpolated color based on progress
                    val progressColor = interpolatedProgressColor(progress, MaterialTheme.colorScheme.error)

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.surface,
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(progress * 100).toInt()}% used",
                        style = MaterialTheme.typography.bodySmall,
                        color = progressColor
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "You spent more on food this week than usual",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            modifier = Modifier.clickable { },
                            text = "Consider setting a weekly limit",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = Color(rgb(59, 120, 194))
                        )
                    }
                    // Placeholder area for sparkline or action
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }

            // Recent transactions section
            // sample transactions - replace with real data from ViewModel when available
            val sampleTx = listOf(
                TransactionItem(
                    id = "1",
                    title = "Grocery at BigMart",
                    category = "Food",
                    amount = 540.0,
                    currencyCode = currencyCode ?: "INR"
                ),
                TransactionItem(
                    id = "2",
                    title = "Coffee",
                    category = "Food",
                    amount = 120.0,
                    currencyCode = currencyCode ?: "INR"
                ),
                TransactionItem(
                    id = "3",
                    title = "Electricity Bill",
                    category = "Bills",
                    amount = 2400.0,
                    currencyCode = currencyCode ?: "INR"
                )
            )


            Column() {

                Text(
                    "Highest spending category this month", style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier,
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    //   spending categories cards with name, budget used and progress bar with % progress

                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Food",
                                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = "2400",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Progress indicator: used portion
                            val used = (budgetLimit - budgetLeft).coerceAtLeast(0.0)
                            val progress =
                                if (budgetLimit > 0.0) (used / budgetLimit).toFloat().coerceIn(0f, 1f) else 0f
                            val progressColor = interpolatedProgressColor(progress, MaterialTheme.colorScheme.error)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = progressColor,
                                trackColor = MaterialTheme.colorScheme.surface,
                                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(progress * 100).toInt()}% used",
                                style = MaterialTheme.typography.bodySmall,
                                color = progressColor
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Clothes",
                                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = "4500",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Progress indicator: used portion
                            val used = (budgetLimit - budgetLeft).coerceAtLeast(0.0)
                            val progress =
                                if (budgetLimit > 0.0) (used / budgetLimit).toFloat().coerceIn(0f, 1f) else 0f
                            val progressColor = interpolatedProgressColor(progress, MaterialTheme.colorScheme.error)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = progressColor,
                                trackColor = MaterialTheme.colorScheme.surface,
                                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(progress * 100).toInt()}% used",
                                style = MaterialTheme.typography.bodySmall,
                                color = progressColor
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Bills",
                                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = "8400",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Progress indicator: used portion
                            val used = (budgetLimit - budgetLeft).coerceAtLeast(0.0)
                            val progress =
                                if (budgetLimit > 0.0) (used / budgetLimit).toFloat().coerceIn(0f, 1f) else 0f
                            val progressColor = interpolatedProgressColor(progress, MaterialTheme.colorScheme.error)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = progressColor,
                                trackColor = MaterialTheme.colorScheme.surface,
                                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(progress * 100).toInt()}% used",
                                style = MaterialTheme.typography.bodySmall,
                                color = progressColor
                            )
                        }
                    }


                }
            }

            RecentTransactionsCard(transactions = sampleTx)

            Spacer(modifier = Modifier.height(20.dp))

        }

        // Floating Add Expense FAB pinned to bottom-end
        FloatingActionButton(
            onClick = { onAddExpense?.invoke() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.background
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add expense")
        }
    }
}

@Composable
fun EmptyHomeContent(
    onAddExpense: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    hasSmsPermission: Boolean,
    onSmsGranted: (() -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val emptyBannerVisible = !hasSmsPermission
        val bannerHeight = 72.dp

        // Fixed banner at top
        if (emptyBannerVisible) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                SmsPermissionBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bannerHeight)
                        .padding(horizontal = 12.dp),
                    onGranted = { onSmsGranted?.invoke() }
                )
            }
        }

        // Scrollable content below the fixed banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = if (emptyBannerVisible) bannerHeight + 12.dp else 16.dp,
                    bottom = 16.dp
                )
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(16.dp))

            Image(
                painter = painterResource(id = R.drawable.illustration_home),
                contentDescription = "Home illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(top = 24.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Start tracking your spending",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "we’ll automatically track expenses from SMS or you can add them manually.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onAddExpense?.invoke() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(text = "+ Add your first expense")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = { onLogout?.invoke() }) {
                    Text(text = "Logout")
                }

            }
        }
    }
}


@Preview
@Composable
fun EmptyHomeContentPreview() {
    EmptyHomeContent(hasSmsPermission = false, onSmsGranted = {})
}

@Preview(showBackground = true)
@Composable
fun HomeTabScreenPreview() {
    Surface {
        HomeTabScreenContent(
            expenseCount = 3,
            monthlySpend = 1234.56,
            budgetLimit = 10000.0,
            budgetLeft = 5123.45,
            hasSmsPermission = false,
            onAddExpense = {},
            onLogout = {},
            onSmsGranted = {},
            username = "Tarun"
        )
    }
}

// Simple transaction model for UI
private data class TransactionItem(
    val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val currencyCode: String = "INR"
)

@Composable
private fun RecentTransactionsCard(
    modifier: Modifier = Modifier,
    transactions: List<TransactionItem>
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent transactions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "View all",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header labels row: use equal weights and spacedBy to give equal width to each column
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Transaction",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Category",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            text = "Amount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))

                // Data rows
                val display = transactions.take(3)
                display.forEachIndexed { index, tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Transaction (left)
                        Text(
                            text = tx.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // Category (middle)
                        Text(
                            text = tx.category,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center

                        )

                        // Amount (right) - right aligned within its weighted column
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                            val (prefix, numeric, suffix) = formatAmountParts(tx.amount, tx.currencyCode)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                                if (prefix.isNotEmpty()) {
                                    Text(
                                        text = prefix,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = numeric,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (suffix.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = suffix,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }

                    if (index < display.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                    }
                }

                if (display.isEmpty()) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "No recent transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RecentTransactionsCardPreview() {
    RecentTransactionsCard(
        transactions = listOf(
            TransactionItem(id = "1", title = "Grocery at BigMart - A very long shop name to test wrapping", category = "Food", amount = 1234.56, currencyCode = "INR"),
            TransactionItem(id = "2", title = "Coffee", category = "Food", amount = 120.0, currencyCode = "INR"),
            TransactionItem(id = "3", title = "Electricity Bill", category = "Bills", amount = 2400000.0, currencyCode = "INR")
        )
    )
}
