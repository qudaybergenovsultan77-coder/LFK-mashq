package uz.kozboyin.mashq

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

data class AppColors(
    val bg: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val soft: Color,
    val accent: Color,
    val onAccent: Color
)

val LightColors = AppColors(
    bg = Color(0xFFFFFFFF), ink = Color(0xFF14231C), muted = Color(0xFF6B7C74),
    line = Color(0xFFDDE6E1), soft = Color(0xFFE9F6EF), accent = Color(0xFF2E9E6B),
    onAccent = Color(0xFFFFFFFF)
)

val DarkColors = AppColors(
    bg = Color(0xFF0F1A15), ink = Color(0xFFE7F1EC), muted = Color(0xFF8DA298),
    line = Color(0xFF25382F), soft = Color(0xFF17291F), accent = Color(0xFF4CC38A),
    onAccent = Color(0xFF0F1A15)
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(primary = c.accent, background = c.bg, surface = c.bg, onBackground = c.ink, onSurface = c.ink)
    } else {
        lightColorScheme(primary = c.accent, background = c.bg, surface = c.bg, onBackground = c.ink, onSurface = c.ink)
    }
    CompositionLocalProvider(LocalAppColors provides c) {
        MaterialTheme(colorScheme = scheme) {
            Surface(modifier = Modifier.fillMaxSize(), color = c.bg, content = content)
        }
    }
}
