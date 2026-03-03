package com.example.truxpense.ml

import android.content.Context
import android.content.SharedPreferences
import com.example.truxpense.data.sms.model.Category
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 3-tier merchant → expense category classifier.
 *
 * **Tier 1 — Exact merchant lookup** (confidence 0.97)
 *   Hard-coded map of 50+ popular Indian merchants. Instant, highest confidence.
 *
 * **Tier 2 — Keyword / regex rules** (confidence 0.80)
 *   Pattern list covering generic terms ("restaurant", "fuel", "pharmacy", …).
 *
 * **Tier 3 — Learned preferences** (confidence 0.75)
 *   SharedPreferences persisting corrections made via the Category Edit Sheet (S-03).
 *   When the user checks "Remember for [Merchant]", the choice is stored here and
 *   takes priority on every subsequent parse for that merchant.
 *
 * A future version can replace Tier 2 + 3 with an on-device TFLite NaiveBayes model
 * without changing the public API.
 */
@Singleton
class MlCategorizer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PREFS_NAME = "sms_merchant_overrides"
        private const val MIN_CONFIDENCE_AUTO_CONFIRM = 0.92f
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ── Tier 1: Exact merchant name → category map ────────────────────────
    private val merchantMap: Map<String, Category> = mapOf(
        // Food & Dining
        "swiggy" to Category.FOOD, "zomato" to Category.FOOD,
        "dominos" to Category.FOOD, "domino" to Category.FOOD,
        "pizza hut" to Category.FOOD, "kfc" to Category.FOOD,
        "mcdonalds" to Category.FOOD, "mcdonald" to Category.FOOD,
        "burger king" to Category.FOOD, "subway" to Category.FOOD,
        "dunkin" to Category.FOOD, "starbucks" to Category.FOOD,
        "haldiram" to Category.FOOD, "barbeque nation" to Category.FOOD,
        // Groceries
        "bigbasket" to Category.GROCERIES, "big basket" to Category.GROCERIES,
        "blinkit" to Category.GROCERIES, "grofers" to Category.GROCERIES,
        "dmart" to Category.GROCERIES, "d-mart" to Category.GROCERIES,
        "reliance fresh" to Category.GROCERIES, "nature basket" to Category.GROCERIES,
        "more supermarket" to Category.GROCERIES, "spencers" to Category.GROCERIES,
        // Transport
        "uber" to Category.TRANSPORT, "ola" to Category.TRANSPORT,
        "rapido" to Category.TRANSPORT, "meru" to Category.TRANSPORT,
        "indian oil" to Category.TRANSPORT, "iocl" to Category.TRANSPORT,
        "bharat petroleum" to Category.TRANSPORT, "bpcl" to Category.TRANSPORT,
        "hp petrol" to Category.TRANSPORT, "fasttag" to Category.TRANSPORT,
        // Shopping
        "amazon" to Category.SHOPPING, "flipkart" to Category.SHOPPING,
        "myntra" to Category.SHOPPING, "ajio" to Category.SHOPPING,
        "meesho" to Category.SHOPPING, "nykaa" to Category.SHOPPING,
        "snapdeal" to Category.SHOPPING, "tatacliq" to Category.SHOPPING,
        "croma" to Category.SHOPPING, "reliance digital" to Category.SHOPPING,
        // Health
        "apollo" to Category.HEALTH, "1mg" to Category.HEALTH,
        "medplus" to Category.HEALTH, "pharmeasy" to Category.HEALTH,
        "netmeds" to Category.HEALTH, "practo" to Category.HEALTH,
        "thyrocare" to Category.HEALTH, "lal path" to Category.HEALTH,
        // Entertainment
        "netflix" to Category.ENTERTAINMENT, "spotify" to Category.ENTERTAINMENT,
        "amazon prime" to Category.ENTERTAINMENT, "hotstar" to Category.ENTERTAINMENT,
        "disney+" to Category.ENTERTAINMENT, "pvr" to Category.ENTERTAINMENT,
        "inox" to Category.ENTERTAINMENT, "bookmyshow" to Category.ENTERTAINMENT,
        "sony liv" to Category.ENTERTAINMENT, "zee5" to Category.ENTERTAINMENT,
        "youtube premium" to Category.ENTERTAINMENT,
        // Travel
        "indigo" to Category.TRAVEL, "air india" to Category.TRAVEL,
        "spicejet" to Category.TRAVEL, "vistara" to Category.TRAVEL,
        "irctc" to Category.TRAVEL, "makemytrip" to Category.TRAVEL,
        "goibibo" to Category.TRAVEL, "cleartrip" to Category.TRAVEL,
        "oyo" to Category.TRAVEL, "fabhotels" to Category.TRAVEL,
        // Utilities / Recharge
        "airtel" to Category.RECHARGE, "jio" to Category.RECHARGE,
        "vi " to Category.RECHARGE, "bsnl" to Category.RECHARGE,
        "tata sky" to Category.UTILITIES, "dish tv" to Category.UTILITIES,
        "sun direct" to Category.UTILITIES, "mahanagar gas" to Category.UTILITIES,
        "indane" to Category.UTILITIES, "hp gas" to Category.UTILITIES,
        // Education
        "byju" to Category.EDUCATION, "unacademy" to Category.EDUCATION,
        "udemy" to Category.EDUCATION, "coursera" to Category.EDUCATION,
        "upgrad" to Category.EDUCATION, "simplilearn" to Category.EDUCATION,
        "vedantu" to Category.EDUCATION, "toppr" to Category.EDUCATION,
        // Fitness
        "cult.fit" to Category.FITNESS, "cultfit" to Category.FITNESS,
        "gold's gym" to Category.FITNESS, "anytime fitness" to Category.FITNESS,
        "cure.fit" to Category.FITNESS,
        // Investments
        "zerodha" to Category.INVESTMENTS, "groww" to Category.INVESTMENTS,
        "upstox" to Category.INVESTMENTS, "kuvera" to Category.INVESTMENTS,
        "coin" to Category.INVESTMENTS, "paytm money" to Category.INVESTMENTS
    )

    // ── Tier 2: Keyword rules ─────────────────────────────────────────────
    private data class KeywordRule(val keywords: List<String>, val category: Category)

    private val keywordRules: List<KeywordRule> = listOf(
        KeywordRule(listOf("restaurant", "cafe", "coffee", "hotel dining", "food court", "biryani", "pizza", "burger", "dhaba"), Category.FOOD),
        KeywordRule(listOf("grocery", "supermarket", "provisions", "vegetables", "fruits", "kirana"), Category.GROCERIES),
        KeywordRule(listOf("fuel", "petrol", "diesel", "gas station", "metro", "bus", "cab", "auto", "taxi", "parking", "toll"), Category.TRANSPORT),
        KeywordRule(listOf("electricity", "water bill", "gas bill", "broadband", "dth recharge", "wifi", "postpaid"), Category.UTILITIES),
        KeywordRule(listOf("pharmacy", "medical", "hospital", "clinic", "doctor", "lab test", "diagnostic", "medicine"), Category.HEALTH),
        KeywordRule(listOf("prime video", "ott", "theater", "cinema", "gaming", "steam", "play store"), Category.ENTERTAINMENT),
        KeywordRule(listOf("flight", "airline", "hotel booking", "resort", "train", "bus ticket", "travel"), Category.TRAVEL),
        KeywordRule(listOf("rent", "maintenance", "society", "housing"), Category.RENT),
        KeywordRule(listOf("school", "college", "university", "tuition", "course", "coaching", "exam fee"), Category.EDUCATION),
        KeywordRule(listOf("salary", "payroll", "stipend", "wages", "remuneration", "hike"), Category.SALARY),
        KeywordRule(listOf("mutual fund", "sip", "equity", "stocks", "demat", "ipo", "nps"), Category.INVESTMENTS),
        KeywordRule(listOf("gym", "yoga", "fitness", "workout", "sports"), Category.FITNESS),
        KeywordRule(listOf("emi", "loan emi", "equated monthly", "home loan", "car loan", "personal loan"), Category.EMI),
        KeywordRule(listOf("donation", "charity", "gift", "ngo"), Category.GIFTS),
        KeywordRule(listOf("neft", "rtgs", "imps", "fund transfer", "upi transfer"), Category.TRANSFER),
        KeywordRule(listOf("prepaid recharge", "mobile recharge", "data pack"), Category.RECHARGE)
    )

    // ─────────────────────────────────────────────────────────────────────

    /**
     * Returns the best [Category] and confidence score for the given [merchant] and SMS [body].
     */
    fun categorize(merchant: String?, body: String): Pair<Category, Float> {
        val lowerMerchant = merchant?.lowercase()?.trim() ?: ""
        val lowerBody     = body.lowercase()

        // Tier 3: User-learned preference takes absolute priority
        if (lowerMerchant.isNotBlank()) {
            val learned = prefs.getString(lowerMerchant, null)
            if (learned != null) {
                return Category.valueOf(learned) to 0.99f
            }
        }

        // Tier 1: Exact merchant lookup
        if (lowerMerchant.isNotBlank()) {
            for ((key, cat) in merchantMap) {
                if (lowerMerchant.contains(key) || lowerBody.contains(key)) {
                    return cat to 0.97f
                }
            }
        }

        // Tier 2: Keyword rules on full body
        for (rule in keywordRules) {
            if (rule.keywords.any { lowerBody.contains(it) }) {
                return rule.category to 0.80f
            }
        }

        return Category.OTHER to 0.40f
    }

    /**
     * Returns true if [confidence] is high enough for the transaction to be auto-confirmed
     * without requiring the user to visit S-02.
     */
    fun isAutoConfident(confidence: Float): Boolean = confidence >= MIN_CONFIDENCE_AUTO_CONFIRM

    /**
     * Persist a user correction so future SMS from the same merchant use this category.
     * Called from S-03 when "Remember for [Merchant]" is checked.
     */
    fun learnMerchantCategory(merchant: String, category: Category) {
        prefs.edit().putString(merchant.lowercase().trim(), category.name).apply()
    }

    /** Wipe all learned preferences (accessible from Settings). */
    fun clearLearned() {
        prefs.edit().clear().apply()
    }
}