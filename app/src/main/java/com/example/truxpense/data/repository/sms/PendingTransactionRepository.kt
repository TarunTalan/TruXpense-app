package com.example.truxpense.data.repository.sms

import com.example.truxpense.data.local.dao.PendingTransactionDao
import com.example.truxpense.data.local.entity.PendingTransactionEntity
import com.example.truxpense.data.sms.model.Category
import com.example.truxpense.data.sms.model.ParsedTransaction
import com.example.truxpense.data.sms.model.TxnState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingTransactionRepository @Inject constructor(
    private val dao: PendingTransactionDao
) {

    val pendingTransactions: Flow<List<ParsedTransaction>> =
        dao.observePending().map { list -> list.map { it.toDomain() } }

    val pendingCount: Flow<Int> = dao.observePendingCount()

    suspend fun savePending(transaction: ParsedTransaction) {
        val isDupe = dao.isDuplicate(
            amount = transaction.amount,
            timestamp = transaction.timestamp,
            txnType = transaction.type.name
        )
        if (!isDupe) {
            dao.insert(PendingTransactionEntity.fromDomain(transaction))
        }
    }

    suspend fun saveAllPending(transactions: List<ParsedTransaction>) {
        dao.insertAll(transactions.map { PendingTransactionEntity.fromDomain(it) })
    }

    suspend fun confirm(id: String, category: Category? = null) {
        if (category != null) {
            dao.confirmWithCategory(id, category.name, TxnState.CONFIRMED.name)
        } else {
            dao.updateState(id, TxnState.CONFIRMED.name)
        }
    }

    suspend fun reject(id: String) {
        dao.updateState(id, TxnState.REJECTED.name)
    }

    suspend fun purgeRejected() {
        dao.deleteByState(TxnState.REJECTED.name)
    }

    suspend fun getPendingCount(): Int = dao.getPendingCount()

    suspend fun exists(id: String): Boolean = dao.exists(id)

    suspend fun getConfirmedForSync(limit: Int = 50): List<ParsedTransaction> =
        dao.getConfirmedForSync(limit).map { it.toDomain() }
}

