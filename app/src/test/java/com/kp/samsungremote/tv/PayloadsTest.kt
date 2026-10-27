package com.kp.samsungremote.tv

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class PayloadsTest {
    @Test
    fun `control url encodes the base64 remote name`() {
        val url = Payloads.controlUrl("192.168.1.50", "Android Remote", null)
        assertEquals("wss://192.168.1.50:8002/api/v2/channels/samsung.remote.control?name=QW5kcm9pZCBSZW1vdGU%3D", url)
    }

    @Test
    fun `control url carries the token once paired`() {
        val url = Payloads.controlUrl("10.0.0.2", "R", "12345678")
        assertEquals(true, url.endsWith("&token=12345678"))
    }

    @Test
    fun `key click payload`() {
        val json = JSONObject(Payloads.key(TvKey.VOL_UP))
        assertEquals("ms.remote.control", json.getString("method"))
        val params = json.getJSONObject("params")
        assertEquals("Click", params.getString("Cmd"))
        assertEquals("KEY_VOLUP", params.getString("DataOfCmd"))
        assertEquals("false", params.getString("Option"))
        assertEquals("SendRemoteKey", params.getString("TypeOfRemote"))
    }

    @Test
    fun `key press and release payloads`() {
        assertEquals("Press", JSONObject(Payloads.key(TvKey.POWER, Payloads.KeyAction.PRESS)).getJSONObject("params").getString("Cmd"))
        assertEquals("Release", JSONObject(Payloads.key(TvKey.POWER, Payloads.KeyAction.RELEASE)).getJSONObject("params").getString("Cmd"))
    }

    @Test
    fun `text payload is base64`() {
        val params = JSONObject(Payloads.text("Café")).getJSONObject("params")
        assertEquals("SendInputString", params.getString("TypeOfRemote"))
        assertEquals("base64", params.getString("DataOfCmd"))
        assertEquals("Café", String(Base64.getDecoder().decode(params.getString("Cmd")), Charsets.UTF_8))
    }

    @Test
    fun `connect event with new token`() {
        val event = Payloads.parseEvent("""{"data":{"clients":[],"id":"abc","token":"98765432"},"event":"ms.channel.connect"}""")
        assertEquals(Payloads.Event.Connected("98765432"), event)
    }

    @Test
    fun `connect event without token`() {
        val event = Payloads.parseEvent("""{"data":{"clients":[],"id":"abc"},"event":"ms.channel.connect"}""")
        assertEquals(Payloads.Event.Connected(null), event)
    }

    @Test
    fun `unauthorized and timeout events`() {
        assertEquals(Payloads.Event.Unauthorized, Payloads.parseEvent("""{"event":"ms.channel.unauthorized"}"""))
        assertEquals(Payloads.Event.TimedOut, Payloads.parseEvent("""{"event":"ms.channel.timeOut"}"""))
    }

    @Test
    fun `garbage is ignored`() {
        assertNull(Payloads.parseEvent("not json"))
        assertNull(Payloads.parseEvent("{}"))
    }
}
