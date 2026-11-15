package com.universalremote.tv.controller

import com.universalremote.tv.data.CredentialStore
import com.universalremote.tv.model.DiscoveredTV
import com.universalremote.tv.model.TVApp
import com.universalremote.tv.model.TVKey
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.WebSocket
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class LGWebOSController(tv: DiscoveredTV, credentialStore: CredentialStore) : BaseTVController(tv, credentialStore) {
    private var socket: WebSocket? = null
    private var pointerSocket: WebSocket? = null
    private var registration = CompletableDeferred<Unit>()
    private val sequence = AtomicInteger(1)
    private val pending = ConcurrentHashMap<String, CompletableDeferred<JSONObject>>()
    private val manifestPermissions = JSONArray(listOf(
        "LAUNCH", "LAUNCH_WEBAPP", "APP_TO_APP", "CLOSE", "TEST_OPEN", "TEST_PROTECTED", "CONTROL_AUDIO",
        "CONTROL_DISPLAY", "CONTROL_INPUT_JOYSTICK", "CONTROL_INPUT_MEDIA_PLAYBACK", "CONTROL_INPUT_TV",
        "CONTROL_POWER", "READ_APP_STATUS", "READ_CURRENT_CHANNEL", "READ_INPUT_DEVICE_LIST", "READ_NETWORK_STATE",
        "READ_RUNNING_APPS", "READ_TV_CHANNEL_LIST", "READ_TV_CURRENT_CHANNEL", "READ_TV_PROGRAM_INFO",
        "READ_INSTALLED_APPS", "READ_SETTINGS", "WRITE_SETTINGS", "WRITE_NOTIFICATION_TOAST", "CONTROL_TV_SCREEN",
        "CONTROL_TV_STANDBY", "CONTROL_TV_POWER"
    ))

    override suspend fun connect(): Result<Unit> = runCatching {
        registration = CompletableDeferred()
        val uri = "ws://${tv.ip}:3000/"
        socket = openWebSocket(uri) { text -> handleMessage(text) }
        val payload = JSONObject().put("type", "register").put("id", "register_0")
            .put("payload", JSONObject().put("manifest", JSONObject()
                .put("manifestVersion", 1).put("appVersion", "1.0")
                .put("signed", JSONObject().put("created", "20240101").put("appId", "com.universalremote.tv").put("vendorId", "com.universalremote"))
                .put("permissions", manifestPermissions)))
        credentialStore.getCredential(tv.ip, "clientKey")?.let { payload.getJSONObject("payload").put("client-key", it) }
        socket?.send(payload.toString())
        connected = true
    }.recoverCatching { throw IllegalStateException("Could not open the LG webOS control socket: ${safeMessage(it)}") }

    private fun handleMessage(text: String) {
        runCatching {
            val json = JSONObject(text)
            if (json.optString("type") == "registered") {
                val key = json.optJSONObject("payload")?.optString("client-key").orEmpty()
                if (key.isNotBlank()) credentialStore.saveCredential(tv.ip, "clientKey", key)
                registration.complete(Unit)
            }
            val id = json.optString("id")
            if (id.isNotBlank()) pending.remove(id)?.complete(json)
        }
    }

    override suspend fun pair(pin: String?): Result<Unit> = runCatching {
        if (socket == null) connect().getOrThrow()
        withTimeout(25_000) { registration.await() }
    }.recoverCatching { throw IllegalStateException("Accept the connection request on the LG TV and try again: ${safeMessage(it)}") }

    private suspend fun request(uri: String, payload: JSONObject = JSONObject()): JSONObject {
        val id = "req_${sequence.getAndIncrement()}"
        val deferred = CompletableDeferred<JSONObject>()
        pending[id] = deferred
        val sent = socket?.send(JSONObject().put("id", id).put("type", "request").put("uri", uri).put("payload", payload).toString()) ?: false
        if (!sent) { pending.remove(id); throw IllegalStateException("LG control socket is closed") }
        return withTimeout(8_000) { deferred.await() }
    }

    private suspend fun ensurePointer() {
        if (pointerSocket != null) return
        val response = request("ssap://com.webos.service.networkinput/getPointerInputSocket")
        val path = response.optJSONObject("payload")?.optString("socketPath")
            ?: response.optJSONObject("payload")?.optString("socket-path")
            ?: throw IllegalStateException("The TV did not provide a pointer input socket")
        pointerSocket = openWebSocket(path) { }
    }

    override suspend fun sendKey(key: TVKey): Result<Unit> = runCatching {
        if (!connected) connect().getOrThrow()
        when (key) {
            TVKey.VOLUME_UP -> request("ssap://audio/volumeUp")
            TVKey.VOLUME_DOWN -> request("ssap://audio/volumeDown")
            TVKey.MUTE -> request("ssap://audio/setMute", JSONObject().put("mute", true))
            TVKey.POWER -> request("ssap://system/turnOff")
            TVKey.CHANNEL_UP -> request("ssap://tv/channelUp")
            TVKey.CHANNEL_DOWN -> request("ssap://tv/channelDown")
            else -> {
                ensurePointer()
                val name = when (key) {
                    TVKey.UP -> "UP"; TVKey.DOWN -> "DOWN"; TVKey.LEFT -> "LEFT"; TVKey.RIGHT -> "RIGHT"
                    TVKey.OK -> "ENTER"; TVKey.BACK -> "BACK"; TVKey.HOME -> "HOME"; TVKey.MENU, TVKey.SETTINGS -> "MENU"
                    TVKey.PLAY -> "PLAY"; TVKey.PAUSE -> "PAUSE"; TVKey.PLAY_PAUSE -> "PLAYPAUSE"; TVKey.STOP -> "STOP"
                    TVKey.REWIND -> "REWIND"; TVKey.FAST_FORWARD -> "FASTFORWARD"; TVKey.RED -> "RED"; TVKey.GREEN -> "GREEN"
                    TVKey.YELLOW -> "YELLOW"; TVKey.BLUE -> "BLUE"; TVKey.SOURCE -> "INPUT"; TVKey.INFO -> "INFO"
                    TVKey.DIGIT_0 -> "0"; TVKey.DIGIT_1 -> "1"; TVKey.DIGIT_2 -> "2"; TVKey.DIGIT_3 -> "3"; TVKey.DIGIT_4 -> "4"
                    TVKey.DIGIT_5 -> "5"; TVKey.DIGIT_6 -> "6"; TVKey.DIGIT_7 -> "7"; TVKey.DIGIT_8 -> "8"; TVKey.DIGIT_9 -> "9"
                    else -> throw IllegalArgumentException("This key is not supported by LG pointer input")
                }
                val line = if (key == TVKey.OK) "type:click\n\n" else "type:button\nname:$name\n\n"
                pointerSocket?.send(line) ?: throw IllegalStateException("LG pointer socket is unavailable")
            }
        }
    }

    override suspend fun launchApp(appId: String): Result<Unit> = runCatching {
        request("ssap://system.launcher/launch", JSONObject().put("id", appId))
    }

    override suspend fun getAppList(): Result<List<TVApp>> = runCatching {
        val payload = request("ssap://com.webos.service.applicationManager/listLaunchPoints").optJSONArray("payload") ?: JSONArray()
        (0 until payload.length()).mapNotNull { i -> payload.optJSONObject(i)?.let { TVApp(it.optString("id"), it.optString("title", it.optString("name"))) } }
    }

    override suspend fun powerOff(): Result<Unit> = sendKey(TVKey.POWER)
    override suspend fun volumeUp(): Result<Unit> = sendKey(TVKey.VOLUME_UP)
    override suspend fun volumeDown(): Result<Unit> = sendKey(TVKey.VOLUME_DOWN)
    override suspend fun setMute(mute: Boolean): Result<Unit> = runCatching { request("ssap://audio/setMute", JSONObject().put("mute", mute)) }
    override suspend fun channelUp(): Result<Unit> = sendKey(TVKey.CHANNEL_UP)
    override suspend fun channelDown(): Result<Unit> = sendKey(TVKey.CHANNEL_DOWN)
    override suspend fun disconnect() { pointerSocket?.close(1000, "disconnect"); socket?.close(1000, "disconnect"); pointerSocket = null; socket = null; connected = false }
    override fun isConnected(): Boolean = connected
}
