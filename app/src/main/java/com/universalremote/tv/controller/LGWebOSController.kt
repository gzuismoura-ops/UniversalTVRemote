package com.universalremote.tv.controller

import com.universalremote.tv.data.CredentialStore
import com.universalremote.tv.model.DiscoveredTV
import com.universalremote.tv.model.TVApp
import com.universalremote.tv.model.TVKey
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import okhttp3.WebSocket
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** LG webOS SSAP controller. Registration is not considered successful until the TV replies "registered". */
class LGWebOSController(tv: DiscoveredTV, credentialStore: CredentialStore) : BaseTVController(tv, credentialStore) {
    private var socket: WebSocket? = null
    private var pointerSocket: WebSocket? = null
    private var registration = CompletableDeferred<Unit>()
    private val sequence = AtomicInteger(1)
    private val pending = ConcurrentHashMap<String, CompletableDeferred<JSONObject>>()

    private val manifestPermissions = JSONArray(listOf(
        "LAUNCH", "LAUNCH_WEBAPP", "APP_TO_APP", "CLOSE", "CONTROL_AUDIO", "CONTROL_DISPLAY",
        "CONTROL_INPUT_JOYSTICK", "CONTROL_INPUT_MEDIA_PLAYBACK", "CONTROL_INPUT_TV", "CONTROL_POWER",
        "READ_APP_STATUS", "READ_CURRENT_CHANNEL", "READ_INPUT_DEVICE_LIST", "READ_NETWORK_STATE",
        "READ_RUNNING_APPS", "READ_TV_CHANNEL_LIST", "READ_TV_CURRENT_CHANNEL", "READ_TV_PROGRAM_INFO",
        "READ_INSTALLED_APPS", "READ_SETTINGS", "WRITE_SETTINGS", "WRITE_NOTIFICATION_TOAST",
        "CONTROL_TV_SCREEN", "CONTROL_TV_STANDBY", "CONTROL_TV_POWER"
    ))

    override suspend fun connect(): Result<Unit> = runCatching {
        if (connected && registration.isCompleted) return@runCatching
        if (socket == null) {
            registration = CompletableDeferred()
            socket = openWebSocket("ws://${tv.ip}:3000/") { text -> handleMessage(text) }
            val payload = JSONObject()
                .put("type", "register")
                .put("id", "register_0")
                .put("payload", JSONObject().put("manifest", JSONObject()
                    .put("manifestVersion", 1)
                    .put("appVersion", "1.1")
                    .put("signed", JSONObject()
                        .put("created", "20260101")
                        .put("appId", "com.universalremote.tv")
                        .put("vendorId", "com.universalremote"))
                    .put("permissions", manifestPermissions)))
            credentialStore.getCredential(tv.ip, "clientKey")?.let {
                payload.getJSONObject("payload").put("client-key", it)
            }
            if (socket?.send(payload.toString()) != true) {
                socket?.close(1000, "registration send failed")
                socket = null
                throw IllegalStateException("Não foi possível enviar o pedido de autorização para a TV LG.")
            }
        }
        // A TV normalmente exibe uma confirmação. Só declaramos Connected depois do aceite.
        withTimeout(45_000) { registration.await() }
        connected = true
    }.recoverCatching {
        connected = false
        throw IllegalStateException(
            when {
                it.message?.contains("Timeout", true) == true -> "A TV não confirmou a autorização. Verifique se apareceu uma janela na TV e selecione Permitir/Accept. Depois tente novamente."
                else -> "Falha no pareamento LG webOS: ${safeMessage(it)}"
            }
        )
    }

    private fun handleMessage(text: String) {
        runCatching {
            val json = JSONObject(text)
            when (json.optString("type")) {
                "registered" -> {
                    val key = json.optJSONObject("payload")?.optString("client-key").orEmpty()
                    if (key.isNotBlank()) credentialStore.saveCredential(tv.ip, "clientKey", key)
                    registration.complete(Unit)
                }
                "error" -> {
                    val message = json.optString("error", json.optJSONObject("payload")?.optString("error") ?: "A TV recusou o registro")
                    registration.completeExceptionally(IllegalStateException(message))
                }
            }
            val id = json.optString("id")
            if (id.isNotBlank()) {
                val waiter = pending.remove(id)
                if (json.optString("type") == "error" || json.optJSONObject("payload")?.optBoolean("returnValue", true) == false) {
                    waiter?.completeExceptionally(IllegalStateException(json.optString("error", "A TV recusou o comando")))
                } else waiter?.complete(json)
            }
        }
    }

    override suspend fun pair(pin: String?): Result<Unit> = connect()

    private suspend fun request(uri: String, payload: JSONObject = JSONObject()): JSONObject {
        if (!connected) connect().getOrThrow()
        val id = "req_${sequence.getAndIncrement()}"
        val deferred = CompletableDeferred<JSONObject>()
        pending[id] = deferred
        val sent = socket?.send(JSONObject().put("id", id).put("type", "request")
            .put("uri", uri).put("payload", payload).toString()) ?: false
        if (!sent) {
            pending.remove(id)
            throw IllegalStateException("A conexão de controle da LG foi encerrada.")
        }
        return try { withTimeout(8_000) { deferred.await() } }
        finally { pending.remove(id) }
    }

    private fun pointerWebSocketUrl(raw: String): String {
        val value = raw.trim()
        require(value.isNotEmpty()) { "A TV não forneceu o endereço do controle direcional." }
        return when {
            value.startsWith("ws://", true) || value.startsWith("wss://", true) -> value
            value.startsWith("https://", true) -> "wss://${value.substringAfter("://")}"
            value.startsWith("http://", true) -> "ws://${value.substringAfter("://")}"
            value.startsWith("/") -> "ws://${tv.ip}:3000$value"
            else -> "ws://${tv.ip}:3000/$value"
        }
    }

    private suspend fun ensurePointer() {
        if (pointerSocket != null) return
        val response = request("ssap://com.webos.service.networkinput/getPointerInputSocket")
        val payload = response.optJSONObject("payload")
        val rawPath = payload?.optString("socketPath").orEmpty()
            .ifBlank { payload?.optString("socket-path").orEmpty() }
        val url = pointerWebSocketUrl(rawPath)
        pointerSocket = openWebSocket(url) { }
    }

    private suspend fun sendPointerButton(name: String) {
        ensurePointer()
        val message = "type:button\nname:$name\n\n"
        if (pointerSocket?.send(message) != true) {
            pointerSocket = null
            throw IllegalStateException("O canal de teclas da LG foi desconectado. Reconecte a TV e tente novamente.")
        }
    }

    override suspend fun sendKey(key: TVKey): Result<Unit> = runCatching {
        if (!connected) connect().getOrThrow()
        when (key) {
            TVKey.VOLUME_UP -> request("ssap://audio/volumeUp")
            TVKey.VOLUME_DOWN -> request("ssap://audio/volumeDown")
            TVKey.MUTE -> setMute(true).getOrThrow()
            TVKey.POWER -> request("ssap://system/turnOff")
            TVKey.CHANNEL_UP -> request("ssap://tv/channelUp")
            TVKey.CHANNEL_DOWN -> request("ssap://tv/channelDown")
            else -> {
                val name = when (key) {
                    TVKey.UP -> "UP"; TVKey.DOWN -> "DOWN"; TVKey.LEFT -> "LEFT"; TVKey.RIGHT -> "RIGHT"
                    TVKey.OK -> "ENTER"; TVKey.BACK, TVKey.EXIT -> "BACK"; TVKey.HOME -> "HOME"
                    TVKey.MENU, TVKey.SETTINGS -> "MENU"; TVKey.PLAY -> "PLAY"; TVKey.PAUSE -> "PAUSE"
                    TVKey.PLAY_PAUSE -> "PLAYPAUSE"; TVKey.STOP -> "STOP"; TVKey.REWIND -> "REWIND"
                    TVKey.FAST_FORWARD -> "FASTFORWARD"; TVKey.RED -> "RED"; TVKey.GREEN -> "GREEN"
                    TVKey.YELLOW -> "YELLOW"; TVKey.BLUE -> "BLUE"; TVKey.SOURCE -> "INPUT"; TVKey.INFO -> "INFO"
                    TVKey.DIGIT_0 -> "0"; TVKey.DIGIT_1 -> "1"; TVKey.DIGIT_2 -> "2"; TVKey.DIGIT_3 -> "3"
                    TVKey.DIGIT_4 -> "4"; TVKey.DIGIT_5 -> "5"; TVKey.DIGIT_6 -> "6"; TVKey.DIGIT_7 -> "7"
                    TVKey.DIGIT_8 -> "8"; TVKey.DIGIT_9 -> "9"; TVKey.DELETE -> "DELETE"
                    else -> throw IllegalArgumentException("Esta tecla não está disponível no protocolo LG webOS.")
                }
                sendPointerButton(name)
            }
        }
    }

    override suspend fun launchApp(appId: String): Result<Unit> = runCatching {
        request("ssap://system.launcher/launch", JSONObject().put("id", appId))
    }

    override suspend fun getAppList(): Result<List<TVApp>> = runCatching {
        val payload = request("ssap://com.webos.service.applicationManager/listLaunchPoints")
            .optJSONArray("payload") ?: JSONArray()
        (0 until payload.length()).mapNotNull { i ->
            payload.optJSONObject(i)?.let { TVApp(it.optString("id"), it.optString("title", it.optString("name"))) }
        }
    }

    override suspend fun powerOff(): Result<Unit> = sendKey(TVKey.POWER)
    override suspend fun volumeUp(): Result<Unit> = sendKey(TVKey.VOLUME_UP)
    override suspend fun volumeDown(): Result<Unit> = sendKey(TVKey.VOLUME_DOWN)
    override suspend fun setMute(mute: Boolean): Result<Unit> = runCatching {
        request("ssap://audio/setMute", JSONObject().put("mute", mute))
    }
    override suspend fun channelUp(): Result<Unit> = sendKey(TVKey.CHANNEL_UP)
    override suspend fun channelDown(): Result<Unit> = sendKey(TVKey.CHANNEL_DOWN)

    override suspend fun disconnect() {
        pointerSocket?.close(1000, "disconnect")
        socket?.close(1000, "disconnect")
        pointerSocket = null; socket = null; connected = false
        registration = CompletableDeferred()
        pending.values.forEach { it.cancel() }; pending.clear()
    }

    override fun isConnected(): Boolean = connected && registration.isCompleted
}
