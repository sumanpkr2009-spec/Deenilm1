package com.deenilm.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DeenColorScheme = darkColorScheme(
    primary = DeenGold,
    onPrimary = DeenOnGold,
    secondary = DeenEmerald,
    background = DeenBg,
    surface = DeenSurface,
    surfaceVariant = DeenSurfaceVariant,
    onBackground = DeenCream,
    onSurface = DeenCream,
    outline = DeenOutline
)

@Composable
fun DeenIlmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DeenColorScheme,
        typography = DeenTypography,
        content = content
    )
}
