package com.example.truxpense.presentation.screens.dashboard.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.utils.amountAbbreviationHint
import com.example.truxpense.presentation.utils.amountDisplayText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AmountInputCard(
    rawAmount: String,
    onRawChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    question: String = "How much did you spend?",
    currencyCode: String = "INR",
    currencySymbol: String = "₹",
    onFocused: (() -> Unit)? = null,   // ← called when the input gains focus
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = question,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            AmountInputZone(
                rawAmount = rawAmount,
                onRawChange = onRawChange,
                currencyCode = currencyCode,
                currencySymbol = currencySymbol,
                onFocused = onFocused,
            )
        }
    }
}

// ── Internal input zone (shared logic) ───────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AmountInputZone(
    rawAmount: String,
    onRawChange: (String) -> Unit,
    currencyCode: String = "INR",
    currencySymbol: String = "₹",
    onFocused: (() -> Unit)? = null,   // ← forwarded from AmountInputCard
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val textMeasurer = rememberTextMeasurer()
    val scope = rememberCoroutineScope()
    val bringIntoView = remember { BringIntoViewRequester() }

    val displayStyle = MaterialTheme.typography.displaySmall.copy(
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        letterSpacing = 1.12.sp,
    )

    val displayText = amountDisplayText(rawAmount)

    val amountPx  = textMeasurer.measure(AnnotatedString(displayText), style = displayStyle).size.width
    val symbolPx  = textMeasurer.measure(AnnotatedString(currencySymbol), style = displayStyle).size.width
    val fieldWidth = with(LocalDensity.current) { (amountPx + symbolPx).toDp() } + 8.dp

    val hint = amountAbbreviationHint(rawAmount, currencyCode)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoView),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { focusRequester.requestFocus(); keyboardController?.show() },
            contentAlignment = Alignment.Center,
        ) {
            BasicTextField(
                value = rawAmount,
                onValueChange = { typed -> onRawChange(typed.filter { it.isDigit() }) },
                singleLine = true,
                modifier = Modifier
                    .widthIn(min = 48.dp)
                    .width(fieldWidth)
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            // Built-in bring-into-view for the internal bringIntoView requester
                            scope.launch { delay(320); bringIntoView.bringIntoView() }
                            // Notify caller so it can scroll its own container
                            onFocused?.invoke()
                        }
                    },
                textStyle = displayStyle.copy(color = Color.Transparent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                decorationBox = { inner ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = currencySymbol, style = displayStyle)
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = displayText,
                                style = displayStyle.copy(
                                    color = if (rawAmount.isEmpty())
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.onBackground,
                                ),
                            )
                            inner()
                        }
                    }
                },
            )
        }

        // Abbreviation hint — shown only for ≥ 1L (INR) or ≥ 1M (others)
        AnimatedVisibility(
            visible = hint != null,
            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
            exit  = fadeOut(tween(150)) + shrinkVertically(tween(150)),
        ) {
            Text(
                text = hint ?: "",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun AmountInputCardEmptyPreview() {
    MaterialTheme {
        AmountInputCard(
            rawAmount = "",
            onRawChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, name = "With amount + hint")
@Composable
private fun AmountInputCardFilledPreview() {
    MaterialTheme {
        AmountInputCard(
            rawAmount = "1500000",
            onRawChange = {},
            question = "Monthly budget limit",
            modifier = Modifier.padding(16.dp),
        )
    }
}
