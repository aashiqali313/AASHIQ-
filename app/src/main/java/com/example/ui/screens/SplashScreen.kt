package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.PremiumGold
import com.example.ui.theme.SoftGoldGlow
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashCompleted: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale_anim"
    )

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "alpha_anim"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(1600) // Preload duration of 1.6s max
        onSplashCompleted()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteBlack)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Decorative Soft Radial Golden Glow Background
        Box(
            modifier = Modifier
                .size(400.dp)
                .alpha(0.08f * alphaAnim)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(SoftGoldGlow, Color.Transparent)
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scaleAnim).alpha(alphaAnim)
        ) {
            // Elegant Glassy Circular Logo Box
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(elevation = 20.dp, shape = RoundedCornerShape(55.dp), clip = false)
                    .background(
                        color = Color(0xFF141414),
                        shape = RoundedCornerShape(55.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Gold Double Outer Ring
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color.Transparent, shape = RoundedCornerShape(50.dp))
                        .shadow(elevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .background(Color.Transparent)
                    )
                }

                // Centered Play triangle stylized
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "AASHIQ+ Core Logo",
                    tint = PremiumGold,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Premium Spaced Typography
            Text(
                text = "A A S H I Q  +",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = PremiumGold,
                letterSpacing = 4.sp,
                modifier = Modifier.testTag("app_logo_title")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "CINEMATIC OFFLINE MASTERCLASS",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = SoftGoldGlow.copy(alpha = 0.7f),
                letterSpacing = 2.sp
            )
        }

        // Sleek Premium Thin Progress Line at the very bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .width(180.dp)
                .height(2.dp)
                .background(Color(0xFF222222), RoundedCornerShape(1.dp))
        ) {
            val progressWidth = remember { Animatable(0f) }
            LaunchedEffect(startAnimation) {
                if (startAnimation) {
                    progressWidth.animateTo(
                        targetValue = 180f,
                        animationSpec = tween(1500, easing = FastOutSlowInEasing)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(progressWidth.value.dp)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(PremiumGold, SoftGoldGlow)
                        ),
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}
