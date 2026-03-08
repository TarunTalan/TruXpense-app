package com.example.truxpense.presentation.screens.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.presentation.screens.premium.model.CardFormState
import com.example.truxpense.presentation.screens.premium.model.FormErrors
import com.example.truxpense.presentation.screens.premium.model.PaymentMethod
import com.example.truxpense.presentation.screens.premium.model.PlanType
import com.example.truxpense.presentation.screens.premium.model.popularBanks
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── UI State ─────────────────────────────────────────────────────────────────

data class PaymentGatewayUiState(
    val selectedPlan: PlanType = PlanType.ANNUAL,
    val selectedMethod: PaymentMethod = PaymentMethod.CARD,
    val card: CardFormState = CardFormState(),
    val upiId: String = "",
    val selectedBank: String = "",
    val errors: FormErrors = FormErrors(),
    val isProcessing: Boolean = false,
    val availableBanks: List<String> = popularBanks,
)

// ─── Events (one-shot) ────────────────────────────────────────────────────────

sealed interface PaymentEvent {
    data object PaymentSuccess : PaymentEvent
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class PaymentGatewayViewModel(
    initialPlan: PlanType = PlanType.ANNUAL,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentGatewayUiState(selectedPlan = initialPlan))
    val uiState: StateFlow<PaymentGatewayUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PaymentEvent>()
    val events: SharedFlow<PaymentEvent> = _events.asSharedFlow()

    // ── Plan ─────────────────────────────────────────────────────────────────

    fun onPlanSelected(plan: PlanType) {
        _uiState.update { it.copy(selectedPlan = plan) }
    }

    // ── Method ───────────────────────────────────────────────────────────────

    fun onMethodSelected(method: PaymentMethod) {
        _uiState.update { it.copy(selectedMethod = method, errors = FormErrors()) }
    }

    // ── Card inputs ──────────────────────────────────────────────────────────

    fun onCardNumberChanged(raw: String) {
        val formatted = raw.filter { it.isDigit() }.take(16)
            .chunked(4).joinToString(" ")
        _uiState.update { it.copy(card = it.card.copy(number = formatted)) }
    }

    fun onCardExpiryChanged(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(4)
        val formatted = if (digits.length >= 3) "${digits.take(2)}/${digits.drop(2)}" else digits
        _uiState.update { it.copy(card = it.card.copy(expiry = formatted)) }
    }

    fun onCardCvvChanged(raw: String) {
        val cvv = raw.filter { it.isDigit() }.take(4)
        _uiState.update { it.copy(card = it.card.copy(cvv = cvv)) }
    }

    fun onCardNameChanged(name: String) {
        _uiState.update { it.copy(card = it.card.copy(name = name)) }
    }

    // ── UPI ──────────────────────────────────────────────────────────────────

    fun onUpiIdChanged(value: String) {
        _uiState.update { it.copy(upiId = value) }
    }

    // ── Net Banking ──────────────────────────────────────────────────────────

    fun onBankSelected(bank: String) {
        _uiState.update { it.copy(selectedBank = bank, errors = it.errors.copy(bank = null)) }
    }

    // ── Payment processing ───────────────────────────────────────────────────

    fun onPayTapped() {
        if (!validate()) return
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            delay(2200) // Simulate network call
            _uiState.update { it.copy(isProcessing = false) }
            _events.emit(PaymentEvent.PaymentSuccess)
        }
    }

    // ── Validation ───────────────────────────────────────────────────────────

    private fun validate(): Boolean {
        val state = _uiState.value
        val errors = when (state.selectedMethod) {
            PaymentMethod.CARD -> FormErrors(
                cardNumber = if (state.card.number.replace(" ", "").length < 16)
                    "Enter a valid 16-digit card number" else null,
                cardExpiry = if (state.card.expiry.length < 5)
                    "Enter a valid expiry date" else null,
                cardCvv = if (state.card.cvv.length < 3)
                    "Enter a valid CVV" else null,
                cardName = if (state.card.name.isBlank())
                    "Enter cardholder name" else null,
            )
            PaymentMethod.UPI -> FormErrors(
                upiId = if (!state.upiId.contains("@"))
                    "Enter a valid UPI ID (e.g. name@upi)" else null,
            )
            PaymentMethod.NETBANKING -> FormErrors(
                bank = if (state.selectedBank.isBlank())
                    "Please select a bank" else null,
            )
        }
        _uiState.update { it.copy(errors = errors) }
        return !errors.hasErrors
    }
}