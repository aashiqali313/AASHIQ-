package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PremiumGold,
    secondary = SoftGoldGlow,
    tertiary = SubtleElectricBlue,
    background = MatteBlack,
    surface = CharcoalGray,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = WarmWhite,
    onSurface = WarmWhite,
    outline = BorderGold
)

private val LightColorScheme = lightColorScheme(
    primary = DarkGoldAccent,
    secondary = BorderGold,
    tertiary = SubtleElectricBlue,
    background = Color(0xFFFAF9F6), // Luxury Warm Off-white
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1B1B1D),
    onSurface = Color(0xFF1B1B1D),
    outline = BorderGold
)

@Composable
fun AashiqTheme(
    darkTheme: Boolean = true,
    amoledBlack: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        val backgroundColor = if (amoledBlack) MatteBlack else CharcoalGray
        val surfaceColor = if (amoledBlack) CharcoalGray else Graphite
        DarkColorScheme.copy(
            background = backgroundColor,
            surface = surfaceColor,
            onBackground = WarmWhite,
            onSurface = WarmWhite
        )
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
