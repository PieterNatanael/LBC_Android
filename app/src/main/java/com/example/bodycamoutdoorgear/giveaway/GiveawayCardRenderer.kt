package com.example.bodycamoutdoorgear.giveaway

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.media.ExifInterface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.example.bodycamoutdoorgear.R
import com.example.bodycamoutdoorgear.ui.camera.StampStyle
import java.io.File
import java.util.Date
import kotlin.math.max
import kotlin.math.min

/**
 * Draws the square (1080 x 1080) Giveaway card. A port of the iOS renderer, so a
 * card looks the same from either phone: same layout, same numbers.
 *
 * Safe to call off the main thread.
 */
object GiveawayCardRenderer {
    const val SIDE = 1080

    /** Where the photo (or video) sits on the card. */
    val PHOTO_FRAME = RectF(48f, 148f, 598f, 892f)

    private val YELLOW = Color.rgb(255, 216, 77)   // #FFD84D
    private val INK = Color.rgb(27, 26, 23)        // #1B1A17
    private val RED = Color.rgb(232, 72, 59)       // #E8483B
    private val SALE_BLUE = Color.rgb(59, 130, 246)    // #3B82F6, easy to tell from the yellow giveaway card in every kind of colour blindness
    private val SALE_LIGHT = Color.rgb(207, 227, 255)  // #CFE3FF
    private val SWAP_PINK = Color.rgb(224, 85, 155)    // #E0559B, still distinct from yellow and blue for red-green and blue-yellow colour blindness
    private val SWAP_LIGHT = Color.rgb(255, 217, 235)  // #FFD9EB

    private val HEAVY: Typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
    private val BOLD: Typeface = Typeface.create("sans-serif", Typeface.BOLD)
    private val MEDIUM: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    @Volatile private var trimmedLogo: Bitmap? = null

    /**
     * Renders the card. With [transparentWindow] the photo area is cut out
     * (alpha 0) and everything else is drawn as usual; the video exporter lays
     * this over the footage so the clip shows through the window.
     */
    fun render(
        context: Context,
        content: GiveawayCardContent,
        photoFile: File?,
        transparentWindow: Boolean = false,
        cardStyle: CardStyle = CardStyle.Giveaway,
    ): Bitmap {
        val strings = GiveawayCardStrings.current
        val sale = cardStyle as? CardStyle.Sale
        val swap = cardStyle as? CardStyle.Swap
        val isSale = sale != null
        val isSwap = swap != null
        val background = if (isSwap) SWAP_PINK else if (isSale) SALE_BLUE else YELLOW
        val accent = if (isSwap) SWAP_LIGHT else if (isSale) SALE_LIGHT else YELLOW
        val bmp = Bitmap.createBitmap(SIDE, SIDE, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(background)

        val size = SIDE.toFloat()
        val margin = 48f

        // Header
        drawFitted(c, if (isSwap) strings.swapHeader else if (isSale) strings.saleHeader else strings.header, RectF(margin, 36f, margin + 700f, 136f),
            maxSize = 96f, minSize = 60f, typeface = HEAVY, color = INK, kern = 4f, maxLines = 1)

        // LBC logo, top right. The date and time sit under it and the item name
        // and facts start below that. Without the logo the compact layout is used.
        var stampTop = 54f
        var columnTop = 148f
        val logo = logoBitmap(context)
        if (logo != null) {
            val box = RectF(0f, 0f, 270f, 225f)
            val aspect = logo.width.toFloat() / logo.height.toFloat()
            var w = box.width()
            var h = w / aspect
            if (h > box.height()) { h = box.height(); w = h * aspect }
            val logoRect = RectF(size - margin - w, 22f, size - margin, 22f + h)
            c.drawBitmap(logo, null, logoRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            stampTop = logoRect.bottom + 8f
            columnTop = stampTop + 56f + 22f
        }

        // Capture date and time, small. Same wording and font as the photo stamp.
        val stampPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = StampStyle.typeface(context)
            textSize = 21f
            color = Color.argb(153, Color.red(INK), Color.green(INK), Color.blue(INK))
            textAlign = Paint.Align.RIGHT
        }
        val stampLines = listOf(StampStyle.dateText(Date(content.dateMillis)), strings.shotLive)
        stampLines.forEachIndexed { i, line ->
            val top = stampTop + i * 28f
            c.drawText(line, size - margin, top - stampPaint.fontMetrics.ascent, stampPaint)
        }

        // Photo frame
        val frame = PHOTO_FRAME
        val framePath = Path().apply { addRoundRect(frame, 30f, 30f, Path.Direction.CW) }
        if (transparentWindow) {
            val clear = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
            c.drawPath(framePath, clear)
        } else {
            c.drawPath(framePath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
            c.save()
            c.clipPath(framePath)
            val photo = photoFile?.let { loadPhoto(it, 1600) }
            if (photo != null) {
                drawAspectFill(c, photo, frame)
                photo.recycle()
            } else {
                drawPlaceholder(c, frame)
            }
            c.restore()
        }
        c.drawPath(framePath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 6f; color = INK
        })

        // FREE stamp, overlapping the frame's top-right corner
        if (swap != null) drawWantedTag(c, swap.wanted ?: strings.wantedDefault, strings.wantedLabel, frame.right - 165f, frame.top + 84f)
        else if (sale != null) drawPriceTag(c, sale.price ?: strings.priceDefault, frame.right - 150f, frame.top + 78f)
        else drawStamp(c, strings.free, frame.right - 105f, frame.top + 78f)

        // Right column
        val colX = frame.right + 40f
        val colW = size - margin - colX

        // Item name: up to two lines, shrinks to fit
        val nameRect = RectF(colX, columnTop, colX + colW, columnTop + 118f)
        val nameHeight = drawFitted(c, content.itemName, nameRect,
            maxSize = 62f, minSize = 34f, typeface = HEAVY, color = INK, kern = -1f, maxLines = 2)

        // Facts, up to three, each its own bullet
        var y = nameRect.top + nameHeight + 22f
        val facts = content.facts.take(3)
        var factFont = factFontSize(facts, colW - 26f)
        // Everything must end above the LBC SAYS box (which starts at 700), so
        // shrink the facts until the facts and the note fit.
        val noteSpace = if (content.note.isEmpty()) 0f else 82f
        while (factFont > 18f && y + factsHeight(facts, colW - 26f, factFont) + noteSpace > 688f) factFont -= 1f
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK }
        for (fact in facts) {
            val rect = RectF(colX + 26f, y, colX + colW, y + 90f)
            c.drawCircle(colX + 6f, y + factFont * 0.5f, 4f, dot)
            val h = drawFitted(c, fact, rect, maxSize = factFont, minSize = factFont,
                typeface = MEDIUM, color = INK, kern = 0f, maxLines = 2)
            y += h + 14f
        }

        // Optional safety note, under the facts
        if (content.note.isNotEmpty()) {
            drawFitted(c, content.note, RectF(colX, y + 6f, colX + colW, y + 76f),
                maxSize = 23f, minSize = 16f, typeface = BOLD,
                color = Color.argb(179, Color.red(INK), Color.green(INK), Color.blue(INK)), kern = 0f, maxLines = 2)
        }

        // LBC SAYS box
        val box = RectF(colX, 700f, colX + colW, 892f)
        val boxPath = Path().apply { addRoundRect(box, 30f, 30f, Path.Direction.CW) }
        c.drawPath(boxPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        c.drawPath(boxPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f; color = INK })
        val inner = RectF(box.left + 24f, box.top + 20f, box.right - 24f, box.bottom - 20f)
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = HEAVY; textSize = 19f; color = INK; letterSpacing = 3f / 19f
        }
        c.drawText(strings.says, inner.left, inner.top - labelPaint.fontMetrics.ascent, labelPaint)
        drawFitted(c, "“${content.comment}”",
            RectF(inner.left, inner.top + 34f, inner.right, inner.bottom),
            maxSize = 30f, minSize = 18f, typeface = BOLD, color = INK, kern = 0f, maxLines = 5)

        // Bottom bar
        val bar = RectF(margin, 924f, size - margin, 1032f)
        c.drawRoundRect(bar, 30f, 30f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK })
        drawFitted(c, if (isSwap) strings.swapContact else if (isSale) strings.saleContact else strings.contact,
            RectF(bar.left + 34f, bar.top + 10f, bar.left + 34f + 560f, bar.bottom - 10f),
            maxSize = 50f, minSize = 30f, typeface = HEAVY, color = accent, kern = -0.5f, maxLines = 1,
            centerVertically = true)
        val battery = RectF(bar.left + 612f, bar.centerY() - 14f, bar.left + 612f + 54f, bar.centerY() + 14f)
        drawBatteryLogo(c, battery)
        val textX = battery.right + 16f
        val textR = bar.right - 24f
        val light = Color.rgb(217, 217, 217)
        drawFitted(c, strings.madeBy, RectF(textX, bar.top + 16f, textR, bar.top + 48f),
            maxSize = 28f, minSize = 20f, typeface = HEAVY, color = Color.WHITE, kern = 0f, maxLines = 1)
        drawFitted(c, strings.footer1, RectF(textX, bar.top + 50f, textR, bar.top + 74f),
            maxSize = 19f, minSize = 12f, typeface = MEDIUM, color = light, kern = 0f, maxLines = 1)
        drawFitted(c, strings.footer2, RectF(textX, bar.top + 73f, textR, bar.top + 97f),
            maxSize = 19f, minSize = 12f, typeface = MEDIUM, color = light, kern = 0f, maxLines = 1)

        // Learn-more line in the margin under the bar
        drawFitted(c, "${strings.learnMore} ${GiveawayCardStrings.WEBSITE}",
            RectF(margin, bar.bottom + 8f, bar.right, bar.bottom + 38f),
            maxSize = 21f, minSize = 13f, typeface = BOLD,
            color = Color.argb(191, Color.red(INK), Color.green(INK), Color.blue(INK)),
            kern = 0f, maxLines = 1, centerHorizontally = true)

        return bmp
    }

    // ── Text ────────────────────────────────────────────────────────────

    private fun textPaint(size: Float, typeface: Typeface, color: Int, kern: Float) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = size
            this.color = color
            letterSpacing = kern / size
        }

    private fun layout(text: String, paint: TextPaint, width: Int, maxLines: Int = Int.MAX_VALUE,
                       ellipsize: Boolean = false, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(align)
            .setIncludePad(false)
            .setMaxLines(maxLines)
            .apply { if (ellipsize) setEllipsize(TextUtils.TruncateAt.END) }
            .build()

    private fun fits(l: StaticLayout, maxLines: Int, rect: RectF): Boolean {
        if (l.lineCount > maxLines || l.height > rect.height() + 0.5f) return false
        // A single word wider than the rect is broken mid-word by the layout;
        // treat that as not fitting so the size shrinks instead.
        for (i in 0 until l.lineCount) if (l.getLineWidth(i) > rect.width() + 0.5f) return false
        return true
    }

    /**
     * Draws [text] in [rect] at the largest size between [maxSize] and
     * [minSize] that fits in [maxLines] lines and the rect's height. If even
     * [minSize] does not fit, the text is cut with an ellipsis rather than
     * overflowing. Returns the height used.
     */
    private fun drawFitted(
        c: Canvas, text: String, rect: RectF,
        maxSize: Float, minSize: Float, typeface: Typeface, color: Int, kern: Float, maxLines: Int,
        centerVertically: Boolean = false, centerHorizontally: Boolean = false,
    ): Float {
        val align = if (centerHorizontally) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL
        val width = rect.width().toInt()
        var size = maxSize
        var paint = textPaint(size, typeface, color, kern)
        var l = layout(text, paint, width, align = align)
        while (!fits(l, maxLines, rect) && size > minSize) {
            size -= 1f
            paint = textPaint(size, typeface, color, kern)
            l = layout(text, paint, width, align = align)
        }
        if (!fits(l, maxLines, rect)) {
            val lineHeight = layout("Ag", paint, width).height.toFloat()
            val h = min(rect.height(), lineHeight * maxLines)
            val cut = layout(text, paint, width, maxLines = maxLines, ellipsize = true, align = align)
            c.save(); c.translate(rect.left, rect.top); c.clipRect(0f, 0f, rect.width(), h); cut.draw(c); c.restore()
            return min(h, cut.height.toFloat())
        }
        val y = if (centerVertically) rect.top + (rect.height() - l.height) / 2f else rect.top
        c.save(); c.translate(rect.left, y); l.draw(c); c.restore()
        return l.height.toFloat()
    }

    private fun measureHeight(text: String, width: Float, size: Float): Float =
        layout(text, textPaint(size, MEDIUM, INK, 0f), width.toInt()).height.toFloat()

    /** One size for all facts, so the list reads evenly: the largest at which every fact fits in two lines. */
    private fun factFontSize(facts: List<String>, width: Float): Float {
        var size = 30f
        while (size > 20f) {
            val ok = facts.all { text ->
                layout(text, textPaint(size, MEDIUM, INK, 0f), width.toInt()).lineCount <= 2
            }
            if (ok) break
            size -= 1f
        }
        return size
    }

    private fun factsHeight(facts: List<String>, width: Float, size: Float): Float =
        facts.sumOf { (measureHeight(it, width, size) + 14f).toDouble() }.toFloat()

    // ── Shapes and images ───────────────────────────────────────────────

    private fun drawStamp(c: Canvas, text: String, cx: Float, cy: Float) {
        val rect = RectF(-100f, -42f, 100f, 42f)
        c.save()
        c.translate(cx, cy)
        c.rotate(6f)
        c.drawRoundRect(rect, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = RED })
        c.drawRoundRect(rect, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 6f; color = INK
        })
        val inner = RectF(rect.left + 10f, rect.top + 6f, rect.right - 10f, rect.bottom - 6f)
        drawFitted(c, text, inner, maxSize = 64f, minSize = 28f, typeface = HEAVY, color = Color.WHITE,
            kern = 2f, maxLines = 1, centerVertically = true)
        c.restore()
    }

    /** The swap card's tag: a small label ("LOOKING FOR") over what the swapper wants. */
    private fun drawWantedTag(c: Canvas, text: String, label: String, cx: Float, cy: Float) {
        val rect = RectF(-165f, -59f, 165f, 59f)
        c.save()
        c.translate(cx, cy)
        c.rotate(-4f)
        c.drawRoundRect(rect, 18f, 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK })
        c.drawRoundRect(rect, 18f, 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 6f; color = Color.WHITE
        })
        drawFitted(c, "\u21C4  " + label, RectF(rect.left + 14f, rect.top + 10f, rect.right - 14f, rect.top + 38f),
            maxSize = 20f, minSize = 12f, typeface = HEAVY, color = Color.argb(191, 255, 255, 255), kern = 2f,
            maxLines = 1, centerHorizontally = true)
        drawFitted(c, text, RectF(rect.left + 14f, rect.top + 42f, rect.right - 14f, rect.bottom - 8f),
            maxSize = 50f, minSize = 24f, typeface = HEAVY, color = Color.WHITE, kern = 0f, maxLines = 1,
            centerVertically = true, centerHorizontally = true)
        c.restore()
    }

    /** The sale card's price tag: dark with a white edge, wide enough for a price such as "Rp 15.000.000". */
    private fun drawPriceTag(c: Canvas, text: String, cx: Float, cy: Float) {
        val rect = RectF(-150f, -48f, 150f, 48f)
        c.save()
        c.translate(cx, cy)
        c.rotate(-4f)
        c.drawRoundRect(rect, 18f, 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK })
        c.drawRoundRect(rect, 18f, 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 6f; color = Color.WHITE
        })
        val inner = RectF(rect.left + 14f, rect.top + 8f, rect.right - 14f, rect.bottom - 8f)
        drawFitted(c, text, inner, maxSize = 56f, minSize = 26f, typeface = HEAVY, color = Color.WHITE,
            kern = 0f, maxLines = 2, centerVertically = true, centerHorizontally = true)
        c.restore()
    }

    /** A battery outline with a nub and one low bar, drawn with paths so it stays crisp and needs no asset. */
    private fun drawBatteryLogo(c: Canvas, r: RectF) {
        val body = RectF(r.left, r.top, r.right - 6f, r.bottom)
        val outline = RectF(body.left + 1.5f, body.top + 1.5f, body.right - 1.5f, body.bottom - 1.5f)
        c.drawRoundRect(outline, 7f, 7f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.WHITE
        })
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        c.drawRoundRect(RectF(body.right + 1f, r.centerY() - 6f, body.right + 6f, r.centerY() + 6f), 2f, 2f, white)
        c.drawRoundRect(RectF(body.left + 6f, body.top + 6f, body.left + 17f, body.bottom - 6f), 3f, 3f, white)
    }

    private fun drawPlaceholder(c: Canvas, rect: RectF) {
        c.drawRect(rect, Paint().apply { color = Color.rgb(237, 237, 237) })
    }

    private fun drawAspectFill(c: Canvas, photo: Bitmap, dst: RectF) {
        val s = max(dst.width() / photo.width, dst.height() / photo.height)
        val w = photo.width * s
        val h = photo.height * s
        val target = RectF(dst.centerX() - w / 2f, dst.centerY() - h / 2f, dst.centerX() + w / 2f, dst.centerY() + h / 2f)
        c.drawBitmap(photo, null, target, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    /**
     * Decodes at most [maxPixel] on the long side straight from disk, upright
     * (the camera stores its rotation in the file's EXIF data), so a 48 MP
     * original never has to be held in memory at full size.
     */
    fun loadPhoto(file: File, maxPixel: Int): Bitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPixel) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
        val orientation = try {
            ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (_: Exception) { ExifInterface.ORIENTATION_NORMAL }
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
            else -> return raw
        }
        val turned = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
        if (turned !== raw) raw.recycle()
        return turned
    }

    // ── Logo ────────────────────────────────────────────────────────────

    /** The LBC logo, trimmed to its visible letters (the picture has a wide transparent border). */
    private fun logoBitmap(context: Context): Bitmap? {
        trimmedLogo?.let { return it }
        synchronized(this) {
            trimmedLogo?.let { return it }
            val src = BitmapFactory.decodeResource(context.resources, R.drawable.giveaway_lbc_logo) ?: return null
            val trimmed = trimToVisible(src)
            trimmedLogo = trimmed
            return trimmed
        }
    }

    private fun trimToVisible(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val row = IntArray(w)
        var minX = w; var maxX = -1; var minY = h; var maxY = -1
        for (y in 0 until h) {
            src.getPixels(row, 0, w, 0, y, w, 1)
            for (x in 0 until w) {
                if ((row[x] ushr 24) > 12) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < minX || maxY < minY) return src
        return Bitmap.createBitmap(src, minX, minY, maxX - minX + 1, maxY - minY + 1)
    }
}
