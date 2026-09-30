package com.example.bodycamoutdoorgear.data

import android.content.Context

/**
 * When each imported file was added to the app.
 *
 * A file on Android carries a single timestamp, and the app uses it for both
 * the grid order and the date shown on a thumbnail, so it has to stay the date
 * the photo was TAKEN. "Recently added" needs a second date, and that is what
 * this holds — but only for imports, keyed by file name.
 *
 * Every other file needs no entry: a live capture is added the moment it is
 * taken, and a HipHop arrival is written when it lands, so for both the file's
 * own date already IS the added date. Anything missing here simply falls back
 * to it.
 *
 * Entries for deleted files are left behind; they are a few bytes each, and a
 * file moved into Private under a different name (only on a name clash) just
 * falls back to its own date.
 */
object AddedDateStore {

    private const val PREFS = "gallery_added_dates"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun record(context: Context, fileName: String, addedAt: Long) {
        prefs(context).edit().putLong(fileName, addedAt).apply()
    }

    fun remove(context: Context, fileName: String) {
        prefs(context).edit().remove(fileName).apply()
    }

    /** Read once per listing, not per file. */
    fun all(context: Context): Map<String, Long> =
        prefs(context).all.mapNotNull { (name, value) -> (value as? Long)?.let { name to it } }.toMap()
}
