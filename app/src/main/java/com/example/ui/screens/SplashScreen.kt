package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AashiqLogo
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Run parallel logo scale and fade animations
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(1200)
        )
    }
    LaunchedEffect(key1 = true) {
        alpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(900)
        )
        // Duration: 1.8 seconds max, then transition
        delay(1300)
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(400)
        )
        delay(400)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070708)),
        contentAlignment = Alignment.Center
    ) {
        // Subtly glowing atmospheric radial background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x1F9C803E), Color.Transparent),
                        radius = 400.dp.value
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha.value)
        ) {
            Box(
                modifier = Modifier
                    .scale(scale.value)
                    .size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                AashiqLogo()
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "A A S H I Q +",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 6.sp,
                fontFamily = FontFamily.SansSerif
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "PREMIUM OFFLINE LEARNING",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFAEAEB2),
                letterSpacing = 4.sp,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}
