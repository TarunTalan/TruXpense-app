package com.example.truxpense.presentation.screens.dashboard.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Surface
import com.example.truxpense.presentation.theme.TruXpenseTheme
import java.util.Calendar
import java.util.Locale


data class SpendPoint(
    val amount: Float,
    val xLabel: String,
    val tooltipDate: String = xLabel,
    val isToday: Boolean = false,
)

// ─────────────────────────────────────────────────────────────────────────────
// Formatter helper (kept internal to this file, used by callers via the card)
// ─────────────────────────────────────────────────────────────────────────────

fun formatChartAmount(v: Float): String = when {
    v >= 100_000f -> "₹${"%.1f".format(v / 100_000f)}L"
    v >= 1_000f   -> "₹${"%.1f".format(v / 1_000f)}k"
    else          -> "₹${v.toInt()}"
}

@Composable
fun SpendingTrendCard(
    points:      List<SpendPoint>,
    modifier:    Modifier    = Modifier,
    title:       String      = "Spending Trend",
    pillLabel:   String?     = null,
    showStats:   Boolean     = true,
    chartHeight: Dp          = 160.dp,
) {
    val hasData = remember(points) { points.any { it.amount > 0f } }

    // Derived stats
    val todayIdx   = remember(points) { points.indexOfFirst { it.isToday }.coerceAtLeast(points.lastIndex) }
    val elapsed    = (todayIdx + 1).coerceAtLeast(1)
    val spentSoFar = remember(points, elapsed) { points.take(elapsed).sumOf { it.amount.toDouble() }.toFloat() }
    val avgPerDay  = if (elapsed > 0) spentSoFar / elapsed else 0f
    val peakIdx    = remember(points) { points.indices.maxByOrNull { points[it].amount } ?: -1 }
    val peakAmt    = if (peakIdx >= 0) points[peakIdx].amount else 0f

    // Theme colours (captured before Canvas scope)
    val primary       = MaterialTheme.colorScheme.primary
    val onSurface     = MaterialTheme.colorScheme.onSurface
    val surfaceVar    = MaterialTheme.colorScheme.surfaceVariant
    val outline       = MaterialTheme.colorScheme.outline
    val surface       = MaterialTheme.colorScheme.surface
    val labelColor    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    val errorColor    = MaterialTheme.colorScheme.error
    val tooltipBg     = MaterialTheme.colorScheme.inverseSurface
    val tooltipTextC  = MaterialTheme.colorScheme.inverseOnSurface

    GradientCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text  = title,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurface,
                )
                if (pillLabel != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(primary.copy(alpha = 0.10f))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text       = pillLabel,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = primary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (!hasData) {
                // ── Empty state ───────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                        .clip(RoundedCornerShape(10.dp))
                        .background(surfaceVar.copy(alpha = 0.5f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("📊", fontSize = 30.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text       = "No spending data yet",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = onSurface.copy(alpha = 0.6f),
                    )
                    Text(
                        text     = "Add expenses to see your trend",
                        fontSize = 12.sp,
                        color    = onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                return@Column
            }

            // ── Stats row ─────────────────────────────────────────────────────
            if (showStats) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    SpendTrendStat(
                        label      = "Spent so far",
                        value      = formatChartAmount(spentSoFar),
                        valueColor = onSurface,
                        modifier   = Modifier.weight(1f),
                    )
                    VerticalDivider(modifier = Modifier.height(36.dp), color = outline.copy(alpha = 0.25f))
                    SpendTrendStat(
                        label      = "Avg / day",
                        value      = formatChartAmount(avgPerDay),
                        valueColor = primary,
                        modifier   = Modifier.weight(1f),
                    )
                    VerticalDivider(modifier = Modifier.height(36.dp), color = outline.copy(alpha = 0.25f))
                    SpendTrendStat(
                        label      = if (peakIdx >= 0) "Peak · ${points[peakIdx].xLabel}" else "Peak",
                        value      = formatChartAmount(peakAmt),
                        valueColor = errorColor,
                        modifier   = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            // ── Chart ─────────────────────────────────────────────────────────
            SpendingLineChart(
                points       = points,
                peakIndex    = peakIdx,
                modifier     = Modifier.fillMaxWidth().height(chartHeight),
                lineColor    = primary,
                fillBrush    = Brush.verticalGradient(
                    listOf(primary.copy(alpha = 0.25f), primary.copy(alpha = 0.0f)),
                ),
                gridColor    = outline.copy(alpha = 0.12f),
                labelColor   = labelColor,
                todayColor   = primary.copy(alpha = 0.45f),
                peakColor    = errorColor,
                surfaceColor = surface,
                tooltipBg    = tooltipBg,
                tooltipText  = tooltipTextC,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SpendTrendStat  — one cell in the stats row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SpendTrendStat(
    label:      String,
    value:      String,
    valueColor: Color,
    modifier:   Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold,
            color      = valueColor,
        )
        Text(
            text      = label,
            fontSize  = 10.sp,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SpendingLineChart  — the raw canvas composable (reusable on its own)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SpendingLineChart(
    points:       List<SpendPoint>,
    peakIndex:    Int         = -1,
    modifier:     Modifier    = Modifier,
    lineColor:    Color       = MaterialTheme.colorScheme.primary,
    fillBrush:    Brush       = Brush.verticalGradient(listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.0f),
    )),
    gridColor:    Color       = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
    labelColor:   Color       = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
    todayColor:   Color       = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
    peakColor:    Color       = MaterialTheme.colorScheme.error,
    surfaceColor: Color       = MaterialTheme.colorScheme.surface,
    tooltipBg:    Color       = MaterialTheme.colorScheme.inverseSurface,
    tooltipText:  Color       = MaterialTheme.colorScheme.inverseOnSurface,
) {
    val n = points.size.coerceAtLeast(1)
    val todayIndex = remember(points) { points.indexOfFirst { it.isToday } }

    // Draw-in animation
    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(points) { triggered = false; triggered = true }
    val drawProgress by animateFloatAsState(
        targetValue    = if (triggered) 1f else 0f,
        animationSpec  = tween(900, easing = FastOutSlowInEasing),
        label          = "chart_draw",
    )

    // Long-press / drag selection
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val tooltipAlpha by animateFloatAsState(
        targetValue   = if (selectedIndex != null) 1f else 0f,
        animationSpec = tween(150),
        label         = "tooltip_alpha",
    )

    val density = LocalDensity.current

    Canvas(
        modifier = modifier
            // ── Press: select immediately, clear on release ───────────────────
            .pointerInput(n) {
                val padLeftPx  = with(density) { 36.dp.toPx() }
                val padRightPx = with(density) { 8.dp.toPx() }
                val chartW     = size.width - padLeftPx - padRightPx
                val stepX      = if (n > 1) chartW / (n - 1).toFloat() else chartW
                fun idx(x: Float) = ((x - padLeftPx) / stepX).toInt().coerceIn(0, n - 1)
                detectTapGestures(
                    onPress = { offset ->
                        selectedIndex = idx(offset.x)
                        tryAwaitRelease()
                        selectedIndex = null
                    },
                )
            }
            // ── Drag: track finger across chart ──────────────────────────────
            .pointerInput(n) {
                val padLeftPx  = with(density) { 36.dp.toPx() }
                val padRightPx = with(density) { 8.dp.toPx() }
                val chartW     = size.width - padLeftPx - padRightPx
                val stepX      = if (n > 1) chartW / (n - 1).toFloat() else chartW
                fun idx(x: Float) = ((x - padLeftPx) / stepX).toInt().coerceIn(0, n - 1)
                detectHorizontalDragGestures(
                    onDragStart  = { offset -> selectedIndex = idx(offset.x) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        selectedIndex = idx(change.position.x)
                    },
                    onDragEnd    = { selectedIndex = null },
                    onDragCancel = { selectedIndex = null },
                )
            },
    ) {
        val w         = size.width
        val h         = size.height
        val padLeft   = 36.dp.toPx()
        val padRight  = 8.dp.toPx()
        val padTop    = 10.dp.toPx()
        val padBottom = 22.dp.toPx()
        val chartW    = w - padLeft - padRight
        val chartH    = h - padTop - padBottom
        val maxVal    = (points.maxOfOrNull { it.amount } ?: 1f).coerceAtLeast(1f)
        val stepX     = if (n > 1) chartW / (n - 1).toFloat() else chartW

        fun xOf(i: Int)   = padLeft + i * stepX
        fun yOf(v: Float) = padTop + chartH - (v / maxVal * chartH)

        // ── Y-axis grid + labels ──────────────────────────────────────────────
        val yAxisPaint = android.graphics.Paint().apply {
            color       = labelColor.toArgb()
            textSize    = 9.sp.toPx()
            textAlign   = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
        }
        listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { frac ->
            val gy = padTop + chartH * (1f - frac)
            drawLine(
                color       = gridColor,
                start       = Offset(padLeft, gy),
                end         = Offset(w - padRight, gy),
                strokeWidth = 1.dp.toPx(),
                pathEffect  = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
            )
            drawContext.canvas.nativeCanvas.drawText(
                formatChartAmount(maxVal * frac),
                padLeft - 4.dp.toPx(),
                gy + 3.5.dp.toPx(),
                yAxisPaint,
            )
        }

        // ── Today dashed vertical marker ──────────────────────────────────────
        if (todayIndex in 0 until n) {
            drawLine(
                color       = todayColor,
                start       = Offset(xOf(todayIndex), padTop),
                end         = Offset(xOf(todayIndex), padTop + chartH),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect  = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f),
            )
        }

        // ── Data points ───────────────────────────────────────────────────────
        val pts = points.mapIndexed { i, p -> Offset(xOf(i), yOf(p.amount)) }

        // ── Catmull-Rom cubic spline (passes through every point) ─────────────
        val tension = if (n > 15) 0.15f else 0.25f
        val yMin    = padTop
        val yMax    = padTop + chartH

        val path = Path()
        when {
            pts.size == 1 -> { path.moveTo(pts[0].x, pts[0].y); path.lineTo(pts[0].x + 1f, pts[0].y) }
            pts.size >= 2 -> {
                path.moveTo(pts[0].x, pts[0].y)
                for (i in 0 until pts.size - 1) {
                    val p0  = if (i > 0) pts[i - 1] else pts[i]
                    val p1  = pts[i]
                    val p2  = pts[i + 1]
                    val p3  = if (i + 2 < pts.size) pts[i + 2] else pts[i + 1]
                    val cp1x = p1.x + (p2.x - p0.x) * tension
                    val cp1y = (p1.y + (p2.y - p0.y) * tension).coerceIn(yMin, yMax)
                    val cp2x = p2.x - (p3.x - p1.x) * tension
                    val cp2y = (p2.y - (p3.y - p1.y) * tension).coerceIn(yMin, yMax)
                    path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
                }
            }
        }

        // ── Fill path (clean baseline close) ─────────────────────────────────
        val fillPath = Path()
        if (pts.size >= 2) {
            val baseline = padTop + chartH
            fillPath.addPath(path)
            fillPath.lineTo(pts.last().x, baseline)
            fillPath.lineTo(pts.first().x, baseline)
            fillPath.lineTo(pts.first().x, pts.first().y)
            fillPath.close()
        }

        // ── Reveal animation clip ─────────────────────────────────────────────
        clipRect(left = padLeft, right = padLeft + chartW * drawProgress) {
            if (pts.size >= 2) drawPath(fillPath, fillBrush)
            drawPath(
                path  = path,
                color = lineColor,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap   = StrokeCap.Round,
                    join  = StrokeJoin.Round,
                ),
            )
        }

        // ── Peak marker ───────────────────────────────────────────────────────
        if (peakIndex in pts.indices && drawProgress > 0.1f) {
            val p = pts[peakIndex]
            drawCircle(color = peakColor.copy(alpha = 0.18f), radius = 9.dp.toPx(), center = p)
            drawCircle(color = surfaceColor,                   radius = 5.dp.toPx(), center = p)
            drawCircle(color = peakColor,                      radius = 4.dp.toPx(), center = p)
        }

        // ── Today marker ─────────────────────────────────────────────────────
        if (todayIndex in pts.indices && drawProgress > 0.1f) {
            val p = pts[todayIndex]
            drawCircle(color = lineColor.copy(alpha = 0.20f), radius = 7.dp.toPx(), center = p)
            drawCircle(color = surfaceColor,                   radius = 4.dp.toPx(), center = p)
            drawCircle(color = lineColor,                      radius = 3.dp.toPx(), center = p)
        }

        // ── X-axis labels ─────────────────────────────────────────────────────
        val xLblPaint = android.graphics.Paint().apply {
            textSize    = 9.5.sp.toPx()
            textAlign   = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        // Show label for first, every-7th (monthly), every point (weekly/yearly), and last
        val showEvery = if (n > 15) 7 else 1
        for (i in 0 until n) {
            val lbl = points[i].xLabel
            val isToday = points[i].isToday
            val show = i == 0 || i == n - 1 || i % showEvery == 0 || isToday
            if (show) {
                xLblPaint.color          = if (isToday) lineColor.copy(alpha = 0.9f).toArgb() else labelColor.toArgb()
                xLblPaint.isFakeBoldText = isToday
                val displayLbl = if (isToday) "Today" else lbl
                drawContext.canvas.nativeCanvas.drawText(displayLbl, xOf(i), h - 4.dp.toPx(), xLblPaint)
            }
        }

        // ── Long-press / drag tooltip ─────────────────────────────────────────
        val selIdx = selectedIndex
        if (selIdx != null && selIdx in pts.indices && tooltipAlpha > 0f) {
            val selX   = xOf(selIdx)
            val selPt  = pts[selIdx]
            val selVal = points[selIdx].amount
            val dateStr = points[selIdx].tooltipDate

            // Crosshair line
            drawLine(
                color       = lineColor.copy(alpha = 0.55f * tooltipAlpha),
                start       = Offset(selX, padTop),
                end         = Offset(selX, padTop + chartH),
                strokeWidth = 1.5.dp.toPx(),
            )
            // Dot on the line
            drawCircle(color = surfaceColor.copy(alpha = tooltipAlpha),          radius = 5.dp.toPx(), center = selPt)
            drawCircle(color = lineColor.copy(alpha = tooltipAlpha),             radius = 4.dp.toPx(), center = selPt)
            drawCircle(color = surfaceColor.copy(alpha = tooltipAlpha * 0.6f),   radius = 2.5.dp.toPx(), center = selPt)

            // Tooltip bubble (drawn entirely in Canvas — no Composable overlay needed)
            val amtText  = formatChartAmount(selVal)
            val tPad     = 8.dp.toPx()
            val tRadius  = 6.dp.toPx()

            val amtPaint = android.graphics.Paint().apply {
                color          = tooltipText.copy(alpha = tooltipAlpha).toArgb()
                textSize       = 12.sp.toPx()
                isFakeBoldText = true
                isAntiAlias    = true
                textAlign      = android.graphics.Paint.Align.LEFT
            }
            val datePaint = android.graphics.Paint().apply {
                color       = tooltipText.copy(alpha = tooltipAlpha * 0.75f).toArgb()
                textSize    = 10.sp.toPx()
                isAntiAlias = true
                textAlign   = android.graphics.Paint.Align.LEFT
            }

            val boxW = maxOf(amtPaint.measureText(amtText), datePaint.measureText(dateStr)) + tPad * 2f
            val boxH = amtPaint.textSize + datePaint.textSize + tPad * 2.1f

            var bx = selX - boxW / 2f
            bx = bx.coerceIn(padLeft, w - padRight - boxW)
            var by = selPt.y - 5.dp.toPx() - boxH - 8.dp.toPx()
            if (by < padTop) by = selPt.y + 14.dp.toPx()

            val bubblePaint = android.graphics.Paint().apply {
                color       = tooltipBg.copy(alpha = 0.93f * tooltipAlpha).toArgb()
                isAntiAlias = true
                style       = android.graphics.Paint.Style.FILL
            }
            drawContext.canvas.nativeCanvas.drawRoundRect(
                bx, by, bx + boxW, by + boxH,
                tRadius, tRadius, bubblePaint,
            )
            val textX = bx + tPad
            drawContext.canvas.nativeCanvas.drawText(
                amtText, textX,
                by + tPad + amtPaint.textSize * 0.85f, amtPaint,
            )
            drawContext.canvas.nativeCanvas.drawText(
                dateStr, textX,
                by + tPad + amtPaint.textSize + datePaint.textSize * 0.9f + tPad * 0.4f, datePaint,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Extension helpers — convert existing data structures to List<SpendPoint>
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Convert a per-day [FloatArray] from HomeViewModel into [SpendPoint] list.
 * Index 0 = day 1 of the current month.
 */
fun FloatArray.toSpendPoints(
    monthName: String = Calendar.getInstance()
        .getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: "",
    year: Int = Calendar.getInstance().get(Calendar.YEAR),
    todayDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH),  // 1-based
): List<SpendPoint> {
    val n = size
    return mapIndexed { i, v ->
        val day = i + 1
        SpendPoint(
            amount      = v,
            xLabel      = "$day",
            tooltipDate = "Day $day, $monthName $year",
            isToday     = day == todayDay,
        )
    }.let { raw ->
        // If empty pad to current month length
        if (n == 0) {
            val daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
            List(daysInMonth) { i ->
                val day = i + 1
                SpendPoint(
                    amount      = 0f,
                    xLabel      = "$day",
                    tooltipDate = "Day $day, $monthName $year",
                    isToday     = day == todayDay,
                )
            }
        } else raw
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────────────────────────────────────

private val samplePoints = listOf(
    200f, 0f, 450f, 300f, 150f, 600f, 50f,
    400f, 320f, 0f, 750f, 250f, 180f, 420f,
    0f, 310f, 500f, 95f, 630f, 200f, 0f,
    410f, 280f, 360f, 140f, 0f, 800f, 230f,
    170f, 520f, 390f,
).mapIndexed { i, v ->
    SpendPoint(
        amount      = v,
        xLabel      = "${i + 1}",
        tooltipDate = "Day ${i + 1}, Mar 2026",
        isToday     = i == 4,
    )
}

@Preview(showBackground = true, name = "SpendingTrendCard – data (light)")
@Composable
private fun SpendingTrendCardPreview() {
    TruXpenseTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            SpendingTrendCard(
                points    = samplePoints,
                pillLabel = "Mar 2026",
            )
        }
    }
}

@Preview(showBackground = true, name = "SpendingTrendCard – empty")
@Composable
private fun SpendingTrendCardEmptyPreview() {
    TruXpenseTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            SpendingTrendCard(points = emptyList())
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    name = "SpendingTrendCard – dark",
)
@Composable
private fun SpendingTrendCardDarkPreview() {
    TruXpenseTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            SpendingTrendCard(
                points    = samplePoints,
                pillLabel = "Mar 2026",
            )
        }
    }
}

