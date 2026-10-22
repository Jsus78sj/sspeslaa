package com.speedmeter.lite.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.speedmeter.lite.utils.ThemeManager

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue, background = BackgroundLight, surface = CardLight,
    onBackground = TextLight, onSurface = TextLight,
    surfaceVariant = TrackLight, onSurfaceVariant = TextMutedLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue, background = BackgroundDark, surface = CardDark,
    onBackground = TextDark, onSurface = TextDark,
    surfaceVariant = TrackDark, onSurfaceVariant = TextMutedDark
)

private val NavyColorScheme = darkColorScheme(
    primary = PrimaryBlue, background = BackgroundNavy, surface = CardNavy,
    onBackground = TextNavy, onSurface = TextNavy,
    surfaceVariant = TrackNavy, onSurfaceVariant = TextMutedNavy
)

@Composable
fun SpeedMeterTheme(theme: String = ThemeManager.LIGHT, content: @Composable () -> Unit) {
    val scheme = when (theme) {
        ThemeManager.DARK -> DarkColorScheme
        ThemeManager.NAVY -> NavyColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
}
