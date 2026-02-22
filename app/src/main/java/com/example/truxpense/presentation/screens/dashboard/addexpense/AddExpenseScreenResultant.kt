package com.example.truxpense.presentation.screens.dashboard.addexpense

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.theme.DashboardDimens
import com.example.truxpense.presentation.screens.dashboard.home.HomeTransactionItem
import kotlinx.coroutines.delay

// Theme-level color aliases (semantic names)
private val ColorDivider = Color(0xFF2A2E38)
private val ColorTextPrimary = Color(0xFFF0F2F5)
private val ColorTextSecondary = Color(0xFF7A8599)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreenResultant(
    tx: HomeTransactionItem? = null,
    onDone: () -> Unit = {},
    onSave: () -> Unit = {},
    onCancel: () -> Unit = {},
    onBack: () -> Unit = {},
    onDatePick: () -> Unit = {},
) {
    // If tx is null, fall back to VM state (preview / direct open). If tx is provided we show its details.
    val vm = hiltViewModel<AddExpenseViewModel>()
    val amount by vm.formattedAmount.collectAsState()
    val category by vm.selectedCategory.collectAsState(initial = null)
    val merchant by vm.merchant.collectAsState()
    val account by vm.selectedAccount.collectAsState(initial = null)
    val notes by vm.notes.collectAsState()

    val displayTx = tx ?: HomeTransactionItem(
        id = "",
        title = merchant.ifBlank { "Expense" },
        category = category ?: "Other",
        amount = try { amount.filter { it.isDigit() || it=='.' }.toDouble() } catch (_: Exception) { 0.0 },
        currencyCode = "INR",
    )

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(tx) {
        if (tx != null) {
            snackbarHostState.showSnackbar("Expense Added")
            delay(900)
            onDone()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Expense",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.back_icon),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->

        Column(
            modifier = Modifier.fillMaxSize().padding(
                    start = DashboardDimens.screenPaddingH,
                    end = DashboardDimens.screenPaddingH,
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                ),
        ) {

            // Hero amount display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(
                        top = DashboardDimens.heroZonePadTop,
                        bottom = DashboardDimens.heroZonePadBottom,
                    ),
            ) {
                Text(
                    text = "Amount",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = DashboardDimens.textLg,
                    letterSpacing = 0.4.sp,
                )
                Spacer(Modifier.height(DashboardDimens.spaceXs))
                Text(
                    text = amount,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = DashboardDimens.textHero,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                )
            }

            // Details card
            Column(
                modifier = Modifier.clip(RoundedCornerShape(DashboardDimens.spaceXl))   // 16dp
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                DetailsRow(label = "Category", value = category)
                RowDivider()
                DetailsRow(label = "Merchant", value = merchant)
                RowDivider()
                DetailsRow(label = "Account", value = account)
                RowDivider()
                DetailsRow(
                    label = "Date",
                    value = null,
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.calender),
                            contentDescription = "Pick date",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(DashboardDimens.iconMd).clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) { onDatePick() },
                        )
                    },
                )
            }

            Spacer(Modifier.height(DashboardDimens.spaceLg))

            // Show added transaction summary (either vm-based or passed tx)
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(DashboardDimens.spaceXl))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = DashboardDimens.detailCardPadH, vertical = DashboardDimens.detailCardPadV)
            ) {
                Text("Added transaction", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(displayTx.title)
                Spacer(Modifier.height(4.dp))
                Text("Category: ${displayTx.category}")
                Spacer(Modifier.height(4.dp))
                Text("Amount: ${displayTx.amount} ${displayTx.currencyCode}")
            }

            Spacer(Modifier.weight(1f))

            // Done button to complete flow
            Column(verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMdL)) {
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(DashboardDimens.buttonHeight)) {
                    Text("Done")
                }
            }
        }
    }
}

// Private components

@Composable
private fun DetailsRow(
    label: String,
    value: String?,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) {}.padding(
                horizontal = DashboardDimens.detailCardPadH,
                vertical = DashboardDimens.rowPadV,
            ),
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.secondary,
            fontSize = DashboardDimens.textLg,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = DashboardDimens.textLg,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
            )
        }
        trailingContent?.invoke()
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = DashboardDimens.detailCardPadH),
        thickness = DashboardDimens.dividerThin,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
    )
}

// Preview

@Preview(showBackground = true)
@Composable
fun AddExpenseResultantPreview() {
    MaterialTheme { AddExpenseScreenResultant() }
}