package com.example.truxpense.presentation.screens.dashboard.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.theme.DashboardDimens


// ─── Screen entry point ───────────────────────────────────────────────────────

@Composable
fun TransactionDetailScreen(
    transactionId: String,
    onBack: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDeleted: () -> Unit = {},
    vm: TransactionDetailViewModel = hiltViewModel(),
) {
    val detail by vm.detail.collectAsState()
    val notes by vm.notes.collectAsState()
    val notesExpanded by vm.notesExpanded.collectAsState()
    val deleteComplete by vm.deleteComplete.collectAsState()

    LaunchedEffect(transactionId) {
        vm.loadTransaction(transactionId)
    }

    // Navigate away once deletion is confirmed
    LaunchedEffect(deleteComplete) {
        if (deleteComplete) onDeleted()
    }

    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            onConfirm = {
                showDeleteDialog = false
                vm.deleteTransaction()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    TransactionDetailContent(
        detail = detail,
        notes = notes,
        notesExpanded = notesExpanded,
        onBack = onBack,
        onEdit = onEdit,
        onDeleteRequest = { showDeleteDialog = true },
        onNotesChange = vm::setNotes,
        onToggleNotes = vm::toggleNotes,
    )
}


// ─── Parameterised content (preview-friendly) ─────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailContent(
    detail: TransactionDetail?,
    notes: String,
    notesExpanded: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
    onNotesChange: (String) -> Unit,
    onToggleNotes: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DetailTopBar(onBack = onBack)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = DashboardDimens.screenPaddingH,
                    vertical = DashboardDimens.spaceXxl,
                ),
            verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg),
        ) {

            // ── Hero: amount + type + source ──────────────────────────────────
            HeroSection(detail = detail)

            // ── Transaction details card ──────────────────────────────────────
            DetailsCard(detail = detail)

            // ── Notes card ────────────────────────────────────────────────────
            NotesCard(
                notes = notes,
                expanded = notesExpanded,
                onChange = onNotesChange,
                onToggle = onToggleNotes,
            )

            Spacer(Modifier.height(DashboardDimens.spaceMd))

            // ── Action buttons ────────────────────────────────────────────────
            EditButton(onClick = onEdit)
            Spacer(Modifier.height(DashboardDimens.spaceMd))
            DeleteButton(onClick = onDeleteRequest)
            Spacer(Modifier.height(DashboardDimens.spaceXxl))
        }
    }
}


// ─── Top bar ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
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
        actions = {
            // Filter icon — navigates back with a filter applied (future feature placeholder)
            Row(
                modifier = Modifier
                    .padding(end = DashboardDimens.spaceMd)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onBack,  // navigates back to filtered list
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceXs),
            ) {
                Icon(
                    painter = painterResource(R.drawable.filter),
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(DashboardDimens.iconMd),
                )
                Text(
                    text = "Filter",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}


// ─── Hero section ─────────────────────────────────────────────────────────────

@Composable
private fun HeroSection(detail: TransactionDetail?) {
    val amountText = if (detail != null) {
        val abs = kotlin.math.abs(detail.amount)
        val sign = if (detail.amount < 0) "−" else "+"
        "$sign₹${"%.0f".format(abs)}"
    } else "−₹—"

    val isExpense = detail == null || detail.amount < 0

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceXs),
    ) {
        Text(
            text = amountText,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            ),
        )
        Text(
            text = detail?.type ?: "Expense",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = detail?.source ?: "Detected from SMS",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


// ─── Details card ─────────────────────────────────────────────────────────────

@Composable
private fun DetailsCard(detail: TransactionDetail?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Card header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = DashboardDimens.screenPaddingH,
                        vertical = DashboardDimens.spaceLg,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
            ) {
                Icon(
                    painter = painterResource(R.drawable.transaction_detail),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DashboardDimens.iconMd),
                )
                Text(
                    text = "Transaction details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = DashboardDimens.dividerThin,
            )

            // Detail rows
            DetailRow(label = "Merchant", value = detail?.merchant ?: "—")
            DetailRow(label = "Category", value = detail?.category ?: "—")
            DetailRow(label = "Account", value = detail?.account ?: "—")
            DetailRow(label = "Date", value = detail?.date ?: "—")
            DetailRow(label = "Time", value = detail?.time ?: "—")
            if (!detail?.paymentMethod.isNullOrBlank()) {
                DetailRow(label = "Payment", value = detail!!.paymentMethod)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DashboardDimens.detailRowHeight)
            .padding(horizontal = DashboardDimens.screenPaddingH),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = DashboardDimens.screenPaddingH),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        thickness = DashboardDimens.dividerThin,
    )
}


// ─── Notes card ───────────────────────────────────────────────────────────────

@Composable
private fun NotesCard(
    notes: String,
    expanded: Boolean,
    onChange: (String) -> Unit,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Header row — always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onToggle,
                    )
                    .padding(
                        horizontal = DashboardDimens.screenPaddingH,
                        vertical = DashboardDimens.spaceLg,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add_notes_icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(DashboardDimens.iconMd),
                    )
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                // "+" when collapsed; "−" when expanded
                Box(
                    modifier = Modifier
                        .size(DashboardDimens.iconButtonSm)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (expanded) "−" else "+",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Expandable notes input
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = DashboardDimens.screenPaddingH,
                            end = DashboardDimens.screenPaddingH,
                            bottom = DashboardDimens.spaceLg,
                        ),
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = DashboardDimens.dividerThin,
                    )
                    Spacer(Modifier.height(DashboardDimens.spaceMd))
                    BasicTextField(
                        value = notes,
                        onValueChange = onChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(DashboardDimens.cornerChip))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(DashboardDimens.spaceLg)
                            .defaultMinSize(minHeight = DashboardDimens.inputMinHeight),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                        ),
                        maxLines = 5,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            if (notes.isEmpty()) {
                                Text(
                                    text = "Add a note about this transaction…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        },
                    )
                }
            }
        }
    }
}


// ─── Action buttons ───────────────────────────────────────────────────────────

@Composable
private fun EditButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(DashboardDimens.buttonHeight),
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp),
    ) {
        Text(
            text = "Edit transaction",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DeleteButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(DashboardDimens.buttonHeight),
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            )
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
    ) {
        Text(
            text = "Delete transaction",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
        )
    }
}


// ─── Delete confirmation dialog ────────────────────────────────────────────────

@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Delete transaction?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        },
        text = {
            Text(
                text = "This action cannot be undone. The transaction will be permanently removed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                elevation = ButtonDefaults.buttonElevation(0.dp),
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.onError)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
    )
}


// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 390, heightDp = 844, backgroundColor = 0xFFFFFFFF)
@Composable
fun TransactionDetailPreview() {
    val stub = TransactionDetail(
        id = "preview-1",
        merchant = "Swiggy",
        category = "Food",
        account = "HDFC Bank",
        date = "12 Feb 2026",
        time = "8:45 PM",
        amount = -450.0,
        type = "Expense",
        source = "Detected from SMS",
        paymentMethod = "UPI",
        notes = "",
    )
    MaterialTheme {
        TransactionDetailContent(
            detail = stub,
            notes = "",
            notesExpanded = false,
            onBack = {},
            onEdit = {},
            onDeleteRequest = {},
            onNotesChange = {},
            onToggleNotes = {},
        )
    }
}