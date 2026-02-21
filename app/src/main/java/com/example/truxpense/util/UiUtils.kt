package com.example.truxpense.util

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

fun Double.toCurrency(fmt: NumberFormat) = runCatching { fmt.format(this) }.getOrDefault("$this")

