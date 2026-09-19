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
            ?: ShutterSound.CLASSIC

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
