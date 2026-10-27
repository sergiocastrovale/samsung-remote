package com.kp.samsungremote.data

import android.content.Context

class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("remote", Context.MODE_PRIVATE)

    var host: String?
        get() = prefs.getString(KEY_HOST, null)
        set(value) = prefs.edit().putString(KEY_HOST, value).apply()

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var mac: String?
        get() = prefs.getString(KEY_MAC, null)
        set(value) = prefs.edit().putString(KEY_MAC, value).apply()

    var tvName: String?
        get() = prefs.getString(KEY_TV_NAME, null)
        set(value) = prefs.edit().putString(KEY_TV_NAME, value).apply()

    var volumeKeys: Boolean
        get() = prefs.getBoolean(KEY_VOLUME_KEYS, true)
        set(value) = prefs.edit().putBoolean(KEY_VOLUME_KEYS, value).apply()

    private companion object {
        const val KEY_HOST = "host"
        const val KEY_TOKEN = "token"
        const val KEY_MAC = "mac"
        const val KEY_TV_NAME = "tvName"
        const val KEY_VOLUME_KEYS = "volumeKeys"
    }
}
