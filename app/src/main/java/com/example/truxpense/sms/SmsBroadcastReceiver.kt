package com.example.truxpense.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.truxpense.data.repository.sms.PendingTransactionRepository
import com.example.truxpense.data.sms.parser.SmsParserEngine
import com.example.truxpense.service.TransactionNotifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Listens for incoming SMS messages and kicks off the expense-parsing pipeline.
 *
 * Declared in AndroidManifest.xml with:
 * ```xml
 * <receiver
 *     android:name=".sms.SmsBroadcastReceiver"
 *     android:enabled="true"
 *     android:exported="true"
 *     android:permission="android.permission.BROADCAST_SMS">
 *     <intent-filter android:priority="999">
 *         <action android:name="android.provider.Telephony.SMS_RECEIVED" />
 *     </intent-filter>
 * </receiver>
 * ```
 *
 * ## Threading
 * [BroadcastReceiver.onReceive] runs on the main thread with a 10-second hard deadline.
 * We use [goAsync] to extend this to ~30 seconds while doing IO-safe work on [Dispatchers.IO].
 * Parsing is CPU-bound but fast (<10 ms per SMS), so it runs inline inside the coroutine.
 *
 * ## Privacy
 * Raw SMS bodies are stored in the local Room DB only. They are never transmitted to the backend.
 */
@AndroidEntryPoint
class SmsBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var smsParser: SmsParserEngine

    @Inject
    lateinit var repository: PendingTransactionRepository

    @Inject
    lateinit var notifier: TransactionNotifier

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            ?: return

        // goAsync() tells the system we're not done when onReceive() returns.
        val pendingResult = goAsync()

        scope.launch {
            try {
                for (sms in messages) {
                    val sender = sms.originatingAddress ?: continue
                    val body   = sms.messageBody        ?: continue

                    // Fast pre-filter — skip non-financial SMS without full parse
                    if (!smsParser.isBankSms(sender, body)) continue

                    Log.d(TAG, "Bank SMS detected from $sender — parsing...")

                    val parsed = smsParser.parse(sender, body)
                    if (parsed == null) {
                        Log.w(TAG, "Parser returned null for sender=$sender (amount not found)")
                        continue
                    }

                    Log.d(TAG, "Parsed: amount=${parsed.amount} type=${parsed.type} " +
                            "merchant=${parsed.merchant} category=${parsed.category} " +
                            "confidence=${parsed.confidence}")

                    repository.savePending(parsed)
                    notifier.showReviewNotification(parsed)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
            } finally {
                // Always call finish() — missing this causes an ANR
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SmsBroadcastReceiver"
    }
}