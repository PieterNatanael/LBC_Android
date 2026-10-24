package com.example.bodycamoutdoorgear.ui.settings

import com.example.bodycamoutdoorgear.i18n.trf
import com.example.bodycamoutdoorgear.i18n.tr
import android.Manifest
import com.example.bodycamoutdoorgear.ui.theme.themedShape
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.bodycamoutdoorgear.sound.ShutterSound
import com.example.bodycamoutdoorgear.sound.ShutterSoundClip
import com.example.bodycamoutdoorgear.sound.ShutterSoundStore
import java.io.File
import java.text.BreakIterator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// ── Recorder ─────────────────────────────────────────────────────────────────

class ShutterSoundRecorder(private val context: Context) {
    enum class Phase { READY, RECORDING, TRIMMING, DENIED }

    var phase by mutableStateOf(Phase.READY)
    var elapsed by mutableStateOf(0f)
    var level by mutableStateOf(0f)
    var samples by mutableStateOf(FloatArray(0))
    var peaks by mutableStateOf(FloatArray(0))
    var windowStart by mutableStateOf(0)
    var isPlaying by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)

    @Volatile private var stopRequested = false
    private var closed = false
    private var player: MediaPlayer? = null
    private val main = Handler(Looper.getMainLooper())
    private val previewFile = File(context.cacheDir, "shutter_preview.wav")

    val selection: FloatArray
        get() = samples.copyOfRange(windowStart, min(windowStart + ShutterSoundClip.maxSamples, samples.size))

    @SuppressLint("MissingPermission")
    fun startRecording() {
        stopPlayback()
        message = null
        val rate = ShutterSoundClip.SAMPLE_RATE
        val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val record = try {
            AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, max(minBuf, rate))
        } catch (_: Exception) { null }
        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            record?.release()
            message = tr("Could not start the microphone.")
            return
        }
        stopRequested = false
        elapsed = 0f
        level = 0f
        phase = Phase.RECORDING

        Thread {
            val total = MAX_RECORD_SECONDS * rate
            val pcm = ShortArray(total)
            var count = 0
            try {
                record.startRecording()
                val chunk = ShortArray(2048)
                while (!stopRequested && count < total) {
                    val n = record.read(chunk, 0, min(chunk.size, total - count))
                    if (n <= 0) break
                    var peak = 0
                    for (i in 0 until n) {
                        pcm[count + i] = chunk[i]
                        peak = max(peak, abs(chunk[i].toInt()))
                    }
                    count += n
                    val seconds = count.toFloat() / rate
                    val lvl = sqrt(peak / 32768f)
                    main.post { elapsed = seconds; level = lvl }
                }
            } catch (_: Exception) {
            } finally {
                try { record.stop() } catch (_: Exception) {}
                record.release()
            }
            val floats = FloatArray(count) { pcm[it] / 32768f }
            main.post { finishRecording(floats) }
        }.start()
    }

    fun stopRecording() {
        if (phase == Phase.RECORDING) stopRequested = true
    }

    private fun finishRecording(floats: FloatArray) {
        if (closed || phase != Phase.RECORDING) return
        if (floats.size < MIN_SAMPLES) {
            message = tr("That was too short. Try again.")
            phase = Phase.READY
            return
        }
        samples = floats
        peaks = ShutterSoundClip.peaks(floats, 90)
        windowStart = ShutterSoundClip.loudestWindowStart(floats)
        phase = Phase.TRIMMING
    }

    fun recordAgain() {
        stopPlayback()
        samples = FloatArray(0)
        peaks = FloatArray(0)
        phase = Phase.READY
    }

    fun togglePreview() {
        if (isPlaying) { stopPlayback(); return }
        try {
            ShutterSoundClip.writeWav(previewFile, ShutterSoundClip.process(selection))
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            )
            mp.setDataSource(previewFile.path)
            mp.setOnCompletionListener { stopPlayback() }
            mp.prepare()
            mp.start()
            player = mp
            isPlaying = true
        } catch (_: Exception) {
            stopPlayback()
        }
    }

    private fun stopPlayback() {
        try { player?.release() } catch (_: Exception) {}
        player = null
        isPlaying = false
    }

    fun tearDown() {
        closed = true
        stopRequested = true
        stopPlayback()
        previewFile.delete()
    }

    companion object {
        const val MAX_RECORD_SECONDS = 5
        private const val MIN_SAMPLES = 4410
    }
}

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun RecordShutterSoundDialog(
    accent: Color,
    onDismiss: () -> Unit,
    onSaved: (ShutterSound) -> Unit
) {
    val context = LocalContext.current
    val rec = remember { ShutterSoundRecorder(context) }
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🎤") }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) rec.startRecording() else rec.phase = ShutterSoundRecorder.Phase.DENIED
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) rec.stopRecording()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            rec.tearDown()
        }
    }

    fun startWithPermission() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) rec.startRecording() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun save() {
        val finalName = name.trim().ifEmpty { tr("My sound") }.take(20)
        val finalEmoji = firstGrapheme(emoji.trim()) ?: "🎤"
        try {
            onSaved(ShutterSoundStore.get(context).add(finalName, finalEmoji, rec.selection))
        } catch (_: Exception) {
            Toast.makeText(context, tr("Could not save the sound"), Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .systemBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tr("RECORD A SOUND"),
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp
                    )
                    Icon(
                        Icons.Filled.Close, contentDescription = tr("Close"), tint = Color.White.copy(0.6f),
                        modifier = Modifier.size(28.dp).clickable { onDismiss() }
                    )
                }

                when (rec.phase) {
                    ShutterSoundRecorder.Phase.READY -> {
                        Spacer(Modifier.height(12.dp))
                        Hint(trf("Record up to %d seconds. You will then pick the best 1.5 seconds of it.", ShutterSoundRecorder.MAX_RECORD_SECONDS))
                        rec.message?.let { Text(it, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
                        RoundActionButton(accent, isStop = false) { startWithPermission() }
                        Label(tr("TAP TO RECORD"))
                    }

                    ShutterSoundRecorder.Phase.RECORDING -> {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "%.1f / %d s".format(rec.elapsed, ShutterSoundRecorder.MAX_RECORD_SECONDS),
                            color = Color.White, fontSize = 22.sp,
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth().height(10.dp)
                                .clip(CircleShape).background(Color.White.copy(0.15f))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(max(0.02f, rec.level)).height(10.dp)
                                    .clip(CircleShape).background(accent)
                            )
                        }
                        RoundActionButton(accent, isStop = true) { rec.stopRecording() }
                        Label(tr("TAP TO STOP"))
                    }

                    ShutterSoundRecorder.Phase.TRIMMING -> {
                        val canDrag = rec.samples.size > ShutterSoundClip.maxSamples
                        Hint(
                            if (canDrag) tr("Drag to choose the 1.5 seconds you want to keep.")
                            else tr("Your sound is 1.5 seconds or shorter, so all of it is kept.")
                        )
                        TrimWaveform(
                            peaks = rec.peaks,
                            totalSamples = rec.samples.size,
                            windowStart = rec.windowStart,
                            onWindowStart = { rec.windowStart = it },
                            canDrag = canDrag,
                            accent = accent
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PillButton(
                                if (rec.isPlaying) tr("STOP") else tr("PLAY"),
                                if (rec.isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                Modifier.weight(1f)
                            ) { rec.togglePreview() }
                            PillButton(tr("RE-RECORD"), Icons.Filled.Refresh, Modifier.weight(1f)) { rec.recordAgain() }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = emoji,
                                onValueChange = { emoji = lastGrapheme(it) },
                                modifier = Modifier.width(72.dp),
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 22.sp, textAlign = TextAlign.Center),
                                colors = fieldColors()
                            )
                            OutlinedTextField(
                                value = name,
                                onValueChange = { if (it.length <= 20) name = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                placeholder = { Text(tr("Name"), color = Color.White.copy(0.3f), fontFamily = FontFamily.Monospace) },
                                textStyle = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                                colors = fieldColors()
                            )
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(themedShape(8.dp))
                                .background(accent).clickable { save() }.padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                tr("SAVE SOUND"), color = accent.readableTextColor(), fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp
                            )
                        }
                    }

                    ShutterSoundRecorder.Phase.DENIED -> {
                        Spacer(Modifier.height(12.dp))
                        Hint(tr("LBC needs microphone access to record a sound. You can allow it in Settings."))
                        PillButton(tr("OPEN SETTINGS"), Icons.Filled.Refresh, Modifier.fillMaxWidth()) {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                        PillButton(tr("TRY AGAIN"), Icons.Filled.Refresh, Modifier.fillMaxWidth()) {
                            rec.phase = ShutterSoundRecorder.Phase.READY
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = Color.White.copy(0.6f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
}

@Composable
private fun Label(text: String) {
    Text(text, color = Color.White.copy(0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
}

@Composable
private fun RoundActionButton(accent: Color, isStop: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(84.dp).border(3.dp, Color.White, CircleShape).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isStop) Box(Modifier.size(30.dp).clip(themedShape(6.dp)).background(accent))
        else Box(Modifier.size(66.dp).clip(CircleShape).background(accent))
    }
}

@Composable
private fun PillButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(themedShape(6.dp))
            .background(Color.White.copy(0.15f))
            .border(1.dp, Color.White.copy(0.35f), themedShape(6.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(0.85f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White.copy(0.85f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color.White.copy(0.7f),
    unfocusedBorderColor = Color.White.copy(0.3f),
    cursorColor = Color.White
)

@Composable
private fun TrimWaveform(
    peaks: FloatArray,
    totalSamples: Int,
    windowStart: Int,
    onWindowStart: (Int) -> Unit,
    canDrag: Boolean,
    accent: Color
) {
    var widthPx by remember { mutableStateOf(1f) }
    val currentStart by rememberUpdatedState(windowStart)
    val windowSamples = min(ShutterSoundClip.maxSamples, totalSamples)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(themedShape(8.dp))
            .background(Color.White.copy(0.08f))
            .onSizeChanged { widthPx = max(1f, it.width.toFloat()) }
            .pointerInput(canDrag, totalSamples) {
                if (!canDrag) return@pointerInput
                detectDragGestures { change, drag ->
                    change.consume()
                    val delta = (drag.x / widthPx * totalSamples).toInt()
                    onWindowStart((currentStart + delta).coerceIn(0, totalSamples - windowSamples))
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val total = max(totalSamples, 1).toFloat()
            val barW = size.width / max(peaks.size, 1)
            peaks.forEachIndexed { i, p ->
                val sampleIndex = i * totalSamples / max(peaks.size, 1)
                val inside = sampleIndex >= windowStart && sampleIndex < windowStart + windowSamples
                val h = max(3.dp.toPx(), p * (size.height - 16.dp.toPx()))
                drawRoundRect(
                    color = if (inside) Color.White else Color.White.copy(0.3f),
                    topLeft = Offset(i * barW + barW * 0.15f, (size.height - h) / 2f),
                    size = Size(barW * 0.7f, h),
                    cornerRadius = CornerRadius(barW * 0.35f)
                )
            }
            val boxX = size.width * windowStart / total
            val boxW = size.width * windowSamples / total
            drawRoundRect(accent.copy(alpha = 0.25f), Offset(boxX, 0f), Size(boxW, size.height), CornerRadius(6.dp.toPx()))
            drawRoundRect(accent, Offset(boxX, 0f), Size(boxW, size.height), CornerRadius(6.dp.toPx()), style = Stroke(3.dp.toPx()))
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

fun Color.readableTextColor(): Color =
    if (0.299f * red + 0.587f * green + 0.114f * blue > 0.55f) Color.Black else Color.White

private fun graphemes(text: String): List<String> {
    val it = BreakIterator.getCharacterInstance()
    it.setText(text)
    val out = ArrayList<String>()
    var start = it.first()
    var end = it.next()
    while (end != BreakIterator.DONE) {
        out.add(text.substring(start, end))
        start = end
        end = it.next()
    }
    return out
}

private fun firstGrapheme(text: String): String? = graphemes(text).firstOrNull()

private fun lastGrapheme(text: String): String = graphemes(text).lastOrNull() ?: ""
