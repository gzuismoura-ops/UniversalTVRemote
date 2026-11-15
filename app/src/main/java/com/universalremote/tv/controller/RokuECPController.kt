package com.universalremote.tv.controller

import com.universalremote.tv.data.CredentialStore
import com.universalremote.tv.model.DiscoveredTV
import com.universalremote.tv.model.TVApp
import com.universalremote.tv.model.TVKey

class RokuECPController(tv: DiscoveredTV, credentialStore: CredentialStore) : BaseTVController(tv, credentialStore) {
    private val base get() = "http://${tv.ip}:8060"
    override suspend fun connect(): Result<Unit> = runCatching {
        val response = http("GET", "$base/query/device-info")
        if (!response.successful) throw IllegalStateException("Roku ECP is unavailable (${response.code})")
        connected = true
    }
    override suspend fun pair(pin: String?): Result<Unit> = connect()

    private fun keyName(key: TVKey): String = when (key) {
        TVKey.UP -> "Up"; TVKey.DOWN -> "Down"; TVKey.LEFT -> "Left"; TVKey.RIGHT -> "Right"; TVKey.OK -> "Select"
        TVKey.BACK -> "Back"; TVKey.HOME -> "Home"; TVKey.PLAY, TVKey.PLAY_PAUSE -> "Play"; TVKey.PAUSE -> "Play"
        TVKey.STOP -> "Stop"; TVKey.REWIND -> "Rev"; TVKey.FAST_FORWARD -> "Fwd"; TVKey.INFO -> "Info"
        TVKey.VOLUME_UP -> "VolumeUp"; TVKey.VOLUME_DOWN -> "VolumeDown"; TVKey.MUTE -> "VolumeMute"
        TVKey.POWER -> "PowerOff"; TVKey.SOURCE -> "InputTuner"; TVKey.CHANNEL_UP -> "ChannelUp"; TVKey.CHANNEL_DOWN -> "ChannelDown"
        TVKey.DIGIT_0 -> "Lit_0"; TVKey.DIGIT_1 -> "Lit_1"; TVKey.DIGIT_2 -> "Lit_2"; TVKey.DIGIT_3 -> "Lit_3"; TVKey.DIGIT_4 -> "Lit_4"
        TVKey.DIGIT_5 -> "Lit_5"; TVKey.DIGIT_6 -> "Lit_6"; TVKey.DIGIT_7 -> "Lit_7"; TVKey.DIGIT_8 -> "Lit_8"; TVKey.DIGIT_9 -> "Lit_9"
        TVKey.EXIT -> "Back"; else -> throw IllegalArgumentException("This Roku key is not supported")
    }
    override suspend fun sendKey(key: TVKey): Result<Unit> = runCatching {
        val response = http("POST", "$base/keypress/${keyName(key)}", "", contentType = "text/plain")
        if (!response.successful) throw IllegalStateException("Roku rejected the command (${response.code})")
    }
    override suspend fun launchApp(appId: String): Result<Unit> = runCatching {
        val response = http("POST", "$base/launch/$appId", "", contentType = "text/plain")
        if (!response.successful) throw IllegalStateException("Roku could not launch this app (${response.code})")
    }
    override suspend fun getAppList(): Result<List<TVApp>> = runCatching {
        val response = http("GET", "$base/query/apps")
        if (!response.successful) throw IllegalStateException("Roku app list is unavailable")
        Regex("<app[^>]*id=\"([^\"]+)\"[^>]*>(.*?)</app>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .findAll(response.body).map { TVApp(it.groupValues[1], it.groupValues[2].trim()) }.toList()
    }
    override suspend fun powerOff(): Result<Unit> = sendKey(TVKey.POWER)
    override suspend fun volumeUp(): Result<Unit> = sendKey(TVKey.VOLUME_UP)
    override suspend fun volumeDown(): Result<Unit> = sendKey(TVKey.VOLUME_DOWN)
    override suspend fun setMute(mute: Boolean): Result<Unit> = sendKey(TVKey.MUTE)
    override suspend fun channelUp(): Result<Unit> = sendKey(TVKey.CHANNEL_UP)
    override suspend fun channelDown(): Result<Unit> = sendKey(TVKey.CHANNEL_DOWN)
    override suspend fun disconnect() { connected = false }
    override fun isConnected(): Boolean = connected
}
