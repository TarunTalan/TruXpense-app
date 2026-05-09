package com.example.truxpense.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.truxpense.data.sms.model.Category
import com.example.truxpense.data.sms.model.ParsedTransaction
import com.example.truxpense.data.sms.model.TxnState
import com.example.truxpense.data.sms.model.TxnType

@Entity(
    tableName = "pending_transactions",
    indices = [Index(value = ["timestamp", "state"])])
data class PendingTransactionEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    @ColumnInfo(name = "txn_type") val txnType: String,
    val merchant: String?,
    val category: String,
    val confidence: Float,
    val balance: Double?,
    @ColumnInfo(name = "account_last4") val accountLast4: String?,
    val bank: String,
    @ColumnInfo(name = "raw_sms") val rawSms: String,
    val timestamp: Long,
    val state: String
) {
    fun toDomain(): ParsedTransaction = ParsedTransaction(
        id = id,
        amount = amount,
        type = TxnType.valueOf(txnType),
        merchant = merchant,
        category = Category.valueOf(category),
        confidence = confidence,
        balance = balance,
        accountLast4 = accountLast4,
        bank = bank,
        rawSms = rawSms,
        timestamp = timestamp,
        state = TxnState.valueOf(state)
    )

    companion object {
        fun fromDomain(tx: ParsedTransaction): PendingTransactionEntity = PendingTransactionEntity(
            id = tx.id,
            amount = tx.amount,
            txnType = tx.type.name,
            merchant = tx.merchant,
            category = tx.category.name,
            confidence = tx.confidence,
            balance = tx.balance,
            accountLast4 = tx.accountLast4,
            bank = tx.bank,
            rawSms = tx.rawSms,
            timestamp = tx.timestamp,
            state = tx.state.name
        )
    }
}

