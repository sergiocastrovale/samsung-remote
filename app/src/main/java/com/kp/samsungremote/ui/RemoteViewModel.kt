package com.kp.samsungremote.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kp.samsungremote.data.Prefs
import com.kp.samsungremote.tv.ConnectionState
import com.kp.samsungremote.tv.NetworkScan
import com.kp.samsungremote.tv.Payloads
import com.kp.samsungremote.tv.SamsungTvClient
import com.kp.samsungremote.tv.SsdpDiscovery
import com.kp.samsungremote.tv.TvInfo
import com.kp.samsungremote.tv.TvKey
import com.kp.samsungremote.tv.WakeOnLan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address

data class DiscoveredTv(val host: String, val name: String, val model: String)

data class SetupUi(
    val host: String = "",
    val tvName: String? = null,
    val model: String? = null,
    val mac: String? = null,
    val powerState: String? = null,
    val paired: Boolean = false,
    val volumeKeys: Boolean = true,
    val discovering: Boolean = false,
    /** This phone's IPv4/prefix on the LAN, shown so a TV on another subnet is easy to spot. */
    val phoneAddress: String? = null,
    val discovered: List<DiscoveredTv> = emptyList(),
)

class RemoteViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val client = SamsungTvClient(REMOTE_NAME) { token ->
        prefs.token = token
        _setup.update { it.copy(paired = true) }
    }
    private val _setup = MutableStateFlow(
        SetupUi(
            host = prefs.host.orEmpty(),
            tvName = prefs.tvName,
            mac = prefs.mac,
            paired = prefs.token != null,
            volumeKeys = prefs.volumeKeys,
        ),
    )
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    private val _playing = MutableStateFlow(true)

    val connection: StateFlow<ConnectionState> = client.state
    val setup: StateFlow<SetupUi> = _setup.asStateFlow()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Last play state sent; the TV does not report it, so the toggle assumes its own last press. */
    val playing: StateFlow<Boolean> = _playing.asStateFlow()
    val hasHost: Boolean get() = !prefs.host.isNullOrBlank()

    init {
        client.configure(prefs.host, prefs.token)
    }

    fun onStart() {
        if (hasHost) {
            client.connect()
            refreshInfo()
        }
    }

    fun onStop() = client.disconnect()

    fun key(code: String) {
        when (code) {
            TvKey.PLAY -> _playing.value = true
            TvKey.PAUSE -> _playing.value = false
        }
        client.send(Payloads.key(code))
    }

    fun playPause() = key(if (_playing.value) TvKey.PAUSE else TvKey.PLAY)

    fun keyDown(code: String) = client.send(Payloads.key(code, Payloads.KeyAction.PRESS))

    fun keyUp(code: String) = client.send(Payloads.key(code, Payloads.KeyAction.RELEASE))

    fun text(value: String) {
        if (value.isNotEmpty()) {
            client.send(Payloads.text(value))
        }
    }

    /** Like the physical button: off when the TV answers as on, Wake-on-LAN otherwise. */
    fun power() {
        val host = prefs.host ?: return say("Set up the TV first")
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) { TvInfo.fetch(host) }
            info?.let(::remember)
            if (info?.isOn == true) {
                key(TvKey.POWER)
            } else {
                wake()
            }
        }
    }

    fun wake() {
        val host = prefs.host ?: return say("Set up the TV first")
        val mac = prefs.mac ?: return say("TV MAC unknown: connect once while the TV is on")
        viewModelScope.launch {
            val sent = withContext(Dispatchers.IO) { runCatching { WakeOnLan.send(mac, host) } }
            if (sent.isFailure) {
                return@launch say("Wake-on-LAN failed: ${sent.exceptionOrNull()?.message}")
            }
            say("Waking up the TV...")
            repeat(WAKE_POLLS) {
                delay(WAKE_POLL_MS)
                val info = withContext(Dispatchers.IO) { TvInfo.fetch(host) }
                if (info?.isOn == true) {
                    remember(info)
                    client.disconnect()
                    client.connect()
                    return@launch
                }
            }
            say("TV did not wake up. Is \"Power On with Mobile\" enabled?")
        }
    }

    fun selectHost(raw: String) {
        val host = raw.trim()
        if (host.isEmpty()) {
            return say("Enter the TV's IP address")
        }
        if (host != prefs.host) {
            prefs.host = host
            prefs.token = null
            prefs.mac = null
            prefs.tvName = null
            _setup.update { it.copy(host = host, paired = false, mac = null, tvName = null, model = null, powerState = null) }
        }
        client.disconnect()
        client.configure(host, prefs.token)
        client.connect()
        refreshInfo()
    }

    fun reconnect() {
        client.disconnect()
        client.connect()
        refreshInfo()
    }

    fun forgetPairing() {
        prefs.token = null
        _setup.update { it.copy(paired = false) }
        client.disconnect()
        client.configure(prefs.host, null)
        say("Pairing removed. Reconnect and press Allow on the TV")
    }

    fun setVolumeKeys(enabled: Boolean) {
        prefs.volumeKeys = enabled
        _setup.update { it.copy(volumeKeys = enabled) }
    }

    fun discover() {
        if (_setup.value.discovering) {
            return
        }
        val local = localAddress()
        _setup.update { it.copy(discovering = true, discovered = emptyList(), phoneAddress = local?.let { (ip, prefix) -> "$ip/$prefix" }) }
        viewModelScope.launch {
            val wifi = getApplication<Application>().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val lock = wifi.createMulticastLock("ssdp").apply {
                setReferenceCounted(false)
                acquire()
            }
            val found = try {
                withContext(Dispatchers.IO) { findTvs(local) }
            } finally {
                lock.release()
            }
            _setup.update { it.copy(discovering = false, discovered = found) }
            if (found.isEmpty()) {
                say("No TV found${local?.let { (ip, prefix) -> " on $ip/$prefix" }.orEmpty()}. Type the TV's IP instead")
            }
        }
    }

    /** SSDP and a subnet scan side by side; a host only counts once the TV info endpoint answers. */
    private suspend fun findTvs(local: Pair<String, Int>?): List<DiscoveredTv> = coroutineScope {
        val ssdp = async { runCatching { SsdpDiscovery.search() }.getOrDefault(emptyList()) }
        val scan = async { local?.let { (ip, prefix) -> NetworkScan.scan(NetworkScan.hostsInSubnet(ip, prefix)) }.orEmpty() }
        (ssdp.await() + scan.await())
            .distinct()
            .map { host -> async { TvInfo.fetch(host)?.let { DiscoveredTv(host, it.name, it.model) } } }
            .awaitAll()
            .filterNotNull()
            .sortedBy { it.host }
    }

    /** IPv4 address and prefix of the phone's Wi-Fi (or Ethernet) network. */
    @Suppress("DEPRECATION")
    private fun localAddress(): Pair<String, Int>? {
        val cm = getApplication<Application>().getSystemService(ConnectivityManager::class.java) ?: return null
        val lan = (listOfNotNull(cm.activeNetwork) + cm.allNetworks).firstOrNull { network ->
            cm.getNetworkCapabilities(network)?.let {
                it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } == true
        } ?: return null
        val link = cm.getLinkProperties(lan)?.linkAddresses?.firstOrNull { it.address is Inet4Address } ?: return null
        val ip = link.address.hostAddress ?: return null
        return ip to link.prefixLength
    }

    private fun refreshInfo() {
        val host = prefs.host ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { TvInfo.fetch(host) }?.let(::remember)
        }
    }

    private fun remember(info: TvInfo) {
        info.mac?.let { prefs.mac = it }
        prefs.tvName = info.name
        _setup.update {
            it.copy(tvName = info.name, model = info.model, mac = info.mac ?: it.mac, powerState = info.powerState)
        }
    }

    private fun say(message: String) {
        _messages.tryEmit(message)
    }

    private companion object {
        const val REMOTE_NAME = "Android Remote"
        const val WAKE_POLLS = 10
        const val WAKE_POLL_MS = 1500L
    }
}
