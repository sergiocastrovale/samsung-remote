package com.kp.samsungremote.tv

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TvInfo(
    val name: String,
    val model: String,
    val mac: String?,
    /** "on" or "standby"; null on firmware that does not report it. */
    val powerState: String?,
) {
    val isOn: Boolean get() = powerState == null || powerState.equals("on", ignoreCase = true)

    companion object {
        private val http = OkHttpClient.Builder()
            .connectTimeout(1500, TimeUnit.MILLISECONDS)
            .readTimeout(1500, TimeUnit.MILLISECONDS)
            .build()

        fun parse(body: String): TvInfo? {
            val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
            val device = json.optJSONObject("device") ?: return null
            return TvInfo(
                name = device.optString("name").ifBlank { json.optString("name") }.ifBlank { "Samsung TV" },
                model = device.optString("modelName"),
                mac = device.optString("wifiMac").takeIf { WakeOnLan.parseMac(it) != null },
                powerState = device.optString("PowerState").ifBlank { null },
            )
        }

        /** Blocking; null when the TV does not answer (off, wrong IP, other network). */
        fun fetch(host: String): TvInfo? = runCatching {
            http.newCall(Request.Builder().url(Payloads.infoUrl(host)).build()).execute().use { response ->
                if (response.isSuccessful) parse(response.body?.string().orEmpty()) else null
            }
        }.getOrNull()
    }
}
