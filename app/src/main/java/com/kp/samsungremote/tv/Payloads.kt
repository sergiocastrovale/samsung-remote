package com.kp.samsungremote.tv

import org.json.JSONObject
import java.net.URLEncoder
import java.util.Base64

/** Wire format of the Tizen remote-control WebSocket (2016+ TVs, port 8002 over TLS). */
object Payloads {
    const val CONTROL_PORT = 8002
    const val INFO_PORT = 8001

    enum class KeyAction(val cmd: String) { CLICK("Click"), PRESS("Press"), RELEASE("Release") }

    fun controlUrl(host: String, remoteName: String, token: String?): String {
        val name = URLEncoder.encode(base64(remoteName), "UTF-8")
        val base = "wss://$host:$CONTROL_PORT/api/v2/channels/samsung.remote.control?name=$name"
        return if (token.isNullOrBlank()) base else "$base&token=${URLEncoder.encode(token, "UTF-8")}"
    }

    fun infoUrl(host: String): String = "http://$host:$INFO_PORT/api/v2/"

    fun key(code: String, action: KeyAction = KeyAction.CLICK): String = remoteControl(
        JSONObject()
            .put("Cmd", action.cmd)
            .put("DataOfCmd", code)
            .put("Option", "false")
            .put("TypeOfRemote", "SendRemoteKey"),
    )

    /** Types into the TV's on-screen keyboard; ignored when no text field has focus. */
    fun text(value: String): String = remoteControl(
        JSONObject()
            .put("Cmd", base64(value))
            .put("DataOfCmd", "base64")
            .put("TypeOfRemote", "SendInputString"),
    )

    sealed interface Event {
        /** Channel accepted; [token] is only present when the TV issued a new one. */
        data class Connected(val token: String?) : Event
        data object Unauthorized : Event
        data object TimedOut : Event
        data class Other(val name: String) : Event
    }

    fun parseEvent(message: String): Event? {
        val json = runCatching { JSONObject(message) }.getOrNull() ?: return null
        return when (val name = json.optString("event")) {
            "ms.channel.connect" -> Event.Connected(
                json.optJSONObject("data")?.optString("token")?.takeIf { it.isNotBlank() },
            )
            "ms.channel.unauthorized" -> Event.Unauthorized
            "ms.channel.timeOut" -> Event.TimedOut
            "" -> null
            else -> Event.Other(name)
        }
    }

    private fun remoteControl(params: JSONObject): String =
        JSONObject().put("method", "ms.remote.control").put("params", params).toString()

    private fun base64(value: String): String = Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
}
