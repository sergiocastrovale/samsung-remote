package com.kp.samsungremote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kp.samsungremote.tv.ConnectionState

@Composable
fun SetupScreen(vm: RemoteViewModel, modifier: Modifier = Modifier) {
    val setup by vm.setup.collectAsStateWithLifecycle()
    val connection by vm.connection.collectAsStateWithLifecycle()
    var ip by rememberSaveable(setup.host) { mutableStateOf(setup.host) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(setup.tvName ?: "No TV selected", style = MaterialTheme.typography.titleMedium)
                InfoLine("Status", describe(connection))
                InfoLine("IP address", setup.host.ifBlank { "-" })
                setup.model?.takeIf { it.isNotBlank() }?.let { InfoLine("Model", it) }
                setup.powerState?.let { InfoLine("Power", it) }
                InfoLine("MAC (power on)", setup.mac ?: "unknown until first connection")
                InfoLine("Paired", if (setup.paired) "yes" else "no")
            }
        }

        Text("TV address", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = ip,
                onValueChange = { ip = it },
                singleLine = true,
                label = { Text("IP, e.g. 192.168.1.50") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.selectHost(ip) }),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { vm.selectHost(ip) }) { Text("Connect") }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = vm::discover, enabled = !setup.discovering) { Text("Find TVs on Wi-Fi") }
            if (setup.discovering) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
        setup.phoneAddress?.let {
            Text(
                "This phone: $it. The TV's IP must start the same way (same subnet).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        setup.discovered.forEach { tv ->
            Card(modifier = Modifier.fillMaxWidth().clickable { vm.selectHost(tv.host) }) {
                Column(Modifier.padding(12.dp)) {
                    Text(tv.name, fontWeight = FontWeight.SemiBold)
                    Text(listOf(tv.host, tv.model).filter { it.isNotBlank() }.joinToString("  ·  "), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        HorizontalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = vm::reconnect, enabled = setup.host.isNotBlank()) { Text("Reconnect") }
            OutlinedButton(onClick = vm::wake, enabled = setup.mac != null) { Text("Power on") }
            OutlinedButton(onClick = vm::forgetPairing, enabled = setup.paired) { Text("Forget pairing") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Phone volume buttons", fontWeight = FontWeight.SemiBold)
                Text("Change TV volume while the app is open", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = setup.volumeKeys, onCheckedChange = vm::setVolumeKeys)
        }

        HorizontalDivider()
        Text("First time", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "1. Phone and TV on the same network, TV switched on.\n" +
                "2. Find the TV or type its IP (TV: Settings > General > Network > Network Status > IP Settings).\n" +
                "3. Press Allow on the TV when it asks about \"Android Remote\". The pairing is remembered.\n" +
                "4. To switch the TV on from the app, enable Settings > General > Network > Expert Settings > Power On with Mobile.\n" +
                "5. Denied by mistake? TV: Settings > General > External Device Manager > Device Connection Manager > Device List, remove the entry, then Reconnect.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoLine(label: String, value: String) = Row {
    Text("$label: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.bodySmall)
}

fun describe(state: ConnectionState): String = when (state) {
    ConnectionState.Idle -> "Disconnected"
    ConnectionState.Connecting -> "Connecting..."
    ConnectionState.WaitingForAllow -> "Press Allow on the TV"
    ConnectionState.Connected -> "Connected"
    ConnectionState.Denied -> "Denied on the TV"
    is ConnectionState.Failed -> "Not reachable (${state.message})"
}
