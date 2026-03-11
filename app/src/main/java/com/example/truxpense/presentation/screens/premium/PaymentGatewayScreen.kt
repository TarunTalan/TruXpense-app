package com.example.truxpense.presentation.screens.premium

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.GradientCard
import com.example.truxpense.presentation.screens.premium.model.CardFormState
import com.example.truxpense.presentation.screens.premium.model.FormErrors
import com.example.truxpense.presentation.screens.premium.model.PaymentMethod
import com.example.truxpense.presentation.screens.premium.model.PlanType
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.theme.TruXpenseTheme

// ─── Screen entry point ───────────────────────────────────────────────────────

@Composable
fun PaymentGatewayScreen(
    selectedPlan: PlanType = PlanType.ANNUAL,
    onNavigateBack: () -> Unit = {},
    onPaymentSuccess: () -> Unit = {},
    viewModel: PaymentGatewayViewModel = viewModel(
        factory = PaymentGatewayViewModelFactory(selectedPlan),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PaymentEvent.PaymentSuccess -> onPaymentSuccess()
            }
        }
    }

    PaymentGatewayContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onPlanSelected = viewModel::onPlanSelected,
        onMethodSelected = viewModel::onMethodSelected,
        onCardNumberChanged = viewModel::onCardNumberChanged,
        onCardExpiryChanged = viewModel::onCardExpiryChanged,
        onCardCvvChanged = viewModel::onCardCvvChanged,
        onCardNameChanged = viewModel::onCardNameChanged,
        onUpiIdChanged = viewModel::onUpiIdChanged,
        onBankSelected = viewModel::onBankSelected,
        onPayTapped = viewModel::onPayTapped,
    )
}

// ─── Stateless content ────────────────────────────────────────────────────────

@Composable
private fun PaymentGatewayContent(
    uiState: PaymentGatewayUiState,
    onNavigateBack: () -> Unit,
    onPlanSelected: (PlanType) -> Unit,
    onMethodSelected: (PaymentMethod) -> Unit,
    onCardNumberChanged: (String) -> Unit,
    onCardExpiryChanged: (String) -> Unit,
    onCardCvvChanged: (String) -> Unit,
    onCardNameChanged: (String) -> Unit,
    onUpiIdChanged: (String) -> Unit,
    onBankSelected: (String) -> Unit,
    onPayTapped: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    // Handle system Back button to navigate within the premium flow back to Paywall
    BackHandler { onNavigateBack() }

    val primary = MaterialTheme.colorScheme.primary
    val headerBrush = Brush.linearGradient(
        colors = listOf(primary, primary.copy(alpha = 0.78f)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(bottom = DashboardDimens.buttonHeight + DashboardDimens.spaceXxl + DashboardDimens.spaceXl),
            ) {
                PaymentHeader(
                    brush = headerBrush,
                    plan = uiState.selectedPlan,
                    onNavigateBack = onNavigateBack,
                )

                Spacer(modifier = Modifier.height(DashboardDimens.spaceXl))

                Column(
                    modifier = Modifier.padding(horizontal = DashboardDimens.screenPaddingH),
                    verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceLg),
                ) {
                    // ── Plan picker ──────────────────────────────────────────
                    SectionCard(title = "Choose Plan") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
                        ) {
                            PlanType.entries.forEach { plan ->
                                PlanChip(
                                    plan = plan,
                                    isSelected = uiState.selectedPlan == plan,
                                    modifier = Modifier.weight(1f),
                                    onSelect = { onPlanSelected(plan) },
                                )
                            }
                        }
                    }

                    // ── Payment method tabs ──────────────────────────────────
                    SectionCard(title = "Payment Method") {
                        PaymentMethodTabs(
                            selected = uiState.selectedMethod,
                            onSelect = onMethodSelected,
                        )
                    }

                    // ── Dynamic payment form ─────────────────────────────────
                    AnimatedContent(
                        targetState = uiState.selectedMethod,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                        label = "payment_form",
                    ) { method ->
                        when (method) {
                            PaymentMethod.CARD -> CardForm(
                                card = uiState.card,
                                errors = uiState.errors,
                                onCardNumberChanged = onCardNumberChanged,
                                onCardExpiryChanged = onCardExpiryChanged,
                                onCardCvvChanged = onCardCvvChanged,
                                onCardNameChanged = onCardNameChanged,
                                onMoveFocusDown = { focusManager.moveFocus(FocusDirection.Down) },
                                onClearFocus = { focusManager.clearFocus() },
                            )

                            PaymentMethod.UPI -> UpiForm(
                                upiId = uiState.upiId,
                                error = uiState.errors.upiId,
                                onUpiIdChanged = onUpiIdChanged,
                                onClearFocus = { focusManager.clearFocus() },
                            )

                            PaymentMethod.NETBANKING -> NetBankingForm(
                                banks = uiState.availableBanks,
                                selectedBank = uiState.selectedBank,
                                error = uiState.errors.bank,
                                onBankSelected = onBankSelected,
                            )
                        }
                    }

                    SecurityBadge()
                }
            }

            // ── Sticky CTA ───────────────────────────────────────────────────
            PaymentCtaBar(
                plan = uiState.selectedPlan,
                isProcessing = uiState.isProcessing,
                onPayTapped = onPayTapped,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun PaymentHeader(
    brush: Brush,
    plan: PlanType,
    onNavigateBack: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().background(brush = brush),
    ) {
        Box(
            modifier = Modifier.size(130.dp).align(Alignment.TopEnd)
                .background(Color.White.copy(alpha = 0.07f), CircleShape),
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(
                    horizontal = DashboardDimens.spaceMd,
                    vertical = DashboardDimens.spaceMd,
                ),
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        painterResource(R.drawable.back_icon),
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(
                    text = "Complete Payment",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.size(DashboardDimens.iconButtonMd))
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(
                    start = DashboardDimens.screenPaddingH,
                    end = DashboardDimens.screenPaddingH,
                    bottom = DashboardDimens.spaceXxl,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(DashboardDimens.cornerFilterChip))
                        .background(Color.White.copy(alpha = 0.18f)).padding(
                            horizontal = DashboardDimens.chipPaddingH,
                            vertical = DashboardDimens.spaceXs,
                        ),
                ) {
                    Icon(
                        painterResource(R.drawable.diamond),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(DashboardDimens.iconSm),
                    )
                    Spacer(modifier = Modifier.width(DashboardDimens.spaceXs))
                    Text(
                        text = "PRO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }

                Spacer(modifier = Modifier.height(DashboardDimens.spaceMd))

                // plan.period comes from the model — no conditional here
                Text(
                    text = "7-day free trial, then ${plan.price}/${plan.period}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(0.9f),
                )
            }
        }
    }
}

// ─── Plan chip ────────────────────────────────────────────────────────────────

@Composable
private fun PlanChip(
    plan: PlanType,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
) {
    Box(
        modifier = modifier.clip(MaterialTheme.shapes.medium).background(
            if (isSelected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.background,
        ).border(
            width = if (isSelected) DashboardDimens.borderStroke * 2 else DashboardDimens.borderStroke,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            shape = MaterialTheme.shapes.medium,
        ).clickable(onClick = onSelect).padding(
            horizontal = DashboardDimens.spaceMd,
            vertical = DashboardDimens.spaceLg,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // plan.badgeLabel is null when no badge should appear — no enum check needed
            if (plan.badgeLabel != null) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(DashboardDimens.cornerBadge * 2f))
                        .background(MaterialTheme.colorScheme.primary).padding(
                            horizontal = DashboardDimens.spaceMd,
                            vertical = DashboardDimens.spaceXxs,
                        ),
                ) {
                    Text(
                        text = plan.badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Spacer(modifier = Modifier.height(DashboardDimens.spaceXs))
            } else {
                // Reserve same vertical space so both chips are equal height
                Spacer(modifier = Modifier.height(DashboardDimens.spaceLg + DashboardDimens.spaceXs))
            }

            Text(
                text = plan.price,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = plan.subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp
            )
        }
    }
}

// ─── Method tabs ──────────────────────────────────────────────────────────────

private data class MethodTabConfig(
    val method: PaymentMethod,
    val icon: ImageVector,
    val label: String,
)

private val methodTabConfigs = listOf(
    MethodTabConfig(PaymentMethod.CARD, Icons.Rounded.CreditCard, "Card"),
    MethodTabConfig(PaymentMethod.UPI, Icons.Rounded.PhoneAndroid, "UPI"),
    MethodTabConfig(PaymentMethod.NETBANKING, Icons.Rounded.AccountBalanceWallet, "Net Banking"),
)

@Composable
private fun PaymentMethodTabs(
    selected: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(DashboardDimens.spaceXxs),
        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceXxs),
    ) {
        methodTabConfigs.forEach { tab ->
            val isSelected = selected == tab.method
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f).clip(MaterialTheme.shapes.small).background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else Color.Transparent,
                ).clickable { onSelect(tab.method) }.padding(
                    vertical = DashboardDimens.spaceLg,
                ),
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(DashboardDimens.iconSm),
                )
                Spacer(modifier = Modifier.width(DashboardDimens.spaceXxs))
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.secondary,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

// ─── Card form ────────────────────────────────────────────────────────────────

@Composable
private fun CardForm(
    card: CardFormState,
    errors: FormErrors,
    onCardNumberChanged: (String) -> Unit,
    onCardExpiryChanged: (String) -> Unit,
    onCardCvvChanged: (String) -> Unit,
    onCardNameChanged: (String) -> Unit,
    onMoveFocusDown: () -> Unit,
    onClearFocus: () -> Unit,
) {
    SectionCard(title = "Card Details") {
        Column(verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd)) {
            PaymentTextField(
                value = card.number,
                onValueChange = onCardNumberChanged,
                label = "CARD NUMBER",
                placeholder = "0000 0000 0000 0000",
                error = errors.cardNumber,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { onMoveFocusDown() }),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd)) {
                PaymentTextField(
                    value = card.expiry,
                    onValueChange = onCardExpiryChanged,
                    label = "EXPIRY",
                    placeholder = "MM/YY",
                    error = errors.cardExpiry,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { onMoveFocusDown() }),
                )
                PaymentTextField(
                    value = card.cvv,
                    onValueChange = onCardCvvChanged,
                    label = "CVV",
                    placeholder = "•••",
                    error = errors.cardCvv,
                    modifier = Modifier.weight(1f),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { onMoveFocusDown() }),
                )
            }

            PaymentTextField(
                value = card.name,
                onValueChange = onCardNameChanged,
                label = "CARDHOLDER NAME",
                placeholder = "Name as on card",
                error = errors.cardName,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onClearFocus() }),
            )
        }
    }
}

// ─── UPI form ─────────────────────────────────────────────────────────────────

@Composable
private fun UpiForm(
    upiId: String,
    error: String?,
    onUpiIdChanged: (String) -> Unit,
    onClearFocus: () -> Unit,
) {
    SectionCard(title = "UPI Payment") {
        Column(verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd)) {
            PaymentTextField(
                value = upiId,
                onValueChange = onUpiIdChanged,
                label = "UPI ID",
                placeholder = "yourname@upi",
                error = error,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onClearFocus() }),
            )
            Text(
                text = "Supports GPay, PhonePe, Paytm & all UPI apps",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd)) {
                listOf("GPay", "PhonePe", "Paytm").forEach { app ->
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(DashboardDimens.cornerChip))
                            .background(MaterialTheme.colorScheme.surfaceContainer).border(
                                width = DashboardDimens.borderStroke,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(DashboardDimens.cornerChip),
                            ).padding(
                                horizontal = DashboardDimens.chipPaddingH,
                                vertical = DashboardDimens.spaceXs,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = app,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ─── Net banking form ─────────────────────────────────────────────────────────

@Composable
private fun NetBankingForm(
    banks: List<String>,
    selectedBank: String,
    error: String?,
    onBankSelected: (String) -> Unit,
) {
    SectionCard(title = "Select Bank") {
        Column(verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceXxs)) {
            banks.forEachIndexed { index, bank ->
                val isSelected = selectedBank == bank
                val isFirst = index == 0
                val isLast = index == banks.lastIndex
                val rowShape = when {
                    isFirst -> RoundedCornerShape(
                        topStart = DashboardDimens.cornerChip,
                        topEnd = DashboardDimens.cornerChip,
                    )

                    isLast -> RoundedCornerShape(
                        bottomStart = DashboardDimens.cornerChip,
                        bottomEnd = DashboardDimens.cornerChip,
                    )

                    else -> RoundedCornerShape(0.dp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(rowShape).background(
                        if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceContainer,
                    ).clickable { onBankSelected(bank) }.padding(
                        horizontal = DashboardDimens.cardPaddingComp,
                        vertical = DashboardDimens.spaceMdL,
                    ),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(DashboardDimens.iconNav).clip(CircleShape).border(
                            width = if (isSelected) 0.dp else DashboardDimens.borderStroke,
                            color = MaterialTheme.colorScheme.outline,
                            shape = CircleShape,
                        ).background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                        ),
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(DashboardDimens.iconXs),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(DashboardDimens.spaceLg))

                    Text(
                        text = bank,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground,
                    )
                }

                if (!isLast) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                        thickness = DashboardDimens.dividerThin,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = error != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = DashboardDimens.spaceMd),
            )
        }
    }
}

// ─── Security badge ───────────────────────────────────────────────────────────

@Composable
private fun SecurityBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.secondaryContainer).padding(
                horizontal = DashboardDimens.cardPaddingComp,
                vertical = DashboardDimens.spaceMd,
            ),
    ) {
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(DashboardDimens.iconNav),
        )
        Spacer(modifier = Modifier.width(DashboardDimens.spaceMd))
        Text(
            text = "256-bit SSL encrypted · Your data is safe & secure",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(DashboardDimens.spaceLg))
    }
}

// ─── CTA bar ──────────────────────────────────────────────────────────────────

@Composable
private fun PaymentCtaBar(
    plan: PlanType,
    isProcessing: Boolean,
    onPayTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).navigationBarsPadding()
            .padding(
                horizontal = DashboardDimens.screenPaddingH,
                vertical = DashboardDimens.spaceLg,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onPayTapped,
            modifier = Modifier.fillMaxWidth().height(DashboardDimens.buttonHeight),
            shape = MaterialTheme.shapes.medium,
            enabled = !isProcessing,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                disabledContentColor = Color.White,
            ),
        ) {
            AnimatedContent(
                targetState = isProcessing,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "cta_state",
            ) { processing ->
                if (processing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(DashboardDimens.iconNav),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            strokeCap = StrokeCap.Round,
                        )
                        Spacer(modifier = Modifier.width(DashboardDimens.spaceMd))
                        Text(
                            text = "Processing…",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            )
                    }
                } else {
                    Text(
                        text = "Pay ₹${plan.priceNumeric} & Start Free Trial",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        )
                }
            }
        }

        Spacer(modifier = Modifier.height(DashboardDimens.spaceXs))

        Text(
            text = "Cancel anytime · No charges during 7-day trial",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

// ─── Shared: section card ─────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    GradientCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = DashboardDimens.cardElevation,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(DashboardDimens.spaceLg))
            content()
        }
    }
}

// ─── Shared: labelled text field ──────────────────────────────────────────────

@Composable
private fun PaymentTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    error: String?,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = DashboardDimens.spaceXs),
        )

        // Use BasicTextField + manual border to avoid animated error flash from OutlinedTextField
        val shape = MaterialTheme.shapes.small
        val borderColor = when {
            !error.isNullOrEmpty() -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        }
        val borderWidth = if (!error.isNullOrEmpty()) 2.dp else 1.dp

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(DashboardDimens.buttonHeight)
                .clip(shape)
                .background(MaterialTheme.colorScheme.background)
                .border(borderWidth, borderColor, shape)
                .padding(horizontal = 12.dp),
            singleLine = true,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onBackground,
            ),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    // place the inner text field centered
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        innerTextField()
                    }
                }
            }
        )

        AnimatedVisibility(
            visible = error != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = DashboardDimens.spaceXs),
            )
        }
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PaymentGatewayScreenLightPreview() {
    TruXpenseTheme { PaymentGatewayScreen() }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PaymentGatewayScreenDarkPreview() {
    TruXpenseTheme(darkTheme = true) { PaymentGatewayScreen() }
}