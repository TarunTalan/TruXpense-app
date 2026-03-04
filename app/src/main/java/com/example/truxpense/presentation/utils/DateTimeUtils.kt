package com.example.truxpense.presentation.utils

import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {
    /**
     * Parse "MMM d" (e.g. "Mar 4") + "hh:mm a" (e.g. "09:30 AM")
     * back to epoch milliseconds. Falls back to now on any parse error.
     */
    fun parseDateTimeToMillis(date: String, time: String): Long {
        return try {
            val year = Calendar.getInstance().get(Calendar.YEAR)
            val sdf = SimpleDateFormat("MMM d yyyy hh:mm a", Locale.getDefault())
            sdf.parse("$date $year $time")?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}

