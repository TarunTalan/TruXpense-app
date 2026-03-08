package com.example.truxpense.presentation.screens.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.truxpense.presentation.screens.premium.model.PlanType

/**
 * Factory that passes [initialPlan] to [PaymentGatewayViewModel] so the selected
 * plan from the Paywall screen is preserved when the ViewModel is first created.
 */
class PaymentGatewayViewModelFactory(
    private val initialPlan: PlanType,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(PaymentGatewayViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return PaymentGatewayViewModel(initialPlan = initialPlan) as T
    }
}