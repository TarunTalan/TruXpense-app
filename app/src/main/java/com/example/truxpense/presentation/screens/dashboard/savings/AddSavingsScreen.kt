package com.example.truxpense.presentation.screens.dashboard.savings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.*
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.theme.TruXpenseTheme
import com.example.truxpense.presentation.utils.clearFocusOnTap

// ══════════════════════════════════════════════════════════════════════════════
// ViewModel-wired entry point
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AddSavingsScreen(
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {},
    vm: AddSavingsViewModel = hiltViewModel(),
) {
    val rawAmount by vm.rawAmount.collectAsStateWithLifecycle()
    val source by vm.source.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val isFormValid by vm.isFormValid.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.saved.collect { onSaved() } }

    AddSavingsScreenContent(
        rawAmount = rawAmount,
        source = source,
        notes = notes,
        isFormValid = isFormValid,
        onRawChange = vm::setRawAmount,
        onQuickAdd = vm::addAmount,
        onSourceChange = vm::setSource,
        onNotesChange = vm::setNotes,
        onSave = vm::saveSavingsEntry,
        onBack = onBack,
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// Stateless screen content
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSavingsScreenContent(
    rawAmount: String,
    source: String,
    notes: String,
    isFormValid: Boolean,
    onRawChange: (String) -> Unit,
    onQuickAdd: (Long) -> Unit,
    onSourceChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible

    LaunchedEffect(isImeVisible) {
        if (!isImeVisible) focusManager.clearFocus(force = false)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                headerTitle = "Add savings",
                showBack = true,
                onBack = onBack,
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(
                            horizontal = DashboardDimens.screenPaddingH,
                            vertical = 12.dp,
                        ),
                ) {
                    SaveButton(
                        enabled = isFormValid,
                        onClick = {
                            focusManager.clearFocus()
                            onSave()
                        },
                        label = "Save savings",
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = DashboardDimens.screenPaddingH)
                .verticalScroll(rememberScrollState())
                .clearFocusOnTap(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(2.dp))

            // ── Amount input ──────────────────────────────────────────────────
            AmountInputCard(
                rawAmount = rawAmount,
                onRawChange = onRawChange,
                question = "How much do you want to save?",
            )

            // ── Quick-add chips ───────────────────────────────────────────────
            QuickAddAmountRow(
                amounts = listOf(500L, 1000L, 1500L, 2000L),
                onAdd = onQuickAdd,
            )

            // ── Source dropdown (replaces free-text + checklist) ─────────────
            PaymentMethodField(
                selectedAccount = source.takeIf { it.isNotBlank() },
                onSelect = { onSourceChange(it) },
                options = SAVINGS_SOURCES,
                fieldLabel = "Source",
            )

            // ── Notes ─────────────────────────────────────────────────────────
            NotesCard(notes = notes, onChange = onNotesChange)

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════=
// Quick-add chip row
// ═════════════════════════════════════════════════════════════════════════════=

@Composable
private fun QuickAddAmountRow(
    amounts: List<Long>,
    onAdd: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        amounts.forEach { delta ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(20.dp),
                    )
                    .clickable { onAdd(delta) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+${"%,d".format(delta)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Preview
// ═════════════════════════════════════════════════════════════════════════════=

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Add Savings – empty",uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Composable
private fun AddSavingsScreenEmptyPreview() {
    TruXpenseTheme {
        AddSavingsScreenContent(
            rawAmount = "",
            source = "",
            notes = "",
            isFormValid = false,
            onRawChange = {},
            onQuickAdd = {},
            onSourceChange = {},
            onNotesChange = {},
            onSave = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Add Savings – filled",uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Composable
private fun AddSavingsScreenFilledPreview() {
    TruXpenseTheme {
        AddSavingsScreenContent(
            rawAmount = "5000",
            source = "Bank Transfer",
            notes = "Monthly salary deposit",
            isFormValid = true,
            onRawChange = {},
            onQuickAdd = {},
            onSourceChange = {},
            onNotesChange = {},
            onSave = {},
            onBack = {},
        )
    }
}

