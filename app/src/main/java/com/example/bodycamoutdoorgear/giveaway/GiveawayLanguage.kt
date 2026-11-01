package com.example.bodycamoutdoorgear.giveaway

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.bodycamoutdoorgear.i18n.Tr
import java.util.Locale

/**
 * The language Giveaway writes the card in: the one the user picked in the AI menu, or,
 * when none is picked, the app's language (Settings, or the phone's), limited to the
 * languages the card has been translated into. Anything else is English.
 *
 * To add a language: add it to [SUPPORTED] and [NAMES], give it one more table in
 * `GiveawayCardStrings.forLanguage` and `GiveawayCardWords.forLanguage`, and add the
 * same language to the server.
 */
object GiveawayLanguage {
    private const val PREFS = "giveaway"
    private const val KEY = "card_language"

    val SUPPORTED = listOf(
        "en", "de", "id", "ja", "fr", "ko", "zh-Hans", "zh-Hant", "fil",
        "ru", "uk", "es", "pt", "tr", "vi", "hi", "it",
    )

    /** Each language in its own words, in the order the picker lists them. */
    val NAMES = listOf(
        "en" to "English", "es" to "Español", "pt" to "Português", "fr" to "Français",
        "de" to "Deutsch", "it" to "Italiano", "ru" to "Русский", "uk" to "Українська",
        "tr" to "Türkçe", "vi" to "Tiếng Việt", "hi" to "हिन्दी", "id" to "Bahasa Indonesia",
        "fil" to "Filipino", "ja" to "日本語", "ko" to "한국어", "zh-Hans" to "简体中文", "zh-Hant" to "繁體中文",
    )

    private var appContext: Context? = null

    /** The AI menu's pick. Empty means "same as the app". A Compose state, so the screens redraw when it changes. */
    var saved: String by mutableStateOf("")
        private set

    /** Call once at startup. */
    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        saved = appContext!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
    }

    fun select(code: String) {
        val context = appContext ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, code).apply()
        saved = code
    }

    fun name(code: String): String = NAMES.firstOrNull { it.first == code }?.second ?: "English"

    /** The card language: the saved pick when there is one, else the app's. */
    val code: String get() = if (saved in SUPPORTED) saved else appCode

    /** The app's own language, limited to [SUPPORTED]. A picked app language wins over the phone's. */
    val appCode: String get() = resolve(Tr.selected.code ?: Locale.getDefault().toLanguageTag())

    /** Maps a language tag ("de-DE", "zh-Hant-TW", "in") to one of [SUPPORTED]. */
    fun resolve(tag: String): String {
        val id = tag.lowercase(Locale.ROOT)
        // Chinese is told apart by script, then by region: Tr already knows how.
        if (id.startsWith("zh")) return Tr.resolve(tag)
        // Filipino is "fil" (or the older "tl"): three letters, so it is checked before the two-letter cut below.
        if (id.startsWith("fil") || id.startsWith("tl")) return "fil"
        // Android still reports Indonesian as "in" on some versions.
        val base = if (id.startsWith("in")) "id" else id.take(2)
        return if (base in SUPPORTED) base else "en"
    }
}
