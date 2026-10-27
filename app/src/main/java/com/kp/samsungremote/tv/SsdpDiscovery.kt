package com.kp.samsungremote.tv

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

/**
 * Asks the LAN who is there through SSDP M-SEARCH. Tizen models differ in which device types they
 * advertise, so several are searched and every responder is returned; the caller confirms which
 * ones are Samsung TVs through [TvInfo].
 */
object SsdpDiscovery {
    private const val MULTICAST_HOST = "239.255.255.250"
    private const val MULTICAST_PORT = 1900

    val SEARCH_TARGETS = listOf(
        "urn:samsung.com:device:RemoteControlReceiver:1",
        "urn:dial-multiscreen-org:service:dial:1",
        "urn:schemas-upnp-org:device:MediaRenderer:1",
        "ssdp:all",
    )

    fun searchMessage(target: String): String = listOf(
        "M-SEARCH * HTTP/1.1",
        "HOST: $MULTICAST_HOST:$MULTICAST_PORT",
        "MAN: \"ssdp:discover\"",
        "MX: 2",
        "ST: $target",
        "",
        "",
    ).joinToString("\r\n")

    fun parseHeaders(response: String): Map<String, String> = response.lineSequence()
        .drop(1)
        .mapNotNull { line ->
            val colon = line.indexOf(':')
            if (colon <= 0) null else line.substring(0, colon).trim().uppercase() to line.substring(colon + 1).trim()
        }
        .toMap()

    /** Blocking; IPs of every device that answered within [timeoutMs]. */
    fun search(timeoutMs: Int = 2500): List<String> {
        val found = linkedSetOf<String>()
        DatagramSocket().use { socket ->
            socket.soTimeout = 300
            val group = InetAddress.getByName(MULTICAST_HOST)
            repeat(2) {
                SEARCH_TARGETS.forEach { target ->
                    val bytes = searchMessage(target).toByteArray()
                    socket.send(DatagramPacket(bytes, bytes.size, group, MULTICAST_PORT))
                }
            }
            val deadline = System.currentTimeMillis() + timeoutMs
            val buffer = ByteArray(2048)
            while (System.currentTimeMillis() < deadline) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                } catch (_: SocketTimeoutException) {
                    continue
                }
                packet.address.hostAddress?.let { found += it }
            }
        }
        return found.toList()
    }
}
