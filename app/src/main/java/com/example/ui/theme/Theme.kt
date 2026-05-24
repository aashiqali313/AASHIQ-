package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

@Composable
fun AashiqTheme(
    darkTheme: Boolean = true,
    amoledBlack: Boolean = true,
    content: @Composable () -> Unit
) {
    val backgroundColor = if (darkTheme) {
        if (amoledBlack) MatteBlack else CharcoalGray
    } else {
        Color(0xFFF5F5F7) // Luxury warm off-white secondary mode
    }

    val surfaceColor = if (darkTheme) {
        if (amoledBlack) CharcoalGray else Graphite
    } else {
        Color.White
    }

    val textColor = if (darkTheme) WarmWhite else Color(0xFF1C1C1E)

    val colors = DarkColorScheme.copy(
        background = backgroundColor,
        surface = surfaceColor,
        onBackground = textColor,
        onSurface = textColor
    )

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
