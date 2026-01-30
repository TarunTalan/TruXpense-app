package com.example.truxpense.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Find the nearest Activity from a Context (works with ContextWrapper)
 */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
