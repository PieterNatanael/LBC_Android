package com.example.bodycamoutdoorgear

import com.example.bodycamoutdoorgear.sound.ShutterSoundClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

class ShutterSoundClipTest {

    private fun tone(seconds: Double, amplitude: Float = 0.5f) =
        FloatArray((seconds * ShutterSoundClip.SAMPLE_RATE).toInt()) { amplitude * sin(it * 0.05f) }

    // 1.2 s of silence, a 1.4 s sound, 1.2 s of silence
    private val recording = FloatArray(52920) + tone(1.4) + FloatArray(52920)

    @Test
    fun loudestWindowCoversTheSound() {
        val start = ShutterSoundClip.loudestWindowStart(recording)
        val startSec = start.toDouble() / ShutterSoundClip.SAMPLE_RATE
        assertTrue("window should start near the sound, was $startSec", startSec in 1.0..1.25)
    }

    @Test
    fun shortRecordingKeepsWholeClip() {
        assertEquals(0, ShutterSoundClip.loudestWindowStart(tone(1.0)))
    }

    @Test
    fun peaksAreScaledToOne() {
        val peaks = ShutterSoundClip.peaks(tone(2.0, 0.2f), 90)
        assertEquals(1f, peaks.max(), 0.0001f)
    }

    @Test
    fun processedClipIsCappedPaddedAndLouder() {
        val out = ShutterSoundClip.process(tone(4.0, 0.1f))
        val pad = (ShutterSoundClip.LEADING_SILENCE * ShutterSoundClip.SAMPLE_RATE).toInt()
        assertEquals(pad + ShutterSoundClip.maxSamples, out.size)
        assertTrue(out.take(pad).all { it.toInt() == 0 })
        assertTrue("quiet clip should be boosted", out.maxOf { kotlin.math.abs(it.toInt()) } > 20000)
        assertEquals(0, out.last().toInt())
    }

    @Test
    fun wavFileHasAValidHeader() {
        val pcm = ShutterSoundClip.process(tone(0.5))
        val file = File.createTempFile("clip", ".wav")
        ShutterSoundClip.writeWav(file, pcm)
        val b = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(44 + pcm.size * 2, b.capacity())
        assertEquals("RIFF", String(ByteArray(4).also { b.get(0); b.position(0); b.get(it) }))
        b.position(20)
        assertEquals(1, b.short.toInt())          // PCM
        assertEquals(1, b.short.toInt())          // mono
        assertEquals(44100, b.int)
        b.position(34)
        assertEquals(16, b.short.toInt())         // bits
        b.position(40)
        assertEquals(pcm.size * 2, b.int)
        file.delete()
    }

    // ── Received files ──────────────────────────────────────────────────────

    private fun wavBytes(pcm: ShortArray, rate: Int = 44100, channels: Int = 1, bits: Int = 16): ByteArray {
        val dataBytes = pcm.size * 2
        val buf = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray()).putInt(36 + dataBytes).put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(channels.toShort())
            .putInt(rate).putInt(rate * 2).putShort(2).putShort(bits.toShort())
        buf.put("data".toByteArray()).putInt(dataBytes)
        for (v in pcm) buf.putShort(v)
        return buf.array()
    }

    @Test
    fun readsOurOwnWavBackExactly() {
        val pcm = ShutterSoundClip.process(tone(0.5))
        val file = File.createTempFile("own", ".wav")
        ShutterSoundClip.writeWav(file, pcm)
        val read = ShutterSoundClip.readWavPcm16(file.readBytes())
        assertTrue(read != null && read.contentEquals(pcm))
        file.delete()
    }

    @Test
    fun readsAnIosWavThatHasAJunkChunk() {
        // Made by the iOS app: 0.5 s tone plus 0.1 s of leading silence, with a JUNK chunk before "fmt ".
        val bytes = javaClass.getResourceAsStream("/ios_sound.wav")!!.readBytes()
        val read = ShutterSoundClip.readWavPcm16(bytes)
        assertEquals(26460, read?.size)
        assertTrue(read!!.take(4410).all { it.toInt() == 0 })
    }

    @Test
    fun rejectsWrongFormatsAndOverlongSounds() {
        val ok = ShortArray(20000) { 100 }
        assertTrue(ShutterSoundClip.readWavPcm16(wavBytes(ok)) != null)
        assertEquals(null, ShutterSoundClip.readWavPcm16(wavBytes(ok, rate = 48000)))
        assertEquals(null, ShutterSoundClip.readWavPcm16(wavBytes(ok, channels = 2)))
        assertEquals(null, ShutterSoundClip.readWavPcm16(wavBytes(ok, bits = 8)))
        assertEquals(null, ShutterSoundClip.readWavPcm16(wavBytes(ShortArray(80000))))   // 1.8 s
        assertEquals(null, ShutterSoundClip.readWavPcm16(ByteArray(100)))
        assertEquals(null, ShutterSoundClip.readWavPcm16("RIFFxxxxWAVE".toByteArray() + ByteArray(60)))
        assertEquals(null, ShutterSoundClip.readWavPcm16(ByteArray(400_000)))
    }
}
