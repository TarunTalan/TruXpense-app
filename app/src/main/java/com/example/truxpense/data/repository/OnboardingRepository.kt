package com.example.truxpense.data.repository

import com.example.truxpense.data.prefs.AuthPreferences
import com.example.truxpense.data.remote.api.OnboardingApi
import com.example.truxpense.data.remote.dto.request.UsernameRequest
import com.example.truxpense.data.remote.dto.response.UsernameResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


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
                val errBody = try { resp.errorBody()?.string() } catch (_: Exception) { "<error reading body>" }

                Result.failure(Exception("Server returned ${resp.code()} - $errBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
