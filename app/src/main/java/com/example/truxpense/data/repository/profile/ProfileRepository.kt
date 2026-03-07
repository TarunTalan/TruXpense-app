package com.example.truxpense.data.repository.profile

import com.example.truxpense.data.local.datastore.AuthPreferences
import com.example.truxpense.data.remote.api.EmailOtpRequest
import com.example.truxpense.data.remote.api.ProfileApi
import com.example.truxpense.data.remote.api.UpdateEmailRequest
import com.example.truxpense.data.remote.api.UpdateUsernameRequest
import com.example.truxpense.presentation.utils.ResponseHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val api: ProfileApi,
    private val prefs: AuthPreferences,
) {

    /**
     * Step 1 – POST /api/auth/signup/send-otp { "email": newEmail }
     * Sends OTP to the new email. Public endpoint (no auth header).
     */
    suspend fun sendEmailChangeOtp(newEmail: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val resp = api.sendEmailChangeOtp(EmailOtpRequest(newEmail))
                if (resp.isSuccessful) {
                    Result.success(resp.body()?.message ?: "OTP sent")
                } else {
                    val msg = ResponseHandler.parseHttpResponse(resp.code(), resp.errorBody()?.string())
                    Result.failure(Exception(msg))
                }
            } catch (e: Exception) {
                Result.failure(Exception(ResponseHandler.parseThrowable(e)))
            }
        }

    /**
     * Step 2 – PUT /api/auth/email { "email": newEmail, "otp": otp }
     * Backend:
     *   1. Reads current user from SecurityContextHolder (populated by JwtAuthFilter).
     *   2. Verifies OTP for the new email.
     *   3. Updates user's email in the database.
     * On success persists the new email locally.
     */
    suspend fun verifyOtpAndUpdateEmail(newEmail: String, otp: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val resp = api.updateEmail(UpdateEmailRequest(email = newEmail, otpCode = otp))
                if (resp.isSuccessful) {
                    prefs.saveEmail(newEmail)
                    Result.success(resp.body()?.message ?: "Email updated")
                } else {
                    val msg = ResponseHandler.parseHttpResponse(resp.code(), resp.errorBody()?.string())
                    Result.failure(Exception(msg))
                }
            } catch (e: Exception) {
                Result.failure(Exception(ResponseHandler.parseThrowable(e)))
            }
        }

    /**
     * Update phone number locally (no server endpoint).
     */
    suspend fun updatePhone(newPhone: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                prefs.savePhone(newPhone)
                Result.success("Phone updated")
            } catch (e: Exception) {
                Result.failure(Exception(ResponseHandler.parseThrowable(e)))
            }
        }

    /**
     * PUT /api/auth/username { "username": username }
     * Backend identifies user from JWT and updates display name.
     */
    suspend fun updateUsername(username: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val resp = api.updateUsername(UpdateUsernameRequest(username))
                if (resp.isSuccessful) {
                    prefs.saveUsername(username)
                    Result.success(resp.body()?.message ?: "Username updated")
                } else {
                    val msg = ResponseHandler.parseHttpResponse(resp.code(), resp.errorBody()?.string())
                    Result.failure(Exception(msg))
                }
            } catch (e: Exception) {
                Result.failure(Exception(ResponseHandler.parseThrowable(e)))
            }
        }
}
