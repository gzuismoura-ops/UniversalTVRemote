package com.universalremote.tv.controller

import com.universalremote.tv.data.CredentialStore
import com.universalremote.tv.model.DiscoveredTV
import com.universalremote.tv.model.TVApp
import com.universalremote.tv.model.TVKey
import org.json.JSONArray
import org.json.JSONObject

class VizioSmartCastController(tv: DiscoveredTV, credentialStore: CredentialStore) : BaseTVController(tv, credentialStore) {
    private val base get() = "https://${tv.ip}:7345"
    private var pairingToken: String? = null

    override suspend fun connect(): Result<Unit> = runCatching {
        val token = credentialStore.getCredential(tv.ip, "authToken")
        if (token.isNullOrBlank()) throw IllegalStateException("Vizio requires PIN pairing before remote commands can be sent")
        val response = http("GET", "$base/state/device/deviceinfo", headers = mapOf("AUTH" to token), insecure = true)
        if (!response.successful) throw IllegalStateException("Vizio SmartCast rejected the saved token (${response.code})")
        connected = true
    }

    override suspend fun pair(pin: String?): Result<Unit> = runCatching {
        if (pin.isNullOrBlank()) {
            val start = http("PUT", "$base/pairing/start", JSONObject().put("DEVICE_ID", "UniversalRemote").put("DEVICE_NAME", "Universal Remote").toString(), insecure = true)
            if (!start.successful) throw IllegalStateException("Could not start Vizio pairing (${start.code})")
            pairingToken = JSONObject(start.body).optString("PAIRING_REQ_TOKEN").takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Vizio did not provide a pairing request token")
            throw IllegalStateException("Enter the PIN shown on the Vizio TV")
        }
        val token = pairingToken ?: throw IllegalStateException("Start pairing again to request a new PIN")
        val body = JSONObject().put("DEVICE_ID", "UniversalRemote").put("CHALLENGE_TYPE", 1)
            .put("RESPONSE_VALUE", pin).put("PAIRING_REQ_TOKEN", token).toString()
        val response = http("PUT", "$base/pairing/pair", body, insecure = true)
        if (!response.successful) throw IllegalStateException("Vizio pairing failed (${response.code})")
        val auth = JSONObject(response.body).optString("AUTH_TOKEN").takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Vizio did not return an authentication token")
        credentialStore.saveCredential(tv.ip, "authToken", auth)
        connected = true
    }

    private fun keySpec(key: TVKey): Pair<Int, Int> = when (key) {
        TVKey.VOLUME_UP -> 5 to 1; TVKey.VOLUME_DOWN -> 5 to 0; TVKey.MUTE -> 5 to 3
        TVKey.CHANNEL_UP -> 8 to 1; TVKey.CHANNEL_DOWN -> 8 to 0; TVKey.POWER -> 11 to 0
        TVKey.UP -> 3 to 8; TVKey.DOWN -> 3 to 0; TVKey.LEFT -> 3 to 1; TVKey.RIGHT -> 3 to 7; TVKey.OK -> 3 to 2
        TVKey.BACK -> 4 to 0; TVKey.EXIT -> 9 to 0; TVKey.HOME -> 4 to 3
        TVKey.PLAY -> 2 to 3; TVKey.PAUSE, TVKey.PLAY_PAUSE -> 2 to 2; TVKey.FAST_FORWARD -> 2 to 0; TVKey.REWIND -> 2 to 1
        TVKey.SOURCE -> 7 to 1
        else -> throw IllegalArgumentException("This Vizio key is not supported")
    }

    override suspend fun sendKey(key: TVKey): Result<Unit> = runCatching {
        if (!connected) connect().getOrThrow()
        val (codeSet, code) = keySpec(key)
        val body = JSONObject().put("KEYLIST", JSONArray().put(JSONObject().put("CODESET", codeSet).put("CODE", code).put("ACTION", "KEYPRESS"))).toString()
        val response = http("PUT", "$base/key_command/", body, headers = mapOf("AUTH" to credentialStore.getCredential(tv.ip, "authToken").orEmpty()), insecure = true)
        if (!response.successful) throw IllegalStateException("Vizio rejected the key (${response.code})")
    }
    override suspend fun launchApp(appId: String): Result<Unit> = failed("App launch is not exposed by the generic Vizio SmartCast remote API")
    override suspend fun getAppList(): Result<List<TVApp>> = failedValue("Vizio SmartCast does not expose a compatible installed-app list endpoint")
    override suspend fun powerOff(): Result<Unit> = sendKey(TVKey.POWER)
    override suspend fun volumeUp(): Result<Unit> = sendKey(TVKey.VOLUME_UP)
    override suspend fun volumeDown(): Result<Unit> = sendKey(TVKey.VOLUME_DOWN)
    override suspend fun setMute(mute: Boolean): Result<Unit> = sendKey(TVKey.MUTE)
    override suspend fun channelUp(): Result<Unit> = sendKey(TVKey.CHANNEL_UP)
    override suspend fun channelDown(): Result<Unit> = sendKey(TVKey.CHANNEL_DOWN)
    override suspend fun disconnect() { connected = false }
    override fun isConnected(): Boolean = connected
}
