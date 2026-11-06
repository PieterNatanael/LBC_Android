package com.example.bodycamoutdoorgear.screenrelay

import android.content.Context
import java.util.UUID

/**
 * This phone's identity for the "Analyze my Mac" relay, and the pairing (if
 * any) linking it to a Mac. Mirrors GiveawayAIService's install_id
 * convention, and the Mac app's own ScreenRelayPairing — same shape on
 * both ends, deliberately.
 */
object ScreenRelayPairing {
    private fun prefs(context: Context) = context.getSharedPreferences("screen_relay", Context.MODE_PRIVATE)

    /** Random id for this phone install. Created once, persisted forever. */
    fun phoneInstallId(context: Context): String {
        val p = prefs(context)
        p.getString("phone_install_id", null)?.let { return it }
        return UUID.randomUUID().toString().also { p.edit().putString("phone_install_id", it).apply() }
    }

    /** The pairing's id on the server, once scanned from the Mac's QR code. */
    fun pairId(context: Context): String? = prefs(context).getString("pair_id", null)

    fun isPaired(context: Context): Boolean = prefs(context).getBoolean("paired", false)

    fun savePairing(context: Context, pairId: String) {
        prefs(context).edit().putString("pair_id", pairId).putBoolean("paired", true).apply()
    }

    fun reset(context: Context) {
        prefs(context).edit().remove("pair_id").putBoolean("paired", false).apply()
    }

    /** Whether the first-time "here's the path you need to take" walkthrough has been shown. */
    fun introSeen(context: Context): Boolean = prefs(context).getBoolean("intro_seen", false)
    fun setIntroSeen(context: Context) = prefs(context).edit().putBoolean("intro_seen", true).apply()
}
