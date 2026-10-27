package com.kp.samsungremote.tv

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/** Powers the TV on from standby. Needs "Power On with Mobile" enabled on the TV. */
object WakeOnLan {
    private val PORTS = listOf(9, 7)
    private const val REPEATS = 3

    fun parseMac(mac: String): ByteArray? {
        val parts = mac.trim().split(':', '-')
        if (parts.size != 6 || parts.any { it.length != 2 }) {
            return null
        }
        return runCatching { ByteArray(6) { parts[it].toInt(16).toByte() } }.getOrNull()
    }

    /** 6 x 0xFF followed by the MAC 16 times (102 bytes). */
    fun magicPacket(mac: ByteArray): ByteArray {
        require(mac.size == 6) { "MAC must be 6 bytes" }
        return ByteArray(6) { 0xFF.toByte() } + ByteArray(16 * 6) { mac[it % 6] }
    }

    /** Targets the limited broadcast plus the TV's /24 broadcast (some routers drop the former). */
    fun broadcastTargets(tvHost: String?): List<String> {
        val subnet = tvHost?.split('.')?.takeIf { it.size == 4 && it.all { p -> p.toIntOrNull() in 0..255 } }
        return listOfNotNull("255.255.255.255", subnet?.let { "${it[0]}.${it[1]}.${it[2]}.255" }, tvHost)
    }

    /** Blocking. */
    fun send(mac: String, tvHost: String?) {
        val bytes = magicPacket(parseMac(mac) ?: throw IllegalArgumentException("Invalid MAC address: $mac"))
        DatagramSocket().use { socket ->
            socket.broadcast = true
            repeat(REPEATS) {
                broadcastTargets(tvHost).forEach { target ->
                    val address = InetAddress.getByName(target)
                    PORTS.forEach { port -> runCatching { socket.send(DatagramPacket(bytes, bytes.size, address, port)) } }
                }
                Thread.sleep(100)
            }
        }
    }
}
