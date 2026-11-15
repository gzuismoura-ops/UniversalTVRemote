package com.universalremote.tv.network

import com.universalremote.tv.model.DiscoveredTV
import com.universalremote.tv.model.TVBrand
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class HTTPFingerprint {
    private val client = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).writeTimeout(5, TimeUnit.SECONDS).build()

    fun identify(ip: String, ports: Set<Int>): DiscoveredTV? {
        fun get(url: String): String = runCatching {
            client.newCall(Request.Builder().url(url).header("User-Agent", "UniversalTVRemote/1.0").get().build()).execute().use { it.body?.string().orEmpty() }
        }.getOrDefault("")
        fun tv(brand: TVBrand, port: Int, protocol: String, name: String, model: String = ""): DiscoveredTV =
            DiscoveredTV(ip, brand, model.ifBlank { brand.displayName }, name.ifBlank { "${brand.displayName} ($ip)" }, port, protocol, brand in setOf(TVBrand.LG, TVBrand.SAMSUNG, TVBrand.SONY, TVBrand.VIZIO, TVBrand.PHILIPS, TVBrand.ANDROID_TV))

        if (8060 in ports) {
            val xml = get("http://$ip:8060/query/device-info")
            val model = tag(xml, "model-name")
            val name = tag(xml, "user-device-name").ifBlank { tag(xml, "friendly-device-name") }
            if (xml.isNotBlank()) return tv(TVBrand.ROKU, 8060, "HTTP/ECP", name, model)
        }
        if (3000 in ports) {
            val body = get("http://$ip:3000/")
            if (body.contains("webos", true) || body.contains("webOS", true) || 3000 in ports) return tv(TVBrand.LG, 3000, "WebSocket/SSAP", "LG webOS ($ip)")
        }
        if (8001 in ports || 8002 in ports) {
            val body = get("http://$ip:8001/api/v2/")
            val name = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1).orEmpty()
            if (body.isNotBlank() || 8001 in ports || 8002 in ports) return tv(TVBrand.SAMSUNG, if (8002 in ports) 8002 else 8001, "WebSocket/Tizen", name.ifBlank { "Samsung TV ($ip)" })
        }
        if (55000 in ports) return tv(TVBrand.PANASONIC, 55000, "HTTP/SOAP", "Panasonic Viera ($ip)")
        if (1925 in ports || 1926 in ports) {
            val body = get("http://$ip:1925/6/system")
            return tv(TVBrand.PHILIPS, if (1926 in ports) 1926 else 1925, "HTTP/JointSpace", tag(body, "name").ifBlank { "Philips TV ($ip)" })
        }
        if (7345 in ports) return tv(TVBrand.VIZIO, 7345, "HTTPS/SmartCast", "Vizio SmartCast ($ip)")
        if (8008 in ports || 8009 in ports) {
            val body = get("http://$ip:8008/ssdp/device-desc.xml")
            return tv(TVBrand.CHROMECAST, if (8008 in ports) 8008 else 8009, "HTTP/DIAL", tag(body, "friendlyName").ifBlank { "Chromecast ($ip)" })
        }
        if (5555 in ports) return tv(TVBrand.ANDROID_TV, 5555, "ADB/TCP (legacy only)", "Android TV ($ip)")
        if (80 in ports || 443 in ports) {
            val body = get("http://$ip/sony/system")
            if (body.contains("product", true) || body.contains("Sony", true)) return tv(TVBrand.SONY, if (80 in ports) 80 else 443, "HTTP/IRCC", "Sony Bravia ($ip)")
        }
        if (ports.isNotEmpty()) return tv(TVBrand.UNKNOWN, ports.first(), "HTTP/UPnP", "Network device ($ip)")
        return null
    }

    private fun tag(xml: String, name: String): String = Regex("<$name[^>]*>(.*?)</$name>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(xml)?.groupValues?.get(1)?.trim().orEmpty()
}
