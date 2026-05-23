package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PremiumGold,
    onPrimary = MatteBlack,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    background = MatteBlack,
    onBackground = WarmWhite,
    surface = DarkSurface,
    onSurface = WarmWhite,
    surfaceVariant = Graphite,
    onSurfaceVariant = SoftGray,
    tertiary = SoftGoldGlow,
    primaryContainer = CharcoalGray,
    onPrimaryContainer = LightGold
)

private val AmoledColorScheme = darkColorScheme(
    primary = PremiumGold,
    onPrimary = AmoledBlack,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    background = AmoledBlack,
    onBackground = WarmWhite,
    surface = AmoledBlack,
    onSurface = WarmWhite,
    surfaceVariant = Graphite,
    onSurfaceVariant = SoftGray,
    tertiary = SoftGoldGlow,
    primaryContainer = CharcoalGray,
    onPrimaryContainer = LightGold
)

private val LightColorScheme = lightColorScheme(
    primary = PremiumGold,
    onPrimary = HeavyMetallic,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = HeavyMetallic,
    surface = LightSurface,
    onSurface = HeavyMetallic,
    surfaceVariant = LightBackground,
    onSurfaceVariant = HeavyMetallic,
    tertiary = HeavyMetallic
)

enum class ThemeMode {
    DARK, LIGHT, AMOLED
}

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.AMOLED -> AmoledColorScheme
        ThemeMode.DARK -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
