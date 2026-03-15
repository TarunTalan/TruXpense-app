package com.example.truxpense.presentation.screens.dashboard.report

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import com.example.truxpense.data.repository.report.ReportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs

/**
 * Generates PDF / CSV / Excel files from [ReportDetailUiState] and
 * triggers the system share-sheet so the user can save or send the file.
 *
 * All file I/O happens on [Dispatchers.IO].
 */
object ReportExporter {

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun export(
        context: Context,
        format: ExportFormat,
        state: ReportDetailUiState,
    ): ExportResult = withContext(Dispatchers.IO) {
        runCatching {
            val dir = outputDir(context)
            val safeName = state.title.replace(Regex("[^A-Za-z0-9_\\-]"), "_").ifBlank { "report" }
            when (format) {
                ExportFormat.PDF -> writePdf(dir, safeName, state)
                ExportFormat.CSV -> writeCsv(dir, safeName, state)
                ExportFormat.EXCEL -> writeExcel(dir, safeName, state)
            }
        }.getOrElse { e ->
            ExportResult(filePath = "", mimeType = "", fileName = "", error = e.message ?: "Unknown error")
        }
    }

    /** Opens the system share-sheet for a successfully exported file. */
    fun share(context: Context, result: ExportResult) {
        if (result.error != null) return
        val file = File(result.filePath)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = result.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share report via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    // ── Output directory ──────────────────────────────────────────────────────

    private fun outputDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "TruXpense")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PDF
    // ─────────────────────────────────────────────────────────────────────────

    private fun writePdf(dir: File, safeName: String, state: ReportDetailUiState): ExportResult {
        val fileName = "$safeName.pdf"
        val file = File(dir, fileName)

        val pageW = 595
        val pageH = 842

        val document = PdfDocument()

        val paintBg = Paint().apply { color = "#F8F8FF".toColorInt() }
        val paintHeader = Paint().apply { color = "#6C63FF".toColorInt() }
        val paintWhite = Paint().apply { color = Color.WHITE; textSize = 14f; isFakeBoldText = true }
        val paintTitle = Paint().apply { color = "#1A1A2E".toColorInt(); textSize = 18f; isFakeBoldText = true }
        val paintSub = Paint().apply { color = "#666680".toColorInt(); textSize = 10f }
        val paintLabel = Paint().apply { color = "#444466".toColorInt(); textSize = 11f; isFakeBoldText = true }
        val paintValue = Paint().apply { color = "#1A1A2E".toColorInt(); textSize = 11f }
        val paintExpense = Paint().apply { color = "#EF4444".toColorInt(); textSize = 11f; isFakeBoldText = true }
        val paintIncome2 = Paint().apply { color = "#2ECC71".toColorInt(); textSize = 11f; isFakeBoldText = true }
        val paintDivider = Paint().apply { color = "#E0E0F0".toColorInt(); strokeWidth = 0.7f }
        val paintRow = Paint().apply { color = "#F2F2FF".toColorInt() }
        val paintKpiBox = Paint().apply { color = Color.WHITE }

        // helper to start a fresh page and return its canvas + reset y
        fun newPage(pageNum: Int): Pair<PdfDocument.Page, Canvas> {
            val info = PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create()
            val p = document.startPage(info)
            p.canvas.drawRect(0f, 0f, pageW.toFloat(), pageH.toFloat(), paintBg)
            return p to p.canvas
        }

        var pageNum = 1
        var (page, canvas) = newPage(pageNum)
        var y = 0f

        fun finishAndNewPage() {
            document.finishPage(page)
            pageNum++
            val pair = newPage(pageNum)
            page = pair.first; canvas = pair.second
            y = 48f
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > pageH - 32f) finishAndNewPage()
        }

        // ── Header bar ────────────────────────────────────────────────────────
        canvas.drawRect(0f, 0f, pageW.toFloat(), 72f, paintHeader)
        paintWhite.textSize = 11f; paintWhite.isFakeBoldText = false
        canvas.drawText("TruXpense", 24f, 22f, paintWhite)
        paintWhite.textSize = 16f; paintWhite.isFakeBoldText = true
        canvas.drawText(state.title.take(50), 24f, 48f, paintWhite)
        paintWhite.textSize = 9f; paintWhite.isFakeBoldText = false
        canvas.drawText(state.dateRangeLabel, 24f, 64f, paintWhite)
        y = 88f

        // ── KPI boxes ─────────────────────────────────────────────────────────
        val kpiBoxW = (pageW - 48f - 16f) / 3f
        val kpis = buildList {
            if (state.reportType != ReportType.INCOME) add("Expenses" to fmt(state.totalExpenses))
            if (state.reportType != ReportType.EXPENSE) add("Income" to fmt(state.totalIncome))
            if (state.reportType == ReportType.ALL) add("Net" to fmt(state.netAmount))
            add("Transactions" to state.transactionCount.toString())
            add("Avg Daily" to fmt(state.avgDailySpend))
        }
        kpis.take(3).forEachIndexed { i, (lbl, v) ->
            val bx = 24f + i * (kpiBoxW + 8f)
            canvas.drawRoundRect(bx, y, bx + kpiBoxW, y + 48f, 8f, 8f, paintKpiBox)
            paintLabel.textSize = 9f; canvas.drawText(lbl, bx + 8f, y + 14f, paintLabel)
            paintTitle.textSize = 13f; canvas.drawText(v, bx + 8f, y + 32f, paintTitle)
        }
        y += 64f

        // ── Summary section ───────────────────────────────────────────────────
        ensureSpace(30f)
        sectionHeader(canvas, "Summary", y, pageW, paintHeader, paintWhite); y += 26f
        val summaryRows = buildList {
            if (state.reportType != ReportType.INCOME) add("Total Expenses" to fmt(state.totalExpenses))
            if (state.reportType != ReportType.EXPENSE) add("Total Income" to fmt(state.totalIncome))
            if (state.reportType == ReportType.ALL) add("Net Balance" to fmt(state.netAmount))
            add("Transactions" to state.transactionCount.toString())
            add("Avg Daily Spend" to fmt(state.avgDailySpend))
            state.topCategory?.let { add("Top Category" to it.first) }
            state.topMerchant?.let { add("Top Merchant" to it.first) }
            state.highestSpendDay?.let { add("Highest Day" to "${it.first}  ${fmt(it.second)}") }
        }
        summaryRows.forEachIndexed { i, (k, v) ->
            ensureSpace(20f)
            if (i % 2 == 0) canvas.drawRect(24f, y - 2f, pageW - 24f, y + 16f, paintRow)
            canvas.drawText(k, 28f, y + 11f, paintLabel)
            canvas.drawText(v, 280f, y + 11f, paintValue)
            y += 20f
        }
        y += 12f

        // ── Spending by Category section ─────────────────────────────────────
        if (state.categoryRows.isNotEmpty()) {
            val rowH = 22f
            val sectionH = 26f + 22f + state.categoryRows.size * rowH + 14f
            ensureSpace(sectionH.coerceAtMost(200f))

            sectionHeader(canvas, "Spending by Category", y, pageW, paintHeader, paintWhite); y += 26f

            // Column headers
            canvas.drawRect(24f, y, pageW - 24f, y + 18f, paintHeader)
            paintWhite.textSize = 9f
            canvas.drawText("Category", 44f, y + 13f, paintWhite)
            canvas.drawText("Amount", 240f, y + 13f, paintWhite)
            canvas.drawText("Share", 330f, y + 13f, paintWhite)
            y += 22f

            // Category palette (mirrors ViewModel palette)
            val catPalette = listOf(
                0xFF6C63FF, 0xFFFF6584, 0xFF43AA8B, 0xFFFFBE0B,
                0xFF3A86FF, 0xFFFF006E, 0xFF8338EC, 0xFFFB5607,
                0xFF06D6A0, 0xFFEF476F, 0xFF118AB2, 0xFFFFD166,
            )
            val paintCatDot = Paint().apply { style = Paint.Style.FILL }
            val paintBar = Paint().apply { style = Paint.Style.FILL }
            val paintBarBg = Paint().apply {
                color = "#E0E0F0".toColorInt(); style = Paint.Style.FILL
            }
            val barMaxW = 180f
            val barLeft = 330f

            state.categoryRows.forEachIndexed { i, row ->
                ensureSpace(rowH + 4f)
                val dotColor = (catPalette[i % catPalette.size] or 0xFF000000L).toInt()

                // Alternating row background
                if (i % 2 == 0) canvas.drawRect(24f, y - 2f, pageW - 24f, y + rowH - 2f, paintRow)

                // Colored dot
                paintCatDot.color = dotColor
                canvas.drawRoundRect(28f, y + 5f, 40f, y + 15f, 3f, 3f, paintCatDot)

                // Category name + amount
                paintValue.textSize = 10f
                canvas.drawText(row.name.take(24), 44f, y + 14f, paintValue)
                canvas.drawText(fmt(row.amount), 240f, y + 14f, paintValue)

                // Percentage bar track
                canvas.drawRoundRect(barLeft, y + 7f, barLeft + barMaxW, y + 15f, 4f, 4f, paintBarBg)

                // Percentage bar fill
                val fillW = (row.share.coerceIn(0.0F, 1.0F) * barMaxW).toFloat()
                if (fillW > 0f) {
                    paintBar.color = dotColor
                    canvas.drawRoundRect(barLeft, y + 7f, barLeft + fillW, y + 15f, 4f, 4f, paintBar)
                }

                // Percentage label after bar
                paintValue.textSize = 9f
                val pctLabel = "${(row.share * 100).toInt()}%"
                canvas.drawText(pctLabel, barLeft + barMaxW + 6f, y + 14f, paintValue)

                y += rowH
            }
            y += 14f
        }

        // ── Spending Trend chart ──────────────────────────────────────────────
        if (state.trendPoints.isNotEmpty()) {
            val chartH = 120f
            val labelH = 18f
            val yAxisW = 48f
            val chartLeft = 24f + yAxisW
            val chartRight = (pageW - 24).toFloat()
            val chartW = chartRight - chartLeft
            val blockH = 26f + 16f + chartH + labelH + 14f

            ensureSpace(blockH)
            sectionHeader(
                canvas,
                "Spending Trend (${state.trendGranularity.name.lowercase().replaceFirstChar { it.uppercase() }})",
                y,
                pageW,
                paintHeader,
                paintWhite
            )
            y += 26f

            val points = state.trendPoints
            val maxVal = points.maxOf { it.amount }.coerceAtLeast(1.0)

            // Y-axis grid lines + labels
            val paintGrid = Paint().apply {
                color = "#E0E0F0".toColorInt(); strokeWidth = 0.5f
            }
            val paintAxisLabel = Paint().apply {
                color = "#888899".toColorInt(); textSize = 8f; textAlign = Paint.Align.RIGHT
            }
            val steps = 4
            val interval = niceInterval(maxVal, steps)
            var gridVal = 0.0
            val chartBottom = y + chartH
            while (gridVal <= maxVal * 1.05) {
                val gy = chartBottom - (gridVal / maxVal * chartH).toFloat()
                canvas.drawLine(chartLeft, gy, chartRight, gy, paintGrid)
                canvas.drawText(compactAmt(gridVal), chartLeft - 4f, gy + 3f, paintAxisLabel)
                gridVal += interval
            }

            // Bars
            val n = points.size
            val gap = (chartW / n) * 0.25f
            val barW = (chartW / n) - gap
            val paintBarChart = Paint().apply {
                color = "#6C63FF".toColorInt(); style = Paint.Style.FILL
            }
            val paintBarChartDim = Paint().apply {
                color = "#6C63FF33".toColorInt(); style = Paint.Style.FILL
            }
            val paintXLabel = Paint().apply {
                color = "#888899".toColorInt(); textSize = 7.5f; textAlign = Paint.Align.CENTER
            }

            points.forEachIndexed { i, pt ->
                val bLeft = chartLeft + i * (barW + gap) + gap / 2f
                val bRight = bLeft + barW
                val bTop = chartBottom - (pt.amount / maxVal * chartH).toFloat()

                // Dim full-height background bar
                canvas.drawRoundRect(bLeft, y, bRight, chartBottom, 3f, 3f, paintBarChartDim)
                // Filled bar
                if (pt.amount > 0) {
                    canvas.drawRoundRect(bLeft, bTop, bRight, chartBottom, 3f, 3f, paintBarChart)
                }
                // X-axis label (every bar if ≤12, else every other)
                if (n <= 12 || i % 2 == 0) {
                    canvas.drawText(pt.label.take(5), bLeft + barW / 2f, chartBottom + 13f, paintXLabel)
                }
            }

            y = chartBottom + labelH + 14f
        }

        // ── Transactions section ──────────────────────────────────────────────
        if (state.transactions.isNotEmpty()) {
            ensureSpace(60f)
            sectionHeader(canvas, "Transactions", y, pageW, paintHeader, paintWhite); y += 26f
            canvas.drawRect(24f, y, pageW - 24f, y + 18f, paintHeader)
            paintWhite.textSize = 9f
            canvas.drawText("Date", 28f, y + 13f, paintWhite)
            canvas.drawText("Merchant", 100f, y + 13f, paintWhite)
            canvas.drawText("Category", 260f, y + 13f, paintWhite)
            canvas.drawText("Amount", 400f, y + 13f, paintWhite)
            canvas.drawText("Type", 480f, y + 13f, paintWhite)
            y += 20f
            state.transactions.forEach { txn ->
                ensureSpace(20f)
                paintValue.textSize = 9f
                canvas.drawText(txn.dateLabel.take(12), 28f, y + 11f, paintValue)
                canvas.drawText(txn.merchant.take(20), 100f, y + 11f, paintValue)
                canvas.drawText(txn.category.take(18), 260f, y + 11f, paintValue)
                val amtP = if (txn.isExpense) paintExpense else paintIncome2
                amtP.textSize = 9f
                canvas.drawText(fmt(txn.amount), 400f, y + 11f, amtP)
                paintValue.textSize = 9f
                canvas.drawText(if (txn.isExpense) "Expense" else "Income", 480f, y + 11f, paintValue)
                y += 16f
                canvas.drawLine(24f, y, pageW - 24f, y, paintDivider)
            }
        }

        // Footer on last page
        paintSub.textSize = 8f
        canvas.drawText("Generated by TruXpense  ·  ${state.dateRangeLabel}", 24f, pageH - 16f, paintSub)

        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        return ExportResult(filePath = file.absolutePath, mimeType = "application/pdf", fileName = fileName)
    }

    private fun sectionHeader(canvas: Canvas, title: String, y: Float, pageW: Int, bg: Paint, fg: Paint) {
        canvas.drawRect(24f, y, pageW - 24f, y + 18f, bg)
        fg.textSize = 10f; fg.isFakeBoldText = true
        canvas.drawText(title, 28f, y + 13f, fg)
    }

    /** Picks a clean round Y-axis interval (1/2/5 × magnitude). */
    private fun niceInterval(maxVal: Double, steps: Int): Double {
        val raw = maxVal / steps
        val mag = Math.pow(10.0, Math.floor(Math.log10(raw)))
        return when {
            raw / mag <= 1.0 -> mag
            raw / mag <= 2.0 -> 2.0 * mag
            raw / mag <= 5.0 -> 5.0 * mag
            else -> 10.0 * mag
        }
    }

    /** Formats axis labels as ₹2K, ₹1L, etc. */
    private fun compactAmt(value: Double): String = when {
        value >= 1_00_000.0 -> "₹${"%.1f".format(value / 1_00_000.0)}L"
        value >= 1_000.0 -> "₹${"%.0f".format(value / 1_000.0)}K"
        else -> "₹${"%.0f".format(value)}"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CSV
    // ─────────────────────────────────────────────────────────────────────────

    private fun writeCsv(dir: File, safeName: String, state: ReportDetailUiState): ExportResult {
        val fileName = "$safeName.csv"
        val file = File(dir, fileName)
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, Charsets.UTF_8).buffered().use { w ->
                w.write("TruXpense Report\n")
                w.write("Title,${csvEsc(state.title)}\n")
                w.write("Date Range,${csvEsc(state.dateRangeLabel)}\n")
                w.write("Report Type,${state.reportType.name}\n\n")

                w.write("SUMMARY\n")
                if (state.reportType != ReportType.INCOME) w.write("Total Expenses,${fmt(state.totalExpenses)}\n")
                if (state.reportType != ReportType.EXPENSE) w.write("Total Income,${fmt(state.totalIncome)}\n")
                if (state.reportType == ReportType.ALL) w.write("Net Balance,${fmt(state.netAmount)}\n")
                w.write("Total Transactions,${state.transactionCount}\n")
                w.write("Avg Daily Spend,${fmt(state.avgDailySpend)}\n")
                state.topCategory?.let { w.write("Top Category,${csvEsc(it.first)}\n") }
                state.topMerchant?.let { w.write("Top Merchant,${csvEsc(it.first)}\n") }
                state.highestSpendDay?.let { w.write("Highest Spend Day,${csvEsc(it.first)},${fmt(it.second)}\n") }

                if (state.categoryRows.isNotEmpty()) {
                    w.write("\nCATEGORY BREAKDOWN\n")
                    w.write("Category,Amount,Share%\n")
                    state.categoryRows.forEach { row ->
                        w.write("${csvEsc(row.name)},${fmt(row.amount)},${(row.share * 100).toInt()}%\n")
                    }
                }

                if (state.transactions.isNotEmpty()) {
                    w.write("\nTRANSACTIONS\n")
                    w.write("Date,Time,Merchant,Category,Amount,Type,Payment Method\n")
                    state.transactions.forEach { txn ->
                        w.write(
                            "${csvEsc(txn.dateLabel)},${csvEsc(txn.timeLabel)},${csvEsc(txn.merchant)}," + "${csvEsc(txn.category)},${
                                fmt(
                                    txn.amount
                                )
                            }," + "${if (txn.isExpense) "Expense" else "Income"},${csvEsc(txn.paymentMethod)}\n"
                        )
                    }
                }
            }
        }
        return ExportResult(filePath = file.absolutePath, mimeType = "text/csv", fileName = fileName)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Excel (XML SpreadsheetML — opens natively in Excel / Google Sheets)
    // ─────────────────────────────────────────────────────────────────────────

    private fun writeExcel(dir: File, safeName: String, state: ReportDetailUiState): ExportResult {
        val fileName = "$safeName.xls"
        val file = File(dir, fileName)
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, Charsets.UTF_8).buffered().use { w ->
                w.write(
                    "<?xml version=\"1.0\"?>\n" + "<?mso-application progid=\"Excel.Sheet\"?>\n" + "<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n" + "          xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n" + "  <Worksheet ss:Name=\"Report\">\n    <Table>\n"
                )

                fun row(vararg cells: String) {
                    w.write("      <Row>\n")
                    cells.forEach { c ->
                        val clean = c.replace(",", "").replace("₹", "").replace("%", "").trim()
                        val isNum = clean.toDoubleOrNull() != null
                        val type = if (isNum) "Number" else "String"
                        val value = if (isNum) clean else xmlEsc(c)
                        w.write("        <Cell><Data ss:Type=\"$type\">$value</Data></Cell>\n")
                    }
                    w.write("      </Row>\n")
                }

                row("TruXpense Report")
                row("Title", state.title)
                row("Date Range", state.dateRangeLabel)
                row("Report Type", state.reportType.name)
                row()

                row("SUMMARY")
                if (state.reportType != ReportType.INCOME) row("Total Expenses", fmt(state.totalExpenses))
                if (state.reportType != ReportType.EXPENSE) row("Total Income", fmt(state.totalIncome))
                if (state.reportType == ReportType.ALL) row("Net Balance", fmt(state.netAmount))
                row("Total Transactions", state.transactionCount.toString())
                row("Avg Daily Spend", fmt(state.avgDailySpend))
                state.topCategory?.let { row("Top Category", it.first) }
                state.topMerchant?.let { row("Top Merchant", it.first) }
                state.highestSpendDay?.let { row("Highest Spend Day", it.first, fmt(it.second)) }

                if (state.categoryRows.isNotEmpty()) {
                    row(); row("CATEGORY BREAKDOWN")
                    row("Category", "Amount", "Share %")
                    state.categoryRows.forEach { r -> row(r.name, fmt(r.amount), "${(r.share * 100).toInt()}%") }
                }

                if (state.transactions.isNotEmpty()) {
                    row(); row("TRANSACTIONS")
                    row("Date", "Time", "Merchant", "Category", "Amount", "Type", "Payment Method")
                    state.transactions.forEach { txn ->
                        row(
                            txn.dateLabel,
                            txn.timeLabel,
                            txn.merchant,
                            txn.category,
                            fmt(txn.amount),
                            if (txn.isExpense) "Expense" else "Income",
                            txn.paymentMethod
                        )
                    }
                }

                w.write("    </Table>\n  </Worksheet>\n</Workbook>\n")
            }
        }
        return ExportResult(filePath = file.absolutePath, mimeType = "application/vnd.ms-excel", fileName = fileName)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun fmt(amount: Double): String {
        val nf = NumberFormat.getInstance(Locale.US)
        nf.maximumFractionDigits = 2
        nf.minimumFractionDigits = 2
        return nf.format(abs(amount))
    }

    private fun csvEsc(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) "\"${value.replace("\"", "\"\"")}\""
        else value

    private fun xmlEsc(value: String): String =
        value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}