package com.elspot.toldos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

private val ElSpotColors = darkColorScheme(
    primary = Color(0xFF55B7F5),
    onPrimary = Color(0xFF06121D),
    primaryContainer = Color(0xFF123653),
    onPrimaryContainer = Color(0xFFD9EFFF),
    secondary = Color(0xFF55D6E8),
    onSecondary = Color(0xFF041316),
    secondaryContainer = Color(0xFF0B3D46),
    onSecondaryContainer = Color(0xFFB7F3FA),
    background = Color(0xFF0B1018),
    onBackground = Color(0xFFF7FAFC),
    surface = Color(0xFF111927),
    onSurface = Color(0xFFF7FAFC),
    surfaceVariant = Color(0xFF1A2535),
    onSurfaceVariant = Color(0xFFB8C5D4),
    outline = Color(0xFF45566B),
    tertiary = Color(0xFFFFC247),
    onTertiary = Color(0xFF261900),
    tertiaryContainer = Color(0xFF4A3400),
    onTertiaryContainer = Color(0xFFFFDEA3),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2A0505)
)

private val ElSpotTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

@Composable
fun ElSpotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ElSpotColors,
        typography = ElSpotTypography,
        content = content
    )
}
