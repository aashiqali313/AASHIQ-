package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AashiqColors(
    val background: Color,
    val surface: Color,
    val elevatedSurface: Color,
    val card: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val border: Color,
    val glassBlur: Color,
    val isLight: Boolean
)

val LocalAashiqColors = staticCompositionLocalOf<AashiqColors> {
    error("No AashiqColors provided")
}

object AashiqTheme {
    val colors: AashiqColors
        @Composable
        get() = LocalAashiqColors.current
}

private val BaseDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD4AF37),
    secondary = Color(0xFFFBE49D),
    tertiary = SubtleElectricBlue,
    background = Color(0xFF070707),
    surface = Color(0xFF111111),
    surfaceVariant = Color(0xFF1B1B1B),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFFDFDFD),
    onSurface = Color(0xFFFDFDFD),
    onSurfaceVariant = Color(0xFFADADAD),
    outline = Color(0xFF8E711A)
)

private val BaseLightColorScheme = lightColorScheme(
    primary = Color(0xFFA67C00),
    secondary = Color(0xFF8E711A),
    tertiary = SubtleElectricBlue,
    background = Color(0xFFFAF8F5), // Luxury Warm Ivory
    surface = Color(0xFFFFFDFB),    // Soft Matte White
    surfaceVariant = Color(0xFFF3EFE7), // Warm Champagne Ivory
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1F1E1A), // Rich dark espresso text
    onSurface = Color(0xFF1F1E1A),   // Rich dark espresso text
    onSurfaceVariant = Color(0xFF7A7568), // Subdued taupe/gray text
    outline = Color(0xFFE2DCCE)     // Subtle warm border
)

@Composable
fun AashiqTheme(
    darkTheme: Boolean = true,
    amoledBlack: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemIsLight = !darkTheme
    val colors = if (darkTheme) {
        val backgroundColor = if (amoledBlack) Color(0xFF070707) else Color(0xFF111111)
        val surfaceColor = if (amoledBlack) Color(0xFF111111) else Color(0xFF1B1B1B)
        BaseDarkColorScheme.copy(
            background = backgroundColor,
            surface = surfaceColor,
            onBackground = Color(0xFFFDFDFD),
            onSurface = Color(0xFFFDFDFD)
        )
    } else {
        BaseLightColorScheme
    }

    val aashiqColors = if (darkTheme) {
        val bg = if (amoledBlack) Color(0xFF070707) else Color(0xFF111111)
        val surf = if (amoledBlack) Color(0xFF111111) else Color(0xFF1B1B1B)
        AashiqColors(
            background = bg,
            surface = surf,
            elevatedSurface = Color(0xFF1B1B1B),
            card = Color(0xFF222222),
            textPrimary = Color(0xFFFDFDFD),
            textSecondary = Color(0xFFADADAD),
            accent = PremiumGold,
            border = Color(0xFF8E711A),
            glassBlur = Color(0x59161616),
            isLight = false
        )
    } else {
        AashiqColors(
            background = Color(0xFFFAF8F5),     // Luxury Warm Ivory
            surface = Color(0xFFFFFDFB),        // Soft Matte White
            elevatedSurface = Color(0xFFF3EFE7),// Warm Champagne Ivory
            card = Color(0xFFFAF6EE),           // Elegant Soft Champagne Ivory card
            textPrimary = Color(0xFF1F1E1A),    // Rich dark espresso
            textSecondary = Color(0xFF7A7568),  // Subdued warm taupe/gray
            accent = Color(0xFFA67C00),          // Champagne Gold Accent
            border = Color(0xFFE2DCCE),         // Luxury warm champagne border
            glassBlur = Color(0xD8FFFFFF),      // Light elegant translucent glass
            isLight = true
        )
    }

    CompositionLocalProvider(
        LocalAashiqColors provides aashiqColors
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = Typography,
            content = content
        )
    }
}
