package com.kp.samsungremote

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.border
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kp.samsungremote.tv.ConnectionState
import com.kp.samsungremote.tv.TvKey
import com.kp.samsungremote.ui.KeysScreen
import com.kp.samsungremote.ui.RemoteScreen
import com.kp.samsungremote.ui.RemoteViewModel
import com.kp.samsungremote.ui.SetupScreen
import com.kp.samsungremote.ui.describe
import com.kp.samsungremote.ui.theme.RemoteColors
import com.kp.samsungremote.ui.theme.RemoteTheme

class MainActivity : ComponentActivity() {
    private val vm: RemoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RemoteTheme {
                App(vm)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.onStart()
    }

    override fun onStop() {
        super.onStop()
        vm.onStop()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> TvKey.VOL_UP
            KeyEvent.KEYCODE_VOLUME_DOWN -> TvKey.VOL_DOWN
            KeyEvent.KEYCODE_VOLUME_MUTE -> TvKey.MUTE
            else -> null
        }
        if (code == null || !vm.setup.value.volumeKeys || !vm.hasHost) {
            return super.dispatchKeyEvent(event)
        }
        if (event.action == KeyEvent.ACTION_DOWN) {
            vm.key(code)
        }
        return true
    }
}

private enum class Tab(val label: String) { REMOTE("Remote"), KEYS("More keys"), SETUP("Setup") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(vm: RemoteViewModel) {
    val connection by vm.connection.collectAsStateWithLifecycle()
    val setup by vm.setup.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(if (vm.hasHost) Tab.REMOTE.ordinal else Tab.SETUP.ordinal) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    Box(Modifier.fillMaxSize().background(RemoteColors.background)) {
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                title = { StatusPill(setup.tvName ?: "Samsung Remote", connection) },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f), tonalElevation = 0.dp) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item.ordinal,
                        onClick = { tab = item.ordinal },
                        label = { Text(item.label) },
                        icon = {
                            Icon(
                                when (item) {
                                    Tab.REMOTE -> Icons.Filled.SettingsRemote
                                    Tab.KEYS -> Icons.Filled.Dialpad
                                    Tab.SETUP -> Icons.Filled.Wifi
                                },
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val content = Modifier.padding(padding)
        when (Tab.entries[tab]) {
            Tab.REMOTE -> RemoteScreen(vm, content)
            Tab.KEYS -> KeysScreen(vm, content)
            Tab.SETUP -> SetupScreen(vm, content)
        }
        if (connection == ConnectionState.WaitingForAllow) {
            LaunchedEffect(Unit) { snackbar.showSnackbar(describe(connection)) }
        }
    }
    }
}

@Composable
private fun StatusPill(name: String, connection: ConnectionState) {
    val dot = when (connection) {
        ConnectionState.Connected -> RemoteColors.Online
        ConnectionState.Connecting, ConnectionState.WaitingForAllow -> RemoteColors.Pending
        else -> RemoteColors.Offline
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box(
            Modifier
                .size(18.dp)
                .background(dot.copy(alpha = 0.22f), CircleShape)
                .padding(5.dp)
                .background(dot, CircleShape),
        )
        Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        Text(
            if (connection == ConnectionState.Connected) "Connected" else describe(connection).substringBefore(" ("),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            maxLines = 1,
        )
    }
}
