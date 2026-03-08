package com.example.truxpense.presentation.screens.premium

import androidx.lifecycle.ViewModel
import com.example.truxpense.presentation.screens.premium.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ─── UI State ─────────────────────────────────────────────────────────────────

data class PaywallUiState(
    val benefits: List<BenefitSection> = premiumBenefits,
    val featureRows: List<FeatureRow> = premiumFeatureRows,
    val selectedPlan: PlanType = PlanType.ANNUAL,
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class PaywallViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    fun onPlanSelected(plan: PlanType) {
        _uiState.update { it.copy(selectedPlan = plan) }
    }
}