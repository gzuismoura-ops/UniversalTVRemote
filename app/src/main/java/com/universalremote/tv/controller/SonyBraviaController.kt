package com.universalremote.tv.controller

import android.util.Base64
import com.universalremote.tv.data.CredentialStore
import com.universalremote.tv.model.DiscoveredTV
import com.universalremote.tv.model.TVApp
import com.universalremote.tv.model.TVKey
import org.json.JSONArray
import org.json.JSONObject

class SonyBraviaController(tv: DiscoveredTV, credentialStore: CredentialStore) : BaseTVController(tv, credentialStore) {
    private val codes = mapOf(
        TVKey.UP to "AAAAAQAAAAEAAAAUAw==", TVKey.DOWN to "AAAAAQAAAAEAAAAVAw==",
        TVKey.RIGHT to "AAAAAQAAAAEAAAAXAw==", TVKey.LEFT to "AAAAAQAAAAEAAAAYAw==", TVKey.OK to "AAAAAQAAAAEAAABlAw==",
        TVKey.BACK to "AAAAAgAAAJcAAAAjAw==", TVKey.HOME to "AAAAAQAAAAEAAABgAw==", TVKey.VOLUME_UP to "AAAAAQAAAAEAAAASAw==",
        TVKey.VOLUME_DOWN to "AAAAAQAAAAEAAAATAw==", TVKey.MUTE to "AAAAAQAAAAEAAAAlAw==", TVKey.SOURCE to "AAAAAQAAAAEAAAAkAw==",
        TVKey.CHANNEL_UP to "AAAAAQAAAAEAAAAQAw==", TVKey.CHANNEL_DOWN to "AAAAAQAAAAEAAAARAw==", TVKey.POWER to "AAAAAQAAAAEAAAAvAw==",
        TVKey.PLAY to "AAAAAQAAAAEAAAAyAw==", TVKey.PAUSE to "AAAAAQAAAAEAAAAzAw==", TVKey.PLAY_PAUSE to "AAAAAQAAAAEAAAAyAw==",
        TVKey.DIGIT_0 to "AAAAAQAAAAEAAAAJAw==", TVKey.DIGIT_1 to "AAAAAQAAAAEAAAAAAw==", TVKey.DIGIT_2 to "AAAAAQAAAAEAAAABAw==",
        TVKey.DIGIT_3 to "AAAAAQAAAAEAAAACAw==", TVKey.DIGIT_4 to "AAAAAQAAAAEAAAADAw==", TVKey.DIGIT_5 to "AAAAAQAAAAEAAAAEAw==",
        TVKey.DIGIT_6 to "AAAAAQAAAAEAAAAFAw==", TVKey.DIGIT_7 to "AAAAAQAAAAEAAAAGAw==", TVKey.DIGIT_8 to "AAAAAQAAAAEAAAAHAw==", TVKey.DIGIT_9 to "AAAAAQAAAAEAAAAIAw=="
    )

    override suspend fun connect(): Result<Unit> = runCatching {
        val headers = credentialStore.getCredential(tv.ip, "cookie")?.let { mapOf("Cookie" to it) } ?: emptyMap()
        val result = http("POST", "http://${tv.ip}/sony/system", JSONObject().put("method", "getSystemInformation").put("params", JSONArray()).put("id", 1).put("version", "1.0").toString(), headers)
        if (result.successful) connected = true else throw IllegalStateException("Sony TV did not authorize the remote (${result.code}). Pair the TV or configure its Pre-Shared Key.")
    }.recoverCatching { throw IllegalStateException("Could not connect to Sony Bravia: ${safeMessage(it)}") }

    override suspend fun pair(pin: String?): Result<Unit> = runCatching {
        val registration = JSONObject().put("method", "actRegister").put("params", JSONArray()
            .put(JSONObject().put("clientid", "UniversalRemote:1").put("nickname", "Universal Remote").put("level", "private"))
            .put(JSONObject().put("function", "WOL").put("value", "yes"))).put("id", 8).put("version", "1.0").toString()
        if (pin.isNullOrBlank()) {
            val challenge = http("POST", "http://${tv.ip}/sony/accessControl", registration)
            if (challenge.code !in 200..499) throw IllegalStateException("Sony pairing request failed (${challenge.code})")
            throw IllegalStateException("Check the Sony TV screen for its registration PIN, then enter it here.")
        }
        val headers = mapOf("Authorization" to "Basic " + Base64.encodeToString(":$pin".toByteArray(), Base64.NO_WRAP))
        val response = http("POST", "http://${tv.ip}/sony/accessControl", registration, headers)
        if (!response.successful) throw IllegalStateException("Sony pairing failed (${response.code}). Check the PIN and try again.")
        response.headers.entries.firstOrNull { it.key.equals("Set-Cookie", true) }?.value?.firstOrNull()?.let { credentialStore.saveCredential(tv.ip, "cookie", it) }
        credentialStore.saveCredential(tv.ip, "pairing", "registered")
        connected = true
    }

    override suspend fun sendKey(key: TVKey): Result<Unit> = runCatching {
        if (!connected) connect().getOrThrow()
        val code = codes[key] ?: throw IllegalArgumentException("This Sony IRCC key is not available")
        val envelope = """<?xml version="1.0"?><s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"><s:Body><u:X_SendIRCC xmlns:u="urn:schemas-sony-com:service:IRCC:1"><IRCCCode>$code</IRCCCode></u:X_SendIRCC></s:Body></s:Envelope>"""
        val psk = credentialStore.getCredential(tv.ip, "psk")
        val headers = mutableMapOf("SOAPACTION" to "\"urn:schemas-sony-com:service:IRCC:1#X_SendIRCC\"")
        if (psk != null) headers["X-Auth-PSK"] = psk
        credentialStore.getCredential(tv.ip, "cookie")?.let { headers["Cookie"] = it }
        val response = http("POST", "http://${tv.ip}/sony/IRCC", envelope, headers, contentType = "text/xml; charset=UTF-8")
        if (!response.successful) throw IllegalStateException("Sony TV rejected the key (${response.code}). A configured Pre-Shared Key may be required.")
    }

    override suspend fun launchApp(appId: String): Result<Unit> = runCatching {
        val body = JSONObject().put("method", "setActiveApp").put("params", JSONArray().put(JSONObject().put("uri", appId))).put("id", 1).put("version", "1.0").toString()
        val response = http("POST", "http://${tv.ip}/sony/appControl", body)
        if (!response.successful) throw IllegalStateException("Sony app launch failed (${response.code})")
    }

    override suspend fun getAppList(): Result<List<TVApp>> = runCatching {
        val body = JSONObject().put("method", "getApplicationList").put("params", JSONArray().put("1.0")).put("id", 2).put("version", "1.0").toString()
        val response = http("POST", "http://${tv.ip}/sony/appControl", body)
        if (!response.successful) throw IllegalStateException("Sony app list unavailable")
        val array = JSONObject(response.body).optJSONArray("result")?.optJSONArray(0) ?: JSONArray()
        (0 until array.length()).mapNotNull { i -> array.optJSONObject(i)?.let { TVApp(it.optString("uri"), it.optString("title")) } }
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
