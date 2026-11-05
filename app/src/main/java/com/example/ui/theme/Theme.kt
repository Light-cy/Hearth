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
    primary = TealPrimaryDark,
    onPrimary = Forest900,
    primaryContainer = Forest800,
    onPrimaryContainer = Color.White,
    secondary = AmberAccentDark,
    onSecondary = Color.Black,
    secondaryContainer = AmberOnContainer,
    onSecondaryContainer = AmberContainer,
    tertiary = CreditGreenDark,
    background = Forest900,
    surface = Forest850,
    surfaceVariant = Forest800,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Sage300,
    error = DebtRedDark,
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = Forest800,
    onPrimary = Color.White,
    primaryContainer = Sage100,
    onPrimaryContainer = Forest900,
    secondary = AmberAccent,
    onSecondary = Color.White,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = AmberOnContainer,
    tertiary = CreditGreen,
    background = HearthAtmosphereBg,
    surface = HearthGlassSurface,
    surfaceVariant = Sage100,
    onBackground = HearthTextDark,
    onSurface = HearthTextDark,
    onSurfaceVariant = HearthTextMuted,
    error = DebtRed,
    onError = Color.White
)

val HearthShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun HearthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = HearthShapes,
        content = content
    )
}

@Composable
fun HisaabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    HearthTheme(darkTheme = darkTheme, content = content)
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    HearthTheme(darkTheme = darkTheme, content = content)
}
