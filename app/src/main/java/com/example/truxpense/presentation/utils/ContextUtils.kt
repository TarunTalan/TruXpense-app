package com.example.truxpense.presentation.utils

// Small context utilities

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

// Find nearest Activity from a Context
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
