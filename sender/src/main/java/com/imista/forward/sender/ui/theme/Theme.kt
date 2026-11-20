package com.imista.forward.sender.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4F46E5),
    secondary = Color(0xFF06B6D4),
    tertiary = Color(0xFF10B981),
    background = Color(0xFF0B0B14),
    surface = Color(0xFF151521),
    surfaceVariant = Color(0xFF1E1E2F),
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFE2E8F0),
    error = Color(0xFFEF4444)
)

private val LightColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1),
    secondary = Color(0xFF06B6D4),
    background = Color(0xFF0B0B14),
    surface = Color(0xFF151521),
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFE2E8F0)
)

@Composable
fun ImistaSenderTheme(content: @Composable () -> Unit) {
    val colorScheme = DarkColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            headlineLarge = Typography().headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp),
            titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold),
            bodyMedium = Typography().bodyMedium.copy(lineHeight = 20.sp)
        ),
        content = content
    )
}
