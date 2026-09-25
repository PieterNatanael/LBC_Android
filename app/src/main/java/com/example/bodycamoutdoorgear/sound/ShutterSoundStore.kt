package com.example.bodycamoutdoorgear.sound

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Recorded sounds: WAV files in the app's files folder, names in preferences. Main thread only. */
class ShutterSoundStore private constructor(private val appContext: Context) {

    private val prefs = appContext.getSharedPreferences("bodycam_settings", Context.MODE_PRIVATE)
    private val dir = File(appContext.filesDir, "shutter_sounds")
    private val player = ShutterSoundPlayer.get(appContext)

    private val _custom = MutableStateFlow<List<ShutterSound>>(emptyList())
    val custom: StateFlow<List<ShutterSound>> = _custom.asStateFlow()

    init {
        val loaded = ArrayList<ShutterSound>()
        try {
            val arr = JSONArray(prefs.getString(KEY, "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.getString("id")
                val file = fileFor(id)
                if (file.exists()) {
                    loaded.add(ShutterSound(id, o.getString("emoji"), o.getString("name"),
                        isCustom = true, filePath = file.absolutePath))
                }
            }
        } catch (_: Exception) {}
        _custom.value = loaded
        loaded.forEach { player.preload(it) }
    }

    fun fileFor(id: String) = File(dir, "$id.wav")

    fun find(id: String?): ShutterSound =
        ShutterSound.builtIn.firstOrNull { it.id == id }
            ?: _custom.value.firstOrNull { it.id == id }
            ?: ShutterSound.CAT

    fun add(name: String, emoji: String, samples: FloatArray): ShutterSound {
        dir.mkdirs()
        val id = UUID.randomUUID().toString()
        val file = fileFor(id)
        ShutterSoundClip.writeWav(file, ShutterSoundClip.process(samples))
        val sound = ShutterSound(id, emoji, name, isCustom = true, filePath = file.absolutePath)
        _custom.value = _custom.value + sound
        persist()
        player.preload(sound)
        return sound
    }

    enum class ImportResult { ADDED, DUPLICATE, FULL, FAILED }

    /**
     * Adds a sound received from another device. Everything about it comes from
     * the network: the file name must be a UUID (it becomes a path here) and the
     * audio is re-written by us instead of being kept as received.
     */
    fun importReceived(fileName: String, title: String?, emoji: String?, received: File): ImportResult {
        if (!fileName.endsWith(".wav", ignoreCase = true)) return ImportResult.FAILED
        val id = try {
            UUID.fromString(fileName.dropLast(4)).toString()
        } catch (_: Exception) { return ImportResult.FAILED }
        if (_custom.value.any { it.id == id }) return ImportResult.DUPLICATE
        if (_custom.value.size >= MAX_CUSTOM) return ImportResult.FULL

        if (received.length() > ShutterSoundClip.MAX_IMPORT_BYTES) return ImportResult.FAILED
        val pcm = ShutterSoundClip.readWavPcm16(received.readBytes()) ?: return ImportResult.FAILED
        dir.mkdirs()
        val file = fileFor(id)
        ShutterSoundClip.writeWav(file, pcm)

        val cleanTitle = (title ?: "").filter { !it.isISOControl() }.trim()
        val name = cleanTitle.ifEmpty { "Shared sound" }.take(20)
        val icon = firstGrapheme((emoji ?: "").trim()) ?: "🎵"
        val sound = ShutterSound(id, icon, name, isCustom = true, filePath = file.absolutePath)
        _custom.value = _custom.value + sound
        persist()
        player.preload(sound)
        return ImportResult.ADDED
    }

    private fun firstGrapheme(text: String): String? {
        if (text.isEmpty()) return null
        val it = java.text.BreakIterator.getCharacterInstance()
        it.setText(text)
        val end = it.next()
        return if (end == java.text.BreakIterator.DONE) null else text.substring(0, end)
    }

    fun remove(sound: ShutterSound) {
        if (!sound.isCustom) return
        player.forget(sound)
        fileFor(sound.id).delete()
        _custom.value = _custom.value.filter { it.id != sound.id }
        persist()
    }

    private fun persist() {
        val arr = JSONArray()
        _custom.value.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("emoji", it.emoji))
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        const val MAX_CUSTOM = 10
        private const val KEY = "custom_shutter_sounds"

        @Volatile private var instance: ShutterSoundStore? = null

        fun get(context: Context): ShutterSoundStore =
            instance ?: synchronized(this) {
                instance ?: ShutterSoundStore(context.applicationContext).also { instance = it }
            }
    }
}
