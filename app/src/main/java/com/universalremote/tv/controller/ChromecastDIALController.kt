package com.universalremote.tv.controller

import com.universalremote.tv.data.CredentialStore
import com.universalremote.tv.model.DiscoveredTV
import com.universalremote.tv.model.TVApp
import com.universalremote.tv.model.TVKey
import org.json.JSONObject

class ChromecastDIALController(tv: DiscoveredTV, credentialStore: CredentialStore) : BaseTVController(tv, credentialStore) {
    private val base get() = "http://${tv.ip}:8008"
    override suspend fun connect(): Result<Unit> = runCatching {
        val response = http("GET", "$base/setup/eureka_info")
        if (!response.successful) throw IllegalStateException("Chromecast DIAL endpoint unavailable (${response.code})")
        connected = true
    }
    override suspend fun pair(pin: String?): Result<Unit> = connect()
    override suspend fun sendKey(key: TVKey): Result<Unit> = when (key) {
        TVKey.VOLUME_UP -> runCatching { val r = http("POST", "$base/setup/assistant/setVolume", JSONObject().put("volume", 0.6).toString()); if (!r.successful) throw IllegalStateException("Chromecast volume request failed") }
        TVKey.VOLUME_DOWN -> runCatching { val r = http("POST", "$base/setup/assistant/setVolume", JSONObject().put("volume", 0.3).toString()); if (!r.successful) throw IllegalStateException("Chromecast volume request failed") }
        else -> failed("DIAL provides app launching and limited device controls, not a universal directional remote. Use the Google TV remote protocol for full controls.")
    }
    override suspend fun launchApp(appId: String): Result<Unit> = runCatching {
        val safeId = appId.replace(Regex("[^A-Za-z0-9._-]"), "")
        require(safeId.isNotBlank()) { "Invalid app identifier" }
        val response = http("POST", "$base/apps/$safeId", "", contentType = "text/plain")
        if (!response.successful) throw IllegalStateException("Chromecast could not launch the app (${response.code})")
    }
    override suspend fun getAppList(): Result<List<TVApp>> = Result.success(listOf(
        TVApp("YouTube", "YouTube"), TVApp("Netflix", "Netflix"), TVApp("GoogleTV", "Google TV")
    ))
    override suspend fun powerOff(): Result<Unit> = failed("DIAL does not expose a reliable TV power-off command")
    override suspend fun volumeUp(): Result<Unit> = sendKey(TVKey.VOLUME_UP)
    override suspend fun volumeDown(): Result<Unit> = sendKey(TVKey.VOLUME_DOWN)
    override suspend fun setMute(mute: Boolean): Result<Unit> = failed("Mute control is not available through the DIAL interface")
    override suspend fun channelUp(): Result<Unit> = failed("Channel control is not available through the DIAL interface")
    override suspend fun channelDown(): Result<Unit> = failed("Channel control is not available through the DIAL interface")
    override suspend fun disconnect() { connected = false }
    override fun isConnected(): Boolean = connected
}
