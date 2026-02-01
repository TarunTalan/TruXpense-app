package com.example.truxpense.presentation.utils

// Input validation helpers

// Validation utils used across auth screens
object InputValidators {
    private const val MAX_EMAIL_WORDS = 100
    private const val MAX_USERNAME_WORDS = 50

    private const val MAX_EMAIL_LENGTH = 320
    private const val MAX_USERNAME_CHARS = 200

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    // Filter email input
    fun filterEmailInput(input: String): String {
        if (input.isEmpty()) return input
        val filtered = buildString {
            for (ch in input) {
                if (ch.isLetterOrDigit() || ch == '@' || ch == '.' || ch == '_' || ch == '+' || ch == '-') {
                    append(ch)
                }
            }
        }
        return if (filtered.length <= MAX_EMAIL_LENGTH) filtered else filtered.substring(0, MAX_EMAIL_LENGTH)
    }

    // Basic email validation
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        if (email.length > MAX_EMAIL_LENGTH) return false
        if (email.contains(' ')) return false
        val words = email.trim().split(Regex("\\s+"))
        if (words.size > MAX_EMAIL_WORDS) return false
        return EMAIL_REGEX.matches(email)
    }

    // Filter username to ASCII letters
    fun filterUsernameInput(input: String): String {
        if (input.isEmpty()) return input
        val filtered = buildString {
            for (ch in input) {
                if (ch in 'A'..'Z' || ch in 'a'..'z') append(ch)
            }
        }
        return if (filtered.length <= MAX_USERNAME_CHARS) filtered else filtered.substring(0, MAX_USERNAME_CHARS)
    }

    // Basic username validation
    fun isValidUsername(username: String): Boolean {
        val trimmed = username.trim()
        if (trimmed.isEmpty()) return false
        if (trimmed.length > MAX_USERNAME_CHARS) return false
        if (!trimmed.all { it in 'A'..'Z' || it in 'a'..'z' }) return false
        val words = trimmed.split(Regex("\\s+"))
        if (words.size > MAX_USERNAME_WORDS) return false
        return true
    }
}
