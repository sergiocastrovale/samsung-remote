package com.kp.samsungremote.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val REPEAT_DELAY_MS = 400L
private const val REPEAT_INTERVAL_MS = 120L
private const val HOLD_DELAY_MS = 600L

/** Raised look: lit from above, a hairline highlight on the rim and a soft drop shadow. */
fun raisedBrush(color: Color): Brush = Brush.verticalGradient(listOf(lerp(color, Color.White, 0.10f), lerp(color, Color.Black, 0.18f)))

fun rimBrush(): Brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.02f)))

/** Surface shared by raised buttons and panels (D-pad ring, rockers, media bar). */
fun Modifier.raised(color: Color, shape: Shape, elevation: Dp = 6.dp): Modifier = this
    .shadow(elevation, shape, ambientColor = Color.Black, spotColor = Color.Black)
    .clip(shape)
    .background(raisedBrush(color))
    .border(1.dp, rimBrush(), shape)

/**
 * Remote key. [repeat] fires [onClick] on press and keeps firing while held (volume, channel,
 * arrows). With [onHoldStart]/[onHoldEnd] a long press becomes a key held down on the TV
 * (power menu, long Home) and a short tap stays a click. [flat] keys have no surface of their
 * own, they sit on a raised panel and only light up while pressed.
 */
@Composable
fun RemoteButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    repeat: Boolean = false,
    onHoldStart: (() -> Unit)? = null,
    onHoldEnd: (() -> Unit)? = null,
    shape: Shape = CircleShape,
    flat: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val click by rememberUpdatedState(onClick)
    val holdStart by rememberUpdatedState(onHoldStart)
    val holdEnd by rememberUpdatedState(onHoldEnd)
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, spring(stiffness = 900f), label = "scale")
    val glow by animateFloatAsState(if (pressed) 1f else 0f, label = "glow")

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(if (flat) Modifier.clip(shape).background(containerColor) else Modifier.raised(containerColor, shape, if (pressed) 2.dp else 6.dp))
            .background(Color.White.copy(alpha = 0.14f * glow), shape)
            .semantics {
                role = Role.Button
                contentDescription = label
            }
            .pointerInput(repeat) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        var held = false
                        val job = when {
                            repeat -> scope.launch {
                                click()
                                delay(REPEAT_DELAY_MS)
                                while (isActive) {
                                    click()
                                    delay(REPEAT_INTERVAL_MS)
                                }
                            }
                            holdStart != null -> scope.launch {
                                delay(HOLD_DELAY_MS)
                                held = true
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                holdStart?.invoke()
                            }
                            else -> null
                        }
                        val released = tryAwaitRelease()
                        job?.cancel()
                        pressed = false
                        when {
                            repeat -> Unit
                            held -> holdEnd?.invoke()
                            released -> click()
                        }
                    },
                )
            },
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

@Composable
fun IconKey(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 24.dp,
    repeat: Boolean = false,
    flat: Boolean = false,
    onHoldStart: (() -> Unit)? = null,
    onHoldEnd: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) = RemoteButton(
    onClick = onClick,
    label = label,
    modifier = modifier.size(size),
    repeat = repeat,
    flat = flat,
    onHoldStart = onHoldStart,
    onHoldEnd = onHoldEnd,
    containerColor = containerColor,
    contentColor = contentColor,
) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize))
}

@Composable
fun TextKey(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    repeat: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontSize: Int = 14,
) = RemoteButton(
    onClick = onClick,
    label = text,
    modifier = modifier,
    repeat = repeat,
    shape = shape,
    containerColor = containerColor,
    contentColor = contentColor,
) {
    Text(text, fontSize = fontSize.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
}
