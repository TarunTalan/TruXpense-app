package com.example.truxpense.data.sms.model


/**
 * Transient domain object produced by SmsParserEngine.
 */
data class ParsedTransaction(
    val id: String,
    val amount: Double,
    val type: TxnType,
    val merchant: String?,
    val category: Category,
    val confidence: Float,
    val balance: Double?,
    val accountLast4: String?,
    val bank: String,
    val rawSms: String,
    val timestamp: Long,
    val state: TxnState = TxnState.PENDING,
    val rememberForMerchant: Boolean = false
)

