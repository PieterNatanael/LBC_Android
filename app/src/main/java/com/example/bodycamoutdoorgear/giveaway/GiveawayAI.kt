package com.example.bodycamoutdoorgear.giveaway

import com.example.bodycamoutdoorgear.i18n.tr
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max

/** Why "Fill with AI" didn't produce text. None of these blocks the card. */
enum class GiveawayAIError(private val english: String) {
    OFFLINE("No internet. You can still write your own text."),
    MONTHLY_LIMIT("You've used this month's AI cards. Back next month. You can still write your own text."),
    UNAVAILABLE("AI isn't available right now. You can still write your own text.");

    /** In the app's language at the moment it is shown. */
    val message: String get() = tr(english)
}

/** What "Fill with AI" came back with. */
sealed class GiveawayAIOutcome {
    /** New card text; [privacyNote] is a tip (a face or document may be visible); [remaining] is the server's count left this month. */
    data class Filled(val content: GiveawayCardContent, val privacyNote: String?, val remaining: Int?) : GiveawayAIOutcome()

    /** The item can't be given away here. Nothing on the card changes. */
    data class Refused(val message: String, val remaining: Int?) : GiveawayAIOutcome()
}

/**
 * How many AI fills the server says are left this month. The app has no limit of
 * its own: the server enforces it (and can change it without an app update), so
 * the count is only what the server last reported. It is unknown until the first
 * fill of the month, and starts over when the month or the Pro status changes.
 */
object GiveawayAIUsage {
    private fun prefs(context: Context) = context.getSharedPreferences("giveaway", Context.MODE_PRIVATE)

    private fun thisMonth(): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    /** Fills left according to the server's last reply, or null when this month's count isn't known yet. */
    fun remaining(context: Context, premium: Boolean): Int? {
        val p = prefs(context)
        val known = p.getString("server_month", null) == thisMonth() && p.getBoolean("server_premium", false) == premium
        return if (known) p.getInt("server_remaining", 0) else null
    }

    fun setRemaining(context: Context, remaining: Int, premium: Boolean) {
        prefs(context).edit()
            .putString("server_month", thisMonth())
            .putBoolean("server_premium", premium)
            .putInt("server_remaining", max(0, remaining))
            .apply()
    }

    /** The server didn't say how many are left: count this fill down from what was known. */
    fun recordUse(context: Context, premium: Boolean) {
        remaining(context, premium)?.let { setRemaining(context, it - 1, premium) }
    }
}

/** The one-time "before you give something away" agreement. Raise [CURRENT_VERSION] when the wording changes. */
object GiveawayAgreement {
    const val CURRENT_VERSION = 1

    private fun prefs(context: Context) = context.getSharedPreferences("giveaway", Context.MODE_PRIVATE)

    fun needed(context: Context): Boolean = prefs(context).getInt("agreement_version", 0) < CURRENT_VERSION
    fun accept(context: Context) = prefs(context).edit().putInt("agreement_version", CURRENT_VERSION).apply()

    /** The user has seen the "sends a small copy to an AI service" notice for Fill with AI. */
    fun aiConsented(context: Context): Boolean = prefs(context).getBoolean("ai_consent", false)
    fun acceptAI(context: Context) = prefs(context).edit().putBoolean("ai_consent", true).apply()
}

/**
 * Asks our server (and through it Claude) for the card text. The app never talks
 * to Anthropic directly and never holds a key. One resized photo is sent; its
 * metadata (location, camera, time) is dropped by re-encoding.
 */
object GiveawayAIService {
    const val ENDPOINT = "https://gogoodmood.com/api/giveaway"

    /** A random id for this installation, not tied to the user, so the server can count fills per month. */
    private fun installId(context: Context): String {
        val p = context.getSharedPreferences("giveaway", Context.MODE_PRIVATE)
        p.getString("install_id", null)?.let { return it }
        return UUID.randomUUID().toString().also { p.edit().putString("install_id", it).apply() }
    }

    /** [pictures] are small JPEGs: one for a photo, three frames for a video. */
    suspend fun fill(context: Context, pictures: List<ByteArray>, premium: Boolean, mode: CardMode = CardMode.GIVEAWAY): Result<GiveawayAIOutcome> =
        withContext(Dispatchers.IO) {
            if (pictures.isEmpty()) return@withContext Result.failure(AIException(GiveawayAIError.UNAVAILABLE))
            val body = JSONObject()
                .put("install_id", installId(context))
                .put("premium", premium)
                .put("language", com.example.bodycamoutdoorgear.i18n.Tr.code)
                .put("mode", if (mode == CardMode.SALE) "sale" else "giveaway")
                .put("images", JSONArray().apply {
                    pictures.forEach {
                        put(JSONObject().put("media_type", "image/jpeg").put("data", Base64.encodeToString(it, Base64.NO_WRAP)))
                    }
                })
                .toString().toByteArray()

            var connection: HttpURLConnection? = null
            try {
                connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 25_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                connection.outputStream.use { it.write(body) }
                val code = connection.responseCode
                val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                val reply = try { JSONObject(text) } catch (_: Exception) { JSONObject() }

                if (code == 429) {
                    // Only the monthly quota is the user's limit; "slow down" is just "not available right now".
                    return@withContext if (reply.optString("error") == "monthly_limit") {
                        GiveawayAIUsage.setRemaining(context, 0, premium)
                        Result.failure(AIException(GiveawayAIError.MONTHLY_LIMIT))
                    } else Result.failure(AIException(GiveawayAIError.UNAVAILABLE))
                }
                if (code != 200) return@withContext Result.failure(AIException(GiveawayAIError.UNAVAILABLE))

                val remaining = if (reply.has("remaining") && !reply.isNull("remaining")) reply.optInt("remaining") else null
                when (reply.optString("status")) {
                    "ok" -> {
                        val name = reply.optString("item_name")
                        val comment = reply.optString("comment")
                        if (name.isEmpty() || comment.isEmpty()) {
                            Result.failure(AIException(GiveawayAIError.UNAVAILABLE))
                        } else {
                            val facts = reply.optJSONArray("facts")?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
                            val content = GiveawayCardContent(name, facts, comment, note = reply.text("note"))
                            val privacy = reply.text("privacy_note").ifEmpty { null }
                            Result.success(GiveawayAIOutcome.Filled(content, privacy, remaining))
                        }
                    }
                    "no_card" -> Result.success(
                        GiveawayAIOutcome.Refused(
                            reply.text("message").ifEmpty { tr("Sorry, this can't be given away here.") }, remaining))
                    else -> Result.failure(AIException(GiveawayAIError.UNAVAILABLE))
                }
            } catch (_: UnknownHostException) {
                Result.failure(AIException(GiveawayAIError.OFFLINE))
            } catch (_: IOException) {
                Result.failure(AIException(GiveawayAIError.UNAVAILABLE))
            } catch (_: Exception) {
                Result.failure(AIException(GiveawayAIError.UNAVAILABLE))
            } finally {
                connection?.disconnect()
            }
        }

    /** A string field, or "" when it is missing or JSON null (optString would give "null"). */
    private fun JSONObject.text(key: String): String = if (isNull(key)) "" else optString(key)

    class AIException(val error: GiveawayAIError) : Exception(error.message)
}

/** Shrinks the picture before it leaves the phone: about 1000 px on the long side, re-encoded as JPEG, which drops location and camera metadata. */
object GiveawayImagePrep {
    private const val MAX_SIDE = 1000
    private const val QUALITY = 70

    fun jpeg(file: File): ByteArray? = GiveawayCardRenderer.loadPhoto(file, MAX_SIDE)?.let { encode(it) }

    /** Three frames, early, middle and late, so the AI sees the item from the whole clip. */
    fun videoFrames(file: File): List<ByteArray> {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(file.absolutePath)
            val durationUs = (r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: return emptyList()) * 1000
            listOf(0.15, 0.5, 0.85).mapNotNull { f ->
                val frame = r.getFrameAtTime((durationUs * f).toLong(), MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return@mapNotNull null
                encode(frame)
            }
        } catch (_: Exception) {
            emptyList()
        } finally {
            r.release()
        }
    }

    private fun encode(bitmap: Bitmap): ByteArray {
        val longSide = max(bitmap.width, bitmap.height)
        val scaled = if (longSide > MAX_SIDE) {
            val f = MAX_SIDE.toFloat() / longSide
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * f).toInt().coerceAtLeast(1), (bitmap.height * f).toInt().coerceAtLeast(1), true)
        } else bitmap
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()
        return out.toByteArray()
    }
}
