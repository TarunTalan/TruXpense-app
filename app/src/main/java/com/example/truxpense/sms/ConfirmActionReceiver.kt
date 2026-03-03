package com.example.truxpense.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.truxpense.data.repository.sms.PendingTransactionRepository
import com.example.truxpense.service.TransactionNotifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles the inline notification action buttons (Confirm / Ignore) posted by [TransactionNotifier].
 *
 * Registered in AndroidManifest.xml so it can receive broadcasts even when the app is not running.
 */
@AndroidEntryPoint
class ConfirmActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: PendingTransactionRepository

    @Inject
    lateinit var notifier: TransactionNotifier

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val txnId = intent.getStringExtra(TransactionNotifier.EXTRA_TXN_ID) ?: return
        val async = goAsync()

        scope.launch {
            try {
                when (intent.action) {
                    TransactionNotifier.ACTION_CONFIRM -> repository.confirm(txnId)
                    TransactionNotifier.ACTION_REJECT  -> repository.reject(txnId)
                }
                notifier.cancelForTransaction(txnId)
                // Update the group summary count after action
                val remaining = repository.getPendingCount()
                notifier.updateSummaryNotification(remaining)
            } finally {
                async.finish()
            }
        }
    }
}