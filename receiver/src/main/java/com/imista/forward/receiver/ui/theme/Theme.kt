package com.imista.forward.receiver.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1),
    secondary = Color(0xFF06B6D4),
    tertiary = Color(0xFF10B981),
    background = Color(0xFF0B0B14),
    surface = Color(0xFF151521),
    surfaceVariant = Color(0xFF1E1E2F),
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFE2E8F0),
    error = Color(0xFFEF4444)
)

@Composable
fun ImistaReceiverTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColorScheme, typography = Typography(
        headlineLarge = Typography().headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp),
        titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold)
    ), content = content)
}
