package com.example.bodycamoutdoorgear.giveaway

import android.content.Context
import java.io.File

/**
 * Where Giveaway files live. Photos taken in Giveaway mode are first written to
 * a temporary folder; only the finished card is saved to the LBC gallery (the
 * app's files folder, which the Gallery tab lists).
 */
object GiveawayStore {

    /** Photos and clips as taken, before they are made into a card. Temporary: the system can clear it. */
    fun originalsDir(context: Context): File =
        File(context.cacheDir, "giveaway-originals").apply { mkdirs() }

    /**
     * Saves (or replaces) the card image in the gallery. Written under a name the
     * Gallery ignores (".part") and renamed once complete, so a listing can never
     * pick up half a file.
     */
    fun saveCardImage(context: Context, jpeg: ByteArray, token: String): Boolean = try {
        val part = File(context.filesDir, "giveaway_$token.jpg.part")
        val final = File(context.filesDir, "giveaway_$token.jpg")
        part.writeBytes(jpeg)
        if (final.exists()) final.delete()
        part.renameTo(final)
    } catch (_: Exception) {
        false
    }

    /** Used when no card could be made: the user's photo goes to the gallery after all, not lost. */
    fun rescueOriginal(context: Context, original: File) {
        try {
            if (!original.exists()) return
            val dest = File(context.filesDir, original.name)
            if (!dest.exists()) original.copyTo(dest)
            original.delete()
        } catch (_: Exception) {
        }
    }
}
