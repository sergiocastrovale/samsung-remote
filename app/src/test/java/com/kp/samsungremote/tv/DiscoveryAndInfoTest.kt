package com.kp.samsungremote.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryAndInfoTest {
    private val ssdpReply = listOf(
        "HTTP/1.1 200 OK",
        "CACHE-CONTROL: max-age=1800",
        "LOCATION: http://192.168.1.50:7676/rcr/",
        "ST: urn:samsung.com:device:RemoteControlReceiver:1",
        "SERVER: SHP, UPnP/1.0, Samsung UPnP SDK/1.0",
        "",
        "",
    ).joinToString("\r\n")

    @Test
    fun `search messages cover several device types`() {
        assertTrue(SsdpDiscovery.SEARCH_TARGETS.contains("urn:samsung.com:device:RemoteControlReceiver:1"))
        assertTrue(SsdpDiscovery.SEARCH_TARGETS.contains("ssdp:all"))
        val message = SsdpDiscovery.searchMessage("ssdp:all")
        assertTrue(message.startsWith("M-SEARCH * HTTP/1.1\r\n"))
        assertTrue(message.contains("ST: ssdp:all\r\n"))
        assertTrue(message.endsWith("\r\n\r\n"))
    }

    @Test
    fun `parses SSDP headers case-insensitively`() {
        val headers = SsdpDiscovery.parseHeaders(ssdpReply.replace("LOCATION", "Location"))
        assertEquals("http://192.168.1.50:7676/rcr/", headers["LOCATION"])
        assertEquals("urn:samsung.com:device:RemoteControlReceiver:1", headers["ST"])
    }

    @Test
    fun `subnet hosts skip network, broadcast and self`() {
        val hosts = NetworkScan.hostsInSubnet("192.168.1.23", 24)
        assertEquals(253, hosts.size)
        assertEquals("192.168.1.1", hosts.first())
        assertEquals("192.168.1.254", hosts.last())
        assertFalse(hosts.contains("192.168.1.23"))
    }

    @Test
    fun `wide subnets shrink to the surrounding 24`() {
        val hosts = NetworkScan.hostsInSubnet("10.20.30.40", 16)
        assertEquals(253, hosts.size)
        assertEquals("10.20.30.1", hosts.first())
    }

    @Test
    fun `22 subnet is scanned whole`() {
        val hosts = NetworkScan.hostsInSubnet("192.168.4.10", 22)
        assertEquals(1021, hosts.size)
        assertEquals("192.168.4.1", hosts.first())
        assertEquals("192.168.7.254", hosts.last())
    }

    @Test
    fun `bad addresses scan nothing`() {
        assertTrue(NetworkScan.hostsInSubnet("nope", 24).isEmpty())
        assertTrue(NetworkScan.hostsInSubnet("192.168.1.1", 31).isEmpty())
    }

    @Test
    fun `parses TV info`() {
        val body = """
            {"device":{"PowerState":"on","modelName":"UE55AU9005KXXC","name":"[TV] Samsung AU9005 55",
            "wifiMac":"64:1c:ae:12:34:ef","networkType":"wireless"},"name":"[TV] Samsung AU9005 55","type":"Samsung SmartTV"}
        """.trimIndent()
        val info = TvInfo.parse(body)!!
        assertEquals("[TV] Samsung AU9005 55", info.name)
        assertEquals("UE55AU9005KXXC", info.model)
        assertEquals("64:1c:ae:12:34:ef", info.mac)
        assertTrue(info.isOn)
    }

    @Test
    fun `standby TV is not on, missing power state counts as on`() {
        assertFalse(TvInfo.parse("""{"device":{"PowerState":"standby","name":"TV"}}""")!!.isOn)
        assertTrue(TvInfo.parse("""{"device":{"name":"TV"}}""")!!.isOn)
    }

    @Test
    fun `bad info bodies`() {
        assertNull(TvInfo.parse("oops"))
        assertNull(TvInfo.parse("{}"))
        assertNull(TvInfo.parse("""{"device":{"wifiMac":"nope"}}""")!!.mac)
    }
}
