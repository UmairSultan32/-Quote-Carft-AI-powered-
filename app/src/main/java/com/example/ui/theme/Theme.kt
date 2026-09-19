package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CopperLight,
    onPrimary = NavyDeep,
    primaryContainer = CopperDark,
    onPrimaryContainer = CopperSoft,
    secondary = CopperVibrant,
    onSecondary = Color.White,
    secondaryContainer = NavyCardDark,
    onSecondaryContainer = CopperLight,
    tertiary = Color(0xFFE2E8F0),
    onTertiary = NavyDeep,
    background = NavyDeep,
    onBackground = NavyTextLight,
    surface = NavySurfaceDark,
    onSurface = NavyTextLight,
    surfaceVariant = NavyCardDark,
    onSurfaceVariant = NavyTextMuted,
    outline = NavyBorderDark,
    error = MarginCriticalRed
)

private val LightColorScheme = lightColorScheme(
    primary = NavyDeep,
    onPrimary = Color.White,
    primaryContainer = NavyMedium,
    onPrimaryContainer = Color.White,
    secondary = CopperPrimary,
    onSecondary = Color.White,
    secondaryContainer = CopperSoft,
    onSecondaryContainer = CopperDark,
    tertiary = CopperVibrant,
    onTertiary = Color.White,
    background = OffWhite,
    onBackground = TextPrimaryLight,
    surface = CardWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    error = MarginCriticalRed
)

@Composable
fun QuoteCraftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
