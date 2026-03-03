package com.example.truxpense.data.sms.parser

import com.example.truxpense.data.sms.model.ParsedTransaction
import com.example.truxpense.data.sms.model.TxnState
import com.example.truxpense.data.sms.model.TxnType
import com.example.truxpense.ml.MlCategorizer
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device SMS parsing engine.
 *
 * All work is pure (no I/O), so it is safe to call from a BroadcastReceiver's
 * [android.content.BroadcastReceiver.goAsync] coroutine on [kotlinx.coroutines.Dispatchers.Default].
 *
 * ## Parsing pipeline
 * 1. [isBankSms]      — fast pre-filter before full parse
 * 2. [extractAmount]  — required field; returns null → skip
 * 3. [detectTxnType]  — DEBIT / CREDIT / REFUND / UNKNOWN
 * 4. [extractMerchant]
 * 5. [extractBalance]
 * 6. [extractAccount]
 * 7. [identifyBank]
 * 8. [MlCategorizer.categorize]
 */
@Singleton
class SmsParserEngine @Inject constructor(
    private val categorizer: MlCategorizer
) {

    // ── Sender ID allowlist ───────────────────────────────────────────────
    private val bankSenders: Set<String> = setOf(
        "HDFCBK", "SBIINB", "ICICIB", "AXISBK", "KOTAKB",
        "PAYTMB", "PHONPE", "YESBNK", "INDUSB", "PNBSMS",
        "BOISMS", "CANBNK", "UNION1", "IDBIBK", "SCBANK",
        "CITIBK", "HSBC", "RBLBNK", "FEDERAL",
        "GPAY", "BHIMUPI", "UPIBNK"
    )

    // ── Amount patterns ───────────────────────────────────────────────────
    private val amountPatterns: List<Regex> = listOf(
        Regex("""(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""([\d,]+(?:\.\d{1,2})?)\s*(?:Rs\.?|INR|₹)""", RegexOption.IGNORE_CASE),
        Regex("""debited\s+(?:by|for|of)?\s*(?:Rs\.?|INR|₹)?\s*([\d,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
        Regex("""credited\s+(?:with|by|of)?\s*(?:Rs\.?|INR|₹)?\s*([\d,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
        Regex("""Dr\.\s*(?:Rs\.?|INR|₹)?\s*([\d,]+(?:\.\d{2})?)\/?\-?""", RegexOption.IGNORE_CASE)
    )

    // ── Merchant patterns ─────────────────────────────────────────────────
    private val merchantPatterns: List<Regex> = listOf(
        Regex("""(?:\bat\b|\bto\b|\btowards\b|\bpaid\s+to\b)\s+([A-Z][A-Za-z0-9 &\-\.]{2,35})""", RegexOption.IGNORE_CASE),
        Regex("""trf\s+to\s+([A-Z][A-Za-z0-9 &\-\.]{2,35})""", RegexOption.IGNORE_CASE),
        Regex("""(?:Info|Ref|UPI)[:/]\s*(?:UPI/)?([A-Z][A-Za-z0-9 &\-\.]{2,30})""", RegexOption.IGNORE_CASE),
        Regex("""[Mm]erchant:\s*([A-Za-z0-9 &\-\.]{3,35})"""),
        Regex("""for\s+([A-Z][A-Z0-9 &\-\.]{2,35})\s*\.""")
    )

    // ── Balance pattern ───────────────────────────────────────────────────
    private val balancePattern: Regex = Regex(
        """(?:Avl\s*Bal|Avail(?:able)?\s*(?:Bal(?:ance)?)?|Balance|Bal)\s*:?\s*(?:Rs\.?|INR|₹)?\s*([\d,]+(?:\.\d{2})?)""",
        RegexOption.IGNORE_CASE
    )

    // ── Account last-4 pattern ────────────────────────────────────────────
    private val accountPattern: Regex = Regex(
        """(?:A\/c|acct?|account|card|AC|XX)\s*[\*xX]*(\d{4})""",
        RegexOption.IGNORE_CASE
    )

    // ── Bank identification ───────────────────────────────────────────────
    private val bankIdentifiers: List<Pair<List<String>, String>> = listOf(
        listOf("HDFCBK", "HDFC")                  to "HDFC Bank",
        listOf("SBIINB", "SBI", "State Bank")      to "SBI",
        listOf("ICICIB", "ICICI")                  to "ICICI Bank",
        listOf("AXISBK", "AXIS")                   to "Axis Bank",
        listOf("KOTAKB", "KOTAK")                  to "Kotak Bank",
        listOf("PAYTMB", "PAYTM")                  to "Paytm",
        listOf("PHONPE", "PHONEPE", "PhonePe")     to "PhonePe",
        listOf("GPAY", "Google Pay")               to "Google Pay",
        listOf("YESBNK", "YES BANK")               to "Yes Bank",
        listOf("INDUSB", "INDUSIND")               to "IndusInd Bank",
        listOf("PNBSMS", "PNB")                    to "Punjab National Bank",
        listOf("CANBNK", "CANARA")                 to "Canara Bank",
        listOf("BOISMS", "BANK OF INDIA")          to "Bank of India",
        listOf("IDBIBK", "IDBI")                   to "IDBI Bank",
        listOf("RBLBNK", "RBL")                    to "RBL Bank",
        listOf("FEDERAL", "FEDERAL BANK")          to "Federal Bank"
    ).map { (keys, name) -> keys to name }

    // ─────────────────────────────────────────────────────────────────────

    /** Fast pre-filter — avoids full parse for non-financial SMS. */
    fun isBankSms(sender: String, body: String): Boolean {
        val senderUpper = sender.uppercase()
        if (bankSenders.any { senderUpper.contains(it) }) return true
        val lowerBody = body.lowercase()
        val hasAmount = lowerBody.contains("rs.") || lowerBody.contains("inr ") || lowerBody.contains("₹")
        val hasTxn    = lowerBody.contains("debited") || lowerBody.contains("credited") ||
                        lowerBody.contains("spent")   || lowerBody.contains("paid")
        return hasAmount && hasTxn
    }

    /** Full parse — returns null when amount cannot be extracted. */
    fun parse(sender: String, body: String): ParsedTransaction? {
        val amount   = extractAmount(body)  ?: return null
        val txnType  = detectTxnType(body)
        val merchant = extractMerchant(body)
        val balance  = extractBalance(body)
        val account  = extractAccount(body)
        val bank     = identifyBank(sender, body)
        val (category, confidence) = categorizer.categorize(merchant, body)

        return ParsedTransaction(
            id           = UUID.randomUUID().toString(),
            amount       = amount,
            type         = txnType,
            merchant     = merchant,
            category     = category,
            confidence   = confidence,
            balance      = balance,
            accountLast4 = account,
            bank         = bank,
            rawSms       = body,
            timestamp    = System.currentTimeMillis(),
            state        = TxnState.PENDING
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private fun extractAmount(body: String): Double? {
        for (pattern in amountPatterns) {
            val raw = pattern.find(body)?.groupValues?.getOrNull(1) ?: continue
            val amount = raw.replace(",", "").toDoubleOrNull()
            if (amount != null && amount > 0 && amount < 5_000_000) return amount
        }
        return null
    }

    internal fun detectTxnType(body: String): TxnType {
        val lower = body.lowercase()
        return when {
            lower.contains("refund") || lower.contains("reversal")                         -> TxnType.REFUND
            lower.contains("credited") || lower.contains("received") ||
                    lower.contains("deposited")                                            -> TxnType.CREDIT
            lower.contains("debited") || lower.contains("spent") ||
                    lower.contains("paid") || lower.contains("sent") ||
                    lower.contains("dr.")                                                  -> TxnType.DEBIT
            else                                                                           -> TxnType.UNKNOWN
        }
    }

    private fun extractMerchant(body: String): String? {
        for (pattern in merchantPatterns) {
            val raw = pattern.find(body)?.groupValues?.getOrNull(1)?.trim() ?: continue
            if (raw.length < 3) continue
            if (raw.all { it.isDigit() || it == '/' || it == '-' }) continue
            return raw.split(" ").joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercaseChar() } }
                      .trimEnd('.', ',', '-', '/')
        }
        return null
    }

    private fun extractBalance(body: String): Double? =
        balancePattern.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

    private fun extractAccount(body: String): String? =
        accountPattern.find(body)?.groupValues?.get(1)

    private fun identifyBank(sender: String, body: String): String {
        val combined = (sender + " " + body).uppercase()
        for ((keys, name) in bankIdentifiers) {
            if (keys.any { combined.contains(it.uppercase()) }) return name
        }
        return "Unknown Bank"
    }
}

