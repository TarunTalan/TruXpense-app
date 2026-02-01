package com.example.truxpense.presentation.screens.onboarding.currency

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.*
import androidx.core.content.edit
import javax.inject.Inject

data class CurrencyItem(val code: String, val symbol: String, val name: String)

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("truxpense_prefs", Context.MODE_PRIVATE)
    private val KEY_CURRENCY = "pref_currency_code"

    // load available currencies once: deduplicate, use device locale display name & symbol for consistency
    val available: List<CurrencyItem> = run {
        val deviceLocale = Locale.getDefault()
        val collator = Collator.getInstance(deviceLocale)

        val baseList = Currency.getAvailableCurrencies()
            .mapNotNull { currency ->
                try {
                    val code = currency.currencyCode
                    // prefer device locale display name
                    val name = try {
                        currency.getDisplayName(deviceLocale)
                    } catch (_: Exception) {
                        currency.currencyCode
                    }
                    val symbol = try {
                        val s = currency.getSymbol(deviceLocale)
                        if (s.isNullOrBlank()) currency.currencyCode else s
                    } catch (_: Exception) {
                        currency.currencyCode
                    }

                    CurrencyItem(code = code, symbol = symbol, name = name)
                } catch (_: Exception) {
                    null
                }
            }
            // deduplicate by code (some runtimes may include duplicates)
            .distinctBy { it.code }
            // sort by display name using device locale collator
            .sortedWith { a, b -> collator.compare(a.name, b.name) }

        // Prefer device locale currency first, then USD, then the rest in sorted order
        val deviceCurrencyCode = try {
            Currency.getInstance(deviceLocale).currencyCode
        } catch (_: Exception) {
            null
        }

        val preferredOrder = listOfNotNull(deviceCurrencyCode, "USD").distinct()
        val preferredItems = preferredOrder.mapNotNull { code -> baseList.firstOrNull { it.code == code } }
        val others = baseList.filterNot { item -> preferredOrder.contains(item.code) }
        preferredItems + others
    }

    private val _selectedCurrency = MutableStateFlow<CurrencyItem?>(null)
    val selectedCurrency: StateFlow<CurrencyItem?> = _selectedCurrency

    init {
        // load saved value
        val code = prefs.getString(KEY_CURRENCY, null)
        if (code != null) {
            val found = available.firstOrNull { it.code == code }
            _selectedCurrency.value = found
        }
    }

    fun selectCurrency(item: CurrencyItem) {
        // Update in-memory selection immediately so UI updates synchronously
        _selectedCurrency.value = item
    }

    /**
     * Persist the current in-memory selection into SharedPreferences.
     * Call this explicitly when the user confirms (Continue/Skip) or when the app needs to persist a default.
     */
    fun persistSelectedCurrency() {
        val code = _selectedCurrency.value?.code ?: return
        viewModelScope.launch {
            prefs.edit { putString(KEY_CURRENCY, code) }
        }
    }

    // Clear in-memory selection (keeps persisted preference intact)
    fun clearSelection() {
        // Clear immediately so UI updates right away when called from DisposableEffect
        _selectedCurrency.value = null
    }

    // Choose a sensible default currency based on device locale, falling back to USD/INR/first available
    fun localeDefaultCurrency(): CurrencyItem? {
        try {
            val deviceLocale = Locale.getDefault()
            val currency = try {
                Currency.getInstance(deviceLocale)
            } catch (_: Exception) {
                null
            }
            val code = currency?.currencyCode
            if (!code.isNullOrBlank()) {
                val found = available.firstOrNull { it.code == code }
                if (found != null) return found
            }
        } catch (_: Exception) {
            // ignore
        }

        // fallback preference order
        val fallbackCandidates = listOf("USD", "INR")
        for (c in fallbackCandidates) {
            val f = available.firstOrNull { it.code == c }
            if (f != null) return f
        }

        // ultimate fallback: first available
        return available.firstOrNull()
    }
}