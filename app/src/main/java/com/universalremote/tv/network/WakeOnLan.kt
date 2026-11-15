package com.universalremote.tv.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object WakeOnLan {
    suspend fun send(macAddress: String, broadcast: String = "255.255.255.255"): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val clean = macAddress.replace(":", "").replace("-", "").trim()
            require(clean.length == 12 && clean.all { it.digitToIntOrNull(16) != null }) { "MAC address is invalid" }
            val mac = ByteArray(6) { i -> clean.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
            val packet = ByteArray(6 + 16 * mac.size)
            for (i in 0 until 6) packet[i] = 0xFF.toByte()
            repeat(16) { repeat(6) { j -> packet[6 + it * 6 + j] = mac[j] } }
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.send(DatagramPacket(packet, packet.size, InetAddress.getByName(broadcast), 9))
            }
        }
    }
}
