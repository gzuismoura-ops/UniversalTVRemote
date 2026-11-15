package com.universalremote.tv.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.net.Socket

class TCPProbe {
    val ports = listOf(3000, 8001, 8002, 8060, 55000, 1925, 1926, 7345, 5555, 8008, 8009, 80, 443)

    suspend fun scanSubnet(localIp: String, timeoutMs: Int = 600): Map<String, Set<Int>> = withContext(Dispatchers.IO) {
        val prefix = localIp.substringBeforeLast('.')
        val semaphore = Semaphore(60)
        val open = java.util.concurrent.ConcurrentHashMap<String, MutableSet<Int>>()
        coroutineScope {
            (1..254).flatMap { host ->
                val ip = "$prefix.$host"
                ports.map { port -> async {
                    semaphore.acquire()
                    try {
                        val isOpen = withTimeoutOrNull(timeoutMs.toLong() + 150) {
                            Socket().use { socket ->
                                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                                true
                            }
                        } ?: false
                        if (isOpen) open.computeIfAbsent(ip) { java.util.concurrent.ConcurrentHashMap.newKeySet() }.add(port)
                    } catch (_: Exception) {
                        Unit
                    } finally {
                        semaphore.release()
                    }
                } }
            }.awaitAll()
        }
        open.mapValues { it.value.toSet() }
    }
}
