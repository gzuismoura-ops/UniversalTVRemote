package com.universalremote.tv.controller

import android.util.Base64
import com.universalremote.tv.data.CredentialStore
import com.universalremote.tv.model.DiscoveredTV
import com.universalremote.tv.model.TVApp
import com.universalremote.tv.model.TVKey
import okhttp3.WebSocket
import org.json.JSONArray
import org.json.JSONObject

class SamsungTizenController(tv: DiscoveredTV, credentialStore: CredentialStore) : BaseTVController(tv, credentialStore) {
    private var socket: WebSocket? = null

    override suspend fun connect(): Result<Unit> = runCatching {
        val name = Base64.encodeToString("Universal TV Remote".toByteArray(), Base64.NO_WRAP)
        val token = credentialStore.getCredential(tv.ip, "token")
        val tokenQuery = token?.let { "&token=$it" }.orEmpty()
        val secureUrl = "wss://${tv.ip}:8002/api/v2/channels/samsung.remote.control?name=$name$tokenQuery"
        socket = try { openWebSocket(secureUrl, insecure = true) { parseToken(it) } }
        catch (_: Exception) { openWebSocket("ws://${tv.ip}:8001/api/v2/channels/samsung.remote.control?name=$name") { parseToken(it) } }
        connected = true
    }.recoverCatching { throw IllegalStateException("Samsung connection failed. Check that remote control is enabled on the TV: ${safeMessage(it)}") }

    private fun parseToken(text: String) {
        runCatching {
            val token = JSONObject(text).optJSONObject("data")?.optString("token").orEmpty()
            if (token.isNotBlank()) credentialStore.saveCredential(tv.ip, "token", token)
        }
    }

    override suspend fun pair(pin: String?): Result<Unit> = if (connected) Result.success(Unit) else connect()

    override suspend fun sendKey(key: TVKey): Result<Unit> = runCatching {
        if (!connected) connect().getOrThrow()
        val keyName = when (key) {
            TVKey.UP -> "KEY_UP"; TVKey.DOWN -> "KEY_DOWN"; TVKey.LEFT -> "KEY_LEFT"; TVKey.RIGHT -> "KEY_RIGHT"
            TVKey.OK -> "KEY_ENTER"; TVKey.BACK -> "KEY_RETURN"; TVKey.HOME -> "KEY_HOME"; TVKey.MENU, TVKey.SETTINGS -> "KEY_MENU"
            TVKey.VOLUME_UP -> "KEY_VOLUP"; TVKey.VOLUME_DOWN -> "KEY_VOLDOWN"; TVKey.MUTE -> "KEY_MUTE"; TVKey.POWER -> "KEY_POWER"
            TVKey.CHANNEL_UP -> "KEY_CHUP"; TVKey.CHANNEL_DOWN -> "KEY_CHDOWN"; TVKey.PLAY -> "KEY_PLAY"; TVKey.PAUSE, TVKey.PLAY_PAUSE -> "KEY_PAUSE"
            TVKey.STOP -> "KEY_STOP"; TVKey.REWIND -> "KEY_REWIND"; TVKey.FAST_FORWARD -> "KEY_FF"; TVKey.SOURCE -> "KEY_SOURCE"
            TVKey.INFO -> "KEY_INFO"; TVKey.RED -> "KEY_RED"; TVKey.GREEN -> "KEY_GREEN"; TVKey.YELLOW -> "KEY_YELLOW"; TVKey.BLUE -> "KEY_BLUE"
            TVKey.EXIT -> "KEY_EXIT"; TVKey.DIGIT_0 -> "KEY_0"; TVKey.DIGIT_1 -> "KEY_1"; TVKey.DIGIT_2 -> "KEY_2"; TVKey.DIGIT_3 -> "KEY_3"
            TVKey.DIGIT_4 -> "KEY_4"; TVKey.DIGIT_5 -> "KEY_5"; TVKey.DIGIT_6 -> "KEY_6"; TVKey.DIGIT_7 -> "KEY_7"; TVKey.DIGIT_8 -> "KEY_8"; TVKey.DIGIT_9 -> "KEY_9"
            else -> throw IllegalArgumentException("Unsupported Samsung key")
        }
        val command = JSONObject().put("method", "ms.remote.control").put("params", JSONObject()
            .put("Cmd", "Click").put("DataOfCmd", keyName).put("Option", "false").put("TypeOfRemote", "SendRemoteKey"))
        if (socket?.send(command.toString()) != true) throw IllegalStateException("Samsung remote socket is closed")
    }

    override suspend fun launchApp(appId: String): Result<Unit> = runCatching {
        if (!connected) connect().getOrThrow()
        val json = JSONObject().put("method", "ms.channel.emit").put("params", JSONObject()
            .put("event", "ed.apps.launch").put("to", "host")
            .put("data", JSONObject().put("appId", appId).put("action_type", "DEEP_LINK")))
        if (socket?.send(json.toString()) != true) throw IllegalStateException("Samsung remote socket is closed")
    }

    override suspend fun getAppList(): Result<List<TVApp>> = runCatching {
        val response = http("GET", "http://${tv.ip}:8001/api/v2/applications")
        if (!response.successful) throw IllegalStateException("TV did not return its app list (${response.code})")
        val apps = JSONArray(response.body)
        (0 until apps.length()).mapNotNull { i -> apps.optJSONObject(i)?.let { TVApp(it.optString("appId"), it.optString("name", it.optString("appId"))) } }
    }

    override suspend fun powerOff(): Result<Unit> = sendKey(TVKey.POWER)
    override suspend fun volumeUp(): Result<Unit> = sendKey(TVKey.VOLUME_UP)
    override suspend fun volumeDown(): Result<Unit> = sendKey(TVKey.VOLUME_DOWN)
    override suspend fun setMute(mute: Boolean): Result<Unit> = sendKey(TVKey.MUTE)
    override suspend fun channelUp(): Result<Unit> = sendKey(TVKey.CHANNEL_UP)
    override suspend fun channelDown(): Result<Unit> = sendKey(TVKey.CHANNEL_DOWN)
    override suspend fun disconnect() { socket?.close(1000, "disconnect"); socket = null; connected = false }
    override fun isConnected(): Boolean = connected
}
