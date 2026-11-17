package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = CobaltPrimaryDark,
    onPrimary = Color(0xFF061B38),
    primaryContainer = CobaltContainerDark,
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = SlateSecondaryDark,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = ShowroomSurfaceVariantDark,
    onSecondaryContainer = ShowroomTextPrimaryDark,
    tertiary = AmberAccentDark,
    onTertiary = Color(0xFF2B1A00),
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = Color(0xFFFDE68A),
    background = ShowroomBackgroundDark,
    onBackground = ShowroomTextPrimaryDark,
    surface = ShowroomSurfaceDark,
    onSurface = ShowroomTextPrimaryDark,
    surfaceVariant = ShowroomSurfaceVariantDark,
    onSurfaceVariant = ShowroomTextSecondaryDark,
    outline = ShowroomBorderDark,
    outlineVariant = Color(0xFF1F2937),
    error = OverdueRedTextDark,
    onError = Color(0xFF450A0A),
    errorContainer = OverdueRedContainerDark,
    onErrorContainer = Color(0xFFFEE2E2)
)

private val LightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = CobaltContainer,
    onPrimaryContainer = Color(0xFF0C2447),
    secondary = SlateSecondary,
    onSecondary = Color.White,
    secondaryContainer = ShowroomSurfaceVariantLight,
    onSecondaryContainer = ShowroomTextPrimaryLight,
    tertiary = AmberAccent,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = Color(0xFF451A03),
    background = ShowroomBackgroundLight,
    onBackground = ShowroomTextPrimaryLight,
    surface = ShowroomSurfaceLight,
    onSurface = ShowroomTextPrimaryLight,
    surfaceVariant = ShowroomSurfaceVariantLight,
    onSurfaceVariant = ShowroomTextSecondaryLight,
    outline = ShowroomBorderLight,
    outlineVariant = Color(0xFFEAEFF6),
    error = OverdueRed,
    onError = Color.White,
    errorContainer = OverdueRedContainerLight,
    onErrorContainer = Color(0xFF7F1D1D)
)

val AutoAgendaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AutoAgendaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AutoAgendaShapes,
        content = content
    )
}
