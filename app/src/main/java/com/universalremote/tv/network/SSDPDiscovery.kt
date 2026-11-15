package com.universalremote.tv.network

import android.content.Context
import android.net.wifi.WifiManager
import com.universalremote.tv.model.TVBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.util.concurrent.TimeUnit

 data class SSDPRecord(val ip: String, val location: String, val server: String, val st: String)

class SSDPDiscovery(private val context: Context) {
    private val targets = listOf(
        "ssdp:all",
        "urn:dial-multiscreen-org:service:dial:1",
        "urn:schemas-upnp-org:device:MediaRenderer:1",
        "urn:lge-com:service:webos-second-screen:1",
        "urn:samsung.com:device:RemoteControlReceiver:1",
        "roku:ecp"
    )

    suspend fun discover(timeoutMs: Long = 3_500): List<SSDPRecord> = withContext(Dispatchers.IO) {
        val results = LinkedHashMap<String, SSDPRecord>()
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val lock = runCatching { wifi?.createMulticastLock("universal-tv-ssdp")?.apply { setReferenceCounted(false); acquire() } }.getOrNull()
        try {
            MulticastSocket().use { socket ->
                socket.soTimeout = 350
                val destination = InetAddress.getByName("239.255.255.250")
                targets.forEach { target ->
                    val message = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: $target\r\nUSER-AGENT: Android/14 UPnP/1.1 UniversalTVRemote/1.0\r\n\r\n"
                    val data = message.toByteArray(Charsets.US_ASCII)
                    runCatching { socket.send(DatagramPacket(data, data.size, destination, 1900)) }
                }
                val endAt = System.currentTimeMillis() + timeoutMs
                while (System.currentTimeMillis() < endAt) {
                    val bytes = ByteArray(8_192)
                    val packet = DatagramPacket(bytes, bytes.size)
                    try {
                        socket.receive(packet)
                        val text = String(packet.data, 0, packet.length, Charsets.ISO_8859_1)
                        val headers = text.lineSequence().mapNotNull { line ->
                            val i = line.indexOf(':')
                            if (i > 0) line.substring(0, i).trim().lowercase() to line.substring(i + 1).trim() else null
                        }.toMap()
                        val location = headers["location"] ?: continue
                        val ip = packet.address.hostAddress ?: continue
                        results[ip] = SSDPRecord(ip, location, headers["server"] ?: "", headers["st"] ?: "")
                    } catch (_: java.net.SocketTimeoutException) {
                        continue
                    }
                }
            }
        } finally {
            runCatching { if (lock?.isHeld == true) lock.release() }
        }
        results.values.toList()
    }

    suspend fun describe(record: SSDPRecord): Map<String, String> = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS).build()
        runCatching {
            client.newCall(Request.Builder().url(record.location).get().build()).execute().use { response ->
                val xml = response.body?.string().orEmpty()
                fun value(tag: String): String = Regex("<$tag[^>]*>(.*?)</$tag>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(xml)?.groupValues?.get(1)?.trim().orEmpty()
                mapOf("friendlyName" to value("friendlyName"), "manufacturer" to value("manufacturer"), "modelName" to value("modelName"), "modelDescription" to value("modelDescription"))
            }
        }.getOrDefault(emptyMap())
    }
}
