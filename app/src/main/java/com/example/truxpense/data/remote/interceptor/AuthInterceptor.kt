package com.example.truxpense.data.remote.interceptor

import com.example.truxpense.data.auth.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds Authorization header using TokenManager cached token.
 * Skips requests for explicitly public/auth endpoints and requests marked with "No-Auth" header.
 */
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val path = original.url.encodedPath

        // Whitelist of auth endpoints that should NOT receive Authorization header
        val authNoAuthPaths = setOf(
            "/api/auth/login",
            "/api/auth/google",
            "/api/auth/refresh",
            "/api/auth/signup/send-otp",
            "/api/auth/signup/verify",
            "/api/auth/login/send-otp",
            "/api/auth/login/verify",
            "/api/auth/signup/resend-otp",
            "/api/auth/login/resend-otp"
        )

        val hasNoAuthHeader = original.header("No-Auth") != null
        if (hasNoAuthHeader || authNoAuthPaths.contains(path)) {
            val req = original.newBuilder().removeHeader("No-Auth").build()
            return chain.proceed(req)
        }

        val token = tokenManager.getToken()
        val builder = original.newBuilder()
        if (!token.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $token")
        }

        val request = builder.build()
        return chain.proceed(request)
    }
}