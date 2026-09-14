package com.example.bodycamoutdoorgear.transfer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object TransferClient {

    var lastError: String = ""

    suspend fun listFiles(host: String, port: Int): List<String> = withContext(Dispatchers.IO) {
        try {
            val conn = URL("http://$host:$port/list").openConnection()
            conn.connectTimeout = 5_000
            conn.readTimeout = 10_000
            val text = conn.getInputStream().bufferedReader().readText()
            val arr = JSONArray(text)
            (0 until arr.length()).map { arr.getJSONObject(it).getString("name") }
        } catch (e: Exception) {
            lastError = e.message ?: e.javaClass.simpleName
            emptyList()
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
