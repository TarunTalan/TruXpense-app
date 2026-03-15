package com.example.truxpense.presentation.screens.dashboard.savings

import com.example.truxpense.R

/** Map emoji/key string (stored in DB as String) to drawable resource id. */
fun goalIconToDrawable(icon: String): Int {
    return when (icon) {
        // new key-based names
        "iphone" -> R.drawable.iphone
        "camera" -> R.drawable.camera
        "women_bag" -> R.drawable.women_bag
        "gaming_controller" -> R.drawable.gaming_controller
        "laptop" -> R.drawable.laptop
        "pager" -> R.drawable.smartwatch
        "car" -> R.drawable.car
        "cruise_ship" -> R.drawable.trip_cruise_ship
        "gift" -> R.drawable.gift
        "home_icon" -> R.drawable.home_investment
        "reading_books" -> R.drawable.reading_books
        "fitness" -> R.drawable.fitness
        else -> R.drawable.diamond
    }
}
