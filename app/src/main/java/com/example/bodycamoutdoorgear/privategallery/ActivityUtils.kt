package com.example.bodycamoutdoorgear.privategallery

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Unwraps a Compose Context down to its hosting Activity — LocalContext.current
 *  is sometimes a ContextWrapper (a themed context, for instance) rather than
 *  the Activity itself. Used wherever Private Gallery needs the real Activity:
 *  FLAG_SECURE and (for FragmentActivity specifically) BiometricPrompt. */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
