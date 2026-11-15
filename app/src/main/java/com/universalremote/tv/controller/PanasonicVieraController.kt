package com.universalremote.tv.controller

import com.universalremote.tv.data.CredentialStore
import com.universalremote.tv.model.DiscoveredTV
import com.universalremote.tv.model.TVApp
import com.universalremote.tv.model.TVKey

class PanasonicVieraController(tv: DiscoveredTV, credentialStore: CredentialStore) : BaseTVController(tv, credentialStore) {
    override suspend fun connect(): Result<Unit> = runCatching {
        val probe = http("GET", "http://${tv.ip}:55000/nrc/", contentType = "text/plain")
        if (probe.code in 200..499) connected = true else throw IllegalStateException("Panasonic Viera did not respond (${probe.code})")
    }
    override suspend fun pair(pin: String?): Result<Unit> = connect()

    private fun command(key: TVKey): String = when (key) {
        TVKey.VOLUME_UP -> "VOLUP"; TVKey.VOLUME_DOWN -> "VOLDOWN"; TVKey.MUTE -> "MUTE"; TVKey.CHANNEL_UP -> "CH_UP"; TVKey.CHANNEL_DOWN -> "CH_DOWN"
        TVKey.POWER -> "POWER"; TVKey.UP -> "UP"; TVKey.DOWN -> "DOWN"; TVKey.LEFT -> "LEFT"; TVKey.RIGHT -> "RIGHT"; TVKey.OK -> "ENTER"
        TVKey.BACK -> "RETURN"; TVKey.HOME -> "HOME"; TVKey.MENU, TVKey.SETTINGS -> "MENU"; TVKey.PLAY -> "PLAY"; TVKey.PAUSE, TVKey.PLAY_PAUSE -> "PAUSE"
        TVKey.STOP -> "STOP"; TVKey.REWIND -> "REW"; TVKey.FAST_FORWARD -> "FF"; TVKey.RED -> "RED"; TVKey.GREEN -> "GREEN"
        TVKey.YELLOW -> "YELLOW"; TVKey.BLUE -> "BLUE"; TVKey.SOURCE -> "INPUT"; TVKey.INFO -> "INFO"; TVKey.DIGIT_0 -> "D0"; TVKey.DIGIT_1 -> "D1"
        TVKey.DIGIT_2 -> "D2"; TVKey.DIGIT_3 -> "D3"; TVKey.DIGIT_4 -> "D4"; TVKey.DIGIT_5 -> "D5"; TVKey.DIGIT_6 -> "D6"
        TVKey.DIGIT_7 -> "D7"; TVKey.DIGIT_8 -> "D8"; TVKey.DIGIT_9 -> "D9"; else -> throw IllegalArgumentException("Unsupported Panasonic key")
    }
    override suspend fun sendKey(key: TVKey): Result<Unit> = runCatching {
        val soap = """<?xml version="1.0" encoding="utf-8"?><s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"><s:Body><u:X_SendKey xmlns:u="urn:panasonic-com:service:p00NetworkControl:1"><X_KeyEvent>NRC_${command(key)}-ONOFF</X_KeyEvent></u:X_SendKey></s:Body></s:Envelope>"""
        val response = http("POST", "http://${tv.ip}:55000/nrc/control_0", soap,
            headers = mapOf("SOAPACTION" to "\"urn:panasonic-com:service:p00NetworkControl:1#X_SendKey\""),
            contentType = "text/xml; charset=UTF-8")
        if (!response.successful) throw IllegalStateException("Panasonic rejected the key (${response.code})${if (response.code == 403) ". This model may require its pairing flow." else ""}")
    }
    override suspend fun launchApp(appId: String): Result<Unit> = failed("App launching is not standardized by Panasonic Viera Network Control")
    override suspend fun getAppList(): Result<List<TVApp>> = failedValue("This Panasonic model does not expose an app list through Viera Network Control")
    override suspend fun powerOff(): Result<Unit> = sendKey(TVKey.POWER)
    override suspend fun volumeUp(): Result<Unit> = sendKey(TVKey.VOLUME_UP)
    override suspend fun volumeDown(): Result<Unit> = sendKey(TVKey.VOLUME_DOWN)
    override suspend fun setMute(mute: Boolean): Result<Unit> = sendKey(TVKey.MUTE)
    override suspend fun channelUp(): Result<Unit> = sendKey(TVKey.CHANNEL_UP)
    override suspend fun channelDown(): Result<Unit> = sendKey(TVKey.CHANNEL_DOWN)
    override suspend fun disconnect() { connected = false }
    override fun isConnected(): Boolean = connected
}
