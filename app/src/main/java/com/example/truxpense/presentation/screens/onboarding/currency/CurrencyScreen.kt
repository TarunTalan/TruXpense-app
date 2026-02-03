package com.example.truxpense.presentation.screens.onboarding.currency

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.auth.components.AuthButton
import com.example.truxpense.presentation.screens.auth.components.AuthTextField
import com.example.truxpense.presentation.utils.InputValidators
import com.example.truxpense.presentation.utils.clearFocusOnTap
import java.text.Normalizer
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyScreen(
    viewModel: CurrencyViewModel = hiltViewModel(),
    onContinue: () -> Unit = {},
    onSkip: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val available = viewModel.available
    val selected by viewModel.selectedCurrency.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    // Clear in-memory selection when the screen leaves composition (navigated away or closed)
    DisposableEffect(viewModel) {
        onDispose {
            // keep persisted preference intact; just reset in-memory selection
            viewModel.clearSelection()
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val backTint = if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onBackground
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(id = R.drawable.back_icon),
                        contentDescription = "Back",
                        tint = backTint
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    ) { innerPadding ->
        CurrencyScreenInner(
            modifier = Modifier.padding(innerPadding),
            available = available,
            selected = selected,
            onSelect = { item -> viewModel.selectCurrency(item) },
            onContinue = onContinue, onSkip = onSkip,
            isSaving = isSaving,
            onClearSelection = { viewModel.clearSelection() }
        )
    }
}

@Composable
private fun CurrencyScreenInner(
    modifier: Modifier = Modifier,
    available: List<CurrencyItem>,
    selected: CurrencyItem?,
    onSelect: (CurrencyItem) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    isSaving: Boolean = false,
    onClearSelection: () -> Unit,
    initialQueryText: String = "",
    initialExpanded: Boolean = false
) {
    // Localized strings used inside lambdas must be resolved here in composable scope
    val requiredMsg = stringResource(id = R.string.select_currency_required)

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var query by remember { mutableStateOf(initialQueryText) }
    var expanded by remember { mutableStateOf(initialExpanded) }
    var pressedCode by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = expanded) {
        expanded = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun String.normalize(): String = Normalizer.normalize(this, Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.getDefault())

    val filtered = remember(query, available) {
        val q = query.trim()
        if (q.isEmpty()) available
        else {
            val nQ = q.normalize()
            available.filter { item ->
                item.code.normalize().contains(nQ) ||
                        item.symbol.normalize().contains(nQ) ||
                        item.name.normalize().contains(nQ)
            }
        }
    }

    // Enable Continue only when a currency is explicitly selected
    val continueEnabled = remember(selected) { selected != null }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clearFocusOnTap(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Column(
            modifier = Modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.intro_illustration_1),
                contentDescription = stringResource(id = R.string.currency_illustration_cd),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(id = R.string.select_currency),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(32.dp))

            AuthTextField(
                bgColor = MaterialTheme.colorScheme.background,
                label = null,
                placeholder = stringResource(id = R.string.search_currencies_placeholder),
                error = errorMessage,
                value = query,
                onValueChange = { value ->
                    // Use centralized filter to remove digits/control chars and cap length
                    val sanitized = InputValidators.filterCurrencyInput(value)
                    // user typing should clear any previous selection so only explicit selection allows continue
                    onClearSelection()
                    query = sanitized
                    expanded = true
                    // clear error when user types or searches
                    if (!errorMessage.isNullOrEmpty()) errorMessage = null
                },
                contentPadding = 12,
                trailing = {
                    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                painter = painterResource(id = R.drawable.drop_down_icon),
                                contentDescription = if (expanded) "Close dropdown" else "Open dropdown",
                                modifier = Modifier.rotate(rotation)
                            )
                        }
                    }
                }
            )
            Spacer(Modifier.height(8.dp))

            // Full-height dropdown: covers entire screen area below the input, buttons are hidden when expanded
            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium
                        )
                ) {
                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.medium),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(id = R.string.no_currencies_found))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium)
                                .padding(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            // Compute preferred codes: device locale currency and USD
                            val deviceCurrencyCode = try {
                                Currency.getInstance(Locale.getDefault()).currencyCode
                            } catch (_: Exception) {
                                null
                            }
                            val preferredCodes = listOfNotNull(deviceCurrencyCode, "USD").distinct()

                            itemsIndexed(filtered) { index, item ->
                                val isPressed = pressedCode == item.code
                                val isSelected = selected?.code == item.code

                                // Selected state should visually override pressed
                                val containerColor = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isPressed -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                }
                                ListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable(enabled = true, onClickLabel = null, role = null) {
                                            pressedCode = item.code
                                            onSelect(item)
                                            // show selected name in the input; selection state comes from viewModel
                                            query = item.name
                                            expanded = false
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        },
                                    colors = ListItemDefaults.colors(containerColor = containerColor),
                                    headlineContent = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.name,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                text = item.symbol,
                                                color = MaterialTheme.colorScheme.onBackground,
                                            )
                                        }
                                    }
                                )

                                // If this is the last preferred item in the filtered list, render a subtle divider below it
                                val isPreferred = preferredCodes.contains(item.code)
                                val hasLaterPreferred = filtered.drop(index + 1).any { preferredCodes.contains(it.code) }
                                if (isPreferred && !hasLaterPreferred) {
                                    HorizontalDivider(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        thickness = 1.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // When dropdown is open, don't render footer buttons so the dropdown visually covers all space below
        if (!expanded) {
            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AuthButton(
                    onClick = {
                        // Validate that a currency is selected before continuing
                        if (selected == null) {
                            errorMessage = requiredMsg
                        } else {
                            onContinue()
                        }
                    },
                    text = stringResource(id = R.string.continue_text),
                    enabled = continueEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    isLoading = isSaving
                )

                TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = R.string.skip_text), color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrencyScreenPreview() {
    val sample: List<CurrencyItem> = listOf(
        CurrencyItem("USD", "\$", "United States Dollar"),
        CurrencyItem("EUR", "€", "Euro"),
        CurrencyItem("INR", "₹", "Indian Rupee")
    )

    CurrencyScreenInner(
        available = sample,
        selected = sample.firstOrNull(),
        onSelect = {},
        onContinue = {},
        onSkip = {},
        onClearSelection = {},
        initialQueryText = "",
        initialExpanded = false
    )
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Currency - Dark - Open")
@Composable
private fun CurrencyScreenDarkPreview() {
    val sample: List<CurrencyItem> = listOf(
        CurrencyItem("USD", "\$", "United States Dollar"),
        CurrencyItem("EUR", "€", "Euro"),
        CurrencyItem("INR", "₹", "Indian Rupee")
    )

    CurrencyScreenInner(
        available = sample,
        selected = sample.firstOrNull(),
        onSelect = {},
        onContinue = {},
        onSkip = {},
        onClearSelection = {},
        initialQueryText = "",
        initialExpanded = true
    )
}
