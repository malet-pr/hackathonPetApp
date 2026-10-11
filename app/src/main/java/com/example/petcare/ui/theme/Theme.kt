package com.example.petcare.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EarthyColors = lightColorScheme(
    background = Color(0xFFF3EBDD),
    surface = Color(0xFFFFF8EF),
    surfaceVariant = Color(0xFFE8DDCD),
    onBackground = Color(0xFF34281F),
    onSurface = Color(0xFF34281F),
    onSurfaceVariant = Color(0xFF62564A),
    primary = Color(0xFF426044),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E3D2),
    onPrimaryContainer = Color(0xFF223324),
    secondary = Color(0xFF426044),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E3D2),
    onSecondaryContainer = Color(0xFF223324),
    tertiary = Color(0xFF78604A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8DDCD),
    onTertiaryContainer = Color(0xFF34281F),
    outline = Color(0xFF8B7C6E),
    outlineVariant = Color(0xFFD3C5B5),
    error = Color(0xFFB24A3F),
    onError = Color.White,
    errorContainer = Color(0xFFF7DAD5),
    onErrorContainer = Color(0xFF61231D),
    surfaceDim = Color(0xFFE8DDCD),
    surfaceBright = Color(0xFFFFF8EF),
    surfaceContainerLowest = Color(0xFFFFF8EF),
    surfaceContainerLow = Color(0xFFF9F1E6),
    surfaceContainer = Color(0xFFF3EBDD),
    surfaceContainerHigh = Color(0xFFEDE2D3),
    surfaceContainerHighest = Color(0xFFE8DDCD),
    inverseSurface = Color(0xFF34281F),
    inverseOnSurface = Color(0xFFF3EBDD),
    inversePrimary = Color(0xFFD7E3D2),
    surfaceTint = Color(0xFF426044),
)

@Composable
fun PetCareTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = EarthyColors, content = content)
}
