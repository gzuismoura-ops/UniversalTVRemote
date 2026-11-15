package com.universalremote.tv.controller

import com.universalremote.tv.data.CredentialStore
import com.universalremote.tv.model.DiscoveredTV
import com.universalremote.tv.model.TVApp
import com.universalremote.tv.model.TVKey
import java.net.URI

/** UPnP AV devices advertise service control URLs in their description document. */
class GenericDLNAController(tv: DiscoveredTV, credentialStore: CredentialStore) : BaseTVController(tv, credentialStore) {
    private var renderingControlUrl: String? = null
    private var avTransportUrl: String? = null

    override suspend fun connect(): Result<Unit> = runCatching {
        val location = tv.descriptionUrl ?: throw IllegalStateException("The UPnP description URL was not retained during discovery")
        val response = http("GET", location)
        if (!response.successful) throw IllegalStateException("Could not read the UPnP device description (${response.code})")
        val description = response.body
        fun controlFor(serviceName: String): String? {
            val service = Regex("<service>(.*?)</service>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .findAll(description).map { it.groupValues[1] }
                .firstOrNull { it.contains(serviceName, true) } ?: return null
            val control = Regex("<controlURL>(.*?)</controlURL>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(service)?.groupValues?.get(1)?.trim()
            return control?.let { URI(location).resolve(it).toString() }
        }
        renderingControlUrl = controlFor("RenderingControl")
        avTransportUrl = controlFor("AVTransport")
        if (renderingControlUrl == null && avTransportUrl == null) throw IllegalStateException("The UPnP device does not advertise AVTransport or RenderingControl services")
        connected = true
    }

    override suspend fun pair(pin: String?): Result<Unit> = connect()

    private suspend fun soap(endpoint: String, service: String, action: String, arguments: String = ""): HttpResult {
        val envelope = """<?xml version="1.0" encoding="utf-8"?><s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"><s:Body><u:$action xmlns:u="urn:schemas-upnp-org:service:$service:1">$arguments</u:$action></s:Body></s:Envelope>"""
        return http("POST", endpoint, envelope, headers = mapOf("SOAPACTION" to "\"urn:schemas-upnp-org:service:$service:1#$action\""), contentType = "text/xml; charset=UTF-8")
    }

    private suspend fun setVolume(value: Int) {
        val endpoint = renderingControlUrl ?: throw IllegalStateException("This UPnP device has no RenderingControl service")
        val args = "<InstanceID>0</InstanceID><Channel>Master</Channel><DesiredVolume>${value.coerceIn(0, 100)}</DesiredVolume>"
        val response = soap(endpoint, "RenderingControl", "SetVolume", args)
        if (!response.successful) throw IllegalStateException("UPnP volume change failed (${response.code})")
    }

    private suspend fun changeVolume(delta: Int) {
        val endpoint = renderingControlUrl ?: throw IllegalStateException("This UPnP device has no RenderingControl service")
        val response = soap(endpoint, "RenderingControl", "GetVolume", "<InstanceID>0</InstanceID><Channel>Master</Channel>")
        if (!response.successful) throw IllegalStateException("Could not read UPnP volume (${response.code})")
        val value = Regex("<CurrentVolume>(\\d+)</CurrentVolume>", RegexOption.IGNORE_CASE).find(response.body)?.groupValues?.get(1)?.toIntOrNull() ?: 20
        setVolume(value + delta)
    }

    override suspend fun sendKey(key: TVKey): Result<Unit> = runCatching {
        when (key) {
            TVKey.VOLUME_UP -> changeVolume(5)
            TVKey.VOLUME_DOWN -> changeVolume(-5)
            TVKey.PLAY, TVKey.PLAY_PAUSE -> {
                val endpoint = avTransportUrl ?: throw IllegalStateException("This UPnP device has no AVTransport service")
                val r = soap(endpoint, "AVTransport", "Play", "<InstanceID>0</InstanceID><Speed>1</Speed>")
                if (!r.successful) throw IllegalStateException("UPnP Play failed (${r.code})")
            }
            TVKey.PAUSE -> {
                val endpoint = avTransportUrl ?: throw IllegalStateException("This UPnP device has no AVTransport service")
                val r = soap(endpoint, "AVTransport", "Pause", "<InstanceID>0</InstanceID>")
                if (!r.successful) throw IllegalStateException("UPnP Pause failed (${r.code})")
            }
            TVKey.STOP -> {
                val endpoint = avTransportUrl ?: throw IllegalStateException("This UPnP device has no AVTransport service")
                val r = soap(endpoint, "AVTransport", "Stop", "<InstanceID>0</InstanceID>")
                if (!r.successful) throw IllegalStateException("UPnP Stop failed (${r.code})")
            }
            else -> throw IllegalArgumentException("This DLNA renderer only exposes media transport and volume controls")
        }
    }

    override suspend fun launchApp(appId: String): Result<Unit> = failed("UPnP DLNA does not define a standard app-launch API")
    override suspend fun getAppList(): Result<List<TVApp>> = failedValue("UPnP DLNA does not define a standard app-list API")
    override suspend fun powerOff(): Result<Unit> = failed("Power control is not part of the standard DLNA renderer profile")
    override suspend fun volumeUp(): Result<Unit> = sendKey(TVKey.VOLUME_UP)
    override suspend fun volumeDown(): Result<Unit> = sendKey(TVKey.VOLUME_DOWN)
    override suspend fun setMute(mute: Boolean): Result<Unit> = runCatching {
        val endpoint = renderingControlUrl ?: throw IllegalStateException("This UPnP device has no RenderingControl service")
        val response = soap(endpoint, "RenderingControl", "SetMute", "<InstanceID>0</InstanceID><Channel>Master</Channel><DesiredMute>${if (mute) 1 else 0}</DesiredMute>")
        if (!response.successful) throw IllegalStateException("UPnP mute change failed (${response.code})")
    }
    override suspend fun channelUp(): Result<Unit> = failed("Channel control is not part of generic DLNA")
    override suspend fun channelDown(): Result<Unit> = failed("Channel control is not part of generic DLNA")
    override suspend fun disconnect() { connected = false; renderingControlUrl = null; avTransportUrl = null }
    override fun isConnected(): Boolean = connected
}
