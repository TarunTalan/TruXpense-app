package com.example.truxpense.data.sms.model

enum class Category(val label: String, val emoji: String) {
    FOOD("Food & Dining", "🍕"),
    GROCERIES("Groceries", "🛒"),
    TRANSPORT("Transport", "🚗"),
    SHOPPING("Shopping", "🛍️"),
    HEALTH("Health", "💊"),
    ENTERTAINMENT("Entertainment", "🎬"),
    TRAVEL("Travel", "✈️"),
    RENT("Rent & Housing", "🏠"),
    UTILITIES("Utilities", "💡"),
    EDUCATION("Education", "📚"),
    SALARY("Salary", "💰"),
    INVESTMENTS("Investments", "📈"),
    FITNESS("Fitness", "🏋️"),
    EMI("EMI / Loan", "💳"),
    GIFTS("Gifts & Donations", "🎁"),
    TRANSFER("Transfer", "💸"),
    RECHARGE("Recharge", "📱"),
    OTHER("Other", "❓");

    val displayName: String get() = "$emoji $label"
}

