package ru.yandex.browser.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Флэт-палитра 2026: чистый чёрный, без градиентов, теней и стекла.
 * Единственный акцент — фирменный красный.
 */
val PureBlack = Color(0xFF000000)
val SurfaceDeep = Color(0xFF141416)
val SurfaceRaised = Color(0xFF1C1D21)
val HairLine = Color(0xFF26272B)
val TextPrimary = Color(0xFFFFFFFF)
val TextMuted = Color(0xFF8A8A8E)
val TextDim = Color(0xFF5A5A5E)
val AccentRed = Color(0xFFFC3F1D)
val AccentGreen = Color(0xFF34C759)
val AccentYellow = Color(0xFFFFC53D)

private val FlatDarkScheme = darkColorScheme(
    primary = AccentRed,
    onPrimary = Color.White,
    primaryContainer = AccentRed,
    onPrimaryContainer = Color.White,
    secondary = AccentRed,
    onSecondary = Color.White,
    background = PureBlack,
    onBackground = TextPrimary,
    surface = SurfaceDeep,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextMuted,
    outline = HairLine,
    outlineVariant = HairLine,
    error = AccentRed,
    onError = Color.White,
    scrim = Color(0xCC000000),
)

@Composable
fun YandexBrowserTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FlatDarkScheme,
        content = content,
    )
}
