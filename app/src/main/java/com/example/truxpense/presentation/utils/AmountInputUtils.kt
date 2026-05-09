package com.example.truxpense.presentation.utils

/** Maximum amount the user is allowed to enter (100 million / 10 Cr). */
const val MAX_AMOUNT = 100_000_000.0   // 100 M / 10 Cr

// ── Thresholds ────────────────────────────────────────────────────────────────
private const val LAKH   = 100_000.0        // 1 L
private const val CRORE  = 10_000_000.0     // 1 Cr
private const val MILLION = 1_000_000.0     // 1 M
private const val BILLION = 1_000_000_000.0 // 1 B

/**
 * Sanitizes a raw amount string typed by the user.
 *
 * Rules applied in order:
 * 1. Strip every character that is NOT a digit or a decimal point.
 * 2. Allow at most one decimal point.
 * 3. Strip ALL leading zeros from the integer part.
 * 4. Cap the numeric value at MAX_AMOUNT (100 000 000).
 */
fun sanitizeAmountInput(raw: String): String {
    if (raw.isEmpty()) return ""

    var hasDecimal = false
    val filtered = buildString {
        for (ch in raw) {
            when {
                ch.isDigit() -> append(ch)
                ch == '.' && !hasDecimal -> { hasDecimal = true; append(ch) }
            }
        }
    }
    if (filtered.isEmpty()) return ""

    val dotIndex = filtered.indexOf('.')
    val intPart = if (dotIndex == -1) filtered else filtered.substring(0, dotIndex)
    val decPart = if (dotIndex == -1) "" else filtered.substring(dotIndex)
    val cleanInt = intPart.trimStart('0')

    val assembled = when {
        cleanInt.isEmpty() && decPart.isEmpty() -> return ""
        cleanInt.isEmpty() && decPart.isNotEmpty() -> "0$decPart"
        else -> cleanInt + decPart
    }

    val numeric = assembled.toDoubleOrNull()
    if (numeric != null && numeric > MAX_AMOUNT) {
        return MAX_AMOUNT.toLong().toString()
    }
    return assembled
}

/**
 * Returns the integer-only display string for the large amount widget.
 *   ""       → "0"
 *   "123.45" → "123"
 */
fun amountDisplayText(raw: String): String {
    if (raw.isEmpty()) return "0"
    val dotIndex = raw.indexOf('.')
    val intPart = if (dotIndex == -1) raw else raw.substring(0, dotIndex)
    return if (intPart.isEmpty()) "0" else intPart
}


fun amountAbbreviationHint(raw: String, currencyCode: String = "INR"): String? {
    val value = raw.toDoubleOrNull() ?: return null
    return if (currencyCode.uppercase() == "INR") {
        when {
            value >= CRORE -> "≈ ${"%.2f".format(value / CRORE)}Cr"
            value >= LAKH  -> "≈ ${"%.2f".format(value / LAKH)}L"
            else           -> null
        }
    } else {
        when {
            value >= BILLION -> "≈ ${"%.2f".format(value / BILLION)}B"
            value >= MILLION -> "≈ ${"%.2f".format(value / MILLION)}M"
            else             -> null
        }
    }
}


fun formatAbbreviatedAmount(amount: Double, currencyCode: String = "INR"): String {
    val abs = kotlin.math.abs(amount)
    return if (currencyCode.uppercase() == "INR") {
        when {
            abs >= CRORE -> "${"%.2f".format(abs / CRORE)}Cr"
            abs >= LAKH  -> "${"%.2f".format(abs / LAKH)}L"
            else         -> "%,.0f".format(abs)
        }
    } else {
        when {
            abs >= BILLION -> "${"%.2f".format(abs / BILLION)}B"
            abs >= MILLION -> "${"%.2f".format(abs / MILLION)}M"
            else           -> "%,.0f".format(abs)
        }
    }
}
