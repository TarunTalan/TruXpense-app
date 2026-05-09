package com.example.truxpense.data.repository.onboarding

// Onboarding repository

import com.example.truxpense.data.local.datastore.AuthPreferences
import com.example.truxpense.data.remote.api.OnboardingApi
import com.example.truxpense.data.remote.dto.request.UsernameRequest
import com.example.truxpense.data.remote.dto.response.UsernameResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.example.truxpense.presentation.utils.ResponseHandler


@Singleton
class OnboardingRepository @Inject constructor(
    private val onboardingApi: OnboardingApi,
    private val prefs: AuthPreferences
) {

    suspend fun setUsername(username: String): Result<UsernameResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = onboardingApi.username(UsernameRequest(username))

            if (resp.isSuccessful) {
                resp.body()?.let {
                    // persist username in preferences
                    prefs.saveUsername(it.username)
                    Result.success(it)
                } ?: Result.failure(Exception("Empty body"))
            } else {
                val errBody = try { resp.errorBody()?.string() } catch (_: Exception) { null }
                val friendly = ResponseHandler.parseHttpResponse(resp.code(), errBody)
                Result.failure(Exception(friendly))
            }
        } catch (e: Exception) {
            val friendly = ResponseHandler.parseThrowable(e)
            Result.failure(Exception(friendly))
        }
    }
}
