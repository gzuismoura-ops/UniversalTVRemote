package com.universalremote.tv.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import com.universalremote.tv.model.DiscoveredTV
import com.universalremote.tv.model.TVBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface

class NetworkScanner(private val context: Context) {
    private val ssdp = SSDPDiscovery(context)
    private val mdns = MDNSDiscovery(context)
    private val tcp = TCPProbe()
    private val fingerprint = HTTPFingerprint()

    suspend fun scan(onProgress: (String) -> Unit = {}): List<DiscoveredTV> = withContext(Dispatchers.IO) {
        val found = LinkedHashMap<String, DiscoveredTV>()
        coroutineScope {
            val ssdpJob = async { runCatching { ssdp.discover() }.getOrDefault(emptyList()) }
            val mdnsJob = async { runCatching { mdns.discover() }.getOrDefault(emptyList()) }
            onProgress("Searching local network services")
            val localIp = findLocalIpv4()
            val portMapJob = if (localIp != null) async { runCatching { tcp.scanSubnet(localIp) }.getOrDefault(emptyMap()) } else async { emptyMap() }
            val ssdpRecords = ssdpJob.await()
            onProgress("Reading UPnP device descriptions")
            ssdpRecords.forEach { record ->
                val details = ssdp.describe(record)
                val combined = listOf(details["manufacturer"], details["modelName"], details["modelDescription"], record.server, record.st).filterNotNull().joinToString(" ")
                val brand = TVBrand.fromText(combined)
                val port = runCatching { java.net.URI(record.location).port }.getOrDefault(-1).let { if (it > 0) it else 80 }
                found[record.ip] = DiscoveredTV(record.ip, brand, details["modelName"].orEmpty(), details["friendlyName"].orEmpty().ifBlank { "${brand.displayName} (${record.ip})" }, port, "SSDP/UPnP", brand in setOf(TVBrand.LG, TVBrand.SAMSUNG, TVBrand.SONY, TVBrand.VIZIO, TVBrand.PHILIPS), descriptionUrl = record.location)
            }
            onProgress("Checking network services")
            mdnsJob.await().forEach { record ->
                val brand = when {
                    "googlecast" in record.serviceType -> TVBrand.CHROMECAST
                    "androidtvremote2" in record.serviceType -> TVBrand.ANDROID_TV
                    else -> TVBrand.UNKNOWN
                }
                found.putIfAbsent(record.ip, DiscoveredTV(record.ip, brand, "", record.name, record.port, "mDNS ${record.serviceType}", brand == TVBrand.ANDROID_TV))
            }
            onProgress("Fingerprinting open ports")
            portMapJob.await().forEach { (ip, ports) ->
                val identified = runCatching { fingerprint.identify(ip, ports) }.getOrNull()
                if (identified != null) {
                    val prior = found[ip]
                    found[ip] = if (prior == null || prior.brand == TVBrand.UNKNOWN) identified else prior.copy(
                        brand = if (prior.brand == TVBrand.UNKNOWN) identified.brand else prior.brand,
                        port = identified.port.takeIf { it > 0 } ?: prior.port,
                        protocol = listOf(prior.protocol, identified.protocol).distinct().joinToString(" + ")
                    )
                }
            }
        }
        val arp = readArpTable()
        found.values.map { device -> device.copy(macAddress = device.macAddress ?: arp[device.ip]) }
            .sortedWith(compareBy<DiscoveredTV> { it.brand == TVBrand.UNKNOWN }.thenBy { it.friendlyName.lowercase() })
    }

    private fun readArpTable(): Map<String, String> = runCatching {
        java.io.File("/proc/net/arp").readLines().drop(1).mapNotNull { line ->
            val columns = line.trim().split(Regex("\\s+"))
            if (columns.size >= 4 && columns[3].matches(Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}"))) columns[0] to columns[3] else null
        }.toMap()
    }.getOrDefault(emptyMap())

    private fun findLocalIpv4(): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val active = cm.activeNetwork
        val properties: LinkProperties? = active?.let(cm::getLinkProperties)
        val linkIp = properties?.linkAddresses?.mapNotNull { it.address as? Inet4Address }?.firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
        if (linkIp != null) return linkIp.hostAddress
        return runCatching {
            NetworkInterface.getNetworkInterfaces().toList().asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList().asSequence() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { it.isSiteLocalAddress && !it.isLoopbackAddress }
                ?.hostAddress
        }.getOrNull()
    }
}
