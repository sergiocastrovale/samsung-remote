package com.kp.samsungremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kp.samsungremote.tv.TvKey
import com.kp.samsungremote.ui.components.TextKey
import com.kp.samsungremote.ui.theme.RemoteColors

private data class KeySpec(
    val label: String,
    val code: String,
    val color: Color? = null,
    val textColor: Color = Color.White,
    val repeat: Boolean = false,
)

private val numberKeys = (1..9).map { KeySpec("$it", TvKey.DIGITS.getValue(it)) } +
    listOf(KeySpec("-", TvKey.DASH), KeySpec("0", TvKey.DIGITS.getValue(0)), KeySpec("PRE-CH", TvKey.PREV_CH))

private val colorKeys = listOf(
    KeySpec("A", TvKey.RED, RemoteColors.Red),
    KeySpec("B", TvKey.GREEN, RemoteColors.Green),
    KeySpec("C", TvKey.YELLOW, RemoteColors.Yellow, Color.Black),
    KeySpec("D", TvKey.BLUE, RemoteColors.Blue),
)

private val playbackKeys = listOf(
    KeySpec("Rew", TvKey.REWIND),
    KeySpec("Play", TvKey.PLAY),
    KeySpec("Pause", TvKey.PAUSE),
    KeySpec("FF", TvKey.FAST_FORWARD),
    KeySpec("Stop", TvKey.STOP),
    KeySpec("Rec", TvKey.RECORD, RemoteColors.Red),
)

private val menuKeys = listOf(
    KeySpec("Home", TvKey.HOME),
    KeySpec("Settings", TvKey.MENU),
    KeySpec("Tools", TvKey.TOOLS),
    KeySpec("Guide", TvKey.GUIDE),
    KeySpec("Info", TvKey.INFO),
    KeySpec("Ch list", TvKey.CH_LIST),
    KeySpec("E-Manual", TvKey.E_MANUAL),
    KeySpec("123", TvKey.NUMPAD),
    KeySpec("Return", TvKey.RETURN),
    KeySpec("Exit", TvKey.EXIT),
    KeySpec("Vol +", TvKey.VOL_UP, repeat = true),
    KeySpec("Vol -", TvKey.VOL_DOWN, repeat = true),
    KeySpec("Mute", TvKey.MUTE),
    KeySpec("Ch +", TvKey.CH_UP, repeat = true),
    KeySpec("Ch -", TvKey.CH_DOWN, repeat = true),
)

private val inputKeys = listOf(
    KeySpec("Source", TvKey.SOURCE),
    KeySpec("TV", TvKey.TV),
    KeySpec("HDMI", TvKey.HDMI),
    KeySpec("Ambient", TvKey.AMBIENT),
    KeySpec("HDMI 1", TvKey.HDMI1),
    KeySpec("HDMI 2", TvKey.HDMI2),
    KeySpec("HDMI 3", TvKey.HDMI3),
    KeySpec("HDMI 4", TvKey.HDMI4),
)

private val pictureKeys = listOf(
    KeySpec("Subtitle", TvKey.SUBTITLE),
    KeySpec("AD", TvKey.AUDIO_DESCRIPTION),
    KeySpec("Pic size", TvKey.PICTURE_SIZE),
    KeySpec("Pic mode", TvKey.PICTURE_MODE),
    KeySpec("Sleep", TvKey.SLEEP),
    KeySpec("Teletext", TvKey.TELETEXT),
)

@Composable
fun KeysScreen(vm: RemoteViewModel, modifier: Modifier = Modifier) {
    var typed by rememberSaveable { mutableStateOf("") }
    var custom by rememberSaveable { mutableStateOf("KEY_") }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Section("Numbers")
        KeyGrid(numberKeys, columns = 3, vm, keyHeight = 56)
        Section("Colour keys")
        KeyGrid(colorKeys, columns = 4, vm)
        Section("Playback")
        KeyGrid(playbackKeys, columns = 3, vm)
        Section("Menus & navigation")
        KeyGrid(menuKeys, columns = 3, vm)
        Section("Inputs")
        KeyGrid(inputKeys, columns = 4, vm)
        Section("Picture, sound & extras")
        KeyGrid(pictureKeys, columns = 3, vm)

        Section("Type on TV")
        Text(
            "Works when a search box or keyboard is open on the TV.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = typed,
            onValueChange = { typed = it },
            singleLine = true,
            label = { Text("Text") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { vm.text(typed) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.text(typed) }) { Text("Send text") }
            OutlinedButton(onClick = { vm.key(TvKey.ENTER) }) { Text("Enter") }
            OutlinedButton(onClick = { typed = "" }) { Text("Clear") }
        }

        Section("Custom key code")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = custom,
                onValueChange = { custom = it.uppercase().trim() },
                singleLine = true,
                label = { Text("KEY_...") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { vm.key(custom) }),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { vm.key(custom) }, enabled = custom.length > 4) { Text("Send") }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String) = Text(
    title,
    style = MaterialTheme.typography.titleSmall,
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(top = 10.dp),
)

@Composable
private fun KeyGrid(keys: List<KeySpec>, columns: Int, vm: RemoteViewModel, keyHeight: Int = 48) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    TextKey(
                        text = key.label,
                        onClick = { vm.key(key.code) },
                        repeat = key.repeat,
                        shape = RoundedCornerShape(12.dp),
                        containerColor = key.color ?: MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (key.color != null) key.textColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = if (keyHeight > 48) 18 else 13,
                        modifier = Modifier.weight(1f).height(keyHeight.dp),
                    )
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
