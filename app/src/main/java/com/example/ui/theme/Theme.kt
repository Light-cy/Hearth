package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val HearthGlassColorScheme = lightColorScheme(
    primary = Forest800,
    onPrimary = Color.White,
    primaryContainer = Sage100,
    onPrimaryContainer = Forest900,
    secondary = Forest700,
    onSecondary = Color.White,
    secondaryContainer = Sage200,
    onSecondaryContainer = Forest900,
    tertiary = CreditGreen,
    onTertiary = Color.White,
    background = HearthAtmosphereBg,
    onBackground = HearthTextDark,
    surface = Color.White,
    onSurface = HearthTextDark,
    surfaceVariant = Sage100,
    onSurfaceVariant = Forest800,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = HearthCardHolster,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Sage100,
    outline = Forest700,
    outlineVariant = HearthGlassBorder,
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
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HearthGlassColorScheme,
        typography = Typography,
        shapes = HearthShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    HearthTheme(darkTheme = false, content = content)
}
