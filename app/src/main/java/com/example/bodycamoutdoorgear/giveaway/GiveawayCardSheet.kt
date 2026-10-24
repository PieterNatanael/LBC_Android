package com.example.bodycamoutdoorgear.giveaway

import com.example.bodycamoutdoorgear.i18n.tr
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

private val SheetYellow = Color(0xFFFFD84D)
private val SheetGrey = Color(0xFF2E2E2E)

/**
 * The card made from a Giveaway photo: a preview, the words you can edit, and
 * Close / Edit text / Save. Nothing goes to the gallery until you tap Save.
 *
 * [photoFile] is the photo as taken, in a temporary folder. It is removed once
 * the card is saved (or thrown away), or moved to the gallery if no card could
 * be made.
 */
@Composable
fun GiveawayCardSheet(
    photoFile: File,
    onClose: () -> Unit,
    onOpenGallery: () -> Unit,
    isPremium: Boolean = false,
    /** [photoFile] is a recording rather than a photo: the card is a short video. */
    isVideo: Boolean = false,
    /** A giveaway card (yellow) or a sale card (blue, with a price). */
    mode: CardMode = CardMode.GIVEAWAY,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sale = mode == CardMode.SALE
    // Save buttons are yellow for a giveaway and light blue for a sale.
    val accent = if (sale) Color(0xFFCFE3FF) else SheetYellow

    var agreed by rememberSaveable { mutableStateOf(!(if (sale) SaleAgreement.needed(context) else GiveawayAgreement.needed(context))) }

    // Fill with AI. [before] holds the words from before the FIRST fill, so Revert
    // always goes back to what the user had, even after retries.
    var working by remember { mutableStateOf(false) }
    var before by remember { mutableStateOf<GiveawayCardContent?>(null) }
    var aiMessage by remember { mutableStateOf<String?>(null) }
    var aiInfo by remember { mutableStateOf<String?>(null) }
    var showAiConsent by remember { mutableStateOf(false) }
    var remaining by remember { mutableStateOf(GiveawayAIUsage.remaining(context, isPremium)) }  // null: not known yet

    var content by remember { mutableStateOf(GiveawayCardContent.fallback(mode)) }
    var priceAmount by rememberSaveable { mutableStateOf("") }
    var currencyCode by remember { mutableStateOf(SaleCurrency.savedCode(context)) }
    val style: CardStyle = if (sale) CardStyle.Sale(SalePrice.format(priceAmount, currencyCode)) else CardStyle.Giveaway
    val token = remember { System.currentTimeMillis().toString() }
    var card by remember { mutableStateOf<ImageBitmap?>(null) }
    var cardJpeg by remember { mutableStateOf<ByteArray?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    /** True once the card has been saved to the gallery at least once. */
    var savedToGallery by remember { mutableStateOf(false) }
    /** The card was changed (edited) after it was saved. */
    var dirtySinceSave by remember { mutableStateOf(false) }
    var showCloseConfirm by remember { mutableStateOf(false) }
    var firstRender by remember { mutableStateOf(true) }
    // Video cards: the exported clip, whether it has sound, and whether making it failed.
    var cardVideo by remember { mutableStateOf<File?>(null) }
    var includeSound by remember { mutableStateOf(true) }
    var exportFailed by remember { mutableStateOf(false) }
    val ready = if (isVideo) cardVideo != null else card != null

    /** Saved, and nothing changed since. */
    val allSaved = savedToGallery && !dirtySinceSave

    // Draw the card now, and again shortly after the last edit.
    LaunchedEffect(content, priceAmount, currencyCode) {
        if (isVideo) return@LaunchedEffect
        if (!firstRender) delay(400)
        firstRender = false
        val cleaned = content.cleaned()
        val bmp = withContext(Dispatchers.Default) { GiveawayCardRenderer.render(context, cleaned, photoFile, cardStyle = style) }
        val bytes = withContext(Dispatchers.Default) {
            ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }.toByteArray()
        }
        card = bmp.asImageBitmap()
        cardJpeg = bytes
        // A card that was already saved is now out of date.
        if (savedToGallery) dirtySinceSave = true
    }

    // Video cards are made by the exporter, again shortly after the last edit or sound change.
    // Each export gets its own file so the preview never plays one that is being rewritten.
    LaunchedEffect(content, includeSound, priceAmount, currencyCode) {
        if (!isVideo) return@LaunchedEffect
        if (!firstRender) delay(400)
        firstRender = false
        val out = File(GiveawayStore.originalsDir(context), "card_${token}_${System.nanoTime()}.mp4")
        val ok = GiveawayVideoExporter.export(context, photoFile, content.cleaned(), includeSound, out, style)
        if (ok) {
            val old = cardVideo
            cardVideo = out
            exportFailed = false
            old?.delete()
            if (savedToGallery) dirtySinceSave = true
        } else {
            exportFailed = true
        }
    }

    /** Writes the card to the gallery. Saving again replaces the same file. */
    fun saveNow(): Boolean {
        if (isVideo) {
            val video = cardVideo ?: return false
            if (!GiveawayStore.saveCardVideo(context, video, token)) return false
            savedToGallery = true
            dirtySinceSave = false
            return true
        }
        val bytes = cardJpeg ?: return false
        if (!GiveawayStore.saveCardImage(context, bytes, token)) return false
        savedToGallery = true
        dirtySinceSave = false
        return true
    }

    /** Leaves the sheet. The original photo is only thrown away once a card made from it is saved, or on purpose. */
    fun finish(discard: Boolean) {
        cardVideo?.delete()
        if (discard || savedToGallery) photoFile.delete() else GiveawayStore.rescueOriginal(context, photoFile)
        onClose()
    }

    fun runFill() {
        // No check against a count here: the server decides, so a raised limit works at once.
        working = true
        aiMessage = null
        aiInfo = null
        scope.launch {
            val pictures = withContext(Dispatchers.Default) {
                if (isVideo) GiveawayImagePrep.videoFrames(photoFile) else listOfNotNull(GiveawayImagePrep.jpeg(photoFile))
            }
            val result = GiveawayAIService.fill(context, pictures, isPremium, mode)
            working = false
            result.onSuccess { outcome ->
                when (outcome) {
                    is GiveawayAIOutcome.Filled -> {
                        if (before == null) before = content
                        content = outcome.content.copy(dateMillis = content.dateMillis)
                        aiInfo = outcome.privacyNote
                        if (outcome.remaining != null) GiveawayAIUsage.setRemaining(context, outcome.remaining, isPremium)
                        else GiveawayAIUsage.recordUse(context, isPremium)
                    }
                    is GiveawayAIOutcome.Refused -> {
                        // The card stays as it is; only a message is shown.
                        aiMessage = outcome.message
                        if (outcome.remaining != null) GiveawayAIUsage.setRemaining(context, outcome.remaining, isPremium)
                    }
                }
                remaining = GiveawayAIUsage.remaining(context, isPremium)
            }.onFailure { e ->
                val error = (e as? GiveawayAIService.AIException)?.error ?: GiveawayAIError.UNAVAILABLE
                aiMessage = error.message
                if (error == GiveawayAIError.MONTHLY_LIMIT) remaining = 0
            }
        }
    }

    fun requestClose() {
        if (allSaved) finish(discard = false) else showCloseConfirm = true
    }

    Dialog(
        onDismissRequest = { requestClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
            // No imePadding(): the dialog window already resizes for the keyboard, and
            // adding it here as well made room for the keyboard twice.
            Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val shown = card
                    if (isVideo) {
                        val video = cardVideo
                        if (video != null) {
                            GiveawayVideoPreview(video, includeSound, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)))
                        } else {
                            Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                                if (exportFailed) Text(tr("Couldn't make the video card."), color = Color(0xFFFF9980), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                                else CircularProgressIndicator(color = Color.White)
                            }
                        }
                    } else if (shown != null) {
                        Image(
                            bitmap = shown,
                            contentDescription = tr("Giveaway card preview"),
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)),
                        )
                    } else {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                    if (isVideo) {
                        SheetButton(if (includeSound) tr("Sound: on") else tr("Sound: off"), SheetGrey, Color.White, Modifier.fillMaxWidth()) {
                            includeSound = !includeSound
                        }
                    }
                    if (sale) {
                        SalePriceField(
                            amount = priceAmount, onAmount = { priceAmount = it },
                            currencyCode = currencyCode,
                            onCurrency = { currencyCode = it; SaleCurrency.remember(context, it) },
                        )
                    }
                    if (showEditor) GiveawayTextEditor(content) { content = it }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        SheetButton(
                            if (working) tr("Writing…") else if (before == null) tr("Fill with AI") else tr("Try again"),
                            accent, Color.Black, Modifier.weight(1f), enabled = !working,
                        ) {
                            if (GiveawayAgreement.aiConsented(context)) runFill() else showAiConsent = true
                        }
                        if (before != null && !working) {
                            SheetButton(tr("Revert"), SheetGrey, Color.White) {
                                before?.let { content = it }
                                before = null; aiMessage = null; aiInfo = null
                            }
                        }
                    }
                    // No count is shown while there are cards left. Once the server has said there are none this
                    // month, the line says so. The button stays tappable: the server decides, so a raised limit works at once.
                    val limitText = if (remaining == 0) GiveawayAIError.MONTHLY_LIMIT.message else null
                    (aiMessage ?: aiInfo ?: limitText)?.let { line ->
                        Caption(line, if (aiInfo != null && aiMessage == null) accent else Color(0xFFFF9980))
                    }
                    if (allSaved) {
                        Caption(tr("Saved to your Gallery"), Color.White.copy(alpha = 0.6f))
                    } else if (ready) {
                        Caption(tr("Not saved yet"), accent.copy(alpha = 0.85f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        SheetButton(tr("Close"), SheetGrey, Color.White, Modifier.weight(1f)) { requestClose() }
                        SheetButton(if (showEditor) tr("Hide text") else tr("Edit text"), SheetGrey, Color.White, Modifier.weight(1f)) {
                            showEditor = !showEditor
                        }
                        if (allSaved) {
                            SheetButton(tr("Gallery"), accent, Color.Black, Modifier.weight(1f)) {
                                finish(discard = false)
                                // Cards are saved and shared outside LBC from the
                                // Gallery tab, so this leads there.
                                onOpenGallery()
                            }
                        } else {
                            SheetButton(
                                if (savedToGallery) tr("Save changes") else tr("Save"),
                                accent, Color.Black, Modifier.weight(1f), enabled = ready,
                            ) { saveNow() }
                        }
                    }
                    GiveawayResponsibilityLine(mode)
                }
            }
        }
    }

    if (!agreed) {
        GiveawayAgreementDialog(
            mode = mode,
            onAgree = { if (sale) SaleAgreement.accept(context) else GiveawayAgreement.accept(context); agreed = true },
            // Declining keeps the photo: it goes to the gallery as a normal photo.
            onDecline = { finish(discard = false) },
        )
    }

    if (showAiConsent) {
        AlertDialog(
            onDismissRequest = { showAiConsent = false },
            title = { Text(tr("Fill with AI")) },
            text = { Text(tr("This sends a small copy of your photo to our server and an AI service (Claude, by Anthropic) to write the text. We don't keep it.")) },
            confirmButton = {
                TextButton(onClick = { showAiConsent = false; GiveawayAgreement.acceptAI(context); runFill() }) { Text(tr("Continue")) }
            },
            dismissButton = { TextButton(onClick = { showAiConsent = false }) { Text(tr("Cancel")) } },
        )
    }

    if (showCloseConfirm) {
        AlertDialog(
            onDismissRequest = { showCloseConfirm = false },
            title = { Text(tr("Close without saving?")) },
            text = { Text(tr("This card won't be kept.")) },
            confirmButton = {
                TextButton(onClick = { showCloseConfirm = false; finish(discard = true) }) {
                    Text(tr("Close anyway"), color = Color(0xFFE8483B))
                }
            },
            dismissButton = { TextButton(onClick = { showCloseConfirm = false }) { Text(tr("Keep editing")) } },
        )
    }
}

@Composable
private fun Caption(text: String, color: Color) {
    Text(
        text, color = color, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SheetButton(
    text: String, fill: Color, textColor: Color, modifier: Modifier = Modifier,
    enabled: Boolean = true, onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(fill.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, color = textColor, fontSize = 14.sp, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center,
        )
    }
}

/** The editable words on the card: item name, up to three facts, the comment and the note. */
@Composable
private fun GiveawayTextEditor(content: GiveawayCardContent, onChange: (GiveawayCardContent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF1A1A1A)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EditorField(tr("Item"), content.itemName, GiveawayCardContent.MAX_NAME) { onChange(content.copy(itemName = it)) }
        for (i in 0 until GiveawayCardContent.MAX_FACTS) {
            EditorField(if (i == 0) tr("Facts") else "", content.facts.getOrElse(i) { "" }, GiveawayCardContent.MAX_FACT) { value ->
                val list = content.facts.toMutableList()
                while (list.size <= i) list.add("")
                list[i] = value
                onChange(content.copy(facts = list))
            }
        }
        EditorField(tr("Comment"), content.comment, GiveawayCardContent.MAX_COMMENT) { onChange(content.copy(comment = it)) }
        EditorField(tr("Note"), content.note, GiveawayCardContent.MAX_NOTE) { onChange(content.copy(note = it)) }
    }
}

@Composable
private fun EditorField(label: String, value: String, limit: Int, onChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold, modifier = Modifier.width(66.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.take(limit)) },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(SheetYellow),
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(SheetGrey).padding(10.dp),
        )
    }
}

/** Plays the card video in a loop, without controls. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun GiveawayVideoPreview(file: File, sound: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(file) {
        player.setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.fromFile(file)))
        player.prepare()
    }
    LaunchedEffect(sound) { player.volume = if (sound) 1f else 0f }
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { c ->
            androidx.media3.ui.PlayerView(c).apply {
                useController = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                this.player = player
            }
        },
        modifier = modifier,
    )
}
