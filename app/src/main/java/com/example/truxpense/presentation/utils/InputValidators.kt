package com.example.truxpense.presentation.utils

// Input validation helpers

// Validation utils used across auth screens
object InputValidators {
    private const val MAX_EMAIL_WORDS = 100
    private const val MAX_USERNAME_WORDS = 50

    private const val MAX_EMAIL_LENGTH = 320
    private const val MAX_USERNAME_CHARS = 100
    private const val MAX_CURRENCY_INPUT_LENGTH = 50

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

    // Filter username to only letters and single internal spaces, no leading/trailing spaces
    fun filterUsernameInput(input: String): String {
        if (input.isEmpty()) return input
        // Determine whether the original input had leading/trailing whitespace
        val hasLeading = input.firstOrNull()?.isWhitespace() == true
        val hasTrailing = input.lastOrNull()?.isWhitespace() == true

        // Remove any character that's not a Unicode letter or space
        val filtered = input.replace(Regex("[^\\p{L} ]+"), "")
        // Collapse multiple whitespace into a single space for internal spacing
        var collapsed = filtered.replace(Regex("\\s+"), " ")
        // Trim to remove any accidental edges, we'll re-add at most one leading/trailing space below
        collapsed = collapsed.trim()

        // Re-add a single leading/trailing space if the original input had whitespace there
        var result = collapsed
        if (hasLeading) result = " " + result
        if (hasTrailing) result = result + " "

        // Enforce max length
        return if (result.length <= MAX_USERNAME_CHARS) result else result.substring(0, MAX_USERNAME_CHARS)
    }

    // Basic username validation: only letters and single internal spaces; optional single leading/trailing spaces allowed
    fun isValidUsername(username: String): Boolean {
        if (username.isEmpty()) return false
        if (username.length > MAX_USERNAME_CHARS) return false
        // Pattern: optional leading space, one or more letters, optionally repeated groups of single space + letters, optional trailing space
        val allowed = Regex("^ ?\\p{L}+( \\p{L}+)* ?$")
        if (!allowed.matches(username)) return false
        val words = username.trim().split(Regex("\\s+"))
        if (words.size > MAX_USERNAME_WORDS) return false
        return true
    }

    /**
     * Return a user-facing error message for username, or null if valid.
     * Keep messages simple; callers can map them to localized resources.
     */
    fun usernameError(username: String): String? {
        if (username.isEmpty()) return "Username cannot be empty"
        if (username.length > MAX_USERNAME_CHARS) return "Username is too long"
        val allowed = Regex("^ ?\\p{L}+( \\p{L}+)* ?$")
        if (!allowed.matches(username)) return "Username can contain only letters and single spaces (at most one leading/trailing space)"
        val words = username.trim().split(Regex("\\s+"))
        if (words.size > MAX_USERNAME_WORDS) return "Username has too many words"
        return null
    }

    /**
     * Filter currency input: disallow digits and control characters, limit to MAX_CURRENCY_INPUT_LENGTH.
     * Allows letters, currency symbols, punctuation and spaces.
     */
    fun filterCurrencyInput(input: String): String {
        if (input.isEmpty()) return input
        val filtered = buildString {
            for (ch in input) {
                // keep printable non-digit characters
                if (!ch.isISOControl() && !ch.isDigit()) append(ch)
            }
        }
        val trimmed = filtered.trim()
        return if (trimmed.length <= MAX_CURRENCY_INPUT_LENGTH) trimmed else trimmed.substring(0, MAX_CURRENCY_INPUT_LENGTH)
    }

    // Other utilities remain unchanged...
}
