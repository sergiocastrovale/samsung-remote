package com.kp.samsungremote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object RemoteColors {
    val Power = Color(0xFFE5484D)
    val Red = Color(0xFFE5484D)
    val Green = Color(0xFF30A46C)
    val Yellow = Color(0xFFF5D90A)
    val Blue = Color(0xFF3E8BFF)
    val Online = Color(0xFF4CC38A)
    val Pending = Color(0xFFFFC53D)
    val Offline = Color(0xFF8B8D98)
    val BackgroundTop = Color(0xFF1B1D23)
    val BackgroundBottom = Color(0xFF0B0C0F)

    val background: Brush = Brush.verticalGradient(listOf(BackgroundTop, BackgroundBottom))
}

private val scheme = darkColorScheme(
    primary = Color(0xFF6EA8FF),
    onPrimary = Color(0xFF06204A),
    secondaryContainer = Color(0xFF2C3446),
    onSecondaryContainer = Color(0xFFDCE6FF),
    background = RemoteColors.BackgroundBottom,
    onBackground = Color(0xFFE8E9ED),
    surface = Color(0xFF121317),
    onSurface = Color(0xFFE8E9ED),
    surfaceVariant = Color(0xFF2B2E35),
    onSurfaceVariant = Color(0xFFC9CCD4),
    surfaceContainer = Color(0xFF1E2026),
    surfaceContainerHighest = Color(0xFF2B2E35),
    outline = Color(0xFF3A3D45),
    outlineVariant = Color(0xFF2A2C32),
)

@Composable
fun RemoteTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = scheme, content = content)
