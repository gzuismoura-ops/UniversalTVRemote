package com.universalremote.tv.controller

import com.universalremote.tv.data.CredentialStore
import com.universalremote.tv.model.DiscoveredTV
import com.universalremote.tv.model.TVApp
import com.universalremote.tv.model.TVKey
import org.json.JSONObject

class PhilipsJointSpaceController(tv: DiscoveredTV, credentialStore: CredentialStore) : BaseTVController(tv, credentialStore) {
    private val https get() = tv.port == 1926
    private val base get() = if (https) "https://${tv.ip}:1926/6" else "http://${tv.ip}:1925/6"
    override suspend fun connect(): Result<Unit> = runCatching {
        val response = http("GET", "$base/system", insecure = https)
        if (!response.successful) throw IllegalStateException("Philips JointSpace did not respond (${response.code})")
        connected = true
    }

    override suspend fun pair(pin: String?): Result<Unit> = runCatching {
        if (!https) { connected = true; return@runCatching }
        if (pin.isNullOrBlank()) {
            val body = JSONObject().put("device", JSONObject().put("device_name", "Universal Remote").put("device_os", "Android")
                .put("app_id", "com.universalremote.tv").put("app_name", "Universal TV Remote").put("type", "native"))
                .put("scope", org.json.JSONArray().put("read").put("write").put("control")).toString()
            val response = http("POST", "$base/pair/request", body, insecure = true)
            if (!response.successful) throw IllegalStateException("Philips pairing request failed (${response.code})")
            throw IllegalStateException("Enter the PIN displayed on the Philips TV. This model may also require Digest authentication support.")
        }
        val body = JSONObject().put("pin", pin).toString()
        val response = http("POST", "$base/pair/grant", body, insecure = true)
        if (!response.successful) throw IllegalStateException("Philips rejected the pairing PIN (${response.code})")
        credentialStore.saveCredential(tv.ip, "pairingPin", pin)
        connected = true
    }

    private fun keyName(key: TVKey): String = when (key) {
        TVKey.POWER -> "Standby"; TVKey.VOLUME_UP -> "VolumeUp"; TVKey.VOLUME_DOWN -> "VolumeDown"; TVKey.MUTE -> "Mute"
        TVKey.UP -> "CursorUp"; TVKey.DOWN -> "CursorDown"; TVKey.LEFT -> "CursorLeft"; TVKey.RIGHT -> "CursorRight"; TVKey.OK -> "Confirm"
        TVKey.BACK -> "Back"; TVKey.HOME -> "Home"; TVKey.PLAY, TVKey.PLAY_PAUSE -> "PlayPause"; TVKey.STOP -> "Stop"
        TVKey.FAST_FORWARD -> "FastForward"; TVKey.REWIND -> "Rewind"; TVKey.NEXT -> "Next"; TVKey.PREVIOUS -> "Previous"
        TVKey.CHANNEL_UP -> "ChannelStepUp"; TVKey.CHANNEL_DOWN -> "ChannelStepDown"; TVKey.SOURCE -> "Source"; TVKey.MENU, TVKey.SETTINGS -> "Options"
        TVKey.INFO -> "Find"; TVKey.RED -> "RedColour"; TVKey.GREEN -> "GreenColour"; TVKey.YELLOW -> "YellowColour"; TVKey.BLUE -> "BlueColour"
        TVKey.DIGIT_0 -> "Digit0"; TVKey.DIGIT_1 -> "Digit1"; TVKey.DIGIT_2 -> "Digit2"; TVKey.DIGIT_3 -> "Digit3"; TVKey.DIGIT_4 -> "Digit4"
        TVKey.DIGIT_5 -> "Digit5"; TVKey.DIGIT_6 -> "Digit6"; TVKey.DIGIT_7 -> "Digit7"; TVKey.DIGIT_8 -> "Digit8"; TVKey.DIGIT_9 -> "Digit9"
        else -> throw IllegalArgumentException("Unsupported Philips key")
    }

    override suspend fun sendKey(key: TVKey): Result<Unit> = runCatching {
        val response = http("POST", "$base/input/key", JSONObject().put("key", keyName(key)).toString(), insecure = https)
        if (!response.successful) throw IllegalStateException("Philips JointSpace rejected the key (${response.code})")
    }
    override suspend fun launchApp(appId: String): Result<Unit> = runCatching {
        val response = http("POST", "$base/activities/launch", JSONObject().put("intent", appId).toString(), insecure = https)
        if (!response.successful) throw IllegalStateException("Philips app launch is not supported by this firmware (${response.code})")
    }
    override suspend fun getAppList(): Result<List<TVApp>> = failedValue("Philips JointSpace does not provide a consistent installed-app list across firmware versions")
    override suspend fun powerOff(): Result<Unit> = sendKey(TVKey.POWER)
    override suspend fun volumeUp(): Result<Unit> = sendKey(TVKey.VOLUME_UP)
    override suspend fun volumeDown(): Result<Unit> = sendKey(TVKey.VOLUME_DOWN)
    override suspend fun setMute(mute: Boolean): Result<Unit> = sendKey(TVKey.MUTE)
    override suspend fun channelUp(): Result<Unit> = sendKey(TVKey.CHANNEL_UP)
    override suspend fun channelDown(): Result<Unit> = sendKey(TVKey.CHANNEL_DOWN)
    override suspend fun disconnect() { connected = false }
    override fun isConnected(): Boolean = connected
}
