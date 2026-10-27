package com.kp.samsungremote.tv

import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Fallback discovery for networks that drop multicast: probe the TV info port on every host of the subnet. */
object NetworkScan {
    private const val MIN_PREFIX = 22
    private const val FALLBACK_PREFIX = 24

    /** Hosts of [ip]'s subnet, minus network, broadcast and [ip] itself. Wider than /22 shrinks to the /24 around [ip]. */
    fun hostsInSubnet(ip: String, prefix: Int): List<String> {
        val address = toInt(ip) ?: return emptyList()
        val bits = if (prefix < MIN_PREFIX) FALLBACK_PREFIX else prefix
        if (bits >= 31) {
            return emptyList()
        }
        val mask = (-1L shl (32 - bits)).toInt()
        val network = address and mask
        val broadcast = network or mask.inv()
        return ((network.toLong() + 1) until broadcast.toLong())
            .map { it.toInt() }
            .filter { it != address }
            .map(::fromInt)
    }

    fun isPortOpen(host: String, port: Int, timeoutMs: Int): Boolean = runCatching {
        Socket().use { it.connect(InetSocketAddress(host, port), timeoutMs) }
        true
    }.getOrDefault(false)

    /** Blocking; hosts with [port] open. */
    fun scan(hosts: List<String>, port: Int = Payloads.INFO_PORT, timeoutMs: Int = 500, parallelism: Int = 64): List<String> {
        if (hosts.isEmpty()) {
            return emptyList()
        }
        val pool = Executors.newFixedThreadPool(parallelism)
        return try {
            hosts.map { host -> pool.submit<String?> { host.takeIf { isPortOpen(it, port, timeoutMs) } } }
                .mapNotNull { runCatching { it.get(timeoutMs * 4L, TimeUnit.MILLISECONDS) }.getOrNull() }
        } finally {
            pool.shutdownNow()
        }
    }

    private fun toInt(ip: String): Int? {
        val parts = ip.split('.').map { it.toIntOrNull() }
        if (parts.size != 4 || parts.any { it == null || it !in 0..255 }) {
            return null
        }
        return parts.fold(0) { acc, part -> (acc shl 8) or part!! }
    }

    private fun fromInt(value: Int): String = (3 downTo 0).joinToString(".") { ((value shr (it * 8)) and 0xFF).toString() }
}
