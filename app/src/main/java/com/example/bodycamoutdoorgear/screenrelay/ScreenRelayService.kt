package com.example.bodycamoutdoorgear.screenrelay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Talks to the "Analyze my Mac" relay on gogoodmood.com from the phone
 * side. Same shape as GiveawayAIService: plain HttpURLConnection, no
 * third-party networking library. See worker/screen-relay-endpoint.js on
 * the server, and the Mac app's own ScreenRelayService, for the full flow
 * this mirrors.
 */
object ScreenRelayService {
    private const val BASE = "https://gogoodmood.com/api/screen"

    sealed class ResultStatus {
        object Pending : ResultStatus()
        data class Done(val text: String) : ResultStatus()
        object Expired : ResultStatus()
    }

    class NotPairedException : Exception("not_paired")
    class QuotaExceededException : Exception("quota_exceeded")

    private fun post(path: String, body: JSONObject): JSONObject = openAndRead(
        "$BASE/$path",
        configure = {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        },
        write = { it.write(body.toString().toByteArray()) },
    )

    private fun get(path: String, query: Map<String, String>): JSONObject {
        val qs = query.entries.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
        return openAndRead("$BASE/$path?$qs", configure = { requestMethod = "GET" })
    }

    private fun openAndRead(
        url: String,
        configure: HttpURLConnection.() -> Unit,
        write: ((java.io.OutputStream) -> Unit)? = null,
    ): JSONObject {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                configure()
            }
            write?.let { w -> connection.outputStream.use { w(it) } }
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = try { JSONObject(text) } catch (_: Exception) { JSONObject() }
            if (code == 403) throw NotPairedException()
            if (code == 429) throw QuotaExceededException()
            if (code !in 200..299) throw IOException("HTTP $code")
            return json
        } finally {
            connection?.disconnect()
        }
    }

    /** Completes a pairing after scanning the Mac's QR code. */
    suspend fun confirmPairing(pairId: String, phoneInstallId: String) = withContext(Dispatchers.IO) {
        post("pair/confirm", JSONObject().put("pair_id", pairId).put("phone_install_id", phoneInstallId))
        Unit
    }

    /**
     * Tapping "Analyze my Mac". [prompt] is any custom instruction the user
     * typed; empty means "just explain what's on screen" (the server's own default).
     * [tier] is "free"/"pro"/"plus"/"max" — it picks the Claude model and the
     * quota on the server (see TIERS in worker/screen-relay-endpoint.js).
     */
    suspend fun requestAnalysis(pairId: String, phoneInstallId: String, prompt: String, tier: String): String =
        withContext(Dispatchers.IO) {
            val reply = post(
                "request",
                JSONObject().put("pair_id", pairId).put("phone_install_id", phoneInstallId)
                    .put("prompt", prompt).put("tier", tier),
            )
            reply.optString("job_id").ifEmpty { throw IOException("bad_response") }
        }

    suspend fun pollResult(jobId: String, phoneInstallId: String): ResultStatus = withContext(Dispatchers.IO) {
        val reply = get("result", mapOf("job_id" to jobId, "phone_install_id" to phoneInstallId))
        when (reply.optString("status")) {
            "done" -> ResultStatus.Done(reply.optString("text"))
            "expired" -> ResultStatus.Expired
            else -> ResultStatus.Pending
        }
    }
}
