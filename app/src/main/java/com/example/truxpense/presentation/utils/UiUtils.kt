package com.example.truxpense.presentation.utils

import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs

/**
 * Returns the interval-based bar/accent color for a budget progress value.
 *
 * Matches the 3-interval theme used across budget cards:
 *   >= 80%  → danger  red    #D64545
 *   >= 45%  → warning amber  #F2C06A
 *   < 45%   → safe    teal   #1B6F73
 */
fun progressColor(progress: Float, @Suppress("UNUSED_PARAMETER") errorColor: Color = Color.Unspecified): Color {
    val p = progress.coerceIn(0f, 1f)
    return when {
        p >= 0.80f -> Color(0xFFD64545)
        p >= 0.45f -> Color(0xFFF2C06A)
        else       -> Color(0xFF1B6F73)
    }
}

/**
 * Returns the interval-based background color for a budget card (light-mode).
 */
fun progressBgColor(progress: Float): Color {
    val p = progress.coerceIn(0f, 1f)
    return when {
        p >= 0.80f -> Color(0xFFFDECEC)
        p >= 0.45f -> Color(0xFFFFF4E5)
        else       -> Color(0xFFE6F4F2)
    }
}

/**
 * Returns the interval-based border color for a budget card (light-mode).
 */
fun progressBorderColor(progress: Float): Color {
    val p = progress.coerceIn(0f, 1f)
    return when {
        p >= 0.80f -> Color(0xFF9C2D2D)
        p >= 0.45f -> Color(0xFFB3741A)
        else       -> Color(0xFF1B6F73)
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
