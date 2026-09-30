package com.example.bodycamoutdoorgear.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Copies photos and videos picked in the system photo picker into a gallery
 * folder — filesDir for the normal gallery, Private's own subfolder for
 * Private, the same "whichever folder this instance shows" rule HipHop
 * receive already follows.
 *
 * The photo picker needs no storage permission: it hands over only what the
 * user selected.
 *
 * Imported files are ordinary gallery items. Nothing marks them, and no stamp
 * is added: a photo taken in LBC can already be stampless (the stamp is
 * optional), so a mark on imports would imply a distinction the app can't
 * actually back up.
 */
object MediaImporter {

    data class Result(val imported: Int = 0, val failed: Int = 0, val noSpace: Int = 0)

    private enum class Outcome { IMPORTED, FAILED, NO_SPACE }

    /** Free space kept back beyond the file being copied, so an import can
     *  never fill the phone to the point recording or the system fails. */
    private const val SPARE_BYTES = 100L * 1_000_000

    /** Decoding a JPEG larger than this many pixels in one go risks running
     *  out of memory, so it is decoded at reduced size instead. */
    private const val MAX_DECODE_PIXELS = 50_000_000L

    /**
     * One at a time: several full-size videos copying at once would multiply
     * the peak disk and memory use for no real speed gain.
     */
    suspend fun import(
        context: Context,
        uris: List<Uri>,
        folder: File,
        onProgress: (current: Int, total: Int) -> Unit
    ): Result = withContext(Dispatchers.IO) {
        folder.mkdirs()
        var imported = 0
        var failed = 0
        var noSpace = 0
        uris.forEachIndexed { index, uri ->
            onProgress(index + 1, uris.size)
            when (importOne(context, uri, folder)) {
                Outcome.IMPORTED -> imported++
                Outcome.FAILED   -> failed++
                Outcome.NO_SPACE -> noSpace++
            }
        }
        Result(imported, failed, noSpace)
    }

    private fun importOne(context: Context, uri: Uri, folder: File): Outcome {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri).orEmpty().lowercase(Locale.ROOT)
        val isVideo = mime.startsWith("video/")
        val isImage = mime.startsWith("image/")
        if (!isVideo && !isImage) return Outcome.FAILED

        val size = sizeOf(context, uri)
        val isPassThrough = mime == "image/jpeg" || mime == "image/png"
        // A JPEG made from a HEIC or WebP can be larger than its source, so
        // conversions ask for double the source size.
        val needed = if (isImage && !isPassThrough) size * 2 else size
        if (folder.usableSpace < needed + SPARE_BYTES) return Outcome.NO_SPACE

        val date = (if (isVideo) videoDate(context, uri) else imageDate(context, uri))
            ?: providerDate(context, uri)
            ?: System.currentTimeMillis()

        val ext = when {
            isVideo -> if (mime == "video/quicktime") "mov" else "mp4"
            mime == "image/png" -> "png"
            else -> "jpg"
        }
        val dest = uniqueFile(folder, date, ext)

        val ok = try {
            when {
                isVideo || isPassThrough -> copyStream(context, uri, dest)
                else -> convertToJpeg(context, uri, dest)
            }
        } catch (t: Throwable) {
            // Throwable, not Exception: decoding a huge photo can run out of
            // memory, and that should fail one import rather than the app.
            false
        }
        if (!ok) {
            dest.delete()
            return Outcome.FAILED
        }
        // The file's own date stays the date TAKEN (the grid's default order
        // and every thumbnail date use it); the import time is kept aside for
        // "Recently added" — see AddedDateStore.
        dest.setLastModified(date)
        AddedDateStore.record(context, dest.name, System.currentTimeMillis())
        return Outcome.IMPORTED
    }

    // ── Copying ─────────────────────────────────────────────────────────────

    private fun copyStream(context: Context, uri: Uri, dest: File): Boolean {
        val input = context.contentResolver.openInputStream(uri) ?: return false
        input.use { source -> FileOutputStream(dest).use { out -> source.copyTo(out) } }
        return true
    }

    /** HEIC, WebP and anything else the gallery can't list: the gallery only
     *  knows jpg, jpeg, png, mov and mp4. The orientation is baked into the
     *  pixels, the way this app's own photo stamp does it. */
    private fun convertToJpeg(context: Context, uri: Uri, dest: File): Boolean {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

        var sample = 1
        while ((bounds.outWidth.toLong() / sample) * (bounds.outHeight.toLong() / sample) > MAX_DECODE_PIXELS) {
            sample *= 2
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return false

        val degrees = try {
            resolver.openInputStream(uri)?.use { rotationDegrees(ExifInterface(it)) } ?: 0f
        } catch (e: Exception) { 0f }

        val upright = if (degrees != 0f) {
            val m = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
                .also { decoded.recycle() }
        } else decoded

        val written = FileOutputStream(dest).use { upright.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        upright.recycle()
        return written
    }

    private fun rotationDegrees(exif: ExifInterface): Float =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90  -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

    // ── Metadata ────────────────────────────────────────────────────────────

    /** 0 when the provider won't say (a length of -1 means "unknown"); the
     *  copy itself still fails cleanly if the disk turns out to be full. */
    private fun sizeOf(context: Context, uri: Uri): Long {
        val length = try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
        } catch (e: Exception) {
            0L
        }
        return length.coerceAtLeast(0L)
    }

    /** When the photo was taken, so the gallery sorts imports among the
     *  recordings by that moment rather than by the moment they were imported.
     *  ExifInterface applies the photo's own time zone offset when it has one. */
    private fun imageDate(context: Context, uri: Uri): Long? = try {
        context.contentResolver.openInputStream(uri)?.use { ExifInterface(it).dateTimeOriginal }
            ?.takeIf { it > MIN_PLAUSIBLE_DATE }
    } catch (e: Exception) { null }

    private fun videoDate(context: Context, uri: Uri): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val text = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE) ?: return null
            // e.g. "20240315T142530.000Z", always UTC.
            val format = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
            format.parse(text.take(15))?.time?.takeIf { it > MIN_PLAUSIBLE_DATE }
        } catch (e: Exception) { null } finally {
            try { retriever.release() } catch (e: Exception) { }
        }
    }

    /** Some devices report 1904 or 1970 for "no date"; anything before 1990
     *  is treated as missing rather than filing the import at the bottom. */
    private const val MIN_PLAUSIBLE_DATE = 631_152_000_000L

    /** Last resort: whatever date the picker's provider knows about. Each
     *  column is tried on its own because providers reject columns they
     *  don't have rather than returning nothing. */
    private fun providerDate(context: Context, uri: Uri): Long? {
        val columns = listOf(
            "datetaken" to 1L,          // ms
            "last_modified" to 1L,      // ms
            "date_modified" to 1000L    // seconds
        )
        for ((column, toMillis) in columns) {
            try {
                context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { c ->
                    if (c.moveToFirst() && !c.isNull(0)) {
                        val value = c.getLong(0) * toMillis
                        if (value > MIN_PLAUSIBLE_DATE) return value
                    }
                }
            } catch (e: Exception) { /* try the next column */ }
        }
        return null
    }

    /** A name in the same shape as a live capture's ("LBC_1755183041280.jpg"),
     *  nudged forward a millisecond until it's free, so a burst taken in the
     *  same instant can't overwrite itself. */
    private fun uniqueFile(folder: File, date: Long, ext: String): File {
        var stamp = date
        while (true) {
            val file = File(folder, "LBC_$stamp.$ext")
            if (!file.exists()) return file
            stamp++
        }
    }
}
