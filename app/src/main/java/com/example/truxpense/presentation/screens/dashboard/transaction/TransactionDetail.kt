package com.example.truxpense.presentation.screens.dashboard.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.utils.clearFocusOnTap


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
        onBack = onBack,
        onEdit = onEdit,
        onDeleteRequest = { showDeleteDialog = true },
        onNotesChange = vm::setNotes,
        onSaveNotes = vm::saveNotes,
        onToggleNotes = vm::toggleNotes,
    )
}


// ─── Parameterised content (preview-friendly) ─────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailContent(
    detail: TransactionDetail?,
    notes: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
    onNotesChange: (String) -> Unit,
    onSaveNotes: () -> Unit = {},
    onToggleNotes: () -> Unit = {},
) {
    // 3-dot menu state
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                headerTitle = "Transaction Details",
                showBack = true,
                onBack = onBack,
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(text = {
                                Text(
                                    text = "Edit",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }, onClick = {
                                showMenu = false
                                onEdit()
                            }, leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.edit),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            })
                            DropdownMenuItem(text = {
                                Text(
                                    text = "Delete",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }, onClick = {
                                showMenu = false
                                onDeleteRequest()
                            }, leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.delete),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            })
                        }
                    }
                }
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DashboardDimens.screenPaddingH)
                .padding(top = DashboardDimens.spaceMd, bottom = DashboardDimens.spaceXxl)
                .clearFocusOnTap(),
            verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg),
        ) {

            // ── Hero: amount + type + source ──────────────────────────────────
            HeroSection(detail = detail)

            // ── Transaction details card ──────────────────────────────────────
            DetailsCard(detail = detail)

            // ── Notes card ────────────────────────────────────────────────────
            NotesCard(
                notes = notes,
                onChange = onNotesChange,
                onSave = onSaveNotes,
            )

            Spacer(Modifier.height(DashboardDimens.spaceXxl))
        }
    }
}


// ─── Hero section (unchanged) ─────────────────────────────────────────────────

@Composable
private fun HeroSection(detail: TransactionDetail?) {
    val amountText = if (detail != null) {
        val abs = kotlin.math.abs(detail.amount)
        val sign = "−"
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
            text = "Expense",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = if (isExpense) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
        Text(
            text = detail?.source ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


// ─── Details card ─────────────────────────────────────────────────────────────

@Composable
private fun DetailsCard(detail: TransactionDetail?) {
    Card(
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Card header ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().height(DashboardDimens.inputMinHeight)
                    .padding(horizontal = DashboardDimens.screenPaddingH),
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
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                thickness = 2.dp,
            )
            // ── Rows ──────────────────────────────────────────────────────────
            DetailRow(label = "Merchant", value = detail?.merchant ?: "—")
            DetailRow(label = "Category", value = detail?.category ?: "—")
            DetailRow(label = "Account", value = detail?.account ?: "—")
            DetailRow(label = "Date", value = detail?.date ?: "—")
            DetailRow(label = "Time", value = detail?.time ?: "—")
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(DashboardDimens.inputMinHeight)
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
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH),
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = DashboardDimens.dividerThin,
    )
}


// ─── Notes card ───────────────────────────────────────────────────────────────

private const val NOTES_MAX_CHARS = 100

@Composable
private fun NotesCard(
    notes: String,
    onChange: (String) -> Unit,
    onSave: () -> Unit = {},
) {
    var editMode by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = DashboardDimens.screenPaddingH,
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
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                when {
                    editMode -> TextButton(onClick = { editMode = false; onSave() }) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    notes.isNotEmpty() -> TextButton(onClick = { editMode = true }) {
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    else -> TextButton(onClick = { editMode = true }) {
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = DashboardDimens.dividerThin,
                modifier = Modifier.padding(horizontal = DashboardDimens.screenPaddingH),
            )

            Spacer(Modifier.height(DashboardDimens.spaceMd))

            // ── Body ──────────────────────────────────────────────────────────
            when {
                editMode -> {
                    val focusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) {
                        try { focusRequester.requestFocus() } catch (_: Exception) {}
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DashboardDimens.screenPaddingH),
                    ) {
                        BasicTextField(
                            value = notes,
                            onValueChange = { if (it.length <= NOTES_MAX_CHARS) onChange(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(DashboardDimens.cornerChip))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(
                                    horizontal = DashboardDimens.spaceLg,
                                    vertical = DashboardDimens.spaceMdL,
                                )
                                .defaultMinSize(minHeight = DashboardDimens.inputMinHeight)
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                            ),
                            maxLines = 3,
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
                        // Character counter
                        Text(
                            text = "${notes.length}/$NOTES_MAX_CHARS",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (notes.length >= NOTES_MAX_CHARS)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = DashboardDimens.spaceXxs),
                        )
                    }
                }

                notes.isNotEmpty() -> {
                    // Read-only — max 3 lines, ellipsis if overflow
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = DashboardDimens.screenPaddingH + DashboardDimens.spaceLg,
                                vertical = DashboardDimens.spaceMdL,
                            ),
                    )
                }

                else -> {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editMode = true }
                            .padding(
                                horizontal = DashboardDimens.screenPaddingH,
                                vertical = DashboardDimens.spaceLg,
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceXs),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.add_notes_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(32.dp),
                        )
                        Text(
                            text = "No notes yet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        Text(
                            text = "Tap to add a note about this transaction",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(DashboardDimens.spaceLg))
        }
    }
}


// ─── Delete confirmation dialog (unchanged) ───────────────────────────────────

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
        notes = "",
    )
    MaterialTheme {
        TransactionDetailContent(
            detail = stub,
            notes = "",
            onBack = {},
            onEdit = {},
            onDeleteRequest = {},
            onNotesChange = {},
        )
    }
}