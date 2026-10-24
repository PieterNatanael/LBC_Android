package com.example.bodycamoutdoorgear.giveaway

import android.content.Context
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Crop
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Composition
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * Turns a recording into the square Giveaway card as an MP4: the clip plays inside
 * the card's photo window while the text around it stays fixed.
 *
 * The card is drawn once with that window cut out (see
 * [GiveawayCardRenderer.render] with transparentWindow) and laid over the footage.
 * The footage is cropped to the window's shape, stretched to the square canvas,
 * then squeezed back into the window by a matrix, which undoes the stretch exactly.
 */
@UnstableApi
object GiveawayVideoExporter {
    /** Longest clip a card can hold. Recording stops here in Giveaway mode, and anything longer is trimmed. */
    const val MAX_SECONDS = 9

    /** Writes the card video to [out]. Returns true on success; cancelling the caller cancels the export. */
    suspend fun export(
        context: Context,
        source: File,
        content: GiveawayCardContent,
        includeSound: Boolean,
        out: File,
        style: CardStyle = CardStyle.Giveaway,
    ): Boolean {
        val (videoW, videoH) = displaySize(source) ?: return false
        val overlayBitmap = withContext(Dispatchers.Default) {
            GiveawayCardRenderer.render(context, content, null, transparentWindow = true, cardStyle = style)
        }
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                val side = GiveawayCardRenderer.SIDE.toFloat()
                val frame = GiveawayCardRenderer.PHOTO_FRAME

                // 1. Cut the footage to the window's shape (centred), in normalised coordinates.
                val windowAspect = frame.width() / frame.height()
                val videoAspect = videoW.toFloat() / videoH
                val crop = if (videoAspect > windowAspect) {
                    val f = windowAspect / videoAspect
                    Crop(-f, f, -1f, 1f)
                } else {
                    val f = videoAspect / windowAspect
                    Crop(-1f, 1f, -f, f)
                }
                // 2. Stretch to the square canvas, 3. squeeze into the window.
                val toCanvas = Presentation.createForWidthAndHeight(
                    GiveawayCardRenderer.SIDE, GiveawayCardRenderer.SIDE, Presentation.LAYOUT_STRETCH_TO_FIT)
                val centreX = (frame.centerX() / side) * 2f - 1f
                val centreY = 1f - (frame.centerY() / side) * 2f
                val intoWindow = MatrixTransformation { _ ->
                    Matrix().apply {
                        setScale(frame.width() / side, frame.height() / side)
                        postTranslate(centreX, centreY)
                    }
                }
                // 4. The card, with its window cut out, over everything.
                val card = OverlayEffect(ImmutableList.of(BitmapOverlay.createStaticBitmapOverlay(overlayBitmap)))

                val item = EditedMediaItem.Builder(
                    MediaItem.Builder()
                        .setUri(Uri.fromFile(source))
                        .setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder().setEndPositionMs(MAX_SECONDS * 1000L).build())
                        .build())
                    .setRemoveAudio(!includeSound)
                    .setEffects(Effects(listOf<AudioProcessor>(), listOf<androidx.media3.common.Effect>(crop, toCanvas, intoWindow, card)))
                    .build()

                val transformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            if (cont.isActive) cont.resume(true)
                        }

                        override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                            out.delete()
                            if (cont.isActive) cont.resume(false)
                        }
                    })
                    .build()
                out.delete()
                transformer.start(item, out.absolutePath)
                cont.invokeOnCancellation {
                    // Cancelling must happen on the thread that started the export.
                    android.os.Handler(android.os.Looper.getMainLooper()).post { transformer.cancel(); out.delete() }
                }
            }
        }
    }

    /** The clip's size as it is displayed (a sideways-stored portrait clip is swapped). */
    private fun displaySize(file: File): Pair<Int, Int>? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(file.absolutePath)
            val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: return null
            val h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: return null
            val rotation = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) h to w else w to h
        } catch (_: Exception) {
            null
        } finally {
            r.release()
        }
    }
}
