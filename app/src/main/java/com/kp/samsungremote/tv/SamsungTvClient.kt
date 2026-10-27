package com.kp.samsungremote.tv

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Connecting : ConnectionState
    /** Socket open, TV is showing the "Allow" prompt. */
    data object WaitingForAllow : ConnectionState
    data object Connected : ConnectionState
    data object Denied : ConnectionState
    data class Failed(val message: String) : ConnectionState
}

/**
 * One WebSocket to the TV's remote-control channel. Keys pressed while disconnected are queued
 * for a few seconds and flushed once the channel connects, so a press after the TV wakes up is
 * not lost.
 */
class SamsungTvClient(
    private val remoteName: String,
    private val onToken: (String) -> Unit,
) {
    private val lock = Any()
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private var socket: WebSocket? = null
    private var generation = 0
    private var host: String? = null
    private var token: String? = null
    private val pending = ArrayDeque<Pair<Long, String>>()

    // The TV serves a self-signed certificate for its LAN IP, so this client (and only this one) trusts it.
    private val http: OkHttpClient = run {
        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val ssl = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trustAll), SecureRandom()) }
        OkHttpClient.Builder()
            .sslSocketFactory(ssl.socketFactory, trustAll)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }

    fun configure(host: String?, token: String?) = synchronized(lock) {
        val changed = host != this.host
        this.host = host
        this.token = token
        if (changed) {
            closeLocked()
        }
    }

    fun connect() = synchronized(lock) {
        val target = host ?: return@synchronized
        if (socket != null) {
            return@synchronized
        }
        val myGeneration = ++generation
        _state.value = ConnectionState.Connecting
        val request = Request.Builder().url(Payloads.controlUrl(target, remoteName, token)).build()
        socket = http.newWebSocket(request, Listener(myGeneration))
    }

    fun disconnect() = synchronized(lock) {
        closeLocked()
        pending.clear()
        _state.value = ConnectionState.Idle
    }

    fun send(message: String) = synchronized(lock) {
        if (_state.value == ConnectionState.Connected && socket?.send(message) == true) {
            return@synchronized
        }
        pending.addLast(System.currentTimeMillis() to message)
        while (pending.size > MAX_PENDING) {
            pending.removeFirst()
        }
        if (socket == null) {
            connect()
        }
    }

    private fun closeLocked() {
        generation++
        socket?.close(1000, null)
        socket = null
    }

    private fun flushLocked() {
        val cutoff = System.currentTimeMillis() - PENDING_TTL_MS
        while (pending.isNotEmpty()) {
            val (time, message) = pending.removeFirst()
            if (time >= cutoff) {
                socket?.send(message)
            }
        }
    }

    private inner class Listener(private val myGeneration: Int) : WebSocketListener() {
        private fun current(): Boolean = myGeneration == generation

        override fun onOpen(webSocket: WebSocket, response: Response) = synchronized(lock) {
            if (current() && token.isNullOrBlank()) {
                _state.value = ConnectionState.WaitingForAllow
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) = synchronized(lock) {
            if (!current()) {
                return@synchronized
            }
            when (val event = Payloads.parseEvent(text)) {
                is Payloads.Event.Connected -> {
                    event.token?.let {
                        token = it
                        onToken(it)
                    }
                    _state.value = ConnectionState.Connected
                    flushLocked()
                }
                Payloads.Event.Unauthorized -> {
                    _state.value = ConnectionState.Denied
                    closeLocked()
                }
                Payloads.Event.TimedOut -> {
                    _state.value = ConnectionState.Failed("No answer to the pairing prompt on the TV")
                    closeLocked()
                }
                else -> Unit
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = synchronized(lock) {
            if (current()) {
                socket = null
                _state.value = ConnectionState.Idle
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = synchronized(lock) {
            if (current()) {
                socket = null
                pending.clear()
                _state.value = ConnectionState.Failed(t.message ?: t.javaClass.simpleName)
            }
        }
    }

    private companion object {
        const val MAX_PENDING = 8
        const val PENDING_TTL_MS = 8_000L
    }
}
