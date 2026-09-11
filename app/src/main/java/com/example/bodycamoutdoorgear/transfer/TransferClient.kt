package com.example.bodycamoutdoorgear.transfer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.URL

object TransferClient {

    var lastError: String = ""

    suspend fun listFiles(host: String, port: Int): List<String> = withContext(Dispatchers.IO) {
        try {
            val conn = URL("http://$host:$port/list").openConnection()
            conn.connectTimeout = 5000
            conn.readTimeout = 10000
            val text = conn.getInputStream().bufferedReader().readText()
            val arr = JSONArray(text)
            (0 until arr.length()).map { arr.getJSONObject(it).getString("name") }
        } catch (e: Exception) {
            lastError = e.message ?: e.javaClass.simpleName
            emptyList()
        }
    }

    suspend fun downloadFile(
        context: Context,
        host: String,
        port: Int,
        name: String,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL("http://$host:$port/file/$name").openConnection()
            conn.connectTimeout = 5000
            conn.readTimeout = 60000
            val bytes = conn.getInputStream().readBytes()
            val dest = File(context.filesDir, name)
            dest.writeBytes(bytes)
            onProgress(1f)
            true
        } catch (e: Exception) {
            lastError = e.message ?: e.javaClass.simpleName
            false
        }
    }
}
