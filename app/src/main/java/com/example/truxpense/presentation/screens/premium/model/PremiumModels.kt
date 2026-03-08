package com.example.truxpense.presentation.screens.premium.model

import androidx.annotation.DrawableRes
import com.example.truxpense.R

// ─── Enums ────────────────────────────────────────────────────────────────────

enum class PaymentMethod { CARD, UPI, NETBANKING }

enum class PlanType(
    val label: String,
    val price: String,
    val priceNumeric: String,
    val subLabel: String,
    /** Short billing period shown in the header trial text, e.g. "month" or "year". */
    val period: String,
    /** Non-null renders a highlighted badge above the price chip. Null = no badge. */
    val badgeLabel: String?,
) {
    MONTHLY(
        label = "Monthly",
        price = "₹149",
        priceNumeric = "149",
        subLabel = "per month",
        period = "month",
        badgeLabel = null,
    ),
    ANNUAL(
        label = "Annual",
        price = "₹999",
        priceNumeric = "999",
        subLabel = "per year·Save 44%",
        period = "year",
        badgeLabel = "BEST VALUE",
    ),
}

// ─── Form state ───────────────────────────────────────────────────────────────

data class CardFormState(
    val number: String = "",
    val expiry: String = "",
    val cvv: String = "",
    val name: String = "",
)

data class FormErrors(
    val cardNumber: String? = null,
    val cardExpiry: String? = null,
    val cardCvv: String? = null,
    val cardName: String? = null,
    val upiId: String? = null,
    val bank: String? = null,
) {
    val hasErrors: Boolean
        get() = listOf(cardNumber, cardExpiry, cardCvv, cardName, upiId, bank).any { it != null }
}

// ─── Content models ───────────────────────────────────────────────────────────

data class BenefitSection(
    @DrawableRes val iconRes: Int,
    val title: String,
    val points: List<String>,
)

data class FeatureRow(
    val name: String,
    val free: FeatureCell,
    val premium: FeatureCell,
)

sealed interface FeatureCell {
    data object Check : FeatureCell
    data object Minus : FeatureCell
    data class Label(val text: String) : FeatureCell
}

// ─── Static content ───────────────────────────────────────────────────────────

val premiumBenefits: List<BenefitSection> = listOf(
    BenefitSection(
        iconRes = R.drawable.automation,
        title = "Automate your tracking",
        points = listOf(
            "Automatic SMS categorization",
            "UPI payment captured instantly",
            "Smart merchant detection",
        ),
    ),
    BenefitSection(
        iconRes = R.drawable.query_stats,
        title = "Advanced Analysis",
        points = listOf(
            "Custom date comparisons",
            "Year-over-year performance tracking",
            "Export PDF/CSV reports",
        ),
    ),
    BenefitSection(
        iconRes = R.drawable.assignment_globe,
        title = "Strategic financial planning",
        points = listOf(
            "Set and track savings goals",
            "Forecasts budget overruns",
            "Predictive overspending alerts",
        ),
    ),
)

val premiumFeatureRows: List<FeatureRow> = listOf(
    FeatureRow("Expense tracking", FeatureCell.Check, FeatureCell.Check),
    FeatureRow("SMS auto-parsing", FeatureCell.Label("5/per days"), FeatureCell.Label("Unlimited")),
    FeatureRow("Advanced charts", FeatureCell.Minus, FeatureCell.Check),
    FeatureRow("Trends history", FeatureCell.Minus, FeatureCell.Check),
    FeatureRow("Export reports", FeatureCell.Minus, FeatureCell.Check),
)

val popularBanks: List<String> = listOf(
    "HDFC Bank",
    "SBI",
    "ICICI Bank",
    "Axis Bank",
    "Kotak Bank",
    "Yes Bank",
)