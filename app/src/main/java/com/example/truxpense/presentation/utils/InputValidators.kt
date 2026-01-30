package com.example.truxpense.presentation.utils

/**
 * Input validation & filtering utilities used across auth screens.
 *
 * Assumptions made:
 * - "under 100 words" for email is interpreted as "under 100 characters" (emails are single tokens, not words).
 * - "under 50 words" for username is interpreted as "under 50 characters".
 * - Email allowed chars: letters, digits, and @ . _ + -
 * - Username allowed chars: letters only (Unicode letters).
 */
object InputValidators {
    // Interpret "words" limits literally as requested; username will now be ASCII letters only
    private const val MAX_EMAIL_WORDS = 100
    private const val MAX_USERNAME_WORDS = 50

    // Also enforce reasonable character caps to avoid extremely long inputs
    private const val MAX_EMAIL_LENGTH = 320 // RFC max local+domain combined
    private const val MAX_USERNAME_CHARS = 200

    // Simple permissive email regex (not full RFC) that works for typical addresses used in UIs.
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /**
     * Filters an email input by removing spaces, emojis and any characters not in the allowed set.
     * Truncates to MAX_EMAIL_LENGTH. Emails should not contain spaces; if user types them they are removed.
     */
    fun filterEmailInput(input: String): String {
        if (input.isEmpty()) return input
        val filtered = buildString {
            for (ch in input) {
                if (ch.isLetterOrDigit() || ch == '@' || ch == '.' || ch == '_' || ch == '+' || ch == '-') {
                    append(ch)
                }
                // ignore any other char (spaces, emoji, symbols)
            }
        }
        return if (filtered.length <= MAX_EMAIL_LENGTH) filtered else filtered.substring(0, MAX_EMAIL_LENGTH)
    }

    /**
     * Returns true when the email is likely valid: within length, no spaces/emojis, matches basic regex,
     * and word count (split by whitespace) is <= MAX_EMAIL_WORDS. For practical purposes a valid email
     * will have a single "word" (no spaces) so this primarily guards against malformed input.
     */
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        if (email.length > MAX_EMAIL_LENGTH) return false
        // no spaces
        if (email.contains(' ')) return false
        // word count guard (mostly redundant for emails without spaces)
        val words = email.trim().split(Regex("\\s+"))
        if (words.size > MAX_EMAIL_WORDS) return false
        // basic regex
        return EMAIL_REGEX.matches(email)
    }

    /**
     * Filters a username input to ASCII letters only (A-Za-z). Removes any non-ASCII-letter characters
     * and truncates to MAX_USERNAME_CHARS. Spaces are not allowed under the ASCII-only rule.
     */
    fun filterUsernameInput(input: String): String {
        if (input.isEmpty()) return input
        val filtered = buildString {
            for (ch in input) {
                if (ch in 'A'..'Z' || ch in 'a'..'z') append(ch)
            }
        }
        return if (filtered.length <= MAX_USERNAME_CHARS) filtered else filtered.substring(0, MAX_USERNAME_CHARS)
    }

    /**
     * Returns true if username is non-blank, within the character cap, contains only ASCII letters,
     * and word count (split by whitespace) does not exceed MAX_USERNAME_WORDS. Because spaces are removed
     * by the filter, the word count check will effectively be 1 for normal inputs.
     */
    fun isValidUsername(username: String): Boolean {
        val trimmed = username.trim()
        if (trimmed.isEmpty()) return false
        if (trimmed.length > MAX_USERNAME_CHARS) return false
        // ensure ASCII letters only
        if (!trimmed.all { it in 'A'..'Z' || it in 'a'..'z' }) return false
        // word count guard
        val words = trimmed.split(Regex("\\s+"))
        if (words.size > MAX_USERNAME_WORDS) return false
        return true
    }
}
