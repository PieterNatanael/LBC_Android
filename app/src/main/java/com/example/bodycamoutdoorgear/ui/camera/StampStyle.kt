package com.example.bodycamoutdoorgear.ui.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.example.bodycamoutdoorgear.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The date stamp's wording, font and layout, shared by photo and video so the
 * two can't drift, and matched to the iOS app so the same stamp reads the same
 * on either platform.
 */
object StampStyle {
    private const val TAGLINE = "Shot live with LBC"

    @Volatile private var cachedTypeface: Typeface? = null

    /**
     * One bundled font, not the system monospace, so the stamp looks the same
     * on iOS and Android. Falls back to the system monospace if the resource
     * can't be loaded, so a bad build loses the look, not the stamp.
     */
    fun typeface(context: Context): Typeface =
        cachedTypeface ?: (
            ResourcesCompat.getFont(context.applicationContext, R.font.jetbrains_mono_bold)
                ?: Typeface.MONOSPACE
            ).also { cachedTypeface = it }

    /**
     * Locale.US, not the device default: some locales use a different
     * calendar or non-Latin digits, and a stamp meant to travel with the file
     * should read the same regardless of the capturing phone's language.
     */
    fun dateText(date: Date = Date()): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(date)

    /**
     * One line when it fits [availableWidth] at the paint's current text size,
     * otherwise the date and the tagline on two lines. The longer half on its
     * own is about half the width, so it fits wherever the whole line doesn't.
     */
    fun lines(paint: Paint, availableWidth: Float, dateText: String): List<String> {
        val oneLine = "$dateText · $TAGLINE"
        return if (paint.measureText(oneLine) <= availableWidth) listOf(oneLine)
        else listOf(dateText, TAGLINE)
    }

    /** Draws [lines] right-aligned, the last line sitting on [bottomBaseline]. */
    fun draw(canvas: Canvas, paint: Paint, lines: List<String>, rightX: Float, bottomBaseline: Float) {
        val lineHeight = paint.textSize * 1.3f
        lines.forEachIndexed { i, line ->
            val fromBottom = lines.size - 1 - i
            canvas.drawText(line, rightX, bottomBaseline - fromBottom * lineHeight, paint)
        }
    }
}
