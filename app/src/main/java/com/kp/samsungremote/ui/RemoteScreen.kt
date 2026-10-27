package com.kp.samsungremote.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kp.samsungremote.tv.TvKey
import com.kp.samsungremote.ui.components.IconKey
import com.kp.samsungremote.ui.components.RemoteButton
import com.kp.samsungremote.ui.components.raised
import com.kp.samsungremote.ui.theme.RemoteColors

@Composable
fun RemoteScreen(vm: RemoteViewModel, modifier: Modifier = Modifier) {
    val playing by vm.playing.collectAsStateWithLifecycle()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        KeyRow {
            Captioned("Power") {
                IconKey(
                    Icons.Filled.PowerSettingsNew,
                    "Power",
                    onClick = vm::power,
                    onHoldStart = { vm.keyDown(TvKey.POWER) },
                    onHoldEnd = { vm.keyUp(TvKey.POWER) },
                    containerColor = RemoteColors.Power,
                    contentColor = Color.White,
                )
            }
            Captioned("Settings") { IconKey(Icons.Filled.Settings, "Settings", { vm.key(TvKey.MENU) }) }
            Captioned("Source") { IconKey(Icons.AutoMirrored.Filled.Input, "Source", { vm.key(TvKey.SOURCE) }) }
        }

        DirectionPad(vm)

        KeyRow {
            Captioned("Return") {
                IconKey(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    "Return",
                    onClick = { vm.key(TvKey.RETURN) },
                    onHoldStart = { vm.keyDown(TvKey.RETURN) },
                    onHoldEnd = { vm.keyUp(TvKey.RETURN) },
                )
            }
            Captioned("Home") {
                IconKey(
                    Icons.Filled.Home,
                    "Home",
                    onClick = { vm.key(TvKey.HOME) },
                    onHoldStart = { vm.keyDown(TvKey.HOME) },
                    onHoldEnd = { vm.keyUp(TvKey.HOME) },
                )
            }
            Captioned("Exit") { IconKey(Icons.Filled.Close, "Exit", { vm.key(TvKey.EXIT) }) }
        }

        KeyRow {
            Rocker(
                title = "VOL",
                up = { vm.key(TvKey.VOL_UP) },
                down = { vm.key(TvKey.VOL_DOWN) },
                upIcon = Icons.Filled.Add,
                downIcon = Icons.Filled.Remove,
                middleIcon = Icons.AutoMirrored.Filled.VolumeOff,
                middleLabel = "Mute",
                middle = { vm.key(TvKey.MUTE) },
            )
            Rocker(
                title = "CH",
                up = { vm.key(TvKey.CH_UP) },
                down = { vm.key(TvKey.CH_DOWN) },
                upIcon = Icons.Filled.KeyboardArrowUp,
                downIcon = Icons.Filled.KeyboardArrowDown,
                middleIcon = Icons.AutoMirrored.Filled.List,
                middleLabel = "Channel list",
                middle = { vm.key(TvKey.CH_LIST) },
            )
        }

        MediaBar(vm, playing)
    }
}

@Composable
private fun KeyRow(content: @Composable () -> Unit) = Row(
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth(),
) {
    content()
}

@Composable
private fun Captioned(caption: String, key: @Composable () -> Unit) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(6.dp),
) {
    key()
    Text(caption, fontSize = 11.sp, letterSpacing = 0.4.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
}

@Composable
private fun DirectionPad(vm: RemoteViewModel) {
    val arrowColor = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(252.dp)
            .raised(MaterialTheme.colorScheme.surfaceContainer, CircleShape, elevation = 10.dp),
    ) {
        Arrow(Icons.Filled.KeyboardArrowUp, "Up", TvKey.UP, vm, Modifier.align(Alignment.TopCenter).padding(top = 6.dp), arrowColor)
        Arrow(Icons.Filled.KeyboardArrowDown, "Down", TvKey.DOWN, vm, Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp), arrowColor)
        Arrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Left", TvKey.LEFT, vm, Modifier.align(Alignment.CenterStart).padding(start = 6.dp), arrowColor)
        Arrow(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Right", TvKey.RIGHT, vm, Modifier.align(Alignment.CenterEnd).padding(end = 6.dp), arrowColor)
        RemoteButton(
            onClick = { vm.key(TvKey.ENTER) },
            label = "OK",
            onHoldStart = { vm.keyDown(TvKey.ENTER) },
            onHoldEnd = { vm.keyUp(TvKey.ENTER) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.Center).size(100.dp),
        ) {
            Text("OK", fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun Arrow(icon: ImageVector, label: String, code: String, vm: RemoteViewModel, modifier: Modifier, color: Color) = IconKey(
    icon,
    label,
    { vm.key(code) },
    modifier,
    size = 72.dp,
    iconSize = 40.dp,
    repeat = true,
    flat = true,
    containerColor = Color.Transparent,
    contentColor = color,
)

@Composable
private fun Rocker(
    title: String,
    up: () -> Unit,
    down: () -> Unit,
    upIcon: ImageVector,
    downIcon: ImageVector,
    middleIcon: ImageVector,
    middleLabel: String,
    middle: () -> Unit,
) {
    val shape = RoundedCornerShape(40.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(104.dp)
            .raised(MaterialTheme.colorScheme.surfaceVariant, shape, elevation = 8.dp)
            .padding(vertical = 4.dp),
    ) {
        RemoteButton(onClick = up, label = "$title up", repeat = true, flat = true, shape = shape, containerColor = Color.Transparent, modifier = Modifier.width(96.dp).height(76.dp)) {
            Icon(upIcon, contentDescription = null, modifier = Modifier.size(32.dp))
        }
        RemoteButton(onClick = middle, label = middleLabel, flat = true, containerColor = Color.Black.copy(alpha = 0.18f), modifier = Modifier.size(58.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Icon(middleIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
        RemoteButton(onClick = down, label = "$title down", repeat = true, flat = true, shape = shape, containerColor = Color.Transparent, modifier = Modifier.width(96.dp).height(76.dp)) {
            Icon(downIcon, contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun MediaBar(vm: RemoteViewModel, playing: Boolean) {
    val shape = RoundedCornerShape(36.dp)
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .raised(MaterialTheme.colorScheme.surfaceContainer, shape, elevation = 8.dp)
            .padding(8.dp),
    ) {
        IconKey(Icons.Filled.FastRewind, "Rewind", { vm.key(TvKey.REWIND) }, size = 56.dp, iconSize = 28.dp, flat = true, containerColor = Color.Transparent)
        RemoteButton(
            onClick = vm::playPause,
            label = if (playing) "Pause" else "Play",
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(64.dp),
        ) {
            AnimatedContent(
                targetState = playing,
                transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.6f)) },
                label = "playPause",
            ) { isPlaying ->
                Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(34.dp))
            }
        }
        IconKey(Icons.Filled.FastForward, "Fast forward", { vm.key(TvKey.FAST_FORWARD) }, size = 56.dp, iconSize = 28.dp, flat = true, containerColor = Color.Transparent)
    }
}
