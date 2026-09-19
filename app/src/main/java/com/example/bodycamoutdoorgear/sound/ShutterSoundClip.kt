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

    /** Largest file accepted from another device: a saved sound is about 140 KB. */
    const val MAX_IMPORT_BYTES = 300_000

    private val maxImportSamples: Int get() = ((MAX_SECONDS + LEADING_SILENCE + 0.05) * SAMPLE_RATE).toInt()

    /**
     * Reads a WAV received from another device. Only our own format is accepted
     * (mono, 44.1 kHz, 16-bit PCM) and only up to the length a saved sound can
     * be; anything else returns null. Chunks are scanned instead of assuming a
     * 44-byte header, because iOS writes extra ones (JUNK) before "fmt ".
     */
    fun readWavPcm16(bytes: ByteArray): ShortArray? {
        if (bytes.size < 44 || bytes.size > MAX_IMPORT_BYTES) return null
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun tag(at: Int) = String(bytes, at, 4, Charsets.US_ASCII)
        if (tag(0) != "RIFF" || tag(8) != "WAVE") return null

        var pos = 12
        var formatOk = false
        while (pos + 8 <= bytes.size) {
            val id = tag(pos)
            val size = b.getInt(pos + 4).toLong() and 0xFFFFFFFFL
            val body = pos + 8
            when (id) {
                "fmt " -> {
                    if (size < 16 || body + 16 > bytes.size) return null
                    formatOk = b.getShort(body).toInt() == 1 &&
                        b.getShort(body + 2).toInt() == 1 &&
                        b.getInt(body + 4) == SAMPLE_RATE &&
                        b.getShort(body + 14).toInt() == 16
                    if (!formatOk) return null
                }
                "data" -> {
                    if (!formatOk) return null
                    val length = min(size, (bytes.size - body).toLong()).toInt() / 2
                    if (length <= 0 || length > maxImportSamples) return null
                    return ShortArray(length) { b.getShort(body + it * 2) }
                }
            }
            val next = body.toLong() + size + (size and 1L)
            if (next > bytes.size) return null
            pos = next.toInt()
        }
        return null
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
