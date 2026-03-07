package com.example.truxpense.presentation.screens.dashboard.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.local.datastore.AuthPreferences
import com.example.truxpense.data.repository.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Contact OTP sub-flow state ────────────────────────────────────────────────

enum class ContactType { EMAIL, PHONE }

data class ContactOtpState(
    val type: ContactType     = ContactType.EMAIL,
    val newContact: String    = "",
    val isSendingOtp: Boolean = false,
    val otpSent: Boolean      = false,
    val isVerifying: Boolean  = false,
    val otpError: String?     = null,
    val isSuccess: Boolean    = false,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class PersonalInfoViewModel @Inject constructor(
    val prefs: AuthPreferences,
    private val profileRepo: ProfileRepository,
) : ViewModel() {

    // Persisted values exposed as Flows
    val username = prefs.username
    val phone    = prefs.phone
    val email    = prefs.email

    // Save-profile state
    private val _isSaving  = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError

    // OTP sub-flow state
    private val _otpState  = MutableStateFlow(ContactOtpState())
    val otpState: StateFlow<ContactOtpState> = _otpState

    // ── Save display name via PUT /api/auth/username ──────────────────────────

    fun saveProfile(name: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value  = true
            _saveError.value = null
            profileRepo.updateUsername(name)
                .onFailure { _saveError.value = it.message }
            _isSaving.value  = false
            onComplete()
        }
    }

    // ── Contact-change OTP flow ───────────────────────────────────────────────

    /**
     * EMAIL  → POST /api/auth/signup/send-otp  then wait for verifyOtp()
     * PHONE  → save directly (no OTP required server-side)
     */
    fun initiateContactChange(type: ContactType, newContact: String) {
        _otpState.value = ContactOtpState(
            type         = type,
            newContact   = newContact,
            isSendingOtp = type == ContactType.EMAIL,   // only EMAIL triggers a network call
        )

        if (type == ContactType.PHONE) {
            // Phone: persist immediately, mark as success without OTP step
            viewModelScope.launch {
                profileRepo.updatePhone(newContact)
                    .onSuccess { _otpState.value = _otpState.value.copy(isSuccess = true) }
                    .onFailure { e -> _otpState.value = _otpState.value.copy(otpError = e.message) }
            }
            return
        }

        // EMAIL: send OTP first
        viewModelScope.launch {
            profileRepo.sendEmailChangeOtp(newContact)
                .onSuccess {
                    _otpState.value = _otpState.value.copy(isSendingOtp = false, otpSent = true)
                }
                .onFailure { e ->
                    _otpState.value = _otpState.value.copy(
                        isSendingOtp = false,
                        otpError     = e.message,
                    )
                }
        }
    }

    /** Resend OTP to the same email (only valid for EMAIL type). */
    fun resendOtp() {
        val s = _otpState.value
        if (s.type != ContactType.EMAIL) return
        _otpState.value = s.copy(otpError = null, isSendingOtp = true)
        viewModelScope.launch {
            profileRepo.sendEmailChangeOtp(s.newContact)
                .onSuccess  { _otpState.value = _otpState.value.copy(isSendingOtp = false, otpSent = true) }
                .onFailure  { e -> _otpState.value = _otpState.value.copy(isSendingOtp = false, otpError = e.message) }
        }
    }

    /**
     * PUT /api/auth/email { "email": newEmail, "otp": otp }
     * Backend reads current user from JWT (SecurityContextHolder), verifies OTP,
     * then updates email. On success persists locally and calls [onSuccess].
     */
    fun verifyOtp(otp: String, onSuccess: () -> Unit = {}) {
        val s = _otpState.value
        _otpState.value = s.copy(isVerifying = true, otpError = null)
        viewModelScope.launch {
            profileRepo.verifyOtpAndUpdateEmail(s.newContact, otp)
                .onSuccess {
                    _otpState.value = _otpState.value.copy(isVerifying = false, isSuccess = true)
                    onSuccess()
                }
                .onFailure { e ->
                    _otpState.value = _otpState.value.copy(isVerifying = false, otpError = e.message)
                }
        }
    }

    /** Reset the OTP sub-flow (call on back-press from OTP screen). */
    fun resetOtpState() { _otpState.value = ContactOtpState() }
}
