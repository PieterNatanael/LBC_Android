package com.example.bodycamoutdoorgear.i18n

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.util.Locale

/**
 * The languages the app can be shown in, chosen in Settings (like the iOS app).
 * [code] is the language's name in the translation files and on the server.
 * [nativeName] is always written in that language's own script, so someone who
 * ends up in a language they can't read can still find their own.
 */
enum class AppLanguage(val code: String?, val nativeName: String) {
    SYSTEM(null, "System"),
    ENGLISH("en", "English"),
    JAPANESE("ja", "日本語"),
    GERMAN("de", "Deutsch"),
    FRENCH("fr", "Français"),
    CHINESE_SIMPLIFIED("zh-Hans", "简体中文"),
    KOREAN("ko", "한국어"),
    CHINESE_TRADITIONAL("zh-Hant", "繁體中文"),
    INDONESIAN("id", "Bahasa Indonesia"),
    TAGLISH("fil", "Taglish"),
}

/**
 * Looks up the translation of the app's text.
 *
 * The English text itself is the key: `tr("Cancel")` returns the translation of
 * "Cancel" in the chosen language, or "Cancel" when there is none. Translations
 * are plain JSON files in assets/translations, one per language, so a missing
 * entry shows English rather than nothing.
 *
 * Changing the language bumps a Compose state that [tr] reads, so screens that
 * show text update on their own, without restarting the app.
 */
object Tr {
    private const val PREFS = "bodycam_settings"
    private const val KEY = "app_language"

    private var appContext: Context? = null
    private var table: Map<String, String> = emptyMap()
    private var tableCode: String? = null
    private val version = mutableIntStateOf(0)

    /** What the user picked in Settings (System by default). */
    var selected: AppLanguage by mutableStateOf(AppLanguage.SYSTEM)
        private set

    /** The language actually used: the pick, or the phone's language, or English when we have no translation. */
    var code: String = "en"
        private set

    val locale: Locale get() = Locale.forLanguageTag(code)

    /**
     * The language of the website pages to open: the website has English, Japanese,
     * German, French, Korean and (simplified) Chinese. Traditional Chinese opens the
     * Chinese pages, and Indonesian and Taglish open the English ones.
     */
    private val siteLanguage: String get() = when (code) {
        "zh-Hans", "zh-Hant" -> "zh"
        "id", "fil" -> "en"
        else -> code
    }

    val privacyUrl: String get() = "https://gogoodmood.com/apps/$siteLanguage/bodycamz/privacy/"
    val termsUrl: String get() = "https://gogoodmood.com/apps/$siteLanguage/bodycamz/terms/"

    /** Call once at startup, before any text is shown. */
    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        val saved = appContext!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        selected = AppLanguage.entries.firstOrNull { it.name == saved } ?: AppLanguage.SYSTEM
        apply()
    }

    fun select(language: AppLanguage) {
        val context = appContext ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, language.name).apply()
        selected = language
        apply()
    }

    private fun apply() {
        code = resolve(selected.code ?: Locale.getDefault().toLanguageTag())
        if (tableCode != code) {
            table = load(code)
            tableCode = code
        }
        version.intValue++
    }

    /** Maps a language tag ("de-DE", "zh-Hant-TW", "in") to one of the languages we have. */
    fun resolve(tag: String): String {
        val id = tag.lowercase(Locale.ROOT)
        if (id.startsWith("zh")) {
            if (id.contains("hans")) return "zh-Hans"
            if (id.contains("hant") || id.endsWith("-tw") || id.endsWith("-hk") || id.endsWith("-mo")) return "zh-Hant"
            return "zh-Hans"
        }
        // Filipino is "fil" (or the older "tl"): three letters, so it is checked before the two-letter cut below.
        if (id.startsWith("fil") || id.startsWith("tl")) return "fil"
        // Android still reports Indonesian as "in" on some versions.
        val base = if (id.startsWith("in")) "id" else id.take(2)
        return AppLanguage.entries.firstOrNull { it.code == base }?.code ?: "en"
    }

    private fun load(code: String): Map<String, String> {
        if (code == "en") return emptyMap()
        val context = appContext ?: return emptyMap()
        return try {
            val json = context.assets.open("translations/$code.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val obj = JSONObject(json)
            buildMap { obj.keys().forEach { put(it, obj.getString(it)) } }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun translate(text: String): String {
        version.intValue // read, so screens showing this text update when the language changes
        return table[text] ?: text
    }
}

/** The translation of [text] in the app's language. */
fun tr(text: String): String = Tr.translate(text)

/** Like [tr], for text with placeholders: `trf("%d selected", count)`. Numbers follow the app's language. */
fun trf(format: String, vararg args: Any?): String = String.format(Tr.locale, Tr.translate(format), *args)
