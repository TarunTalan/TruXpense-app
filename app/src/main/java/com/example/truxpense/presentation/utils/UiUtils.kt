package com.example.truxpense.presentation.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs

fun progressColor(progress: Float, errorColor: Color): Color {
    val p = progress.coerceIn(0f, 1f)
    return when {
        p <= 0.4f -> lerp(Color(0xFF4CAF50), Color(0xFFFFC107), p / 0.4f)
        p <= 0.6f -> lerp(Color(0xFFFFC107), Color(0xFFFFA726), (p - 0.4f) / 0.2f)
        p <= 0.8f -> lerp(Color(0xFFFFA726), errorColor, (p - 0.6f) / 0.2f)
        else -> errorColor
    }
}

fun formatAmountParts(amount: Double, currencyCode: String?): Triple<String, String, String> {
    val sign = if (amount < 0) "-" else ""
    val absAmt = abs(amount)
    val (value, suffix) = when {
        absAmt >= 1_000_000 -> absAmt / 1_000_000.0 to "M"
        absAmt >= 1_000 -> absAmt / 1_000.0 to "K"
        else -> absAmt to ""
    }
    val numeric = if (suffix.isNotEmpty()) {
        String.format(Locale.ENGLISH, "%.2f", value)
    } else {
        var s = String.format(Locale.ENGLISH, "%.2f", value)
        if (s.contains('.')) s = s.trimEnd('0').trimEnd('.')
        s
    }
    val symbol = runCatching {
        Currency.getInstance(currencyCode ?: "INR").getSymbol(Locale.getDefault())
    }.getOrDefault(currencyCode ?: "INR")

    return Triple(if (symbol.isNotBlank()) sign + symbol else sign, numeric, suffix)
}

fun currencyFormat(currencyCode: String?): NumberFormat =
    runCatching {
        val locale = Locale.Builder().setLanguage("en").setRegion("IN").build()
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(currencyCode ?: "INR")
        }
    }.getOrElse { NumberFormat.getCurrencyInstance() }

fun Double.toCurrency(fmt: NumberFormat): String = runCatching {
    // Format using provided NumberFormat then strip unnecessary fractional zeros
    val formatted = fmt.format(this)
    val decimalSep = (fmt as? java.text.DecimalFormat)?.decimalFormatSymbols?.decimalSeparator ?: '.'

    // Common case: ends with decimalSep + two zeros (e.g. ".00" or ",00")
    val zeroSuffix = "${decimalSep}00"
    if (formatted.endsWith(zeroSuffix)) {
        return@runCatching formatted.substring(0, formatted.length - zeroSuffix.length)
    }

    if (!formatted.contains(decimalSep)) return@runCatching formatted

    // Otherwise trim trailing zeros after decimal separator (e.g. 123.40 -> 123.4, 123.00 handled above)
    var s = formatted
    while (s.isNotEmpty() && s.last() == '0') s = s.dropLast(1)
    if (s.isNotEmpty() && s.last() == decimalSep) s = s.dropLast(1)
    s
}.getOrDefault("$this")
