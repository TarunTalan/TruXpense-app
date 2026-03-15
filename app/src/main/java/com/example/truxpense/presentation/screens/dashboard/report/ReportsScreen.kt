package com.example.truxpense.presentation.screens.dashboard.report

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.truxpense.R
import com.example.truxpense.data.repository.report.Report
import com.example.truxpense.data.repository.report.ReportType
import com.example.truxpense.presentation.screens.dashboard.components.GradientCard
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.theme.TruXpenseTheme
import java.util.*

// ── Palette (matches ReportDetailScreen) ─────────────────────────────────────

private val AccentPurple = Color(0xFF6C63FF)
private val ColorIncome = Color(0xFF2ECC71)
private val ColorExpense = Color(0xFFEF4444)
private val ColorAll = Color(0xFF3A86FF)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun ReportsScreen(
    onBack: () -> Unit = {},
    onGenerateReport: () -> Unit = {},
    onReportClick: (String) -> Unit = {},
    onOpenVault: () -> Unit = {},
    vm: ReportsViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                headerTitle = "Reports",
                showBack = true,
                onBack = onBack,
                actions = {
                    // Vault icon
                    IconButton(onClick = onOpenVault) {
                        Icon(
                            painter = painterResource(R.drawable.vault),
                            contentDescription = "Open Vault",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(DashboardDimens.iconMd),
                        )
                    }
                    // Generate new report
                    Box {
                        IconButton(onClick = onGenerateReport) {
                            Box(
                                modifier = Modifier.size(38.dp).border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                                    CircleShape,
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier.matchParentSize().background(
                                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.15f), CircleShape,
                                    ).blur(8.dp)
                                )
                                Icon(
                                    painter = painterResource(R.drawable.add),
                                    contentDescription = "Generate Report",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentPurple)
            }
            return@Scaffold
        }

        if (uiState.reports.isEmpty()) {
            ReportsEmptyState(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                onGenerateReport = onGenerateReport,
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(
                start = DashboardDimens.screenPaddingH,
                end = DashboardDimens.screenPaddingH,
                top = 16.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Summary strip ─────────────────────────────────────────────────
            item {
                ReportsSummaryStrip(count = uiState.reports.size)
            }

            // ── Section label ─────────────────────────────────────────────────
            item {
                Text(
                    text = "All generated reports",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Report cards ──────────────────────────────────────────────────
            items(uiState.reports, key = { it.id }) { report ->
                AnimatedVisibility(visible = true, enter = fadeIn()) {
                    ReportCard(
                        report = report,
                        onClick = { onReportClick(report.id) },
                    )
                }
            }
        }
    }
}

// ── Summary strip ─────────────────────────────────────────────────────────────

@Composable
private fun ReportsSummaryStrip(count: Int) {
    val colorCircle = MaterialTheme.colorScheme.onBackground
    // Use the shared GradientCard component for consistent gradient, shadow and shape.
    GradientCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = DashboardDimens.cardElevation,
    ) {
        // Keep a subtle circular highlight using drawBehind inside the card content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .drawBehind {
                    drawCircle(
                        colorCircle.copy(alpha = 0.07f),
                        size.width * 0.4f,
                        Offset(size.width * 0.9f, size.height * 0.5f),
                    )
                },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "$count",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "report${if (count != 1) "s" else ""} generated",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.report),
                            contentDescription = "Report",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Reports hub",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Medium
                        )

                    }
                }
            }
        }
    }
}

// ── Report card ───────────────────────────────────────────────────────────────

@Composable
private fun ReportCard(
    report: Report,
    onClick: () -> Unit,
) {
    val type = report.parsedType()
    val typeColor = when (type) {
        ReportType.EXPENSE -> ColorExpense
        ReportType.INCOME -> ColorIncome
        ReportType.ALL -> ColorAll
    }
    val typeLabel = when (type) {
        ReportType.EXPENSE -> "Expense"
        ReportType.INCOME -> "Income"
        ReportType.ALL -> "Full"
    }
    val dateLabel = formatDateRange(report.fromDate, report.toDate)
    val cats = report.parsedCategories()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Type badge icon
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(typeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                val iconRes = when (type) {
                    ReportType.EXPENSE -> R.drawable.refund
                    ReportType.INCOME -> R.drawable.salary
                    ReportType.ALL -> R.drawable.report_ic
                }
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = typeLabel,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Content
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = report.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Type pill
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(typeColor.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(typeLabel, fontSize = 9.sp, color = typeColor, fontWeight = FontWeight.SemiBold)
                    }
                    Text("·", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = dateLabel,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (cats.isNotEmpty()) {
                    Text(
                        text = cats.take(3).joinToString(", ") + if (cats.size > 3) " +${cats.size - 3}" else "",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Chevron
            Icon(
                painter = painterResource(R.drawable.right_arrow),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun ReportsEmptyState(
    modifier: Modifier = Modifier,
    onGenerateReport: () -> Unit,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("📊", fontSize = 52.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No reports yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Generate your first report to see your spending, income and trends in one place.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onGenerateReport,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text("Generate a Report", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
        }
    }
}

// ── Date helpers ──────────────────────────────────────────────────────────────

private fun formatDateRange(from: Long, to: Long): String {
    val c1 = Calendar.getInstance().apply { timeInMillis = from }
    val c2 = Calendar.getInstance().apply { timeInMillis = to }
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val d1 = c1.get(Calendar.DAY_OF_MONTH)
    val m1 = months[c1.get(Calendar.MONTH)]
    val y1 = c1.get(Calendar.YEAR)
    val d2 = c2.get(Calendar.DAY_OF_MONTH)
    val m2 = months[c2.get(Calendar.MONTH)]
    val y2 = c2.get(Calendar.YEAR)
    return if (y1 == y2) "$d1 $m1 – $d2 $m2 $y2" else "$d1 $m1 $y1 – $d2 $m2 $y2"
}

// ─────────────────────────────────────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun ReportsScreenPreview() {
    TruXpenseTheme {
        // Build a few sample reports
        val now = System.currentTimeMillis()
        val oneWeek = 7L * 24 * 60 * 60 * 1000
        val sampleReports = listOf(
            Report(
                id = "r1",
                title = "January Summary",
                fromDate = now - oneWeek * 4,
                toDate = now,
                reportType = ReportType.ALL.name
            ),
            Report(
                id = "r2",
                title = "Travel Expenses",
                fromDate = now - oneWeek * 8,
                toDate = now - oneWeek * 2,
                reportType = ReportType.EXPENSE.name
            ),
            Report(
                id = "r3",
                title = "Freelance Income",
                fromDate = now - oneWeek * 12,
                toDate = now - oneWeek * 5,
                reportType = ReportType.INCOME.name
            ),
        )

        // Minimal UI mirroring ReportsScreen layout
        Scaffold(
            contentWindowInsets = WindowInsets(0), containerColor = MaterialTheme.colorScheme.background, topBar = {
                ScreenTopBar(headerTitle = "Reports", showBack = true, onBack = {}, actions = {})
            }) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(
                    start = DashboardDimens.screenPaddingH,
                    end = DashboardDimens.screenPaddingH,
                    top = 6.dp,
                    bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { ReportsSummaryStrip(count = sampleReports.size) }
                item {
                    Text(
                        text = "Generated reports",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(sampleReports, key = { it.id }) { report ->
                    ReportCard(report = report, onClick = {})
                }
            }
        }
    }
}

