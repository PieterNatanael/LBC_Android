package com.example.bodycamoutdoorgear.sound

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Turns a recording into a playable shutter sound. Every saved sound gets
 * 100 ms of leading silence: short sounds are otherwise sometimes swallowed
 * while the audio hardware wakes from idle.
 */
object ShutterSoundClip {
    const val SAMPLE_RATE = 44100
    const val MAX_SECONDS = 1.5
    const val LEADING_SILENCE = 0.1

    val maxSamples: Int get() = (MAX_SECONDS * SAMPLE_RATE).toInt()

    /** Bar heights for drawing, scaled so the loudest bar is 1. */
    fun peaks(samples: FloatArray, count: Int): FloatArray {
        if (samples.isEmpty() || count <= 0) return FloatArray(0)
        val per = max(1, samples.size / count)
        val raw = ArrayList<Float>()
        var start = 0
        while (start < samples.size) {
            var peak = 0f
            for (i in start until min(start + per, samples.size)) peak = max(peak, abs(samples[i]))
            raw.add(peak)
            start += per
        }
        val top = max(raw.max(), 0.0001f)
        return FloatArray(raw.size) { raw[it] / top }
    }

    fun loudestWindowStart(samples: FloatArray): Int {
        val window = maxSamples
        if (samples.size <= window) return 0
        val prefix = DoubleArray(samples.size + 1)
        for (i in samples.indices) prefix[i + 1] = prefix[i] + abs(samples[i])
        var best = 0
        var bestEnergy = -1.0
        var start = 0
        while (start + window <= samples.size) {
            val energy = prefix[start + window] - prefix[start]
            if (energy > bestEnergy) { bestEnergy = energy; best = start }
            start += 441
        }
        return best
    }

    /** The selected clip made louder, faded at the edges and padded in front. */
    fun process(samples: FloatArray): ShortArray {
        val clip = samples.copyOf(min(samples.size, maxSamples))
        val peak = clip.fold(0f) { acc, v -> max(acc, abs(v)) }
        if (peak > 0.001f) {
            val gain = min(0.9f / peak, 8f)
            for (i in clip.indices) clip[i] *= gain
        }
        val fade = min(176, clip.size / 2)
        for (i in 0 until fade) {
            val f = i.toFloat() / fade
            clip[i] *= f
            clip[clip.size - 1 - i] *= f
        }
        val pad = (LEADING_SILENCE * SAMPLE_RATE).toInt()
        val out = ShortArray(pad + clip.size)
        for (i in clip.indices) {
            out[pad + i] = (clip[i] * 32767f).roundToInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    /** Mono, 44.1 kHz, 16-bit PCM WAV. */
    fun writeWav(file: File, pcm: ShortArray) {
        val dataBytes = pcm.size * 2
        val buf = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray()).putInt(36 + dataBytes).put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort(2).putShort(16)
        buf.put("data".toByteArray()).putInt(dataBytes)
        for (v in pcm) buf.putShort(v)
        file.outputStream().use { it.write(buf.array()) }
    }
}
