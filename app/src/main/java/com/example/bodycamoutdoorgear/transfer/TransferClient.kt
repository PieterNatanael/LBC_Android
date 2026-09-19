package com.example.bodycamoutdoorgear.transfer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class RemoteFile(
    val name: String,
    val size: Long,
    val kind: String? = null,
    val title: String? = null,
    val emoji: String? = null
) {
    val isSound: Boolean get() = kind == "sound"
}

object TransferClient {

    var lastError: String = ""

    suspend fun listFiles(host: String, port: Int): List<RemoteFile> = withContext(Dispatchers.IO) {
        try {
            val conn = URL("http://$host:$port/list").openConnection()
            conn.connectTimeout = 5_000
            conn.readTimeout = 10_000
            val text = conn.getInputStream().bufferedReader().readText()
            val arr = JSONArray(text)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                RemoteFile(
                    name = o.getString("name"),
                    size = o.optLong("size", 0L),
                    kind = o.optString("kind").ifEmpty { null },
                    title = o.optString("title").ifEmpty { null },
                    emoji = o.optString("emoji").ifEmpty { null }
                )
            }
        } catch (e: Exception) {
            lastError = e.message ?: e.javaClass.simpleName
            emptyList()
        }
    }

    /** A file name from the network becomes a path here, so it must be a plain name. */
    private fun isPlainFileName(name: String) =
        name.isNotEmpty() && name != "." && name != ".." && !name.contains('/') && !name.contains('\\')

    /** Downloads a shutter sound into the cache, ready for ShutterSoundStore.importReceived. */
    suspend fun downloadSound(
        context: Context,
        host: String,
        port: Int,
        file: RemoteFile,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        if (!isPlainFileName(file.name) || file.size > com.example.bodycamoutdoorgear.sound.ShutterSoundClip.MAX_IMPORT_BYTES) {
            return@withContext null
        }
        val tmp = File(context.cacheDir, "incoming_${System.nanoTime()}.wav")
        try {
            val conn = URL("http://$host:$port/file/${java.net.URLEncoder.encode(file.name, "UTF-8")}")
                .openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout = 15_000
            conn.connect()
            val limit = com.example.bodycamoutdoorgear.sound.ShutterSoundClip.MAX_IMPORT_BYTES
            var downloaded = 0L
            conn.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buf = ByteArray(16 * 1024)
                    var n: Int
                    while (input.read(buf).also { n = it } != -1) {
                        downloaded += n
                        if (downloaded > limit) { tmp.delete(); return@withContext null }
                        output.write(buf, 0, n)
                    }
                }
            }
            onProgress(1f)
            tmp
        } catch (e: Exception) {
            lastError = e.message ?: e.javaClass.simpleName
            tmp.delete()
            null
        }
    }

    // Streams directly to disk in 256 KB chunks — never loads the full file into RAM.
    // Reports real progress based on bytes received vs Content-Length.
    suspend fun downloadFile(
        context: Context,
        host: String,
        port: Int,
        name: String,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isPlainFileName(name)) return@withContext false
        try {
            val conn = URL("http://$host:$port/file/$name").openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout = 3_600_000  // 1 hour — large video files take time
            conn.connect()

            val totalBytes = conn.contentLengthLong.takeIf { it > 0L }
            var downloaded = 0L

            val dest = File(context.filesDir, name)
            val tmp = File(context.filesDir, "_tmp_$name")

            conn.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buf = ByteArray(256 * 1024)
                    var n: Int
                    while (input.read(buf).also { n = it } != -1) {
                        output.write(buf, 0, n)
                        downloaded += n
                        if (totalBytes != null) {
                            onProgress((downloaded.toFloat() / totalBytes).coerceIn(0f, 1f))
                        }
                    }
                }
            }

            if (dest.exists()) dest.delete()
            tmp.renameTo(dest)
            onProgress(1f)
            true
        } catch (e: Exception) {
            lastError = e.message ?: e.javaClass.simpleName
            false
        }
    }
}
