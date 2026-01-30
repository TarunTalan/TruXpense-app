package com.example.truxpense.presentation.utils

import retrofit2.HttpException

object ResponseHandler {
    /**
     * Return a user-friendly message for a Result failure or defaultMessage.
     */
    fun getMessageFromResult(result: Result<*>, defaultMessage: String): String {
        val t = result.exceptionOrNull()
        return if (t != null) parseThrowable(t) else defaultMessage
    }

    fun parseThrowable(t: Throwable): String {
        return when (t) {
            is java.net.SocketTimeoutException -> "Connection timed out. Please try again."
            is java.net.UnknownHostException -> "No internet connection. Please check your network."
            is java.io.IOException -> "Network error. Please check your connection."
            is HttpException -> {
                // Treat 5xx server errors as a generic server-unavailable message
                val code = try { t.code() } catch (_: Exception) { -1 }
                if (code in 500..599) {
                    "Server error. Please try again later."
                } else {
                    try {
                        val body = t.response()?.errorBody()?.string()
                        if (!body.isNullOrEmpty()) extractMessage(body) else (t.localizedMessage ?: "Server error")
                    } catch (_: Exception) {
                        t.localizedMessage ?: "Server error"
                    }
                }
            }
            else -> {
                val msg = t.localizedMessage
                if (!msg.isNullOrBlank()) extractMessage(msg) else "Something went wrong. Please try again."
            }
        }
    }

    private fun extractMessage(body: String): String {
        // Try to extract common JSON fields like message or error
        val messageRegex = "\"message\"\\s*:\\s*\"([^\"]+)\"".toRegex(RegexOption.IGNORE_CASE)
        messageRegex.find(body)?.let { return it.groupValues[1] }

        val errorRegex = "\"error\"\\s*:\\s*\"([^\"]+)\"".toRegex(RegexOption.IGNORE_CASE)
        errorRegex.find(body)?.let { return it.groupValues[1] }

        // Fallback: strip simple HTML tags and return a short snippet
        val stripped = body.replace("<[^>]*>".toRegex(), "").trim()
        return if (stripped.length <= 300) stripped else stripped.substring(0, 300) + "..."
    }
}
