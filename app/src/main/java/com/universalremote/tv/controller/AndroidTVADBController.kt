package com.universalremote.tv.controller

import com.universalremote.tv.data.CredentialStore
import com.universalremote.tv.model.DiscoveredTV
import com.universalremote.tv.model.TVApp
import com.universalremote.tv.model.TVKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/** Legacy TCP ADB endpoint probe. Modern Android TV uses a separate wireless-pairing service and RSA authorization. */
class AndroidTVADBController(tv: DiscoveredTV, credentialStore: CredentialStore) : BaseTVController(tv, credentialStore) {
    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            Socket().use { socket -> socket.connect(InetSocketAddress(tv.ip, 5555), 1_500) }
            connected = true
        }.recoverCatching { throw IllegalStateException("ADB TCP was not reachable. Modern Android TV remote control uses a separate pairing protocol and cannot be authorized through port 5555 alone.") }
    }

    override suspend fun pair(pin: String?): Result<Unit> = failed("This build detects legacy ADB endpoints but does not implement Android TV's certificate-based wireless pairing. Use the official Android TV remote pairing flow on a compatible TV.")
    override suspend fun sendKey(key: TVKey): Result<Unit> = failed("ADB commands are disabled until an authenticated ADB transport is established. A TCP connection alone is not authorization.")
    override suspend fun launchApp(appId: String): Result<Unit> = failed("ADB application launch requires authenticated ADB access")
    override suspend fun getAppList(): Result<List<TVApp>> = failedValue("ADB app listing requires authenticated ADB access")
    override suspend fun powerOff(): Result<Unit> = sendKey(TVKey.POWER)
    override suspend fun volumeUp(): Result<Unit> = sendKey(TVKey.VOLUME_UP)
    override suspend fun volumeDown(): Result<Unit> = sendKey(TVKey.VOLUME_DOWN)
    override suspend fun setMute(mute: Boolean): Result<Unit> = sendKey(TVKey.MUTE)
    override suspend fun channelUp(): Result<Unit> = sendKey(TVKey.CHANNEL_UP)
    override suspend fun channelDown(): Result<Unit> = sendKey(TVKey.CHANNEL_DOWN)
    override suspend fun disconnect() { connected = false }
    override fun isConnected(): Boolean = connected
}
