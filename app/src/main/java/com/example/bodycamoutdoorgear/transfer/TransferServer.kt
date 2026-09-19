package com.example.bodycamoutdoorgear.transfer

import android.content.Context
import android.net.wifi.WifiManager
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

const val TRANSFER_PORT = 8765

/** Sent along with a file to announce it as a shutter sound instead of gallery media. */
data class SoundMeta(val title: String, val emoji: String)

object TransferServer {

    private var server: io.ktor.server.engine.ApplicationEngine? = null
    private var filesToServe: List<File> = emptyList()
    private var soundMeta: Map<String, SoundMeta> = emptyMap()

    /** [soundMeta] is keyed by file name. */
    fun start(files: List<File>, soundMeta: Map<String, SoundMeta> = emptyMap()) {
        filesToServe = files
        this.soundMeta = soundMeta
        if (server != null) return
        server = embeddedServer(Netty, port = TRANSFER_PORT) {
            routing {
                get("/list") {
                    val list = JSONArray()
                    filesToServe.forEach { f ->
                        val entry = JSONObject().put("name", f.name).put("size", f.length())
                        soundMeta[f.name]?.let {
                            entry.put("kind", "sound").put("title", it.title).put("emoji", it.emoji)
                        }
                        list.put(entry)
                    }
                    call.respondText(list.toString(), ContentType.Application.Json)
                }
                get("/file/{name}") {
                    val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val file = filesToServe.find { it.name == name }
                        ?: return@get call.respond(HttpStatusCode.NotFound)
                    call.respondFile(file)
                }
            }
        }
        CoroutineScope(Dispatchers.IO).launch { server!!.start(wait = false) }
    }

    fun stop() {
        server?.stop(0, 0)
        server = null
        filesToServe = emptyList()
        soundMeta = emptyMap()
    }

    fun getLocalIp(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()
                ?.flatMap { it.inetAddresses.toList() }
                ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                ?.hostAddress
        } catch (_: Exception) { null }
    }

    fun buildWifiQrPayload(ssid: String, password: String): String =
        "WIFI:T:WPA;S:$ssid;P:$password;;"

    fun buildTransferUrl(ip: String) = "lbc://transfer?host=$ip&port=$TRANSFER_PORT"
}
