package com.example.bodycamoutdoorgear.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * Plays the shutter sound. Plays on the media stream, the same as the beep
 * it replaces, so it follows the media volume.
 *
 * A sound that has not finished loading falls back to the classic beep, so a
 * shot is never silent by accident.
 */
class ShutterSoundPlayer private constructor(private val appContext: Context) {

    private val pool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val poolIds = HashMap<String, Int>()
    private val loaded = HashSet<Int>()

    init {
        pool.setOnLoadCompleteListener { _, poolId, status ->
            if (status == 0) synchronized(loaded) { loaded.add(poolId) }
        }
        ShutterSound.builtIn.forEach { preload(it) }
    }

    fun preload(sound: ShutterSound) {
        if (poolIds.containsKey(sound.id)) return
        val res = sound.rawRes
        val path = sound.filePath
        when {
            res != null -> poolIds[sound.id] = pool.load(appContext, res, 1)
            path != null -> poolIds[sound.id] = pool.load(path, 1)
        }
    }

    /** Releases a deleted custom sound. */
    fun forget(sound: ShutterSound) {
        val poolId = poolIds.remove(sound.id) ?: return
        synchronized(loaded) { loaded.remove(poolId) }
        pool.unload(poolId)
    }

    fun play(sound: ShutterSound) {
        preload(sound)
        val poolId = poolIds[sound.id]
        val ready = poolId != null && synchronized(loaded) { loaded.contains(poolId) }
        if (poolId == null || !ready) {
            beep()
            return
        }
        pool.play(poolId, 1f, 1f, 1, 0, 1f)
    }

    private fun beep() {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 35)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
            Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, 300)
        } catch (_: Exception) {}
    }

    companion object {
        @Volatile private var instance: ShutterSoundPlayer? = null

        fun get(context: Context): ShutterSoundPlayer =
            instance ?: synchronized(this) {
                instance ?: ShutterSoundPlayer(context.applicationContext).also { instance = it }
            }
    }
}
