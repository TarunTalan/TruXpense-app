package com.example.truxpense.presentation.screens.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.GradientCard
import com.example.truxpense.presentation.screens.premium.model.BenefitSection
import com.example.truxpense.presentation.screens.premium.model.FeatureCell
import com.example.truxpense.presentation.screens.premium.model.FeatureRow
import com.example.truxpense.presentation.screens.premium.model.PlanType
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.theme.TruXpenseTheme

// ─── Screen entry point ───────────────────────────────────────────────────────

@Composable
fun PaywallScreen(
    onNavigateBack: () -> Unit = {},
    onStartTrial: (plan: PlanType) -> Unit = {},
    viewModel: PaywallViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    PaywallContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onStartTrial = { onStartTrial(uiState.selectedPlan) },
    )
}

// ─── Stateless content ────────────────────────────────────────────────────────

@Composable
private fun PaywallContent(
    uiState: PaywallUiState,
    onNavigateBack: () -> Unit,
    onStartTrial: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val headerBrush = Brush.linearGradient(
        colors = listOf(primary, primary.copy(alpha = 0.78f)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = DashboardDimens.buttonHeight + DashboardDimens.spaceXxl + DashboardDimens.spaceXxl),
            ) {
                PaywallHeader(brush = headerBrush, onNavigateBack = onNavigateBack)
                Spacer(modifier = Modifier.height(DashboardDimens.spaceXl))
                Column(
                    modifier = Modifier.padding(horizontal = DashboardDimens.screenPaddingH),
                    verticalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
                ) {
                    uiState.benefits.forEach { section -> BenefitCard(section = section) }
                }
                Spacer(modifier = Modifier.height(DashboardDimens.spaceLg))
                FeatureComparisonTable(
                    rows = uiState.featureRows,
                    modifier = Modifier.padding(horizontal = DashboardDimens.screenPaddingH),
                )
                Spacer(modifier = Modifier.height(DashboardDimens.spaceXl))
            }
            PaywallCtaBar(onStartTrial = onStartTrial, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun PaywallHeader(brush: Brush, onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = brush),
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.TopEnd)
                .background(Color.White.copy(alpha = 0.07f), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.BottomStart)
                .background(Color.White.copy(alpha = 0.05f), CircleShape),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DashboardDimens.spaceMd, vertical = DashboardDimens.spaceMd),
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        painterResource(R.drawable.back_icon),
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(
                    text = "\"TruXpense premium\"",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.size(DashboardDimens.iconButtonMd))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = DashboardDimens.screenPaddingH,
                        end = DashboardDimens.screenPaddingH,
                        bottom = DashboardDimens.spaceXxl,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Upgrade to Premium",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(DashboardDimens.spaceMd))
                Text(
                    text = "\"Automated tracking, advanced analytics, and\nsmarter financial decision.\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(0.9f),
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic,
                )
                Spacer(modifier = Modifier.height(DashboardDimens.spaceLg))
                ProBadge()
            }
        }
    }
}

@Composable
private fun ProBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(DashboardDimens.cornerFilterChip))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = DashboardDimens.chipPaddingH, vertical = DashboardDimens.spaceXs),
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
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

// ─── Benefit card ─────────────────────────────────────────────────────────────

@Composable
private fun BenefitCard(section: BenefitSection) {
    GradientCard(modifier = Modifier.fillMaxWidth(), elevation = DashboardDimens.cardElevation) {
        Column(modifier = Modifier.fillMaxWidth().padding(DashboardDimens.cardPadding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = DashboardDimens.spaceMd),
            ) {
                Icon(
                    painter = painterResource(id = section.iconRes),
                    contentDescription = section.title,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(DashboardDimens.iconLg),
                )
                Spacer(modifier = Modifier.width(DashboardDimens.spaceMd))
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            section.points.forEach { point ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(bottom = DashboardDimens.spaceSm),
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(end = DashboardDimens.spaceMd, top = 1.dp),
                    )
                    Text(
                        text = point,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ─── Feature comparison table ─────────────────────────────────────────────────

@Composable
private fun FeatureComparisonTable(rows: List<FeatureRow>, modifier: Modifier = Modifier) {
    GradientCard(modifier = modifier.fillMaxWidth(), elevation = DashboardDimens.cardElevation) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Feature",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start = DashboardDimens.cardPadding,
                            top = DashboardDimens.spaceMdL,
                            bottom = DashboardDimens.spaceMdL
                        ),
                )
                Text(
                    text = "Free",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .width(72.dp)
                        .padding(vertical = DashboardDimens.spaceMdL),
                    textAlign = TextAlign.Center,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .width(100.dp)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(vertical = DashboardDimens.spaceMdL),
                ) {
                    Icon(
                        painterResource(R.drawable.diamond),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(DashboardDimens.iconSm),
                    )
                    Spacer(modifier = Modifier.width(DashboardDimens.spaceXs))
                    Text(
                        text = "Premium",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color =  MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            rows.forEachIndexed { index, row ->
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    thickness = DashboardDimens.dividerThin,
                )
                FeatureTableRow(row = row, isLast = index == rows.lastIndex)
            }
        }
    }
}

@Composable
private fun FeatureTableRow(row: FeatureRow, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = row.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = DashboardDimens.cardPadding,
                    top = DashboardDimens.spaceMdL,
                    bottom = DashboardDimens.spaceMdL
                ),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(72.dp).padding(vertical = DashboardDimens.spaceMdL),
        ) { FeatureCellContent(cell = row.free, isPremium = false) }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(100.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .then(if (isLast) Modifier.clip(RoundedCornerShape(bottomEnd = DashboardDimens.cornerCard)) else Modifier)
                .padding(vertical = DashboardDimens.spaceMdL),
        ) { FeatureCellContent(cell = row.premium, isPremium = true) }
    }
}

@Composable
private fun FeatureCellContent(cell: FeatureCell, isPremium: Boolean) {
    when (cell) {
        is FeatureCell.Check -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(DashboardDimens.iconLg).background(MaterialTheme.colorScheme.primary, CircleShape),
        ) {
            Icon(Icons.Rounded.Check, "Available", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(DashboardDimens.iconSm))
        }

        is FeatureCell.Minus -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(DashboardDimens.iconLg)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), CircleShape),
        ) {
            Text("-", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        }

        is FeatureCell.Label -> Text(
            text = cell.text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isPremium) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

// ─── CTA bar ──────────────────────────────────────────────────────────────────

@Composable
private fun PaywallCtaBar(onStartTrial: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = DashboardDimens.screenPaddingH, vertical = DashboardDimens.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onStartTrial,
            modifier = Modifier.fillMaxWidth().height(DashboardDimens.buttonHeight),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor =  MaterialTheme.colorScheme.onPrimary
            ),
        ) {
            Text(
                "\"Start 7-days Free Trial\"",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(DashboardDimens.spaceXs))
        Text(
            text = "* ₹149/month or ₹999/year*",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PaywallScreenLightPreview() {
    TruXpenseTheme { PaywallScreen() }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PaywallScreenDarkPreview() {
    TruXpenseTheme(darkTheme = true) { PaywallScreen() }
}