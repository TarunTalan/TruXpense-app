package com.example.truxpense.data.sms.model

enum class TxnType(val label: String) {
    DEBIT("Debited"),
    CREDIT("Credited"),
    REFUND("Refund"),
    UNKNOWN("Unknown")
}

