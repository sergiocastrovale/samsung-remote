package com.kp.samsungremote.tv

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WakeOnLanTest {
    @Test
    fun `parses colon and dash separated MACs`() {
        val expected = byteArrayOf(0x64, 0x1C, 0xAE.toByte(), 0x12, 0x34, 0xEF.toByte())
        assertArrayEquals(expected, WakeOnLan.parseMac("64:1C:AE:12:34:EF"))
        assertArrayEquals(expected, WakeOnLan.parseMac("64-1c-ae-12-34-ef"))
    }

    @Test
    fun `rejects malformed MACs`() {
        assertNull(WakeOnLan.parseMac(""))
        assertNull(WakeOnLan.parseMac("64:1C:AE:12:34"))
        assertNull(WakeOnLan.parseMac("64:1C:AE:12:34:ZZ"))
        assertNull(WakeOnLan.parseMac("641CAE1234EF"))
    }

    @Test
    fun `magic packet is 6 x FF then 16 MAC repeats`() {
        val mac = WakeOnLan.parseMac("01:02:03:04:05:06")!!
        val packet = WakeOnLan.magicPacket(mac)
        assertEquals(102, packet.size)
        (0 until 6).forEach { assertEquals(0xFF.toByte(), packet[it]) }
        (0 until 16).forEach { repeatIndex ->
            assertArrayEquals(mac, packet.copyOfRange(6 + repeatIndex * 6, 12 + repeatIndex * 6))
        }
    }

    @Test
    fun `broadcast targets include the TV subnet`() {
        assertEquals(listOf("255.255.255.255", "192.168.1.255", "192.168.1.50"), WakeOnLan.broadcastTargets("192.168.1.50"))
        assertEquals(listOf("255.255.255.255", "tv.local"), WakeOnLan.broadcastTargets("tv.local"))
        assertEquals(listOf("255.255.255.255"), WakeOnLan.broadcastTargets(null))
    }
}
