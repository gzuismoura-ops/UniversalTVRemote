package com.universalremote.tv.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList

 data class MDNSRecord(val ip: String, val port: Int, val serviceType: String, val name: String, val attributes: Map<String, String>)

class MDNSDiscovery(context: Context) {
    private val manager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val serviceTypes = listOf("_googlecast._tcp.", "_airplay._tcp.", "_androidtvremote2._tcp.")

    suspend fun discover(windowMs: Long = 1_800): List<MDNSRecord> = withContext(Dispatchers.IO) {
        coroutineScope {
            serviceTypes.map { type -> async { browseType(type, windowMs) } }.awaitAll().flatten().distinctBy { "${it.ip}:${it.port}:${it.serviceType}" }
        }
    }

    private suspend fun browseType(type: String, windowMs: Long): List<MDNSRecord> {
        val records = CopyOnWriteArrayList<MDNSRecord>()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                runCatching {
                    manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val attrs = info.attributes.mapNotNull { (k, v) -> runCatching { k to String(v, Charsets.UTF_8) }.getOrNull() }.toMap()
                            info.host?.hostAddress?.let { ip -> records.add(MDNSRecord(ip, info.port, type, info.serviceName, attrs)) }
                        }
                    })
                }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { runCatching { manager.stopServiceDiscovery(this) } }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) { runCatching { manager.stopServiceDiscovery(this) } }
            override fun onDiscoveryStopped(serviceType: String) = Unit
        }
        runCatching { manager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener) }
        delay(windowMs)
        runCatching { manager.stopServiceDiscovery(listener) }
        return records.toList()
    }
}
